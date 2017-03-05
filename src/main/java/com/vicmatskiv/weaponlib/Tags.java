package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import com.vicmatskiv.weaponlib.network.TypeRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;

public final class Tags {

	private static final String ACTIVE_TEXTURE_INDEX_TAG = "ActiveTextureIndex";
	private static final String LASER_ON_TAG = "LaserOn";
	private static final String AMMO_TAG = "Ammo";

	private static final String DEFAULT_TIMER_TAG = "DefaultTimer";
	
	private static final String INSTANCE_TAG = "Instance";

	static boolean isLaserOn(ItemStack itemStack) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return false;
		return compatibility.getTagCompound(itemStack).getBoolean(LASER_ON_TAG);
	}

	static void setLaser(ItemStack itemStack, boolean enabled) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
		compatibility.getTagCompound(itemStack).setBoolean(LASER_ON_TAG, enabled);
	}

	static int getAmmo(ItemStack itemStack) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return 0;
		return compatibility.getTagCompound(itemStack).getInteger(AMMO_TAG);
	}

	static void setAmmo(ItemStack itemStack, int ammo) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
		compatibility.getTagCompound(itemStack).setInteger(AMMO_TAG, ammo);
	}

//	static void setAimed(ItemStack itemStack, boolean aimed) {
//		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
//		compatibility.getTagCompound(itemStack).setBoolean(AIMED_TAG, aimed);
//	}
//	
//	static boolean isAimed(ItemStack itemStack) {
//		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return false;
//		return compatibility.getTagCompound(itemStack).getBoolean(Tags.AIMED_TAG);
//	}
//	
//	static float getAllowedZoom(ItemStack itemStack) {
//		if (itemStack == null || itemStack.getTagCompound() == null) {
//			return 0f;
//		}
//		return itemStack.getTagCompound().getFloat(ALLOWED_ZOOM_TAG);
//	}
//
//	static void setAllowedZoom(ItemStack itemStack, float zoom) {
//		setAllowedZoom(itemStack, zoom, false);
//	}
//	
//	static void setAllowedZoom(ItemStack itemStack, float zoom, boolean attachmentOnlyZoomMode) {
//		if (itemStack == null || itemStack.getTagCompound() == null) {
//			return;
//		}
//		itemStack.getTagCompound().setFloat(ALLOWED_ZOOM_TAG, zoom);
//		compatibility.getTagCompound(itemStack).setBoolean(ZOOM_MODE_TAG, attachmentOnlyZoomMode);
//	}
//
//	static float getZoom(ItemStack itemStack) {
//		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return 0f;
//		return compatibility.getTagCompound(itemStack).getFloat(ZOOM_TAG);
//	}
//	
//	static boolean isAttachmentOnlyZoom(ItemStack itemStack) {
//		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return false;
//		return compatibility.getTagCompound(itemStack).getBoolean(ZOOM_MODE_TAG);
//	}
//
//	static void setZoom(ItemStack itemStack, float zoom) {
//		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
//		compatibility.getTagCompound(itemStack).setFloat(ZOOM_TAG, zoom);
//		
//	}

	static void setActiveTexture(ItemStack itemStack, int currentIndex) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
		compatibility.getTagCompound(itemStack).setInteger(ACTIVE_TEXTURE_INDEX_TAG, currentIndex);
	}
	
	static int getActiveTexture(ItemStack itemStack) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return 0;
		return compatibility.getTagCompound(itemStack).getInteger(ACTIVE_TEXTURE_INDEX_TAG);
	}
	
	public static long getDefaultTimer(ItemStack itemStack) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return 0;
		return compatibility.getTagCompound(itemStack).getLong(DEFAULT_TIMER_TAG);
	}

	static void setDefaultTimer(ItemStack itemStack, long ammo) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
		compatibility.getTagCompound(itemStack).setLong(DEFAULT_TIMER_TAG, ammo);
	}
		
	static PlayerItemInstance<?> getInstance(ItemStack itemStack) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return null;
		
		byte[] bytes = compatibility.getTagCompound(itemStack).getByteArray(INSTANCE_TAG);
		if(bytes != null && bytes.length > 0) {
			return TypeRegistry.getInstance().fromBytes(Unpooled.wrappedBuffer(bytes));
		}
		return null;
	}
	
	static <T extends PlayerItemInstance<?>> T getInstance(ItemStack itemStack, Class<T> targetClass) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return null;
		
		byte[] bytes = compatibility.getTagCompound(itemStack).getByteArray(INSTANCE_TAG);
		if(bytes != null && bytes.length > 0) {
			try {
				return targetClass.cast(TypeRegistry.getInstance().fromBytes(Unpooled.wrappedBuffer(bytes)));
			} catch(RuntimeException e) {
				return null;
			}
		}
		return null;
	}
	
	static void setInstance(ItemStack itemStack, PlayerItemInstance<?> instance) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return;
		ByteBuf buf = Unpooled.buffer();
		if(instance != null) {
			TypeRegistry.getInstance().toBytes(instance, buf);
			compatibility.getTagCompound(itemStack).setByteArray(INSTANCE_TAG, buf.array());
		} else {
			compatibility.getTagCompound(itemStack).removeTag(INSTANCE_TAG);
		}
		
	}

	public static byte[] getInstanceBytes(ItemStack itemStack) {
		if(itemStack == null || compatibility.getTagCompound(itemStack) == null) return null;
		return compatibility.getTagCompound(itemStack).getByteArray(INSTANCE_TAG);
	}
}
