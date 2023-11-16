package net.frozenorb.apiv3.util;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;

import java.util.Map;

import io.vertx.ext.web.RoutingContext;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ErrorUtils {

	public static void respondNotFound(RoutingContext ctx, String itemType, String id) {
		respond(ctx, 404, "Not found: " + itemType + " with id " + id + " cannot be found.", null, null);
	}

	public static void respondInvalidInput(RoutingContext ctx, String message) {
		respond(ctx, 400, "Invalid input: " + message, null, null);
	}

	public static void respondRequiredInput(RoutingContext ctx, String field) {
		respond(ctx, 400, "Required input: " + field + " is required.", null, null);
	}

	public static void respondInternalError(RoutingContext ctx, Throwable error) {
		log.error("Request \"" + ctx.request().method().name() + " " + ctx.request().path() + "\" failed.", error);
		respond(ctx, 500, "Internal error: " + error.getClass().getSimpleName(), null, null);
	}

	public static void respondOther(RoutingContext ctx, int code, String message, String translationId, Map<String, Object> translationParams) {
		respond(ctx, code, message, translationId, translationParams);
	}

	private static void respond(RoutingContext ctx, int code, String message, String translationId, Map<String, Object> translationParams) {
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

		builder.put("success", false);
		builder.put("message", message);

		if (translationId != null) {
			builder.put("translationId", translationId);
			builder.put("translationParams", translationParams);
		}

		APIv3.respondJson(ctx, code, builder.build());
	}

}