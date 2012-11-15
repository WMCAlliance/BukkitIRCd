package com.Jdbye.BukkitIRCd;

import java.util.ArrayList;
import java.util.List;

public class IRCServer {

	public IRCServer(String host, String name, String SID, String hub) {
		this.host = host;
		this.name = name;
		this.SID = SID;
		this.hub = hub;
	}
	
	public String host = null, name = null, SID = null, hub = null;
	public List<String> leaves = new ArrayList<String>();
}