package com.vierec.modules.file.service;

import org.springframework.security.core.Authentication;

/**
 * Extra download rule for files owned by another module. Every guard bean is asked before a file is streamed;
 * a guard that does not own the file does nothing.
 */
public interface FileAccessGuard {

    /** Throws when {@code authentication} may not read file {@code fileId}. */
    void checkRead(Long fileId, Authentication authentication);
}
