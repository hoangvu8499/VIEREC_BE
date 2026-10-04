package com.vierec.infrastructure.mail;

/** Plain-text emails to users (password reset codes...). */
public interface EmailService {

    /** Sends the email; returns false when it could not be sent (the error is logged). */
    boolean send(String to, String subject, String text);
}
