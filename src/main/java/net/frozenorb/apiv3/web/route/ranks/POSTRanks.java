package net.frozenorb.apiv3.web.route.ranks;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.Rank;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTRanks implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		JsonObject requestBody = ctx.getBodyAsJson();
		String id = requestBody.getString("id");
		String inheritsFromId = requestBody.getString("inheritsFromId");
		int generalWeight = requestBody.getInteger("generalWeight", -1);
		int displayWeight = requestBody.getInteger("displayWeight", -1);
		String displayName = requestBody.getString("displayName");
		String gamePrefix = requestBody.getString("gamePrefix");
		String gameColor = requestBody.getString("gameColor");
		String websiteColor = requestBody.getString("websiteColor");
		boolean staffRank = requestBody.getBoolean("staffRank");
		boolean grantRequiresTotp = requestBody.getBoolean("grantRequiresTotp");
		String queueMessage = requestBody.getString("queueMessage");
		boolean queueBypassCap = requestBody.getBoolean("queueBypassCap");

		Rank rank = new Rank(id, inheritsFromId, generalWeight, displayWeight, displayName, gamePrefix, gameColor, websiteColor, staffRank, grantRequiresTotp, queueMessage, queueBypassCap);
		SyncUtils.<Void>runBlocking(v -> rank.insert(v));

		if (requestBody.containsKey("addedBy")) {
			AuditLog.log(UuidUtils.parseUuid(requestBody.getString("addedBy")), requestBody.getString("addedByIp"), ctx, AuditLogActionType.RANK_CREATE, ImmutableMap.of("rankId", id), (ignored, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, rank);
				}
			});
		} else {
			APIv3.respondJson(ctx, 200, rank);
		}
	}

}