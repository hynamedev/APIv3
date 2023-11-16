package net.frozenorb.apiv3.domain;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;

import net.frozenorb.apiv3.service.sms.SmsCarrierInfo;
import net.frozenorb.apiv3.service.sms.SmsService;
import net.frozenorb.apiv3.util.PhoneUtils;
import net.frozenorb.apiv3.util.SpringUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.bson.Document;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import fr.javatic.mongo.jacksonCodec.Entity;
import fr.javatic.mongo.jacksonCodec.objectId.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@AllArgsConstructor
public final class PhoneIntel {

	private static final MongoCollection<PhoneIntel> phoneIntelCollection = SpringUtils.getBean(MongoDatabase.class).getCollection("phoneIntel", PhoneIntel.class);

	@Getter @Id private String id;
	@Getter private Instant lastUpdatedAt;
	@Getter private SmsCarrierInfo result;

	public static void findAll(SingleResultCallback<List<PhoneIntel>> callback) {
		phoneIntelCollection.find().sort(new Document("lastSeenAt", -1)).into(new LinkedList<>(), SyncUtils.vertxWrap(callback));
	}

	public static void findById(String id, SingleResultCallback<PhoneIntel> callback) {
		String e164Phone = PhoneUtils.toE164(id);

		if (e164Phone == null) {
			callback.onResult(null, null);
		} else {
			phoneIntelCollection.find(new Document("_id", id)).first(SyncUtils.vertxWrap(callback));
		}
	}

	public static void findOrCreateById(String id, SingleResultCallback<PhoneIntel> callback) {
		String e164Phone = PhoneUtils.toE164(id);

		if (e164Phone == null) {
			callback.onResult(null, null);
			return;
		}

		findById(e164Phone, (existingPhoneIntel, error) -> {
			if (error != null) {
				callback.onResult(null, error);
				return;
			}

			if (existingPhoneIntel != null) {
				callback.onResult(existingPhoneIntel, null);
				return;
			}


			SpringUtils.getBean(SmsService.class).lookupCarrierInfo(e164Phone, (smsCarrierInfo, error2) -> {
				if (error2 != null) {
					callback.onResult(null, error2);
					return;
				}

				PhoneIntel newPhoneIntel = new PhoneIntel(e164Phone, smsCarrierInfo);

				phoneIntelCollection.insertOne(newPhoneIntel, SyncUtils.vertxWrap((ignored, error3) -> {
					if (error3 != null) {
						callback.onResult(null, error3);
					} else {
						callback.onResult(newPhoneIntel, null);
					}
				}));
			});
		});
	}

	private PhoneIntel() {} // For Jackson

	private PhoneIntel(String phoneNumber, SmsCarrierInfo result) {
		this.id = phoneNumber;
		this.lastUpdatedAt = Instant.now();
		this.result = result;
	}

}