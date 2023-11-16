package net.frozenorb.apiv3.web.route.ipBans;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.IpBan;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;

import org.bson.Document;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETIpBans implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		try {
			int skip = ctx.request().getParam("skip") == null ? 0 : Integer.parseInt(ctx.request().getParam("skip"));
			int pageSize = ctx.request().getParam("pageSize") == null ? 100 : Integer.parseInt(ctx.request().getParam("pageSize"));
			String userIp = ctx.request().getParam("userIp");

			IpBan.findPaginated(userIp == null ? new Document() : (IpUtils.isValidIp(userIp) ? new Document("userIp", userIp) : new Document("hashedUserIp", userIp)), skip, pageSize, (grants, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, grants);
				}
			});
		} catch (NumberFormatException ignored) {
			ErrorUtils.respondInvalidInput(ctx, "skip and pageSize must be numerical inputs.");
		}
	}

}