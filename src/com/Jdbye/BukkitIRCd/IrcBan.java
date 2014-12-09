package com.Jdbye.BukkitIRCd;

public class IrcBan {

	public IrcBan(String fullHost, String bannedBy, long banTime) {
		this.fullHost = fullHost;
		this.bannedBy = bannedBy;
		this.banTime = banTime;
	}

	public String fullHost = null;
	public String bannedBy = null;
	public long banTime = 0;
}
