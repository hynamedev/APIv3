package net.frozenorb.apiv3.web.route.disposableLoginTokens;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.disposablelogintoken.DisposableLoginTokenService;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTDisposableLoginTokens implements Handler<RoutingContext> {

	@Autowired private DisposableLoginTokenService disposableLoginTokenService;

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		User user = SyncUtils.runBlocking(v -> User.findById(requestBody.getString("user"), v));
		String userIp = requestBody.getString("userIp");

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", requestBody.getString("user"));
			return;
		}

		if (!IpUtils.isValidIp(userIp)) {
			ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
			return;
		}

		disposableLoginTokenService.createToken(user.getId(), userIp, (token, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, ImmutableMap.of(
						"success", true,
						"token", token
				));
			}
		});
	}

}