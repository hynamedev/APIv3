package net.frozenorb.apiv3.web.route.servers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.*;
import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.unsorted.MongoToVertxCallback;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.PermissionUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public final class POSTServersHeartbeat implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Actor actor = ctx.get("actor");

		if (actor.getType() != ActorType.SERVER) {
			ErrorUtils.respondOther(ctx, 403, "This action can only be performed when requested by a server.", "serverOnly", ImmutableMap.of());
			return;
		}

		Server actorServer = Server.findById(actor.getName());
		JsonObject requestBody = ctx.getBodyAsJson();
		JsonObject players = requestBody.getJsonObject("players");
		Map<UUID, String> playerNames = extractPlayerNames(players);
		Map<UUID, String> playerIps = extractPlayerIps(players);

		CompositeFuture.all(
				createInfoResponse(actorServer, requestBody.getDouble("lastTps"), playerNames),
				createPlayerResponse(actorServer, playerNames, playerIps),
				createPermissionsResponse(ServerGroup.findById(actorServer.getServerGroup())),
				createEventsResponse(actorServer, requestBody.getJsonArray("events"))
		).setHandler((result) -> {
			if (result.succeeded()) {
				// We don't do anything with the info callback, as
				// it's just to update our database.
				APIv3.respondJson(ctx, 200, ImmutableMap.of(
						"players", result.result().result(1),
						"permissions", result.result().result(2),
						"events", result.result().result(3)
				));
			} else {
				ErrorUtils.respondInternalError(ctx, result.cause());
			}
		});
	}

	private Future<Void> createInfoResponse(Server server, double tps, Map<UUID, String> playerNames) {
		Future<Void> callback = Future.future();

		server.receivedHeartbeat(tps, playerNames.keySet());
		server.save((ignored, error) -> {
			if (error != null) {
				callback.fail(error);
			} else {
				callback.complete();
			}
		});

		return callback;
	}

	private Future<Map<String, Object>> createPlayerResponse(Server server, Map<UUID, String> playerNames, Map<UUID, String> playerIps) {
		Future<Map<String, Object>> callback = Future.future();

		Future<Map<UUID, User>> userLookupCallback = Future.future();
		Future<Map<UUID, List<Grant>>> grantLookupCallback = Future.future();
		Future<Map<UUID, List<Punishment>>> punishmentLookupCallback = Future.future();
		Future<Map<UUID, List<PrefixGrant>>> prefixGrantLookupCallback = Future.future();

		User.findOrCreateByIdGrouped(playerNames, new MongoToVertxCallback<>(userLookupCallback));
		Grant.findByUserGrouped(playerNames.keySet(), new MongoToVertxCallback<>(grantLookupCallback));
		Punishment.findByUserGrouped(playerNames.keySet(), new MongoToVertxCallback<>(punishmentLookupCallback));
		PrefixGrant.findByUserGrouped(playerNames.keySet(), new MongoToVertxCallback<>(prefixGrantLookupCallback));

		CompositeFuture.all(
				userLookupCallback,
				grantLookupCallback,
				punishmentLookupCallback,
				prefixGrantLookupCallback
		).setHandler((batchLookupInfo) -> {
			if (batchLookupInfo.failed()) {
				callback.fail(batchLookupInfo.cause());
				return;
			}

			Map<UUID, User> users = batchLookupInfo.result().result(0);
			Map<UUID, List<Grant>> grants = batchLookupInfo.result().result(1);
			Map<UUID, List<Punishment>> punishments = batchLookupInfo.result().result(2);
			Map<UUID, List<PrefixGrant>> prefixGrants = batchLookupInfo.result().result(3);
			Map<UUID, Future> loginInfoFutures = new HashMap<>();

			users.forEach((uuid, user) -> {
				Future<Map<String, Object>> loginInfoFuture = Future.future();
				createLoginInfo(user, server, grants.get(uuid), punishments.get(uuid), prefixGrants.get(uuid), loginInfoFuture);
				loginInfoFutures.put(uuid, loginInfoFuture);
			});

			CompositeFuture.all(ImmutableList.copyOf(loginInfoFutures.values())).setHandler((allLoginInfo) -> {
				if (allLoginInfo.failed()) {
					callback.fail(allLoginInfo.cause());
					return;
				}

				Map<String, Object> response = new HashMap<>();
				loginInfoFutures.forEach((uuid, loginInfo) -> response.put(uuid.toString(), loginInfo.result()));
				callback.complete(response);
			});
		});

		return callback;
	}

	private Future<Map<String, Object>> createPermissionsResponse(ServerGroup serverGroup) {
		Future<Map<String, Object>> callback = Future.future();
		Map<String, Object> permissionsResponse = new HashMap<>();

		for (Rank rank : Rank.findAll()) {
			Map<String, Boolean> scopedPermissions = PermissionUtils.mergePermissions(
					ServerGroup.findDefault().calculatePermissions(rank),
					serverGroup.calculatePermissions(rank)
			);

			permissionsResponse.put(rank.getId(), PermissionUtils.convertToList(scopedPermissions));
		}

		callback.complete(permissionsResponse);
		return callback;
	}

	private Future<Map<String, Object>> createEventsResponse(Server server, JsonArray events) {
		Future<Map<String, Object>> callback = Future.future();
		List<Future> eventFutures = new ArrayList<>();

		for (Object event : events) {
			JsonObject eventJson = (JsonObject) event;
			String type = eventJson.getString("type");

			switch (type) {
				case "leave":
					Future eventFuture = Future.future();
					eventFutures.add(eventFuture);

					User.findById(eventJson.getString("user"), ((user, error) -> {
						if (error != null) {
							eventFuture.fail(error);
							return;
						}

						if (user == null) {
							eventFuture.complete();
							return;
						}

						if (!user.leftServer(server)) {
							eventFuture.complete();
							return;
						}

						user.save((ignored, saveError) -> {
							if (saveError != null) {
								eventFuture.fail(saveError);
							} else {
								eventFuture.complete();
							}
						});
					}));

					break;
				default:
					log.warn("Recieved event with unknown type " + type + ".");
			}
		}

		CompositeFuture.all(eventFutures).setHandler((allEvents) -> {
			if (allEvents.failed()) {
				callback.fail(allEvents.cause());
			} else {
				callback.complete(ImmutableMap.of());
			}
		});

		return callback;
	}

	private Map<UUID, String> extractPlayerNames(JsonObject players) {
		Map<UUID, String> result = new HashMap<>();

		players.forEach((entry) -> {
			UUID uuid = UuidUtils.parseUuid(entry.getKey());
			JsonObject data = (JsonObject) entry.getValue();

			if (UuidUtils.isAcceptableUuid(uuid)) {
				result.put(uuid, data.getString("username"));
			}
		});

		return result;
	}

	private Map<UUID, String> extractPlayerIps(JsonObject players) {
		Map<UUID, String> result = new HashMap<>();

		players.forEach((entry) -> {
			UUID uuid = UuidUtils.parseUuid(entry.getKey());
			JsonObject data = (JsonObject) entry.getValue();
			String userIp = data.getString("userIp");

			if (UuidUtils.isAcceptableUuid(uuid) && IpUtils.isValidIp(userIp)) {
				result.put(uuid, userIp);
			}
		});

		return result;
	}

	private void createLoginInfo(User user, Server server, List<Grant> grants, List<Punishment> punishments, List<PrefixGrant> prefixGrants, Future<Map<String, Object>> callback) {
		if (user.seenOnServer(server)) {
			user.save((ignored, error) -> {
				if (error != null) {
					callback.fail(error);
					return;
				}

				user.getLoginInfo(server, punishments, ImmutableList.of(), grants, prefixGrants, new MongoToVertxCallback<>(callback));
			});
		} else {
			user.getLoginInfo(server, punishments, ImmutableList.of(), grants, prefixGrants, new MongoToVertxCallback<>(callback));
		}
	}

}