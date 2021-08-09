package com.littlecheesecake.redis.demo.controller;

import com.littlecheesecake.redis.demo.annotation.RateLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateController {

  @GetMapping("/rate/second/{id}")
  @RateLimiter(apiPath = "/rate/second", customerId = "{id}", unit = RateLimiter.UNIT.SECOND, limit = 2)
  public Object getRate(@PathVariable String id) {
    return "ok";
  }

  @GetMapping("/rate/min/{id}")
  @RateLimiter(apiPath = "/rate/min", customerId = "{id}", unit = RateLimiter.UNIT.MINUTE, limit = 2)
  public Object getRateMin(@PathVariable String id) {
    return "ok";
  }

  @GetMapping("/rate/hour/{id}")
  @RateLimiter(apiPath = "/rate/hour", customerId = "{id}", unit = RateLimiter.UNIT.HOUR, limit = 2)
  public Object getRateHour(@PathVariable String id) {
    return "ok";
  }

  @GetMapping("/rate_free/{id}")
  public Object getRateFree(@PathVariable String id) {
    return "ok";
  }
}
