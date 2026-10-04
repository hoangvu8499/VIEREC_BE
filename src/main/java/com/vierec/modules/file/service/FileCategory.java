package com.vierec.modules.file.service;

/** Kind of upload; decides the allowed extensions and the size limit ({@code app.upload.*}). */
public enum FileCategory {
    DOCUMENT,
    /** Issued certificate: PDF only, document size limit. */
    CERTIFICATE
}
