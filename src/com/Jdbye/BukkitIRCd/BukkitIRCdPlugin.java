package com.Jdbye.BukkitIRCd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dynmap.DynmapAPI;

import com.Jdbye.BukkitIRCd.commands.*;
/**
 * BukkitIRCdPlugin for Bukkit
 *
 * @author Jdbye
 */
 
public class BukkitIRCdPlugin extends JavaPlugin {
	static class CriticalSection extends Object {
	}
	static public CriticalSection csLastReceived = new CriticalSection();
	
	private final BukkitIRCdPlayerListener playerListener = new BukkitIRCdPlayerListener(this);
	private final BukkitIRCdServerListener serverListener = new BukkitIRCdServerListener(this);
	private BukkitIRCdDynmapListener dynmapListener = null;
	
	private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();

	private String mode = "standalone";

	public static BukkitIRCdPlugin thePlugin = null;

	private static Date curDate = new Date();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	public static String ircd_creationdate = dateFormat.format(curDate);

	public Map<String, String> lastReceived = new HashMap<String, String>();

	boolean ircd_redundant_modes = false;
	private int ircd_port = 6667;
	private int ircd_maxconn = 1000;
	private int ircd_pinginterval = 45;
	private int ircd_timeout = 180;
	private int ircd_maxnicklen = 32;
	private String ircd_channel = "#minecraft";
	private static String ircd_servername = "BukkitIRCd";
	private String ircd_serverdescription = "Minecraft BukkitIRCd Server";
	private String ircd_serverhostname = "bukkitircd.localhost";
	private String ircd_ingamesuffix = "/minecraft";
	public static String ircd_topic = "Welcome to a Bukkit server!";
	public static String ircd_topicsetby = ircd_servername;
	public static long ircd_topicsetdate = System.currentTimeMillis() / 1000L;
	public static String ircd_bantype = "ip";
	private boolean ircd_convertcolorcodes = true;
	private static boolean ircd_handleampersandcolors = true;
	private static String ircd_version;
	private static boolean ircd_enablenotices = true;
	private String ircd_operuser = "";
	private String ircd_operpass = "";
	private String ircd_opermodes = "~&@%+";
	private String ircd_consolechannel = "#staff";
	private String ircd_irc_colors = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15";
	private String ircd_game_colors = "0,f,1,2,c,4,5,6,e,a,3,b,9,d,8,7";
	private boolean ircd_color_death_messages = false;
	private boolean ircd_color_say_messages = false;
	private boolean ircd_broadcast_death_messages = true;
	public static boolean debugmode = false;
	public boolean dynmapEventRegistered = false;
	private boolean ircd_strip_ingame_suffix = true;
	
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
	public static final Logger log = Logger.getLogger("Minecraft");
	
	public boolean enableRawSend = false;

//	public static PermissionHandler permissionHandler = null;
	public static DynmapAPI dynmap = null;
	FileConfiguration config,messages;

	static IRCd ircd = null;
	private Thread thr = null;

	public BukkitIRCdPlugin() {
		thePlugin = this;
	}

	public void onEnable() {
		// Register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);
		pm.registerEvents(this.serverListener, this);
		
		PluginDescriptionFile pdfFile = getDescription();
		ircd_version = pdfFile.getName() + " " + pdfFile.getVersion() + " by " + pdfFile.getAuthors().get(0);
		setupMetrics();
		pluginInit();

		getCommand("ircban").setExecutor(new IRCBanCommand(this));
		getCommand("irckick").setExecutor(new IRCKickCommand(this));
		getCommand("irclist").setExecutor(new IRCListCommand(this));
		getCommand("ircunban").setExecutor(new IRCUnbanCommand(this));
		getCommand("ircwhois").setExecutor(new IRCWhoisCommand(this));
		getCommand("ircmsg").setExecutor(new IRCMsgCommand(this));
		getCommand("ircreply").setExecutor(new IRCReplyCommand(this));
		getCommand("irctopic").setExecutor(new IRCTopicCommand(this));
		getCommand("irclink").setExecutor(new IRCLinkCommand(this));
		getCommand("ircreload").setExecutor(new IRCReloadCommand(this));
		getCommand("rawsend").setExecutor(new RawsendCommand(this));
		
		log.info(ircd_version + " is enabled!");
		
	}
	
