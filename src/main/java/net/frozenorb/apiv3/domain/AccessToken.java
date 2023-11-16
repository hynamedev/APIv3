package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableList;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@AllArgsConstructor
public final class AccessToken {

	private static final MongoCollection<AccessToken> accessTokensCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("accessTokens", AccessToken.class);

	@Getter @Id private String id;
	@Getter private String actorName;
	@Getter private ActorType actorType;
	@Getter @Setter private List<String> lockedIps;
	@Getter private Instant createdAt;
	@Getter @Setter private Instant lastUpdatedAt;

	public static void findAll(SingleResultCallback<List<AccessToken>> callback) {
		accessTokensCollection.find().into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<AccessToken> callback) {
		accessTokensCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByNameAndType(String actorName, ActorType actorType, SingleResultCallback<AccessToken> callback) {
		accessTokensCollection.find(new Document("actorName", actorName).append("actorType", actorType.name())).first(SyncUtils.vertxWrap(callback));
	}

	private AccessToken() {} // For Jackson

	public AccessToken(Server server) {
		// Can't extract server host code to another line because the call to another constructor must be on the first line.
		this(server.getId(), ActorType.SERVER, ImmutableList.of(server.getServerIp().split(":")[0]));
	}

	public AccessToken(String actorName, ActorType actorType, List<String> lockedIps) {
		this.id = UUID.randomUUID().toString().replace("-", "");
		this.actorName = actorName;
		this.actorType = actorType;
		this.lockedIps = lockedIps;
		this.createdAt = Instant.now();
		this.lastUpdatedAt = Instant.now();
	}

	public void insert(SingleResultCallback<Void> callback) {
		accessTokensCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		accessTokensCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

	public void delete(SingleResultCallback<Void> callback) {
		accessTokensCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}