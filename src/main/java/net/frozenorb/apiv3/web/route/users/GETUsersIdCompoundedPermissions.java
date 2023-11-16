package net.frozenorb.apiv3.web.route.users;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.PermissionUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETUsersIdCompoundedPermissions implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		User.findById(ctx.request().getParam("userId"), (user, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else if (user == null) {
				ErrorUtils.respondNotFound(ctx, "User", ctx.request().getParam("userId"));
			} else {
				user.getCompoundedPermissions((permissions, error2) -> {
					if (error2 != null) {
						ErrorUtils.respondInternalError(ctx, error2);
					} else {
						APIv3.respondJson(ctx, 200, PermissionUtils.convertToList(permissions));
					}
				});
			}
		});
	}

}