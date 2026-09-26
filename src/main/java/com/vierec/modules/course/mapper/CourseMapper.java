package com.vierec.modules.course.mapper;

import com.vierec.modules.course.dto.CourseDetailResponse;
import com.vierec.modules.course.dto.CourseResponse;
import com.vierec.modules.course.dto.LessonResponse;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.Lesson;
import com.vierec.modules.course.entity.LessonFile;
import com.vierec.modules.file.service.FileStorageService;
import com.vierec.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Course / lesson entity to DTO conversions. Implementation is generated at compile time by MapStruct.
 * Requests are not mapped here: the services set every entity field explicitly.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = FileStorageService.class)
public interface CourseMapper {

    @Mapping(target = "instructorId", source = "course.instructor.id")
    @Mapping(target = "instructorUsername", source = "course.instructor.username")
    @Mapping(target = "instructorName", expression = "java(fullName(course.getInstructor()))")
    @Mapping(target = "createdByUsername", source = "course.createdBy.username")
    @Mapping(target = "lessonCount", source = "lessonCount")
    CourseResponse toResponse(Course course, long lessonCount);

    @Mapping(target = "instructorId", source = "course.instructor.id")
    @Mapping(target = "instructorUsername", source = "course.instructor.username")
    @Mapping(target = "instructorName", expression = "java(fullName(course.getInstructor()))")
    @Mapping(target = "createdByUsername", source = "course.createdBy.username")
    @Mapping(target = "lessons", source = "lessons")
    CourseDetailResponse toDetailResponse(Course course, List<Lesson> lessons);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "files", source = "lessonFiles")
    LessonResponse toResponse(Lesson lesson);

    @Mapping(target = "fileId", source = "file.id")
    @Mapping(target = "originalName", source = "file.originalName")
    @Mapping(target = "contentType", source = "file.contentType")
    @Mapping(target = "sizeBytes", source = "file.sizeBytes")
    @Mapping(target = "url", expression = "java(FileStorageService.downloadUrl(lessonFile.getFile().getId()))")
    LessonResponse.FileItem toFileItem(LessonFile lessonFile);

    /** Vietnamese order: last name (họ) then first name (tên). */
    default String fullName(User user) {
        return user == null ? null : (user.getLastName() + " " + user.getFirstName()).trim();
    }
}
