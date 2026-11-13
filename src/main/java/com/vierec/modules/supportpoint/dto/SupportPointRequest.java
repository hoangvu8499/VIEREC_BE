package com.vierec.modules.supportpoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Body of {@code POST /api/v1/support-points} and {@code PUT /api/v1/support-points/{id}}, also the rule set every
 * row of the Excel import is checked with. Coordinates are optional here: without them the server looks the place up
 * from the address, district and province (the import requires them, see {@code SupportPointWorkbook}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SupportPointRequest")
public class SupportPointRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int NAME_MAX = 255;
    public static final int AREA_MAX = 100;
    public static final int ADDRESS_MAX = 500;
    /** 3 to 20 characters: digits, optionally a leading +, with spaces, dots or dashes between them (e.g. 114). */
    public static final String PHONE_REGEX = "^\\+?[0-9](?:[0-9 .-]{0,18}[0-9])?$";

    @NotBlank(message = "{supportPoint.name.required}")
    @Size(max = NAME_MAX, message = "{supportPoint.name.size}")
    @Schema(example = "Đơn vị xử lý sự cố số 1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "{supportPoint.province.required}")
    @Size(max = AREA_MAX, message = "{supportPoint.province.size}")
    @Schema(example = "Đà Nẵng", requiredMode = Schema.RequiredMode.REQUIRED)
    private String province;

    @NotBlank(message = "{supportPoint.district.required}")
    @Size(max = AREA_MAX, message = "{supportPoint.district.size}")
    @Schema(example = "Ngũ Hành Sơn", requiredMode = Schema.RequiredMode.REQUIRED)
    private String district;

    @Size(max = ADDRESS_MAX, message = "{supportPoint.address.size}")
    @Schema(example = "12 Lê Văn Hiến", description = "Street address, optional")
    private String address;

    @NotBlank(message = "{supportPoint.phone.required}")
    @Pattern(regexp = PHONE_REGEX, message = "{supportPoint.phone.invalid}")
    @Schema(example = "0905123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @DecimalMin(value = "-90", message = "{supportPoint.latitude.range}")
    @DecimalMax(value = "90", message = "{supportPoint.latitude.range}")
    @Schema(example = "15.994224", description = "Give both coordinates, or neither to look them up from the address")
    private Double latitude;

    @DecimalMin(value = "-180", message = "{supportPoint.longitude.range}")
    @DecimalMax(value = "180", message = "{supportPoint.longitude.range}")
    @Schema(example = "108.237739")
    private Double longitude;
}
