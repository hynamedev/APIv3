package net.frozenorb.apiv3.web.route.auditLog;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.AuditLogEntry;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.UuidUtils;

import org.bson.Document;
import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETAuditLog implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		try {
			int skip = ctx.request().getParam("skip") == null ? 0 : Integer.parseInt(ctx.request().getParam("skip"));
			int pageSize = ctx.request().getParam("pageSize") == null ? 100 : Integer.parseInt(ctx.request().getParam("pageSize"));

			AuditLogEntry.findPaginated(ctx.request().getParam("user") == null ? new Document() : new Document("user", UuidUtils.parseUuid(ctx.request().getParam("user"))), skip, pageSize, (auditLog, error) -> {
				if (error != null) {
					ErrorUtils.respondInternalError(ctx, error);
				} else {
					APIv3.respondJson(ctx, 200, auditLog);
				}
			});
		} catch (NumberFormatException ignored) {
			ErrorUtils.respondInvalidInput(ctx, "skip and pageSize must be numerical inputs.");
		}
	}

}