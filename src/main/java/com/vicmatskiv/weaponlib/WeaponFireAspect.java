package com.vicmatskiv.weaponlib;

import static com.vicmatskiv.weaponlib.compatibility.CompatibilityProvider.compatibility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vicmatskiv.weaponlib.animation.ClientValueRepo;
import com.vicmatskiv.weaponlib.command.DebugCommand;
import com.vicmatskiv.weaponlib.compatibility.CompatibleClientEventHandler;
import com.vicmatskiv.weaponlib.compatibility.CompatibleSound;
import com.vicmatskiv.weaponlib.network.packets.BulletShellClient;
import com.vicmatskiv.weaponlib.network.packets.GunFXPacket;
import com.vicmatskiv.weaponlib.render.shells.ShellParticleSimulator;
import com.vicmatskiv.weaponlib.render.shells.ShellParticleSimulator.Shell;
import com.vicmatskiv.weaponlib.render.shells.ShellParticleSimulator.Shell.Type;
import com.vicmatskiv.weaponlib.sound.JSoundEngine;
import com.vicmatskiv.weaponlib.state.Aspect;
import com.vicmatskiv.weaponlib.state.PermitManager;
import com.vicmatskiv.weaponlib.state.StateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;


/*
 * On a client side this class is used from within a separate client "ticker" thread
 */
public class WeaponFireAspect implements Aspect<WeaponState, PlayerWeaponInstance> {
    
    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(WeaponFireAspect.class);

    private static final float FLASH_X_OFFSET_ZOOMED = -0.03f;

    private static final long ALERT_TIMEOUT = 500;
    
//    private static <T> Predicate<T> logging(Predicate<T> predicate, String message) {
//        return t -> {
//            boolean result = predicate.test(t);
//            logger.debug(message, result);
//            return result;
//        };
//    }

    private static Predicate<PlayerWeaponInstance> readyToShootAccordingToFireRate = instance ->
        System.currentTimeMillis() - instance.getLastFireTimestamp() >= 50f / instance.getWeapon().builder.fireRate;
        
    private static Predicate<PlayerWeaponInstance> postBurstTimeoutExpired = instance ->
        System.currentTimeMillis() - instance.getLastBurstEndTimestamp()
            >= instance.getWeapon().builder.burstTimeoutMilliseconds;

    private static Predicate<PlayerWeaponInstance> readyToShootAccordingToFireMode =
            instance -> instance.getSeriesShotCount() < instance.getMaxShots();
            
    private static Predicate<PlayerWeaponInstance> oneClickBurstEnabled = PlayerWeaponInstance::isOneClickBurstAllowed;
    
    private static Predicate<PlayerWeaponInstance> seriesResetAllowed = PlayerWeaponInstance::isSeriesResetAllowed;

    private static Predicate<PlayerWeaponInstance> hasAmmo = instance -> instance.getAmmo() > 0
            && Tags.getAmmo(instance.getItemStack()) > 0;

    private static Predicate<PlayerWeaponInstance> ejectSpentRoundRequired = instance -> instance.getWeapon().ejectSpentRoundRequired();

    private static Predicate<PlayerWeaponInstance> ejectSpentRoundTimeoutExpired = instance ->
        System.currentTimeMillis() >= instance.getWeapon().builder.pumpTimeoutMilliseconds + instance.getStateUpdateTimestamp();

    private static Predicate<PlayerWeaponInstance> alertTimeoutExpired = instance ->
        System.currentTimeMillis() >= ALERT_TIMEOUT + instance.getStateUpdateTimestamp();

    private static Predicate<PlayerWeaponInstance> sprinting = instance -> instance.getPlayer().isSprinting();

    private static final Set<WeaponState> allowedFireOrEjectFromStates = new HashSet<>(
            Arrays.asList(WeaponState.READY, WeaponState.PAUSED, WeaponState.EJECT_REQUIRED));

    private static final Set<WeaponState> allowedUpdateFromStates = new HashSet<>(
            Arrays.asList(WeaponState.EJECTING, WeaponState.PAUSED, WeaponState.FIRING,
                    WeaponState.RECOILED, WeaponState.PAUSED, WeaponState.ALERT));

    private ModContext modContext;

    private StateManager<WeaponState, ? super PlayerWeaponInstance> stateManager;

    public WeaponFireAspect(CommonModContext modContext) {
        this.modContext = modContext;
    }

    @Override
    public void setPermitManager(PermitManager permitManager) {}

