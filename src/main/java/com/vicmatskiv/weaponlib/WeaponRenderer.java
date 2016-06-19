package com.vicmatskiv.weaponlib;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import com.vicmatskiv.weaponlib.animation.PositionProvider;
import com.vicmatskiv.weaponlib.animation.RenderStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer;
import scala.actors.threadpool.Arrays;


public class WeaponRenderer implements IItemRenderer, PositionProvider<RenderableState> {
	
	

	public static class Builder {
		private ModelBase model;
		private String textureName;
		private float weaponProximity;
		private float yOffsetZoom;
		private float xOffsetZoom = 0.69F;
		
		private Consumer<ItemStack> entityPositioning;
		private Consumer<ItemStack> inventoryPositioning;
		private BiConsumer<EntityPlayer, ItemStack> thirdPersonPositioning;
		private BiConsumer<EntityPlayer, ItemStack> firstPersonPositioning;
		private BiConsumer<EntityPlayer, ItemStack> firstPersonPositioningZooming;
		private BiConsumer<EntityPlayer, ItemStack> firstPersonPositioningRunning;
		private BiConsumer<EntityPlayer, ItemStack> firstPersonPositioningModifying;
		
		private List<BiConsumer<EntityPlayer, ItemStack>> firstPersonPositioningReloading;
		private String modId;
		private long reloadingAnimationDuration;
		
		public Builder withModId(String modId) {
			this.modId = modId;
			return this;
		}
		
		public Builder withModel(ModelBase model) {
			this.model = model;
			return this;
		}
		
		public Builder withTextureName(String textureName) {
			this.textureName = textureName + ".png";
			return this;
		}
		
		public Builder withWeaponProximity(float weaponProximity) {
			this.weaponProximity = weaponProximity;
			return this;
		}
		
		public Builder withYOffsetZoom(float yOffsetZoom) {
			this.yOffsetZoom = yOffsetZoom;
			return this;
		}
		
		public Builder withXOffsetZoom(float xOffsetZoom) {
			this.xOffsetZoom = xOffsetZoom;
			return this;
		}
		
		public Builder withEntityPositioning(Consumer<ItemStack> entityPositioning) {
			this.entityPositioning = entityPositioning;
			return this;
		}
		
		public Builder withInventoryPositioning(Consumer<ItemStack> inventoryPositioning) {
			this.inventoryPositioning = inventoryPositioning;
			return this;
		}

		public Builder withThirdPersonPositioning(BiConsumer<EntityPlayer, ItemStack> thirdPersonPositioning) {
			this.thirdPersonPositioning = thirdPersonPositioning;
			return this;
		}

		public Builder withFirstPersonPositioning(BiConsumer<EntityPlayer, ItemStack> firstPersonPositioning) {
			this.firstPersonPositioning = firstPersonPositioning;
			return this;
		}
		
		public Builder withFirstPersonPositioningRunning(BiConsumer<EntityPlayer, ItemStack> firstPersonPositioningRunning) {
			this.firstPersonPositioningRunning = firstPersonPositioningRunning;
			return this;
		}
		
		public Builder withFirstPersonPositioningZooming(BiConsumer<EntityPlayer, ItemStack> firstPersonPositioningZooming) {
			this.firstPersonPositioningZooming = firstPersonPositioningZooming;
			return this;
		}
		
		@SafeVarargs
		@SuppressWarnings("unchecked")
		public final Builder withFirstPersonPositioningReloading(long duration, BiConsumer<EntityPlayer, ItemStack> ...firstPersonPositioningReloading) {
			this.reloadingAnimationDuration = duration;
			this.firstPersonPositioningReloading = Arrays.asList(firstPersonPositioningReloading);
			return this;
		}
		
		public Builder withFirstPersonPositioningModifying(BiConsumer<EntityPlayer, ItemStack> firstPersonPositioningModifying) {
			this.firstPersonPositioningModifying = firstPersonPositioningModifying;
			return this;
		}

		public WeaponRenderer build() {
			if(modId == null) {
				throw new IllegalStateException("ModId is not set");
			}
			
			if(inventoryPositioning == null) {
				inventoryPositioning = itemStack -> {GL11.glTranslatef(0,  0.12f, 0);};
			}
			
			if(entityPositioning == null) {
				entityPositioning = itemStack -> {
				};
			}
			
			if(firstPersonPositioning == null) {
				firstPersonPositioning = (player, itemStack) -> {
					GL11.glRotatef(45F, 0f, 1f, 0f);
					if(itemStack.stackTagCompound != null && itemStack.stackTagCompound.getFloat(Weapon.ZOOM_TAG) != 1.0f) {
						GL11.glTranslatef(xOffsetZoom, yOffsetZoom, weaponProximity);
					} else {
						GL11.glTranslatef(0F, -1.2F, 0F);
					}
				};
			}
			
			if(firstPersonPositioningReloading == null) {
				firstPersonPositioningReloading = Collections.singletonList(firstPersonPositioning);
			}
			
			if(thirdPersonPositioning == null) {
				thirdPersonPositioning = (player, itemStack) -> {
					GL11.glTranslatef(-0.4F, 0.2F, 0.4F);
					GL11.glRotatef(-45F, 0f, 1f, 0f);
					GL11.glRotatef(70F, 1f, 0f, 0f);
				};
			}
			
			return new WeaponRenderer(this);
		}
	}
	
