package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.serialization.gson.ExcludeFromReplies;
import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.PermissionUtils;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;

@Entity
public final class ServerGroup {

	public static final String DEFAULT_GROUP_ID = "default";
	private static final MongoCollection<ServerGroup> serverGroupsCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("serverGroups", ServerGroup.class);

	private static Map<String, ServerGroup> serverGroupIdCache = null;
	private static List<ServerGroup> serverGroupCache = null;

	@Getter @Id private String id;
	@Getter private String image;
	@Getter @Setter private Set<String> announcements;
	@Getter @Setter @ExcludeFromReplies private Map<String, List<String>> permissions;

	public static List<ServerGroup> findAll() {
		return ImmutableList.copyOf(serverGroupCache);
	}

	public static ServerGroup findById(String id) {
		return id == null ? null : serverGroupIdCache.get(id.toLowerCase());
	}

	public static ServerGroup findDefault() {
		return findById(DEFAULT_GROUP_ID);
	}

	static {
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.SECONDS.toMillis(15), (id) -> updateCache());
	}

	public static void updateCache() {
		List<ServerGroup> serverGroups = SyncUtils.runBlocking(v -> serverGroupsCollection.find().into(new LinkedList<>(), v));
		Map<String, ServerGroup> working = new HashMap<>();

		for (ServerGroup serverGroup : serverGroups) {
			working.put(serverGroup.getId().toLowerCase(), serverGroup);
		}

		serverGroupIdCache = working;
		serverGroupCache = serverGroups;
	}

	private ServerGroup() {} // For Jackson

	public ServerGroup(String id, String image) {
		this.id = id;
		this.image = image;
	}

	public Map<String, Boolean> calculatePermissions(Rank rank) {
		if (permissions == null) {
			return ImmutableMap.of();
		} else {
			return PermissionUtils.mergeUpTo(permissions, rank);
		}
	}

	public void insert(SingleResultCallback<Void> callback) {
		serverGroupCache.add(this);
		serverGroupIdCache.put(id.toLowerCase(), this);
		serverGroupsCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		serverGroupsCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

	public void delete(SingleResultCallback<Void> callback) {
		serverGroupCache.remove(this);
		serverGroupIdCache.remove(id.toLowerCase());
		serverGroupsCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}