package com.api.bedhcd.util;

import com.api.bedhcd.shared.dto.importing.ExpectedAttendanceImportRecord;
import com.api.bedhcd.shared.dto.importing.ProxyImportRecord;
import com.api.bedhcd.shared.dto.importing.ShareholderImportRecord;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StreamingExcelHelper {

    public static void streamShareholders(String filePath, int batchSize,
            Consumer<List<ShareholderImportRecord>> batchConsumer) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(filePath, PackageAccess.READ)) {
            XSSFReader xssfReader = new XSSFReader(pkg);
            StylesTable styles = xssfReader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            if (iter.hasNext()) {
                try (InputStream stream = iter.next()) {
                    ShareholderSheetHandler handler = new ShareholderSheetHandler(batchSize, batchConsumer);
                    processSheet(styles, strings, handler, stream);
                }
            }
        }
    }

    public static void streamProxies(String filePath, int batchSize, Consumer<List<ProxyImportRecord>> batchConsumer)
            throws Exception {
        try (OPCPackage pkg = OPCPackage.open(filePath, PackageAccess.READ)) {
            XSSFReader xssfReader = new XSSFReader(pkg);
            StylesTable styles = xssfReader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            if (iter.hasNext()) {
                try (InputStream stream = iter.next()) {
                    ProxySheetHandler handler = new ProxySheetHandler(batchSize, batchConsumer);
                    processSheet(styles, strings, handler, stream);
                }
            }
        }
    }

    public static void streamExpectedAttendance(String filePath, int batchSize,
            Consumer<List<ExpectedAttendanceImportRecord>> batchConsumer) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(filePath, PackageAccess.READ)) {
            XSSFReader xssfReader = new XSSFReader(pkg);
            StylesTable styles = xssfReader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
            if (iter.hasNext()) {
                try (InputStream stream = iter.next()) {
                    ExpectedAttendanceSheetHandler handler = new ExpectedAttendanceSheetHandler(batchSize,
                            batchConsumer);
                    processSheet(styles, strings, handler, stream);
                }
            }
        }
    }

    private static void processSheet(StylesTable styles, ReadOnlySharedStringsTable strings,
            XSSFSheetXMLHandler.SheetContentsHandler sheetHandler, InputStream sheetInputStream) throws Exception {
        javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XMLReader sheetParser = factory.newSAXParser().getXMLReader();
        XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(styles, strings, sheetHandler, false);
        sheetParser.setContentHandler(handler);
        sheetParser.parse(new InputSource(sheetInputStream));
    }

    // --- Shareholder Handler ---
    private static class ShareholderSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final int batchSize;
        private final Consumer<List<ShareholderImportRecord>> batchConsumer;
        // Dùng Map để merge record trùng CCCD (giống mergeShareholderRecords trong
        // ExcelHelper)
        private final java.util.Map<String, ShareholderImportRecord> mergeMap = new java.util.LinkedHashMap<>();
        private final List<ShareholderImportRecord> currentBatch = new ArrayList<>();
        private ShareholderImportRecord.ShareholderImportRecordBuilder currentRecordBuilder;
        private int currentRow = -1;

        public ShareholderSheetHandler(int batchSize, Consumer<List<ShareholderImportRecord>> batchConsumer) {
            this.batchSize = batchSize;
            this.batchConsumer = batchConsumer;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = rowNum;
            if (rowNum > 0) { // Skip header
                currentRecordBuilder = ShareholderImportRecord.builder();
            }
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum > 0 && currentRecordBuilder != null) {
                ShareholderImportRecord record = currentRecordBuilder.build();
                if (record.getCccd() != null && !record.getCccd().isEmpty()) {
                    // Merge nếu trùng CCCD (cộng dồn cổ phần)
                    if (mergeMap.containsKey(record.getCccd())) {
                        ShareholderImportRecord existing = mergeMap.get(record.getCccd());
                        long totalShares = (existing.getShares() != null ? existing.getShares() : 0L)
                                + (record.getShares() != null ? record.getShares() : 0L);
                        existing.setShares(totalShares);
                    } else {
                        mergeMap.put(record.getCccd(), record);
                        currentBatch.add(record);
                    }
                }

                if (currentBatch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue,
                org.apache.poi.xssf.usermodel.XSSFComment comment) {
            if (currentRow == 0 || currentRecordBuilder == null)
                return;
            if (formattedValue == null || formattedValue.isEmpty())
                return;

            String colLetter = cellReference.replaceAll("[0-9]", "");
            int colIndex = colLetterToIndex(colLetter);

            // Mapping theo ExcelHelper.parseShareholders():
            // cell(1)=cccd, cell(2)=fullName, cell(3)=investorCode, cell(4)=shares
            // cell(5)=email, cell(6)=phoneNumber, cell(7)=address, cell(8)=dateOfIssue
            // cell(9)=placeOfIssue, cell(10)=nation, cell(11)=password
            switch (colIndex) {
                case 1:
                    currentRecordBuilder.cccd(formattedValue);
                    break;
                case 2:
                    currentRecordBuilder.fullName(formattedValue);
                    break;
                case 3:
                    currentRecordBuilder.investorCode(formattedValue);
                    break;
                case 4:
                    currentRecordBuilder.shares(parseLongSafely(formattedValue));
                    break;
                case 5:
                    currentRecordBuilder.email(formattedValue);
                    break;
                case 6:
                    currentRecordBuilder.phoneNumber(formattedValue);
                    break;
                case 7:
                    currentRecordBuilder.address(formattedValue);
                    break;
                case 8:
                    currentRecordBuilder.dateOfIssue(formattedValue);
                    break;
                case 9:
                    currentRecordBuilder.placeOfIssue(formattedValue);
                    break;
                case 10:
                    currentRecordBuilder.nation(formattedValue);
                    break;
                case 11:
                    currentRecordBuilder.password(formattedValue);
                    break;
            }
        }

        @Override
        public void endSheet() {
            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }
    }

    // --- Proxy Handler ---
    private static class ProxySheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final int batchSize;
        private final Consumer<List<ProxyImportRecord>> batchConsumer;
        private final List<ProxyImportRecord> currentBatch = new ArrayList<>();
        private ProxyImportRecord.ProxyImportRecordBuilder currentRecordBuilder;
        private int currentRow = -1;

        public ProxySheetHandler(int batchSize, Consumer<List<ProxyImportRecord>> batchConsumer) {
            this.batchSize = batchSize;
            this.batchConsumer = batchConsumer;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = rowNum;
            if (rowNum > 0) {
                currentRecordBuilder = ProxyImportRecord.builder();
            }
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum > 0 && currentRecordBuilder != null) {
                ProxyImportRecord record = currentRecordBuilder.build();
                if (record.getDelegatorCccd() != null && !record.getDelegatorCccd().isEmpty()) {
                    currentBatch.add(record);
                }

                if (currentBatch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue,
                org.apache.poi.xssf.usermodel.XSSFComment comment) {
            if (currentRow == 0 || currentRecordBuilder == null)
                return;
            if (formattedValue == null || formattedValue.isEmpty())
                return;

            int colIndex = colLetterToIndex(cellReference.replaceAll("[0-9]", ""));

            // Mapping theo ExcelHelper.parseProxies():
            // cell(0)=delegatorCccd, cell(1)=proxyCccd, cell(2)=sharesDelegated
            // cell(3)=authorizationDocument, cell(4)=authorizationDate, cell(5)=description
            // cell(6)=fullName, cell(7)=email, cell(8)=dateOfIssue
            // cell(9)=address, cell(10)=placeOfIssue
            switch (colIndex) {
                case 0:
                    currentRecordBuilder.delegatorCccd(formattedValue);
                    break;
                case 1:
                    currentRecordBuilder.proxyCccd(formattedValue);
                    break;
                case 2:
                    currentRecordBuilder.sharesDelegated(parseLongSafely(formattedValue));
                    break;
                case 3:
                    currentRecordBuilder.authorizationDocument(formattedValue);
                    break;
                case 4:
                    currentRecordBuilder.authorizationDate(ExcelHelper.parseDateOfIssue(formattedValue));
                    break;
                case 5:
                    currentRecordBuilder.description(formattedValue);
                    break;
                case 6:
                    currentRecordBuilder.fullName(formattedValue);
                    break;
                case 7:
                    currentRecordBuilder.email(formattedValue);
                    break;
                case 8:
                    currentRecordBuilder.dateOfIssue(formattedValue);
                    break;
                case 9:
                    currentRecordBuilder.address(formattedValue);
                    break;
                case 10:
                    currentRecordBuilder.placeOfIssue(formattedValue);
                    break;
            }
        }

        @Override
        public void endSheet() {
            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }
    }

    // --- Expected Attendance Handler ---
    private static class ExpectedAttendanceSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final int batchSize;
        private final Consumer<List<ExpectedAttendanceImportRecord>> batchConsumer;
        // Dùng Map để dedupe trùng CCCD (giữ bản ghi cuối cùng)
        private final java.util.Map<String, ExpectedAttendanceImportRecord> mergeMap = new java.util.LinkedHashMap<>();
        private final List<ExpectedAttendanceImportRecord> currentBatch = new ArrayList<>();
        private ExpectedAttendanceImportRecord.ExpectedAttendanceImportRecordBuilder currentRecordBuilder;
        private int currentRow = -1;

        public ExpectedAttendanceSheetHandler(int batchSize,
                Consumer<List<ExpectedAttendanceImportRecord>> batchConsumer) {
            this.batchSize = batchSize;
            this.batchConsumer = batchConsumer;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = rowNum;
            if (rowNum > 0) { // Skip header
                currentRecordBuilder = ExpectedAttendanceImportRecord.builder();
            }
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum > 0 && currentRecordBuilder != null) {
                ExpectedAttendanceImportRecord record = currentRecordBuilder.build();
                if (record.getCccd() != null && !record.getCccd().isEmpty()) {
                    // Nếu trùng CCCD, thay thế bản ghi cũ trong batch
                    if (mergeMap.containsKey(record.getCccd())) {
                        ExpectedAttendanceImportRecord existing = mergeMap.get(record.getCccd());
                        currentBatch.remove(existing);
                        mergeMap.put(record.getCccd(), record);
                        currentBatch.add(record);
                    } else {
                        mergeMap.put(record.getCccd(), record);
                        currentBatch.add(record);
                    }
                }

                if (currentBatch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue,
                org.apache.poi.xssf.usermodel.XSSFComment comment) {
            if (currentRow == 0 || currentRecordBuilder == null)
                return;
            if (formattedValue == null || formattedValue.isEmpty())
                return;

            int colIndex = colLetterToIndex(cellReference.replaceAll("[0-9]", ""));

            // Format 4 cột: 
            // cell(0) = CCCD cổ đông
            // cell(1) = CCCD người nhận uỷ quyền
            // cell(2) = Số CP tham dự
            // cell(3) = Số CP uỷ quyền
            switch (colIndex) {
                case 0:
                    currentRecordBuilder.cccd(formattedValue);
                    break;
                case 1:
                    currentRecordBuilder.proxyCccd(formattedValue);
                    break;
                case 2:
                    currentRecordBuilder.expectedShares(parseLongSafely(formattedValue));
                    break;
                case 3:
                    currentRecordBuilder.proxyShares(parseLongSafely(formattedValue));
                    break;
            }
        }

        @Override
        public void endSheet() {
            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }
    }

    private static int colLetterToIndex(String colLetter) {
        int index = 0;
        for (int i = 0; i < colLetter.length(); i++) {
            index = index * 26 + (colLetter.charAt(i) - 'A' + 1);
        }
        return index - 1;
    }

    private static long parseLongSafely(String val) {
        try {
            if (val == null || val.trim().isEmpty())
                return 0L;
            String cleanVal = val.trim().replaceAll("[^0-9.]", "");
            if (cleanVal.isEmpty())
                return 0L;
            if (cleanVal.contains(".")) {
                return (long) Double.parseDouble(cleanVal);
            }
            return Long.parseLong(cleanVal);
        } catch (Exception e) {
            return 0L;
        }
    }
}
