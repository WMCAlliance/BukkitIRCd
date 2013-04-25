package com.Jdbye.BukkitIRCd.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.Hash;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;

import com.Jdbye.BukkitIRCd.commands.*;

public class Messages extends JavaPlugin {
	public static FileConfiguration messages;
	
	
	public void saveMessages() {
		File messagesFile = new File(getDataFolder(), "messages.yml");
		Messages.messages = YamlConfiguration.Configuration(messagesFile);
		if (!(messagesFile.exists())) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Creating default messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin192." : ""));
			messages.options().copyDefaults(true);
			saveDefaultMessages(getDataFolder(),"messages.yml");
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Saving initial messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin194." : ""));
			
		}
	}
	
	
	
	
	
	// Load the messages from messages.yml
	public static void loadMessages(IRCd ircd) {
		try {
			IRCd.msgLinked = messages.getString("linked", IRCd.msgLinked);
			
			IRCd.msgSendQueryFromIngame = messages.getString("irc-send-pm", IRCd.msgSendQueryFromIngame);
			IRCd.msgDelinked = messages.getString("delinked", IRCd.msgDelinked);
			IRCd.msgDelinkedReason = messages.getString("delinked-reason", IRCd.msgDelinkedReason);
			
			IRCd.msgIRCJoin = messages.getString("irc-join", IRCd.msgIRCJoin);
			IRCd.msgIRCJoinDynmap = messages.getString("irc-join-dynmap", IRCd.msgIRCJoinDynmap);

			IRCd.groupPrefixes = messages.getConfigurationSection("group-prefixes");
			IRCd.groupSuffixes = messages.getConfigurationSection("group-suffixes");
			
			IRCd.msgIRCLeave = messages.getString("irc-leave", IRCd.msgIRCLeave);
			IRCd.msgIRCLeaveReason = messages.getString("irc-leave-reason", IRCd.msgIRCLeaveReason);
			IRCd.msgIRCLeaveDynmap = messages.getString("irc-leave-dynmap", IRCd.msgIRCLeaveDynmap);
			IRCd.msgIRCLeaveReasonDynmap = messages.getString("irc-leave-reason-dynmap", IRCd.msgIRCLeaveReasonDynmap);

			IRCd.msgIRCKick = messages.getString("irc-kick", IRCd.msgIRCKick);
			IRCd.msgIRCKickReason = messages.getString("irc-kick-reason", IRCd.msgIRCKickReason);
			IRCd.msgIRCKickDynmap = messages.getString("irc-kick-dynmap", IRCd.msgIRCKickDynmap);
			IRCd.msgIRCKickReasonDynmap = messages.getString("irc-kick-reason-dynmap", IRCd.msgIRCKickReasonDynmap);

			IRCd.msgIRCBan = messages.getString("irc-ban", IRCd.msgIRCBan);
			IRCd.msgIRCBanDynmap = messages.getString("irc-ban-dynmap", IRCd.msgIRCBanDynmap);

			IRCd.msgIRCUnban = messages.getString("irc-unban", IRCd.msgIRCUnban);
			IRCd.msgIRCUnbanDynmap = messages.getString("irc-unban-dynmap", IRCd.msgIRCUnbanDynmap);

			IRCd.msgIRCNickChange = messages.getString("irc-nick-change", IRCd.msgIRCNickChange);
			IRCd.msgIRCNickChangeDynmap = messages.getString("irc-nick-change-dynmap", IRCd.msgIRCNickChangeDynmap);

			IRCd.msgIRCAction = messages.getString("irc-action", IRCd.msgIRCAction);
			IRCd.msgIRCMessage = messages.getString("irc-message", IRCd.msgIRCMessage);
			IRCd.msgIRCNotice = messages.getString("irc-notice", IRCd.msgIRCNotice);

			IRCd.msgIRCPrivateAction = messages.getString("irc-private-action", IRCd.msgIRCPrivateAction);
			IRCd.msgIRCPrivateMessage = messages.getString("irc-private-message", IRCd.msgIRCPrivateMessage);
			IRCd.msgIRCPrivateNotice = messages.getString("irc-private-notice", IRCd.msgIRCPrivateNotice);

			IRCd.msgIRCActionDynmap = messages.getString("irc-action-dynmap", IRCd.msgIRCActionDynmap);
			IRCd.msgIRCMessageDynmap = messages.getString("irc-message-dynmap", IRCd.msgIRCMessageDynmap);
			IRCd.msgIRCNoticeDynmap = messages.getString("irc-notice-dynmap", IRCd.msgIRCNoticeDynmap);

			IRCd.msgDynmapMessage = messages.getString("dynmap-message", IRCd.msgDynmapMessage);
			IRCd.msgPlayerList = messages.getString("player-list", IRCd.msgPlayerList);
			
			IRCd.consoleFilters = messages.getStringList("console-filters");
			//** RECOLOUR ALL MESSAGES **
			
			IRCd.msgSendQueryFromIngame = BukkitIRCdPlugin.colorize(IRCd.msgSendQueryFromIngame);
			IRCd.msgLinked = BukkitIRCdPlugin.colorize(IRCd.msgLinked);
			IRCd.msgDelinked = BukkitIRCdPlugin.colorize(IRCd.msgDelinked);
			IRCd.msgDelinkedReason = BukkitIRCdPlugin.colorize(IRCd.msgDelinked);
			IRCd.msgIRCJoin = BukkitIRCdPlugin.colorize(IRCd.msgIRCJoin);
			IRCd.msgIRCJoinDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCJoinDynmap);
			IRCd.msgIRCLeave = BukkitIRCdPlugin.colorize(IRCd.msgIRCLeave);
			IRCd.msgIRCLeaveReason = BukkitIRCdPlugin.colorize(IRCd.msgIRCLeaveReason);
			IRCd.msgIRCLeaveDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCLeaveDynmap);
			IRCd.msgIRCLeaveReasonDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCLeaveReasonDynmap);
			IRCd.msgIRCKick = BukkitIRCdPlugin.colorize(IRCd.msgIRCKick);
			IRCd.msgIRCKickReason = BukkitIRCdPlugin.colorize(IRCd.msgIRCKickReason);
			IRCd.msgIRCKickDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCKickDynmap);
			IRCd.msgIRCKickReasonDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCKickReasonDynmap);
			IRCd.msgIRCBan = BukkitIRCdPlugin.colorize(IRCd.msgIRCBan);
			IRCd.msgIRCBanDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCBanDynmap);
			IRCd.msgIRCUnban = BukkitIRCdPlugin.colorize(IRCd.msgIRCUnban);
			IRCd.msgIRCUnbanDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCUnbanDynmap);
			IRCd.msgIRCNickChange = BukkitIRCdPlugin.colorize(IRCd.msgIRCNickChange);
			IRCd.msgIRCNickChangeDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCNickChangeDynmap);
			IRCd.msgIRCAction = BukkitIRCdPlugin.colorize(IRCd.msgIRCAction);
			IRCd.msgIRCMessage = BukkitIRCdPlugin.colorize(IRCd.msgIRCMessage);
			IRCd.msgIRCNotice = BukkitIRCdPlugin.colorize(IRCd.msgIRCNotice);
			IRCd.msgIRCPrivateAction = BukkitIRCdPlugin.colorize(IRCd.msgIRCPrivateAction);
			IRCd.msgIRCPrivateMessage = BukkitIRCdPlugin.colorize(IRCd.msgIRCPrivateMessage);
			IRCd.msgIRCPrivateNotice = BukkitIRCdPlugin.colorize(IRCd.msgIRCPrivateNotice);
			IRCd.msgIRCActionDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCActionDynmap);
			IRCd.msgIRCMessageDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCMessageDynmap);
			IRCd.msgIRCNoticeDynmap = BukkitIRCdPlugin.colorize(IRCd.msgIRCNoticeDynmap);
			IRCd.msgDynmapMessage = BukkitIRCdPlugin.colorize(IRCd.msgDynmapMessage);
			IRCd.msgPlayerList = BukkitIRCdPlugin.colorize(IRCd.msgPlayerList);

			BukkitIRCdPlugin.log.info("[BukkitIRCd] Loaded messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin464." : ""));
		}
		catch (Exception e) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load messages file: " + e.toString());
		}
	}
	
	private void saveDefaultMessages(File dataFolder, String fileName)
	{
		BukkitIRCdPlugin.log.info("[BukkitIRCd] Messages file not found, creating new one." + (IRCd.debugMode ? " Code BukkitIRCdPlugin705" : ""));
		dataFolder.mkdirs();

		File msgFile = new File(dataFolder, fileName);
		try
		{
			if(!msgFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			BukkitIRCdPlugin.log.warning("[BukkitIRCd] Could not create messages file!" + (IRCd.debugMode ? " Error code BukkitIRCdPlugin716." : ""));
		}

		writeMessages(msgFile);
	}
	
	private void writeMessages(File messagesFile)
	{
		try
		{
			messages.save(messagesFile);
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Saved messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin728." : ""));
		}
		catch(Exception e)
		{
			BukkitIRCdPlugin.log.warning("[BukkitIRCd] Caught exception while writing messages to file: ");
			e.printStackTrace();
		}
	}

	
}