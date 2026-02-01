package com.api.bedhcd.util;

import com.api.bedhcd.dto.importing.ProxyImportRecord;
import com.api.bedhcd.dto.importing.ShareholderImportRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

            // Nhóm các bản ghi theo họ tên và gộp lại
            return mergeShareholderRecords(records);
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

                try {
                    ProxyImportRecord record = ProxyImportRecord.builder()
                            .delegatorCccd(getCellValueAsString(currentRow.getCell(0)))
                            .proxyCccd(getCellValueAsString(currentRow.getCell(1)))
                            .sharesDelegated(getCellValueAsLong(currentRow.getCell(2)))
                            .authorizationDocument(getCellValueAsString(currentRow.getCell(3)))
                            .authorizationDate(getCellValueAsLocalDate(currentRow.getCell(4)))
                            .description(getCellValueAsString(currentRow.getCell(5)))
                            .fullName(getCellValueAsString(currentRow.getCell(6)))
                            .email(getCellValueAsString(currentRow.getCell(7)))
                            .dateOfIssue(getCellValueAsString(currentRow.getCell(8)))
                            .address(getCellValueAsString(currentRow.getCell(9)))
                            .placeOfIssue(getCellValueAsString(currentRow.getCell(10)))
                            .build();

                    if (record.getDelegatorCccd() != null && !record.getDelegatorCccd().isEmpty()) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing row " + rowNumber + ": " + e.getMessage());
                }
                rowNumber++;
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
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell))
                    return cell.getDateCellValue().toString();
                return String.format("%.0f", cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
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
                String val = cell.getStringCellValue().replaceAll("[^0-9]", "");
                return val.isEmpty() ? 0L : Long.parseLong(val);
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
            String dateStr = cell.getStringCellValue().trim();
            if (dateStr.isEmpty())
                return null;

            // Try different formats
            String[] patterns = { "yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy" };
            for (String pattern : patterns) {
                try {
                    return java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern(pattern));
                } catch (Exception ignored) {
                }
            }
            System.err.println("Could not parse date: " + dateStr);
        }
        return null;
    }

    private static List<ShareholderImportRecord> mergeShareholderRecords(List<ShareholderImportRecord> records) {
        // Nhóm theo họ tên đã chuẩn hóa
        Map<String, List<ShareholderImportRecord>> groupedByName = records.stream()
                .collect(Collectors.groupingBy(r -> normalizeFullName(r.getFullName())));

        List<ShareholderImportRecord> mergedRecords = new ArrayList<>();

        for (Map.Entry<String, List<ShareholderImportRecord>> entry : groupedByName.entrySet()) {
            List<ShareholderImportRecord> group = entry.getValue();

            if (group.size() == 1) {
                // Chỉ có 1 bản ghi, giữ nguyên
                mergedRecords.add(group.get(0));
            } else {
                // Có nhiều bản ghi cùng họ tên, cần gộp lại
                System.out.println("Found " + group.size() + " records for: " + entry.getKey());

                // Tìm bản ghi có dateOfIssue mới nhất
                ShareholderImportRecord latestRecord = findLatestRecord(group);

                // Tổng số cổ phần từ tất cả các bản ghi
                Long totalShares = group.stream()
                        .mapToLong(r -> r.getShares() != null ? r.getShares() : 0L)
                        .sum();

                // Cập nhật số cổ phần tổng hợp
                latestRecord.setShares(totalShares);

                System.out.println("Merged into CCCD: " + latestRecord.getCccd() +
                        ", Total shares: " + totalShares +
                        ", DateOfIssue: " + latestRecord.getDateOfIssue());

                mergedRecords.add(latestRecord);
            }
        }

        return mergedRecords;
    }

    /**
     * Chuẩn hóa họ tên để so sánh
     */
    private static String normalizeFullName(String fullName) {
        if (fullName == null) {
            return "";
        }
        // Chuyển về lowercase, trim, loại bỏ khoảng trắng thừa
        return fullName.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Tìm bản ghi có dateOfIssue mới nhất trong nhóm
     */
    private static ShareholderImportRecord findLatestRecord(List<ShareholderImportRecord> records) {
        return records.stream()
                .max((r1, r2) -> {
                    LocalDate date1 = parseDateOfIssue(r1.getDateOfIssue());
                    LocalDate date2 = parseDateOfIssue(r2.getDateOfIssue());

                    // Nếu cả 2 đều null, coi như bằng nhau
                    if (date1 == null && date2 == null)
                        return 0;
                    // Nếu date1 null, date2 mới hơn
                    if (date1 == null)
                        return -1;
                    // Nếu date2 null, date1 mới hơn
                    if (date2 == null)
                        return 1;
                    // So sánh 2 ngày
                    return date1.compareTo(date2);
                })
                .orElse(records.get(0)); // Fallback về bản ghi đầu tiên nếu không tìm được
    }

    /**
     * Parse dateOfIssue string thành LocalDate
     */
    private static LocalDate parseDateOfIssue(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || "N/A".equals(dateStr)) {
            return null;
        }

        // Thử các định dạng phổ biến
        String[] patterns = { "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy" };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // Thử pattern tiếp theo
            }
        }

        System.err.println("Could not parse dateOfIssue: " + dateStr);
        return null;
    }
}
