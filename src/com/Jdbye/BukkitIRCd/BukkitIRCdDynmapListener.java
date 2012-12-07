package com.Jdbye.BukkitIRCd;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dynmap.DynmapWebChatEvent;

public class BukkitIRCdDynmapListener implements Listener {
	private BukkitIRCdPlugin plugin;
	
	public BukkitIRCdDynmapListener(final BukkitIRCdPlugin instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDynmapWebChatEvent(DynmapWebChatEvent evt) {
		try {
			if (plugin.dynmap == null) return;
		
			if(!(evt instanceof DynmapWebChatEvent)) return;
		
			DynmapWebChatEvent webevt = (DynmapWebChatEvent) evt;
			if (plugin.ircd.mode == Modes.STANDALONE) {
				IRCd.writeAll(":"+IRCd.serverName+"!"+IRCd.serverName+"@"+IRCd.serverHostName+" PRIVMSG " + IRCd.channelName + " :[Dynmap] "+webevt.getName()+": "+webevt.getMessage());
			}
			else {
				if (IRCd.linkcompleted) {
					if (IRCd.msgDynmapMessage.length() > 0) IRCd.println(":"+IRCd.serverUID+" PRIVMSG "+IRCd.channelName+" :"+IRCd.msgDynmapMessage.replace("%USER%", webevt.getName()).replace("%MESSAGE%", webevt.getMessage()));
				}
			}
		} catch (Exception e) {
			// Catch-all block to avoid plugin crashes
			plugin.log.warning("Unexpected error occurred in event "+evt.getEventName());
			e.printStackTrace();
		}
	}
}