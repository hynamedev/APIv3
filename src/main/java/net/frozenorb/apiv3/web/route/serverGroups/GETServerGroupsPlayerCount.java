package net.frozenorb.apiv3.web.route.serverGroups;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Server;

@Component
public final class GETServerGroupsPlayerCount implements Handler<RoutingContext> {

    public void handle(RoutingContext ctx) {
        Map<String, Integer> map = Maps.newHashMap();
        AtomicInteger total = new AtomicInteger();
        
        Server.findAll().forEach(server -> {
            int size;
            Integer lastCount = null;
            map.put(server.getServerGroup(), map.getOrDefault(server.getServerGroup(), 0) + (size = server.getPlayers().size()));
            total.addAndGet(size);
        });

        map.put("total", Integer.valueOf(total.intValue()));

        APIv3.respondJson(ctx, 200, map);
    }
    
}