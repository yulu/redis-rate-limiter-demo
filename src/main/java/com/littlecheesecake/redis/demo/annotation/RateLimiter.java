package com.littlecheesecake.redis.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimiter {
  enum UNIT {
    SECOND,
    MINUTE,
    HOUR,
    DAY
  }
  String apiPath();
  String customerId();
  UNIT unit();
  int limit();
}
