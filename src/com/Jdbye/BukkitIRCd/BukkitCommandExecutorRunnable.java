package com.Jdbye.BukkitIRCd;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.server.ServerCommandEvent;
public class BukkitCommandExecutorRunnable extends BukkitRunnable{

	private String command;
	private final BukkitIRCdPlugin instance;
	public BukkitCommandExecutorRunnable(BukkitIRCdPlugin instance, String command){
		this.command = command;
		this.instance = instance;
	}

	public void run() {
		//Call a ServerCommandEvent whenever a command is run from IRC to allow other plugins to get to it
		ServerCommandEvent commandEvent = new ServerCommandEvent(instance.getServer().getConsoleSender(),command);
		instance.getServer().getPluginManager().callEvent(commandEvent);
		instance.getServer().dispatchCommand(commandEvent.getSender(), commandEvent.getCommand());
	}
	

}
