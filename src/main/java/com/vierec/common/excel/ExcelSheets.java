package com.vierec.common.excel;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Helpers shared by the Excel imports that report problems as {@code rows[<Excel row>].<field>}: opening the
 * uploaded file, reading a cell as the text the user sees, and styling the downloadable templates.
 */
@Slf4j
public final class ExcelSheets {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    /** A real Excel date cell is read back in this format. */
    public static final DateTimeFormatter DATE_TEXT = DateTimeFormatter.ofPattern("d/M/uuuu");
    public static final int EXCEL_CHAR_WIDTH = 256;

    /** Rejected values are echoed back so the user recognises the cell; long texts are cut. */
    private static final int REJECTED_VALUE_MAX = 100;

    private ExcelSheets() {
    }

    /**
     * Opens an uploaded .xlsx / .xls file of at most 5 MB; the caller closes it.
     *
     * @param unreadable code thrown when the bytes are not a workbook (corrupt, password protected, not Excel)
     */
    public static Workbook open(MultipartFile file, ErrorCode unreadable) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "Only .xlsx and .xls files are accepted");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "The file must be at most 5 MB");
        }
        try (InputStream in = file.getInputStream()) {
            return WorkbookFactory.create(in);
        } catch (IOException | RuntimeException e) {
            log.warn("Unreadable Excel file '{}': {}", file.getOriginalFilename(), e.toString());
            throw new BusinessException(unreadable);
        }
    }

    /** The named sheet, or the first one when the user renamed it. */
    public static Sheet sheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        return sheet == null ? workbook.getSheetAt(0) : sheet;
    }

    /**
     * Text of one cell, trimmed; empty for a missing cell. A real Excel date becomes {@code d/M/uuuu} and a number
     * its plain digits, so that a CCCD, a phone number or a coordinate typed as a number is not read in scientific
     * notation or with a lost leading zero.
     */
    public static String text(Row row, int column, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row == null ? null : row.getCell(column);
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? evaluate(cell, evaluator) : cell.getCellType();
        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_TEXT);
            }
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        try {
            return formatter.formatCellValue(cell, evaluator).trim();
        } catch (RuntimeException e) {
            // A formula POI cannot evaluate: fall back to its text.
            return formatter.formatCellValue(cell).trim();
        }
    }

    public static ErrorResponse.FieldViolation violation(int excelRow, String field, Object rejected,
                                                         String message) {
        String value = rejected == null ? "" : String.valueOf(rejected);
        return ErrorResponse.FieldViolation.builder()
                .field("rows[" + excelRow + "]." + field)
                .rejectedValue(value.length() > REJECTED_VALUE_MAX ? value.substring(0, REJECTED_VALUE_MAX) + "…"
                        : value)
                .message(message)
                .build();
    }

    public static void writeRow(Sheet sheet, int index, String[] values, CellStyle style) {
        Row row = sheet.createRow(index);
        for (int column = 0; column < values.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(values[column]);
            cell.setCellStyle(style);
        }
    }

    /** White bold text on dark blue, like every template of the application. */
    public static CellStyle headerStyle(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        bold.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellType evaluate(Cell cell, FormulaEvaluator evaluator) {
        try {
            return evaluator.evaluateFormulaCell(cell);
        } catch (RuntimeException e) {
            // Unevaluatable formula: treat it as text and let validation reject the cell.
            return CellType.STRING;
        }
    }
}
