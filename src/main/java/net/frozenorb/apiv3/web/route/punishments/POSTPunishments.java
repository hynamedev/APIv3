package net.frozenorb.apiv3.web.route.punishments;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.IpBan;
import net.frozenorb.apiv3.domain.IpLogEntry;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.unsorted.Permissions;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTPunishments implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		User target = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("user"), v));

		if (target == null) {
			ErrorUtils.respondNotFound(ctx, "User", requestBody.getString("user"));
			return;
		}

		String publicReason = requestBody.getString("publicReason");
		String privateReason = requestBody.getString("privateReason");

		if (publicReason == null || publicReason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "publicReason");
			return;
		}

		if (privateReason == null || privateReason.trim().isEmpty()) {
			ErrorUtils.respondRequiredInput(ctx, "privateReason");
			return;
		}

		Punishment.PunishmentType type = Punishment.PunishmentType.valueOf(requestBody.getString("type", "").toUpperCase());

		if (type != Punishment.PunishmentType.WARN) {
			List<Punishment> punishments = SyncUtils.runBlocking(v -> Punishment.findByUserAndType(target, ImmutableSet.of(type), v));

			for (Punishment alternatePunishment : punishments) {
				if (alternatePunishment.isActive()) {
					User user = SyncUtils.runBlocking(v -> User.findById(alternatePunishment.getAddedBy(), v));
					String lastPunishmentAddedBy = "";

					if (user != null) {
					    lastPunishmentAddedBy = user.getLastUsername();
					}

					ErrorUtils.respondOther(ctx, 409, "User already covered by alternate punishment.", "alreadyCoveredByAlternatePunishment", ImmutableMap.of("alternatePunishmentBy", lastPunishmentAddedBy));
					return;
				}
			}
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

		Map<String, Object> meta = requestBody.getJsonObject("metadata").getMap();

		if (meta == null) {
			ErrorUtils.respondRequiredInput(ctx, "request body meta");
			return;
		}

		// We purposely don't do a null check, punishments don't have to have a source.
		User addedBy = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("addedBy"), v));
		boolean isProtected = SyncUtils.runBlocking(v -> target.hasPermissionAnywhere(Permissions.PROTECTED_PUNISHMENT, v));

		if (isProtected) {
			ErrorUtils.respondOther(ctx, 409, "User is protected from punishments.", "protectedFromPunishments", ImmutableMap.of());
			return;
		}

		Punishment punishment = new Punishment(target, publicReason, privateReason, type, expiresAt, addedBy, ctx.get("actor"), meta);
		String userIp = requestBody.getString("userIp");

		if (userIp == null) {
			IpLogEntry latestIpLogEntry = SyncUtils.runBlocking(v -> IpLogEntry.findLatestByUser(target.getId(), v));

			if (latestIpLogEntry != null) {
				userIp = latestIpLogEntry.getUserIp();
			}
		} else if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "IP address \"" + userIp + "\" is not valid.");
			return;
		}

		if (addedBy != null) {
			boolean allowed = SyncUtils.runBlocking(v -> addedBy.hasPermissionAnywhere(Permissions.CREATE_PUNISHMENT + "." + type.name().toLowerCase(), v));

			if (!allowed) {
				ErrorUtils.respondOther(ctx, 409, "User given does not have permission to create this punishment.", "userDoesNotHavePermission", ImmutableMap.of());
				return;
			}
		}

		if ((type == Punishment.PunishmentType.BAN || type == Punishment.PunishmentType.BLACKLIST) && userIp != null) {
			IpBan ipBan = new IpBan(userIp, punishment);
			SyncUtils.<Void>runBlocking(v -> ipBan.insert(v));

			punishment.linkIpBan(ipBan);
		}

		SyncUtils.<Void>runBlocking(v -> punishment.insert(v));

		if (addedBy != null) {
			AuditLog.log(addedBy.getId(), requestBody.getString("addedByIp"), ctx, AuditLogActionType.PUNISHMENT_CREATE, ImmutableMap.of("punishmentId", punishment.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, punishment);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, punishment);
		}
	}

}