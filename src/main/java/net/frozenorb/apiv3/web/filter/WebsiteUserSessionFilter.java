package net.frozenorb.apiv3.web.filter;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.unsorted.actor.Actor;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.service.usersession.UserSessionService;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class WebsiteUserSessionFilter implements Handler<RoutingContext> {

	@Autowired private UserSessionService userSessionService;

	@Override
	public void handle(RoutingContext ctx) {
		Actor actor = ctx.get("actor");

		if (actor.getType() != ActorType.WEBSITE) {
			ctx.next();
			return;
		}

		if (!isUserSessionRequired(ctx)) {
			ctx.next();
			return;
		}

		String userSession = ctx.request().getHeader("MHQ-UserSession");
		String userIp = ctx.request().getHeader("MHQ-UserIp");

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "IP address \"" + userIp + "\" is not valid.");
			return;
		}

		userSessionService.sessionExists(userIp, userSession, (exists, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
				return;
			}

			if (exists) {
				ctx.next();
			} else {
				ErrorUtils.respondOther(ctx, 403, "User session invalid.", "userSessionInvalid", ImmutableMap.of());
			}
		});
	}

	public boolean isUserSessionRequired(RoutingContext ctx) {
		HttpMethod method = ctx.request().method();
		String path = ctx.request().path().toLowerCase();

		/*
	    http.get("/emailTokens/:emailToken/owner").blockingHandler(new GETEmailTokensIdOwner(), false);
        http.get("/ranks/:rankId").handler(new GETRanksId());
        http.get("/serverGroups/:serverGroupId").handler(new GETServerGroupsId());
        http.get("/servers/:serverId").handler(new GETServersId());
        http.get("/dumps/:dumpType").handler(new GETDumpsType());
        http.get("/users/:userId").handler(new GETUsersId());
        http.get("/users/:userId/compoundedPermissions").handler(new GETUsersIdCompoundedPermissions());
        http.get("/users/:userId/details").blockingHandler(new GETUsersIdDetails(), false);
        http.get("/users/:userId/requiresTotp").handler(new GETUsersIdRequiresTotp());
        http.get("/users/:userId/verifyPassword").blockingHandler(new GETUsersIdVerifyPassword(), false);

        http.post("/users/:userId/changePassword").blockingHandler(new POSTUsersIdChangePassword(), false);
        http.post("/users/:userId/passwordReset").blockingHandler(new POSTUsersIdPasswordReset(), false);
        http.post("/users/:userId/verifyTotp").handler(new POSTUsersIdVerifyTotp());
        http.post("/logout").handler(new POSTLogout());
        http.post("/emailTokens/:emailToken/confirm").blockingHandler(new POSTEmailTokensIdConfirm(), false);
		 */

		/*if (method == HttpMethod.GET) {
			switch (path) {
				case "/ranks":
				case "/staff":
				case "/servers":
				case "/servergroups":
				case "/whoami":
					return false;
			}

			for  (String allowedRoutes : new String[] { "/emailTokens", "/ranks", "/serverGroups", "/servers", "/dumps", "/users" }) {
				if (path.contains(allowedRoutes)) {
					return false;
				}
			}
		} else if (method == HttpMethod.POST) {
			switch (path) {
				case "/logout":
					return false;
			}
		}*/

		return false;
	}

}