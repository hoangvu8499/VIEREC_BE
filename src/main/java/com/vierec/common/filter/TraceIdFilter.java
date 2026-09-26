package com.vierec.common.filter;

import com.vierec.common.constant.AppConstants;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Attaches a trace id to every request so a log line and an error response can be
 * matched up later. Honours an inbound {@code X-Trace-Id} from an upstream gateway.
 */
@Component
@Order(Integer.MIN_VALUE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String inbound = request.getHeader(AppConstants.TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(inbound)
                ? inbound
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        MDC.put(AppConstants.TRACE_ID, traceId);
        response.setHeader(AppConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(AppConstants.TRACE_ID);
        }
    }
}
