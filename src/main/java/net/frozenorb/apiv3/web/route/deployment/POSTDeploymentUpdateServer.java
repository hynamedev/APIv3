package net.frozenorb.apiv3.web.route.deployment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.domain.Server;
import net.frozenorb.apiv3.domain.ServerGroup;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.time.Instant;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTDeploymentUpdateServer implements Handler<RoutingContext> {

    public void handle(RoutingContext ctx) {
        Actor actor = ctx.get("actor");

        if (actor.getType() != ActorType.ANSIBLE) {
            ErrorUtils.respondOther(ctx, 403, "This action can only be performed by Ansible.", "ansibleOnly", ImmutableMap.of());
            return;
        }

        String serverId = ctx.request().getParam("serverId");

        JsonObject requestBody = ctx.getBodyAsJson();

        String ip = requestBody.getString("ip");
        int port = requestBody.getInteger("port", 0);
        String typeRaw = requestBody.getString("type");
        String groupRaw = requestBody.getString("group");
        String displayName = requestBody.getString("displayName");

        if (ip == null || port == 0 || typeRaw == null || groupRaw == null || displayName == null) {
            ErrorUtils.respondInvalidInput(ctx, "'ip', 'port', 'type', 'group', and 'displayName' are required.");
            return;
        }

        ActorType type;

        try {
            type = ActorType.valueOf(typeRaw.toUpperCase());
        } catch (Exception ignored) {
            ErrorUtils.respondInvalidInput(ctx, "type must be one of [ WEBSITE, ANSIBLE, STORE, BUNGEE_CORD, SERVER ]");
            return;
        }

        ServerGroup group = ServerGroup.findById(groupRaw);

        if (group == null) {
            ErrorUtils.respondNotFound(ctx, "server group", groupRaw);
            return;
        }

        Server server = Server.findById(serverId);
        AccessToken accessToken = SyncUtils.runBlocking(v -> AccessToken.findByNameAndType(serverId, type, v));

        if (server == null) {
            server = new Server(serverId, displayName, group, ip + ":" + port);
            accessToken = new AccessToken(server);

            SyncUtils.runBlocking(server::insert);
            SyncUtils.runBlocking(accessToken::insert);
        } else {
            server.setServerIp(ip + ":" + port);
            server.setLastUpdatedAt(Instant.now());
            SyncUtils.runBlocking(server::save);

            accessToken.setLockedIps(ImmutableList.of(ip));
            accessToken.setLastUpdatedAt(Instant.now());
            SyncUtils.runBlocking(accessToken::save);
        }

        ctx.response().end(accessToken.getId());
    }

}