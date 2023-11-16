package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableMap;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.Getter;

@Entity
public final class AuditLogEntry {

	private static final MongoCollection<AuditLogEntry> auditLogCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("auditLog", AuditLogEntry.class);

	@Getter @Id private String id;
	@Getter private UUID user;
	@Getter private String userIp;
	@Getter private Instant performedAt;
	@Getter private String actorName;
	@Getter private ActorType actorType;
	@Getter private String actorIp;
	// We store 'reversible'  in each object in case later on we go back and
	// make something reversible (by storing more meta or such)
	@Getter private boolean reversible;
	@Getter private AuditLogActionType type;
	@Getter private Map<String, Object> metadata;

	public static void findPaginated(Document query, int skip, int pageSize, SingleResultCallback<List<AuditLogEntry>> callback) {
		auditLogCollection.find(query).sort(new Document("performedAt", -1)).skip(skip).limit(pageSize).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<AuditLogEntry> callback) {
		auditLogCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void find(Document query, SingleResultCallback<List<AuditLogEntry>> callback) {
		auditLogCollection.find(query).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	private AuditLogEntry() {} // For Jackson

	public AuditLogEntry(UUID user, String userIp, Actor actor, String actorIp, AuditLogActionType type, Map<String, Object> metadata) {
		this.id = new ObjectId().toString();
		this.user = user;
		this.userIp = userIp;
		this.performedAt = Instant.now();
		this.actorName = actor.getName();
		this.actorType = actor.getType();
		this.actorIp = actorIp;
		this.reversible = type.isReversible();
		this.type = type;
		this.metadata = ImmutableMap.copyOf(metadata);
	}

	public void insert(SingleResultCallback<Void> callback) {
		auditLogCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

}