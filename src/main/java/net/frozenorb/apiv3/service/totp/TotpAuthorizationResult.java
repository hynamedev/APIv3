package net.frozenorb.apiv3.service.totp;

import lombok.Getter;

public enum TotpAuthorizationResult {

	AUTHORIZED_NOT_SET(true),
	AUTHORIZED_IP_PRE_AUTH(true),
	AUTHORIZED_GOOD_CODE(true),
	NOT_AUTHORIZED_NOT_SET(false),
	NOT_AUTHORIZED_RECENTLY_USED(false),
	NOT_AUTHORIZED_BAD_CODE(false);

	@Getter private final boolean authorized;

	TotpAuthorizationResult(boolean authorized) {
		this.authorized = authorized;
	}

}