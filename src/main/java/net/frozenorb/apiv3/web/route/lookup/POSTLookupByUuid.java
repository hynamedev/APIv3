package net.frozenorb.apiv3.web.route.lookup;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTLookupByUuid implements Handler<RoutingContext> {

    private final MongoCollection<Document> usersCollection;

    @Autowired
    public POSTLookupByUuid(MongoDatabase database) {
        this.usersCollection = database.getCollection("users");
    }

    public void handle(RoutingContext ctx) {
        JsonObject requestBody = ctx.getBodyAsJson();
        List<String> rawUuids = (List<String>) requestBody.getJsonArray("uuids", new JsonArray()).getList();
        // because we accept uuids with/without dashes, we store the actual uuid ->
        // how we were given the uuid, so we can return it to clients properly
        Map<String, String> originalInputs = new HashMap<>();

        for (String rawUuid : rawUuids) {
            try {
                UUID parsedUuid = UuidUtils.parseUuid(rawUuid);

                if (UuidUtils.isAcceptableUuid(parsedUuid)) {
                    originalInputs.put(parsedUuid.toString(), rawUuid);
                }
            } catch (IllegalArgumentException ignored) {
                // that player will just be absent from the result,
                // identical to how Mojang does it
            }
        }

        Document query = new Document("_id", new Document("$in", originalInputs.keySet()));
        Document project = new Document("lastUsername", 1);
        List<Document> into = new ArrayList<>();

        usersCollection.find(query).projection(project).into(into, SyncUtils.vertxWrap((users, error) -> {
            if (error != null) {
                ErrorUtils.respondInternalError(ctx, error);
                return;
            }

            Map<String, String> result = new HashMap<>();

            users.forEach(doc -> {
                String properUuid = doc.getString("_id");
                String clientUuid = originalInputs.get(properUuid);

                result.put(clientUuid, doc.getString("lastUsername"));
            });

            APIv3.respondJson(ctx, 200, result);
        }));
    }

}