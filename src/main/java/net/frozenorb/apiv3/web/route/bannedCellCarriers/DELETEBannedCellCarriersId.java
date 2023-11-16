package net.frozenorb.apiv3.web.route.bannedCellCarriers;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.BannedCellCarrier;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEBannedCellCarriersId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		BannedCellCarrier bannedCellCarrier = BannedCellCarrier.findById(Integer.parseInt(ctx.request().getParam("bannedCellCarrier")));

		if (bannedCellCarrier == null) {
			ErrorUtils.respondNotFound(ctx, "Banned cell carrier", ctx.request().getParam("bannedCellCarrier"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> bannedCellCarrier.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.BANNED_CALL_CARRIER_DELETE, ImmutableMap.of("bannedCellCarrierId", bannedCellCarrier.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, bannedCellCarrier);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, bannedCellCarrier);
		}
	}

}