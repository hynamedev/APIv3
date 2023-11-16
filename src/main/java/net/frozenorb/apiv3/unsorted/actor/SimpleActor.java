package net.frozenorb.apiv3.unsorted.actor;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public final class SimpleActor implements Actor {

	@Getter private String name;
	@Getter private ActorType type;
	@Getter private boolean authorized;

}