package com.vicmatskiv.weaponlib;

import java.util.function.Function;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ReloadMessageHandler implements IMessageHandler<ReloadMessage, IMessage> {
	
	private Function<MessageContext, EntityPlayer> entityPlayerSupplier;

	public ReloadMessageHandler(Function<MessageContext, EntityPlayer> entityPlayerSupplier) {
		this.entityPlayerSupplier = entityPlayerSupplier;
	}
	@Override
	public IMessage onMessage(ReloadMessage message, MessageContext ctx) {
		if(ctx.side == Side.SERVER) {
			EntityPlayer player = entityPlayerSupplier.apply(ctx);
			ItemStack itemStack = player.getHeldItem();
			
			if(itemStack != null && itemStack.getItem() instanceof Weapon) {
				((Weapon) itemStack.getItem()).reload(itemStack, player);
			}
		} else {
			onClientMessage(message, ctx);
		}
		return null;
	}

	private void onClientMessage(ReloadMessage message, MessageContext ctx) {
		EntityPlayer player = entityPlayerSupplier.apply(ctx);
		ItemStack itemStack = player.getHeldItem();
		if(itemStack != null && itemStack.getItem() instanceof Weapon) {
			//Weapon weapon = (Weapon) itemStack.getItem();
			Weapon targetWeapon = message.getWeapon();
			targetWeapon.completeReload(itemStack, player, message.getAmmo(), itemStack.getItem() != targetWeapon);
		}
	}

}