    @Override
    public void setStateManager(StateManager<WeaponState, ? super PlayerWeaponInstance> stateManager) {
        this.stateManager = stateManager;

        stateManager

        .in(this).change(WeaponState.READY).to(WeaponState.ALERT)
        .when(hasAmmo.negate())
        .withAction(this::cannotFire)
        .manual() // on start fire

        .in(this).change(WeaponState.ALERT).to(WeaponState.READY)
        .when(alertTimeoutExpired)
        .automatic() //

        .in(this).change(WeaponState.READY).to(WeaponState.FIRING)
        .when(hasAmmo.and(sprinting.negate()).and(readyToShootAccordingToFireRate))
        .withAction(this::fire)
        .manual() // on start fire

        .in(this).change(WeaponState.FIRING).to(WeaponState.RECOILED)
        .automatic() // unconditional

        .in(this).change(WeaponState.RECOILED).to(WeaponState.PAUSED)
        .automatic() // unconditional

        .in(this).change(WeaponState.PAUSED).to(WeaponState.EJECT_REQUIRED)
        .when(ejectSpentRoundRequired)
        .manual() // on stop

        .in(this).change(WeaponState.EJECT_REQUIRED).to(WeaponState.EJECTING)
        .withAction(this::ejectSpentRound)
        .manual() // on fire ?

        .in(this).change(WeaponState.EJECTING).to(WeaponState.READY)
        .when(ejectSpentRoundTimeoutExpired) // TODO: enforce it only if a trigger was released
        .automatic() // on stop fire and eject animation completed

        .in(this).change(WeaponState.PAUSED).to(WeaponState.FIRING)
        .when(hasAmmo
                .and(sprinting.negate())
                .and(readyToShootAccordingToFireMode)
                .and(readyToShootAccordingToFireRate)
                )
        .withAction(this::fire)
        .manual() // on fire, requires fire button to be down
        
        /// Applies 
        .in(this).change(WeaponState.PAUSED).to(WeaponState.FIRING)
        .when(hasAmmo.and(sprinting.negate())
                .and(oneClickBurstEnabled)
                .and(readyToShootAccordingToFireMode)
                .and((readyToShootAccordingToFireRate))
                )
        .withAction(this::fire)
        .automatic() // on update
        
        .in(this).change(WeaponState.PAUSED).to(WeaponState.READY)
        .when(ejectSpentRoundRequired.negate()
                .and(oneClickBurstEnabled)
                .and(readyToShootAccordingToFireMode.negate().or(hasAmmo.negate()))
                .and(seriesResetAllowed)
                .and(postBurstTimeoutExpired)
                )
        .withAction(PlayerWeaponInstance::resetCurrentSeries)
        .automatic() // on update
        
        .in(this).change(WeaponState.PAUSED).to(WeaponState.READY)
        .when(ejectSpentRoundRequired.negate().and(oneClickBurstEnabled.negate()))
        .withAction(PlayerWeaponInstance::resetCurrentSeries)
        .manual() // on stop

        ;
    }

    void onFireButtonDown(EntityPlayer player) {
        PlayerWeaponInstance weaponInstance = modContext.getPlayerItemInstanceRegistry().getMainHandItemInstance(player, PlayerWeaponInstance.class);
        if(weaponInstance != null) {
            stateManager.changeStateFromAnyOf(this, weaponInstance, allowedFireOrEjectFromStates, WeaponState.FIRING, WeaponState.EJECTING, WeaponState.ALERT);
        }
    }

    void onFireButtonRelease(EntityPlayer player) {
        PlayerWeaponInstance weaponInstance = modContext.getPlayerItemInstanceRegistry().getMainHandItemInstance(player, PlayerWeaponInstance.class);
        if(weaponInstance != null) {
            weaponInstance.setSeriesResetAllowed(true);
            stateManager.changeState(this, weaponInstance, WeaponState.EJECT_REQUIRED, WeaponState.READY);
        }
    }

    void onUpdate(EntityPlayer player) {
        PlayerWeaponInstance weaponInstance = modContext.getPlayerItemInstanceRegistry().getMainHandItemInstance(player, PlayerWeaponInstance.class);
        if(weaponInstance != null) {
            stateManager.changeStateFromAnyOf(this, weaponInstance, allowedUpdateFromStates); // triggers "auto" state transitions
        }
    }

    private void cannotFire(PlayerWeaponInstance weaponInstance) {
        if(weaponInstance.getAmmo() == 0 || Tags.getAmmo(weaponInstance.getItemStack()) == 0) {
            String message;
            if(weaponInstance.getWeapon().getAmmoCapacity() == 0
                    && modContext.getAttachmentAspect().getActiveAttachment(weaponInstance, AttachmentCategory.MAGAZINE) == null) {
                message = compatibility.getLocalizedString("gui.noMagazine");
            } else {
                message = compatibility.getLocalizedString("gui.noAmmo");
            }
            modContext.getStatusMessageCenter().addAlertMessage(message, 3, 250, 200);
            if(weaponInstance.getPlayer() instanceof EntityPlayer) {
                compatibility.playSound((EntityPlayer)weaponInstance.getPlayer(), modContext.getNoAmmoSound(), 1F, 1F);
            }
        }
    }

