package com.littlecheesecake.redis.demo.repository;

import com.google.common.collect.Lists;
import com.sun.deploy.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

@Repository
public class RateDaoImpl implements RateDao {

  @Autowired
  RedisTemplate<String, Object> redisTemplate;

  /**
   * fixed window counter algorithm - used for second only
   *
   * Pros:
   * - easy to implement
   * - memory efficient
   *
   * Cons:
   * - allow burst of request at the edge of time window
   *
   * @param apiPath the api uri path e.g. /app/data
   * @param id the customer id e.g. access key
   * @param limit the rate limit
   * @return remaining api count in the given interval
   */
  @Override
  public int limitRateFixedWindowCounter(String apiPath, String id, int interval, int limit) {
    // only allow per second interval to be applied
    if (interval != 1) {
      throw new RuntimeException("invalid interval provided: only allow value as 1 (per second)");
    }

    long epochTime = System.currentTimeMillis() / 1000; //current time in second

    String key = StringUtils.join(Lists.newArrayList(apiPath, id, String.valueOf(interval)), ":");

    RedisSerializer keyS = redisTemplate.getKeySerializer();
    List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) redisConnection -> {
      redisConnection.incrBy(keyS.serialize(key), 1);
      redisConnection.expireAt(keyS.serialize(key), epochTime + 1);
      return null;
    });

    int count = ((Long)results.get(0)).intValue();

    return limit - count;
  }

  /**
   * rolling window log with sorted set (used for interval = 1|60, second and hourly)
   *
   * Pros:
   * - very accurate
   * - distributed safe
   *
   * Cons:
   * - memory intensive
   *
   * @param apiPath the api uri path e.g. /app/data
   * @param id the customer id e.g. access key
   * @param interval the time interval that the limit is applied to
   * @param limit the rate limit
   * @return remaining api count in the given interval
   */
  @Override
  public int limitRateRollingWindowLog(String apiPath, String id, int interval, int limit) {
    if (interval != 1 && interval != 60) {
      throw new RuntimeException("invalid interval provided: only allow value of 1 or 60 (per second or per min)");
    }

    long epochTimeMilli = System.currentTimeMillis();

    String key = StringUtils.join(Lists.newArrayList(apiPath, id, String.valueOf(interval)), ":");

    // get clear before time
    long clearBeforeTime = epochTimeMilli - interval * 1000;

    // get ttl time
    long ttl = interval;

    RedisSerializer keyS = redisTemplate.getKeySerializer();
    RedisSerializer valueS = redisTemplate.getValueSerializer();
    byte[] k = keyS.serialize(key);

    List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) redisConnection -> {
      // When a user attempts to perform an action, we first drop all elements of the set which occured before one interval ago.
      redisConnection.zRemRangeByScore(k, 0, clearBeforeTime);

      // We add the current timestamp to the set
      redisConnection.zAdd(k, epochTimeMilli, valueS.serialize(String.valueOf(epochTimeMilli)));

      // We fetch all elements of the set
      redisConnection.zRangeWithScores(k, 0, -1);

      // We set a TTL equal to the rate-limiting interval on the set (to save space)
      redisConnection.expire(k, ttl);
      return null;
    });

    Set<RedisZSetCommands.Tuple> s = (Set<RedisZSetCommands.Tuple>) results.get(2);
    int count = s.size();

    return limit - count;
  }

  /**
   * rolling window counter (apply to min, hour or day limit)
   *
   * Pros:
   * - memory efficient
   *
   * Cons:
   * - not accurate: According to experiments done by Cloudflare, only 0.003% of requests are wrongly allowed or rate limited
   * among 400 million requests: https://blog.cloudflare.com/counting-things-a-lot-of-different-things/
   *
   * @param apiPath the api uri path e.g. /app/data
   * @param id the customer id e.g. access key
   * @param interval the time interval that the limit is applied to
   * @param limit the rate limit
   * @return remaining api count in the given interval
   */
  @Override
  public int limitRateRollingWindowCounter(String apiPath, String id, int interval, int limit) {
    if (interval != 60 && interval != 3600 && interval != 3600 * 24) {
      throw new RuntimeException("invalid interval provided: only allow value of 1 or 60 (per second or per min)");
    }

    // get time windows - start time of previous and current windows
    long currentTime = System.currentTimeMillis();
    long currentWindow;
    long prevWindow;
    Calendar now = Calendar.getInstance();
    now.set(Calendar.SECOND, 0);
    now.set(Calendar.MILLISECOND, 0);

    if (interval == 3600) {
      // get start of the hour
      now.set(Calendar.MINUTE, 0);
    } else if (interval == 3600 * 24) {
      // get start of the day
      now.set(Calendar.MINUTE, 0);
      now.set(Calendar.HOUR_OF_DAY, 0);
    }
    currentWindow = now.getTime().getTime();
    prevWindow = currentWindow - interval * 1000;

    String keyPrev = StringUtils.join(Lists.newArrayList(apiPath, id, String.valueOf(interval), String.valueOf(prevWindow)), ":");
    String keyCurrent = StringUtils.join(Lists.newArrayList(apiPath, id, String.valueOf(interval), String.valueOf(currentWindow)), ":");
    RedisSerializer keyS = redisTemplate.getKeySerializer();
    byte[] kPrev = keyS.serialize(keyPrev);
    byte[] kCurrent = keyS.serialize(keyCurrent);

    List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) redisConnection -> {
      // get prevWindow count
      redisConnection.get(kPrev);

      // add and get currentWindow count
      redisConnection.incrBy(kCurrent, 1);

      // add ttl: expires only after the next time window expires because the prev window needs to be retained
      // for count calculation
      redisConnection.expireAt(kCurrent, currentWindow / 1000 + 2 * interval);

      return null;
    });

    long prevCount = results.get(0) == null ? 0L : Long.valueOf((String)results.get(0));
    long currentCount = (long) results.get(1);

    // calculate the estimated count
    long count = (long)((1 - 1.0 * (currentTime - currentWindow) / (interval * 1000)) * prevCount) + currentCount;

    return (int)(limit - count);
  }
}
