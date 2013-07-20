package com.Jdbye.BukkitIRCd;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitKickRunnable extends BukkitRunnable{

	//We need this to avoid async kicks.
	//Kicks are not thread safe
	private final String kickedPlayer;
	private final String kickReason;
	public BukkitKickRunnable(BukkitIRCdPlugin instance, String kickedPlayer, String kickReason){
		this.kickedPlayer = kickedPlayer;
		this.kickReason = kickReason;

	}

	@Override
	public void run() {
		Bukkit.getServer().getPlayer(kickedPlayer).kickPlayer(kickReason);
	}
}
