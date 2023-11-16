package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableList;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import io.vertx.core.Vertx;
import lombok.Getter;
import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Entity
public final class Prefix {

	private static final MongoCollection<Prefix> prefixesCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("prefixes", Prefix.class);

	private static Map<String, Prefix> prefixIdCache = null;
	private static List<Prefix> prefixCache = null;

	@Getter @Id private String id;
	@Getter private String displayName;
	@Getter private String prefix;
	@Getter private boolean purchaseable;
	@Getter private String buttonName;
	@Getter private String buttonDescription;

	public static List<Prefix> findAll() {
		return ImmutableList.copyOf(prefixCache);
	}

	public static Prefix findById(String id) {
		return id == null ? null : prefixIdCache.get(id.toLowerCase());
	}

	static {
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.MINUTES.toMillis(1), (id) -> updateCache());
	}

	public static void updateCache() {
		List<Prefix> prefixes = SyncUtils.runBlocking(v -> prefixesCollection.find().into(new LinkedList<>(), v));
		Map<String, Prefix> working = new HashMap<>();

		for (Prefix prefix : prefixes) {
			working.put(prefix.getId().toLowerCase(), prefix);
		}

		prefixIdCache = working;
		prefixCache = prefixes;
	}

	private Prefix() {} // For Jackson

	public Prefix(String id, String displayName, String prefix, boolean purchaseable, String buttonName, String buttonDescription) {
		this.id = id;
		this.displayName = displayName;
		this.prefix = prefix;
		this.purchaseable = purchaseable;
		this.buttonName = buttonName;
		this.buttonDescription = buttonDescription;
	}

	public void insert(SingleResultCallback<Void> callback) {
		prefixCache.add(this);
		prefixIdCache.put(id.toLowerCase(), this);
		prefixesCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(SingleResultCallback<Void> callback) {
		prefixCache.remove(this);
		prefixIdCache.remove(id.toLowerCase());
		prefixesCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}