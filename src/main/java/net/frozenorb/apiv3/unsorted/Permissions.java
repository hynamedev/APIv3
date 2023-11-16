package net.frozenorb.apiv3.unsorted;

import net.frozenorb.apiv3.util.SpringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Permissions {

	private static final String rootPermission = SpringUtils.getProperty("network.rootPermission");

	public static final String PROTECTED_PUNISHMENT = rootPermission + ".punishment.protected";
	public static final String BYPASS_VPN_CHECK = rootPermission + ".vpn.bypass";
	public static final String REQUIRE_TOTP_CODE = rootPermission + ".totp.require";
	public static final String CREATE_PUNISHMENT = rootPermission + ".punishment.create";
	public static final String REMOVE_PUNISHMENT = rootPermission + ".punishment.remove";
	public static final String CREATE_GRANT = rootPermission + ".grant.create";
	public static final String REMOVE_GRANT = rootPermission + ".grant.remove";
	public static final String CREATE_PREFIXGRANT = rootPermission + ".prefixgrant.create";
	public static final String REMOVE_PREFIXGRANT = rootPermission + ".prefixgrant.remove";

}