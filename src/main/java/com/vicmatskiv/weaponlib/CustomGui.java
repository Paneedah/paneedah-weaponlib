package com.vicmatskiv.weaponlib;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomGui extends Gui {
	private Minecraft mc;
	private AttachmentManager attachmentManager;

	public CustomGui(Minecraft mc, AttachmentManager attachmentManager) {
		this.mc = mc;
		this.attachmentManager = attachmentManager;
	}
	private static final int BUFF_ICON_SIZE = 256;
	
	
	//TODO: fix this method @SubscribeEvent
	@SubscribeEvent
	public void onRenderHud(RenderGameOverlayEvent.Pre event) {
		
		if(event.type == RenderGameOverlayEvent.ElementType.HELMET) {
			ItemStack helmet = mc.thePlayer.getEquipmentInSlot(4);
			if(helmet != null && mc.gameSettings.thirdPersonView == 0 && helmet.getItem() instanceof CustomArmor) {
				// Texture must be Width: 427, height: 240
				String hudTexture = ((CustomArmor)helmet.getItem()).getHudTexture();
				if(hudTexture != null) {
					ScaledResolution scaledResolution = event.resolution;
					int width = scaledResolution.getScaledWidth();
				    int height = scaledResolution.getScaledHeight();
				    
					GlStateManager.pushAttrib();
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					GlStateManager.disableLighting();
					GlStateManager.enableAlpha();
					GlStateManager.enableBlend();
					
					
					this.mc.renderEngine.bindTexture(new ResourceLocation(hudTexture));
					
					drawTexturedQuadFit(0, 0, width, height, 0);
					
					GlStateManager.popAttrib();
					
					event.setCanceled(true);
				}
			}
		}
	}

	
	@SubscribeEvent
	public void onRenderCrosshair(RenderGameOverlayEvent.Pre event) {
		
		if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS ) {
			return;
		}
		
		ItemStack itemStack = mc.thePlayer.getHeldItem();
		if(itemStack == null) {
			return;
		}
		
		if(itemStack.getItem() instanceof Weapon) {
			Weapon weaponItem = (Weapon) itemStack.getItem();
			String crosshair = weaponItem != null ? weaponItem.getCrosshair(itemStack, mc.thePlayer) : null;
			if(crosshair != null) {
				ScaledResolution scaledResolution = event.resolution;
				int width = scaledResolution.getScaledWidth();
			    int height = scaledResolution.getScaledHeight();
			    
			    int xPos = width / 2 - BUFF_ICON_SIZE / 2;
				int yPos = height / 2 - BUFF_ICON_SIZE / 2;
				
			    FontRenderer fontRender = mc.fontRendererObj;

				mc.entityRenderer.setupOverlayRendering();
				
				int color = 0xFFFFFF;
				
				this.mc.renderEngine.bindTexture(new ResourceLocation(crosshair));

				GlStateManager.pushAttrib();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				GlStateManager.disableLighting();
				GlStateManager.enableAlpha();
				GlStateManager.enableBlend();
				
				if(weaponItem.isCrosshairFullScreen(itemStack))	 {
					drawTexturedQuadFit(0, 0, width, height, 0);
				} else {
					drawTexturedModalRect(xPos, yPos, 0, 0, BUFF_ICON_SIZE, BUFF_ICON_SIZE);
				}
				
				if(Weapon.isModifying(itemStack) /*weaponItem.getState(weapon) == Weapon.STATE_MODIFYING*/) {
					fontRender.drawStringWithShadow("Attachment selection mode. Press [f] to exit.", 10, 10, color);
					fontRender.drawStringWithShadow("Press [up] to add optic", width / 2 - 40, 60, color);
					fontRender.drawStringWithShadow("Press [left] to add barrel rig", 10, height / 2 - 10, color);
					fontRender.drawStringWithShadow("Press [right] to change camo", width / 2 + 60, height / 2 - 20, color);
					fontRender.drawStringWithShadow("Press [down] to add under-barrel rig", 10, height - 40, color);
				} else {
					ItemMagazine magazine = (ItemMagazine) attachmentManager.getActiveAttachment(itemStack, AttachmentCategory.MAGAZINE);
					int totalCapacity;
					if(magazine != null) {
						totalCapacity = magazine.getAmmo();
					} else {
						totalCapacity = weaponItem.getAmmoCapacity();
					}
					
					String text;
					if(weaponItem.getAmmoCapacity() == 0 && totalCapacity == 0) {
						text = "No magazine";
					} else {
						text = "Ammo: " + weaponItem.getCurrentAmmo(mc.thePlayer) + "/" + totalCapacity;
					}
					
					int x = width - 80;
					int y = 10;

					fontRender.drawStringWithShadow(text, x, y, color);
				}
				GlStateManager.popAttrib();
				
				event.setCanceled(true);
			}
		} else if(itemStack.getItem() instanceof ItemMagazine) {
			ScaledResolution scaledResolution = event.resolution;
			int width = scaledResolution.getScaledWidth();
			FontRenderer fontRender = mc.fontRendererObj;
			mc.entityRenderer.setupOverlayRendering();
			int color = 0xFFFFFF;
			
			ItemMagazine magazine = (ItemMagazine) itemStack.getItem();
			
			String text = "Ammo: " + Tags.getAmmo(itemStack) + "/" + magazine.getAmmo();
			int x = width - 80;
			int y = 10;

			fontRender.drawStringWithShadow(text, x, y, color);
			event.setCanceled(true);
		}
	}
	
	private static void drawTexturedQuadFit(double x, double y, double width, double height, double zLevel){
		//throw new UnsupportedOperationException("Refactor the commented code below!");
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldRenderer = tessellator.getWorldRenderer();
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		worldRenderer.pos(x + 0, y + height, zLevel).tex(0,1).endVertex();
		worldRenderer.pos(x + width, y + height, zLevel).tex(1, 1).endVertex();
		worldRenderer.pos(x + width, y + 0, zLevel).tex(1,0).endVertex();
		worldRenderer.pos(x + 0, y + 0, zLevel).tex(0, 0).endVertex();
		tessellator.draw();
	}
}
