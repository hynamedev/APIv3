package net.frozenorb.apiv3.service.disposablelogintoken;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.domain.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

import io.vertx.redis.RedisClient;

@Component
public final class RedisDisposableLoginTokenService implements DisposableLoginTokenService {

    @Autowired private RedisClient redisClient;
    @Value("${disposableLoginToken.tokenLifetimeSeconds}") private int tokenLifetimeSeconds;

    @Override
    public void attemptLogin(String token, String userIp, SingleResultCallback<User> callback) {
        if (token == null || token.isEmpty()) {
            callback.onResult(null, null);
            return;
        }

        redisClient.get("apiv3:disposableLoginTokens:" + userIp + ":" + token, (result) -> {
            if (result.failed()) {
                callback.onResult(null, result.cause());
                return;
            }

            if (result.result() == null) {
                callback.onResult(null, null);
                return;
            }

            User.findById(result.result(), (user, error) -> {
                if (error != null) {
                    callback.onResult(null, error);
                    return;
                }

                redisClient.del("apiv3:disposableLoginTokens:" + userIp + ":" + token, (result2) -> {
                    if (result2.failed()) {
                        callback.onResult(null, result2.cause());
                    } else {
                        callback.onResult(user, null);
                    }
                });
            });
        });
    }

    @Override
    public void createToken(UUID user, String userIp, SingleResultCallback<String> callback) {
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        redisClient.setex("apiv3:disposableLoginTokens:" + userIp + ":" + token, tokenLifetimeSeconds, user.toString(), (result) -> {
            if (result.succeeded()) {
                callback.onResult(token, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

}