package net.frozenorb.apiv3.web.route.bannedAsns;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.BannedAsn;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEBannedAsnsId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		BannedAsn bannedAsn = BannedAsn.findById(Integer.parseInt(ctx.request().getParam("bannedAsn")));

		if (bannedAsn == null) {
			ErrorUtils.respondNotFound(ctx, "Banned asn", ctx.request().getParam("bannedAsn"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> bannedAsn.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.BANNED_ASN_DELETE, ImmutableMap.of("bannedAsnId", bannedAsn.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, bannedAsn);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, bannedAsn);
		}
	}

}