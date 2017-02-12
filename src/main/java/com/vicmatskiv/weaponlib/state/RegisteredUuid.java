package com.vicmatskiv.weaponlib.state;

import java.util.UUID;

public final class RegisteredUuid {

	private UUID value;

	RegisteredUuid(UUID value) {
		this.value = value;
	}

	public UUID getValue() {
		return value;
	}
}
