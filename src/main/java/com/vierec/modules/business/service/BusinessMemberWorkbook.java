package com.vierec.modules.business.service;

import com.vierec.common.excel.ExcelSheets;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ErrorResponse;
import com.vierec.modules.auth.dto.RegisterRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel side of the business learner import: builds the template and reads a filled-in file.
 *
 * <p>Layout of the learner sheet: row 1 is the header, then one learner per row with the columns STT (ignored),
 * last name, first name, CCCD, date of birth, address, phone number and email. Blank rows are skipped. There is no
 * username column: the CCCD is the username, so a learner logs in with it.
 * The password is not a column: the business sends one default password for the whole file.</p>
 *
 * <p>Every row is checked with the same Bean Validation rules as {@link RegisterRequest}, plus the unique values
 * (CCCD, which is also the username, phone, email) are checked against the other rows of the file. One bad row
 * rejects the whole file, with every problem reported as {@code rows[<Excel row>].<field>} so the manager sees the
 * real row number
 * of the sheet. Conflicts with accounts already in the database are checked by the caller, which knows the
 * repository.</p>
 */
@Component
@RequiredArgsConstructor
public class BusinessMemberWorkbook {

    public static final int MAX_MEMBERS = 500;
    public static final String MEMBER_SHEET = "Học viên";

