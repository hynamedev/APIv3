package net.frozenorb.apiv3.web.route.chatFilter;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.ChatFilterEntry;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETChatFilterId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		ChatFilterEntry.findById(ctx.request().getParam("chatFilterEntryId"), (notificationTemplate, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, notificationTemplate);
			}
		});
	}

}