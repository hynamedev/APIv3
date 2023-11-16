package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.service.emaildomain.EmailDomainService;
import net.frozenorb.apiv3.service.email.EmailService;
import net.frozenorb.apiv3.domain.NotificationTemplate;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.unsorted.Notification;
import net.frozenorb.apiv3.util.EmailUtils;
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
public final class POSTUsersIdRegisterEmail implements Handler<RoutingContext> {

	@Autowired private EmailService emailService;
	@Autowired private EmailDomainService emailDomainService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getEmail() != null) {
			ErrorUtils.respondInvalidInput(ctx, "User provided already has email set.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		String email = requestBody.getString("email");

		if (!EmailUtils.isValidEmail(email)) {
			ErrorUtils.respondInvalidInput(ctx, email + " is not a valid email.");
			return;
		}

		if (emailDomainService.isBannedDomain(email)) {
			ErrorUtils.respondInvalidInput(ctx, email + " is from a blacklisted domain.");
			return;
		}

		if (user.getPendingEmailToken() != null && (System.currentTimeMillis() - user.getPendingEmailTokenSetAt().toEpochMilli()) < TimeUnit.DAYS.toMillis(2)) {
			ErrorUtils.respondOther(ctx, 409, "Confirmation email recently sent.", "confirmationEmailRecentlySent", ImmutableMap.of());
			return;
		}

		User sameEmail = SyncUtils.runBlocking(v -> User.findByConfirmedEmail(email, v));

		if (sameEmail != null) {
			ErrorUtils.respondInvalidInput(ctx, email + " is already in use.");
			return;
		}

		user.startEmailRegistration(email);
		SyncUtils.<Void>runBlocking(v -> user.save(v));

		Map<String, Object> replacements = ImmutableMap.of(
				"username", user.getLastUsername(),
				"email", user.getPendingEmail(),
				"emailToken", user.getPendingEmailToken()
		);

		NotificationTemplate template = SyncUtils.runBlocking(v -> NotificationTemplate.findById("email-confirmation", v));
		Notification notification = new Notification(template, replacements, replacements);

		emailService.sendEmail(notification, user.getPendingEmail(), (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				String userIp = requestBody.getString("userIp");

				if (!IpUtils.isValidIp(userIp)) {
					ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
					return;
				}

				AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_REGISTER_EMAIL, (ignored2, error2) -> {
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