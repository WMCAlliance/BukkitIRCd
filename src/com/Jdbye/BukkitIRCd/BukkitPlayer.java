package com.Jdbye.BukkitIRCd;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 Provides a Bukkit Player object, and everything involved.
 */
public class BukkitPlayer {

    String nick = null, host = null, realhost = null, ip = null, UID = null, world = null;
    private String mode = null, textMode = null;
    long signedOn = 0, idleTime = 0;

    public BukkitPlayer(String nick, String world, String mode, String realhost, String host, String ip, long signedOn, long idleTime) {
	this.nick = nick;
	this.mode = mode;
	this.world = world;
	if (mode != null) {
	    this.textMode = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v");
	}
	this.host = host;
	this.realhost = realhost;
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
	this.textMode = mode.replace("~", "q").replace("&", "a")
		.replace("@", "o").replace("%", "h").replace("+", "v");
    }

    public String getMode() {
	return this.mode;
    }

    public String getTextMode() {
	return this.textMode;
    }
    // TODO Replace .getPlayer as it seems to be Deprecated. 
    @SuppressWarnings("deprecation")
	public boolean hasPermission(String permission) {
	if (IRCd.isPlugin()) {
	    final Player p = Bukkit.getServer().getPlayer(nick);
	    if (p != null) {
		return p.hasPermission(permission);
	    }
	}
	return false;
    }
    // TODO Replace .getPlayer as it seems to be Deprecated.
    @SuppressWarnings("deprecation")
    public String getWorld() {
	if (IRCd.isPlugin() && (BukkitIRCdPlugin.thePlugin != null)) {
	    Player p = BukkitIRCdPlugin.thePlugin.getServer().getPlayer(nick);
	    if (p != null) {
		return p.getWorld().getName();
	    }
	}
	return null;
    }
}
