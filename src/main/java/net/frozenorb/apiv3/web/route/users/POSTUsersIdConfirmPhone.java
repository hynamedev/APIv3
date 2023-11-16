package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdConfirmPhone implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getPhone() != null) {
			ErrorUtils.respondOther(ctx, 409, "User provided already has a confirmed phone number", "phoneNumberAlreadyConfirmed", ImmutableMap.of());
			return;
		}

		if (user.getPendingPhoneToken() == null) {
			ErrorUtils.respondOther(ctx, 409, "User provided already hasn't started confirming a phone number.", "phoneConfirmationNotStarted", ImmutableMap.of());
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		int phoneCode = requestBody.getInteger("phoneCode", -1);

		if ((System.currentTimeMillis() - user.getPendingPhoneTokenSetAt().toEpochMilli()) > TimeUnit.HOURS.toMillis(6)) {
			ErrorUtils.respondOther(ctx, 409, "Phone token is expired", "phoneTokenExpired", ImmutableMap.of());
			return;
		}

		if ((System.currentTimeMillis() - user.getPendingPhoneTokenVerificationAttemptedAt().toEpochMilli()) < TimeUnit.MINUTES.toMillis(20)) {
			ErrorUtils.respondOther(ctx, 409, "Wait before attempting phone verification again.", "waitBeforeAttemptingPhoneVerificationAgain", ImmutableMap.of());
			return;
		}

		if (user.getPhoneVerificationFailedAttempts().size() >= 5) {
			ErrorUtils.respondOther(ctx, 409, "Too many failed verification attempts", "tooManyFailedPhoneVerifications", ImmutableMap.of());
			return;
		}

		if (!String.valueOf(phoneCode).equals(user.getPendingPhoneToken())) {
			user.failedPhoneRegistration();
			SyncUtils.<Void>runBlocking(v -> user.save(v));

			ErrorUtils.respondOther(ctx, 409, "Phone token doesn't match", "phoneTokenNoMatch", ImmutableMap.of());
			return;
		}

		user.completePhoneRegistration(user.getPendingPhone());
		SyncUtils.<Void>runBlocking(v -> user.save(v));
		String userIp = requestBody.getString("userIp");

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
			return;
		}

		AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_CONFIRM_PHONE, (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, ImmutableMap.of(
						"success", true
				));
			}
		});
	}

}