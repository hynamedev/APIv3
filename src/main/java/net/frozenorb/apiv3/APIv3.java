package net.frozenorb.apiv3;

import com.google.common.net.MediaType;
import com.google.gson.Gson;

import net.frozenorb.apiv3.domain.*;
import net.frozenorb.apiv3.unsorted.actor.ActorType;
import net.frozenorb.apiv3.util.SyncUtils;
import net.frozenorb.apiv3.web.filter.AuthenticationFilter;
import net.frozenorb.apiv3.web.filter.AuthorizationFilter;
import net.frozenorb.apiv3.web.filter.MetricsFilter;
import net.frozenorb.apiv3.web.filter.WebsiteUserSessionFilter;
import net.frozenorb.apiv3.web.route.GETDumpsType;
import net.frozenorb.apiv3.web.route.GETSearch;
import net.frozenorb.apiv3.web.route.GETWhoAmI;
import net.frozenorb.apiv3.web.route.POSTLogout;
import net.frozenorb.apiv3.web.route.accessTokens.DELETEAccessTokensId;
import net.frozenorb.apiv3.web.route.accessTokens.GETAccessTokens;
import net.frozenorb.apiv3.web.route.accessTokens.GETAccessTokensId;
import net.frozenorb.apiv3.web.route.accessTokens.POSTAccessTokens;
import net.frozenorb.apiv3.web.route.auditLog.DELETEAuditLogId;
import net.frozenorb.apiv3.web.route.auditLog.GETAuditLog;
import net.frozenorb.apiv3.web.route.auditLog.POSTAuditLog;
import net.frozenorb.apiv3.web.route.bannedAsns.DELETEBannedAsnsId;
import net.frozenorb.apiv3.web.route.bannedAsns.GETBannedAsns;
import net.frozenorb.apiv3.web.route.bannedAsns.GETBannedAsnsId;
import net.frozenorb.apiv3.web.route.bannedAsns.POSTBannedAsns;
import net.frozenorb.apiv3.web.route.bannedCellCarriers.DELETEBannedCellCarriersId;
import net.frozenorb.apiv3.web.route.bannedCellCarriers.GETBannedCellCarriers;
import net.frozenorb.apiv3.web.route.bannedCellCarriers.GETBannedCellCarriersId;
import net.frozenorb.apiv3.web.route.bannedCellCarriers.POSTBannedCellCarriers;
import net.frozenorb.apiv3.web.route.chatFilter.DELETEChatFilterId;
import net.frozenorb.apiv3.web.route.chatFilter.GETChatFilter;
import net.frozenorb.apiv3.web.route.chatFilter.GETChatFilterId;
import net.frozenorb.apiv3.web.route.chatFilter.POSTChatFilter;
import net.frozenorb.apiv3.web.route.deployment.POSTDeploymentUpdateServer;
import net.frozenorb.apiv3.web.route.disposableLoginTokens.POSTDisposableLoginTokens;
import net.frozenorb.apiv3.web.route.disposableLoginTokens.POSTDisposableLoginTokensIdUse;
import net.frozenorb.apiv3.web.route.emailTokens.GETEmailTokensIdOwner;
import net.frozenorb.apiv3.web.route.emailTokens.POSTEmailTokensIdConfirm;
import net.frozenorb.apiv3.web.route.grants.DELETEGrantsId;
import net.frozenorb.apiv3.web.route.grants.GETGrants;
import net.frozenorb.apiv3.web.route.grants.GETGrantsId;
import net.frozenorb.apiv3.web.route.grants.POSTGrants;
import net.frozenorb.apiv3.web.route.ipBans.DELETEIpBansId;
import net.frozenorb.apiv3.web.route.ipBans.GETIpBans;
import net.frozenorb.apiv3.web.route.ipBans.GETIpBansId;
import net.frozenorb.apiv3.web.route.ipBans.POSTIpBans;
import net.frozenorb.apiv3.web.route.ipLog.GETIpLogId;
import net.frozenorb.apiv3.web.route.lookup.POSTLookupByName;
import net.frozenorb.apiv3.web.route.lookup.POSTLookupByUuid;
import net.frozenorb.apiv3.web.route.notificationTemplates.DELETENotificationTemplatesId;
import net.frozenorb.apiv3.web.route.notificationTemplates.GETNotificationTemplates;
import net.frozenorb.apiv3.web.route.notificationTemplates.GETNotificationTemplatesId;
import net.frozenorb.apiv3.web.route.notificationTemplates.POSTNotificationTemplates;
import net.frozenorb.apiv3.web.route.phoneIntel.GETPhoneInteld;
import net.frozenorb.apiv3.web.route.prefix.DELETEPrefixesId;
import net.frozenorb.apiv3.web.route.prefix.GETPrefixes;
import net.frozenorb.apiv3.web.route.prefix.GETPrefixesId;
import net.frozenorb.apiv3.web.route.prefix.POSTPrefixes;
import net.frozenorb.apiv3.web.route.prefixGrants.DELETEPrefixGrantsId;
import net.frozenorb.apiv3.web.route.prefixGrants.GETPrefixGrants;
import net.frozenorb.apiv3.web.route.prefixGrants.GETPrefixGrantsId;
import net.frozenorb.apiv3.web.route.prefixGrants.POSTPrefixGrants;
import net.frozenorb.apiv3.web.route.punishments.DELETEPunishmentsId;
import net.frozenorb.apiv3.web.route.punishments.DELETEUsersIdActivePunishment;
import net.frozenorb.apiv3.web.route.punishments.GETPunishments;
import net.frozenorb.apiv3.web.route.punishments.GETPunishmentsId;
import net.frozenorb.apiv3.web.route.punishments.POSTPunishments;
import net.frozenorb.apiv3.web.route.ranks.DELETERanksId;
import net.frozenorb.apiv3.web.route.ranks.GETRanks;
import net.frozenorb.apiv3.web.route.ranks.GETRanksId;
import net.frozenorb.apiv3.web.route.ranks.POSTRanks;
import net.frozenorb.apiv3.web.route.serverGroups.DELETEServerGroupsId;
import net.frozenorb.apiv3.web.route.serverGroups.GETServerGroups;
import net.frozenorb.apiv3.web.route.serverGroups.GETServerGroupsId;
import net.frozenorb.apiv3.web.route.serverGroups.GETServerGroupsPlayerCount;
import net.frozenorb.apiv3.web.route.serverGroups.POSTServerGroups;
import net.frozenorb.apiv3.web.route.servers.DELETEServersId;
import net.frozenorb.apiv3.web.route.servers.GETServers;
import net.frozenorb.apiv3.web.route.servers.GETServersId;
import net.frozenorb.apiv3.web.route.servers.GETServersPlayerCountId;
import net.frozenorb.apiv3.web.route.servers.POSTServers;
import net.frozenorb.apiv3.web.route.servers.POSTServersHeartbeat;
import net.frozenorb.apiv3.web.route.users.*;
import net.frozenorb.apiv3.util.SpringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.TimeoutHandler;

