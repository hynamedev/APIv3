package net.frozenorb.apiv3.web.route;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.usersession.UserSessionService;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTLogout implements Handler<RoutingContext> {

	@Autowired private UserSessionService userSessionService;

	public void handle(RoutingContext ctx) {
		String userSession = ctx.request().getHeader("MHQ-UserSession");
		String userIp = ctx.request().getHeader("MHQ-UserIp");

		userSessionService.invalidateSession(userIp, userSession, (ignored, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, ImmutableMap.of());
			}
		});
	}

}