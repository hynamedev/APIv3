package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableList;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import io.vertx.core.Vertx;
import lombok.Getter;

@Entity
public final class Rank {

	private static final MongoCollection<Rank> ranksCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("ranks", Rank.class);

	private static Map<String, Rank> rankIdCache = null;
	private static List<Rank> rankCache = null;

	@Getter @Id private String id;
	@Getter private String inheritsFromId;
	@Getter private int generalWeight;
	@Getter private int displayWeight;
	@Getter private String displayName;
	@Getter private String gamePrefix;
	@Getter private String gameColor;
	@Getter private String websiteColor;
	@Getter private boolean staffRank;
	@Getter private boolean grantRequiresTotp;
	@Getter private String queueMessage;
	@Getter private boolean queueBypassCap;

	public static List<Rank> findAll() {
		return ImmutableList.copyOf(rankCache);
	}

	public static Rank findById(String id) {
		return id == null ? null : rankIdCache.get(id.toLowerCase());
	}

	static {
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.MINUTES.toMillis(1), (id) -> updateCache());
	}

	public static void updateCache() {
		List<Rank> ranks = SyncUtils.runBlocking(v -> ranksCollection.find().into(new LinkedList<>(), v));
		Map<String, Rank> working = new HashMap<>();

		for (Rank rank : ranks) {
			working.put(rank.getId().toLowerCase(), rank);
		}

		rankIdCache = working;
		rankCache = ranks;
	}

	private Rank() {} // For Jackson

	public Rank(String id, String inheritsFromId, int generalWeight, int displayWeight, String displayName, String gamePrefix, String gameColor,
	            String websiteColor, boolean staffRank, boolean grantRequiresTotp, String queueMessage, boolean queueBypassCap) {
		this.id = id;
		this.inheritsFromId = inheritsFromId;
		this.generalWeight = generalWeight;
		this.displayWeight = displayWeight;
		this.displayName = displayName;
		this.gamePrefix = gamePrefix;
		this.gameColor = gameColor;
		this.websiteColor = websiteColor;
		this.staffRank = staffRank;
		this.grantRequiresTotp = grantRequiresTotp;
		this.queueMessage = queueMessage;
		this.queueBypassCap = queueBypassCap;
	}

	public void insert(SingleResultCallback<Void> callback) {
		rankCache.add(this);
		rankIdCache.put(id.toLowerCase(), this);
		ranksCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(SingleResultCallback<Void> callback) {
		rankCache.remove(this);
		rankIdCache.remove(id.toLowerCase());
		ranksCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}