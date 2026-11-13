package com.vierec.modules.supportpoint.service;

import com.vierec.common.excel.ExcelSheets;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ErrorResponse;
import com.vierec.modules.supportpoint.dto.SupportPointRequest;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel side of the support point import: builds the template and reads a filled-in file.
 *
 * <p>Layout of the sheet: row 1 is the header, then one support point per row with the columns STT (ignored),
 * name, province, district, street address (optional), phone number, latitude and longitude. Blank rows are
 * skipped. Unlike the form, the import needs the coordinates: looking up hundreds of addresses would take minutes
 * (OpenStreetMap allows one lookup per second).</p>
 *
 * <p>Every row is checked with the same Bean Validation rules as {@link SupportPointRequest}; one bad row rejects
 * the whole file, with every problem reported as {@code rows[<Excel row>].<field>}.</p>
 */
@Component
@RequiredArgsConstructor
public class SupportPointWorkbook {

    public static final int MAX_POINTS = 500;
    public static final String POINT_SHEET = "Điểm hỗ trợ";

    private static final String GUIDE_SHEET = "Hướng dẫn";
    private static final String[] HEADERS = {"STT", "Tên đơn vị", "Tỉnh / Thành phố", "Quận / Huyện",
        "Địa chỉ chi tiết", "Số điện thoại", "Vĩ độ", "Kinh độ"};
    /** Request field read from each column; the STT column (0) is not read. */
    private static final String[] FIELDS = {null, "name", "province", "district", "address", "phoneNumber",
        "latitude", "longitude"};
    private static final int[] COLUMN_WIDTHS = {6, 36, 18, 18, 40, 16, 14, 14};
    private static final int NAME_COLUMN = 1;
    private static final int PROVINCE_COLUMN = 2;
    private static final int DISTRICT_COLUMN = 3;
    private static final int ADDRESS_COLUMN = 4;
    private static final int PHONE_COLUMN = 5;
    private static final int LATITUDE_COLUMN = 6;
    private static final int LONGITUDE_COLUMN = 7;
    private static final String TEXT_FORMAT = "@";
    private static final String[] GUIDE = {
        "Cách điền file điểm hỗ trợ sự cố",
        "1. Mỗi dòng ở sheet \"" + POINT_SHEET + "\" là một điểm hỗ trợ, bắt đầu từ dòng 2. Giữ nguyên dòng tiêu đề.",
        "2. Bắt buộc: Tên đơn vị, Tỉnh / Thành phố, Quận / Huyện, Số điện thoại, Vĩ độ, Kinh độ."
                + " Địa chỉ chi tiết có thể bỏ trống.",
        "3. Lấy toạ độ trên Google Maps: chuột phải vào vị trí, bấm vào dòng số đầu tiên (vd. 15.994224, 108.237739)"
                + " để sao chép; số trước dấu phẩy là Vĩ độ, số sau là Kinh độ.",
        "4. Vĩ độ từ -90 đến 90, Kinh độ từ -180 đến 180; dùng dấu chấm cho phần thập phân. Ở Việt Nam vĩ độ"
                + " khoảng 8 đến 24, kinh độ khoảng 102 đến 110.",
        "5. Số điện thoại 3 đến 20 chữ số, có thể có dấu cách, dấu chấm, gạch ngang (vd. 114, 0236 3822 114).",
        "6. Dòng trống được bỏ qua. Tối đa " + MAX_POINTS + " điểm mỗi file. Các điểm trong file được THÊM vào"
                + " danh sách hiện có.",
        "7. Chỉ cần một dòng sai là KHÔNG điểm nào được lưu; sửa đúng các dòng được báo rồi import lại.",
        "",
        "Ví dụ:",
    };
    private static final String[] EXAMPLE = {"1", "Đội PCCC và CNCH quận Ngũ Hành Sơn", "Đà Nẵng", "Ngũ Hành Sơn",
        "12 Lê Văn Hiến", "0236 3822 114", "15.994224", "108.237739"};

    private final Validator validator;

    /** One row of the file: the Excel row number it came from and the support point to create. */
    @Getter
    @AllArgsConstructor
    public static final class ParsedPoint {

        private final int excelRow;
        private final SupportPointRequest request;
    }

    /** Empty sheet (header, phone and coordinates as text) plus a guide sheet with an example. */
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

