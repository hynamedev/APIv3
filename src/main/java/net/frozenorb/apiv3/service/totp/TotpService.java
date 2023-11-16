package net.frozenorb.apiv3.service.totp;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.domain.User;

import org.springframework.stereotype.Service;

@Service
public interface TotpService {

    boolean authorizeUser(String secret, int code);

    void isPreAuthorized(User user, String ip, SingleResultCallback<Boolean> callback);

    void markPreAuthorized(User user, String ip, SingleResultCallback<Void> callback);

    void wasRecentlyUsed(User user, int code, SingleResultCallback<Boolean> callback);

    void markRecentlyUsed(User user, int code, SingleResultCallback<Void> callback);

}