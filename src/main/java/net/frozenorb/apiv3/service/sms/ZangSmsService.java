package net.frozenorb.apiv3.service.sms;

import com.google.common.base.Charsets;
import com.google.common.net.MediaType;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.unsorted.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

@Component
public final class ZangSmsService implements SmsService {

    @Autowired private HttpClient httpsClient;
    @Value("${zang.accountSid}") private String accountSid;
    @Value("${zang.authToken}") private String authToken;
    @Value("${zang.fromNumber}") private String fromNumber;

    @Override
    public void sendText(Notification notification, String to, SingleResultCallback<Void> callback) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(Charsets.UTF_8));

        httpsClient.post(443, "api.zang.io", "/v2/Accounts/" + accountSid + "/SMS/Messages.json", (response) -> {
            response.bodyHandler((body) -> {
                JsonObject bodyJson = new JsonObject(body.toString());

                if (bodyJson.getString("status", "").equals("queued")) {
                    callback.onResult(null, null);
                } else {
                    callback.onResult(null, new IOException("Could not send text message: " + bodyJson.encode()));
                }
            });

            response.exceptionHandler((error) -> callback.onResult(null, error));
        }).putHeader("Authorization", authHeader).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString()).end("To=" + to + "&From=" + fromNumber + "&Body=" + notification.getBody());
    }

    @Override
    public void lookupCarrierInfo(String phoneNumber, SingleResultCallback<SmsCarrierInfo> callback) {
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(Charsets.UTF_8));

        httpsClient.post(443, "api.zang.io", "/v2/Accounts/" + accountSid + "/Lookups/Carrier.json", (response) -> {
            response.bodyHandler((body) -> {
                JsonObject bodyJson = new JsonObject(body.toString());

                if (bodyJson.containsKey("carrier_lookups")) {
                    // Zang returns an array, but we don't batch at all so we always just get the first element
                    JsonObject lookupResult = bodyJson.getJsonArray("carrier_lookups").getJsonObject(0);
                    callback.onResult(new SmsCarrierInfo(lookupResult), null);
                } else {
                    callback.onResult(null, new IOException("Could not parse Zang result: " + bodyJson.encode()));
                }
            });

            response.exceptionHandler((error) -> callback.onResult(null, error));
        })
        .putHeader("Authorization", authHeader)
        .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
        .end("PhoneNumber=" + phoneNumber);
    }

}