    private static final String GUIDE_SHEET = "Hướng dẫn";
    private static final String[] HEADERS = {"STT", "Họ", "Tên", "Số CCCD", "Ngày sinh", "Địa chỉ",
        "Số điện thoại", "Email"};
    /** Request field read from each column; the STT column (0) is not read. The CCCD is also the username. */
    private static final String[] FIELDS = {null, "lastName", "firstName", "cccd", "dateOfBirth", "address",
        "phoneNumber", "email"};
    private static final int[] COLUMN_WIDTHS = {6, 18, 22, 16, 14, 45, 16, 30};
    private static final int LAST_NAME_COLUMN = 1;
    private static final int FIRST_NAME_COLUMN = 2;
    private static final int CCCD_COLUMN = 3;
    private static final int BIRTH_COLUMN = 4;
    private static final int ADDRESS_COLUMN = 5;
    private static final int PHONE_COLUMN = 6;
    private static final int EMAIL_COLUMN = 7;
    private static final String TEXT_FORMAT = "@";
    /** Accepted ways of typing a date of birth when the cell is text rather than a real Excel date. */
    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("d/M/uuuu"),
        DateTimeFormatter.ofPattern("uuuu-M-d"),
        DateTimeFormatter.ofPattern("d-M-uuuu"),
        DateTimeFormatter.ofPattern("d.M.uuuu"),
    };
    private static final String[] GUIDE = {
        "Cách điền file tài khoản học viên của doanh nghiệp",
        "1. Mỗi dòng ở sheet \"" + MEMBER_SHEET + "\" là một học viên, bắt đầu từ dòng 2. Giữ nguyên dòng tiêu đề.",
        "2. Nhập đủ mọi cột trừ STT: Họ, Tên, Số CCCD, Ngày sinh, Địa chỉ, Số điện thoại, Email.",
        "3. Không có cột tên đăng nhập: học viên đăng nhập bằng chính SỐ CCCD của mình.",
        "   Mật khẩu KHÔNG nằm trong file: doanh nghiệp nhập một mật khẩu mặc định khi import, dùng cho mọi dòng;"
                + " học viên phải tự đổi sau lần đăng nhập đầu.",
        "4. Số CCCD đúng 12 chữ số và chỉ được tạo một tài khoản duy nhất: trùng với dòng khác trong file hoặc"
                + " trùng tài khoản đã có (kể cả đã xoá) đều bị báo lỗi.",
        "5. Số điện thoại và Email cũng không được trùng. Số điện thoại đúng 10 chữ số.",
        "6. Cột Số CCCD và Số điện thoại đã để dạng Text để không mất số 0 ở đầu; đừng đổi sang dạng Number.",
        "7. Ngày sinh nhập dạng ngày/tháng/năm, ví dụ 08/04/1999, và phải là ngày trong quá khứ.",
        "8. Dòng trống được bỏ qua. Tối đa " + MAX_MEMBERS + " học viên mỗi file.",
        "9. Chỉ cần một dòng sai là KHÔNG tài khoản nào được tạo; sửa đúng các dòng được báo rồi import lại.",
        "",
        "Ví dụ:",
    };
    private static final String[] EXAMPLE = {"1", "Nguyễn", "Văn An", "001099012345", "08/04/1999",
        "12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh", "0901234567", "nv.an@congty.vn"};

    private final Validator validator;

    /** One learner row of the file: the Excel row number it came from and the account to create. */
    @Getter
    @AllArgsConstructor
    public static final class ParsedMember {

        private final int excelRow;
        private final RegisterRequest request;
    }

    /** Empty learner sheet (header, text columns for CCCD and phone) plus a guide sheet with an example. */
    public byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = ExcelSheets.headerStyle(workbook);
            CellStyle wrap = workbook.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(VerticalAlignment.TOP);
            DataFormat formats = workbook.createDataFormat();
            CellStyle text = workbook.createCellStyle();
            text.setDataFormat(formats.getFormat(TEXT_FORMAT));
            text.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet sheet = workbook.createSheet(MEMBER_SHEET);
            ExcelSheets.writeRow(sheet, 0, HEADERS, header);
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.setColumnWidth(column, COLUMN_WIDTHS[column] * ExcelSheets.EXCEL_CHAR_WIDTH);
                // Leading zeros of a CCCD or a phone number survive only in a text column.
                boolean digits = column == CCCD_COLUMN || column == PHONE_COLUMN;
                sheet.setDefaultColumnStyle(column, digits ? text : wrap);
            }
            sheet.createFreezePane(0, 1);

            Sheet guide = workbook.createSheet(GUIDE_SHEET);
            guide.setColumnWidth(0, COLUMN_WIDTHS[0] * ExcelSheets.EXCEL_CHAR_WIDTH);
            for (int i = 0; i < GUIDE.length; i++) {
                guide.createRow(i).createCell(0).setCellValue(GUIDE[i]);
            }
            for (int column = 1; column < HEADERS.length; column++) {
                guide.setColumnWidth(column, COLUMN_WIDTHS[column] * ExcelSheets.EXCEL_CHAR_WIDTH);
            }
            ExcelSheets.writeRow(guide, GUIDE.length, HEADERS, header);
            ExcelSheets.writeRow(guide, GUIDE.length + 1, EXAMPLE, wrap);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // Written to memory only.
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Learners of the file in row order, each carrying {@code defaultPassword}; throws with the reason (and every
     * bad cell) when the file cannot be used.
     */
    public List<ParsedMember> read(MultipartFile file, String defaultPassword) {
        try (Workbook workbook = ExcelSheets.open(file, ErrorCode.MEMBER_IMPORT_UNREADABLE)) {
            return read(workbook, defaultPassword);
        } catch (IOException e) {
            // Only close() can get here; the rows were already read.
            throw new UncheckedIOException(e);
        }
    }

    private List<ParsedMember> read(Workbook workbook, String defaultPassword) {
        Sheet sheet = ExcelSheets.sheet(workbook, MEMBER_SHEET);
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<ParsedMember> members = new ArrayList<>();
        List<ErrorResponse.FieldViolation> violations = new ArrayList<>();
        // Unique value (lower-cased where the column is case-insensitive) -> first Excel row that used it.
        Map<String, Integer> seen = new HashMap<>();

        for (int index = 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            String[] values = new String[HEADERS.length];
            boolean blank = true;
            for (int column = 1; column < HEADERS.length; column++) {
                values[column] = ExcelSheets.text(row, column, formatter, evaluator);
                blank &= values[column].isEmpty();
            }
            if (blank) {
                continue;
            }
            if (members.size() == MAX_MEMBERS) {
                throw new BusinessException(ErrorCode.MEMBER_IMPORT_TOO_MANY_ROWS,
                        "The file has more than " + MAX_MEMBERS + " learners");
            }
            int excelRow = index + 1;
            members.add(new ParsedMember(excelRow, toRequest(excelRow, values, defaultPassword, violations)));
            checkUniqueInFile(excelRow, values, seen, violations);
        }

        if (members.isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBER_IMPORT_EMPTY);
        }
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBER_IMPORT_INVALID_ROWS, violations);
        }
        return members;
    }

    /** One row as a request; its problems are added to {@code violations} in column order. */
    private RegisterRequest toRequest(int excelRow, String[] values, String defaultPassword,
                                      List<ErrorResponse.FieldViolation> violations) {
        RegisterRequest request = new RegisterRequest();
        // The learner logs in with their CCCD: 12 digits always satisfy the username rules.
        request.setUsername(values[CCCD_COLUMN]);
        request.setLastName(values[LAST_NAME_COLUMN]);
        request.setFirstName(values[FIRST_NAME_COLUMN]);
        request.setCccd(values[CCCD_COLUMN]);
        request.setAddress(values[ADDRESS_COLUMN]);
        request.setPhoneNumber(values[PHONE_COLUMN]);
        request.setEmail(values[EMAIL_COLUMN]);
        request.setPassword(defaultPassword);

        Map<String, ErrorResponse.FieldViolation> byField = new HashMap<>();
        String birth = values[BIRTH_COLUMN];
        LocalDate dateOfBirth = parseDate(birth);
        if (dateOfBirth != null) {
            request.setDateOfBirth(dateOfBirth);
        } else if (!birth.isEmpty()) {
            byField.put("dateOfBirth", ExcelSheets.violation(excelRow, "dateOfBirth", birth,
                    "Date of birth must look like 08/04/1999"));
        }
        for (ConstraintViolation<RegisterRequest> problem : validator.validate(request)) {
            String field = problem.getPropertyPath().toString();
            Object rejected = problem.getInvalidValue() == null ? "" : problem.getInvalidValue();
            byField.putIfAbsent(field, ExcelSheets.violation(excelRow, field, rejected, problem.getMessage()));
        }
        // The password is the same for every row, so a bad one is reported once on the form field, not per row.
        byField.remove("password");
        // The username is the CCCD: a bad CCCD is already reported on its own column.
        byField.remove("username");
        for (String field : FIELDS) {
            if (field != null && byField.containsKey(field)) {
                violations.add(byField.get(field));
            }
        }
        return request;
    }

    /**
     * A value that must be unique in the {@code users} table may not repeat inside the file either; the second
     * row carrying it is the one reported.
     */
    private static void checkUniqueInFile(int excelRow, String[] values, Map<String, Integer> seen,
                                          List<ErrorResponse.FieldViolation> violations) {
        // The CCCD doubles as the username, so its check covers both unique keys.
        int[] columns = {CCCD_COLUMN, PHONE_COLUMN, EMAIL_COLUMN};
        for (int column : columns) {
            String value = values[column];
            if (value.isEmpty()) {
                continue;
            }
            // The table collates email case-insensitively, so compare it that way.
            String key = FIELDS[column] + "=" + value.toLowerCase(Locale.ROOT);
            Integer first = seen.putIfAbsent(key, excelRow);
            if (first != null) {
                violations.add(ExcelSheets.violation(excelRow, FIELDS[column], value,
                        "Already used on row " + first + " of the file"));
            }
        }
    }

    /** Null when the text is empty or in none of the accepted formats. */
    private static LocalDate parseDate(String text) {
        if (text.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // Try the next accepted format.
            }
        }
        return null;
    }
}
