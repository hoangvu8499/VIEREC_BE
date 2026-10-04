package com.vierec.modules.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BusinessMemberDetailResponse")
public class BusinessMemberDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private BusinessMemberResponse member;

    @Schema(description = "Every enrollment of the learner (CANCELLED included), newest first")
    private List<MemberCourseResponse> courses;
}
