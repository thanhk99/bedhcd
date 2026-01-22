package com.api.bedhcd.util;

import com.api.bedhcd.dto.importing.ProxyImportRecord;
import com.api.bedhcd.dto.importing.ShareholderImportRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    public static List<ShareholderImportRecord> parseShareholders(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            List<ShareholderImportRecord> records = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) { // Skip header
                    rowNumber++;
                    continue;
                }

                ShareholderImportRecord record = ShareholderImportRecord.builder()
                        .cccd(getCellValueAsString(currentRow.getCell(1)))
                        .fullName(getCellValueAsString(currentRow.getCell(2)))
                        .investorCode(getCellValueAsString(currentRow.getCell(3)))
                        .shares(getCellValueAsLong(currentRow.getCell(4)))
                        .email(getCellValueAsString(currentRow.getCell(5)))
                        .phoneNumber(getCellValueAsString(currentRow.getCell(6)))
                        .address(getCellValueAsString(currentRow.getCell(7)))
                        .dateOfIssue(getCellValueAsString(currentRow.getCell(8)))
                        .placeOfIssue(getCellValueAsString(currentRow.getCell(9)))
                        .nation(getCellValueAsString(currentRow.getCell(10)))
                        .password(getCellValueAsString(currentRow.getCell(11)))
                        .build();

                if (record.getCccd() != null && !record.getCccd().isEmpty()) {
                    records.add(record);
                }
            }
            return records;
        } catch (Exception e) {
            throw new RuntimeException("Fail to parse Excel file: " + e.getMessage());
        }
    }

    public static List<ProxyImportRecord> parseProxies(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            List<ProxyImportRecord> records = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (rowNumber == 0) { // Skip header
                    rowNumber++;
                    continue;
                }

                ProxyImportRecord record = ProxyImportRecord.builder()
                        .delegatorCccd(getCellValueAsString(currentRow.getCell(0)))
                        .proxyCccd(getCellValueAsString(currentRow.getCell(1)))
                        .sharesDelegated(getCellValueAsLong(currentRow.getCell(2)))
                        .authorizationDocument(getCellValueAsString(currentRow.getCell(3)))
                        .authorizationDate(getCellValueAsLocalDate(currentRow.getCell(4)))
                        .description(getCellValueAsString(currentRow.getCell(5)))
                        .build();

                if (record.getDelegatorCccd() != null && !record.getDelegatorCccd().isEmpty()) {
                    records.add(record);
                }
            }
            return records;
        } catch (Exception e) {
            throw new RuntimeException("Fail to parse Excel file: " + e.getMessage());
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell))
                    return cell.getDateCellValue().toString();
                return String.format("%.0f", cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static Long getCellValueAsLong(Cell cell) {
        if (cell == null)
            return 0L;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Long.parseLong(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    private static java.time.LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null)
            return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return java.time.LocalDate.parse(cell.getStringCellValue());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
