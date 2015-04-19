package com.Jdbye.BukkitIRCd.Configuration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Messages extends JavaPlugin {

	public static FileConfiguration messages;

	private static String populate(final String key, final String def) {
		messages.addDefault(key, def);
		final String msg = messages.getString(key);
		final String coloredMsg = ChatColor.translateAlternateColorCodes('&',
				msg);
		return coloredMsg;
	}

	// Load the messages from messages.yml
	public static void loadMessages(IRCd ircd) {

		final File dataFolder = BukkitIRCdPlugin.thePlugin.getDataFolder();
		final File messagesFile = new File(dataFolder, "messages.yml");
		messages = YamlConfiguration.loadConfiguration(messagesFile);

		try {
			IRCd.msgLinked = populate("linked", IRCd.msgLinked);

			IRCd.msgSendQueryFromIngame = populate("irc-send-pm", IRCd.msgSendQueryFromIngame);
			IRCd.msgDelinked = populate("delinked", IRCd.msgDelinked);
			IRCd.msgDelinkedReason = populate("delinked-reason", IRCd.msgDelinkedReason);

			IRCd.msgIRCJoin = populate("irc-join", IRCd.msgIRCJoin);
			IRCd.msgIRCJoinDynmap = populate("irc-join-dynmap", IRCd.msgIRCJoinDynmap);

			IRCd.msgIRCLeave = populate("irc-leave", IRCd.msgIRCLeave);
			IRCd.msgIRCLeaveReason = populate("irc-leave-reason", IRCd.msgIRCLeaveReason);
			IRCd.msgIRCLeaveDynmap = populate("irc-leave-dynmap", IRCd.msgIRCLeaveDynmap);
			IRCd.msgIRCLeaveReasonDynmap = populate("irc-leave-reason-dynmap", IRCd.msgIRCLeaveReasonDynmap);

			IRCd.msgIRCKick = populate("irc-kick", IRCd.msgIRCKick);
			IRCd.msgIRCKickReason = populate("irc-kick-reason", IRCd.msgIRCKickReason);
			IRCd.msgIRCKickDisplay = populate("irc-kick-display", IRCd.msgIRCKickDisplay);
			IRCd.msgIRCKickDisplayReason = populate("irc-kick-display-reason", IRCd.msgIRCKickDisplayReason);
			IRCd.msgIRCKickDynmap = populate("irc-kick-dynmap", IRCd.msgIRCKickDynmap);
			IRCd.msgIRCKickReasonDynmap = populate("irc-kick-reason-dynmap", IRCd.msgIRCKickReasonDynmap);

			IRCd.msgIRCBan = populate("irc-ban", IRCd.msgIRCBan);
			IRCd.msgIRCBanDynmap = populate("irc-ban-dynmap", IRCd.msgIRCBanDynmap);

			IRCd.msgIRCUnban = populate("irc-unban", IRCd.msgIRCUnban);
			IRCd.msgIRCUnbanDynmap = populate("irc-unban-dynmap", IRCd.msgIRCUnbanDynmap);

			IRCd.msgIRCNickChange = populate("irc-nick-change", IRCd.msgIRCNickChange);
			IRCd.msgIRCNickChangeDynmap = populate("irc-nick-change-dynmap", IRCd.msgIRCNickChangeDynmap);

			IRCd.msgIRCAction = populate("irc-action", IRCd.msgIRCAction);
			IRCd.msgIRCMessage = populate("irc-message", IRCd.msgIRCMessage);
			IRCd.msgIRCNotice = populate("irc-notice", IRCd.msgIRCNotice);

			IRCd.msgIRCPrivateAction = populate("irc-private-action", IRCd.msgIRCPrivateAction);
			IRCd.msgIRCPrivateMessage = populate("irc-private-message", IRCd.msgIRCPrivateMessage);
			IRCd.msgIRCPrivateNotice = populate("irc-private-notice", IRCd.msgIRCPrivateNotice);

			IRCd.msgIRCActionDynmap = populate("irc-action-dynmap", IRCd.msgIRCActionDynmap);
			IRCd.msgIRCMessageDynmap = populate("irc-message-dynmap", IRCd.msgIRCMessageDynmap);
			IRCd.msgIRCNoticeDynmap = populate("irc-notice-dynmap", IRCd.msgIRCNoticeDynmap);

			IRCd.msgDynmapMessage = populate("dynmap-message", IRCd.msgDynmapMessage);

			IRCd.groupPrefixes = messages.getConfigurationSection("group-prefixes");
			IRCd.groupSuffixes = messages.getConfigurationSection("group-suffixes");
					
			IRCd.consoleFilters = messages.getStringList("console-filters");
			
			IRCd.userDisconnectMsg = messages.getString("user-disconnect-msg");
			IRCd.userModeMsg = messages.getString("user-usermode-msg");
			
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Loaded messages file." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin464." : ""));
		} catch (Exception e) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load messages file: " + e.toString());
		}

		if (!messagesFile.exists()) {
			BukkitIRCdPlugin.log
					.info("[BukkitIRCd] Saving initial messages file." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin194." : ""));

			try {
				dataFolder.mkdirs();
				messages.options().copyDefaults(true);
				messages.save(messagesFile);
			} catch (IOException e) {
				BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to save messages file." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin195." : ""));
			}
		}
	}
}
