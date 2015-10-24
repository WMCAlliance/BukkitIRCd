package com.Jdbye.BukkitIRCd.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dynmap.DynmapWebChatEvent;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCFunctionality;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Configuration.Config;

public class DynmapListener implements Listener {

    /**
     Listener for the Dynmap web chat event.
     <p>
     @param webevt The event itself, containing the message.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDynmapWebChatEvent(DynmapWebChatEvent webevt) {
	try {
	    if (BukkitIRCdPlugin.dynmap == null) {
	    	return;
	    }

	    switch (IRCd.mode) {
		case STANDALONE:
		    IRCFunctionality.writeAll(":" + Config.getIrcdServerName() + "!" +
			    Config.getIrcdServerName() + "@" +
			    Config.getIrcdServerHostName() + " PRIVMSG " +
			    Config.getIrcdChannel() + " :[DynMap] " +
			    webevt.getName() + ": " + webevt.getMessage());
		    break;
		case INSPIRCD:
		    if (IRCd.isLinkcompleted() &&
			    IRCd.msgDynmapMessage.length() > 0) {
			final String message = IRCd.msgDynmapMessage.replace("{User}", webevt.getName()).replace("{Message}", webevt.getMessage());

			IRCFunctionality.privmsg(IRCd.serverUID, Config.getIrcdChannel(),
				message);
		    }
		    break;
	    }
	} catch (Exception e) {
	    // Catch-all block to avoid plugin crashes
	    BukkitIRCdPlugin.log.warning("Unexpected error occurred in event " +
		    webevt.getEventName());
	    e.printStackTrace();
	}
    }
}