	private Builder builder;
	
	private Map<EntityPlayer, RenderStateManager<RenderableState>> firstPersonStateManagers = new HashMap<>();
	
	private WeaponRenderer (Builder builder)
	{
		this.builder = builder;
	}
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type)
	{
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
	{
		return true;
	}
	
	private RenderStateManager<RenderableState> getStateManager(EntityPlayer player, ItemStack itemStack) {
		RenderableState currentState = null;
		long animationDuration = 250;
		if(((Weapon) itemStack.getItem()).getState(itemStack) == Weapon.STATE_MODIFYING && builder.firstPersonPositioningModifying != null) {
			currentState = RenderableState.MODIFYING;
		} else if(player.isSprinting() && builder.firstPersonPositioningRunning != null) {
			currentState = RenderableState.RUNNING;
		} else if(Weapon.isReloading(player, itemStack)) {
			currentState = RenderableState.RELOADING;
			animationDuration = builder.reloadingAnimationDuration;
		} else if(Weapon.isZoomed(itemStack)) {
			currentState = RenderableState.ZOOMING;
		} else{
			currentState = RenderableState.NORMAL;
		}
		
		RenderStateManager<RenderableState> stateManager = firstPersonStateManagers.get(player);
		if(stateManager == null) {
			stateManager = new RenderStateManager<>(currentState, this);
			firstPersonStateManagers.put(player, stateManager);
		} else {
			stateManager.setState(currentState, animationDuration);
		}
		return stateManager;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data)
	{
		GL11.glPushMatrix();
		
		GL11.glScaled(-1F, -1F, 1F);
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		switch (type)
		{
		case ENTITY:
			builder.entityPositioning.accept(item);
			break;
		case INVENTORY:
			builder.inventoryPositioning.accept(item);
			break;
		case EQUIPPED:
			
			builder.thirdPersonPositioning.accept(player, item);
			break;
		case EQUIPPED_FIRST_PERSON:
//			if(((Weapon) item.getItem()).getState(item) == Weapon.STATE_MODIFYING && builder.firstPersonPositioningModifying != null) {
//				builder.firstPersonPositioningModifying.accept(player, item);
//			} else if(player.isSprinting() && builder.firstPersonPositioningRunning != null) {
//				builder.firstPersonPositioningRunning.accept(player, item);
//			} else{
//				builder.firstPersonPositioning.accept(player, item);
//			}
			RenderStateManager<RenderableState> stateManager = getStateManager(player, item);
			stateManager.getPosition().accept(player, item);
			break;
		default:
		}
		
		if(builder.textureName != null) {
			Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(builder.modId 
					+ ":textures/models/" + builder.textureName));
		} else {
			Weapon weapon = ((Weapon) item.getItem());
			String textureName = weapon.getActiveTextureName(item);
			if(textureName != null) {
				Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(builder.modId 
						+ ":textures/models/" + textureName));
			}
		}
		
		builder.model.render(null,  0.0F, 0.0f, -0.4f, 0.0f, 0.0f, 0.08f);
		if(builder.model instanceof ModelWithAttachments) {
			List<CompatibleAttachment<Weapon>> attachments = ((Weapon) item.getItem()).getActiveAttachments(item);
			((ModelWithAttachments)builder.model).renderAttachments(builder.modId, item, 
					type, attachments , null,  0.0F, 0.0f, -0.4f, 0.0f, 0.0f, 0.08f);
		}
		
		GL11.glPopMatrix();
	   
	}

