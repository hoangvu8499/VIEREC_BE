package com.vierec.modules.supportpoint.mapper;

import com.vierec.modules.supportpoint.dto.SupportPointResponse;
import com.vierec.modules.supportpoint.entity.SupportPoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupportPointMapper {

    @Mapping(target = "distanceKm", source = "distanceKm")
    SupportPointResponse toResponse(SupportPoint point, double distanceKm);
}
