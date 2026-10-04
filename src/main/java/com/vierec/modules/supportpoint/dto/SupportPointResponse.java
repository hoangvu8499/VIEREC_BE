package com.vierec.modules.supportpoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** A support point and how far it is from the searched location. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SupportPointResponse")
public class SupportPointResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Đơn vị xử lý sự cố số 1")
    private String name;

    @Schema(example = "Đà Nẵng")
    private String province;

    @Schema(example = "Ngũ Hành Sơn")
    private String district;

    @Schema(description = "Street address; omitted when unknown")
    private String address;

    @Schema(example = "15.994224")
    private double latitude;

    @Schema(example = "108.237739")
    private double longitude;

    @Schema(example = "0905123456")
    private String phoneNumber;

    @Schema(description = "Straight-line distance from the searched location (km, 3 decimals)", example = "1.254")
    private double distanceKm;
}
