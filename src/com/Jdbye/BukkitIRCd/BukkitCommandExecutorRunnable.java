package com.Jdbye.BukkitIRCd;

import org.bukkit.scheduler.BukkitRunnable;

public class BukkitCommandExecutorRunnable extends BukkitRunnable{

	private String command;
	private final BukkitIRCdPlugin instance;
	public BukkitCommandExecutorRunnable(BukkitIRCdPlugin instance, String command){
		this.command = command;
		this.instance = instance;
	}

	public void run() {
		instance.getServer().dispatchCommand(instance.getServer().getConsoleSender(), command);
		
	}
	

}
