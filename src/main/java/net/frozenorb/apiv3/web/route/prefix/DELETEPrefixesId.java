package net.frozenorb.apiv3.web.route.prefix;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Prefix;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;
import org.springframework.stereotype.Component;

@Component
public final class DELETEPrefixesId implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		Prefix prefix = Prefix.findById(ctx.request().getParam("prefixId"));

		if (prefix == null) {
			ErrorUtils.respondNotFound(ctx, "Prefix", ctx.request().getParam("prefixId"));
			return;
		}

		SyncUtils.<Void>runBlocking(v -> prefix.delete(v));

		JsonObject requestBody = ctx.getBodyAsJson();

		if (requestBody.containsKey("removedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("removedBy")), requestBody.getString("removedByIp"), ctx, AuditLogActionType.RANK_DELETE, ImmutableMap.of("prefixId", prefix.getId()), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, prefix);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, prefix);
		}
	}

}