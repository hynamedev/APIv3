package net.frozenorb.apiv3.web.route.prefixGrants;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.PrefixGrant;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETPrefixGrants implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		try {
			int skip = ctx.request().getParam("skip") == null ? 0 : Integer.parseInt(ctx.request().getParam("skip"));
			int pageSize = ctx.request().getParam("pageSize") == null ? 100 : Integer.parseInt(ctx.request().getParam("pageSize"));

			PrefixGrant.findPaginated(ctx.request().getParam("user") == null ? new Document() : new Document("user", UuidUtils.parseUuid(ctx.request().getParam("user"))), skip, pageSize, (grants, error) -> {
				if (ctx.request().getParam("active") != null) {
					boolean requireActive = Boolean.parseBoolean(ctx.request().getParam("active"));
					APIv3.respondJson(ctx, 200, grants.stream().filter(grant -> grant.isActive() == requireActive).collect(Collectors.toList()));
				} else {
					APIv3.respondJson(ctx, 200, grants);
				}
			});
		} catch (NumberFormatException ignored) {
			ErrorUtils.respondInvalidInput(ctx, "skip and pageSize must be numerical inputs.");
		}
	}

}