    private void fire(PlayerWeaponInstance weaponInstance) {
    	
    
        EntityLivingBase player = weaponInstance.getPlayer();
        Weapon weapon = (Weapon) weaponInstance.getItem();
        Random random = player.getRNG();

        
        //if(true) return;
        modContext.getChannel().getChannel().sendToServer(new TryFireMessage(true, 
                oneClickBurstEnabled.test(weaponInstance) && weaponInstance.getSeriesShotCount() ==  0));

        
    	
        boolean silencerOn = modContext.getAttachmentAspect().isSilencerOn(weaponInstance);
        
        
        
        CompatibleSound shootSound = null;
        /*
         * If oneClickBurstEnabled and it's a first shot and burst sound is defined, then play a burst sound
         */
        if(oneClickBurstEnabled.test(weaponInstance)) {
            
            CompatibleSound burstShootSound = null;
            if(silencerOn) {
                burstShootSound = weapon.getSilencedBurstShootSound();
            }
            if(burstShootSound == null) {
                burstShootSound = weapon.getBurstShootSound();
            }
            if(burstShootSound != null) {
                if(weaponInstance.getSeriesShotCount() == 0) {
                    // Play burst sound only on start of the series
                    shootSound = burstShootSound;
                }
            } else {
                shootSound = silencerOn ? weapon.getSilencedShootSound() : weapon.getShootSound();
            }
        } else {
            shootSound = silencerOn ? weapon.getSilencedShootSound() : weapon.getShootSound();
        }
        
        if(shootSound != null) {
        	/*
        	try {
        		JSoundEngine.getInstance().playSound();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}*/
        	
        	
        	
            compatibility.playSound(player, shootSound,
                    silencerOn ? weapon.getSilencedShootSoundVolume() : weapon.getShootSoundVolume(), 1F);
        }
        
        int currentAmmo = weaponInstance.getAmmo();
        if(currentAmmo == 1 && weapon.getEndOfShootSound() != null) {
            compatibility.playSound(player, weapon.getEndOfShootSound(), 1F, 1F);
           
        }
        
       
        if(currentAmmo == 1) {
        	 weaponInstance.setSlideLock(true);
        }
        
        
        player.rotationPitch = player.rotationPitch - weaponInstance.getRecoil();
        float rotationYawFactor = -1.0f + random.nextFloat() * 2.0f;
        player.rotationYaw = player.rotationYaw + weaponInstance.getRecoil() * rotationYawFactor;
		

        Boolean muzzleFlash = modContext.getConfigurationManager().getProjectiles().isMuzzleEffects();
        if(muzzleFlash == null || muzzleFlash) {
            if(weapon.builder.flashIntensity > 0 && !silencerOn && Math.random() <= 1.0) {
            	
            	
            	
            		modContext.getEffectManager().spawnFlashParticle(player, weapon.builder.flashIntensity,
                            weapon.builder.flashScale.get(),
                            weaponInstance.isAimed() ? FLASH_X_OFFSET_ZOOMED : compatibility.getEffectOffsetX()
                                    + weapon.builder.flashOffsetX.get(),
                                    weaponInstance.isAimed() ? -1.55f :
                                    compatibility.getEffectOffsetY() + weapon.builder.flashOffsetY.get(),
                            weapon.builder.flashTexture);
            	
            	
                
            }  
        }
        
       
        //ClientValueRepo.gunPow.prevPosition = ClientValueRepo.gunPow.position;
        ClientValueRepo.fireWeapon(weaponInstance);
       // System.out.println("Gun tick added @ " + Minecraft.getMinecraft().player.ticksExisted);
        //System.out.println("WFA: " + System.currentTimeMillis());
        ClientValueRepo.flash = 1;
     
       ClientValueRepo.recoilStop = 50f;
        if(weapon.isSmokeEnabled()) {
            modContext.getEffectManager().spawnSmokeParticle(player, compatibility.getEffectOffsetX()
                    + weapon.builder.smokeOffsetX.get(),
                    compatibility.getEffectOffsetY() + weapon.builder.smokeOffsetY.get()+0.3f);
        }

        if(weapon.isShellCasingEjectEnabled() && weaponInstance != null)  {
        	
        	
        	/*
        	
        	// Change the raw position
        	Vec3d rawPosition = new Vec3d(CompatibleClientEventHandler.NEW_POS.get(0), CompatibleClientEventHandler.NEW_POS.get(1), CompatibleClientEventHandler.NEW_POS.get(2));
        	
        	
        	// Calculate the final position of the bullet spawn point
        	// by changing it's position along its own vector
        	double distance = 0.5;
			Vec3d eyePos = Minecraft.getMinecraft().player.getPositionEyes(1.0f);
			Vec3d finalPosition = rawPosition.subtract(eyePos).normalize().scale(distance).add(eyePos);
			
        	// Calculate velocity as 90 degrees to player
			Vec3d velocity = new Vec3d(-0.3, 0.1, 0.0);
    		velocity = velocity.rotateYaw((float) Math.toRadians(-Minecraft.getMinecraft().player.rotationYaw));
    		
    		// Spawn in shell
    		Shell shell = new Shell(weaponInstance.getWeapon().getShellType(), new Vec3d(finalPosition.x, finalPosition.y, finalPosition.z), new Vec3d(90, 0, 90), velocity);
        	CompatibleClientEventHandler.shellManager.enqueueShell(shell);
        	*/
        	
        	
        	
        	
        	
        }
        
       
        
        int seriesShotCount = weaponInstance.getSeriesShotCount();
        if(seriesShotCount == 0) {
            weaponInstance.setSeriesResetAllowed(false);
        }

        weaponInstance.setSeriesShotCount(seriesShotCount + 1);
        if(currentAmmo == 1 || weaponInstance.getSeriesShotCount() == weaponInstance.getMaxShots()) {
            weaponInstance.setLastBurstEndTimestamp(System.currentTimeMillis());
        }
        weaponInstance.setLastFireTimestamp(System.currentTimeMillis());
        weaponInstance.setAmmo(currentAmmo - 1);
        
        
    }

