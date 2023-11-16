package net.frozenorb.apiv3.web.route.phoneIntel;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.PhoneIntel;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.PhoneUtils;

import org.springframework.stereotype.Component;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETPhoneInteld implements Handler<RoutingContext> {

	public void handle(RoutingContext ctx) {
		String phoneNumber = ctx.request().getParam("phone");

		if (!PhoneUtils.isValidPhone(phoneNumber)) {
			ErrorUtils.respondInvalidInput(ctx, "Phone number \"" + phoneNumber + "\" is not valid.");
			return;
		}

		PhoneIntel.findOrCreateById(phoneNumber, (ipIntel, error) -> {
			if (error != null) {
				ErrorUtils.respondInternalError(ctx, error);
			} else {
				APIv3.respondJson(ctx, 200, ipIntel);
			}
		});
	}

}