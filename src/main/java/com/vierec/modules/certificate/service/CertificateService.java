package com.vierec.modules.certificate.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.modules.certificate.dto.CertificateVerificationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public interface CertificateService {

    /** Codes are stored upper case; a code typed or read off paper may not be. */
    static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Issues the certificate of a COMPLETED enrollment with the PDF prepared by the admin. The trainee's name, date
     * of birth and CCCD and the course name are copied into the certificate.
     */
    CertificateResponse issue(Long courseId, Long enrollmentId, MultipartFile pdf, String actorUsername);

    /** Certificates of the caller, newest first. */
    PageResponse<CertificateResponse> listMine(String username, int page, int size);

    /** One certificate of the caller by its code; {@code CERTIFICATE_NOT_FOUND} for someone else's. */
    CertificateResponse getMine(String username, String code);

    /** Public lookup by code, with the CCCD masked and no date of birth or file. */
    CertificateVerificationResponse verify(String code);
}
