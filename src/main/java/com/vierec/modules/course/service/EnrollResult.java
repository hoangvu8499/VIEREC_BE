package com.vierec.modules.course.service;

import com.vierec.modules.course.dto.EnrollmentResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Result of an enroll call: the body plus whether a new row was created (201) or a cancelled one reused (200). */
@Getter
@AllArgsConstructor
public class EnrollResult {

    private final EnrollmentResponse body;
    private final boolean created;
}
