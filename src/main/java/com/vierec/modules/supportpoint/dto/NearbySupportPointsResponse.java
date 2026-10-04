package com.vierec.modules.supportpoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/** Support points around a location, nearest first. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NearbySupportPointsResponse")
public class NearbySupportPointsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Origin origin;

    @Schema(example = "3")
    private int radiusKm;

    private List<SupportPointResponse> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SupportPointSearchOrigin")
    public static class Origin implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(example = "15.9967")
        private double latitude;

        @Schema(example = "108.2462")
        private double longitude;

        @Schema(description = "Place the address was resolved to; omitted when searching by coordinates",
                example = "Ngũ Hành Sơn, Đà Nẵng, Việt Nam")
        private String label;
    }
}