	public void onDisable() {
		if (ircd != null) {
			ircd.running = false;
			IRCd.disconnectAll();
			ircd = null; 
		}
		if (thr != null) {
			thr.interrupt();
			thr = null;
		}
		dynmapEventRegistered = false;

		File configFile = new File(getDataFolder(), "config.yml");
		writeSettings(configFile);

		writeBans();

		log.info(ircd_version + " is disabled!");
	}
	public boolean isDebugging(final Player player) {
		if (debugees.containsKey(player)) {
			return debugees.get(player);
		} else {
			return false;
		}
	}
	
	private void pluginInit() {
		pluginInit(false);
	}
	
	public void pluginInit(boolean reload) {
		if (reload) {
			if (ircd != null) {
				ircd.running = false;
				IRCd.disconnectAll("Reloading configuration.");
				ircd = null; 
			}
			if (thr != null) {
				thr.interrupt();
				thr = null;
			}
		}
		
		reloadConfig();
		config = getConfig();
		// Create default config.yml if it doesn't exist.
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			log.info("[BukkitIRCd] Creating default configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin183." : ""));
		}
		config.options().copyDefaults(true);
		loadSettings();
		
		// Create default messages.yml if it doesn't exist.
		File messagesFile = new File(getDataFolder(), "messages.yml");
		messages = YamlConfiguration.loadConfiguration(messagesFile);
		if (!(messagesFile.exists())) {
			log.info("[BukkitIRCd] Creating default messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin192." : ""));
			messages.options().copyDefaults(true);
			saveDefaultMessages(getDataFolder(),"messages.yml");
			log.info("[BukkitIRCd] Saving initial messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin194." : ""));
			
		}
		messages.options().copyDefaults(true);
		

		if (!(new File(getDataFolder(), "motd.txt")).exists()) {
			saveDefaultMOTD(getDataFolder(),"motd.txt");
			log.info("[BukkitIRCd] Default MOTD file created." + (IRCd.debugMode ? " Code BukkitIRCdPlugin199." : ""));
		}
		loadMOTD();

		if (!(new File(getDataFolder(), "bans.txt")).exists()) {
			if (writeBans()) log.info("[BukkitIRCd] Blank bans file created." + (IRCd.debugMode ? " Code BukkitIRCdPlugin204." : ""));
			else log.warning("[BukkitIRCd] Failed to create bans file." + (IRCd.debugMode ? " Error Code BukkitIRCdPlugin205." : ""));
		}

		setupDynmap();
		
		ircd = new IRCd();
		
		loadMessages(ircd);
		IRCd.redundantModes = ircd_redundant_modes;
		IRCd.port = ircd_port;
		IRCd.maxConnections = ircd_maxconn;
		IRCd.pingInterval = ircd_pinginterval;
		IRCd.timeoutInterval = ircd_timeout;
		IRCd.nickLen = ircd_maxnicklen;
		IRCd.channelName = ircd_channel;
		if (!reload) {
			IRCd.channelTopic = ircd_topic;
			IRCd.channelTopicSet = ircd_topicsetby;
			IRCd.channelTopicSetDate = ircd_topicsetdate / 1000L;
		}
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
		IRCd.version = ircd_version;
		IRCd.operUser = ircd_operuser;
		IRCd.operPass = ircd_operpass;
		IRCd.operModes = ircd_opermodes;
		IRCd.consoleChannelName = ircd_consolechannel;
		ircd.modestr = mode;
		IRCd.debugMode = debugmode;
		IRCd.gameColors = ircd_game_colors.split(",");
		IRCd.ircColors = convertStringArrayToIntArray(ircd_irc_colors.split(","), IRCd.ircColors);
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

		loadBans();
		IRCd.bukkitPlayers.clear();
		
		// Set players to different IRC modes based on permission
		for (Player player : getServer().getOnlinePlayers()) {
			StringBuffer mode = new StringBuffer();
            if (player.hasPermission("bukkitircd.mode.owner")){
            	if (IRCd.debugMode) {
            		log.info("Add mode +q for " + player.getName());
            	}
            	mode.append("~");
            }
            else if (player.hasPermission("bukkitircd.mode.protect")){
            	if (IRCd.debugMode) {
            		log.info("Add mode +a for " + player.getName());
            	}
            	mode.append("&");
            }
            else if (player.hasPermission("bukkitircd.mode.op")){
            	if (IRCd.debugMode) {
            		log.info("Add mode +o for " + player.getName());
            	}
            	mode.append("@");
            }
            else if (player.hasPermission("bukkitircd.mode.halfop")){
            	if (IRCd.debugMode) {
            		log.info("Add mode +h for " + player.getName());
            	}
            	mode.append("%");
            }
            else if (player.hasPermission("bukkitircd.mode.voice")){
            	if (IRCd.debugMode) {
            		log.info("Add mode +v for " + player.getName());
            	}
            	mode.append("+");
            }

			IRCd.addBukkitUser(mode.toString(),player);
		}

		thr = new Thread(ircd);
		thr.start();

	}

	public void setDebugging(final Player player, final boolean value) {
		debugees.put(player, value);
	}
	
	
	// check for Dynmap, and if it's installed, register events and hooks
	private void setupDynmap() {
		PluginManager pm = getServer().getPluginManager();
		Plugin plugin = pm.getPlugin("dynmap");
		if (BukkitIRCdPlugin.dynmap == null) {
			if (plugin != null) {
				if (dynmapListener == null) dynmapListener = new BukkitIRCdDynmapListener(this);
				if (!dynmapEventRegistered) { 
					pm.registerEvents(this.dynmapListener, this);
				}
				setupDynmap((DynmapAPI)plugin);
			}
		}
	}
	
	public void setupDynmap(DynmapAPI plugin) {
		if (plugin != null) {
			dynmap = plugin;
			log.info("[BukkitIRCd] Hooked into Dynmap." + (IRCd.debugMode ? " Code BukkitIRCdPlugin301." : ""));
		}
	}

	public void unloadDynmap() {
		if (BukkitIRCdPlugin.dynmap != null) {
			BukkitIRCdPlugin.dynmap = null;
			log.info("[BukkitIRCd] Dynmap plugin lost." + (IRCd.debugMode ? " Error Code BukkitIRCdPlugin308." : ""));
		}
	}

	private void loadSettings() {
		try {
			ircd_redundant_modes = config.getBoolean("redundant-modes",ircd_redundant_modes);
			ircd_strip_ingame_suffix = config.getBoolean("strip-ingame-suffix",ircd_strip_ingame_suffix);
			ircd_color_death_messages = config.getBoolean("color-death-messages", ircd_color_death_messages);
			ircd_color_say_messages = config.getBoolean("color-say-messages", ircd_color_say_messages);
			mode = config.getString("mode", mode);
			ircd_ingamesuffix = config.getString("ingame-suffix", ircd_ingamesuffix);
			ircd_enablenotices = config.getBoolean("enable-notices", ircd_enablenotices);
			ircd_convertcolorcodes = config.getBoolean("convert-color-codes", ircd_convertcolorcodes);
			ircd_handleampersandcolors = config.getBoolean("handle-ampersand-colors",ircd_handleampersandcolors);
			ircd_irc_colors = config.getString("irc-colors", ircd_irc_colors);
			ircd_game_colors = config.getString("game-colors", ircd_game_colors);
			ircd_channel = config.getString("channel-name", ircd_channel);
			ircd_consolechannel = config.getString("console-channel-name", ircd_consolechannel);
			ircd_creationdate = config.getString("server-creation-date", ircd_creationdate);
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
				ircd_topicsetdate = dateFormat.parse(config.getString("standalone.channel-topic-set-date", dateFormat.format(ircd_topicsetdate))).getTime();
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

			log.info("[BukkitIRCd] Loaded configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin363." : ""));
			
			saveConfig();
			log.info("[BukkitIRCd] Saved initial configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin365." : ""));
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load configuration file: " + e.toString());
		}
	}

	
	/**
     * Converts color codes to processed codes
     *
     * @param message Message with raw color codes
     * @return String with processed colors
	 * Thanks to Jkcclemens (of RoyalDev) for this code
     */
    public String colorize(final String message) {
        if (message == null) return null;
        return message.replaceAll("&([a-f0-9k-or])", "\u00a7$1");
    }
	
	// Load the messages from messages.yml
	private void loadMessages(IRCd ircd) {
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
			
			IRCd.msgSendQueryFromIngame = colorize(IRCd.msgSendQueryFromIngame);
			IRCd.msgLinked = colorize(IRCd.msgLinked);
			IRCd.msgDelinked = colorize(IRCd.msgDelinked);
			IRCd.msgDelinkedReason = colorize(IRCd.msgDelinked);
			IRCd.msgIRCJoin = colorize(IRCd.msgIRCJoin);
			IRCd.msgIRCJoinDynmap = colorize(IRCd.msgIRCJoinDynmap);
			IRCd.msgIRCLeave = colorize(IRCd.msgIRCLeave);
			IRCd.msgIRCLeaveReason = colorize(IRCd.msgIRCLeaveReason);
			IRCd.msgIRCLeaveDynmap = colorize(IRCd.msgIRCLeaveDynmap);
			IRCd.msgIRCLeaveReasonDynmap = colorize(IRCd.msgIRCLeaveReasonDynmap);
			IRCd.msgIRCKick = colorize(IRCd.msgIRCKick);
			IRCd.msgIRCKickReason = colorize(IRCd.msgIRCKickReason);
			IRCd.msgIRCKickDynmap = colorize(IRCd.msgIRCKickDynmap);
			IRCd.msgIRCKickReasonDynmap = colorize(IRCd.msgIRCKickReasonDynmap);
			IRCd.msgIRCBan = colorize(IRCd.msgIRCBan);
			IRCd.msgIRCBanDynmap = colorize(IRCd.msgIRCBanDynmap);
			IRCd.msgIRCUnban = colorize(IRCd.msgIRCUnban);
			IRCd.msgIRCUnbanDynmap = colorize(IRCd.msgIRCUnbanDynmap);
			IRCd.msgIRCNickChange = colorize(IRCd.msgIRCNickChange);
			IRCd.msgIRCNickChangeDynmap = colorize(IRCd.msgIRCNickChangeDynmap);
			IRCd.msgIRCAction = colorize(IRCd.msgIRCAction);
			IRCd.msgIRCMessage = colorize(IRCd.msgIRCMessage);
			IRCd.msgIRCNotice = colorize(IRCd.msgIRCNotice);
			IRCd.msgIRCPrivateAction = colorize(IRCd.msgIRCPrivateAction);
			IRCd.msgIRCPrivateMessage = colorize(IRCd.msgIRCPrivateMessage);
			IRCd.msgIRCPrivateNotice = colorize(IRCd.msgIRCPrivateNotice);
			IRCd.msgIRCActionDynmap = colorize(IRCd.msgIRCActionDynmap);
			IRCd.msgIRCMessageDynmap = colorize(IRCd.msgIRCMessageDynmap);
			IRCd.msgIRCNoticeDynmap = colorize(IRCd.msgIRCNoticeDynmap);
			IRCd.msgDynmapMessage = colorize(IRCd.msgDynmapMessage);
			IRCd.msgPlayerList = colorize(IRCd.msgPlayerList);

			log.info("[BukkitIRCd] Loaded messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin464." : ""));
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load messages file: " + e.toString());
		}
	}

	// It was originally disabled, I'd like to know why.
	@SuppressWarnings("unused")
	private void firstRunSettings(File dataFolder)
	{
		log.info("[BukkitIRCd] Configuration file not found, creating new one." + (IRCd.debugMode ? " Code BukkitIRCdPlugin475." : ""));
		dataFolder.mkdirs();

		File configFile = new File(dataFolder, "config.yml");
		try
		{
			if(!configFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create config file!" + (IRCd.debugMode ? " Error Code BukkitIRCdPlugin486." : ""));
		}

		writeSettings(configFile);
	}
	
	// Set up the MOTD for the standalone BukkitIRCd server
	private void loadMOTD() {
		File motdFile = new File(getDataFolder(), "motd.txt");

		IRCd.MOTD.clear();

		try {
			// use buffering, reading one line at a time
			// FileReader always assumes default encoding is OK!
			BufferedReader input =  new BufferedReader(new FileReader(motdFile));
			try {
				String line = null; // not declared within while loop
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				while (( line = input.readLine()) != null){
					IRCd.MOTD.add(line);
				}
			}
			finally {
				input.close();
				log.info("[BukkitIRCd] Loaded MOTD file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin516." : ""));
			}
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load MOTD file: " + e.toString());
		}
	}

	// Load the bans file
	private void loadBans() {
		File bansFile = new File(getDataFolder(), "bans.txt");

		IRCd.ircBans.clear();

		try {
			// use buffering, reading one line at a time
			// FileReader always assumes default encoding is OK!
			BufferedReader input =  new BufferedReader(new FileReader(bansFile));
			try {
				String line = null; //not declared within while loop
				/*
				 * readLine is a bit quirky :
				 * it returns the content of a line MINUS the newline.
				 * it returns null only for the END of the stream.
				 * it returns an empty String if two newlines appear in a row.
				 */
				while (( line = input.readLine()) != null){
					String[] split = line.split(",");
					if (!line.trim().startsWith("#")) {
						try { IRCd.ircBans.add(new IrcBan(split[0], split[1], Long.parseLong(split[2]))); }
						catch (NumberFormatException e) { log.warning("[BukkitIRCd] Invalid ban: " + line); }
					}
				}
			}
			finally {
				input.close();
				log.info("[BukkitIRCd] Loaded bans file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin551." : ""));
			}
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load bans file: " + e.toString());
		}
	}

	// Write the bans file
	private boolean writeBans()
	{
		File bansFile = new File(getDataFolder(), "bans.txt");

		boolean result = false;
		OutputStreamWriter fileWriter = null;
		BufferedWriter bufferWriter = null;
		try
		{
			if(!bansFile.exists())
				bansFile.createNewFile();

			fileWriter = new OutputStreamWriter(new FileOutputStream(bansFile), "UTF8");
			bufferWriter = new BufferedWriter(fileWriter);

			bufferWriter.append("# wildcard hostmask,banned by,time");
			bufferWriter.newLine();

			synchronized(IRCd.csIrcBans) {
				for (IrcBan ban : IRCd.ircBans) {
					bufferWriter.append(ban.fullHost + "," + ban.bannedBy + "," + ban.banTime);
					bufferWriter.newLine();
				}
			}

			bufferWriter.flush();
			log.info("[BukkitIRCd] Saved bans file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin585." : ""));
			result = true;
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Caught exception while writing bans to file: ");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(bufferWriter != null)
				{
					bufferWriter.flush();
					bufferWriter.close();
				}

				if(fileWriter != null)
					fileWriter.close();
			}
			catch(IOException e)
			{
				log.warning("[BukkitIRCd] IO Exception writing file: " + bansFile.getName());
			}
		}
		return result;
	}

	// If a motd is not found, save it
	private void saveDefaultMOTD(File dataFolder, String fileName)
	{
		log.info("[BukkitIRCd] MOTD file not found, creating new one." + (IRCd.debugMode ? " Code BukkitIRCdPlugin616." : ""));
		dataFolder.mkdirs();

		File motdFile = new File(dataFolder, fileName);
		try
		{
			if(!motdFile.createNewFile())
				throw new IOException("Failed file creation.");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create MOTD file!" + (IRCd.debugMode ? " Code BukkitIRCdPlugin627." : ""));
		}

		writeMOTD(motdFile);
	}

	private void writeMOTD(File motdFile)
	{
		OutputStreamWriter fileWriter = null;
		BufferedWriter bufferWriter = null;
		try
		{
			if(!motdFile.exists())
				motdFile.createNewFile();

			fileWriter = new OutputStreamWriter(new FileOutputStream(motdFile), "UTF8");
			bufferWriter = new BufferedWriter(fileWriter);
			// E M d H:m:s
			Date curDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss");

			bufferWriter.append("Last changed on " + dateFormat.format(curDate));
			bufferWriter.newLine();
			bufferWriter.append("");
			bufferWriter.newLine();
			bufferWriter.append("_________        __    __   .__        ___________  _____     _"); 
			bufferWriter.newLine();
			bufferWriter.append("\\______  \\___ __|  |  |  |  |__| __   |_   _| ___ \\/  __ \\   | |");
			bufferWriter.newLine();
			bufferWriter.append(" |   |_\\  \\  |  |  | _|  | _____/  |_   | | | |_/ /| /  \\/ __| |");
			bufferWriter.newLine();
			bufferWriter.append(" |    __ _/  |  \\  |/ /  |/ /  \\   __\\  | | |    / | |    / _` |");
			bufferWriter.newLine();
			bufferWriter.append(" |   |_/  \\  |  /    <|    <|  ||  |   _| |_| |\\ \\ | \\__/\\ (_| |");
			bufferWriter.newLine();
			bufferWriter.append(" |______  /____/|__|_ \\__|_ \\__||__|   \\___/\\_| \\_| \\____/\\__,_|");
			bufferWriter.newLine();
			bufferWriter.append("        \\/           \\/    \\/");
			bufferWriter.newLine();
			bufferWriter.append("");
			bufferWriter.newLine();
			bufferWriter.append("Welcome to " + ircd_servername + ", running " + ircd_version + ".");
			bufferWriter.newLine();
			bufferWriter.append("Enjoy your stay!");
			bufferWriter.newLine();

			bufferWriter.flush();
			log.info("[BukkitIRCd] Saved MOTD file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin674" : ""));
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Caught exception while writing MOTD to file: ");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(bufferWriter != null)
				{
					bufferWriter.flush();
					bufferWriter.close();
				}

				if(fileWriter != null)
					fileWriter.close();
			}
			catch(IOException e)
			{
				log.warning("[BukkitIRCd] IO Exception writing file: " + motdFile.getName());
			}
		}
	}
	
	// Was also disabled, I'd like to know why
	private void saveDefaultMessages(File dataFolder, String fileName)
	{
		log.info("[BukkitIRCd] Messages file not found, creating new one." + (IRCd.debugMode ? " Code BukkitIRCdPlugin705" : ""));
		dataFolder.mkdirs();

		File msgFile = new File(dataFolder, fileName);
		try
		{
			if(!msgFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create messages file!" + (IRCd.debugMode ? " Error code BukkitIRCdPlugin716." : ""));
		}

		writeMessages(msgFile);
	}
	
	
	private void writeMessages(File messagesFile)
	{
		try
		{
			messages.save(messagesFile);
			log.info("[BukkitIRCd] Saved messages file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin728." : ""));
		}
		catch(Exception e)
		{
			log.warning("[BukkitIRCd] Caught exception while writing messages to file: ");
			e.printStackTrace();
		}
	}

	private void writeSettings(File configFile)
	{
		try
		{
			saveConfig();
			log.info("[BukkitIRCd] Saved configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin742." : ""));
		}
		catch(Exception e)
		{
			log.warning("[BukkitIRCd] Caught exception while writing settings to file: ");
			e.printStackTrace();
		}
	}


	public boolean hasPermission(Player player, String permission)
	{
		if (player.hasPermission(permission)) {
			//log.info("[BukkitIRCd] "+player.getName()+" has permission "+permission+" (Superperms)");
			return true;
		}
		//else try {
		//	if (permissionHandler != null) {
		//		if (this.permissionHandler.has(player, permission)) {
		//			//log.info("[BukkitIRCd] "+player.getName()+" has permission "+permission+" (Permissions 2.x)");
		//			return true;
		//		}
		//	}
		//} catch (Exception e) { }
		return false;
	}

	public void setLastReceived(String receivedBy, String receivedFrom)
	{
		synchronized(csLastReceived) {
			lastReceived.put(receivedBy, receivedFrom);
		}
	}

	public void updateLastReceived(String oldReceivedFrom, String newReceivedFrom)
	{
		List<String> update = new ArrayList<String>();
		synchronized(csLastReceived) {
			for (Map.Entry<String, String> lastReceivedEntry : lastReceived.entrySet()) { 
				if (lastReceivedEntry.getValue().equalsIgnoreCase(oldReceivedFrom)) update.add(lastReceivedEntry.getKey());
			}
			for (String toUpdate : update) lastReceived.put(toUpdate, newReceivedFrom);
		}
	}

	public void removeLastReceivedBy(String receivedBy)
	{
		synchronized(csLastReceived) {
			lastReceived.remove(receivedBy);
		}
	}

	public void removeLastReceivedFrom(String receivedFrom)
	{
		List<String> remove = new ArrayList<String>();
		synchronized(csLastReceived) {
			for (Map.Entry<String, String> lastReceivedEntry : lastReceived.entrySet()) {
				if (lastReceivedEntry.getValue().equalsIgnoreCase(receivedFrom)) remove.add(lastReceivedEntry.getKey());
			}
			for (String toRemove : remove) lastReceived.remove(toRemove);
		}
	}

	public int countStr(String text, String search)
	{
		int count = 0;
		for (int fromIndex = 0; fromIndex > -1; count++)
			fromIndex = text.indexOf(search, fromIndex + ((count > 0) ? 1 : 0));
		return count - 1;
	}
	
	public int[] convertStringArrayToIntArray(String[] sarray, int[] def) {
		try {
			if (sarray != null) {
				int intarray[] = new int[sarray.length];
				for (int i = 0; i < sarray.length; i++) {
					intarray[i] = Integer.parseInt(sarray[i]);
				}
				return intarray;
			}
		} catch (Exception e) { log.severe("[BukkitIRCd] Unable to parse string array " + IRCd.join(sarray, " ", 0) + ", invalid number. " + e); }
		return def;
	}
	
	/**
	 * Setup PluginMetrics
	 */
	private void setupMetrics(){
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
		    // Failed to submit metrics
		}
	}
}

