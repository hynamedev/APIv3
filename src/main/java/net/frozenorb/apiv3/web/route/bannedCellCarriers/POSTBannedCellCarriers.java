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
public final class POSTBannedCellCarriers implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		int id = requestBody.getInteger("id", -1);
		String note = requestBody.getString("note");

		BannedCellCarrier bannedCellCarrier = new BannedCellCarrier(id, note);
		SyncUtils.<Void>runBlocking(v -> bannedCellCarrier.insert(v));

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.BANNED_CALL_CARRIER_CREATE, ImmutableMap.of("bannedCellCarrierId", id), (ignored, error) -> {
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