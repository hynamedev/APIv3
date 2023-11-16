package net.frozenorb.apiv3.web.route.users;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.domain.IpLogEntry;
import net.frozenorb.apiv3.domain.Punishment;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETUsersIdDetails implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

		if (user == null) {
			ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			return;
		}

		List<Grant> grants = SyncUtils.runBlocking(v -> Grant.findByUser(user, v));
		List<IpLogEntry> ipLog = SyncUtils.runBlocking(v -> IpLogEntry.findByUser(user, v));
		List<Punishment> punishments = SyncUtils.runBlocking(v -> Punishment.findByUser(user, v));
		Map<String, Object> result = new HashMap<>();

		result.put("user", user);
		result.put("grants", grants);
		result.put("ipLog", ipLog);
		result.put("punishments", punishments);
		result.put("aliases", user.getAliases());
		result.put("totpSetup", user.getTotpSecret() != null);

		APIv3.respondJson(ctx, 200, result);
	}

}