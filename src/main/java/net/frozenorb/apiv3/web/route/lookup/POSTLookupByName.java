package net.frozenorb.apiv3.web.route.lookup;

import com.google.common.collect.ImmutableMap;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTLookupByName implements Handler<RoutingContext> {

    private final MongoCollection<Document> usersCollection;

    @Autowired
    public POSTLookupByName(MongoDatabase database) {
        this.usersCollection = database.getCollection("users");
    }

    public void handle(RoutingContext ctx) {
        JsonObject requestBody = ctx.getBodyAsJson();
        List<String> rawNames = (List<String>) requestBody.getJsonArray("names", new JsonArray()).getList();
        // because we accept names in any case, we store the lower case ->
        // how we were given the name, so we can return it to clients properly
        Map<String, String> originalCase = new HashMap<>();

        for (String rawName : rawNames) {
            originalCase.put(rawName.toLowerCase(), rawName);
        }

        Document query = new Document("lastUsernameLower", new Document("$in", originalCase.keySet()));
        Document project = new Document("lastUsernameLower", 1).append("lastUsername", 1); // includes _id automatically
        List<Document> into = new ArrayList<>();

        usersCollection.find(query).projection(project).into(into, SyncUtils.vertxWrap((users, error) -> {
            if (error != null) {
                ErrorUtils.respondInternalError(ctx, error);
                return;
            }

            Map<String, Map<String, String>> result = new HashMap<>();

            users.forEach(doc -> {
                String lowerName = doc.getString("lastUsernameLower");
                String clientUuid = originalCase.get(lowerName);

                result.put(clientUuid, ImmutableMap.of(
                    "uuid", doc.getString("_id"),
                    "username", doc.getString("lastUsername")
                ));
            });

            APIv3.respondJson(ctx, 200, result);
        }));
    }

}