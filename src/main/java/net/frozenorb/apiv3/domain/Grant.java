package net.frozenorb.apiv3.domain;

import com.google.common.collect.Collections2;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@AllArgsConstructor
public final class Grant {

	private static final MongoCollection<Grant> grantsCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("grants", Grant.class);

	@Getter @Id private String id;
	@Getter private UUID user;
	@Getter private String reason;
	@Getter private Set<String> scopes;
	@Getter private String rank;
	@Getter private Instant expiresAt;

	@Getter private UUID addedBy;
	@Getter private Instant addedAt;

	@Getter private UUID removedBy;
	@Getter private Instant removedAt;
	@Getter private String removalReason;

	@Getter private int storeItemId;
	@Getter private int storeOrderId;

	public static void findAll(SingleResultCallback<List<Grant>> callback) {
		grantsCollection.find().sort(new Document("addedAt", -1)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findByRank(Collection<Rank> ranks, SingleResultCallback<List<Grant>> callback) {
		Collection<String> convertedRanks = ranks.stream().map(Rank::getId).collect(Collectors.toList());
		grantsCollection.find(new Document("rank", new Document("$in", convertedRanks))).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findPaginated(Document query, int skip, int pageSize, SingleResultCallback<List<Grant>> callback) {
		grantsCollection.find(query).sort(new Document("addedAt", -1)).skip(skip).limit(pageSize).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<Grant> callback) {
		grantsCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByUser(User user, SingleResultCallback<List<Grant>> callback) {
		findByUser(user.getId(), callback);
	}

	public static void findByUser(UUID user, SingleResultCallback<List<Grant>> callback) {
		grantsCollection.find(new Document("user", user)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findByUserGrouped(Iterable<UUID> users, SingleResultCallback<Map<UUID, List<Grant>>> callback) {
		grantsCollection.find(new Document("user", new Document("$in", users))).into(new LinkedList<>(), SyncUtils.vertxWrap((grants, error) -> {
			if (error != null) {
				callback.onResult(null, error);
			} else {
				Map<UUID, List<Grant>> result = new HashMap<>();

				for (UUID user : users) {
					result.put(user, new LinkedList<>());
				}

				for (Grant grant : grants) {
					result.get(grant.getUser()).add(grant);
				}

				callback.onResult(result, null);
			}
		}));
	}

	private Grant() {} // For Jackson

	public Grant(User user, String reason, Set<ServerGroup> scopes, Rank rank, Instant expiresAt, User addedBy) {
		this(user, reason, scopes, rank, expiresAt, addedBy, -1, -1);
	}

	public Grant(User user, String reason, Set<ServerGroup> scopes, Rank rank, Instant expiresAt, User addedBy, int storeItemId, int storeOrderId) {
		this.id = new ObjectId().toString();
		this.user = user.getId();
		this.reason = reason;
		this.scopes = new HashSet<>(Collections2.transform(scopes, ServerGroup::getId));
		this.rank = rank.getId();
		this.expiresAt = expiresAt;
		this.addedBy = addedBy == null ? null : addedBy.getId();
		this.addedAt = Instant.now();
		this.storeItemId = storeItemId;
		this.storeOrderId = storeOrderId;
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

	public boolean appliesOn(ServerGroup serverGroup) {
		return isGlobal() || scopes.contains(serverGroup.getId());
	}

	public boolean isGlobal() {
		return scopes.isEmpty();
	}

	public void insert(SingleResultCallback<Void> callback) {
		grantsCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(User removedBy, String reason, SingleResultCallback<Void> callback) {
		this.removedBy = removedBy == null ? null : removedBy.getId();
		this.removedAt = Instant.now();
		this.removalReason = reason;

		grantsCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}