package com.vicmatskiv.weaponlib;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.vicmatskiv.weaponlib.animation.ClientValueRepo;
import com.vicmatskiv.weaponlib.compatibility.CompatibleClientEventHandler;
import com.vicmatskiv.weaponlib.compatibility.FlatSurfaceModelBox;
import com.vicmatskiv.weaponlib.config.novel.ModernConfigManager;
import com.vicmatskiv.weaponlib.perspective.OpticalScopePerspective;
import com.vicmatskiv.weaponlib.render.scopes.Reticle;
import com.vicmatskiv.weaponlib.shader.jim.Shader;
import com.vicmatskiv.weaponlib.shader.jim.ShaderManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class ViewfinderModel extends ModelBase {
	
	private static final ResourceLocation SCOPE_GRIME_TEXTURE = new ResourceLocation("mw" + ":" + "textures/hud/scopedirt.png");

	private ModelRenderer surfaceRenderer;
	private FlatSurfaceModelBox box;

	public ViewfinderModel() {
		textureWidth = 128;
		textureHeight = 64;

		surfaceRenderer = new ModelRenderer(this, 0, 0);
		box = new FlatSurfaceModelBox(surfaceRenderer, 0, 0, 0f, 0f, 0f, 3, 3, 0, 0.0F);
		surfaceRenderer.cubeList.add(box);
		surfaceRenderer.mirror = true;
		surfaceRenderer.setRotationPoint(0F, -10F, 0F);
		surfaceRenderer.setTextureSize(100, 100);
		
		setRotation(surfaceRenderer, 0F, 0F, 0F);
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch, float scale) {
	
		super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
		setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
		surfaceRenderer.render(scale);
	}
	
	public void render(Reticle ret, RenderContext<RenderableState> renderContext, Entity entity, float f5) {
		

	
		
		if(ModernConfigManager.enableAllShaders && ModernConfigManager.enableScopeEffects) {
			renderWithScopeFX(ret, renderContext, entity, f5);
		} else {
			renderDry(ret, entity, f5);
		}
		
		
		
	}
	
	public void renderDry(Reticle ret, Entity entity, float f5) {
		GlStateManager.disableBlend();
		GlStateManager.disableAlpha();
		surfaceRenderer.render(f5);
		
		GlStateManager.enableAlpha();
		GlStateManager.enableBlend();
		Minecraft.getMinecraft().getTextureManager().bindTexture(ret.getReticleTexture());
		GlStateManager.pushMatrix();
		
		GlStateManager.pushMatrix();
		double yOff = 0.68;
		double xOff = -0.119;
		GlStateManager.translate(-xOff, -yOff, 0);
		GlStateManager.rotate(180f, 0, 0, 1);
		GlStateManager.translate(xOff, yOff, 0.001);
		
		surfaceRenderer.render(f5);
		
		GlStateManager.popMatrix();
		GlStateManager.popMatrix();
		
		
	}
	
	public void renderWithScopeFX(Reticle ret, RenderContext<RenderableState> renderContext, Entity entity, float f5) {
		GlStateManager.enableBlend();
		GlStateManager.enableAlpha();
		
		
	
		boolean isNightVisionOn = false;
		boolean useWhitePhosphor = false;
		if(renderContext.getWeaponInstance() != null) {
			ItemAttachment<Weapon> scope = renderContext.getWeaponInstance().getAttachmentItemWithCategory(AttachmentCategory.SCOPE);
			if(scope != null) { 
				if(scope instanceof ItemScope) {
					isNightVisionOn = ((ItemScope) scope).hasNightVision() && renderContext.getWeaponInstance().isNightVisionOn();
					useWhitePhosphor = ((ItemScope) scope).usesWhitePhosphor();
				}
			}
		}
		
		
	
		OpticalScopePerspective.scope = ShaderManager.loadVMWShader("vignette");
		
		Shader scopeShader = OpticalScopePerspective.scope;
	
		scopeShader.use();
		
		GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + 4);
		Minecraft.getMinecraft().getTextureManager().bindTexture(ret.getReticleTexture());
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    	GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    	GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
    	
    	
    	GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + 6);
		Minecraft.getMinecraft().getTextureManager().bindTexture(SCOPE_GRIME_TEXTURE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    	GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    	GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
    	
    	/* Code for a future hybrid scope
    	GlStateManager.setActiveTexture(GL13.GL_TEXTURE0+7);
		ResourceLocation loc = new ResourceLocation("mw" + ":" + "textures/crosshairs/reflexret.png");
		Minecraft.getMinecraft().getTextureManager().bindTexture(loc);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
		*/
    	

    	if(ClientModContext.getContext() != null && ClientModContext.getContext().getMainHeldWeapon() != null) {
    		float pwi = ClientModContext.getContext().getMainHeldWeapon().getZoom();
    		if(ClientModContext.getContext().getMainHeldWeapon().state != WeaponState.READY && ClientModContext.getContext().getMainHeldWeapon().state != WeaponState.PAUSED && ClientModContext.getContext().getMainHeldWeapon().state != WeaponState.EJECT_REQUIRED && ClientModContext.getContext().getMainHeldWeapon().state != WeaponState.ALERT) {
        		ClientValueRepo.scopeY.currentValue = 1;
        	}
    		scopeShader.uniform1f("reticleZoom", (pwi+0.86f));
    		scopeShader.uniform1f("actualZoom", (1.0f - pwi) - 0.80f);
    	}
    	
 
    	scopeShader.uniform1i("reticle", 4);
    	scopeShader.uniform1i("dirt", 6);
    	scopeShader.uniform1i("holo", 7);
    	//scopeShader.uniform2f("Velocity", (float) ClientValueRepo.scopeX.getLerpedFloat(), (float) ClientValueRepo.scopeY.getLerpedFloat());
    	scopeShader.uniform2f("resolution", Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
    	scopeShader.boolean1b("isNightVisionOn", isNightVisionOn);
    	scopeShader.boolean1b("useWhitePhosphor", useWhitePhosphor);
    	scopeShader.uniform1f("time", ClientValueRepo.ticker.getLerpedFloat());
 
    	
    	
    	
    	GlStateManager.enableBlend();
		surfaceRenderer.render(f5);
		scopeShader.release();
	}

	private void setRotation(ModelRenderer model, float x, float y, float z) {
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}

}
