package net.frozenorb.apiv3.unsorted.actor;

public interface Actor {

	boolean isAuthorized();

	String getName();

	ActorType getType();

}