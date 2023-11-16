package net.frozenorb.apiv3.service.disposablelogintoken;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.domain.User;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface DisposableLoginTokenService {

    void attemptLogin(String token, String userIp, SingleResultCallback<User> callback);

    void createToken(UUID user, String userIp, SingleResultCallback<String> callback);

}