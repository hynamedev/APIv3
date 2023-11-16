package net.frozenorb.apiv3.service.usersession;

import com.mongodb.async.SingleResultCallback;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserSessionService {

    void sessionExists(String userIp, String userSession, SingleResultCallback<Boolean> callback);

    void createSession(UUID user, String userIp, SingleResultCallback<String> callback);

    void invalidateSession(String userIp, String userSession, SingleResultCallback<Void> callback);

    void invalidateAllSessions(UUID user, SingleResultCallback<Void> callback);
    
}