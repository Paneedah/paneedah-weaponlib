package com.vicmatskiv.weaponlib.numerical;

import java.util.Random;

import com.vicmatskiv.weaponlib.animation.ClientValueRepo;
import com.vicmatskiv.weaponlib.animation.MatrixHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.datafix.fixes.MinecartEntityTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

public class RandomVector {
	
	private double dirX, dirY, dirZ;
	private double x, y, z;
	private double prevX, prevY, prevZ;
	
	// value between 0.0-1.0
	private double agressiveness = 0.2;
	
	public RandomVector() {
		
	}
	
	
	public void update(double speed, double dampening) {
		
		
		prevX = x; 
		prevY = y;
		prevZ = z;
		
		x += dirX*speed;
		y += dirY*speed;
		z += dirZ*speed;
		
		dirX *= dampening;
		dirY *= dampening;
		dirZ *= dampening;
		
		x *= dampening;
		y *= dampening;
		z *= dampening;
		
	}
	
	public double getX() {
		return this.x;
	}
	
	public void setAgressiveness(double agr) {
		this.agressiveness = agr;
	}
	
	public double getY() {
		return this.y;
	}
	
	public double getZ() {
		return this.z;
	}
	
	public void callRandom(double mag) {
		
		double halfMag = mag/2;
		
		if(Math.random() < agressiveness) {
			this.dirX = Math.random()*mag - halfMag;
			this.dirY = Math.random()*mag - halfMag;
			this.dirZ = Math.random()*mag - halfMag;
		}
		
		
	
	}
	
	public Vec3d getVector(double amplitude) {
		return new Vec3d(this.x*amplitude, this.y*amplitude, this.z*amplitude);
	}
	
	public Vec3d getInterpolatedVector(double amplitude) {
		float ticks = Minecraft.getMinecraft().getRenderPartialTicks();
		return new Vec3d(MatrixHelper.solveLerp(this.prevX, this.x, ticks),
				MatrixHelper.solveLerp(this.prevY, this.y, ticks),
				MatrixHelper.solveLerp(this.prevZ, this.z, ticks)).scale(amplitude);
	}

}
