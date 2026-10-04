package com.vierec.modules.exam.service;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ErrorResponse;
import com.vierec.modules.exam.dto.ExamQuestionRequest;
import com.vierec.modules.exam.entity.ExamOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel side of the question import: builds the template and reads a filled-in file.
 *
 * <p>Layout of the question sheet: row 1 is the header, then one question per row with the columns
 * STT (ignored), question, options A to D and the correct option (A, B, C or D). Blank rows are skipped.
 * Every row is checked with the same Bean Validation rules as {@link ExamQuestionRequest}; one bad row
 * rejects the whole file, with every problem reported as {@code rows[<Excel row>].<field>}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamQuestionWorkbook {

    public static final int MAX_QUESTIONS = 500;
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    public static final String QUESTION_SHEET = "Câu hỏi";

    private static final String GUIDE_SHEET = "Hướng dẫn";
    private static final String[] HEADERS = {"STT", "Câu hỏi", "Đáp án A", "Đáp án B", "Đáp án C", "Đáp án D",
        "Đáp án đúng"};
    /** Request field read from each column; the STT column (0) is not read. */
    private static final String[] FIELDS = {null, "content", "optionA", "optionB", "optionC", "optionD",
        "correctOption"};
    private static final int[] COLUMN_WIDTHS = {6, 60, 30, 30, 30, 30, 14};
    private static final int CORRECT_COLUMN = 6;
    private static final int EXCEL_CHAR_WIDTH = 256;
    /** Rejected values are echoed back so the admin recognises the cell; long texts are cut. */
    private static final int REJECTED_VALUE_MAX = 100;
    private static final String[] GUIDE = {
        "Cách điền file câu hỏi bài thi chứng chỉ",
        "1. Mỗi dòng ở sheet \"" + QUESTION_SHEET + "\" là một câu hỏi, bắt đầu từ dòng 2. Giữ nguyên dòng tiêu đề.",
        "2. Nhập đủ: Câu hỏi, 4 đáp án A, B, C, D và Đáp án đúng (một chữ A, B, C hoặc D).",
        "3. Cột STT chỉ để dễ theo dõi, có thể bỏ trống. Thứ tự câu trong bài thi là thứ tự dòng.",
        "4. Dòng trống được bỏ qua. Tối đa " + MAX_QUESTIONS + " câu mỗi file; câu hỏi tối đa "
                + ExamQuestionRequest.CONTENT_MAX + " ký tự, mỗi đáp án tối đa " + ExamQuestionRequest.OPTION_MAX
                + " ký tự.",
        "5. Khi import chọn \"Thêm vào cuối\" (giữ câu đã có) hoặc \"Thay toàn bộ\" (xoá câu đã có).",
        "   File có dòng sai thì không câu nào được lưu; sửa các dòng được báo rồi import lại.",
        "6. Điểm thang 10 = số câu đúng / tổng số câu x 10.",
        "",
        "Ví dụ:",
    };
    private static final String[] EXAMPLE = {"1", "Khi phát hiện rò rỉ khí gas, việc đầu tiên cần làm là gì?",
        "Bật đèn để kiểm tra", "Khoá van gas và mở cửa thông gió", "Gọi điện thoại ngay trong phòng",
        "Tiếp tục nấu ăn", "B"};

    private final Validator validator;

    /** Empty question sheet (header, drop-down for the correct option) plus a guide sheet with an example. */
    public byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            CellStyle wrap = workbook.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet sheet = workbook.createSheet(QUESTION_SHEET);
            writeRow(sheet, 0, HEADERS, header);
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.setColumnWidth(column, COLUMN_WIDTHS[column] * EXCEL_CHAR_WIDTH);
                sheet.setDefaultColumnStyle(column, wrap);
            }
            sheet.createFreezePane(0, 1);
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidation answer = helper.createValidation(
                    helper.createExplicitListConstraint(names(ExamOption.values())),
                    new CellRangeAddressList(1, MAX_QUESTIONS, CORRECT_COLUMN, CORRECT_COLUMN));
            answer.setShowErrorBox(true);
            answer.createErrorBox("Đáp án đúng", "Chọn A, B, C hoặc D.");
            sheet.addValidationData(answer);

            Sheet guide = workbook.createSheet(GUIDE_SHEET);
            guide.setColumnWidth(0, COLUMN_WIDTHS[0] * EXCEL_CHAR_WIDTH);
            for (int i = 0; i < GUIDE.length; i++) {
                guide.createRow(i).createCell(0).setCellValue(GUIDE[i]);
            }
            for (int column = 1; column < HEADERS.length; column++) {
                guide.setColumnWidth(column, COLUMN_WIDTHS[column] * EXCEL_CHAR_WIDTH);
            }
            writeRow(guide, GUIDE.length, HEADERS, header);
            writeRow(guide, GUIDE.length + 1, EXAMPLE, wrap);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // Written to memory only.
            throw new UncheckedIOException(e);
        }
    }

    /** Questions of the file in row order; throws with the reason (and every bad cell) when it cannot be used. */
    public List<ExamQuestionRequest> read(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "Only .xlsx and .xls files are accepted");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "The file must be at most 5 MB");
        }
        try (Workbook workbook = open(file)) {
            return read(workbook);
        } catch (IOException e) {
            // Only close() can get here; the questions were already read.
            throw new UncheckedIOException(e);
        }
    }

    private static Workbook open(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return WorkbookFactory.create(in);
        } catch (IOException | RuntimeException e) {
            log.warn("Unreadable exam question file '{}': {}", file.getOriginalFilename(), e.toString());
            throw new BusinessException(ErrorCode.EXAM_IMPORT_UNREADABLE);
        }
    }

    private List<ExamQuestionRequest> read(Workbook workbook) {
        Sheet sheet = workbook.getSheet(QUESTION_SHEET);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<ExamQuestionRequest> questions = new ArrayList<>();
        List<ErrorResponse.FieldViolation> violations = new ArrayList<>();

        for (int index = 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            String[] values = new String[HEADERS.length];
            boolean blank = true;
            for (int column = 1; column < HEADERS.length; column++) {
                values[column] = text(row, column, formatter, evaluator);
                blank &= values[column].isEmpty();
            }
            if (blank) {
                continue;
            }
            if (questions.size() == MAX_QUESTIONS) {
                throw new BusinessException(ErrorCode.EXAM_IMPORT_TOO_MANY_ROWS,
                        "The file has more than " + MAX_QUESTIONS + " questions");
            }
            questions.add(toRequest(index + 1, values, violations));
        }

        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.EXAM_IMPORT_EMPTY);
        }
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.EXAM_IMPORT_INVALID_ROWS, violations);
        }
        return questions;
    }

    /** One row as a request; its problems are added to {@code violations} in column order. */
    private ExamQuestionRequest toRequest(int excelRow, String[] values,
                                          List<ErrorResponse.FieldViolation> violations) {
        ExamQuestionRequest request = new ExamQuestionRequest();
        request.setContent(values[1]);
        request.setOptionA(values[2]);
        request.setOptionB(values[3]);
        request.setOptionC(values[4]);
        request.setOptionD(values[5]);

        Map<String, ErrorResponse.FieldViolation> byField = new HashMap<>();
        String correct = values[CORRECT_COLUMN].toUpperCase(Locale.ROOT);
        if (Arrays.asList(names(ExamOption.values())).contains(correct)) {
            request.setCorrectOption(ExamOption.valueOf(correct));
        } else if (!correct.isEmpty()) {
            byField.put("correctOption", violation(excelRow, "correctOption", values[CORRECT_COLUMN],
                    "Correct option must be A, B, C or D"));
        }
        for (ConstraintViolation<ExamQuestionRequest> problem : validator.validate(request)) {
            String field = problem.getPropertyPath().toString();
            Object rejected = problem.getInvalidValue() == null ? "" : problem.getInvalidValue();
            byField.putIfAbsent(field, violation(excelRow, field, rejected, problem.getMessage()));
        }
        for (String field : FIELDS) {
            if (field != null && byField.containsKey(field)) {
                violations.add(byField.get(field));
            }
        }
        return request;
    }

    private static ErrorResponse.FieldViolation violation(int excelRow, String field, Object rejected,
                                                          String message) {
        String value = String.valueOf(rejected);
        return ErrorResponse.FieldViolation.builder()
                .field("rows[" + excelRow + "]." + field)
                .rejectedValue(value.length() > REJECTED_VALUE_MAX ? value.substring(0, REJECTED_VALUE_MAX) + "…"
                        : value)
                .message(message)
                .build();
    }

    /** Text shown in the cell (numbers as typed, formulas evaluated), trimmed; empty for a missing cell. */
    private static String text(Row row, int column, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row == null ? null : row.getCell(column);
        if (cell == null) {
            return "";
        }
        try {
            return formatter.formatCellValue(cell, evaluator).trim();
        } catch (RuntimeException e) {
            // A formula POI cannot evaluate: fall back to its text.
            return formatter.formatCellValue(cell).trim();
        }
    }

    private static void writeRow(Sheet sheet, int index, String[] values, CellStyle style) {
        Row row = sheet.createRow(index);
        for (int column = 0; column < values.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(values[column]);
            cell.setCellStyle(style);
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
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

    private static String[] names(ExamOption[] options) {
        String[] names = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            names[i] = options[i].name();
        }
        return names;
    }
}
