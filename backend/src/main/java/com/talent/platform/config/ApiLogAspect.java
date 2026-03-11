package com.talent.platform.config;

import com.talent.platform.entity.ApiAccessLog;
import com.talent.platform.repository.ApiAccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiLogAspect {

    private final ApiAccessLogRepository apiAccessLogRepository;

    @Around("execution(* com.talent.platform.controller..*(..))")
    public Object logApiAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
        }

        long startTime = System.currentTimeMillis();
        int statusCode = 200;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            statusCode = 500;
            throw t;
        } finally {
            long costMs = System.currentTimeMillis() - startTime;
            try {
                saveLog(request, costMs, statusCode);
            } catch (Exception e) {
                log.warn("Failed to save API access log: {}", e.getMessage());
            }
        }
    }

    private void saveLog(HttpServletRequest request, long costMs, int statusCode) {
        ApiAccessLog apiLog = new ApiAccessLog();
        apiLog.setCostMs(costMs);
        apiLog.setStatusCode(statusCode);

        if (request != null) {
            apiLog.setMethod(request.getMethod());
            apiLog.setUri(request.getRequestURI());
            apiLog.setIp(getClientIp(request));
        } else {
            apiLog.setMethod("");
            apiLog.setUri("");
            apiLog.setIp("");
        }

        String username = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
        }
        apiLog.setUsername(username);

        apiAccessLogRepository.save(apiLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        if (request.getRemoteAddr() != null) {
            return request.getRemoteAddr();
        }
        return "";
    }
}
