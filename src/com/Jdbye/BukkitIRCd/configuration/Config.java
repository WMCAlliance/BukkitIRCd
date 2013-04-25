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


public class Config extends JavaPlugin{

	private String mode = "standalone";

	private static Date curDate = new Date();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	public static String ircd_creationdate = dateFormat.format(curDate);
	
	public static boolean ircd_redundant_modes = false;
	public static int ircd_port = 6667;
	public static int ircd_maxconn = 1000;
	public static int ircd_pinginterval = 45;
	public static int ircd_timeout = 180;
	public static int ircd_maxnicklen = 32;
	public static String ircd_channel = "#minecraft";
	public static String ircd_servername = "BukkitIRCd";
	public static String ircd_serverdescription = "Minecraft BukkitIRCd Server";
	public static String ircd_serverhostname = "bukkitircd.localhost";
	public static String ircd_ingamesuffix = "/minecraft";
	public static String ircd_topic = "Welcome to a Bukkit server!";
	public static String ircd_topicsetby = ircd_servername;
	public static long ircd_topicsetdate = System.currentTimeMillis() / 1000L;
	public static String ircd_bantype = "ip";
	public static boolean ircd_convertcolorcodes = true;
	public static boolean ircd_handleampersandcolors = true;
	public static boolean ircd_enablenotices = true;
	public static String ircd_operuser = "";
	public static String ircd_operpass = "";
	public static String ircd_opermodes = "~&@%+";
	public static String ircd_consolechannel = "#staff";
	public static String ircd_irc_colors = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15";
	public static String ircd_game_colors = "0,f,1,2,c,4,5,6,e,a,3,b,9,d,8,7";
	public static boolean ircd_color_death_messages = false;
	public static boolean ircd_color_say_messages = false;
	public static boolean ircd_broadcast_death_messages = true;
	public static boolean debugmode = false;
	public static boolean ircd_strip_ingame_suffix = true;
	
	public static String link_remotehost = "localhost";
	public static int link_remoteport = 7000;
	public static int link_localport = 7000;
	public static boolean link_autoconnect = true;
	public static String link_name = "irc.localhost";
	public static String link_connectpassword = "test";
	public static String link_receivepassword = "test";
	public static int link_pinginterval = 60;
	public static int link_timeout = 180;
	public static int link_delay = 60;
	public static int link_serverid = new Random().nextInt(900) + 100;

	public static List<String> kickCommands = Arrays.asList("/kick");
	
	public static boolean enableRawSend = false;
	
	static IRCd ircd = null;
	private Thread thr = null;
	
	public static FileConfiguration config;
	
	public void saveSettings() {
		File configFile = new File(getDataFolder(), "config.yml");
		writeSettings(configFile);
	}
	
