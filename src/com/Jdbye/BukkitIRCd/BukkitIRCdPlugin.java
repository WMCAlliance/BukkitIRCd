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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dynmap.DynmapAPI;
import org.bukkit.ChatColor;

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

	private Map<String, String> lastReceived = new HashMap<String, String>();

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
	private static String ircd_version;
	private static boolean ircd_enablenotices = true;
	private String ircd_operuser = "";
	private String ircd_operpass = "";
	private String ircd_opermodes = "~&@%+";
	private String ircd_consolechannel = "#staff";
	private String ircd_irc_colors = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15";
	private String ircd_game_colors = "0,f,1,2,c,4,5,6,e,a,3,b,9,d,8,7";
	public static boolean debugmode = false;
	public boolean dynmapEventRegistered = false;
	
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

	public static final Logger log = Logger.getLogger("Minecraft");
	
	private boolean enableRawSend = false;

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
		/*
		pm.registerEvent(Event.Type.PLAYER_JOIN, this.playerListener, org.bukkit.event.Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, this.playerListener, org.bukkit.event.Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_KICK, this.playerListener, org.bukkit.event.Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_CHAT, this.playerListener, org.bukkit.event.Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, this.playerListener, org.bukkit.event.Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_MOVE, this.playerListener, org.bukkit.event.Event.Priority.Monitor, this);
		
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, this.serverListener, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, this.serverListener, Event.Priority.Monitor, this);
		*/
		
		PluginDescriptionFile pdfFile = getDescription();
		ircd_version = pdfFile.getName() + " " + pdfFile.getVersion() + " by " + pdfFile.getAuthors().get(0);

		pluginInit();

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
	
	private void pluginInit(boolean reload) {
		if (reload) {
			if (ircd != null) {
				ircd.running = false;
				IRCd.disconnectAll("Reloading configuration");
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
			log.info("[BukkitIRCd] Creating default configuration file. Code BukkitIRCdPlugin183.");
		}
		config.options().copyDefaults(true);
		loadSettings();
		
		// Create default messages.yml if it doesn't exist.
		File messagesFile = new File(getDataFolder(), "messages.yml");
		messages = YamlConfiguration.loadConfiguration(messagesFile);
		if (!(messagesFile.exists())) {
			log.info("[BukkitIRCd] Creating default messages file. Code BukkitIRCdPlugin192.");
		}
		messages.options().copyDefaults(true);
		

		if (!(new File(getDataFolder(), "motd.txt")).exists()) {
			saveDefaultMOTD(getDataFolder(),"motd.txt");
			log.info("[BukkitIRCd] Default MOTD file created. Code BukkitIRCdPlugin199.");
		}
		loadMOTD();

		if (!(new File(getDataFolder(), "bans.txt")).exists()) {
			if (writeBans()) log.info("[BukkitIRCd] Blank bans file created. Code BukkitIRCdPlugin204.");
			else log.warning("[BukkitIRCd] Failed to create bans file. Error Code BukkitIRCdPlugin205.");
		}

		setupDynmap();
		
		ircd = new IRCd();
		
		loadMessages(ircd);
		
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
		IRCd.serverName = ircd_servername;
		IRCd.serverDescription = ircd_serverdescription;
		IRCd.serverHostName = ircd_serverhostname;
		IRCd.serverCreationDate = ircd_creationdate;
		IRCd.ingameSuffix = ircd_ingamesuffix;
		IRCd.enableNotices = ircd_enablenotices;
		IRCd.convertColorCodes = ircd_convertcolorcodes;
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
		for (Player player : getServer().getOnlinePlayers()) {
			String mode = "";
			if (hasPermission(player, "bukkitircd.mode.owner")) mode += "~";
			if (hasPermission(player, "bukkitircd.mode.protect")) mode += "&";
			if (hasPermission(player, "bukkitircd.mode.op")) mode += "@";
			if (hasPermission(player, "bukkitircd.mode.halfop")) mode += "%";
			if (hasPermission(player, "bukkitircd.mode.voice")) mode += "+";
			IRCd.addBukkitUser(mode,player);
		}

		thr = new Thread(ircd);
		thr.start();

	}

	public void setDebugging(final Player player, final boolean value) {
		debugees.put(player, value);
	}
	
//	public void unloadPermissions() {
//		if (this.permissionHandler != null) {
//			this.permissionHandler = null;
//			log.info("[BukkitIRCd] Permissions plugin lost.");
//		}
//	}
	
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
			log.info("[BukkitIRCd] Hooked into Dynmap. Code BukkitIRCdPlugin301.");
		}
	}

	public void unloadDynmap() {
		if (BukkitIRCdPlugin.dynmap != null) {
			BukkitIRCdPlugin.dynmap = null;
			log.info("[BukkitIRCd] Dynmap plugin lost. Error Code BukkitIRCdPlugin308.");
		}
	}

	private void loadSettings() {
		try {
			mode = config.getString("mode", mode);
			ircd_ingamesuffix = config.getString("ingame-suffix", ircd_ingamesuffix);
			ircd_enablenotices = config.getBoolean("enable-notices", ircd_enablenotices);
			ircd_convertcolorcodes = config.getBoolean("convert-color-codes", ircd_convertcolorcodes);
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

			log.info("[BukkitIRCd] Loaded configuration file. Code BukkitIRCdPlugin363.");
			saveConfig();
			log.info("[BukkitIRCd] Saved configuration file. Code BukkitIRCdPlugin365.");
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
	
	
	private void loadMessages(IRCd ircd) {
		try {
			IRCd.msgLinked = messages.getString("linked", IRCd.msgLinked);
			
			IRCd.msgDelinked = messages.getString("delinked", IRCd.msgDelinked);
			IRCd.msgDelinkedReason = messages.getString("delinked-reason", IRCd.msgDelinkedReason);
			
			IRCd.msgIRCJoin = messages.getString("irc-join", IRCd.msgIRCJoin);
			IRCd.msgIRCJoinDynmap = messages.getString("irc-join-dynmap", IRCd.msgIRCJoinDynmap);

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
			
			
			//** RECOLOUR ALL MESSAGES **/
			
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
			

			log.info("[BukkitIRCd] Loaded messages file. Code BukkitIRCdPlugin464.");
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load messages file: " + e.toString());
		}
	}

	// It was originally disabled, I'd like to know why.
	@SuppressWarnings("unused")
	private void firstRunSettings(File dataFolder)
	{
		log.info("[BukkitIRCd] Configuration file not found, creating new one. Code BukkitIRCdPlugin475.");
		dataFolder.mkdirs();

		File configFile = new File(dataFolder, "config.yml");
		try
		{
			if(!configFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create config file! Error Code BukkitIRCdPlugin486.");
		}

		writeSettings(configFile);
	}
	

	private void loadMOTD() {
		File motdFile = new File(getDataFolder(), "motd.txt");

		IRCd.MOTD.clear();

		try {
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader input =  new BufferedReader(new FileReader(motdFile));
			try {
				String line = null; //not declared within while loop
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
				log.info("[BukkitIRCd] Loaded MOTD file. Code BukkitIRCdPlugin516.");
			}
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load MOTD file: " + e.toString());
		}
	}

	private void loadBans() {
		File bansFile = new File(getDataFolder(), "bans.txt");

		IRCd.ircBans.clear();

		try {
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
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
				log.info("[BukkitIRCd] Loaded bans file. Code BukkitIRCdPlugin551.");
			}
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load bans file: " + e.toString());
		}
	}

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
			log.info("[BukkitIRCd] Saved bans file. Code BukkitIRCdPlugin585.");
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

	private void saveDefaultMOTD(File dataFolder, String fileName)
	{
		log.info("[BukkitIRCd] MOTD file not found, creating new one. Code BukkitIRCdPlugin616");
		dataFolder.mkdirs();

		File motdFile = new File(dataFolder, fileName);
		try
		{
			if(!motdFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create MOTD file! BukkitIRCdPlugin627");
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
			log.info("[BukkitIRCd] Saved MOTD file. Code BukkitIRCdPlugin674");
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
	@SuppressWarnings("unused")
	private void saveDefaultMessages(File dataFolder, String fileName)
	{
		log.info("[BukkitIRCd] Messages file not found, creating new one. Code BukkitIRCdPlugin705");
		dataFolder.mkdirs();

		File msgFile = new File(dataFolder, fileName);
		try
		{
			if(!msgFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create messages file! Error code BukkitIRCdPlugin716.");
		}

		writeMessages(msgFile);
	}
	
	
	private void writeMessages(File messagesFile)
	{
		try
		{
			messages.save(messagesFile);
			log.info("[BukkitIRCd] Saved messages file. Code BukkitIRCdPlugin728.");
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
			log.info("[BukkitIRCd] Saved configuration file. Code BukkitIRCdPlugin742.");
		}
		catch(Exception e)
		{
			log.warning("[BukkitIRCd] Caught exception while writing settings to file: ");
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (!isEnabled()) { return true; }
		String commandName = command.getName().toLowerCase();
		String[] trimmedArgs = args;



		if (sender instanceof Player) {
			Player player = (Player)sender;
			if (commandName.equalsIgnoreCase("irclist") || commandName.equalsIgnoreCase("ilist")) {
				if (hasPermission(player, "bukkitircd.list")) {
					String players[] = IRCd.getIRCNicks();
					String allplayers = "";
					for (String curplayer : players) allplayers += ChatColor.GRAY + curplayer + ChatColor.WHITE + ", ";
					player.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
					if (players.length > 0) player.sendMessage(allplayers.substring(0,allplayers.length()-2));
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("irckick") || commandName.equalsIgnoreCase("ikick")) {
				if (hasPermission(player, "bukkitircd.kick")) {
					if (trimmedArgs.length > 0) {
						String reason = null;
						if (trimmedArgs.length > 1) reason = IRCd.join(trimmedArgs, " ", 1);
						IRCUser ircuser = IRCd.getIRCUser(trimmedArgs[0]);
						if (ircuser != null) {
							if (IRCd.kickIRCUser(ircuser, player.getName(), player.getName(), player.getAddress().getAddress().getHostName(), reason, true))
								player.sendMessage(ChatColor.RED + "Player kicked.");
							else
								player.sendMessage(ChatColor.RED + "Failed to kick player.");
						}
						else { player.sendMessage(ChatColor.RED + "That user is not online."); }
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a nickname and optionally a kick reason."); return false; }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircban") || commandName.equalsIgnoreCase("iban")) {
				if (hasPermission(player, "bukkitircd.ban")) {
					if (trimmedArgs.length > 0) {
						String reason = null;
						IRCUser ircuser;
						String ban;
						String banType = null;
						if ((trimmedArgs[0].equalsIgnoreCase("ip")) || (trimmedArgs[0].equalsIgnoreCase("host")) || (trimmedArgs[0].equalsIgnoreCase("ident")) || (trimmedArgs[0].equalsIgnoreCase("nick"))) {
							ircuser = IRCd.getIRCUser(trimmedArgs[1]);
							ban = trimmedArgs[1];
							banType = trimmedArgs[0];
							if (trimmedArgs.length > 2) reason = IRCd.join(trimmedArgs, " ", 2);
						}
						else {
							ircuser = IRCd.getIRCUser(trimmedArgs[0]);
							ban = trimmedArgs[0];
							if (trimmedArgs.length > 1) reason = IRCd.join(trimmedArgs, " ", 1);
						}
						if (IRCd.wildCardMatch(ban, "*!*@*")) {
							// Full hostmask
							if (IRCd.banIRCUser(ban, player.getName() + IRCd.ingameSuffix + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
								if (IRCd.msgIRCBan.length() > 0) getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage(ChatColor.RED + "User banned.");
							}
							else player.sendMessage(ChatColor.RED + "User is already banned.");
						}
						else if (countStr(ban, ".") == 3) { // It's an IP
							if (IRCd.banIRCUser("*!*@" + ban, player.getName() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
								if (IRCd.msgIRCBan.length() > 0) getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage(ChatColor.RED + "IP banned.");
							}
							else
								player.sendMessage(ChatColor.RED + "IP is already banned.");
						}
						else {
							if (ircuser != null) {
								if (IRCd.kickBanIRCUser(ircuser, player.getName(), player.getName() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName(), reason, true, banType))
									player.sendMessage(ChatColor.RED + "User banned.");
								else
									player.sendMessage(ChatColor.RED + "User is already banned.");
							}
							else { player.sendMessage(ChatColor.RED + "That user is not online."); }
						}
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a nickname or IP and optionally a ban reason."); return false; }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircunban") || commandName.equalsIgnoreCase("iunban")) {
				if (hasPermission(player, "bukkitircd.unban")) {
					if (trimmedArgs.length > 0) {
						String ban;
						ban = trimmedArgs[0];
						if (trimmedArgs.length > 1)
							IRCd.join(trimmedArgs, " ", 1);

						if (IRCd.wildCardMatch(ban, "*!*@*")) { // Full hostmask
							if (IRCd.unBanIRCUser(ban, player.getName() + IRCd.ingameSuffix + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
								if (IRCd.msgIRCUnban.length() > 0) getServer().broadcastMessage(IRCd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (IRCd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage(ChatColor.RED + "User unbanned.");
							}
							else
								player.sendMessage(ChatColor.RED + "User is not banned.");
						}
						else if (countStr(ban, ".") == 3) { // It's an IP
							if (IRCd.unBanIRCUser("*!*@" + ban, player.getName() + IRCd.ingameSuffix + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
								if (IRCd.msgIRCUnban.length() > 0) getServer().broadcastMessage(IRCd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (IRCd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage(ChatColor.RED + "IP unbanned.");
							}
							else
								player.sendMessage(ChatColor.RED + "IP is not banned.");
						} 
						else { player.sendMessage(ChatColor.RED + "Invalid hostmask."); return false; }
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a IP/full hostmask."); return false; }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircwhois") || commandName.equalsIgnoreCase("iwhois")) {
				if (hasPermission(player, "bukkitircd.whois")) {
					if (trimmedArgs.length > 0) {
						IRCUser ircuser = IRCd.getIRCUser(trimmedArgs[0]);
						if (ircuser != null) {
							String[] whois = IRCd.getIRCWhois(ircuser);
							if (whois != null) {
								for (String whoisline : whois) player.sendMessage(whoisline);
							}
						}
						else { player.sendMessage(ChatColor.RED + "That user is not online."); }
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a nickname."); return false; }		}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircmsg") || commandName.equalsIgnoreCase("imsg") || commandName.equalsIgnoreCase("im")) {
				if (hasPermission(player, "bukkitircd.msg")) {
					if (trimmedArgs.length > 1) {
						IRCUser ircuser = IRCd.getIRCUser(trimmedArgs[0]);
						if (ircuser != null) {
							if (IRCd.mode == Modes.STANDALONE) { 
								IRCd.writeTo(ircuser.nick, ":" + player.getName() + IRCd.ingameSuffix + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName() + " PRIVMSG " + ircuser.nick + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 1), false));
								player.sendMessage(ChatColor.RED + "Message sent.");
							}
							else if (IRCd.mode == Modes.INSPIRCD) {
								BukkitPlayer bp;
								if ((bp = IRCd.getBukkitUserObject(player.getName())) != null) {
									String UID = IRCd.getUIDFromIRCUser(ircuser);
									if (UID != null) {
										if (IRCd.linkcompleted) {
											IRCd.println(":" + bp.getUID() + " PRIVMSG " + UID + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 1), false));
											player.sendMessage(ChatColor.RED + "Message sent.");
										}
										else player.sendMessage(ChatColor.RED + "Failed to send message, not currently linked to IRC server.");
									}
									else {
										log.severe("UID not found in list: " + UID); // Log this as severe since it should never occur unless something is wrong with the code
										player.sendMessage(ChatColor.RED + "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
									}
								}
								else player.sendMessage(ChatColor.RED + "Failed to send message, you could not be found in the UID list. This should not happen, please report it to Jdbye.");
							}
						}
						else { player.sendMessage(ChatColor.RED + "That user is not online."); }
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a nickname and a message."); return false; }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}    		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreply") || commandName.equalsIgnoreCase("ireply") || commandName.equalsIgnoreCase("ir")) {
				if (hasPermission(player, "bukkitircd.reply")) {
					if (trimmedArgs.length > 0) {
						String lastReceivedFrom = lastReceived.get(player.getName());
						if (lastReceivedFrom == null) {
							player.sendMessage(ChatColor.RED + "There are no messages to reply to!");
						}
						else {
							IRCUser ircuser = IRCd.getIRCUser(lastReceivedFrom);
							if (ircuser != null) {
								if (IRCd.mode == Modes.STANDALONE) {
									IRCd.writeTo(ircuser.nick, ":" + player.getName() + IRCd.ingameSuffix + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName() + " PRIVMSG " + ircuser.nick + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 0), false));
									player.sendMessage(ChatColor.RED + "Message sent to " + lastReceivedFrom + ".");
								}
								else if (IRCd.mode == Modes.INSPIRCD) {
									BukkitPlayer bp;
									if ((bp = IRCd.getBukkitUserObject(player.getName())) != null) {
										String UID = IRCd.getUIDFromIRCUser(ircuser);
										if (UID != null) {
											if (IRCd.linkcompleted) {
												IRCd.println(":" + bp.getUID() + " PRIVMSG " + UID + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 0), false));
												player.sendMessage(ChatColor.RED + "Message sent to " + lastReceivedFrom + ".");
											} else player.sendMessage(ChatColor.RED + "Failed to send message, not currently linked to IRC server.");
										}
										else {
											log.severe("UID not found in list: " + UID); // Log this as severe since it should never occur unless something is wrong with the code
											player.sendMessage(ChatColor.RED + "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
										}
									}
									else player.sendMessage(ChatColor.RED + "Failed to send message, you could not be found in the UID list. This should not happen, please report it to Jdbye.");
								}
							}
							else { player.sendMessage(ChatColor.RED + "That user is not online."); }
						}
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a nickname and a message."); return false; }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}    			
				return true;
			}
			else if (commandName.equalsIgnoreCase("irctopic") || commandName.equalsIgnoreCase("itopic")) {
				if (hasPermission(player, "bukkitircd.topic")) {
					if (trimmedArgs.length > 0) {
						String topic = IRCd.join(trimmedArgs, " ", 0);
						String playername = player.getName();
						String playerhost = player.getAddress().getAddress().getHostName();
						IRCd.setTopic(IRCd.convertColors(topic, false), playername + IRCd.ingameSuffix, playername + IRCd.ingameSuffix + "!" + playername + "@" + playerhost);
						player.sendMessage(ChatColor.RED + "Topic set to " + topic);
					}
					else { player.sendMessage(ChatColor.RED + "Please provide a topic to set."); return false; }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}    		
				return true;
			}
			else if (commandName.equalsIgnoreCase("irclink") || commandName.equalsIgnoreCase("ilink")) {
				if (hasPermission(player, "bukkitircd.link")) {
					if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
						if ((!IRCd.linkcompleted) && (!IRCd.isConnected())) {
							if (IRCd.connect()) player.sendMessage(ChatColor.RED + "Successfully connected to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
							else player.sendMessage(ChatColor.RED + "Failed to connect to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
						}
						else {
							if (IRCd.linkcompleted) player.sendMessage(ChatColor.RED + "Already linked to " + IRCd.linkName + ".");
							else player.sendMessage(ChatColor.RED + "Already connected to " + IRCd.linkName + ", but not linked.");
						}
					}
					else { player.sendMessage(ChatColor.RED + "Not in linking mode."); }
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreload") || commandName.equalsIgnoreCase("ireload")) {
				if (hasPermission(player, "bukkitircd.reload")) {
					pluginInit(true);
					log.info("[BukkitIRCd] Configuration file reloaded.");
					player.sendMessage(ChatColor.RED + "Configuration file reloaded.");
				}
				else {
					player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("rawsend")) {
				if (enableRawSend) {
					sender.sendMessage(ChatColor.RED + "Rawsend is only usable from console.");
				}
				else { sender.sendMessage(ChatColor.RED + "Rawsend is not enabled."); }
				return true;
			}
		}
		else {
			if (commandName.equalsIgnoreCase("irclist") || commandName.equalsIgnoreCase("ilist")) {
				String players[] = IRCd.getIRCNicks();
				String allplayers = "";
				for (String curplayer : players) allplayers += ChatColor.GRAY + curplayer + ChatColor.WHITE + ", ";
				sender.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
				if (players.length > 0) sender.sendMessage(allplayers.substring(0,allplayers.length()-2));
				return true;
			}
			else if (commandName.equalsIgnoreCase("irckick") || commandName.equalsIgnoreCase("ikick")) {
				if (trimmedArgs.length > 0) {
					String reason = null;
					if (trimmedArgs.length > 1) reason = IRCd.join(trimmedArgs, " ", 1);
					IRCUser ircuser = IRCd.getIRCUser(trimmedArgs[0]);
					if (ircuser != null) {
						if (IRCd.kickIRCUser(ircuser, IRCd.serverName, IRCd.serverName, IRCd.serverHostName, reason, false)) sender.sendMessage(ChatColor.RED + "Player kicked.");
						else sender.sendMessage(ChatColor.RED + "Failed to kick player.");
						
					}
					else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a nickname and optionally a kick reason."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircban") || commandName.equalsIgnoreCase("iban")) {
				if (trimmedArgs.length > 0) {
					String reason = null;
					IRCUser ircuser;
					String ban;
					String banType = null;
					if ((trimmedArgs[0].equalsIgnoreCase("ip")) || (trimmedArgs[0].equalsIgnoreCase("host")) || (trimmedArgs[0].equalsIgnoreCase("ident")) || (trimmedArgs[0].equalsIgnoreCase("nick"))) {
						ircuser = IRCd.getIRCUser(trimmedArgs[1]);
						ban = trimmedArgs[1];
						banType = trimmedArgs[0];
						if (trimmedArgs.length > 2) reason = IRCd.join(trimmedArgs, " ", 2);
					}
					else {
						ircuser = IRCd.getIRCUser(trimmedArgs[0]);
						ban = trimmedArgs[0];
						if (trimmedArgs.length > 1) reason = IRCd.join(trimmedArgs, " ", 1);
					}
					if (IRCd.wildCardMatch(ban, "*!*@*")) {
						// Full hostmask
						if (IRCd.banIRCUser(ban, IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName)) {
							if (IRCd.msgIRCBan.length() > 0) getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage(ChatColor.RED + "User banned.");
						}
						else
							sender.sendMessage(ChatColor.RED + "User is already banned.");
					}
					else if (countStr(ban, ".") == 3) { // It's an IP
						if (IRCd.banIRCUser("*!*@" + ban, IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName)) {
							if (IRCd.msgIRCBan.length() > 0) getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage(ChatColor.RED + "IP banned.");
						}
						else
							sender.sendMessage(ChatColor.RED + "IP is already banned.");
					}
					else {
						if (ircuser != null) {
							if (IRCd.kickBanIRCUser(ircuser, "server", IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName, reason, true, banType))
								sender.sendMessage(ChatColor.RED + "User banned.");
							else
								sender.sendMessage(ChatColor.RED + "User is already banned.");
						}
						else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
					}
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a nickname or IP and optionally a ban reason."); return false; }
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircunban") || commandName.equalsIgnoreCase("iunban")) {
				if (trimmedArgs.length > 0) {
					String ban;
					ban = trimmedArgs[0];
					if (trimmedArgs.length > 1)
						IRCd.join(trimmedArgs, " ", 1);

					if (IRCd.wildCardMatch(ban, "*!*@*")) { // Full hostmask
						if (IRCd.unBanIRCUser(ban, IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName)) {
							if (IRCd.msgIRCUnban.length() > 0) getServer().broadcastMessage(IRCd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (IRCd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage(ChatColor.RED + "User unbanned.");
						}
						else
							sender.sendMessage(ChatColor.RED + "User is not banned.");
					}
					else if (countStr(ban, ".") == 3) { // It's an IP
						if (IRCd.unBanIRCUser("*!*@" + ban, IRCd.serverName+"!" + IRCd.serverName+"@" + IRCd.serverHostName)) {
							if (IRCd.msgIRCUnban.length() > 0) getServer().broadcastMessage(IRCd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (IRCd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage(ChatColor.RED + "IP unbanned.");
						}
						else
							sender.sendMessage(ChatColor.RED + "IP is not banned.");
					} 
					else { sender.sendMessage(ChatColor.RED + "Invalid hostmask."); return false; }

				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a IP/full hostmask."); return false; }
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircwhois") || commandName.equalsIgnoreCase("iwhois")) {
				if (trimmedArgs.length > 0) {
					IRCUser ircuser = IRCd.getIRCUser(trimmedArgs[0]);
					if (ircuser != null) {
						String[] whois = IRCd.getIRCWhois(ircuser);
						for (String whoisline : whois) sender.sendMessage(whoisline);
					}
					else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a nickname."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircmsg") || commandName.equalsIgnoreCase("imsg") || commandName.equalsIgnoreCase("im")) {
				if (trimmedArgs.length > 1) {
					IRCUser ircuser = IRCd.getIRCUser(trimmedArgs[0]);
					if (ircuser != null) {
						if (IRCd.mode == Modes.STANDALONE) {
							IRCd.writeTo(ircuser.nick, ":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " PRIVMSG " + ircuser.nick + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 1),false));
							sender.sendMessage(ChatColor.RED + "Message sent.");
						}
						else if (IRCd.mode == Modes.INSPIRCD) {
							String UID = IRCd.getUIDFromIRCUser(ircuser);
							if (UID != null) {
								if (IRCd.linkcompleted) {
									IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + UID + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 1), false));
									sender.sendMessage(ChatColor.RED + "Message sent.");
								} else sender.sendMessage(ChatColor.DARK_RED + "Failed to send message, not currently linked to IRC server.");
							}
							else {
								log.severe("UID not found in list: " + UID); // Log this as severe since it should never occur unless something is wrong with the code
								sender.sendMessage(ChatColor.RED + "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
							}
						}
					}
					else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a nickname and a message."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreply") || commandName.equalsIgnoreCase("ireply") || commandName.equalsIgnoreCase("ir")) {
				if (trimmedArgs.length > 0) {
					String lastReceivedFrom = lastReceived.get("@CONSOLE@");
					if (lastReceivedFrom == null) {
						sender.sendMessage(ChatColor.RED + "There are no messages to reply to!");
					}
					else {
						IRCUser ircuser = IRCd.getIRCUser(lastReceivedFrom);
						if (ircuser != null) {
							if (IRCd.mode == Modes.STANDALONE) {
								IRCd.writeTo(ircuser.nick, ":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " PRIVMSG " + ircuser.nick + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 0),false));
								sender.sendMessage(ChatColor.RED + "Message sent to " + lastReceivedFrom + ".");
							}
							else if (IRCd.mode == Modes.INSPIRCD) {
								String UID = IRCd.getUIDFromIRCUser(ircuser);
								if (UID != null) {
									if (IRCd.linkcompleted) {
										IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + UID + " :" + IRCd.convertColors(IRCd.join(trimmedArgs, " ", 0), false));
										sender.sendMessage(ChatColor.RED + "Message sent to " + lastReceivedFrom + ".");
									} else sender.sendMessage(ChatColor.RED + "Failed to send message, not currently linked to IRC server.");
								}
								else {
									log.severe("UID not found in list: " + UID); // Log this as severe since it should never occur unless something is wrong with the code
									sender.sendMessage(ChatColor.RED + "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
								}
							}
						}
						else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
					}
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a nickname and a message."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("irctopic") || commandName.equalsIgnoreCase("itopic")) {
				if (trimmedArgs.length > 0) {
					String topic = IRCd.join(trimmedArgs, " ", 0);
					IRCd.setTopic(IRCd.convertColors(topic, false), IRCd.serverName, ircd_servername + "!" + ircd_servername + "@" + ircd_serverhostname);
					sender.sendMessage(ChatColor.RED + "Topic set to " + topic);
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a topic to set."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("irclink") || commandName.equalsIgnoreCase("ilink")) {
				if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
					if ((!IRCd.linkcompleted) && (!IRCd.isConnected())) {
						if (IRCd.connect()) sender.sendMessage(ChatColor.RED + "Successfully connected to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
						else sender.sendMessage(ChatColor.RED + "Failed to connect to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
					}
					else {
						if (IRCd.linkcompleted) sender.sendMessage(ChatColor.RED + "Already linked to " + IRCd.linkName + ".");
						else sender.sendMessage(ChatColor.RED + "Already connected to " + IRCd.linkName + ", but not linked.");
					}
				}
				else { sender.sendMessage(ChatColor.RED + "Not in linking mode."); }
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreload") || commandName.equalsIgnoreCase("ireload")) {
				pluginInit(true);
				log.info("[BukkitIRCd] Configuration file reloaded.");
				sender.sendMessage(ChatColor.RED + "Configuration file reloaded.");
				return true;
			}
			else if (commandName.equalsIgnoreCase("rawsend")) {
				if (enableRawSend) {
					if (trimmedArgs.length > 0) {
						if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
							if (IRCd.println(IRCd.join(trimmedArgs, " ", 0))) sender.sendMessage(ChatColor.RED + "Command sent to IRC server link.");
							else sender.sendMessage(ChatColor.RED + "Failed to send command to IRC server link, not currently linked.");
						}
					}
					else { sender.sendMessage(ChatColor.RED + "Please provide a command to send."); return false; }
				}
				else { sender.sendMessage(ChatColor.RED + "Rawsend is not enabled."); }
				return true;
			}
		}
		return false;
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
}

