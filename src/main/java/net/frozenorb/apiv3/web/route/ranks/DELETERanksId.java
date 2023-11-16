package net.frozenorb.apiv3.web.route.ranks;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.Rank;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETERanksId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Rank rank = Rank.findById(ctx.request().getParam("rankId"));

		if (rank == null) {
			ErrorUtils.respondNotFound(ctx, "Rank", ctx.request().getParam("rankId"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> rank.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.RANK_DELETE, ImmutableMap.of("rankId", rank.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, rank);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, rank);
		}
	}

}