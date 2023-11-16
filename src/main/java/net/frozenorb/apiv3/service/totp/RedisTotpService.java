package net.frozenorb.apiv3.service.totp;

import com.mongodb.async.SingleResultCallback;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;

import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.IpUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import io.vertx.redis.RedisClient;

@Component
public final class RedisTotpService implements TotpService {

    @Autowired private RedisClient redisClient;
    @Value("${totp.windowSize}") int windowSize;
    @Value("${totp.recentlyUsedPeriodSeconds}") int recentlyUsedPeriodSeconds;
    @Value("${totp.ipAuthorizationDays}") int ipAuthorizationDays;
    private GoogleAuthenticator googleAuthenticator;

    // has to be ran after construction (or else windowSize won't be defined when we go to
    // create this object)
    @PostConstruct
    private void setupGoogleAuth() {
        googleAuthenticator = new GoogleAuthenticator(
            new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(windowSize)
                .build()
        );
    }

    @Override
    public boolean authorizeUser(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    @Override
    public void isPreAuthorized(User user, String ip, SingleResultCallback<Boolean> callback) {
        if (!IpUtils.isValidIp(ip)) {
            callback.onResult(false, null);
            return;
        }

        redisClient.exists(user.getId() + ":preAuthorizedIp:" + ip.toLowerCase(), (result) -> {
            if (result.succeeded()) {
                callback.onResult(result.result() == 1, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

    @Override
    public void markPreAuthorized(User user, String ip, SingleResultCallback<Void> callback) {
        if (!IpUtils.isValidIp(ip)) {
            callback.onResult(null, null);
            return;
        }

        String key = user.getId() + ":preAuthorizedIp:" + ip.toLowerCase();

        redisClient.setex(key, TimeUnit.DAYS.toSeconds(ipAuthorizationDays), "", (result) -> {
            if (result.succeeded()) {
                callback.onResult(null, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

    @Override
    public void wasRecentlyUsed(User user, int code, SingleResultCallback<Boolean> callback) {
        redisClient.exists(user.getId() + ":recentTotpCodes:" + code, (result) -> {
            if (result.succeeded()) {
                callback.onResult(result.result() == 1, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

    @Override
    public void markRecentlyUsed(User user, int code, SingleResultCallback<Void> callback) {
        String key = user.getId() + ":recentTotpCodes:" + code;

        redisClient.setex(key, recentlyUsedPeriodSeconds, "", (result) -> {
            if (result.succeeded()) {
                callback.onResult(null, null);
            } else {
                callback.onResult(null, result.cause());
            }
        });
    }

}