            Sheet sheet = workbook.createSheet(POINT_SHEET);
            ExcelSheets.writeRow(sheet, 0, HEADERS, header);
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.setColumnWidth(column, COLUMN_WIDTHS[column] * ExcelSheets.EXCEL_CHAR_WIDTH);
                // A leading zero of a phone number survives only in a text column.
                sheet.setDefaultColumnStyle(column, column == PHONE_COLUMN ? text : wrap);
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

    /** Support points of the file in row order; throws with the reason (and every bad cell) when it cannot be used. */
    public List<ParsedPoint> read(MultipartFile file) {
        try (Workbook workbook = ExcelSheets.open(file, ErrorCode.SUPPORT_POINT_IMPORT_UNREADABLE)) {
            return read(workbook);
        } catch (IOException e) {
            // Only close() can get here; the rows were already read.
            throw new UncheckedIOException(e);
        }
    }

    private List<ParsedPoint> read(Workbook workbook) {
        Sheet sheet = ExcelSheets.sheet(workbook, POINT_SHEET);
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<ParsedPoint> points = new ArrayList<>();
        List<ErrorResponse.FieldViolation> violations = new ArrayList<>();

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
            if (points.size() == MAX_POINTS) {
                throw new BusinessException(ErrorCode.SUPPORT_POINT_IMPORT_TOO_MANY_ROWS,
                        "The file has more than " + MAX_POINTS + " support points");
            }
            int excelRow = index + 1;
            points.add(new ParsedPoint(excelRow, toRequest(excelRow, values, violations)));
        }

        if (points.isEmpty()) {
            throw new BusinessException(ErrorCode.SUPPORT_POINT_IMPORT_EMPTY);
        }
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.SUPPORT_POINT_IMPORT_INVALID_ROWS, violations);
        }
        return points;
    }

    /** One row as a request; its problems are added to {@code violations} in column order. */
    private SupportPointRequest toRequest(int excelRow, String[] values,
                                          List<ErrorResponse.FieldViolation> violations) {
        SupportPointRequest request = new SupportPointRequest();
        request.setName(values[NAME_COLUMN]);
        request.setProvince(values[PROVINCE_COLUMN]);
        request.setDistrict(values[DISTRICT_COLUMN]);
        request.setAddress(values[ADDRESS_COLUMN].isEmpty() ? null : values[ADDRESS_COLUMN]);
        request.setPhoneNumber(values[PHONE_COLUMN]);

        Map<String, ErrorResponse.FieldViolation> byField = new HashMap<>();
        request.setLatitude(coordinate(excelRow, "latitude", values[LATITUDE_COLUMN], "Latitude is required",
                byField));
        request.setLongitude(coordinate(excelRow, "longitude", values[LONGITUDE_COLUMN], "Longitude is required",
                byField));
        for (ConstraintViolation<SupportPointRequest> problem : validator.validate(request)) {
            String field = problem.getPropertyPath().toString();
            Object rejected = problem.getInvalidValue() == null ? "" : problem.getInvalidValue();
            // Echo a coordinate as typed ("95"), not as the parsed number ("95.0").
            if ("latitude".equals(field)) {
                rejected = values[LATITUDE_COLUMN];
            } else if ("longitude".equals(field)) {
                rejected = values[LONGITUDE_COLUMN];
            }
            byField.putIfAbsent(field, ExcelSheets.violation(excelRow, field, rejected, problem.getMessage()));
        }
        for (String field : FIELDS) {
            if (field != null && byField.containsKey(field)) {
                violations.add(byField.get(field));
            }
        }
        return request;
    }

    /**
     * A coordinate cell as a number; {@code null} (with the problem recorded) when it is empty or not a number.
     * A Vietnamese Excel shows decimals with a comma, so a single comma is read as the decimal point.
     */
    private static Double coordinate(int excelRow, String field, String text, String requiredMessage,
                                     Map<String, ErrorResponse.FieldViolation> byField) {
        if (text.isEmpty()) {
            byField.put(field, ExcelSheets.violation(excelRow, field, text, requiredMessage));
            return null;
        }
        String normalized = text.indexOf('.') < 0 && text.indexOf(',') == text.lastIndexOf(',')
                ? text.replace(',', '.') : text;
        try {
            return Double.valueOf(normalized);
        } catch (NumberFormatException e) {
            byField.put(field, ExcelSheets.violation(excelRow, field, text,
                    "Coordinate must be a number like 15.994224"));
            return null;
        }
    }
}
