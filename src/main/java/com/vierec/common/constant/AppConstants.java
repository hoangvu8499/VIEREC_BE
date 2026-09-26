package com.vierec.common.constant;

/**
 * Application-wide immutable constants.
 */
public final class AppConstants {

    public static final String API_V1 = "/api/v1";

    /** Name of the OpenAPI security scheme for the access_token cookie. */
    public static final String AUTH_COOKIE_SCHEME = "cookieAuth";

    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private AppConstants() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