    private void ejectSpentRound(PlayerWeaponInstance weaponInstance) {
        EntityLivingBase player = weaponInstance.getPlayer();
        compatibility.playSound(player, weaponInstance.getWeapon().getEjectSpentRoundSound(), 1F, 1F);
    }

    //(weapon, player) 
    public void serverFire(EntityLivingBase player, ItemStack itemStack, boolean isBurst) {
        serverFire(player, itemStack, null, isBurst);
    }
    
    public void serverFire(EntityLivingBase player, ItemStack itemStack, BiFunction<Weapon, EntityLivingBase, ? extends WeaponSpawnEntity> spawnEntityWith, boolean isBurst) {
        if(!(itemStack.getItem() instanceof Weapon)) {
            return;
        }
        
        TargetPoint tp = new TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 100);
        modContext.getChannel().getChannel().sendToAllAround(new GunFXPacket(player.getEntityId()), tp);
        

        Weapon weapon = (Weapon) itemStack.getItem();
        
        int currentServerAmmo = Tags.getAmmo(itemStack);
        
        if(currentServerAmmo <= 0) {
            logger.error("No server ammo");
            return;
        }
        
        Tags.setAmmo(itemStack, --currentServerAmmo);
        
        if(spawnEntityWith == null) {
            spawnEntityWith = weapon.builder.spawnEntityWith;
        }
        
        for(int i = 0; i < weapon.builder.pellets; i++) {
            WeaponSpawnEntity spawnEntity = spawnEntityWith.apply(weapon, player);
            compatibility.spawnEntity(player, spawnEntity);
        }

        PlayerWeaponInstance playerWeaponInstance = Tags.getInstance(itemStack, PlayerWeaponInstance.class);

        if(playerWeaponInstance != null) {
        	
        	Vec3d velocity = new Vec3d(-0.3, 0.1, 0.0);
    		velocity = velocity.rotateYaw((float) Math.toRadians(-player.rotationYaw));
        	modContext.getChannel().getChannel().sendToAllAround(new BulletShellClient(playerWeaponInstance.getWeapon().getShellType(), player.getPositionEyes(1.0f), velocity), tp);
             

        }
        
        
        
        
        
        CompatibleSound shootSound = null;
        
        boolean silencerOn = playerWeaponInstance != null && modContext.getAttachmentAspect().isSilencerOn(playerWeaponInstance);
        if(isBurst && weapon.builder.isOneClickBurstAllowed) {
            
            CompatibleSound burstShootSound = null;
            if(silencerOn) {
                burstShootSound = weapon.getSilencedBurstShootSound();
            }
            if(burstShootSound == null) {
                burstShootSound = weapon.getBurstShootSound();
            }
            if(burstShootSound != null) {
                shootSound = burstShootSound;
            } else {
                shootSound = silencerOn ? weapon.getSilencedShootSound() : weapon.getShootSound();
            }
        } else {
            shootSound = silencerOn ? weapon.getSilencedShootSound() : weapon.getShootSound();
        }

        compatibility.playSoundToNearExcept(player, shootSound, 
                silencerOn ? weapon.getSilencedShootSoundVolume() : weapon.getShootSoundVolume(), 1.0F);

    }

}
