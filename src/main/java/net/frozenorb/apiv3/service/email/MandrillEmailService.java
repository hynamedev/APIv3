package net.frozenorb.apiv3.service.email;

import com.google.common.net.MediaType;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.unsorted.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Component
public final class MandrillEmailService implements EmailService {

    @Autowired private HttpClient httpClient;
    @Value("${mandrill.apiKey}") String apiKey;
    @Value("${mandrill.fromEmail}") String fromEmail;
    @Value("${mandrill.fromName}") String fromName;

    @Override
    public void sendEmail(Notification notification, String target, SingleResultCallback<Void> callback) {
        JsonObject requestBody = new JsonObject()
            .put("key", apiKey)
            .put("message", new JsonObject()
                .put("html", notification.getBody())
                .put("subject", notification.getSubject())
                .put("from_email", fromEmail)
                .put("from_name", fromName)
                .put("to", new JsonArray()
                    .add(new JsonObject()
                        .put("email", target)
                    )
                )
        );

        httpClient.post("mandrillapp.com", "/api/1.0/messages/send.json", (response) -> {
            response.bodyHandler((responseBody) -> {
                try {
                    JsonArray bodyJson = new JsonArray(responseBody.toString());
                    JsonObject emailJson = bodyJson.getJsonObject(0);
                    String emailStatus = emailJson.getString("status");

                    if (emailStatus.equals("rejected") || emailStatus.equals("invalid")) {
                        callback.onResult(null, new IOException("Illegal email status while reading Mandrill response: " + emailStatus + " (" + emailJson.encode() + ")"));
                    } else {
                        callback.onResult(null, null);
                    }
                } catch (Exception ex) {
                    callback.onResult(null, new IOException("Failed to process Mandrill response: " + responseBody, ex));
                }
            });
            response.exceptionHandler((error) -> callback.onResult(null, error));
        }).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString()).end(requestBody.encode());
    }

}