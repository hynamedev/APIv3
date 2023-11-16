package net.frozenorb.apiv3.web.filter;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.unsorted.actor.SimpleActor;
import net.frozenorb.apiv3.domain.AccessToken;
import net.frozenorb.apiv3.util.ErrorUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class AuthenticationFilter implements Handler<RoutingContext> {

	@Override
	public void handle(RoutingContext ctx) {
		String mhqAuthorizationHeader = ctx.request().getHeader("MHQ-Authorization");

		if (mhqAuthorizationHeader != null) {
			processMHQAuthorization(mhqAuthorizationHeader, ctx);
		} else {
			processNoAuthorization(ctx);
		}
	}

	private void processMHQAuthorization(String accessTokenString, RoutingContext ctx) {
		if (accessTokenString == null || accessTokenString.isEmpty()) {
			ErrorUtils.respondOther(ctx, 403, "Failed to authorize.", "failedToAuthorizeNoKey", ImmutableMap.of());
			return;
		}

		AccessToken.findById(accessTokenString, (accessToken, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
				return;
			}

			if (accessToken == null) {
				ErrorUtils.respondOther(ctx, 403, "Failed to authorize.", "failedToAuthorizeUnknownKey", ImmutableMap.of());
				return;
			}

			if (accessToken.getLockedIps() != null && !accessToken.getLockedIps().isEmpty()) {
				boolean allowed = accessToken.getLockedIps().contains(ctx.request().remoteAddress().host());

				if (!allowed) {
					ErrorUtils.respondOther(ctx, 403, "Failed to authorize.", "failedToAuthorizeNoIpWhitelist", ImmutableMap.of());
					return;
				}
			}

			ctx.put("actor", new SimpleActor(accessToken.getActorName(), accessToken.getActorType(), true));
			ctx.next();
		});
	}

	private void processNoAuthorization(RoutingContext ctx) {
		ctx.put("actor", new SimpleActor("UNKNOWN", ActorType.UNKNOWN, false));
		ctx.next();
	}

}