	public void reloadingConfig() {
		reloadConfig();
		config = getConfig();
		// IF config file doesn't exist, create it
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Creating default configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin183." : ""));
			config.options().copyDefaults(true);
		}
		loadSettings();
	}
	
	public void initConfig() {
		IRCd.redundantModes = ircd_redundant_modes;
		IRCd.port = ircd_port;
		IRCd.maxConnections = ircd_maxconn;
		IRCd.pingInterval = ircd_pinginterval;
		IRCd.timeoutInterval = ircd_timeout;
		IRCd.nickLen = ircd_maxnicklen;
		IRCd.channelName = ircd_channel;
		//if (!reload) {
		//	IRCd.channelTopic = ircd_topic;
		//	IRCd.channelTopicSet = ircd_topicsetby;
		//	IRCd.channelTopicSetDate = ircd_topicsetdate / 1000L;
		//}
		IRCd.stripIngameSuffix = ircd_strip_ingame_suffix;
		IRCd.serverName = ircd_servername;
		IRCd.serverDescription = ircd_serverdescription;
		IRCd.serverHostName = ircd_serverhostname;
		IRCd.serverCreationDate = ircd_creationdate;
		IRCd.ingameSuffix = ircd_ingamesuffix;
		IRCd.enableNotices = ircd_enablenotices;
		IRCd.convertColorCodes = ircd_convertcolorcodes;
		IRCd.handleAmpersandColors = ircd_handleampersandcolors;
		IRCd.ircBanType = ircd_bantype;
		IRCd.version = BukkitIRCdPlugin.ircd_version;
		IRCd.operUser = ircd_operuser;
		IRCd.operPass = ircd_operpass;
		IRCd.operModes = ircd_opermodes;
		IRCd.consoleChannelName = ircd_consolechannel;
		ircd.modestr = mode;
		IRCd.debugMode = debugmode;
		IRCd.gameColors = ircd_game_colors.split(",");
		IRCd.ircColors = BukkitIRCdPlugin.convertStringArrayToIntArray(ircd_irc_colors.split(","), IRCd.ircColors);
		IRCd.broadcastDeathMessages = ircd_broadcast_death_messages;
		IRCd.colorDeathMessages = ircd_color_death_messages;
		IRCd.colorSayMessages = ircd_color_say_messages;
		// Linking specific settings
		IRCd.remoteHost = link_remotehost;
		IRCd.remotePort = link_remoteport;
		IRCd.localPort = link_localport;
		IRCd.autoConnect = link_autoconnect;
		IRCd.linkName = link_name;
		IRCd.connectPassword = link_connectpassword;
		IRCd.receivePassword = link_receivepassword;
		IRCd.linkPingInterval = link_pinginterval;
		IRCd.linkTimeoutInterval = link_timeout;
		IRCd.linkDelay = link_delay;
		IRCd.SID = link_serverid;
	}
	private void writeSettings(File configFile)
	{
		try
		{
			saveConfig();
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Saved configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin742." : ""));
		}
		catch(Exception e)
		{
			BukkitIRCdPlugin.log.warning("[BukkitIRCd] Caught exception while writing settings to file: ");
			e.printStackTrace();
		}
	}
	
	public static void loadSettings() {
		try {
			ircd_redundant_modes = config.getBoolean("redundant-modes",ircd_redundant_modes);
			ircd_strip_ingame_suffix = config.getBoolean("strip-ingame-suffix",ircd_strip_ingame_suffix);
			ircd_color_death_messages = config.getBoolean("color-death-messages", ircd_color_death_messages);
			ircd_color_say_messages = config.getBoolean("color-say-messages", ircd_color_say_messages);
			BukkitIRCdPlugin.mode = config.getString("mode", BukkitIRCdPlugin.mode);
			ircd_ingamesuffix = config.getString("ingame-suffix", ircd_ingamesuffix);
			ircd_enablenotices = config.getBoolean("enable-notices", ircd_enablenotices);
			ircd_convertcolorcodes = config.getBoolean("convert-color-codes", ircd_convertcolorcodes);
			ircd_handleampersandcolors = config.getBoolean("handle-ampersand-colors",ircd_handleampersandcolors);
			ircd_irc_colors = config.getString("irc-colors", ircd_irc_colors);
			ircd_game_colors = config.getString("game-colors", ircd_game_colors);
			ircd_channel = config.getString("channel-name", ircd_channel);
			ircd_consolechannel = config.getString("console-channel-name", ircd_consolechannel);
			BukkitIRCdPlugin.ircd_creationdate = config.getString("server-creation-date", BukkitIRCdPlugin.ircd_creationdate);
			ircd_servername = config.getString("server-name", ircd_servername);
			ircd_serverdescription = config.getString("server-description", ircd_serverdescription);
			ircd_serverhostname = config.getString("server-host", ircd_serverhostname);
			ircd_bantype = config.getString("ban-type", ircd_bantype);
			debugmode = config.getBoolean("debug-mode", debugmode);
			enableRawSend = config.getBoolean("enable-raw-send", enableRawSend);
			kickCommands = config.getStringList("kick-commands");

			String operpass = "";
			
			ircd_port = config.getInt("standalone.port", ircd_port);
			ircd_maxconn = config.getInt("standalone.max-connections", ircd_maxconn);
			ircd_pinginterval = config.getInt("standalone.ping-interval", ircd_pinginterval);
			ircd_timeout = config.getInt("standalone.timeout", ircd_timeout);
			ircd_maxnicklen = config.getInt("standalone.max-nick-length", ircd_maxnicklen);
			ircd_operuser = config.getString("standalone.oper-username", ircd_operuser);
			operpass = config.getString("standalone.oper-password", ircd_operpass);
			ircd_opermodes = config.getString("standalone.oper-modes", ircd_opermodes);
			ircd_topic = config.getString("standalone.channel-topic", ircd_topic).replace("^K", (char)3 + "").replace("^B", (char)2 + "").replace("^I", (char)29 + "").replace("^O", (char)15 + "").replace("^U", (char)31 + "");
			ircd_topicsetby = config.getString("standalone.channel-topic-set-by", ircd_topicsetby);
			ircd_broadcast_death_messages = config.getBoolean("broadcast-death-messages",ircd_broadcast_death_messages);
			try {
				ircd_topicsetdate = BukkitIRCdPlugin.dateFormat.parse(config.getString("standalone.channel-topic-set-date", BukkitIRCdPlugin.dateFormat.format(ircd_topicsetdate))).getTime();
			}
			catch (ParseException e) { }

			link_remotehost = config.getString("inspircd.remote-host", link_remotehost);
			link_remoteport = config.getInt("inspircd.remote-port", link_remoteport);
			link_localport = config.getInt("inspircd.local-port", link_localport);
			link_autoconnect = config.getBoolean("inspircd.auto-connect", link_autoconnect);
			link_name = config.getString("inspircd.link-name", link_name);
			link_connectpassword = config.getString("inspircd.connect-password", link_connectpassword);
			link_receivepassword = config.getString("inspircd.receive-password", link_receivepassword);
			link_pinginterval = config.getInt("inspircd.ping-interval", link_pinginterval);
			link_timeout = config.getInt("inspircd.timeout", link_timeout);
			link_delay = config.getInt("inspircd.connect-delay", link_delay);
			link_serverid = config.getInt("inspircd.server-id", link_serverid);

			if (operpass.length() == 0) ircd_operpass = "";
			else if (operpass.startsWith("~")) { ircd_operpass = operpass.substring(1); }
			else { ircd_operpass = Hash.compute(operpass, HashType.SHA_512); }

			BukkitIRCdPlugin.log.info("[BukkitIRCd] Loaded configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin363." : ""));
			
			//saveConfig();
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Saved initial configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin365." : ""));
		}
		catch (Exception e) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load configuration file: " + e.toString());
		}
		
	}
	
	private void firstRunSettings(File dataFolder)
	{
		BukkitIRCdPlugin.log.info("[BukkitIRCd] Configuration file not found, creating new one." + (IRCd.debugMode ? " Code BukkitIRCdPlugin475." : ""));
		dataFolder.mkdirs();

		File configFile = new File(dataFolder, "config.yml");
		try
		{
			if(!configFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			BukkitIRCdPlugin.log.warning("[BukkitIRCd] Could not create config file!" + (IRCd.debugMode ? " Error Code BukkitIRCdPlugin486." : ""));
		}

		writeSettings(configFile);
	}
	
	
	
	
}