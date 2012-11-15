package com.Jdbye.BukkitIRCd;

public class IrcBan {
	
	public IrcBan(String fullHost, String bannedBy, long banTime) {
		this.fullHost = fullHost;
		this.bannedBy = bannedBy;
		this.banTime = banTime;
	}
	
	String fullHost = null, bannedBy = null;
	long banTime = 0;
}