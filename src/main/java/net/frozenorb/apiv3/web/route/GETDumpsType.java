package net.frozenorb.apiv3.web.route;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.domain.IpBan;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.domain.Rank;
import net.frozenorb.apiv3.domain.Server;
import net.frozenorb.apiv3.domain.ServerGroup;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.GeoJsonPoint;
import net.frozenorb.apiv3.util.SpringUtils;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETDumpsType implements Handler<RoutingContext> {

	private String totpCache = SpringUtils.getBean(Gson.class).toJson("");
	private String banCache = SpringUtils.getBean(Gson.class).toJson("");
	private String blacklistCache = SpringUtils.getBean(Gson.class).toJson("");
	private String ipBanCache = SpringUtils.getBean(Gson.class).toJson("");
	private String deniableCache = SpringUtils.getBean(Gson.class).toJson("");
	private String emptyIpIntelJSON = SpringUtils.getBean(Gson.class).toJson(ImmutableSet.of());
	private Map<String, String> grantCache = ImmutableMap.of();
	private Map<String, GeoJsonPoint> ipIntelCache = ImmutableMap.of();

	// 5 minutes, can't use TimeUnit expression in annotation
	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void updateCache() {
		User.findAllWithTotpSetup((users, error) -> {
			if (error != null) {
				error.printStackTrace();
				return;
			}

			List<UUID> totpCache = new LinkedList<>();

			for (User user : users) {
				totpCache.add(user.getId());
			}

			this.totpCache = SpringUtils.getBean(Gson.class).toJson(totpCache);
		});

		Punishment.findByType(ImmutableSet.of(
				Punishment.PunishmentType.BAN,
				Punishment.PunishmentType.BLACKLIST
		), (punishments, error) -> {
			if (error != null) {
				error.printStackTrace();
				return;
			}

			List<UUID> banCache = new LinkedList<>();
			List<UUID> blacklistCache = new LinkedList<>();
			List<UUID> accessDeniableCache = new LinkedList<>();

			for (Punishment punishment : punishments) {
				if (!punishment.isActive()) {
					continue;
				}

				if (punishment.getType() == Punishment.PunishmentType.BAN) {
					banCache.add(punishment.getUser());
					accessDeniableCache.add(punishment.getUser());
				} else if (punishment.getType() == Punishment.PunishmentType.BLACKLIST) {
					blacklistCache.add(punishment.getUser());
					accessDeniableCache.add(punishment.getUser());
				}
			}

			this.banCache = SpringUtils.getBean(Gson.class).toJson(banCache);
			this.blacklistCache = SpringUtils.getBean(Gson.class).toJson(blacklistCache);
			this.deniableCache = SpringUtils.getBean(Gson.class).toJson(accessDeniableCache);
		});

		Grant.findAll((grants, error) -> {
			if (error != null) {
				error.printStackTrace();
				return;
			}

			Map<String, Map<String, List<UUID>>> grantCachePreJSON = new HashMap<>();

			for (ServerGroup serverGroup : ServerGroup.findAll()) {
				grantCachePreJSON.put(serverGroup.getId().toLowerCase(), new HashMap<>());
			}

			for (Grant grant : grants) {
				if (!grant.isActive()) {
					continue;
				}

				for (ServerGroup serverGroup : ServerGroup.findAll()) {
					if (!serverGroup.getId().equals(ServerGroup.DEFAULT_GROUP_ID) && !grant.appliesOn(serverGroup)) {
						continue;
					}

					Map<String, List<UUID>> serverGroupGrantDump = grantCachePreJSON.get(serverGroup.getId().toLowerCase());
					List<UUID> users = serverGroupGrantDump.get(grant.getRank());

					if (users == null) {
						users = new LinkedList<>();
						serverGroupGrantDump.put(grant.getRank(), users);
					}

					users.add(grant.getUser());
				}
			}

			Map<String, String> grantCacheJSON = Maps.newHashMap();

			for (Entry<String, Map<String, List<UUID>>> entry : grantCachePreJSON.entrySet()) {
			    grantCacheJSON.put(entry.getKey(), SpringUtils.getBean(Gson.class).toJson(entry.getValue()));
			}

			this.grantCache = grantCacheJSON;
		});

		IpBan.find((ipBans, error) -> {
			if (error != null) {
				error.printStackTrace();
				return;
			}

			List<String> ipBanCache = new LinkedList<>();

			for (IpBan ipBan : ipBans) {
				if (!ipBan.isActive()) {
					continue;
				}

				ipBanCache.add(ipBan.getUserIp());
			}

			this.ipBanCache = SpringUtils.getBean(Gson.class).toJson(ipBanCache);
		});
	}

	public void handle(RoutingContext ctx) {
		String dumpType = ctx.request().getParam("dumpType");

		switch (dumpType.toLowerCase()) {
			case "totp":
				APIv3.respondRawJson(ctx, 200, totpCache);
				return;
			case "ban":
				APIv3.respondRawJson(ctx, 200, banCache);
				return;
			case "blacklist":
				APIv3.respondRawJson(ctx, 200, blacklistCache);
				return;
			case "accessdeniable": // Lowercase d because we convert to lowercase above
				APIv3.respondRawJson(ctx, 200, deniableCache);
				return;
			case "ipban":
				APIv3.respondRawJson(ctx, 200, ipBanCache);
				return;
			case "grant":
				String serverGroup = ServerGroup.DEFAULT_GROUP_ID;
				String customScope = ctx.request().getParam("scope");

				if (customScope != null) {
					if (customScope.equalsIgnoreCase("me")) {
						Actor actor = ctx.get("actor");

						if (actor.getType() != ActorType.SERVER) {
							ErrorUtils.respondOther(ctx, 403, "This action can only be performed when requested by a server.", "serverOnly", ImmutableMap.of());
							return;
						}

						Server actorServer = Server.findById(actor.getName());
						serverGroup = actorServer.getServerGroup();
					} else {
 						serverGroup = customScope;
					}
				}

				APIv3.respondRawJson(ctx, 200, grantCache.get(serverGroup.toLowerCase()));
				return;
			case "rankusers":
				Rank rank = Rank.findById(ctx.request().getParam("rank"));

				if (rank == null) {
					ErrorUtils.respondNotFound(ctx, "Rank", ctx.request().getParam("rank"));
					return;
				}

				Grant.findByRank(ImmutableSet.of(rank), (grants, grantError) -> {
                    if (grantError != null) {
                        ErrorUtils.respondInternalError(ctx, grantError);
                        return;
                    }

                    Set<UUID> uuids = new HashSet<>();

                    for (Grant grant : grants) {
                        if (grant.isActive()) {
                            uuids.add(grant.getUser());
                        }
                    }

                    User.findByIdGrouped(uuids, (users, userError) -> {
						if (userError != null) {
							ErrorUtils.respondInternalError(ctx, userError);
						} else {
							APIv3.respondJson(ctx, 200, users.values());
						}
					});
                });
				return;
			case "ipintel":
				APIv3.respondRawJson(ctx, 200, emptyIpIntelJSON);
				return;
			case "ipintelformatted":
				List<Map<String, Object>> features = new ArrayList<>(ipIntelCache.size());

				ipIntelCache.values().forEach(point -> {
					features.add(ImmutableMap.of(
							"type", "Feature",
							"geometry", point
					));
				});

				APIv3.respondJson(ctx, 200, ImmutableMap.of(
						"type", "FeatureCollection",
						"features", features
				));
				return;
			default:
				ErrorUtils.respondInvalidInput(ctx, dumpType + " is not a valid type. Not in [totp, ban, blacklist, accessDeniable, ipBan, grant, ipIntel, ipIntelFormatted]");
		}
	}

}