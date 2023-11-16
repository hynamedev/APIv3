package net.frozenorb.apiv3.domain;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.TimeUtils;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@AllArgsConstructor
public final class Punishment {

	private static final MongoCollection<Punishment> punishmentsCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("punishments", Punishment.class);

	@Getter @Id private String id;
	@Getter private UUID user;
	@Getter private String publicReason;
	@Getter private String privateReason;
	@Getter private PunishmentType type;
	@Getter private Instant expiresAt;
	@Getter private Map<String, Object> metadata;
	@Getter private String linkedIpBanId;

	@Getter private UUID addedBy;
	@Getter private Instant addedAt;
	@Getter private String actorName;
	@Getter private ActorType actorType;

	@Getter private UUID removedBy;
	@Getter private Instant removedAt;
	@Getter private String removalReason;

	public static void findByType(Collection<PunishmentType> types, SingleResultCallback<List<Punishment>> callback) {
		Collection<String> convertedTypes = types.stream().map(PunishmentType::name).collect(Collectors.toList());
		punishmentsCollection.find(new Document("type", new Document("$in", convertedTypes))).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findPaginated(Document query, int skip, int pageSize, UUID addedBy, SingleResultCallback<List<Punishment>> callback) {
		if (addedBy != null) {
			query.put("addedBy", addedBy);
		}

		punishmentsCollection.find(query).sort(new Document("addedAt", -1)).skip(skip).limit(pageSize).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<Punishment> callback) {
		punishmentsCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByLinkedIpBanId(String id, SingleResultCallback<Punishment> callback) {
		punishmentsCollection.find(new Document("linkedIpBanId", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByUser(User user, SingleResultCallback<List<Punishment>> callback) {
		findByUser(user.getId(), callback);
	}

	public static void findByUser(UUID user, SingleResultCallback<List<Punishment>> callback) {
		punishmentsCollection.find(new Document("user", user)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findByUserGrouped(Iterable<UUID> users, SingleResultCallback<Map<UUID, List<Punishment>>> callback) {
		punishmentsCollection.find(new Document("user", new Document("$in", users))).into(new LinkedList<>(), SyncUtils.vertxWrap((punishments, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else {
				Map<UUID, List<Punishment>> result = new HashMap<>();

				for (UUID user : users) {
					result.put(user, new LinkedList<>());
				}

				for (Punishment punishment : punishments) {
					result.get(punishment.getUser()).add(punishment);
				}

				callback.onResult(result, null);
			}
		}));
	}

	public static void findByUserAndType(User user, Collection<PunishmentType> types, SingleResultCallback<List<Punishment>> callback) {
		findByUserAndType(user.getId(), types, callback);
	}

	public static void findByUserAndType(UUID user, Collection<PunishmentType> types, SingleResultCallback<List<Punishment>> callback) {
		Collection<String> convertedTypes = types.stream().map(PunishmentType::name).collect(Collectors.toList());
		punishmentsCollection.find(new Document("user", user).append("type", new Document("$in", convertedTypes))).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	private Punishment() {} // For Jackson

	public Punishment(User user, String publicReason, String privateReason, PunishmentType type, Instant expiresAt, User addedBy, Actor actor, Map<String, Object> metadata) {
		this.id = new ObjectId().toString();
		this.user = user.getId();
		this.publicReason = publicReason;
		this.privateReason = privateReason;
		this.type = type;
		this.expiresAt = expiresAt;
		this.addedBy = addedBy == null ? null : addedBy.getId();
		this.addedAt = Instant.now();
		this.actorName = actor.getName();
		this.actorType = actor.getType();
		this.metadata = metadata;
	}

	public boolean isActive() {
		return !(isExpired() || isRemoved());
	}

	public boolean isExpired() {
		return expiresAt != null && expiresAt.isBefore(Instant.now());
	}

	public boolean isRemoved() {
		return removedAt != null;
	}

	public String getAccessDenialReason() {
		switch (type) {
			case BLACKLIST:
				return "Your account has been blacklisted from the " + SpringUtils.getProperty("network.name") + ". \n\nThis type of punishment cannot be appealed.";
			case BAN:
				String accessDenialReason = "Your account has been suspended from the " + SpringUtils.getProperty("network.name") + ". \n\n";

				if (getExpiresAt() != null) {
					accessDenialReason += "Expires in " + TimeUtils.formatIntoDetailedString(TimeUtils.getSecondsBetween(getExpiresAt(), Instant.now()));
				} else {
					accessDenialReason += "Appeal at " + SpringUtils.getProperty("network.appealUrl");
				}

				return accessDenialReason;
			default:
				return null;
		}
	}

	public void linkIpBan(IpBan ipBan) {
		this.linkedIpBanId = ipBan.getId();
	}

	public void insert(SingleResultCallback<Void> callback) {
		punishmentsCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(User removedBy, String reason, SingleResultCallback<Void> callback) {
		this.removedBy = removedBy == null ? null : removedBy.getId();
		this.removedAt = Instant.now();
		this.removalReason = reason;

		if (linkedIpBanId == null) {
			punishmentsCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
			return;
		}

		IpBan.findById(linkedIpBanId, (ipBan, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (ipBan != null && ipBan.isActive()) {
				ipBan.delete(removedBy, "Linked punishment removed.",  (ignored, error2) -> {
					if (error2 != null) {
						callback.onResult(null, error2);
					} else {
						punishmentsCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
					}
				});
			} else {
				punishmentsCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
			}
		});
	}

	public enum PunishmentType {

		BLACKLIST, BAN, MUTE, WARN

	}

}
