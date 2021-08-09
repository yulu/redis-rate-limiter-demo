package com.littlecheesecake.redis.demo.handler;

import com.google.common.collect.Maps;
import com.littlecheesecake.redis.demo.annotation.RateLimiter;
import com.littlecheesecake.redis.demo.repository.RateDao;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Aspect
@Component
public class RateLimiterInterceptor {
  private final String X_RATE_LIMITER_REMAINING_HEADER = "X-Ratelimit-Remaining";
  private final String X_RATE_LIMITER_LIMIT_HEADER = "X-Ratelimit-Limit";

  @Autowired
  RateDao rateDao;

  @Around(" @annotation(com.littlecheesecake.redis.demo.annotation.RateLimiter)")
  public Object rateLimitAspect(ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();

    Object[] argValues = pjp.getArgs();
    String[] argNames = signature.getParameterNames();
    Map<String, Object> paramMap = Maps.newHashMap();
    for (int i = 0 ; i < argNames.length; i++) {
      paramMap.put("{" + argNames[i] + "}", argValues[i]);
    }

    RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
    String customerId = rateLimiter.customerId();
    for (String p : paramMap.keySet()) {
      customerId = customerId.replace(p, paramMap.get(p).toString());
    }

    String apiPath = rateLimiter.apiPath();
    RateLimiter.UNIT unit = rateLimiter.unit();

    int limit = rateLimiter.limit();

    int remaining = 0;
    switch (unit) {
      case SECOND:
//        remaining = rateDao.limitRateFixedWindowCounterPerSecond(apiPath, customerId, limit);
        remaining = rateDao.limitRateRollingWindowLog(apiPath, customerId, 1, limit);
        break;
      case MINUTE:
//        remaining = rateDao.limitRateRollingWindowLog(apiPath, customerId, 60, limit);
        remaining = rateDao.limitRateRollingWindowCounter(apiPath, customerId, 60, limit);
        break;
      case HOUR:
        remaining = rateDao.limitRateRollingWindowCounter(apiPath, customerId, 3600, limit);
        break;
      case DAY:
        remaining = rateDao.limitRateRollingWindowCounter(apiPath, customerId, 24 * 3600, limit);
        break;
      default:
        return pjp.proceed();
    }

    if (remaining >= 0) {
      return pjp.proceed();
    }

    // set headers and return exceed rate limit response
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set(X_RATE_LIMITER_REMAINING_HEADER, String.valueOf(remaining));
    responseHeaders.set(X_RATE_LIMITER_LIMIT_HEADER, String.valueOf(limit));

    return ResponseEntity
      .status(HttpStatus.TOO_MANY_REQUESTS)
      .headers(responseHeaders)
      .build();
  }
}
