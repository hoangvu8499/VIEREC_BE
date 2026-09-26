package com.vierec.modules.course.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.course.dto.CourseDetailResponse;
import com.vierec.modules.course.dto.CourseResponse;
import com.vierec.modules.course.dto.CourseRequest;
import com.vierec.modules.course.entity.CourseStatus;

public interface CourseService {

    /** Fixed page size of the course list. */
    int PAGE_SIZE = 10;

    /**
     * One page of {@link #PAGE_SIZE} courses, newest first.
     *
     * @param page          0-based page number
     * @param publishedOnly true for users who may only see {@code PUBLISHED} courses; {@code status} is ignored
     */
    PageResponse<CourseResponse> list(String keyword, CourseStatus status, int page, boolean publishedOnly);

    /**
     * Course with its lessons.
     *
     * @param publishedOnly true for users who may only see {@code PUBLISHED} courses; other courses are not found
     */
    CourseDetailResponse get(Long id, boolean publishedOnly);

    CourseResponse create(CourseRequest request, String creatorUsername);

    CourseResponse update(Long id, CourseRequest request);

    /** Soft delete: sets {@code deleted_at}. Lessons and files are kept but unreachable through the course. */
    void delete(Long id);
}
