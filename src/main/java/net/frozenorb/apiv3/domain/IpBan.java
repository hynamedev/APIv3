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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@AllArgsConstructor
public final class IpBan {

	private static final MongoCollection<IpBan> ipBansCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("ipBans", IpBan.class);

	@Getter @Id private String id;
	@Getter private String userIp;
	@Getter private String reason;
	@Getter private Instant expiresAt;
	@Getter private String linkedPunishmentId;

	@Getter private UUID addedBy;
	@Getter private Instant addedAt;
	@Getter private String actorName;
	@Getter private ActorType actorType;

	@Getter private UUID removedBy;
	@Getter private Instant removedAt;
	@Getter private String removalReason;

	public static void find(SingleResultCallback<List<IpBan>> callback) {
		ipBansCollection.find().into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findPaginated(Document query, int skip, int pageSize, SingleResultCallback<List<IpBan>> callback) {
		ipBansCollection.find(query).sort(new Document("addedAt", -1)).skip(skip).limit(pageSize).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<IpBan> callback) {
		ipBansCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByIp(String userIp, SingleResultCallback<List<IpBan>> callback) {
		ipBansCollection.find(new Document("userIp", userIp)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findByIpGrouped(Iterable<String> userIps, SingleResultCallback<Map<String, List<IpBan>>> callback) {
		ipBansCollection.find(new Document("userIp", new Document("$in", userIps))).into(new LinkedList<>(), SyncUtils.vertxWrap((ipBans, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else {
				Map<String, List<IpBan>> result = new HashMap<>();

				for (String userIp : userIps) {
					result.put(userIp, new LinkedList<>());
				}

				for (IpBan ipBan : ipBans) {
					result.get(ipBan.getUserIp()).add(ipBan);
				}

				callback.onResult(result, null);
			}
		}));
	}

	private IpBan() {} // For Jackson

	public IpBan(String userIp, Punishment linked) {
		this.id = new ObjectId().toString();
		this.userIp = userIp;
		this.reason = linked.getPublicReason();
		this.expiresAt = linked.getExpiresAt();
		this.linkedPunishmentId = linked.getId();
		this.addedBy = linked.getAddedBy();
		this.addedAt = Instant.now();
		this.actorName = linked.getActorName();
		this.actorType = linked.getActorType();
	}

	public IpBan(String userIp, String reason, Instant expiresAt, User addedBy, Actor actor) {
		this.id = new ObjectId().toString();
		this.userIp = userIp;
		this.reason = reason;
		this.expiresAt = expiresAt;
		this.addedBy = addedBy == null ? null : addedBy.getId();
		this.addedAt = Instant.now();
		this.actorName = actor.getName();
		this.actorType = actor.getType();
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

	public void getAccessDenialReason(SingleResultCallback<String> callback) {
		Punishment.findByLinkedIpBanId(id, (punishment, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (punishment != null) {
				User.findById(punishment.getUser(), (user, error2) -> {
					if (error2 != null) {
						callback.onResult(null, error2);
					} else {
						callback.onResult(buildDenialReason(user), null);
					}
				});
			} else {
				callback.onResult(buildDenialReason(null), null);
			}
		});
	}

	private String buildDenialReason(User linkedIpBanUser) {
		String accessDenialReason;

		if (linkedIpBanUser != null) {
			accessDenialReason = "Your IP address has been suspended from the " + SpringUtils.getProperty("network.name") + " for a punishment related to " + linkedIpBanUser.getLastUsername() + ". \n\n";
		} else {
			accessDenialReason = "Your IP address has been suspended from the " + SpringUtils.getProperty("network.name") + ". \n\n";
		}

		if (getExpiresAt() != null) {
			accessDenialReason += "Expires in " + TimeUtils.formatIntoDetailedString(TimeUtils.getSecondsBetween(getExpiresAt(), Instant.now()));
		} else {
			accessDenialReason += "Appeal at " + SpringUtils.getProperty("network.appealUrl");
		}

		return accessDenialReason;
	}

	public void insert(SingleResultCallback<Void> callback) {
		ipBansCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(User removedBy, String reason, SingleResultCallback<Void> callback) {
		this.removedBy = removedBy == null ? null : removedBy.getId();
		this.removedAt = Instant.now();
		this.removalReason = reason;

		ipBansCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}