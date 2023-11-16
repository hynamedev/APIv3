package net.frozenorb.apiv3.domain;

import com.google.common.base.Charsets;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.service.mojang.MojangService;
import net.frozenorb.apiv3.serialization.gson.ExcludeFromReplies;
import net.frozenorb.apiv3.serialization.jackson.UuidJsonDeserializer;
import net.frozenorb.apiv3.serialization.jackson.UuidJsonSerializer;
import net.frozenorb.apiv3.service.totp.RequiresTotpResult;
import net.frozenorb.apiv3.service.totp.TotpAuthorizationResult;
import net.frozenorb.apiv3.service.totp.TotpService;
import net.frozenorb.apiv3.unsorted.MongoToVertxCallback;
import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.unsorted.Permissions;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.PermissionUtils;
import net.frozenorb.apiv3.util.PhoneUtils;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@AllArgsConstructor
public final class User {

	private static final MongoCollection<User> usersCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("users", User.class);

	@Getter @Id @JsonSerialize(using = UuidJsonSerializer.class) @JsonDeserialize(using = UuidJsonDeserializer.class) private UUID id;
	@Getter private String lastUsername;
    @Getter private String lastUsernameLower;
	@Getter @ExcludeFromReplies private Map<String, Instant> aliases = new HashMap<>();
	@Getter @ExcludeFromReplies @Setter private String totpSecret;
	@Getter @ExcludeFromReplies private String password;
	@Getter @ExcludeFromReplies private String passwordResetToken;
	@Getter @ExcludeFromReplies private Instant passwordResetTokenSetAt;
	@Getter private String email;
	@Getter private Instant registeredAt;
	@Getter @ExcludeFromReplies private String pendingEmail;
	@Getter @ExcludeFromReplies private String pendingEmailToken;
	@Getter @ExcludeFromReplies private Instant pendingEmailTokenSetAt;
	@Getter private String phone;
	@Getter private Instant phoneRegisteredAt;
	@Getter @ExcludeFromReplies private String pendingPhone;
	@Getter @ExcludeFromReplies private String pendingPhoneToken;
	@Getter @ExcludeFromReplies private Instant pendingPhoneTokenSetAt;
	@Getter @ExcludeFromReplies private Instant pendingPhoneTokenVerificationAttemptedAt;
	@Getter @ExcludeFromReplies private Set<Instant> phoneVerificationFailedAttempts;
	@Getter private String lastSeenOn;
	@Getter private Instant lastSeenAt;
	@Getter private Instant firstSeenAt;
	@Getter private boolean online;
	@Getter private String activePrefix;
	@Getter private String iconColor;
	@Getter private String nameColor;

