package com.Jdbye.BukkitIRCd;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitKickRunnable extends BukkitRunnable{


	//We need this to avoid async kicks. 
	//Kicks are not thread safe
	private Player kickedPlayer;
	private String kickReason;
	public BukkitKickRunnable(BukkitIRCdPlugin instance, Player kickedPlayer, String kickReason){
		this.kickedPlayer = kickedPlayer;
		this.kickReason = kickReason;
		
	}

	public void run() {
		kickedPlayer.kickPlayer(kickReason);
	}
	
	
	
	
}
