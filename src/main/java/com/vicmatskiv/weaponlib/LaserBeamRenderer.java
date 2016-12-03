package com.vicmatskiv.weaponlib;

import java.util.Random;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@SuppressWarnings("deprecation")
public class LaserBeamRenderer implements CustomRenderer {
	
	private float leftOffset = 0.3f;
	private float forwardOffset = 0.1f;
	
	public LaserBeamRenderer() {
		
	}

	@Override
	public void render(TransformType type, ItemStack itemStack) {
		
		Item item = itemStack.getItem();
		if(!(item instanceof Weapon)) {
			throw new IllegalStateException("Item is not weapon");
		}

		if(Tags.isLaserOn(itemStack) && (
				type == TransformType.GROUND || type == TransformType.FIRST_PERSON /*|| type == ItemRenderType.ENTITY ?*/)) {
			GL11.glPushMatrix();
			GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_TEXTURE_2D);

			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glColor4f(1f, 0f, 0f, 0.5f); 
			GL11.glLineWidth(1.5F);
			GL11.glDepthMask(false);

			GL11.glRotatef(-0.1f, 0f, 1f, 0f);

			Tessellator tessellator = Tessellator.getInstance();
			WorldRenderer renderer = tessellator.getWorldRenderer();
			renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_TEX_NORMAL);

			long time = System.currentTimeMillis();
			Random random = new Random(time - time % 300);
			float start = forwardOffset;
			float length = 100;

			float end = 0;
			for(int i = 0; i < 100 && start < length && end < length; i++) {
				//tessellator.addVertex(leftOffset, 0, start);
				renderer.pos(leftOffset, 0, start);
				
				//tessellator.setBrightness(15728880);
				
		        int ii = 15728880; //this.getBrightnessForRender(partialTicks); // or simply set it to 200?
		        int j = ii >> 16 & 65535;
		        int k = ii & 65535;
		        renderer.lightmap(j, k);
		        
				end = start - ( 1 + random.nextFloat() * 2);
				if(end > length) end = length;
				//tessellator.addVertex(leftOffset, 0, end);
				renderer.pos(leftOffset, 0, end);
				renderer.endVertex();
				start = end + random.nextFloat() * 0.5f;
			}

			tessellator.draw();
			
			GL11.glDepthMask(true);
			
			GL11.glPopAttrib();

			GL11.glPopMatrix();
		}
	}
}