	public static void findAll(SingleResultCallback<List<User>> callback) {
		usersCollection.find().sort(new Document("lastSeenAt", -1)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findAllWithTotpSetup(SingleResultCallback<List<User>> callback) {
		usersCollection.find(new Document("totpSecret", new Document("$exists", true))).sort(new Document("lastSeenAt", -1)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<User> callback) {
		try {
			findById(UuidUtils.parseUuid(id), callback);
		} catch (NullPointerException | IllegalArgumentException ignored) { // from UUID parsing
			callback.onResult(null, null); // We don't pass in the exception, we just pretend we couldn't find them.
		}
	}

	public static void findById(UUID id, SingleResultCallback<User> callback) {
		if (UuidUtils.isAcceptableUuid(id)) {
			usersCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
		} else {
			callback.onResult(null, null);
		}
	}

	public static void findOrCreateById(UUID id, String username, SingleResultCallback<User> callback) {
		if (!UuidUtils.isAcceptableUuid(id)) {
			callback.onResult(null, null);
			return;
		}

		usersCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap((user, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (user != null) {
				callback.onResult(user, null);
				return;
			}

			User created = new User(id, username);

			created.checkNameCollisions((ignored, nameCollisionsError) -> {
				if (nameCollisionsError != null) {
					callback.onResult(null, nameCollisionsError);
					return;
				}

				created.insert((ignored2, insertUserError) -> {
					if (insertUserError != null) {
						callback.onResult(null, insertUserError);
					} else {
						callback.onResult(created, null);
					}
				});
			});
		}));
	}

	public static void findByPhone(String phoneNumber, SingleResultCallback<User> callback) {
		String e164Phone = PhoneUtils.toE164(phoneNumber);

		if (e164Phone == null) {
			callback.onResult(null, null);
		} else {
			usersCollection.find(new Document("$or", ImmutableList.of(
					new Document("phone", phoneNumber),
					new Document("pendingPhone", phoneNumber)
			))).first(SyncUtils.vertxWrap(callback));
		}
	}

	public static void findByConfirmedEmail(String email, SingleResultCallback<User> callback) {
		usersCollection.find(new Document("email", email.toLowerCase())).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByPasswordResetToken(String passwordResetToken, SingleResultCallback<User> callback) {
		usersCollection.find(new Document("passwordResetToken", passwordResetToken)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByEmailToken(String emailToken, SingleResultCallback<User> callback) {
		usersCollection.find(new Document("pendingEmailToken", emailToken)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByLastUsername(String lastUsername, SingleResultCallback<User> callback) {
		usersCollection.find(new Document("lastUsername", lastUsername)).first(SyncUtils.vertxWrap(callback));
	}

    public static void findByLastUsernameLower(String lastUsernameLower, SingleResultCallback<User> callback) {
        usersCollection.find(new Document("lastUsernameLower", lastUsernameLower.toLowerCase())).first(SyncUtils.vertxWrap(callback));
    }

    public static void findByIdGrouped(Set<UUID> search, SingleResultCallback<Map<UUID, User>> callback) {
        usersCollection.find(new Document("_id", new Document("$in", search))).into(new LinkedList<>(), SyncUtils.vertxWrap((users, error) -> {
            if (error != null) {
                callback.onResult(null, error);
                return;
            }

            Map<UUID, User> result = new HashMap<>();

            for (User user : users) {
                result.put(user.getId(), user);
            }

            callback.onResult(result, null);
        }));
    }

	public static void findOrCreateByIdGrouped(Map<UUID, String> search, SingleResultCallback<Map<UUID, User>> callback) {
		usersCollection.find(new Document("_id", new Document("$in", search.keySet()))).into(new LinkedList<>(), SyncUtils.vertxWrap((users, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			Map<UUID, User> result = new ConcurrentHashMap<>();

			for (User user : users) {
				result.put(user.getId(), user);
			}

			List<Future> createNewUserFutures = new ArrayList<>();

			search.forEach((uuid, username) -> {
				if (result.containsKey(uuid) || !UuidUtils.isAcceptableUuid(uuid)) {
					return;
				}

				Future createNewUserFuture = Future.future();
				createNewUserFutures.add(createNewUserFuture);

				User created = new User(uuid, username);
				created.checkNameCollisions((ignored, error2) -> {
					if (error2 != null) {
						createNewUserFuture.fail(error2);
						return;
					}

					created.insert((ignored2, error3) -> {
						if (error3 != null) {
							createNewUserFuture.fail(error3);
							return;
						}

						result.put(uuid, created);
						createNewUserFuture.complete();
					});
				});
			});

			CompositeFuture.all(createNewUserFutures).setHandler((creationStatus) -> {
				if (creationStatus.failed()) {
					callback.onResult(null, creationStatus.cause());
				} else {
					callback.onResult(result, null);
				}
			});
		}));
	}

	private User() {} // For Jackson

	public User(UUID id, String lastUsername) {
		this.id = id;
		this.lastUsername = lastUsername;
        this.lastUsernameLower = lastUsername.toLowerCase();
		this.aliases = new HashMap<>();
		this.lastSeenAt = Instant.now();
		this.firstSeenAt = Instant.now();

		this.aliases.put(lastUsername, Instant.now());
	}

	public void updateUsername(String newUsername) {
		this.aliases.put(newUsername, Instant.now());
		this.lastUsername = newUsername;
        this.lastUsernameLower = newUsername.toLowerCase();
	}

	public void checkNameCollisions(SingleResultCallback<Void> callback) {
		User.findByLastUsername(lastUsername, (collision, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (collision == null) {
				callback.onResult(null, null);
				return;
			}

			SpringUtils.getBean(MojangService.class).getName(collision.getId(), (collisionNewUsername, error2) -> {
				if (error2 != null) {
					callback.onResult(null, error2);
					return;
				}

				collision.updateUsername(collisionNewUsername);
				collision.checkNameCollisions((ignored, error3) -> {
					if (error3 != null) {
						callback.onResult(null, error3);
						return;
					}

					collision.save((ignored2, error4) -> {
						if (error4 != null) {
							callback.onResult(null, error4);
						} else {
							callback.onResult(null, null);
						}
					});
				});
			});
		});
	}

	public void getLoginInfo(Server server, String userIp, SingleResultCallback<Map<String, Object>> callback) {
		Future<List<Punishment>> punishmentsFuture = Future.future();
		Future<List<IpBan>> ipBansFuture = Future.future();
		Future<List<Grant>> grantsFuture = Future.future();
		Future<List<PrefixGrant>> prefixGrantsFuture = Future.future();

		Punishment.findByUserAndType(this, ImmutableSet.of(
				Punishment.PunishmentType.BLACKLIST,
				Punishment.PunishmentType.BAN,
				Punishment.PunishmentType.MUTE
		), new MongoToVertxCallback<>(punishmentsFuture));

		if (userIp != null) {
			IpBan.findByIp(userIp, new MongoToVertxCallback<>(ipBansFuture));
		} else {
			ipBansFuture.complete(ImmutableList.of());
		}

		Grant.findByUser(this, new MongoToVertxCallback<>(grantsFuture));
		PrefixGrant.findByUser(this, new MongoToVertxCallback<>(prefixGrantsFuture));

		CompositeFuture.all(punishmentsFuture, ipBansFuture, grantsFuture, prefixGrantsFuture).setHandler((result) -> {
			if (result.failed()) {
				callback.onResult(null, result.cause());
				return;
			}

			Iterable<Punishment> punishments = result.result().result(0);
			Iterable<IpBan> ipBans = result.result().result(1);
			Iterable<Grant> grants = result.result().result(2);
			Iterable<PrefixGrant> prefixGrants = result.result().result(3);

			getLoginInfo(server, punishments, ipBans, grants, prefixGrants, (loginInfo, error) -> {
				if (error != null) {
					callback.onResult(null, error);
				} else {
					callback.onResult(loginInfo, null);
				}
			});
		});
	}

	// This is only used to help batch requests to mongo
	public void getLoginInfo(Server server, Iterable<Punishment> punishments, Iterable<IpBan> ipBans, Iterable<Grant> grants, Iterable<PrefixGrant> prefixGrants, SingleResultCallback<Map<String, Object>> callback) {
		getAccessInfo(punishments, ipBans, (accessInfo, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			ServerGroup serverGroup = ServerGroup.findById(server.getServerGroup());
			Punishment activeMute = null;

			for (Punishment punishment : punishments) {
				if (punishment.isActive() && punishment.getType() == Punishment.PunishmentType.MUTE) {
					activeMute = punishment;
					break;
				}
			}

			Map<String, Object> result = new HashMap<>();
			List<Rank> ranks = getRanksScoped(serverGroup, grants);

			// Currently, we exclude this from login info. We do still do the relevant
			// db query, but we don't include the result because it's not used in
			// Hydrogen at all (bnut probably will be later on)
			// result.put("user", this);
			result.put("ranks", Collections2.transform(ranks, Rank::getId));

			Map<String, Collection<String>> scopeRanks = new HashMap<>();

			for (ServerGroup scope : ServerGroup.findAll()) {
				List<Rank> ranksOnScope = getRanksScoped(scope, grants);
				scopeRanks.put(scope.getId(), Collections2.transform(ranksOnScope, Rank::getId));
			}

			result.put("scopeRanks", scopeRanks);

			if (activeMute != null) {
				result.put("mute", activeMute);
			}

			if (accessInfo != null) {
				result.put("access", accessInfo);
			}

			if (iconColor != null) {
				result.put("iconColor", iconColor);
			}

			if (nameColor != null) {
				result.put("nameColor", nameColor);
			}

			List<Prefix> prefixes = getPrefixesScoped(serverGroup, prefixGrants);
			if (!prefixes.isEmpty()) {
				result.put("prefixes", Collections2.transform(prefixes, Prefix::getId));
			}

			if (activePrefix != null) {
				result.put("activePrefix", activePrefix);
			}

			callback.onResult(result, null);
		});
	}

	private void getAccessInfo(Iterable<Punishment> punishments, Iterable<IpBan> ipBans, SingleResultCallback<Map<String, Object>> callback) {
		Punishment activeBan = null;
		IpBan activeIpBan = null;

		for (Punishment punishment : punishments) {
			if (punishment.isActive() && (punishment.getType() == Punishment.PunishmentType.BAN || punishment.getType() == Punishment.PunishmentType.BLACKLIST)) {
				activeBan = punishment;
				break;
			}
		}

		for (IpBan ipBan : ipBans) {
			if (ipBan.isActive()) {
				activeIpBan = ipBan;
				break;
			}
		}

		if (activeBan != null) {
			callback.onResult(ImmutableMap.of(
					"allowed", false,
					"message", activeBan.getAccessDenialReason()
			), null);
		} else if (activeIpBan != null) {
			activeIpBan.getAccessDenialReason((denialReason, error) -> {
				if (error != null) {
					callback.onResult(null, error);
				} else {
					callback.onResult(ImmutableMap.of(
							"allowed", false,
							"message", denialReason
					), null);
				}
			});
		} else {
			callback.onResult(null, null);
		}
	}

	public boolean seenOnServer(Server server) {
		if (online && server.getId().equals(lastSeenOn)) {
			return false;
		}

		this.lastSeenOn = server.getId();
		this.lastSeenAt = Instant.now();
		this.online = true;
		return true;
	}

	// Returns if any change was actually made.
	public boolean leftServer(Server server) {
		// We have this check to prevent issues where one server's
		// leave is processed after another server's login.
		if (server.getId().equals(lastSeenOn)) {
			this.lastSeenAt = Instant.now();
			this.online = false;
			return true;
		} else {
			return false;
		}
	}

	public void startPasswordReset() {
		this.passwordResetToken = UUID.randomUUID().toString().replaceAll("-", "");
		this.passwordResetTokenSetAt = Instant.now();
	}

	public void updatePassword(String password) {
		this.passwordResetToken = null;
		this.passwordResetTokenSetAt = null;
		this.password = Hashing
				.sha256()
				.hashString(password + "$" + id.toString(), Charsets.UTF_8)
				.toString();
	}

	public boolean checkPassword(String input) {
		String hashed = Hashing
				.sha256()
				.hashString(input + "$" + id.toString(), Charsets.UTF_8)
				.toString();

		return password != null && input != null && hashed.equals(password);
	}

	public void requiresTotpAuthorization(String ip, SingleResultCallback<RequiresTotpResult> callback) {
		if (totpSecret == null) {
			callback.onResult(RequiresTotpResult.NOT_REQUIRED_NOT_SET, null);
			return;
		}

		if (!IpUtils.isValidIp(ip)) {
			callback.onResult(RequiresTotpResult.REQUIRED_NO_EXEMPTIONS, null);
			return;
		}

		TotpService totpService = SpringUtils.getBean(TotpService.class);

		totpService.isPreAuthorized(this, ip, (ipPreAuth, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else if (ipPreAuth) {
				callback.onResult(RequiresTotpResult.NOT_REQUIRED_IP_PRE_AUTHORIZED, null);
			} else {
				callback.onResult(RequiresTotpResult.REQUIRED_NO_EXEMPTIONS, null);
			}
		});
	}

	public void checkTotpAuthorization(int code, String ip, SingleResultCallback<TotpAuthorizationResult> callback) {
		if (totpSecret == null) {
			hasPermissionAnywhere(Permissions.REQUIRE_TOTP_CODE, (totpRequired, error) -> {
				if (error != null) {
					callback.onResult(null, error);
				} else {
					TotpAuthorizationResult result = totpRequired ? TotpAuthorizationResult.NOT_AUTHORIZED_NOT_SET : TotpAuthorizationResult.AUTHORIZED_NOT_SET;
					callback.onResult(result, null);
				}
			});

			return;
		}

		TotpService totpService = SpringUtils.getBean(TotpService.class);

		totpService.isPreAuthorized(this, ip, (preAuthorized, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (preAuthorized) {
				callback.onResult(TotpAuthorizationResult.AUTHORIZED_IP_PRE_AUTH, null);
				return;
			}

			totpService.wasRecentlyUsed(this, code, (recentlyUsed, error2) -> {
				if (error2 != null) {
					callback.onResult(null, error2);
					return;
				}

				if (recentlyUsed) {
					callback.onResult(TotpAuthorizationResult.NOT_AUTHORIZED_RECENTLY_USED, null);
					return;
				}

				if (!totpService.authorizeUser(totpSecret, code)) {
					callback.onResult(TotpAuthorizationResult.NOT_AUTHORIZED_BAD_CODE, null);
					return;
				}

				Future<Void> markPreAuthFuture = Future.future();
				Future<Void> markRecentlyUsedFuture = Future.future();

				totpService.markPreAuthorized(this, ip, new MongoToVertxCallback<>(markPreAuthFuture));
				totpService.markRecentlyUsed(this, code, new MongoToVertxCallback<>(markRecentlyUsedFuture));

				CompositeFuture.all(markPreAuthFuture, markRecentlyUsedFuture).setHandler((result) -> {
					if (result.failed()) {
						callback.onResult(null, result.cause());
					} else {
						callback.onResult(TotpAuthorizationResult.AUTHORIZED_GOOD_CODE, null);
					}
				});
			});
		});
	}

	public void startEmailRegistration(String pendingEmail) {
		this.pendingEmail = pendingEmail.toLowerCase();
		this.pendingEmailToken = UUID.randomUUID().toString().replace("-", "");
		this.pendingEmailTokenSetAt = Instant.now();
	}

	public void completeEmailRegistration(String email) {
		this.email = email.toLowerCase();
		this.registeredAt = Instant.now();
		this.pendingEmail = null;
		this.pendingEmailToken = null;
		this.pendingEmailTokenSetAt = null;
	}

	public void startPhoneRegistration(String phoneNumber) {
		String e164Phone = PhoneUtils.toE164(phoneNumber);

		if (e164Phone == null) return;

		this.pendingPhone = e164Phone;
		this.pendingPhoneToken = String.valueOf(new Random().nextInt(999999 - 100000) + 100000);
		this.pendingPhoneTokenSetAt = Instant.now();
	}

	public void failedPhoneRegistration() {
		this.pendingPhoneTokenVerificationAttemptedAt = Instant.now();
		this.phoneVerificationFailedAttempts.add(Instant.now());
	}

	public void completePhoneRegistration(String phoneNumber) {
		this.phone = phoneNumber;
		this.phoneRegisteredAt = Instant.now();
		this.pendingPhone = null;
		this.pendingPhoneToken = null;
		this.pendingPhoneTokenSetAt = null;
		this.pendingPhoneTokenVerificationAttemptedAt = null;
		this.phoneVerificationFailedAttempts = null;
	}

	public void updateColors(String iconColor, String nameColor) {
		this.iconColor = iconColor;
		this.nameColor = nameColor;
	}

	public void updateActivePrefix(String prefix) {
		this.activePrefix = prefix;
	}

	public void hasPermissionAnywhere(String permission, SingleResultCallback<Boolean> callback) {
		getCompoundedPermissions((permissions, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else {
				boolean hasPermission = hasPermission(permission, permissions);
				callback.onResult(hasPermission, null);
			}
		});
	}

	private boolean hasPermission(String permission, Map<String, Boolean> permissions) {
		if (permissions.containsKey(permission) && permissions.get(permission)) {
			return true;
		}

		int lastDot = permission.lastIndexOf(".");

		if (lastDot == -1) {
			return false;
		}

		String modifiedPermission = permission.substring(0, lastDot) + ".*";
		return permissions.containsKey(modifiedPermission) && permissions.get(modifiedPermission);
	}

	public void getCompoundedPermissions(SingleResultCallback<Map<String, Boolean>> callback) {
		Grant.findByUser(this, (grants, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else {
				Map<String, Boolean> compoundedPermissions = new HashMap<>();

				for (Rank globalRank : getRanksScoped(null, grants)) {
					compoundedPermissions = PermissionUtils.mergePermissions(
							compoundedPermissions,
							ServerGroup.findDefault().calculatePermissions(globalRank)
					);
				}

				for (ServerGroup serverGroup : ServerGroup.findAll()) {
					for (Rank scopedRank : getRanksScoped(serverGroup, grants)) {
						compoundedPermissions = PermissionUtils.mergePermissions(
								compoundedPermissions,
								serverGroup.calculatePermissions(scopedRank)
						);
					}
				}

				callback.onResult(ImmutableMap.copyOf(compoundedPermissions), null);
			}
		});
	}

	private List<Rank> getRanksScoped(ServerGroup serverGroup, Iterable<Grant> grants) {
		Set<Rank> grantedRanks = new HashSet<>();

		for (Grant grant : grants) {
			if (!grant.isActive() || (serverGroup != null && !grant.appliesOn(serverGroup))) {
				continue;
			}

			Rank grantedRank = Rank.findById(grant.getRank());

			if (grantedRank != null) {
				grantedRanks.add(grantedRank);
			} else {
				log.warn(lastUsername + " (" + id + ") has a grant for a non-existant rank: " + grant.getRank());
			}
		}

		grantedRanks.add(Rank.findById("default"));

		if (registeredAt != null) {
			grantedRanks.add(Rank.findById("registered"));
		}

		// This is to remove redundant ranks. Say they have mod, mod-plus, admin, and youtuber,
		// we should remove mod and mod-plus as it'll be made redundant by the higher ranked admin.
		for (Rank grantedRank : ImmutableSet.copyOf(grantedRanks)) {
			// Check all other ranks for inherited collision
			for (Rank otherRank : ImmutableSet.copyOf(grantedRanks)) {
				if (grantedRank == otherRank) {
					continue;
				}

				Rank parent = otherRank;

				// Iterate up the inheritance tree to detect rank redundancies.
				while (parent.getInheritsFromId() != null) {
					if (parent == grantedRank) {
						grantedRanks.remove(grantedRank);
					}

					parent = Rank.findById(parent.getInheritsFromId());
				}
			}
		}

		List<Rank> grantedRanksList = new ArrayList<>(grantedRanks);
		grantedRanksList.sort((a, b) -> Ints.compare(b.getGeneralWeight(), a.getGeneralWeight()));
		return ImmutableList.copyOf(grantedRanksList);
	}

	private List<Prefix> getPrefixesScoped(ServerGroup serverGroup, Iterable<PrefixGrant> grants) {
		Set<Prefix> grantedPrefixes = new HashSet<>();

		for (PrefixGrant grant : grants) {
			if (!grant.isActive() || (serverGroup != null && !grant.appliesOn(serverGroup))) {
				continue;
			}

			Prefix grantedPrefix = Prefix.findById(grant.getPrefix());

			if (grantedPrefix != null) {
				grantedPrefixes.add(grantedPrefix);
			} else {
				log.warn(lastUsername + " (" + id + ") has a grant for a non-existant rank: " + grant.getPrefix());
			}
		}

		List<Prefix> grantedRanksList = new ArrayList<>(grantedPrefixes);
		return ImmutableList.copyOf(grantedRanksList);
	}

	public void insert(SingleResultCallback<Void> callback) {
		usersCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		usersCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}
