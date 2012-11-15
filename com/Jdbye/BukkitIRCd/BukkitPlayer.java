package com.Jdbye.BukkitIRCd;

import org.bukkit.entity.Player;

public class BukkitPlayer {
	
	public BukkitPlayer(String nick, String world, String mode, String host, String ip, long signedOn, long idleTime) {
		this.nick = nick;
		this.mode = mode;
		this.world = world;
		if (mode != null) this.textMode = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v");
		this.host = host;
		this.ip = ip;
		this.signedOn = signedOn;
		this.idleTime = idleTime;
	}
	
	public void setUID(String UID) {
		this.UID = UID;
	}
	
	public String getUID() {
		return this.UID;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
		this.textMode = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v");
	}
	
	public String getMode() {
		return this.mode;
	}
	
	public String getTextMode() {
		return this.textMode;
	}
	
	public boolean hasPermission(String permission) {
		if (IRCd.isPlugin && (BukkitIRCdPlugin.thePlugin != null)) {
			Player p = BukkitIRCdPlugin.thePlugin.getServer().getPlayer(nick);
			if (p != null) return BukkitIRCdPlugin.thePlugin.hasPermission(p, permission);
		}
		return false;
	}
	
	public String getWorld() {
		if (IRCd.isPlugin && (BukkitIRCdPlugin.thePlugin != null)) {
			Player p = BukkitIRCdPlugin.thePlugin.getServer().getPlayer(nick);
			if (p != null) return p.getWorld().getName();
		}
		return null;
	}
	
	String nick = null, host = null, ip = null, UID = null, world = null;
	private String mode = null,textMode = null;
	long signedOn = 0, idleTime = 0;
}