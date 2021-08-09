## Redis Rate Limiter Demo

This is a demo project to implement a few rate limiter algorithm summaries in the book [System Design Interview](https://www.amazon.sg/System-Design-Interview-insiders-Second/dp/B08CMF2CQF)

## Summary of Rate Limiter Algos in Diagram

**Token Bucket**

![Token Bucket](./docs/ratelimiter_1-token_bucket.PNG?raw=true "Token Bucket")

**Leaking Bucket**

![Leaking Bucket](./docs/ratelimiter_2-leaking_bucket.PNG?raw=true "Leaking Bucket")

**Fixed window counter**

![Fixed window counter](./docs/ratelimiter_3-fixed_window_counter.PNG?raw=true "Fixed window counter")

**Sliding window log**

![Sliding window log](./docs/ratelimiter_4-sliding_window_log.PNG?raw=true "Sliding window log")

**Sliding window counter**

![Sliding window counter](./docs/ratelimiter_5-sliding_window_counter.PNG?raw=true "Sliding window counter")

### Demo

The demo is in Spring Boot, with Spring Data Redis Starter and Redis Client (Jedis). 

The rate limiter is written as a annotation with the interceptor [here](./src/main/java/com/littlecheesecake/redis/demo/handler/RateLimiterInterceptor.java)

```java
  @GetMapping("/rate/second/{id}")
  @RateLimiter(apiPath = "/rate/second", customerId = "{id}", unit = RateLimiter.UNIT.SECOND, limit = 2)
  public Object getRate(@PathVariable String id) {
    return "ok";
  }
```

The implementation of the redis operation is in [RateDaoImpl](./src/main/java/com/littlecheesecake/redis/demo/repository/RateDaoImpl.java). 