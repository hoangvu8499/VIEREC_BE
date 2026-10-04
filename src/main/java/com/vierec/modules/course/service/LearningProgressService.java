package com.vierec.modules.course.service;

import com.vierec.modules.course.dto.CourseProgressResponse;
import com.vierec.modules.course.dto.VideoProgressRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Video watching of learners, stored by the server so that businesses can follow it and the exam can require
 * 80% of it.
 */
public interface LearningProgressService {

    /** Share of the total video length to watch before the exam opens. */
    double EXAM_WATCH_RATIO = 0.8;

    /** The logged-in learner's progress in a course they were approved for. */
    CourseProgressResponse getMine(Long courseId, String username);

    /** Merges the reported videos into the stored progress (values only grow) and returns the new progress. */
    CourseProgressResponse saveMine(Long courseId, String username, List<VideoProgressRequest> videos);

    /** Progress of any user in a course, without access checks (business follow-up, exam start). */
    CourseProgressResponse summarize(Long userId, Long courseId);

    /** {@link #summarize} for several courses in two queries; the per-video details are left out. */
    Map<Long, CourseProgressResponse> summarizeAll(Long userId, Collection<Long> courseIds);
}
