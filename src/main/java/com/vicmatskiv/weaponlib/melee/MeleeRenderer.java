package com.vicmatskiv.weaponlib.melee;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.vicmatskiv.weaponlib.AttachmentContainer;
import com.vicmatskiv.weaponlib.ClientModContext;
import com.vicmatskiv.weaponlib.CompatibleAttachment;
import com.vicmatskiv.weaponlib.CustomRenderer;
import com.vicmatskiv.weaponlib.DefaultPart;
import com.vicmatskiv.weaponlib.ItemAttachment;
import com.vicmatskiv.weaponlib.ItemSkin;
import com.vicmatskiv.weaponlib.ModelWithAttachments;
import com.vicmatskiv.weaponlib.Part;
import com.vicmatskiv.weaponlib.PlayerItemInstance;
import com.vicmatskiv.weaponlib.RenderContext;
import com.vicmatskiv.weaponlib.Tuple;
import com.vicmatskiv.weaponlib.Weapon;
import com.vicmatskiv.weaponlib.animation.MultipartPositioning.Positioner;
import com.vicmatskiv.weaponlib.animation.MultipartRenderStateManager;
import com.vicmatskiv.weaponlib.animation.MultipartTransition;
import com.vicmatskiv.weaponlib.animation.MultipartTransitionProvider;
import com.vicmatskiv.weaponlib.animation.Transition;
import com.vicmatskiv.weaponlib.compatibility.CompatibleMeleeRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class MeleeRenderer extends CompatibleMeleeRenderer {
	
	private static final Logger logger = LogManager.getLogger(MeleeRenderer.class);

	
	private static final float DEFAULT_RANDOMIZING_RATE = 0.33f;
	
	private static final float DEFAULT_NORMAL_RANDOMIZING_AMPLITUDE = 0.06f;
	
	private static final int DEFAULT_ANIMATION_DURATION = 250;


	public static class Builder {
		
		private ModelBase model;
		private String textureName;
		
		private Consumer<ItemStack> entityPositioning;
		private Consumer<ItemStack> inventoryPositioning;
		private Consumer<RenderContext<RenderableState>> thirdPersonPositioning;
		
		private Consumer<RenderContext<RenderableState>> firstPersonPositioning;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningZooming;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningRunning;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningModifying;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningRecoiled;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningShooting;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningZoomingRecoiled;
		private Consumer<RenderContext<RenderableState>> firstPersonPositioningZoomingShooting;
		
		private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioning;
		private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioningZooming;
		private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioningRunning;
		private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioningModifying;
		private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioningRecoiled;
		private Consumer<RenderContext<RenderableState>> firstPersonLeftHandPositioningShooting;
		
		private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioning;
		private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioningZooming;
		private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioningRunning;
		private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioningModifying;
		private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioningRecoiled;
		private Consumer<RenderContext<RenderableState>> firstPersonRightHandPositioningShooting;

		private List<Transition<RenderContext<RenderableState>>> firstPersonPositioningReloading;
		private List<Transition<RenderContext<RenderableState>>> firstPersonLeftHandPositioningReloading;
		private List<Transition<RenderContext<RenderableState>>> firstPersonRightHandPositioningReloading;
		
		private List<Transition<RenderContext<RenderableState>>> firstPersonPositioningUnloading;
		private List<Transition<RenderContext<RenderableState>>> firstPersonLeftHandPositioningUnloading;
		private List<Transition<RenderContext<RenderableState>>> firstPersonRightHandPositioningUnloading;
		
		private long totalReloadingDuration;
		private long totalUnloadingDuration;
		
		private String modId;
		
		private float normalRandomizingRate = DEFAULT_RANDOMIZING_RATE; // movements per second, e.g. 0.25 = 0.25 movements per second = 1 movement in 3 minutes
		
		private float normalRandomizingAmplitude = DEFAULT_NORMAL_RANDOMIZING_AMPLITUDE;
		
		private LinkedHashMap<Part, Consumer<RenderContext<RenderableState>>> firstPersonCustomPositioning = new LinkedHashMap<>();
		private LinkedHashMap<Part, List<Transition<RenderContext<RenderableState>>>> firstPersonCustomPositioningUnloading = new LinkedHashMap<>();
		private LinkedHashMap<Part, List<Transition<RenderContext<RenderableState>>>> firstPersonCustomPositioningReloading = new LinkedHashMap<>();
		private LinkedHashMap<Part, Consumer<RenderContext<RenderableState>>> firstPersonCustomPositioningRecoiled = new LinkedHashMap<>();
		private LinkedHashMap<Part, Consumer<RenderContext<RenderableState>>> firstPersonCustomPositioningZoomingRecoiled = new LinkedHashMap<>();
		private LinkedHashMap<Part, Consumer<RenderContext<RenderableState>>> firstPersonCustomPositioningZoomingShooting = new LinkedHashMap<>();
		
		private LinkedHashMap<Part, List<Transition<RenderContext<RenderableState>>>> firstPersonCustomPositioningEjectSpentRound = new LinkedHashMap<>();
		private boolean hasRecoilPositioningDefined;

		public Builder withModId(String modId) {
			this.modId = modId;
			return this;
		}
		
		public Builder withModel(ModelBase model) {
			this.model = model;
			return this;
		}
		
		public Builder withNormalRandomizingRate(float normalRandomizingRate) {
			this.normalRandomizingRate = normalRandomizingRate;
			return this;
		}
		
		public Builder withTextureName(String textureName) {
			this.textureName = textureName + ".png";
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

		public Builder withThirdPersonPositioning(Consumer<RenderContext<RenderableState>> thirdPersonPositioning) {
			this.thirdPersonPositioning = thirdPersonPositioning;
			return this;
		}

		public Builder withFirstPersonPositioning(Consumer<RenderContext<RenderableState>> firstPersonPositioning) {
			this.firstPersonPositioning = firstPersonPositioning;
			return this;
		}
		
		public Builder withFirstPersonPositioningRunning(Consumer<RenderContext<RenderableState>> firstPersonPositioningRunning) {
			this.firstPersonPositioningRunning = firstPersonPositioningRunning;
			return this;
		}
		
		public Builder withFirstPersonPositioningZooming(Consumer<RenderContext<RenderableState>> firstPersonPositioningZooming) {
			this.firstPersonPositioningZooming = firstPersonPositioningZooming;
			return this;
		}
		
		public Builder withFirstPersonPositioningRecoiled(Consumer<RenderContext<RenderableState>> firstPersonPositioningRecoiled) {
			this.hasRecoilPositioningDefined = true;
			this.firstPersonPositioningRecoiled = firstPersonPositioningRecoiled;
			return this;
		}
		
		public Builder withFirstPersonPositioningShooting(Consumer<RenderContext<RenderableState>> firstPersonPositioningShooting) {
			this.firstPersonPositioningShooting = firstPersonPositioningShooting;
			return this;
		}
		
		public Builder withFirstPersonPositioningZoomingRecoiled(Consumer<RenderContext<RenderableState>> firstPersonPositioningZoomingRecoiled) {
			this.firstPersonPositioningZoomingRecoiled = firstPersonPositioningZoomingRecoiled;
			return this;
		}
		
		public Builder withFirstPersonPositioningZoomingShooting(Consumer<RenderContext<RenderableState>> firstPersonPositioningZoomingShooting) {
			this.firstPersonPositioningZoomingShooting = firstPersonPositioningZoomingShooting;
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonPositioningReloading(Transition<RenderContext<RenderableState>> ...transitions) {
			this.firstPersonPositioningReloading = Arrays.asList(transitions);
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonPositioningUnloading(Transition<RenderContext<RenderableState>> ...transitions) {
			this.firstPersonPositioningUnloading = Arrays.asList(transitions);
			return this;
		}
		
		public Builder withFirstPersonPositioningModifying(Consumer<RenderContext<RenderableState>> firstPersonPositioningModifying) {
			this.firstPersonPositioningModifying = firstPersonPositioningModifying;
			return this;
		}
		
		
		public Builder withFirstPersonHandPositioning(
				Consumer<RenderContext<RenderableState>> leftHand,
				Consumer<RenderContext<RenderableState>> rightHand) 
		{
			this.firstPersonLeftHandPositioning = leftHand;
			this.firstPersonRightHandPositioning = rightHand;
			return this;
		}
		
		public Builder withFirstPersonHandPositioningRunning(
				Consumer<RenderContext<RenderableState>> leftHand,
				Consumer<RenderContext<RenderableState>> rightHand) 
		{
			this.firstPersonLeftHandPositioningRunning = leftHand;
			this.firstPersonRightHandPositioningRunning = rightHand;
			return this;
		}
		
		public Builder withFirstPersonHandPositioningZooming(
				Consumer<RenderContext<RenderableState>> leftHand,
				Consumer<RenderContext<RenderableState>> rightHand)
		{
			this.firstPersonLeftHandPositioningZooming = leftHand;
			this.firstPersonRightHandPositioningZooming = rightHand;
			return this;
		}
		
		public Builder withFirstPersonHandPositioningRecoiled(
				Consumer<RenderContext<RenderableState>> leftHand,
				Consumer<RenderContext<RenderableState>> rightHand)
		{
			this.firstPersonLeftHandPositioningRecoiled = leftHand;
			this.firstPersonRightHandPositioningRecoiled = rightHand;
			return this;
		}
		
		public Builder withFirstPersonHandPositioningShooting(
				Consumer<RenderContext<RenderableState>> leftHand,
				Consumer<RenderContext<RenderableState>> rightHand)
		{
			this.firstPersonLeftHandPositioningShooting = leftHand;
			this.firstPersonRightHandPositioningShooting = rightHand;
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonLeftHandPositioningReloading(Transition<RenderContext<RenderableState>> ...transitions) {
			this.firstPersonLeftHandPositioningReloading = Arrays.asList(transitions);
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonLeftHandPositioningUnloading(Transition<RenderContext<RenderableState>> ...transitions) {
			this.firstPersonLeftHandPositioningUnloading = Arrays.asList(transitions);
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonRightHandPositioningReloading(Transition<RenderContext<RenderableState>> ...transitions) {
			this.firstPersonRightHandPositioningReloading = Arrays.asList(transitions);
			return this;
		}

		@SafeVarargs
		public final Builder withFirstPersonRightHandPositioningUnloading(Transition<RenderContext<RenderableState>> ...transitions) {
			this.firstPersonRightHandPositioningUnloading = Arrays.asList(transitions);
			return this;
		}
		
		public Builder withFirstPersonHandPositioningModifying(
				Consumer<RenderContext<RenderableState>> leftHand,
				Consumer<RenderContext<RenderableState>> rightHand)
		{
			this.firstPersonLeftHandPositioningModifying = leftHand;
			this.firstPersonRightHandPositioningModifying = rightHand;
			return this;
		}
		
		public Builder withFirstPersonCustomPositioning(Part part, Consumer<RenderContext<RenderableState>> positioning) {
			if(part instanceof DefaultPart) {
				throw new IllegalArgumentException("Part " + part + " is not custom");
			}
			if(this.firstPersonCustomPositioning.put(part, positioning) != null) {
				throw new IllegalArgumentException("Part " + part + " already added");
			}
			return this;
		}
		
		public Builder withFirstPersonPositioningCustomZoomingRecoiled(Part part, Consumer<RenderContext<RenderableState>> positioning) {
			if(part instanceof DefaultPart) {
				throw new IllegalArgumentException("Part " + part + " is not custom");
			}
			if(this.firstPersonCustomPositioningZoomingRecoiled.put(part, positioning) != null) {
				throw new IllegalArgumentException("Part " + part + " already added");
			}
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonCustomPositioningReloading(Part part, Transition<RenderContext<RenderableState>> ...transitions) {
			if(part instanceof DefaultPart) {
				throw new IllegalArgumentException("Part " + part + " is not custom");
			}
			
			this.firstPersonCustomPositioningReloading.put(part, Arrays.asList(transitions));
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonCustomPositioningUnloading(Part part, Transition<RenderContext<RenderableState>> ...transitions) {
			if(part instanceof DefaultPart) {
				throw new IllegalArgumentException("Part " + part + " is not custom");
			}
			this.firstPersonCustomPositioningUnloading.put(part, Arrays.asList(transitions));
			return this;
		}
		
		@SafeVarargs
		public final Builder withFirstPersonCustomPositioningEjectSpentRound(Part part, Transition<RenderContext<RenderableState>> ...transitions) {
			if(part instanceof DefaultPart) {
				throw new IllegalArgumentException("Part " + part + " is not custom");
			}
			
			this.firstPersonCustomPositioningEjectSpentRound.put(part, Arrays.asList(transitions));
			return this;
		}
		
		public MeleeRenderer build() {
			if(!compatibility.isClientSide()) {
				return null;
			}

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
			
			MeleeRenderer renderer = new MeleeRenderer(this);
			
			if(firstPersonPositioning == null) {
				firstPersonPositioning = (renderContext) -> {
					GL11.glRotatef(45F, 0f, 1f, 0f);

					if(renderer.getClientModContext() != null) {
						PlayerMeleeInstance instance = renderer.getClientModContext().getMainHeldMeleeWeapon();
					}
				};
			}
			
			if(firstPersonPositioningZooming == null) {
				firstPersonPositioningZooming = firstPersonPositioning;
			}
			
			if(firstPersonPositioningReloading == null) {
				firstPersonPositioningReloading = Collections.singletonList(new Transition<>(firstPersonPositioning, DEFAULT_ANIMATION_DURATION));
			}
			
			for(Transition<RenderContext<RenderableState>> t: firstPersonPositioningReloading) {
				totalReloadingDuration += t.getDuration();
				totalReloadingDuration += t.getPause();
			}
			
			if(firstPersonPositioningUnloading == null) {
				firstPersonPositioningUnloading = Collections.singletonList(new Transition<>(firstPersonPositioning, DEFAULT_ANIMATION_DURATION));
			}
			
			for(Transition<RenderContext<RenderableState>> t: firstPersonPositioningUnloading) {
				totalUnloadingDuration += t.getDuration();
				totalUnloadingDuration += t.getPause();
			}
			
			if(firstPersonPositioningRecoiled == null) {
				firstPersonPositioningRecoiled = firstPersonPositioning;
			}
			
			if(firstPersonPositioningRunning == null) {
				firstPersonPositioningRunning = firstPersonPositioning;
			}
			
			if(firstPersonPositioningModifying == null) {
				firstPersonPositioningModifying = firstPersonPositioning;
			}
			
			if(firstPersonPositioningShooting == null) {
				firstPersonPositioningShooting = firstPersonPositioning;
			}
			
			if(firstPersonPositioningZoomingRecoiled == null) {
				firstPersonPositioningZoomingRecoiled = firstPersonPositioningZooming;
			}
			
			if(firstPersonPositioningZoomingShooting == null) {
				firstPersonPositioningZoomingShooting = firstPersonPositioningZooming;
			}
			
			if(thirdPersonPositioning == null) {
				thirdPersonPositioning = (context) -> {
					GL11.glTranslatef(-0.4F, 0.2F, 0.4F);
					GL11.glRotatef(-45F, 0f, 1f, 0f);
					GL11.glRotatef(70F, 1f, 0f, 0f);
				};
			}
			
			// Left hand positioning
			
			if(firstPersonLeftHandPositioning == null) {
				firstPersonLeftHandPositioning = (context) -> {};
			}
			
			if(firstPersonLeftHandPositioningReloading == null) {
				firstPersonLeftHandPositioningReloading = firstPersonPositioningReloading.stream().map(t -> new Transition<RenderContext<RenderableState>>((p, i) -> {}, 0)).collect(Collectors.toList());
			}
			
			if(firstPersonLeftHandPositioningUnloading == null) {
				firstPersonLeftHandPositioningUnloading = firstPersonPositioningUnloading.stream().map(t -> new Transition<RenderContext<RenderableState>>((p, i) -> {}, 0)).collect(Collectors.toList());
			}
			
			if(firstPersonLeftHandPositioningRecoiled == null) {
				firstPersonLeftHandPositioningRecoiled = firstPersonLeftHandPositioning;
			}
			
			if(firstPersonLeftHandPositioningShooting == null) {
				firstPersonLeftHandPositioningShooting = firstPersonLeftHandPositioning;
			}
			
			if(firstPersonLeftHandPositioningZooming == null) {
				firstPersonLeftHandPositioningZooming = firstPersonLeftHandPositioning;
			}
			
			if(firstPersonLeftHandPositioningRunning == null) {
				firstPersonLeftHandPositioningRunning = firstPersonLeftHandPositioning;
			}
			
			if(firstPersonLeftHandPositioningModifying == null) {
				firstPersonLeftHandPositioningModifying = firstPersonLeftHandPositioning;
			}
			
			// Right hand positioning
			
			if(firstPersonRightHandPositioning == null) {
				firstPersonRightHandPositioning = (context) -> {};
			}
			
			if(firstPersonRightHandPositioningReloading == null) {
				//firstPersonRightHandPositioningReloading = Collections.singletonList(new Transition(firstPersonRightHandPositioning, DEFAULT_ANIMATION_DURATION));
				firstPersonRightHandPositioningReloading = firstPersonPositioningReloading.stream().map(t -> new Transition<RenderContext<RenderableState>>((p, i) -> {}, 0)).collect(Collectors.toList());
			}

			if(firstPersonRightHandPositioningUnloading == null) {
				firstPersonRightHandPositioningUnloading = firstPersonPositioningUnloading.stream().map(t -> new Transition<RenderContext<RenderableState>>((p, i) -> {}, 0)).collect(Collectors.toList());
			}

			if(firstPersonRightHandPositioningRecoiled == null) {
				firstPersonRightHandPositioningRecoiled = firstPersonRightHandPositioning;
			}

			if(firstPersonRightHandPositioningShooting == null) {
				firstPersonRightHandPositioningShooting = firstPersonRightHandPositioning;
			}
			
			if(firstPersonRightHandPositioningZooming == null) {
				firstPersonRightHandPositioningZooming = firstPersonRightHandPositioning;
			}
			
			if(firstPersonRightHandPositioningRunning == null) {
				firstPersonRightHandPositioningRunning = firstPersonRightHandPositioning;
			}
			
			if(firstPersonRightHandPositioningModifying == null) {
				firstPersonRightHandPositioningModifying = firstPersonRightHandPositioning;
			}
			
			/*
			 * If custom positioning for recoil is not set, default it to normal custom positioning
			 */
			if(!firstPersonCustomPositioning.isEmpty() && firstPersonCustomPositioningRecoiled.isEmpty()) {
				firstPersonCustomPositioning.forEach((part, pos) -> {
					firstPersonCustomPositioningRecoiled.put(part, pos);
				});
			}
			
			if(!firstPersonCustomPositioning.isEmpty() && firstPersonCustomPositioningZoomingRecoiled.isEmpty()) {
				firstPersonCustomPositioning.forEach((part, pos) -> {
					firstPersonCustomPositioningZoomingRecoiled.put(part, pos);
				});
			}
			
			if(!firstPersonCustomPositioning.isEmpty() && firstPersonCustomPositioningZoomingShooting.isEmpty()) {
				firstPersonCustomPositioning.forEach((part, pos) -> {
					firstPersonCustomPositioningZoomingShooting.put(part, pos);
				});
			}
						
			firstPersonCustomPositioningReloading.forEach((p, t) -> {
				if(t.size() != firstPersonPositioningReloading.size()) {
					throw new IllegalStateException("Custom reloading transition number mismatch. Expected " + firstPersonPositioningReloading.size()
					+ ", actual: " + t.size());
				}
			});
			
			firstPersonCustomPositioningUnloading.forEach((p, t) -> {
				if(t.size() != firstPersonPositioningUnloading.size()) {
					throw new IllegalStateException("Custom unloading transition number mismatch. Expected " + firstPersonPositioningUnloading.size()
					+ ", actual: " + t.size());
				}
			});
			
			
			return renderer;
		}

		public Consumer<ItemStack> getEntityPositioning() {
			return entityPositioning;
		}

		public Consumer<ItemStack> getInventoryPositioning() {
			return inventoryPositioning;
		}

		public Consumer<RenderContext<RenderableState>> getThirdPersonPositioning() {
			return thirdPersonPositioning;
		}

		public String getTextureName() {
			return textureName;
		}

		public ModelBase getModel() {
			return model;
		}

		public String getModId() {
			return modId;
		}


	}
	
	private Builder builder;
	
	private Map<EntityPlayer, MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>>> firstPersonStateManagers;
		
	private MultipartTransitionProvider<RenderableState, Part, RenderContext<RenderableState>> weaponTransitionProvider;
	
	protected ClientModContext clientModContext;
			
	private MeleeRenderer(Builder builder) {
		super(builder);
		this.builder = builder;
		this.firstPersonStateManagers = new HashMap<>();
		this.weaponTransitionProvider = new WeaponPositionProvider();
	}
	
	protected long getTotalReloadingDuration() {
		return builder.totalReloadingDuration;
	}
	
	protected long getTotalUnloadingDuration() {
		return builder.totalUnloadingDuration;
	}

	protected ClientModContext getClientModContext() {
		return clientModContext;
	}
	
	protected void setClientModContext(ClientModContext clientModContext) {
		this.clientModContext = clientModContext;
	}

	@Override
	protected StateDescriptor getStateDescriptor(EntityPlayer player, ItemStack itemStack) {
		float amplitude = builder.normalRandomizingAmplitude;
		float rate = builder.normalRandomizingRate;
		RenderableState currentState = null;
		
		PlayerItemInstance<?> playerItemInstance = clientModContext.getPlayerItemInstanceRegistry().getItemInstance(player, itemStack);
				//.getMainHandItemInstance(player, PlayerWeaponInstance.class); // TODO: cannot be always main hand, need to which hand from context
		
		PlayerMeleeInstance playerMeleeInstance = null;
		if(playerItemInstance == null || !(playerItemInstance instanceof PlayerMeleeInstance) 
		        || playerItemInstance.getItem() != itemStack.getItem()) {
		    logger.error("Invalid or mismatching item. Player item instance: {}. Item stack: {}", playerItemInstance, itemStack);
		} else {
		    playerMeleeInstance = (PlayerMeleeInstance) playerItemInstance;
		}
		
		if(playerMeleeInstance != null) {
			AsyncMeleeState asyncWeaponState = getNextNonExpiredState(playerMeleeInstance);
			
			switch(asyncWeaponState.getState()) {
				
			case FIRING:
			    currentState = RenderableState.ATTACKING;
			    break;
				
			case MODIFYING: case MODIFYING_REQUESTED: case NEXT_ATTACHMENT: case NEXT_ATTACHMENT_REQUESTED:
				currentState = RenderableState.MODIFYING;
				break;	
				
			default:
				if(player.isSprinting() && builder.firstPersonPositioningRunning != null) {
					currentState = RenderableState.RUNNING;
				}
			}

			logger.trace("Rendering state {} created from {}", currentState, asyncWeaponState.getState());
		}
		
		if(currentState == null) {
			currentState = RenderableState.NORMAL;
		}
		
		MultipartRenderStateManager<RenderableState, Part, RenderContext<RenderableState>> stateManager = firstPersonStateManagers.get(player);
		if(stateManager == null) {
			stateManager = new MultipartRenderStateManager<>(currentState, weaponTransitionProvider, Part.WEAPON);
			firstPersonStateManagers.put(player, stateManager);
		} else {
			stateManager.setState(currentState, true, currentState == RenderableState.ATTACKING);
		}
		
		
		return new StateDescriptor(playerMeleeInstance, stateManager, rate, amplitude);
	}

	private AsyncMeleeState getNextNonExpiredState(PlayerMeleeInstance playerWeaponState) {
	    AsyncMeleeState asyncWeaponState = null;
		while((asyncWeaponState = playerWeaponState.nextHistoryState()) != null) {
			if(System.currentTimeMillis() < asyncWeaponState.getTimestamp() + asyncWeaponState.getDuration()) {
			    continue;
			}
		}	
		
		return asyncWeaponState;
	}
	
	private BiConsumer<Part, RenderContext<RenderableState>> createWeaponPartPositionFunction(Transition<RenderContext<RenderableState>> t) {
		if(t == null) {
			return (part, context) -> {};
		}
		Consumer<RenderContext<RenderableState>> weaponPositionFunction = t.getItemPositioning();
		if(weaponPositionFunction != null) {
			return (part, context) -> weaponPositionFunction.accept(context);
		} 
		
		return (part, context) -> {};
		
	}
	
	private BiConsumer<Part, RenderContext<RenderableState>> createWeaponPartPositionFunction(Consumer<RenderContext<RenderableState>> weaponPositionFunction) {
		if(weaponPositionFunction != null) {
			return (part, context) -> weaponPositionFunction.accept(context);
		} 
		return (part, context) -> {};
		
	}
	
	private List<MultipartTransition<Part, RenderContext<RenderableState>>> getComplexTransition(
			List<Transition<RenderContext<RenderableState>>> wt, 
			List<Transition<RenderContext<RenderableState>>> lht, 
			List<Transition<RenderContext<RenderableState>>> rht, 
			LinkedHashMap<Part, List<Transition<RenderContext<RenderableState>>>> custom) 
	{
		List<MultipartTransition<Part, RenderContext<RenderableState>>> result = new ArrayList<>();
		for(int i = 0; i < wt.size(); i++) {
			Transition<RenderContext<RenderableState>> p = wt.get(i);
			Transition<RenderContext<RenderableState>> l = lht.get(i);
			Transition<RenderContext<RenderableState>> r = rht.get(i);
			
			MultipartTransition<Part, RenderContext<RenderableState>> t = new MultipartTransition<Part, RenderContext<RenderableState>>(p.getDuration(), p.getPause())
					.withPartPositionFunction(Part.WEAPON, createWeaponPartPositionFunction(p))
					.withPartPositionFunction(Part.LEFT_HAND, createWeaponPartPositionFunction(l))
					.withPartPositionFunction(Part.RIGHT_HAND, createWeaponPartPositionFunction(r));
			
			for(Entry<Part, List<Transition<RenderContext<RenderableState>>>> e: custom.entrySet()){
				List<Transition<RenderContext<RenderableState>>> partTransitions = e.getValue();
				Transition<RenderContext<RenderableState>> partTransition = null;
				if(partTransitions != null && partTransitions.size() > i) {
					partTransition = partTransitions.get(i);
				} else {
					logger.warn("Transition not defined for part {}", custom);
				}
				t.withPartPositionFunction(e.getKey(), createWeaponPartPositionFunction(partTransition));
			}
	
			result.add(t);
		}
		return result;
	}
	
	private List<MultipartTransition<Part, RenderContext<RenderableState>>> getSimpleTransition(
			Consumer<RenderContext<RenderableState>> w,
			Consumer<RenderContext<RenderableState>> lh,
			Consumer<RenderContext<RenderableState>> rh,
			//Consumer<RenderContext<RenderableState>> m,
			LinkedHashMap<Part, Consumer<RenderContext<RenderableState>>> custom,
			int duration) {
		MultipartTransition<Part, RenderContext<RenderableState>> mt = new MultipartTransition<Part, RenderContext<RenderableState>>(duration, 0)
				.withPartPositionFunction(Part.WEAPON, createWeaponPartPositionFunction(w))
				.withPartPositionFunction(Part.LEFT_HAND, createWeaponPartPositionFunction(lh))
				.withPartPositionFunction(Part.RIGHT_HAND, createWeaponPartPositionFunction(rh));
		custom.forEach((part, position) -> {
			mt.withPartPositionFunction(part, createWeaponPartPositionFunction(position));
		});
		return Collections.singletonList(mt);
	}
	
	private class WeaponPositionProvider implements MultipartTransitionProvider<RenderableState, Part, RenderContext<RenderableState>> {

		@Override
		public List<MultipartTransition<Part, RenderContext<RenderableState>>> getPositioning(RenderableState state) {
			switch(state) {
			case MODIFYING:
				return getSimpleTransition(builder.firstPersonPositioningModifying,
						builder.firstPersonLeftHandPositioningModifying,
						builder.firstPersonRightHandPositioningModifying,
						builder.firstPersonCustomPositioning,
						DEFAULT_ANIMATION_DURATION);
			case RUNNING:
				return getSimpleTransition(builder.firstPersonPositioningRunning,
						builder.firstPersonLeftHandPositioningRunning,
						builder.firstPersonRightHandPositioningRunning,
						builder.firstPersonCustomPositioning,
						DEFAULT_ANIMATION_DURATION);
			case ATTACKING:
				return getComplexTransition(builder.firstPersonPositioningUnloading, 
						builder.firstPersonLeftHandPositioningUnloading,
						builder.firstPersonRightHandPositioningUnloading,
						builder.firstPersonCustomPositioningUnloading
						);
			case NORMAL:
				return getSimpleTransition(builder.firstPersonPositioning, 
						builder.firstPersonLeftHandPositioning,
						builder.firstPersonRightHandPositioning,
						builder.firstPersonCustomPositioning,
						DEFAULT_ANIMATION_DURATION);
			default:
				break;
			}
			return null;
		}
	}
	
	@Override
	public void renderItem(ItemStack weaponItemStack, RenderContext<RenderableState> renderContext,
			Positioner<Part, RenderContext<RenderableState>> positioner) {
		List<CompatibleAttachment<? extends AttachmentContainer>> attachments = null;
		if(builder.getModel() instanceof ModelWithAttachments) {
			attachments = ((ItemMelee) weaponItemStack.getItem()).getActiveAttachments(renderContext.getPlayer(), weaponItemStack);
		}
		
		if(builder.getTextureName() != null) {
			Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(builder.getModId() 
					+ ":textures/models/" + builder.getTextureName()));
		} else {
			String textureName = null;
			CompatibleAttachment<?> compatibleSkin = attachments.stream()
					.filter(ca -> ca.getAttachment() instanceof ItemSkin).findAny().orElse(null);
			if(compatibleSkin != null) {
				PlayerItemInstance<?> itemInstance = getClientModContext().getPlayerItemInstanceRegistry()
						.getItemInstance(renderContext.getPlayer(), weaponItemStack);
				if(itemInstance instanceof PlayerMeleeInstance) {
					int textureIndex = ((PlayerMeleeInstance) itemInstance).getActiveTextureIndex();
					if(textureIndex >= 0) {
						textureName = ((ItemSkin) compatibleSkin.getAttachment()).getTextureVariant(textureIndex)
								+ ".png";
					}
				}
			} 
			
			if(textureName == null) {
				Weapon weapon = ((Weapon) weaponItemStack.getItem());
				textureName = weapon.getTextureName();
			}
			
			Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(builder.getModId() 
					+ ":textures/models/" + textureName));
		}
		
		//limbSwing, float flimbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale
		builder.getModel().render(null,  
				renderContext.getLimbSwing(), 
				renderContext.getFlimbSwingAmount(), 
				renderContext.getAgeInTicks(), 
				renderContext.getNetHeadYaw(), 
				renderContext.getHeadPitch(), 
				renderContext.getScale());
		
		if(attachments != null) {
			renderAttachments(positioner, renderContext, attachments);
		}
	}
	
	public void renderAttachments(Positioner<Part, RenderContext<RenderableState>> positioner, RenderContext<RenderableState> renderContext,List<CompatibleAttachment<? extends AttachmentContainer>> attachments) {
		for(CompatibleAttachment<?> compatibleAttachment: attachments) {
			if(compatibleAttachment != null && !(compatibleAttachment.getAttachment() instanceof ItemSkin)) {
				renderCompatibleAttachment(compatibleAttachment, positioner, renderContext);
			}
		}
	}

	private void renderCompatibleAttachment(CompatibleAttachment<?> compatibleAttachment,
			Positioner<Part, RenderContext<RenderableState>> positioner, RenderContext<RenderableState> renderContext) {
		
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
		
		if(compatibleAttachment.getPositioning() != null) {
			compatibleAttachment.getPositioning().accept(renderContext.getPlayer(), renderContext.getWeapon());
		}
		
		ItemAttachment<?> itemAttachment = compatibleAttachment.getAttachment();
		
		
		if(positioner != null) {
			if(itemAttachment instanceof Part) {
				positioner.position((Part) itemAttachment, renderContext);
			} else if(itemAttachment.getRenderablePart() != null) {
				positioner.position(itemAttachment.getRenderablePart(), renderContext);
			}
		}

		for(Tuple<ModelBase, String> texturedModel: compatibleAttachment.getAttachment().getTexturedModels()) {
			Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(builder.getModId() 
					+ ":textures/models/" + texturedModel.getV()));
			GL11.glPushMatrix();
			GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
			if(compatibleAttachment.getModelPositioning() != null) {
				compatibleAttachment.getModelPositioning().accept(texturedModel.getU());
			}
			texturedModel.getU().render(renderContext.getPlayer(), 
					renderContext.getLimbSwing(), 
					renderContext.getFlimbSwingAmount(), 
					renderContext.getAgeInTicks(), 
					renderContext.getNetHeadYaw(), 
					renderContext.getHeadPitch(), 
					renderContext.getScale());

			GL11.glPopAttrib();
			GL11.glPopMatrix();
		}
		
		@SuppressWarnings("unchecked")
        CustomRenderer<RenderableState> postRenderer = (CustomRenderer<RenderableState>) compatibleAttachment.getAttachment().getPostRenderer();
		if(postRenderer != null) {
			GL11.glPushMatrix();
			GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
			postRenderer.render(renderContext);
			GL11.glPopAttrib();
			GL11.glPopMatrix();
		}
		
		for(CompatibleAttachment<?> childAttachment: itemAttachment.getAttachments()) {
			renderCompatibleAttachment(childAttachment, positioner, renderContext);
		}
		
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	public boolean hasRecoilPositioning() {
		return builder.hasRecoilPositioningDefined;
	}
}