//	private double getDistanceFromItemToTarget(EntityClientPlayerMP player, double itemX, double itemY, double itemZ) {
//		FloatBuffer buf = BufferUtils.createFloatBuffer(16);
//
//	    // Get current modelview matrix:
//	    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf);
//
//	    // Rewind buffer. Not sure if this is needed, but it can't hurt.
//	    buf.rewind();
//
//	    // Create a Matrix4f.
//	    Matrix4f mat = new Matrix4f();
//
//	    // Load matrix from buf into the Matrix4f.
//	    mat.load(buf);
//	    
//	    Vec3 absolutePlayerPosition = player.getPosition(1);
//	    Vector4f startOfRay = new Vector4f((float)itemX, (float)itemY, (float)itemZ, 1f); //(float)pos.xCoord, (float)pos.yCoord, (float)pos.zCoord, 1f);
//		Vector4f relativeRayStartPosition = new Vector4f();
//		Matrix4f.transform(mat, startOfRay, relativeRayStartPosition);
//		
//		
//		
//		//MovingObjectPosition mouseOver = Minecraft.getMinecraft().objectMouseOver;
//		//System.out.println("Mouse over: " + mouseOver);
//		
//
//		
//		Vec3 targetPosition = player.rayTrace(1000, 1f).hitVec;
//		
//		
//		Vec3 absoluteRayStartPos = absolutePlayerPosition.addVector(relativeRayStartPosition.x, relativeRayStartPosition.y, relativeRayStartPosition.z); //.addVector(-itemX, -itemY, -itemZ);
//		
//		
//		//MovingObjectPosition result = Minecraft.getMinecraft().theWorld.rayTraceBlocks(absoluteRayStartPos, absoluteRayEndPos);
//
//		
//		double distance = absoluteRayStartPos.distanceTo(targetPosition);
//		
//		Vector4f endOfRay = new Vector4f((float)itemX, (float)itemY, (float)itemZ - (float)distance, 1f);
//		
//		Vector4f relativeRayEndPosition = new Vector4f();
//		Matrix4f.transform(mat, endOfRay, relativeRayEndPosition);
//		
//		Vec3 absoluteRayEndPos = absolutePlayerPosition.addVector(relativeRayEndPosition.x, relativeRayEndPosition.y, relativeRayEndPosition.z);
//		
//		if(System.currentTimeMillis() % 1000 == 0) {
//			System.out.println("Relative item pos: " + relativeRayStartPosition.x + ", " + relativeRayStartPosition.y + ", " + relativeRayStartPosition.z);
////			if(result != null) {
////				System.out.println("Hit info " + result.hitInfo);
////			} else {
////				System.out.println("No trace found");
////			}
//			
//			System.out.println("Absolute item pos: " + absoluteRayStartPos);
//			System.out.println("Distance to target: " + distance);
//			System.out.println("Ray end position: " + absoluteRayEndPos);
//			System.out.println("Target position:  " + targetPosition);
//		}
//		return distance + 100;
//	}

	private void getMatrix(EntityClientPlayerMP player, String msg) {
		// Create FloatBuffer that can hold 16 values.
		    FloatBuffer buf = BufferUtils.createFloatBuffer(16);

		    // Get current modelview matrix:
		    GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf);

		    // Rewind buffer. Not sure if this is needed, but it can't hurt.
		    buf.rewind();

		    // Create a Matrix4f.
		    Matrix4f mat = new Matrix4f();

		    // Load matrix from buf into the Matrix4f.
		    mat.load(buf);
		    
		    //System.out.println("Current matrix " + msg + ": " + mat);
		    
		    Vec3 pos = player.getPosition(1);
		    Vector4f currentPos = new Vector4f(0f, 0f, 0f, 1f); //(float)pos.xCoord, (float)pos.yCoord, (float)pos.zCoord, 1f);
			Vector4f dest = new Vector4f();
			Matrix4f.transform(mat, currentPos, dest);
			//System.out.println("Relative item pos: " + dest.x + ", " + dest.y + ", " + dest.z);
			MovingObjectPosition mouseOver = Minecraft.getMinecraft().objectMouseOver;
			//System.out.println("Mouse over: " + mouseOver);
			
			Vec3 targetPosition = player.rayTrace(1000, 1f).hitVec;
			
			
			Vec3 absoluteItemPos = pos.addVector(dest.x, dest.y, dest.z);
			System.out.println("Absolute item pos: " + absoluteItemPos);
			
			double distance = absoluteItemPos.distanceTo(targetPosition);
			System.out.println("Distance to target: " + distance);
	}

	@Override
	public List<BiConsumer<EntityPlayer, ItemStack>> getPositioning(RenderableState state) {
		switch(state) {
		case MODIFYING:
			return Collections.singletonList(builder.firstPersonPositioningModifying);
		case RUNNING:
			return Collections.singletonList(builder.firstPersonPositioningRunning);
		case RELOADING:
			return builder.firstPersonPositioningReloading;
		case NORMAL:
			return Collections.singletonList(builder.firstPersonPositioning);
		case ZOOMING:
			return Collections.singletonList(
					builder.firstPersonPositioningZooming != null ? builder.firstPersonPositioningZooming : builder.firstPersonPositioning);
		default:
			break;
		}
		return null;
	}
}
