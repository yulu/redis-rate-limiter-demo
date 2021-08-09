package com.littlecheesecake.redis.demo.repository;

public interface RateDao {
   int limitRateFixedWindowCounter(String apiPath, String id, int interval, int limit);

   int limitRateRollingWindowLog(String apiPath, String id, int interval, int limit);

   int limitRateRollingWindowCounter(String apiPath, String id, int interval, int limit);
}
