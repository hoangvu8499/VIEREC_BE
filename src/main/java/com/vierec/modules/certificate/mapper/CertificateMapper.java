package com.vierec.modules.certificate.mapper;

import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.modules.certificate.dto.CertificateVerificationResponse;
import com.vierec.modules.certificate.entity.Certificate;
import com.vierec.modules.file.service.FileStorageService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = FileStorageService.class)
public interface CertificateMapper {

    /** Number of CCCD digits left visible at the start and at the end of the masked value. */
    int CCCD_VISIBLE_HEAD = 4;
    int CCCD_VISIBLE_TAIL = 2;

    @Mapping(target = "enrollmentId", source = "enrollment.id")
    @Mapping(target = "courseId", source = "enrollment.course.id")
    @Mapping(target = "userId", source = "enrollment.user.id")
    @Mapping(target = "fileUrl", expression = "java(FileStorageService.downloadUrl(certificate.getFile().getId()))")
    CertificateResponse toResponse(Certificate certificate);

    @Mapping(target = "cccdMasked", expression = "java(maskCccd(certificate.getCccd()))")
    CertificateVerificationResponse toVerification(Certificate certificate);

    /** {@code @Named} so that MapStruct does not apply it to every String field of this mapper. */
    @Named("maskCccd")
    default String maskCccd(String cccd) {
        if (cccd == null || cccd.length() <= CCCD_VISIBLE_HEAD + CCCD_VISIBLE_TAIL) {
            return cccd == null ? null : "******";
        }
        StringBuilder masked = new StringBuilder(cccd.substring(0, CCCD_VISIBLE_HEAD));
        for (int i = CCCD_VISIBLE_HEAD; i < cccd.length() - CCCD_VISIBLE_TAIL; i++) {
            masked.append('*');
        }
        return masked.append(cccd.substring(cccd.length() - CCCD_VISIBLE_TAIL)).toString();
    }
}
