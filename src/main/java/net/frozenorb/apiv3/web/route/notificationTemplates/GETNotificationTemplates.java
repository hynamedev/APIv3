package net.frozenorb.apiv3.web.route.notificationTemplates;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.NotificationTemplate;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETNotificationTemplates implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		NotificationTemplate.findAll((notificationTemplates, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, notificationTemplates);
			}
		});
	}

}