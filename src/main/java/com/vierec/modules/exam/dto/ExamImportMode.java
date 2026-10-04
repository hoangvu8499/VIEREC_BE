package com.vierec.modules.exam.dto;

/** What an Excel import does with the questions the exam already has. */
public enum ExamImportMode {
    /** Keep them and add the file's questions after them. */
    APPEND,
    /** Delete them; the file becomes the whole question list. */
    REPLACE
}
