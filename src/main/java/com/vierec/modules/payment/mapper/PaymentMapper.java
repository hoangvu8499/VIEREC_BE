package com.vierec.modules.payment.mapper;

import com.vierec.modules.payment.dto.PaymentResponse;
import com.vierec.modules.payment.entity.Payment;
import com.vierec.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Requests are not mapped here: the service sets every entity field explicitly. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "payerName", expression = "java(fullName(payment.getUser()))")
    @Mapping(target = "payerEmail", source = "user.email")
    @Mapping(target = "payerPhone", source = "user.phoneNumber")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    PaymentResponse toResponse(Payment payment);

    /** Vietnamese order: last name (họ) then first name (tên). Takes a User, so MapStruct never applies it alone. */
    default String fullName(User user) {
        return user == null ? null : (user.getLastName() + " " + user.getFirstName()).trim();
    }
}
