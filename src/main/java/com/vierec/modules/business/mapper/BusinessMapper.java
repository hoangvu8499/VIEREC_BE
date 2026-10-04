package com.vierec.modules.business.mapper;

import com.vierec.modules.business.dto.BusinessMemberResponse;
import com.vierec.modules.business.dto.BusinessResponse;
import com.vierec.modules.business.dto.MemberCourseResponse;
import com.vierec.modules.business.entity.Business;
import com.vierec.modules.certificate.entity.Certificate;
import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.exam.entity.ExamAttempt;
import com.vierec.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Business entity to DTO conversions; the counts and the course details are computed by the service. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BusinessMapper {

    @Mapping(target = "id", source = "business.id")
    @Mapping(target = "createdAt", source = "business.createdAt")
    @Mapping(target = "updatedAt", source = "business.updatedAt")
    BusinessResponse toResponse(Business business, long managerCount, long memberCount);

    @Mapping(target = "fullName", expression = "java((user.getLastName() + \" \" + user.getFirstName()).trim())")
    BusinessMemberResponse toMember(User user);

    @Mapping(target = "enrollmentId", source = "id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.name")
    @Mapping(target = "progress", ignore = true)
    @Mapping(target = "exam", ignore = true)
    @Mapping(target = "certificate", ignore = true)
    MemberCourseResponse toCourse(CourseEnrollment enrollment);

    @Mapping(target = "submitted", expression = "java(attempt.isSubmitted())")
    MemberCourseResponse.Exam toExam(ExamAttempt attempt);

    MemberCourseResponse.Certificate toCertificate(Certificate certificate);
}
