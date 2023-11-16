package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.service.email.EmailService;
import net.frozenorb.apiv3.domain.NotificationTemplate;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.unsorted.Notification;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdPasswordReset implements Handler<RoutingContext> {

	@Autowired private EmailService emailService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getPasswordResetToken() != null && (System.currentTimeMillis() - user.getPasswordResetTokenSetAt().toEpochMilli()) < TimeUnit.DAYS.toMillis(2)) {
			ErrorUtils.respondInvalidInput(ctx, "User provided already has password reset token set.");
			return;
		}

		user.startPasswordReset();
		SyncUtils.<Void>runBlocking(v -> user.save(v));

		Map<String, Object> replacements = ImmutableMap.of(
				"username", user.getLastUsername(),
				"passwordResetToken", user.getPasswordResetToken()
		);

		NotificationTemplate template = SyncUtils.runBlocking(v -> NotificationTemplate.findById("password-reset", v));
		Notification notification = new Notification(template, replacements, replacements);

		emailService.sendEmail(notification, user.getEmail(), (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				JsonObject requestBody = ctx.getBodyAsJson();
				String userIp = requestBody.getString("userIp");

				if (!IpUtils.isValidIp(userIp)) {
					ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
					return;
				}

				AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_PASSWORD_RESET, (ignored2, error2) -> {
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