package net.frozenorb.apiv3.web.route.users;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.service.auditlog.AuditLog;
import net.frozenorb.apiv3.service.auditlog.AuditLogActionType;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.service.usersession.UserSessionService;
import net.frozenorb.apiv3.util.ErrorUtils;
import net.frozenorb.apiv3.util.IpUtils;
import net.frozenorb.apiv3.util.PasswordUtils;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Component
public final class POSTUsersUsePasswordResetToken implements Handler<RoutingContext> {

    @Autowired private UserSessionService userSessionService;

    public void handle(RoutingContext ctx) {
        JsonObject requestBody = ctx.getBodyAsJson();

        if (!requestBody.containsKey("passwordResetToken")) {
            ErrorUtils.respondRequiredInput(ctx, "passwordResetToken");
            return;
        }

        String passwordResetToken = requestBody.getString("passwordResetToken");
        User user = SyncUtils.runBlocking(v -> User.findByPasswordResetToken(passwordResetToken, v));

        if (user == null) {
            ErrorUtils.respondNotFound(ctx, "Password reset token", passwordResetToken);
            return;
        }

        if ((System.currentTimeMillis() - user.getPasswordResetTokenSetAt().toEpochMilli()) > TimeUnit.DAYS.toMillis(2)) {
            ErrorUtils.respondOther(ctx, 409, "Password reset token is expired.", "passwordTokenExpired", ImmutableMap.of());
            return;
        }

        String newPassword = requestBody.getString("newPassword");

        if (PasswordUtils.isTooShort(newPassword)) {
            ErrorUtils.respondOther(ctx, 409, "Your password is too short.", "passwordTooShort", ImmutableMap.of());
            return;
        }

        if (PasswordUtils.isTooSimple(newPassword)) {
            ErrorUtils.respondOther(ctx, 409, "Your password is too simple.", "passwordTooSimple", ImmutableMap.of());
            return;
        }

        user.updatePassword(newPassword);
        SyncUtils.<Void>runBlocking(v -> user.save(v));
        SyncUtils.<Void>runBlocking(v -> userSessionService.invalidateAllSessions(user.getId(), v));
        String userIp = requestBody.getString("userIp");

        if (!IpUtils.isValidIp(userIp)) {
            ErrorUtils.respondInvalidInput(ctx, "Ip address \"" + userIp + "\" is not valid.");
            return;
        }

        AuditLog.log(user.getId(), userIp, ctx, AuditLogActionType.USER_CHANGE_PASSWORD, (ignored, error) -> {
            if (error != null) {
                ErrorUtils.respondInternalError(ctx, error);
            } else {
                APIv3.respondJson(ctx, 200, ImmutableMap.of(
                        "success", true,
                        "uuid", user.getId()
                ));
            }
        });
    }

}
