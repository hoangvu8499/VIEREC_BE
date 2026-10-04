package com.vierec.modules.business.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.business.dto.BusinessEnrollRequest;
import com.vierec.modules.business.dto.BusinessEnrollResponse;
import com.vierec.modules.business.dto.BusinessManagerRequest;
import com.vierec.modules.business.dto.BusinessMemberDetailResponse;
import com.vierec.modules.business.dto.BusinessMemberResponse;
import com.vierec.modules.business.dto.BusinessRequest;
import com.vierec.modules.business.dto.BusinessResponse;
import com.vierec.modules.business.dto.CreateBusinessRequest;
import com.vierec.modules.business.entity.BusinessStatus;
import com.vierec.modules.user.dto.UserResponse;

import java.util.List;

/**
 * Corporate customers. Admins manage the businesses and their manager accounts; a manager (role BUSINESS) creates,
 * enrolls and follows the learners of their own business only.
 */
public interface BusinessService {

    /** Certificates of the learners of the caller's business, newest first; keyword on code, name or course. */
    PageResponse<CertificateResponse> myCertificates(String username, String keyword, int page, int size);

    /** One certificate of a learner of the caller's business; {@code CERTIFICATE_NOT_FOUND} otherwise. */
    CertificateResponse myCertificate(String username, String code);

    // ---------------------------------------------------------------- admin

    PageResponse<BusinessResponse> search(String keyword, BusinessStatus status, int page, int size);

    /** The business and its first manager account, in one transaction. */
    BusinessResponse create(CreateBusinessRequest request, String actorUsername);

    BusinessResponse get(Long id);

    BusinessResponse update(Long id, BusinessRequest request);

    List<UserResponse> managers(Long id);

    UserResponse addManager(Long id, BusinessManagerRequest request, String actorUsername);

    PageResponse<BusinessMemberResponse> members(Long id, String keyword, int page, int size);

    BusinessMemberDetailResponse member(Long id, Long userId);

    // ---------------------------------------------------------------- business manager

    /**
     * The business of the logged-in manager. Throws {@code NOT_A_BUSINESS_MANAGER} for an account without a
     * business and {@code BUSINESS_INACTIVE} once the business is deactivated.
     */
    BusinessResponse mine(String username);

    PageResponse<BusinessMemberResponse> myMembers(String username, String keyword, int page, int size);

    /** Creates a learner account (role TRAINEE) in the manager's business. */
    BusinessMemberResponse createMember(String username, RegisterRequest request);

    BusinessMemberDetailResponse myMember(String username, Long userId);

    /**
     * PENDING enrollments of learners of the business in one course; learners already waiting for, learning or
     * having completed it are skipped. An admin approves them once the transfer is received.
     */
    BusinessEnrollResponse enroll(String username, BusinessEnrollRequest request);
}
