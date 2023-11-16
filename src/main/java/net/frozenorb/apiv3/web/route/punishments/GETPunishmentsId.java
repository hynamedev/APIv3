package net.frozenorb.apiv3.web.route.punishments;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETPunishmentsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Punishment.findById(ctx.request().getParam("punishmentId"), (punishment, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, punishment);
			}
		});
	}

}