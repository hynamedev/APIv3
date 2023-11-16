package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.BannedCellCarrier;
import net.frozenorb.apiv3.domain.NotificationTemplate;
import net.frozenorb.apiv3.domain.PhoneIntel;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.sms.SmsService;
import net.frozenorb.apiv3.unsorted.Notification;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.PhoneUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdRegisterPhone implements Handler<RoutingContext> {

	@Autowired private SmsService smsService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getPhone() != null) {
			ErrorUtils.respondInvalidInput(ctx, "User provided already has phone set.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		String phone = requestBody.getString("phone");

		if (!PhoneUtils.isValidPhone(phone)) {
			ErrorUtils.respondInvalidInput(ctx, phone + " is not a valid phone number.");
			return;
		}

		if (user.getPendingPhoneToken() != null && (System.currentTimeMillis() - user.getPendingPhoneTokenSetAt().toEpochMilli()) < TimeUnit.HOURS.toMillis(6)) {
			ErrorUtils.respondOther(ctx, 409, "Confirmation code recently sent.", "confirmationCodeRecentlySent", ImmutableMap.of());
			return;
		}

		User samePhone = SyncUtils.runBlocking(v -> User.findByPhone(phone, v));

		if (samePhone != null) {
			ErrorUtils.respondInvalidInput(ctx, phone + " is already in use.");
			return;
		}

		PhoneIntel phoneIntel = SyncUtils.runBlocking(v -> PhoneIntel.findOrCreateById(phone, v));

		if (BannedCellCarrier.findById(phoneIntel.getResult().getCarrierId()) != null) {
			ErrorUtils.respondInvalidInput(ctx, phone + " is from a banned cell provider.");
			return;
		}

		user.startPhoneRegistration(phone);
		SyncUtils.<Void>runBlocking(v -> user.save(v));

		Map<String, Object> replacements = ImmutableMap.of(
				"username", user.getLastUsername(),
				"phone", user.getPendingPhone(),
				"phoneToken", user.getPendingPhoneToken()
		);

		NotificationTemplate template = SyncUtils.runBlocking(v -> NotificationTemplate.findById("phone-confirmation", v));
		Notification notification = new Notification(template, replacements, replacements);

		smsService.sendText(notification, user.getPendingPhone(), (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				String userIp = requestBody.getString("userIp");

				if (!IpUtils.isValidIp(userIp)) {
					ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
					return;
				}

				AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_REGISTER_PHONE, (ignored2, error2) -> {
					if (error2 != null) {
						ErrorUtils.respondInternalError(ctx, error2);
					} else {
						APIv3.respondJson(ctx, 200, ImmutableMap.of(
								"success", true
						));
					}
				});
			}
		});
	}

}