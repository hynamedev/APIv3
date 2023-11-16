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
import java.util.Map;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public final class NotificationTemplate {

	private static final MongoCollection<NotificationTemplate> notificationTemplatesCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("notificationTemplates", NotificationTemplate.class);

	@Getter @Id private String id;
	@Getter @Setter private String subject;
	@Getter @Setter private String body;

	public static void findAll(SingleResultCallback<List<NotificationTemplate>> callback) {
		notificationTemplatesCollection.find().into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<NotificationTemplate> callback) {
		notificationTemplatesCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
	}

	private NotificationTemplate() {} // For Jackson

	public NotificationTemplate(String id, String subject, String body) {
		this.id = id;
		this.subject = subject;
		this.body = body;
	}

	public String fillSubject(Map<String, Object> replacements) {
		return fill(subject, replacements);
	}

	public String fillBody(Map<String, Object> replacements) {
		return fill(body, replacements);
	}

	private String fill(String working, Map<String, Object> replacements) {
		for (Map.Entry<String, Object> replacement : replacements.entrySet()) {
			String key = replacement.getKey();
			String value = String.valueOf(replacement.getValue());

			working = working.replace("%" + key + "%", value);
		}

		return working;
	}

	public void insert(SingleResultCallback<Void> callback) {
		notificationTemplatesCollection.insertOne(this, SyncUtils.vertxWrap(callback));
	}

	public void delete(SingleResultCallback<Void> callback) {
		notificationTemplatesCollection.deleteOne(new Document("_id", id), SyncUtils.vertxWrap(new MongoToVoidMongoCallback<>(callback)));
	}

}