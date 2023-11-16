package net.frozenorb.apiv3.web.route.serverGroups;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Server;
import net.frozenorb.apiv3.domain.ServerGroup;

@Component
public final class GETServerGroupsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
	    ServerGroup serverGroup = ServerGroup.findById(ctx.request().getParam("serverGroupId"));
	    AtomicInteger totalOnline = new AtomicInteger();

	    Server.findAll().forEach(server -> {
	        if (serverGroup == null || Objects.equal(serverGroup.getId(), server.getServerGroup())) totalOnline.addAndGet(server.getPlayers().size());
	    });

		APIv3.respondJson(ctx, 200, Integer.valueOf(totalOnline.intValue()));
	}

}