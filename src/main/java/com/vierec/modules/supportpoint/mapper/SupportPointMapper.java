package com.vierec.modules.supportpoint.mapper;

import com.vierec.modules.supportpoint.dto.SupportPointDetailResponse;
import com.vierec.modules.supportpoint.dto.SupportPointRequest;
import com.vierec.modules.supportpoint.dto.SupportPointResponse;
import com.vierec.modules.supportpoint.entity.SupportPoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupportPointMapper {

    @Mapping(target = "distanceKm", source = "distanceKm")
    SupportPointResponse toResponse(SupportPoint point, double distanceKm);

    SupportPointDetailResponse toDetail(SupportPoint point);

    /** Copies the text fields; the coordinates are resolved (and rounded) by the service. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SupportPointRequest request, @MappingTarget SupportPoint point);
}
