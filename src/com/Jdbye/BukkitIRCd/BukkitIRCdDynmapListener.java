package com.Jdbye.BukkitIRCd;

import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dynmap.DynmapWebChatEvent;

public class BukkitIRCdDynmapListener implements Listener {
	public BukkitIRCdDynmapListener(final BukkitIRCdPlugin instance) {
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDynmapWebChatEvent(DynmapWebChatEvent evt) {
		try {
			if (BukkitIRCdPlugin.dynmap == null) return;
		
			if(!(evt instanceof DynmapWebChatEvent)) return;
		
			DynmapWebChatEvent webevt = (DynmapWebChatEvent) evt;
			if (IRCd.mode == Modes.STANDALONE) {
				IRCd.writeAll(":" + Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName() + " PRIVMSG " + Config.getIrcdChannel() + " :[DynMap] " + webevt.getName() + ": "+webevt.getMessage());
			}
			else {
<<<<<<< HEAD
				if (IRCd.isLinkcompleted()) {
					if (IRCd.msgDynmapMessage.length() > 0) IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + Config.getIrcdChannel()+" :" + IRCd.msgDynmapMessage.replace("{User}", webevt.getName()).replace("{Message}", webevt.getMessage()));
=======
				if (IRCd.linkcompleted) {
					if (IRCd.msgDynmapMessage.length() > 0) IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + IRCd.channelName+" :" + IRCd.msgDynmapMessage.replace("%USER%", webevt.getName()).replace("%MESSAGE%", webevt.getMessage()));
>>>>>>> development
				}
			}
		} catch (Exception e) {
			// Catch-all block to avoid plugin crashes
			BukkitIRCdPlugin.log.warning("Unexpected error occurred in event " + evt.getEventName());
			e.printStackTrace();
		}
	}
}
