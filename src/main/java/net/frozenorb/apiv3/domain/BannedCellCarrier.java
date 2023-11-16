
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
public final class BannedCellCarrier {

	private static final MongoCollection<BannedCellCarrier> bannedCellCarriersCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("bannedCellCarriers", BannedCellCarrier.class);

	private static Map<Integer, BannedCellCarrier> bannedCellCarrierIdCache = null;
	private static List<BannedCellCarrier> bannedCellCarrierCache = null;

	@Getter @Id private int id;
	@Getter @Setter String note;
	@Getter private Instant bannedAt;
	@Getter private Instant lastUpdatedAt;

	public static List<BannedCellCarrier> findAll() {
		return ImmutableList.copyOf(bannedCellCarrierCache);
	}

	public static BannedCellCarrier findById(int id) {
		return bannedCellCarrierIdCache.get(id);
	}

	static {
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.MINUTES.toMillis(1), (id) -> updateCache());
	}

	public static void updateCache() {
		List<BannedCellCarrier> bannedCellCarriers = SyncUtils.runBlocking(v -> bannedCellCarriersCollection.find().into(new LinkedList<>(), v));
		Map<Integer, BannedCellCarrier> working = new HashMap<>();

		for (BannedCellCarrier bannedCellCarrier : bannedCellCarriers) {
			working.put(bannedCellCarrier.getId(), bannedCellCarrier);
		}

		bannedCellCarrierIdCache = working;
		bannedCellCarrierCache = bannedCellCarriers;
	}

	private BannedCellCarrier() {} // For Jackson

	public BannedCellCarrier(int id, String note) {
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
		bannedCellCarrierCache.add(this);
		bannedCellCarrierIdCache.put(id, this);
		bannedCellCarriersCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		bannedCellCarriersCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

	public void delete(SingleResultCallback<Void> callback) {
		bannedCellCarrierCache.remove(this);
		bannedCellCarrierIdCache.remove(id);
		bannedCellCarriersCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}