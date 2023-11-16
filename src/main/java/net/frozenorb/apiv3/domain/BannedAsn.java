package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableList;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;

@Entity
public final class BannedAsn {

	private static final MongoCollection<BannedAsn> bannedAsnsCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("bannedAsns", BannedAsn.class);

	private static Map<Integer, BannedAsn> bannedAsnIdCache = null;
	private static List<BannedAsn> bannedAsnCache = null;

	@Getter @Id private int id;
	@Getter @Setter String note;
	@Getter private Instant bannedAt;
	@Getter private Instant lastUpdatedAt;

	public static List<BannedAsn> findAll() {
		return ImmutableList.copyOf(bannedAsnCache);
	}

	public static BannedAsn findById(int id) {
		return bannedAsnIdCache.get(id);
	}

	static {
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.MINUTES.toMillis(1), (id) -> updateCache());
	}

	public static void updateCache() {
		List<BannedAsn> bannedAsns = SyncUtils.runBlocking(v -> bannedAsnsCollection.find().into(new LinkedList<>(), v));
		Map<Integer, BannedAsn> working = new HashMap<>();

		for (BannedAsn bannedAsn : bannedAsns) {
			working.put(bannedAsn.getId(), bannedAsn);
		}

		bannedAsnIdCache = working;
		bannedAsnCache = bannedAsns;
	}

	private BannedAsn() {} // For Jackson

	public BannedAsn(int id, String note) {
		this.id = id;
		this.note = note;
		this.bannedAt = Instant.now();
		this.lastUpdatedAt = Instant.now();
	}

	public void updateNote(String newNote) {
		this.note = newNote;
		this.lastUpdatedAt = Instant.now();
	}

	public void insert(SingleResultCallback<Void> callback) {
		bannedAsnCache.add(this);
		bannedAsnIdCache.put(id, this);
		bannedAsnsCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		bannedAsnsCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

	public void delete(SingleResultCallback<Void> callback) {
		bannedAsnCache.remove(this);
		bannedAsnIdCache.remove(id);
		bannedAsnsCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}