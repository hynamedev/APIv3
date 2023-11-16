package net.frozenorb.apiv3.service.mojang;

import com.mongodb.async.SingleResultCallback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

@Component
public final class HttpMojangService implements MojangService {

    @Autowired private HttpClient httpsClient;

    @Override
    public void getName(UUID id, SingleResultCallback<String> callback) {
        httpsClient.get(443, "sessionserver.mojang.com", "/session/minecraft/profile/" + id.toString().replace("-", ""), (response) -> {
            response.bodyHandler((body) -> {
                try {
                    JsonObject bodyJson = new JsonObject(body.toString());
                    String name = bodyJson.getString("name");

                    if (name == null) {
                        callback.onResult(null, new IOException("Hit Mojang API rate limit: " + bodyJson.encode()));
                    } else {
                        callback.onResult(name, null);
                    }
                } catch (DecodeException ex) {
                    callback.onResult(null, ex);
                }
            });

            response.exceptionHandler((error) -> callback.onResult(null, error));
        }).end();
    }

}