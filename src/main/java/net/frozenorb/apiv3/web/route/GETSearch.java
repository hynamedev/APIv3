package net.frozenorb.apiv3.web.route;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETSearch implements Handler<RoutingContext> {

    public void handle(RoutingContext ctx) {
        String type = ctx.request().getParam("type");
        String query = ctx.request().getParam("q");

        if (type == null) {
            ErrorUtils.respondRequiredInput(ctx, "type");
            return;
        } else if (query == null) {
            ErrorUtils.respondRequiredInput(ctx, "q");
            return;
        }

        switch (type.toLowerCase()) {
            case "user":
                User user = SyncUtils.runBlocking(v -> User.findById(query, v));

                if (user == null) {
                    user = SyncUtils.runBlocking(v -> User.findByLastUsernameLower(query, v));
                }

                if (user == null) {
                    user = SyncUtils.runBlocking(v -> User.findByConfirmedEmail(query, v));
                }

                if (user == null) {
                    ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
                } else {
                    APIv3.respondJson(ctx, 200, user);
                }

                break;
            default:
                ErrorUtils.respondInvalidInput(ctx, type + " is not a valid search type. Valid types: [ user ]");
                break;
        }
    }

}