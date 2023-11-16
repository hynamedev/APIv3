package net.frozenorb.apiv3.web.route.grants;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.domain.Rank;
import net.frozenorb.apiv3.domain.ServerGroup;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.totp.TotpAuthorizationResult;
import net.frozenorb.apiv3.unsorted.Permissions;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTGrants implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		User target = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("user"), v));

		if (target == null) {
			ErrorUtils.respondNotFound(ctx, "User", requestBody.getString("user"));
			return;
		}

		String reason = requestBody.getString("reason");

		if (reason == null || reason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "reason");
			return;
		}

		Set<ServerGroup> scopes = new HashSet<>();
		List<String> scopeIds = (List<String>) requestBody.getJsonArray("scopes").getList();

		if (!scopeIds.isEmpty()) {
			for (String serverGroupId : scopeIds) {
				ServerGroup serverGroup = ServerGroup.findById(serverGroupId);

				if (serverGroup == null) {
					ErrorUtils.respondNotFound(ctx, "Server group", serverGroupId);
					return;
				}

				scopes.add(serverGroup);
			}
		}

		Rank rank = Rank.findById(requestBody.getString("rank"));

		if (rank == null) {
			ErrorUtils.respondNotFound(ctx, "Rank", requestBody.getString("rank"));
			return;
		}

		Instant expiresAt = null;

		if (requestBody.containsKey("expiresIn") && requestBody.getLong("expiresIn") != -1) {
			long expiresInMillis = requestBody.getLong("expiresIn") * 1000;
			expiresAt = Instant.ofEpochMilli(System.currentTimeMillis() + expiresInMillis);
		}

		if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
			ErrorUtils.respondInvalidInput(ctx, "Expiration time cannot be in the past.");
			return;
		}

		// We purposely don't fail on a null check, grants don't have to have a source.
		User addedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("addedBy"), v));

		if (addedBy != null) {
			boolean allowed = SyncUtils.runBlocking(v -> addedBy.hasPermissionAnywhere(Permissions.CREATE_GRANT + "." + rank.getId(), v));

			if (!allowed) {
				ErrorUtils.respondOther(ctx, 409, "User given does not have permission to create this grant.", "userDoesNotHavePermission", ImmutableMap.of());
				return;
			}

			if (rank.isGrantRequiresTotp()) {
			    if (!requestBody.containsKey("totpCode")) {
			        ErrorUtils.respondInvalidInput(ctx, "Rank must be granted through API or website.");
			        return;
			    }

				int code = requestBody.getInteger("totpCode", -1);
				TotpAuthorizationResult totpAuthorizationResult = SyncUtils.runBlocking(v -> addedBy.checkTotpAuthorization(code, null, v));

				if (!totpAuthorizationResult.isAuthorized()) {
					ErrorUtils.respondInvalidInput(ctx, "Totp authorization failed: " + totpAuthorizationResult.name());
					return;
				}
			}
		} else if (rank.isGrantRequiresTotp()) {
		    ErrorUtils.respondInvalidInput(ctx, "Rank must be granted through API or website.");
		    return;
		}

		int storeItemId = requestBody.getInteger("storeItemId", -1);
		int storeOrderId = requestBody.getInteger("storeOrderId", -1);

		Grant grant = new Grant(target, reason, scopes, rank, expiresAt, addedBy, storeItemId, storeOrderId);
		SyncUtils.<Void>runBlocking(v -> grant.insert(v));

		if (addedBy != null) {
			AuditLog.log(addedBy.getId(), requestBody.getString("addedByIp"), ctx, AuditLogActionType.GRANT_CREATE, ImmutableMap.of("grantId", grant.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, grant);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, grant);
		}
	}

}