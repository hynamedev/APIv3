package net.frozenorb.apiv3.web.route.accessTokens;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class DELETEAccessTokensId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		AccessToken accessToken = SyncUtils.runBlocking(v -> AccessToken.findById(ctx.request().getParam("accessToken"), v));

		if (accessToken == null) {
			ErrorUtils.respondNotFound(ctx, "Access token", ctx.request().getParam("accessToken"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> accessToken.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.ACCESS_TOKEN_DELETE, ImmutableMap.of("accessTokenId", accessToken.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, accessToken);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, accessToken);
		}
	}

}