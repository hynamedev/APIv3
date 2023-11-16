package net.frozenorb.apiv3.web.route.ipBans;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.IpBan;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETIpBansId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		IpBan.findById(ctx.request().getParam("ipBanId"), (ipBan, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, ipBan);
			}
		});
	}

}