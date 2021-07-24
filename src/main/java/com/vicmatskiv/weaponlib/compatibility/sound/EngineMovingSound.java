package com.vicmatskiv.weaponlib.compatibility.sound;

import java.util.function.Supplier;

import com.vicmatskiv.weaponlib.compatibility.CompatibleSound;
import com.vicmatskiv.weaponlib.compatibility.CompatibleVec3;
import com.vicmatskiv.weaponlib.vehicle.EntityVehicle;

public class EngineMovingSound extends AdvCompatibleMovingSound {

	public EntityVehicle vehicle;
	
	public EngineMovingSound(CompatibleSound sound, Supplier<CompatibleVec3> positionProvider,
			Supplier<Boolean> donePlayingProvider, EntityVehicle vehicle, boolean shouldFade) {
		super(sound, positionProvider, donePlayingProvider, shouldFade);
		
		this.vehicle = vehicle;
	}
	
	@Override
	public void update() {
		// TODO Auto-generated method stub
		super.update();
		int rpm = vehicle.getSolver().currentRPM;
		
		if(!isDonePlaying()) {
			//this.pitch = (float) 2.0*(vehicle.solver.currentRPM/4500.0F);
			this.volume = 1.0f;
			this.pitch = 1.0f;
		}
	}

}
