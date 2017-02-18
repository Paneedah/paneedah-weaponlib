package com.vicmatskiv.weaponlib.state;

public interface ExtendedState<T extends ManagedState<T>> {

	public boolean setState(T updateToState);

	public T getState();
	
	public long getStateUpdateTimestamp();
}
