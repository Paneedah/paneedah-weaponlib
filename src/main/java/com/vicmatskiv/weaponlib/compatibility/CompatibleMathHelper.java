package com.vicmatskiv.weaponlib.compatibility;

import net.minecraft.util.math.MathHelper;

public class CompatibleMathHelper {

	public static float cos(float in) {
		return MathHelper.cos(in);
	}

	public static float sin(float in) {
		return MathHelper.sin(in);
	}

	public static float sqrt_float(float in) {
		return MathHelper.sqrt(in);
	}

	public static float sqrt_double(double in) {
		return MathHelper.sqrt(in);
	}
}
