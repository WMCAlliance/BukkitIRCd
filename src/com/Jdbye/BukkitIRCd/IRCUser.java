package com.Jdbye.BukkitIRCd;

public class IRCUser {

	public String nick, realname, ident, hostmask, realhost, ipaddress;
	private String modes = "", textModes = "";
	private String consoleModes = "", consoleTextModes = "";
	public String customWhois = ""; // Not used yet
	// public boolean isIdented = false;
	// public boolean isNickSet = false;
	public String accountname = "";
	public boolean isRegistered = false;
	public boolean isOper = false;
	public boolean joined = false; // Whether the user has joined the plugin channel

	public boolean consoleJoined = false; // Whether the user has joined the console channel, only used in linking mode

	public String awayMsg = "";
	// public long lastPingResponse;
	public long signonTime;
	public long lastActivity;

	public IRCUser(String nick, String realname, String ident, String hostmask, String ipaddress, String modes, String customWhois, boolean isRegistered, boolean isOper, String awayMsg, long signonTime, long lastActivity, String accountname) {
		this.nick = nick;
		this.realname = realname;
		this.ident = ident;
		this.hostmask = hostmask;
		this.realhost = hostmask;
		this.ipaddress = ipaddress;
		this.modes = modes;
		this.textModes = modes.replace("~", "q").replace("&", "a") .replace("@", "o").replace("%", "h").replace("+", "v");
		this.customWhois = customWhois;
		this.isRegistered = isRegistered;
		this.accountname = accountname;
		this.isOper = isOper;
		this.awayMsg = awayMsg;
		this.signonTime = signonTime;
		this.lastActivity = lastActivity;
		this.joined = true;
		this.consoleJoined = false;
	}

	public IRCUser(String nick, String realname, String ident, String hostmask, String vhost, String ipaddress, String modes, String customWhois, boolean isRegistered, boolean isOper, String awayMsg, long signonTime, long lastActivity, String accountname) {
		this.nick = nick;
		this.realname = realname;
		this.ident = ident;
		this.hostmask = vhost;
		this.realhost = hostmask;
		this.ipaddress = ipaddress;
		this.textModes = modes.replace("~", "q").replace("&", "a") .replace("@", "o").replace("%", "h").replace("+", "v") .replaceAll("[^A-Za-z0-9 ]", "");
		this.modes = textModes.replace("q", "~").replace("a", "&").replace("o", "@").replace("h", "%").replace("v", "+");
		this.customWhois = customWhois;
		this.isRegistered = isRegistered;
		this.accountname = accountname;
		this.isOper = isOper;
		this.awayMsg = awayMsg;
		this.signonTime = signonTime;
		this.lastActivity = lastActivity;
		this.joined = false;
		this.consoleJoined = false;
	}

	public void setModes(String mode) {
		this.textModes = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v").replaceAll("[^A-Za-z0-9 ]", "");
		this.modes = textModes.replace("q", "~").replace("a", "&").replace("o", "@").replace("h", "%").replace("v", "+");
	}

	public String getModes() {
		return this.modes;
	}

	public String getTextModes() {
		return this.textModes;
	}

	public void setConsoleModes(String mode) {
		this.consoleTextModes = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v").replaceAll("[^A-Za-z0-9 ]", "");
		this.consoleModes = consoleTextModes.replace("q", "~").replace("a", "&").replace("o", "@").replace("h", "%").replace("v", "+");
	}

	public String getConsoleModes() {
		return this.consoleModes;
	}

	public String getConsoleTextModes() {
		return this.consoleTextModes;
	}

	public long getSecondsIdle() {
		return (System.currentTimeMillis() - lastActivity) / 1000L;
	}
}