package net.frozenorb.apiv3.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.serialization.gson.ExcludeFromReplies;
import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.TimeUtils;

import org.bson.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;

@Entity
public final class Server {

	private static final MongoCollection<Server> serversCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("servers", Server.class);

	private static Map<String, Server> serverIdCache = null;
	private static List<Server> serverCache = null;

	@Getter @Id private String id;
	@Getter private String displayName;
	@Getter private String serverGroup;
	@Getter @Setter private String serverIp;
	@Getter @Setter private Instant lastUpdatedAt;
	@Getter private double lastTps;
	@Getter @ExcludeFromReplies private Set<UUID> players;

	public static List<Server> findAll() {
		return ImmutableList.copyOf(serverCache);
	}

	public static Server findById(String id) {
		return id == null ? null : serverIdCache.get(id.toLowerCase());
	}

	static {
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.SECONDS.toMillis(15), (id) -> updateCache());
		SpringUtils.getBean(Vertx.class).setPeriodic(TimeUnit.MINUTES.toMillis(1), (id) -> updateTimedOutServers());
	}

	public static void updateCache() {
		List<Server> servers = SyncUtils.runBlocking(v -> serversCollection.find().into(new LinkedList<>(), v));
		Map<String, Server> working = new HashMap<>();

		for (Server server : servers) {
			working.put(server.getId().toLowerCase(), server);
		}

		serverIdCache = working;
		serverCache = servers;
	}

	private static void updateTimedOutServers() {
		for (Server server : serverCache) {
			int lastUpdatedAgo = TimeUtils.getSecondsBetween(server.getLastUpdatedAt(), Instant.now());

			if (lastUpdatedAgo < 60 || server.getPlayers().isEmpty()) {
				continue;
			}

			for (UUID online : server.getPlayers()) {
				User.findById(online, (user, findUserError) -> {
					if (findUserError != null) {
						findUserError.printStackTrace();
						return;
					}

					if (user.leftServer(server)) {
						user.save((ignored, saveUserError) -> {
							if (saveUserError != null) {
								saveUserError.printStackTrace();
							}
						});
					}
				});
			}

			server.players = new HashSet<>();
			server.save((ignored, error) -> {
				if (error != null) {
					error.printStackTrace();
				}
			});
		}
	}

	private Server() {} // For Jackson

	public Server(String id, String displayName, ServerGroup serverGroup, String serverIp) {
		this.id = id;
		this.displayName = displayName;
		this.serverGroup = serverGroup.getId();
		this.serverIp = serverIp;
		this.lastUpdatedAt = Instant.now();
		this.lastTps = 0;
		this.players = new HashSet<>();
	}

	public void receivedHeartbeat(double tps, Iterable<UUID> players) {
		this.lastUpdatedAt = Instant.now();
		this.lastTps = tps;
		this.players = ImmutableSet.copyOf(players);
	}

	public void insert(SingleResultCallback<Void> callback) {
		serverCache.add(this);
		serverIdCache.put(id.toLowerCase(), this);
		serversCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void save(SingleResultCallback<Void> callback) {
		serversCollection.replaceOne(new Document("_id", id), this, SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

	public void delete(SingleResultCallback<Void> callback) {
		serverCache.remove(this);
		serverIdCache.remove(id.toLowerCase());
		serversCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}