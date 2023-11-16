package net.frozenorb.apiv3.domain;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.unsorted.MongoToVoidMongoCallback;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;

import java.util.LinkedList;
import java.util.List;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.Getter;

@Entity
public final class ChatFilterEntry {

	private static final MongoCollection<ChatFilterEntry> chatFilterCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("chatFilter", ChatFilterEntry.class);

	@Getter @Id private String id;
	@Getter private String regex;

	public static void findById(String id, SingleResultCallback<ChatFilterEntry> callback) {
		chatFilterCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	public static void findAll(SingleResultCallback<List<ChatFilterEntry>> callback) {
		chatFilterCollection.find().into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	private ChatFilterEntry() {} // For Jackson

	public ChatFilterEntry(String id, String regex) {
		this.id = id;
		this.regex = regex;
	}

	public void insert(SingleResultCallback<Void> callback) {
		chatFilterCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(SingleResultCallback<Void> callback) {
		chatFilterCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}