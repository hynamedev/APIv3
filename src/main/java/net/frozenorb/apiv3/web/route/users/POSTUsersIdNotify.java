package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.email.EmailService;
import net.frozenorb.apiv3.domain.NotificationTemplate;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.unsorted.Notification;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersIdNotify implements Handler<RoutingContext> {

	@Autowired private EmailService emailService;

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		if (user.getEmail() == null) {
			ErrorUtils.respondInvalidInput(ctx, "User provided does not have email set.");
			return;
		}

		JsonObject requestBody = ctx.getBodyAsJson();
		NotificationTemplate template = SyncUtils.runBlocking(v -> NotificationTemplate.findById(requestBody.getString("template"), v));

		if (template == null) {
			ErrorUtils.respondNotFound(ctx, "Notification template", requestBody.getString("template"));
			return;
		}

		Map<String, Object> subjectReplacements = requestBody.getJsonObject("subjectReplacements").getMap();
		Map<String, Object> bodyReplacements = requestBody.getJsonObject("subjectReplacements").getMap();

		Notification notification = new Notification(template, subjectReplacements, bodyReplacements);

		emailService.sendEmail(notification, user.getEmail(), (ignored, error) -> {
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