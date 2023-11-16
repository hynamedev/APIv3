package net.frozenorb.apiv3.service.email;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.unsorted.Notification;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {

    void sendEmail(Notification notification, String target, SingleResultCallback<Void> callback);

}