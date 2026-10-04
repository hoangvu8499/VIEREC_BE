package com.vierec.modules.supportpoint.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Coordinates an address was resolved to. */
@Getter
@AllArgsConstructor
public class GeocodedPlace {

    private final double latitude;
    private final double longitude;
    /** Full name of the place, e.g. "Ngũ Hành Sơn, Đà Nẵng, Việt Nam". */
    private final String label;
}
