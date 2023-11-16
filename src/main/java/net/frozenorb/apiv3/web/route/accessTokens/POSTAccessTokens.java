package net.frozenorb.apiv3.web.route.accessTokens;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.totp.TotpAuthorizationResult;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTAccessTokens implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String actorName = requestBody.getString("actorName");
		ActorType actorType = ActorType.valueOf(requestBody.getString("actorType").toUpperCase());
		List<String> lockedIps = (List<String>) requestBody.getJsonArray("lockedIps").getList();
		User addedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("addedBy"), v));

		if (addedBy == null) {
			ErrorUtils.respondNotFound(ctx, "User", requestBody.getString("addedBy"));
			return;
		}

		int code = requestBody.getInteger("totpCode", -1);
		TotpAuthorizationResult totpAuthorizationResult = SyncUtils.runBlocking(v -> addedBy.checkTotpAuthorization(code, null, v));

		if (!totpAuthorizationResult.isAuthorized()) {
			ErrorUtils.respondInvalidInput(ctx, "Totp authorization failed: " + totpAuthorizationResult.name());
			return;
		}

		AccessToken accessToken = new AccessToken(actorName, actorType, lockedIps);
		SyncUtils.<Void>runBlocking(v -> accessToken.insert(v));

		AuditLog.log(addedBy.getId(), requestBody.getString("addedByIp"), ctx, AuditLogActionType.ACCESS_TOKEN_CREATE, ImmutableMap.of("accessTokenActorName", actorName), (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, accessToken);
			}
		});
	}

}