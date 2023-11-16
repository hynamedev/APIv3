package net.frozenorb.apiv3.service.sms;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.unsorted.Notification;

import org.springframework.stereotype.Service;

@Service
public interface SmsService {

    void sendText(Notification notification, String to, SingleResultCallback<Void> callback);

    void lookupCarrierInfo(String phoneNumber, SingleResultCallback<SmsCarrierInfo> callback);

}