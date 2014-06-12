package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.channelTopic;
import static com.Jdbye.BukkitIRCd.IRCd.channelTopicSet;
import static com.Jdbye.BukkitIRCd.IRCd.channelTopicSetDate;
import static com.Jdbye.BukkitIRCd.IRCd.csServer;
import static com.Jdbye.BukkitIRCd.IRCd.isPlugin;
import static com.Jdbye.BukkitIRCd.IRCd.linkcompleted;
import static com.Jdbye.BukkitIRCd.IRCd.mode;
import static com.Jdbye.BukkitIRCd.IRCd.msgDelinkedReason;
import static com.Jdbye.BukkitIRCd.IRCd.pre;
import static com.Jdbye.BukkitIRCd.IRCd.server;
import static com.Jdbye.BukkitIRCd.IRCd.writeAll;
import static com.Jdbye.BukkitIRCd.IRCd.writeOpers;
import com.Jdbye.BukkitIRCd.configuration.Config;
import java.io.IOException;

public class IRCFunctionality {
    
    // This is where the channel topic is configured
    public static void setTopic(String topic, String user, String userhost) {
	channelTopic = topic;
	channelTopicSetDate = System.currentTimeMillis() / 1000L;
	if (user.length() > 0) {
	    channelTopicSet = user;
	}
	if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
	    Config.setIrcdTopic(topic);
	    Config.setIrcdTopicSetDate(System.currentTimeMillis());
	    if (user.length() > 0) {
		Config.setIrcdTopicSetBy(user);
	    }
	}

	if (mode == Modes.STANDALONE) {
	    writeAll(":" + userhost + " TOPIC " + Config.getIrcdChannel() +
		    " :" + channelTopic);
	    writeOpers(":" + userhost + " TOPIC " +
		    Config.getIrcdConsoleChannel() + " :" + channelTopic);
	} else if (mode == Modes.INSPIRCD) {
	    BukkitPlayer bp;
	    if ((bp = BukkitUserManagement.getUserObject(user)) != null) {
		Utils.println(":" + bp.getUID() + " TOPIC " + Config.getIrcdChannel() +
			" :" + channelTopic);
	    }
	}
    }

    public static void disconnectServer(String reason) {
	synchronized (csServer) {
	    if (mode == Modes.INSPIRCD) {
		if ((server != null) && server.isConnected()) {
		    Utils.println(pre + "SQUIT " + Config.getLinkServerID() + " :" +
			    reason);
		    if (linkcompleted) {
			if (reason != null && msgDelinkedReason.length() > 0) {
			    IRCd.broadcastMessage(msgDelinkedReason.replace(
				    "{LinkName}", Config.getLinkName())
				    .replace("{Reason}", reason));
			}
			linkcompleted = false;
		    }
		    try {
			server.close();
		    } catch (IOException e) {
		    }
		} else if (Config.isDebugModeEnabled()) {
		    System.out
			    .println("[BukkitIRCd] Already disconnected from link, so no need to cleanup.");
		}
	    }
	}
	if (IRCd.listener != null) {
	    try {
		IRCd.listener.close();
	    } catch (IOException e) {
		// TODO Original developer left this blank, we want to catch errors properly, or not cause them at all
	    }
	}
    }

}