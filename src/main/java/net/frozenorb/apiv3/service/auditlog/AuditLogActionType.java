package net.frozenorb.apiv3.service.auditlog;

import com.mongodb.async.SingleResultCallback;

import net.frozenorb.apiv3.domain.AuditLogEntry;
import net.frozenorb.apiv3.domain.Punishment;

import java.time.Instant;

import lombok.Getter;

public enum AuditLogActionType {

	// TODO
	DISPOSABLE_LOGIN_TOKEN_USE(false),
	DISPOSABLE_LOGIN_TOKEN_CREATE(false),
	ACCESS_TOKEN_CREATE(false),
	ACCESS_TOKEN_UPDATE(false),
	ACCESS_TOKEN_DELETE(false),
	AUDIT_LOG_REVERT(false),
	BANNED_ASN_CREATE(false),
	BANNED_ASN_UPDATE(false),
	BANNED_ASN_DELETE(false),
	BANNED_CALL_CARRIER_CREATE(false),
	BANNED_CALL_CARRIER_UPDATE(false),
	BANNED_CALL_CARRIER_DELETE(false),
	GRANT_CREATE(false),
	GRANT_UPDATE(false),
	GRANT_DELETE(false),
	PREFIXGRANT_CREATE(false),
	PREFIXGRANT_UPDATE(false),
	PREFIXGRANT_DELETE(false),
	IP_BAN_CREATE(false),
	IP_BAN_UPDATE(false),
	IP_BAN_DELETE(false),
	NOTIFICATION_TEMPLATE_CREATE(false),
	NOTIFICATION_TEMPLATE_UPDATE(false),
	NOTIFICATION_TEMPLATE_DELETE(false),
	CHAT_FILTER_ENTRY_CREATE(false),
	CHAT_FILTER_ENTRY_UPDATE(false),
	CHAT_FILTER_ENTRY_DELETE(false),
	PUNISHMENT_CREATE(true) {
		@Override
		public void reverse(AuditLogEntry entry, SingleResultCallback<Void> callback) {
			String punishmentId = (String) entry.getMetadata().get("punishmentId");

			Punishment.findById(punishmentId, (punishment, error) -> {
				if (error != null) {
					callback.onResult(null, error);
					return;
				}

				if (punishment == null || !punishment.isActive()) {
					callback.onResult(null, null);
					return;
				}

				punishment.delete(null, "Removed via audit log reversal at " + Instant.now().toString() + ".", callback);
			});
		}

	},
	PUNISHMENT_UPDATE(false),
	PUNISHMENT_DELETE(false),
	RANK_CREATE(false),
	RANK_UPDATE(false),
	RANK_DELETE(false),
	PREFIX_CREATE(false),
	PREFIX_UPDATE(false),
	PREFIX_DELETE(false),
	SERVER_GROUP_CREATE(false),
	SERVER_GROUP_UPDATE(false),
	SERVER_GROUP_DELETE(false),
	SERVER_CREATE(false),
	SERVER_UPDATE(false),
	SERVER_DELETE(false),
	USER_LOGIN_SUCCESS(false),
	USER_LOGIN_FAIL(false),
	USER_CHANGE_PASSWORD(false),
	USER_PASSWORD_RESET(false),
	USER_REGISTER_EMAIL(false),
	USER_REGISTER_PHONE(false),
	USER_CONFIRM_EMAIL(false),
	USER_CONFIRM_PHONE(false),
	USER_SETUP_TOTP(false),
	USER_VERIFY_TOTP(false),
	USER_CHANGE_COLORS(false),
	USER_CHANGE_PREFIX(false);

	@Getter private boolean reversible;

	AuditLogActionType(boolean reversible) {
		this.reversible = reversible;
	}

	public void reverse(AuditLogEntry entry, SingleResultCallback<Void> callback) {
		callback.onResult(null, new UnsupportedOperationException());
	}

}