@Component
public final class APIv3 {

	@PostConstruct
	public void updateCaches() {
		BannedAsn.updateCache();
		BannedCellCarrier.updateCache();
		Rank.updateCache();
		Prefix.updateCache();
		Server.updateCache();
		ServerGroup.updateCache();
	}

	@Bean
	public Router router(Vertx vertx, HttpServerOptions httpServerOptions, @Value("${http.port}") int port) {
		HttpServer webServer = vertx.createHttpServer(httpServerOptions);
		Router router = Router.router(vertx);

		//make default registrar
		AccessToken accessToken = new AccessToken("practice", ActorType.SERVER, Arrays.asList("76.81.244.230", "127.0.0.1"));


		router.route().handler(LoggerHandler.create(LoggerFormat.TINY));
		router.route().handler(TimeoutHandler.create(TimeUnit.SECONDS.toMillis(5)));
		router.route().method(HttpMethod.PUT).method(HttpMethod.POST).method(HttpMethod.DELETE).handler(BodyHandler.create());
		router.route().handler(SpringUtils.getBean(AuthenticationFilter.class));
		router.route().handler(SpringUtils.getBean(MetricsFilter.class));
		router.route().handler(SpringUtils.getBean(WebsiteUserSessionFilter.class));
		router.route().handler(SpringUtils.getBean(AuthorizationFilter.class));
		router.exceptionHandler(Throwable::printStackTrace);


		// TODO: The commented out routes

		httpGet(router, "/accessTokens/:accessToken", GETAccessTokensId.class);
		httpGet(router, "/accessTokens", GETAccessTokens.class);
		httpPost(router, "/accessTokens", POSTAccessTokens.class);
		//httpPut(router, "/accessTokens/:accessToken", PUTAccessTokensId.class);
		httpDelete(router, "/accessTokens/:accessToken", DELETEAccessTokensId.class);

		httpGet(router, "/auditLog", GETAuditLog.class);
		httpPost(router, "/auditLog", POSTAuditLog.class);
		httpDelete(router, "/auditLog/:auditLogEntryId", DELETEAuditLogId.class);

		httpGet(router, "/bannedAsns/:bannedAsn", GETBannedAsnsId.class);
		httpGet(router, "/bannedAsns", GETBannedAsns.class);
		httpPost(router, "/bannedAsns", POSTBannedAsns.class);
		//httpPut(router, "/bannedAsns/:bannedAsn", PUTBannedAsnsId.class);
		httpDelete(router, "/bannedAsns/:bannedAsn", DELETEBannedAsnsId.class);

		httpGet(router, "/bannedCellCarriers/:bannedCellCarrier", GETBannedCellCarriersId.class);
		httpGet(router, "/bannedCellCarriers", GETBannedCellCarriers.class);
		httpPost(router, "/bannedCellCarriers", POSTBannedCellCarriers.class);
		//httpPut(router, "/bannedCellCarriers/:bannedCellCarrier", PUTBannedCellCarriersId.class);
		httpDelete(router, "/bannedCellCarriers/:bannedCellCarrier", DELETEBannedCellCarriersId.class);

		httpGet(router, "/chatFilter/:chatFilterEntryId", GETChatFilterId.class);
		httpGet(router, "/chatFilter", GETChatFilter.class);
		httpPost(router, "/chatFilter", POSTChatFilter.class);
		//httpPut(router, "/chatFilter/:chatFilterEntryId", PUTChatFilterId.class);
		httpDelete(router, "/chatFilter/:chatFilterEntryId", DELETEChatFilterId.class);

		httpPost(router, "/deployment/updateServer/:serverId", POSTDeploymentUpdateServer.class);

		httpPost(router, "/disposableLoginTokens", POSTDisposableLoginTokens.class);
		httpPost(router, "/disposableLoginTokens/:disposableLoginToken/use", POSTDisposableLoginTokensIdUse.class);

		httpGet(router, "/emailTokens/:emailToken/owner", GETEmailTokensIdOwner.class);
		httpPost(router, "/emailTokens/:emailToken/confirm", POSTEmailTokensIdConfirm.class);

		httpGet(router, "/grants/:grantId", GETGrantsId.class);
		httpGet(router, "/grants", GETGrants.class);
		httpPost(router, "/grants", POSTGrants.class);
		//httpPut(router, "/grants/:grantId", PUTGrantsId.class);
		httpDelete(router, "/grants/:grantId", DELETEGrantsId.class);

		httpGet(router, "/ipBans/:ipBanId", GETIpBansId.class);
		httpGet(router, "/ipBans", GETIpBans.class);
		httpPost(router, "/ipBans", POSTIpBans.class);
		//httpPut(router, "/ipBans/:ipBanId", PUTIpBansId.class);
		httpDelete(router, "/ipBans/:ipBanId", DELETEIpBansId.class);

		httpGet(router, "/ipLog/:id", GETIpLogId.class);

		httpPost(router, "/lookup/byName", POSTLookupByName.class);
		httpPost(router, "/lookup/byUuid", POSTLookupByUuid.class);

		httpGet(router, "/notificationTemplates/:notificationTemplateId", GETNotificationTemplatesId.class);
		httpGet(router, "/notificationTemplates", GETNotificationTemplates.class);
		httpPost(router, "/notificationTemplates", POSTNotificationTemplates.class);
		//httpPut(router, "/notificationTemplates/:notificationTemplateId", PUTNotificationTemplatesId.class);
		httpDelete(router, "/notificationTemplates/:notificationTemplateId", DELETENotificationTemplatesId.class);

		httpGet(router, "/phoneIntel/:phone", GETPhoneInteld.class);

		httpGet(router, "/punishments/:punishmentId", GETPunishmentsId.class);
		httpGet(router, "/punishments", GETPunishments.class);
		httpPost(router, "/punishments", POSTPunishments.class);
		//httpPut(router, "/punishments/:punishmentId", PUTPunishmentsId.class);
		httpDelete(router, "/punishments/:punishmentId", DELETEPunishmentsId.class);
		httpDelete(router, "/users/:userId/activePunishment", DELETEUsersIdActivePunishment.class);

		httpGet(router, "/ranks/:rankId", GETRanksId.class);
		httpGet(router, "/ranks", GETRanks.class);
		httpPost(router, "/ranks", POSTRanks.class);
		//httpPut(router, "/ranks/:rankId", PUTRanksId.class);
		httpDelete(router, "/ranks/:rankId", DELETERanksId.class);

		httpGet(router, "/serverGroups/:serverGroupId", GETServerGroupsId.class);
	    httpGet(router, "/serverGroups/:serverGroupId/playerCount", GETServerGroupsPlayerCount.class);
		httpGet(router, "/serverGroups", GETServerGroups.class);
		httpPost(router, "/serverGroups", POSTServerGroups.class);
		//httpPut(router, "/serverGroups/:serverGroupId", PUTServerGroupsId.class);
		httpDelete(router, "/serverGroups/:serverGroupId", DELETEServerGroupsId.class);

		httpGet(router, "/servers/:serverId", GETServersId.class);
		httpGet(router, "/servers/:serverId/playerCount", GETServersPlayerCountId.class);
		httpGet(router, "/servers", GETServers.class);
		httpPost(router, "/servers/heartbeat", POSTServersHeartbeat.class);
		httpPost(router, "/servers", POSTServers.class);
		//httpPut(router, "/servers/:serverId", PUTServersId.class);
		httpDelete(router, "/servers/:serverId", DELETEServersId.class);

		httpGet(router, "/prefixes/:prefixId", GETPrefixesId.class);
		httpGet(router, "/prefixes", GETPrefixes.class);
		httpPost(router, "/prefixes", POSTPrefixes.class);
		//httpPut(router, "/prefixes/:prefixId", PUTPrefixesId.class);
		httpDelete(router, "/prefixes/:prefixId", DELETEPrefixesId.class);

		httpGet(router, "/prefixes/grants/:grantId", GETPrefixGrantsId.class);
		httpGet(router, "/prefixes/grants", GETPrefixGrants.class);
		httpPost(router, "/prefixes/grants", POSTPrefixGrants.class);
		//httpPut(router, "/grants/:grantId", PUTPrefixGrantsId.class);
		httpDelete(router, "/prefixes/grants/:grantId", DELETEPrefixGrantsId.class);

		httpGet(router, "/staff", GETStaff.class);
		httpGet(router, "/users/:userId", GETUsersId.class);
		httpGet(router, "/users/:userId/compoundedPermissions", GETUsersIdCompoundedPermissions.class);
		httpGet(router, "/users/:userId/details", GETUsersIdDetails.class);
		httpGet(router, "/users/:userId/requiresTotp", GETUsersIdRequiresTotp.class);
		httpGet(router, "/users/:userId/verifyPassword", GETUsersIdVerifyPassword.class);
		httpPost(router, "/users/:userId/changePassword", POSTUsersIdChangePassword.class);
		httpPost(router, "/users/:userId/confirmPhone", POSTUsersIdConfirmPhone.class);
		httpPost(router, "/users/:userId/login", POSTUsersIdLogin.class);
		httpPost(router, "/users/:userId/notify", POSTUsersIdNotify.class);
		httpPost(router, "/users/:userId/passwordReset", POSTUsersIdPasswordReset.class);
		httpPost(router, "/users/:userId/registerEmail", POSTUsersIdRegisterEmail.class);
		httpPost(router, "/users/:userId/registerPhone", POSTUsersIdRegisterPhone.class);
		httpPost(router, "/users/usePasswordResetToken", POSTUsersUsePasswordResetToken.class);
		httpPost(router, "/users/:userId/setupTotp", POSTUsersIdSetupTotp.class);
		httpPost(router, "/users/:userId/verifyTotp", POSTUsersIdVerifyTotp.class);
		httpPost(router, "/users/:userId/colors", POSTUsersIdChangeColors.class);
		httpPost(router, "/users/:userId/prefix", POSTUsersIdChangePrefix.class);

		httpGet(router, "/dumps/:dumpType", GETDumpsType.class);
		httpGet(router, "/search", GETSearch.class);
		httpGet(router, "/whoami", GETWhoAmI.class);
		httpPost(router, "/logout", POSTLogout.class);

		webServer.requestHandler(router::accept).listen(port);
		return router;
	}

	public static void respondJson(RoutingContext ctx, int code, Object response) {
		ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());

		if (response == null) {
			ctx.response().setStatusCode(404);
			ctx.response().end("{}");
		} else {
			ctx.response().setStatusCode(code);
			ctx.response().end(SpringUtils.getBean(Gson.class).toJson(response));
		}
	}

	   public static void respondRawJson(RoutingContext ctx, int code, String response) {
	        ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());

	        if (response == null) {
	            ctx.response().setStatusCode(404);
	            ctx.response().end("{}");
	        } else {
	            ctx.response().setStatusCode(code);
	            ctx.response().end(response);
	        }
	    }

	private void httpGet(Router router, String route, Class<?> handler) {
		router.get(route).blockingHandler((Handler<RoutingContext>) SpringUtils.getBean(handler), false);
	}

	private void httpPost(Router router, String route, Class<?> handler) {
		router.post(route).blockingHandler((Handler<RoutingContext>) SpringUtils.getBean(handler), false);
	}

	private void httpDelete(Router router, String route, Class<?> handler) {
		router.delete(route).blockingHandler((Handler<RoutingContext>) SpringUtils.getBean(handler), false);
	}

}