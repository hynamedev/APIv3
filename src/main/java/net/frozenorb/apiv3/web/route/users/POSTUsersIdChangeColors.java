package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.SyncUtils;
import org.springframework.stereotype.Component;

@Component
public final class POSTUsersIdChangeColors implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext ctx) {
        User user = SyncUtils.runBlocking(v -> User.findById(ctx.request().getParam("userId"), v));

        if (user == null) {
            ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
            return;
        }

        JsonObject requestBody = ctx.getBodyAsJson();

        String iconColor = requestBody.getString("iconColor");
        String nameColor = requestBody.getString("nameColor");

        user.updateColors(iconColor, nameColor);
        SyncUtils.<Void>runBlocking(user::save);

        APIv3.respondJson(ctx, 200, ImmutableMap.of(
                "success", true
        ));
    }
}
