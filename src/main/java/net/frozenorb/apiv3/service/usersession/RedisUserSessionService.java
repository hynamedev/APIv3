package net.frozenorb.apiv3.service.usersession;

import com.mongodb.async.SingleResultCallback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.vertx.redis.RedisClient;

@Component
public final class RedisUserSessionService implements UserSessionService {

    @Autowired private RedisClient redisClient;
    @Value("${userSession.sessionExpirationTimeDays}") private int sessionExpirationTimeDays;

    @Override
    public void sessionExists(String userIp, String userSession, SingleResultCallback<Boolean> callback) {
        if (userIp == null || userIp.isEmpty() || userSession == null || userSession.isEmpty()) {
            callback.onResult(false, null);
            return;
        }

        redisClient.exists("apiv3:sessions:" + userIp + ":" + userSession, (result) -> {
            if (result.succeeded()) {
                callback.onResult(result.result() == 1, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

    @Override
    public void createSession(UUID user, String userIp, SingleResultCallback<String> callback) {
        String userSession = UUID.randomUUID().toString().replaceAll("-", "");
        String key = "apiv3:sessions:" + userIp + ":" + userSession;

        redisClient.setex(key, TimeUnit.DAYS.toSeconds(sessionExpirationTimeDays), "", (result) -> {
            if (result.succeeded()) {
                redisClient.sadd("apiv3:sessions:" + user, key, (result2) -> {
                    if (result2.succeeded()) {
                        callback.onResult(userSession, null);
                    } else {
                        callback.onResult(null, result2.cause());
                    }
                });
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

    @Override
    public void invalidateSession(String userIp, String userSession, SingleResultCallback<Void> callback) {
        redisClient.del("apiv3:sessions:" + userIp + ":" + userSession, (result) -> {
            if (result.succeeded()) {
                callback.onResult(null, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

    @Override
    public void invalidateAllSessions(UUID user, SingleResultCallback<Void> callback) {
        redisClient.smembers("apiv3:sessions:" + user, (result) -> {
            if (result.failed()) {
                callback.onResult(null, result.cause());
                return;
            }

            List<String> sessions = (List<String>) result.result().getList();

            if (sessions.isEmpty()) {
                callback.onResult(null, null);
                return;
            }

            redisClient.delMany(sessions, (result2) -> {
                if (result2.succeeded()) {
                    callback.onResult(null, null);
                } else {
                    callback.onResult(null, result2.cause());
                }
            });
        });
    }

}