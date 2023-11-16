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
public final class POSTPrefixes implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String id = requestBody.getString("id");
		String displayName = requestBody.getString("displayName");
		String prefix = requestBody.getString("prefix");
		boolean purchaseable = requestBody.getBoolean("purchaseable");
		String buttonName = requestBody.getString("buttonName");
		String buttonDescription = requestBody.getString("buttonDescription");

		Prefix pref = new Prefix(id, displayName, prefix, purchaseable, buttonName, buttonDescription);
		SyncUtils.<Void>runBlocking(pref::insert);

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.PREFIX_CREATE, ImmutableMap.of("prefixId", id), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, pref);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, pref);
		}
	}

}