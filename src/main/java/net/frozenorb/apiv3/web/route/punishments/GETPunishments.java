package net.frozenorb.apiv3.web.route.punishments;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETPunishments implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		try {
			int skip = ctx.request().getParam("skip") == null ? 0 : Integer.parseInt(ctx.request().getParam("skip"));
			int pageSize = ctx.request().getParam("pageSize") == null ? 100 : Integer.parseInt(ctx.request().getParam("pageSize"));
			UUID addedBy = ctx.request().getParam("addedBy") == null ? null : UUID.fromString(ctx.request().getParam("addedBy"));

			Punishment.findPaginated(ctx.request().getParam("user") == null ? new Document() : new Document("user", UuidUtils.parseUuid(ctx.request().getParam("user"))), skip, pageSize, addedBy, (punishments, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					if (ctx.request().getParam("active") != null) {
						boolean requireActive = Boolean.parseBoolean(ctx.request().getParam("active"));
						APIv3.respondJson(ctx, 200, punishments.stream().filter(punishment -> punishment.isActive() == requireActive).collect(Collectors.toList()));
					} else {
						APIv3.respondJson(ctx, 200, punishments);
					}
				}
			});
		} catch (NumberFormatException ignored) {
			ErrorUtils.respondInvalidInput(ctx, "skip and pageSize must be numerical inputs.");
		}
	}

}