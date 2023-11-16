package net.frozenorb.apiv3.web.route.ipLog;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.IpLogEntry;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import java.util.UUID;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETIpLogId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		String search = ctx.request().getParam("id");

		if (search.length() >= 32) {
			UUID uuid = null;

			try {
				uuid = UuidUtils.parseUuid(search);
			} catch (IllegalArgumentException ignored) {}

			if (uuid != null) {
				IpLogEntry.findByUser(uuid, (ipLog, error) -> {
					if (error != null) {
						ErrorUtils.respondInternalError(ctx, error);
					} else {
						APIv3.respondJson(ctx, 200, ipLog);
					}
				});
			} else {
				IpLogEntry.findByHashedIp(search, (ipLogs, error) -> {
					if (error != null) {
						ErrorUtils.respondInternalError(ctx, error);
					} else {
						APIv3.respondJson(ctx, 200, ipLogs);
					}
				});
			}
		} else {
			IpLogEntry.findByIp(search, (ipLogs, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, ipLogs);
				}
			});
		}
	}

}