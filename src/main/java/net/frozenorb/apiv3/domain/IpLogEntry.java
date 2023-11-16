package net.frozenorb.apiv3.domain;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@AllArgsConstructor
public final class IpLogEntry {

	private static final MongoCollection<IpLogEntry> ipLogCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("ipLog", IpLogEntry.class);

	@Getter @Id private String id;
	@Getter private UUID user;
	@Getter private String userIp;
	@Getter private String hashedUserIp;
	@Getter private Instant firstSeenAt;
	@Getter private Instant lastSeenAt;
	@Getter private int uses;

	public static void findAll(SingleResultCallback<List<IpLogEntry>> callback) {
		ipLogCollection.find().sort(new Document("lastSeenAt", -1)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<IpLogEntry> callback) {
		ipLogCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByUser(User user, SingleResultCallback<List<IpLogEntry>> callback) {
		findByUser(user.getId(), callback);
	}

	public static void findByUser(UUID user, SingleResultCallback<List<IpLogEntry>> callback) {
		ipLogCollection.find(new Document("user", user)).sort(new Document("lastSeenAt", -1)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findLatestByUser(UUID user, SingleResultCallback<IpLogEntry> callback) {
		ipLogCollection.find(new Document("user", user)).sort(new Document("lastSeenAt", -1)).limit(1).first(SyncUtils.vertxWrap(callback));
	}

	public static void findByIp(String userIp, SingleResultCallback<List<IpLogEntry>> callback) {
		ipLogCollection.find(new Document("userIp", userIp)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findByHashedIp(String hashedUserIp, SingleResultCallback<List<IpLogEntry>> callback) {
		ipLogCollection.find(new Document("hashedUserIp", hashedUserIp)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findByUserAndIp(User user, String userIp, SingleResultCallback<IpLogEntry> callback) {
		findByUserAndIp(user.getId(), userIp, callback);
	}

	public static void findByUserAndIp(UUID user, String userIp, SingleResultCallback<IpLogEntry> callback) {
		ipLogCollection.find(new Document("user", user).append("userIp", userIp)).first(SyncUtils.vertxWrap(callback));
	}

	private IpLogEntry() {} // For Jackson

	public IpLogEntry(User user, String userIp) {
		this.id = new ObjectId().toString();
		this.user = user.getId();
		this.userIp = userIp;
		this.hashedUserIp = Hashing.sha256().hashString(userIp + SpringUtils.getProperty("ipHashing.salt"), Charsets.UTF_8).toString();
		this.firstSeenAt = Instant.now();
		this.lastSeenAt = Instant.now();
		this.uses = 0;
	}

	public void used() {
		this.lastSeenAt = Instant.now();
		this.uses++;
	}

	public void insert(SingleResultCallback<Void> callback) {
		ipLogCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		ipLogCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}