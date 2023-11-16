package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.domain.IpLogEntry;
import net.frozenorb.apiv3.domain.Server;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import java.util.UUID;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdLogin implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		UUID uuid = UuidUtils.parseUuid(ctx.request().getParam("userId"));

		if (!UuidUtils.isAcceptableUuid(uuid)) {
			ErrorUtils.respondInvalidInput(ctx, "Uuid \"" + uuid + "\" is not valid.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		String currentUsername = requestBody.getString("username");
		String userIp = requestBody.getString("userIp");
		Actor actor = ctx.get("actor");

		if (actor.getType() != ActorType.SERVER) {
			ErrorUtils.respondOther(ctx, 403, "This action can only be performed when requested by a server.", "serverOnly", ImmutableMap.of());
			return;
		}

		Server actorServer = Server.findById(actor.getName());

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "IP address \"" + userIp + "\" is not valid.");
			return;
		}

		User.findOrCreateById(uuid, currentUsername, (user, findUserError) -> {
			if (findUserError != null) {
				ErrorUtils.respondInternalError(ctx, findUserError);
				return;
			}

			incrementIpLog(user, userIp, (ignored, ipLogError) -> {
				if (ipLogError != null) {
					ErrorUtils.respondInternalError(ctx, ipLogError);
					return;
				}

				updateUsername(user, currentUsername, (ignored2, updateUsernameError) -> {
					if (updateUsernameError != null) {
						ErrorUtils.respondInternalError(ctx, updateUsernameError);
						return;
					}

					user.seenOnServer(actorServer);
					user.save((ignored3, saveUserError) -> {
						if (saveUserError != null) {
							ErrorUtils.respondInternalError(ctx, saveUserError);
							return;
						}

						user.getLoginInfo(actorServer, userIp, (loginInfo, loginInfoError) -> {
							if (loginInfoError != null) {
								ErrorUtils.respondInternalError(ctx, loginInfoError);
							} else {
								APIv3.respondJson(ctx, 200, loginInfo);
							}
						});
					});
				});
			});
		});
	}

	public void incrementIpLog(User user, String userIp, SingleResultCallback<Void> callback) {
		IpLogEntry.findByUserAndIp(user, userIp, (existingEntry, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (existingEntry != null) {
				existingEntry.used();
				existingEntry.save(callback);
				return;
			}

			IpLogEntry inserted = new IpLogEntry(user, userIp);
			inserted.used();
			inserted.insert(callback);
		});
	}

	public void updateUsername(User user, String currentUsername, SingleResultCallback<Void> callback) {
		String lastUsername = user.getLastUsername();
		user.updateUsername(currentUsername);

		if (!currentUsername.equals(lastUsername)) {
			user.checkNameCollisions(callback);
		} else {
			callback.onResult(null, null);
		}
	}

}