package com.vicmatskiv.weaponlib.compatibility;


import java.io.ByteArrayOutputStream;
import org.apache.commons.codec.binary.Hex;

import com.vicmatskiv.weaponlib.ModContext;
import com.vicmatskiv.weaponlib.config.BalancePackManager;
import com.vicmatskiv.weaponlib.crafting.CraftingFileManager;
import com.vicmatskiv.weaponlib.jim.util.ByteArrayUtils;
import com.vicmatskiv.weaponlib.network.packets.BalancePackClient;
import com.vicmatskiv.weaponlib.network.packets.CraftingClientPacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public abstract class CompatibleServerEventHandler {

    public abstract String getModId();

	@SubscribeEvent
	public void onItemToss(ItemTossEvent itemTossEvent) {
		onCompatibleItemToss(itemTossEvent);
	}

	
	protected abstract void onCompatibleItemToss(ItemTossEvent itemTossEvent);
	
	@SubscribeEvent
	public final void onEntityJoinedEvent(EntityJoinWorldEvent evt) {
		// We are only interested in the player. We also only want to deal with this if the server and the client
		// are operating off of DIFFERENT file systems (hence the dedicated server check!).
		if(!(evt.getEntity() instanceof EntityPlayer) || !FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) return;
		
		System.out.println("WARNING RUNNING!");
		
		EntityPlayer player = (EntityPlayer) evt.getEntity();
		if(player.world.isRemote) return;
		
	
		ByteArrayOutputStream baos = ByteArrayUtils.createByteArrayOutputStreamFromBytes(CraftingFileManager.getInstance().getCurrentFileHash());
		if(baos == null) return;
		
		getModContext().getChannel().getChannel().sendTo(new CraftingClientPacket(baos, true), (EntityPlayerMP) player);
		
		System.out.println(Hex.encodeHexString(CraftingFileManager.getInstance().getCurrentFileHash()));
		
		System.out.println("An entity has joined the server ;)");
		
	}
	
	@SubscribeEvent
    public final void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
		

		if(event.phase == Phase.END) {
			if(!event.player.world.isRemote) {
				for(EntityPlayer ep : event.player.getEntityWorld().playerEntities) {
					//new HelloWorldPacket();
					
					//ByteArrayOutputStream baos = new ByteArrayOutputStream();
					
					
				
					/*
					
					
					try {
						byte[] ba = new byte[100000];
						for(int i = 0; i < ba.length; ++i) {
							ba[i] = 0x01b;
						}
						
						baos.write(ba);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					JsonObject obj = new JsonObject();
					obj.addProperty("cock", 5);
					
			
					byte[] json = obj.toString().getBytes();
					ByteArrayInputStream bais = new ByteArrayInputStream(json);
					
					JsonObject obj2 = new GsonBuilder().create().fromJson(new InputStreamReader(bais), JsonObject.class);
					System.out.println(obj2);
					*/
					
					
					//getModContext().getChannel().getChannel().sendTo(new CraftingClientPacket(baos), (EntityPlayerMP) ep);
				}
			}
		}
 		
 		
        if(event.phase == Phase.END) {            
            int updatedFlags = CompatibleExtraEntityFlags.getFlags(event.player);
            if((updatedFlags & CompatibleExtraEntityFlags.PRONING) != 0) {
                setSize(event.player, 0.6f, 0.6f); //player.width, player.width);
            }
        }
    }
    
    protected void setSize(EntityPlayer entityPlayer, float width, float height)
    {
        if (width != entityPlayer.width || height != entityPlayer.height)
        {
            entityPlayer.width = width;
            entityPlayer.height = height;
            AxisAlignedBB axisalignedbb = entityPlayer.getEntityBoundingBox();
            entityPlayer.setEntityBoundingBox(new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ, axisalignedbb.minX + (double)entityPlayer.width, axisalignedbb.minY + (double)entityPlayer.height, axisalignedbb.minZ + (double)entityPlayer.width));
        }
    }

	@SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if(event.phase == Phase.START) {
            onCompatibleServerTickEvent(new CompatibleServerTickEvent(event));
        }
    }
	
	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
	    onCompatiblePlayerLoggedIn(event);
	    //System.out.println("hi");
	    getModContext().getChannel().getChannel().sendTo(new BalancePackClient(BalancePackManager.getActiveBalancePack()), (EntityPlayerMP) event.player);
	    
	}
	
    protected abstract void onCompatibleServerTickEvent(CompatibleServerTickEvent e);
    
    protected abstract void onCompatiblePlayerLoggedIn(PlayerLoggedInEvent e);


	@SubscribeEvent
	public void attachCapability(AttachCapabilitiesEvent<Entity> event)
	{
	    if(event.getObject() instanceof EntityPlayer) {
	        ResourceLocation PLAYER_ENTITY_TRACKER = new ResourceLocation(getModId(), "PLAYER_ENTITY_TRACKER");
	        event.addCapability(PLAYER_ENTITY_TRACKER, new CompatiblePlayerEntityTrackerProvider());
	        
	        ResourceLocation extraFlagsResourceLocation = new ResourceLocation(getModId(), "PLAYER_ENTITY_FLAGS");
            event.addCapability(extraFlagsResourceLocation, new CompatibleExtraEntityFlags());
            
            ResourceLocation customInventoryLocation = new ResourceLocation(getModId(), "PLAYER_CUSTOM_INVENTORY");

            event.addCapability(customInventoryLocation, new CompatibleCustomPlayerInventoryCapability());
            
            ResourceLocation playerMissionsResourceLocation = new ResourceLocation(getModId(), "PLAYER_MISSIONS");
            event.addCapability(playerMissionsResourceLocation, new CompatibleMissionCapability());
	    }
	    
        ResourceLocation exposureResourceLocation = new ResourceLocation(getModId(), "EXPOSURE");
        event.addCapability(exposureResourceLocation, new CompatibleExposureCapability());
	    
	}

    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent e) {
        onCompatibleEntityJoinWorld(new CompatibleEntityJoinWorldEvent(e));
    }

    protected abstract void onCompatibleEntityJoinWorld(CompatibleEntityJoinWorldEvent e);
    
    @SubscribeEvent
    public void playerDroppedItem(PlayerDropsEvent e) {
        onCompatiblePlayerDropsEvent(new CompatiblePlayerDropsEvent(e));
    }

    @SubscribeEvent
    public void playerStartedTracking(PlayerEvent.StartTracking e) {
        onCompatiblePlayerStartedTracking(new CompatibleStartTrackingEvent(e));
    }

    @SubscribeEvent
    public void playerStoppedTracking(PlayerEvent.StopTracking e) {
        //onCompatiblePlayerStoppedTracking(new CompatibleStopTrackingEvent(e));
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent e) {
        onCompatibleLivingDeathEvent(new CompatibleLivingDeathEvent(e));
    }
    
    @SubscribeEvent
    public void onEntityUpdate(LivingUpdateEvent e) {
        onCompatibleLivingUpdateEvent(new CompatibleLivingUpdateEvent(e));
    }
    
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        onCompatibleLivingHurtEvent(new CompatibleLivingHurtEvent(event));
    }
    
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone e) {
        onCompatiblePlayerCloneEvent(new CompatiblePlayerCloneEvent(e));
    }
    
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        onCompatiblePlayerRespawnEvent(new CompatiblePlayerRespawnEvent(e));
    }
    
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.EntityInteract e) {
        onCompatiblePlayerInteractInteractEvent(new CompatiblePlayerEntityInteractEvent(e));
    }

//    protected abstract void onCompatibleLivingDeathEvent(LivingDeathEvent e);

    protected abstract void onCompatiblePlayerInteractInteractEvent(
            CompatiblePlayerEntityInteractEvent compatiblePlayerInteractEvent);

    protected abstract void onCompatiblePlayerStartedTracking(CompatibleStartTrackingEvent e);

    protected abstract void onCompatiblePlayerStoppedTracking(CompatibleStopTrackingEvent e);
    
    protected abstract void onCompatibleLivingUpdateEvent(CompatibleLivingUpdateEvent e);

    protected abstract void onCompatibleLivingHurtEvent(CompatibleLivingHurtEvent e);

    protected abstract void onCompatiblePlayerDropsEvent(CompatiblePlayerDropsEvent e);
    
    protected abstract void onCompatiblePlayerCloneEvent(CompatiblePlayerCloneEvent compatiblePlayerCloneEvent);

    protected abstract void onCompatiblePlayerRespawnEvent(CompatiblePlayerRespawnEvent compatiblePlayerRespawnEvent);

    protected abstract void onCompatibleLivingDeathEvent(CompatibleLivingDeathEvent event);

    public abstract ModContext getModContext();
}
