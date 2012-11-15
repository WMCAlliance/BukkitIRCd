package com.Jdbye.BukkitIRCd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.DriverManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dynmap.DynmapAPI;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

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
	public static int link_serverid = new Random().nextInt(900)+100;

	public static final Logger log = Logger.getLogger("Minecraft");
	
	private boolean enableRawSend = false;

	public static PermissionHandler permissionHandler = null;
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
			ircd.disconnectAll();
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
				ircd.disconnectAll("Reloading configuration");
				ircd = null; 
			}
			if (thr != null) {
				thr.interrupt();
				thr = null;
			}
		}
		
		reloadConfig();
		config = getConfig();
		// Create default config if it doesn't exist.
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			log.info("[BukkitIRCd] Creating default configuration file - please modify this file then type /ireload.");
		}
		config.options().copyDefaults(true);
		loadSettings();
		
		// Create default config if it doesn't exist.
		File messagesFile = new File(getDataFolder(), "messages.yml");
		messages = YamlConfiguration.loadConfiguration(messagesFile);
		if (!(messagesFile.exists())) {
			log.info("[BukkitIRCd] Creating default messages file...");
		}
		messages.options().copyDefaults(true);
		

		if (!(new File(getDataFolder(), "motd.txt")).exists()) {
			saveDefaultMOTD(getDataFolder(),"motd.txt");
			log.info("[BukkitIRCd] Default MOTD file created.");
		}
		loadMOTD();

		File bans;
		if (!(bans = new File(getDataFolder(), "bans.txt")).exists()) {
			if (writeBans()) log.info("[BukkitIRCd] Blank bans file created.");
			else log.warning("[BukkitIRCd] Failed to create bans file.");
		}

		setupPermissions();
		setupDynmap();
		
		ircd = new IRCd();
		loadMessages(ircd);
		
		ircd.port = ircd_port;
		ircd.maxConnections = ircd_maxconn;
		ircd.pingInterval = ircd_pinginterval;
		ircd.timeoutInterval = ircd_timeout;
		ircd.nickLen = ircd_maxnicklen;
		ircd.channelName = ircd_channel;
		if (!reload) {
			ircd.channelTopic = ircd_topic;
			ircd.channelTopicSet = ircd_topicsetby;
			ircd.channelTopicSetDate = ircd_topicsetdate / 1000L;
		}
		ircd.serverName = ircd_servername;
		ircd.serverDescription = ircd_serverdescription;
		ircd.serverHostName = ircd_serverhostname;
		ircd.serverCreationDate = ircd_creationdate;
		ircd.ingameSuffix = ircd_ingamesuffix;
		ircd.enableNotices = ircd_enablenotices;
		ircd.convertColorCodes = ircd_convertcolorcodes;
		ircd.ircBanType = ircd_bantype;
		ircd.version = ircd_version;
		ircd.operUser = ircd_operuser;
		ircd.operPass = ircd_operpass;
		ircd.operModes = ircd_opermodes;
		ircd.consoleChannelName = ircd_consolechannel;
		ircd.modestr = mode;
		ircd.debugMode = debugmode;
		ircd.gameColors = ircd_game_colors.split(",");
		ircd.ircColors = convertStringArrayToIntArray(ircd_irc_colors.split(","), ircd.ircColors);
		
		// Linking specific settings
		ircd.remoteHost = link_remotehost;
		ircd.remotePort = link_remoteport;
		ircd.localPort = link_localport;
		ircd.autoConnect = link_autoconnect;
		ircd.linkName = link_name;
		ircd.connectPassword = link_connectpassword;
		ircd.receivePassword = link_receivepassword;
		ircd.linkPingInterval = link_pinginterval;
		ircd.linkTimeoutInterval = link_timeout;
		ircd.linkDelay = link_delay;
		ircd.SID = link_serverid;

		loadBans();
		ircd.bukkitPlayers.clear();
		for (Player player : getServer().getOnlinePlayers()) {
			String mode = "";
			if (hasPermission(player, "bukkitircd.mode.owner")) mode += "~";
			if (hasPermission(player, "bukkitircd.mode.protect")) mode += "&";
			if (hasPermission(player, "bukkitircd.mode.op")) mode += "@";
			if (hasPermission(player, "bukkitircd.mode.halfop")) mode += "%";
			if (hasPermission(player, "bukkitircd.mode.voice")) mode += "+";
			ircd.addBukkitUser(mode,player);
		}

		thr = new Thread(ircd);
		thr.start();

	}

	public void setDebugging(final Player player, final boolean value) {
		debugees.put(player, value);
	}

	private void setupPermissions() {
		Plugin plugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (this.permissionHandler == null) {
			if (plugin != null) {
				setupPermissions((Permissions)plugin);
			} else {
				log.info("[BukkitIRCd] Permissions plugin not found. Using Superperms.");
			}
		}
	}
	
	public void setupPermissions(Permissions plugin) {
		if (plugin != null) {
			this.permissionHandler = plugin.getHandler();
			log.info("[BukkitIRCd] Hooked into Permissions 2.x");
		}
	}
	
	public void unloadPermissions() {
		if (this.permissionHandler != null) {
			this.permissionHandler = null;
			log.info("[BukkitIRCd] Permissions plugin lost.");
		}
	}
	
	private void setupDynmap() {
		PluginManager pm = getServer().getPluginManager();
		Plugin plugin = pm.getPlugin("dynmap");
		if (this.dynmap == null) {
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
			log.info("[BukkitIRCd] Hooked into Dynmap.");
		}
	}

	public void unloadDynmap() {
		if (this.dynmap != null) {
			this.dynmap = null;
			log.info("[BukkitIRCd] Dynmap plugin lost.");
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
			ircd_topic = config.getString("standalone.channel-topic", ircd_topic).replace("^K", (char)3+"").replace("^B", (char)2+"").replace("^I", (char)29+"").replace("^O", (char)15+"").replace("^U", (char)31+"");
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

			log.info("[BukkitIRCd] Loaded configuration file.");
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load configuration file: "+e.toString());
		}
	}

	private void loadMessages(IRCd ircd) {
		try {
			ircd.msgLinked = messages.getString("linked", ircd.msgLinked);

			ircd.msgDelinked = messages.getString("delinked", ircd.msgDelinked);
			ircd.msgDelinkedReason = messages.getString("delinked-reason", ircd.msgDelinkedReason);

			ircd.msgIRCJoin = messages.getString("irc-join", ircd.msgIRCJoin);
			ircd.msgIRCJoinDynmap = messages.getString("irc-join-dynmap", ircd.msgIRCJoinDynmap);

			ircd.msgIRCLeave = messages.getString("irc-leave", ircd.msgIRCLeave);
			ircd.msgIRCLeaveReason = messages.getString("irc-leave-reason", ircd.msgIRCLeaveReason);
			ircd.msgIRCLeaveDynmap = messages.getString("irc-leave-dynmap", ircd.msgIRCLeaveDynmap);
			ircd.msgIRCLeaveReasonDynmap = messages.getString("irc-leave-reason-dynmap", ircd.msgIRCLeaveReasonDynmap);

			ircd.msgIRCKick = messages.getString("irc-kick", ircd.msgIRCKick);
			ircd.msgIRCKickReason = messages.getString("irc-kick-reason", ircd.msgIRCKickReason);
			ircd.msgIRCKickDynmap = messages.getString("irc-kick-dynmap", ircd.msgIRCKickDynmap);
			ircd.msgIRCKickReasonDynmap = messages.getString("irc-kick-reason-dynmap", ircd.msgIRCKickReasonDynmap);

			ircd.msgIRCBan = messages.getString("irc-ban", ircd.msgIRCBan);
			ircd.msgIRCBanDynmap = messages.getString("irc-ban-dynmap", ircd.msgIRCBanDynmap);

			ircd.msgIRCUnban = messages.getString("irc-unban", ircd.msgIRCUnban);
			ircd.msgIRCUnbanDynmap = messages.getString("irc-unban-dynmap", ircd.msgIRCUnbanDynmap);

			ircd.msgIRCNickChange = messages.getString("irc-nick-change", ircd.msgIRCNickChange);
			ircd.msgIRCNickChangeDynmap = messages.getString("irc-nick-change-dynmap", ircd.msgIRCNickChangeDynmap);

			ircd.msgIRCAction = messages.getString("irc-action", ircd.msgIRCAction);
			ircd.msgIRCMessage = messages.getString("irc-message", ircd.msgIRCMessage);
			ircd.msgIRCNotice = messages.getString("irc-notice", ircd.msgIRCNotice);

			ircd.msgIRCPrivateAction = messages.getString("irc-private-action", ircd.msgIRCPrivateAction);
			ircd.msgIRCPrivateMessage = messages.getString("irc-private-message", ircd.msgIRCPrivateMessage);
			ircd.msgIRCPrivateNotice = messages.getString("irc-private-notice", ircd.msgIRCPrivateNotice);

			ircd.msgIRCActionDynmap = messages.getString("irc-action-dynmap", ircd.msgIRCActionDynmap);
			ircd.msgIRCMessageDynmap = messages.getString("irc-message-dynmap", ircd.msgIRCMessageDynmap);
			ircd.msgIRCNoticeDynmap = messages.getString("irc-notice-dynmap", ircd.msgIRCNoticeDynmap);

			ircd.msgDynmapMessage = messages.getString("dynmap-message", ircd.msgDynmapMessage);
			ircd.msgPlayerList = messages.getString("player-list", ircd.msgPlayerList);

			log.info("[BukkitIRCd] Loaded messages file.");
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load messages file: "+e.toString());
		}
	}

	/*
	private void firstRunSettings(File dataFolder)
	{
		log.info("[BukkitIRCd] Configuration file not found, creating new one.");
		dataFolder.mkdirs();

		File configFile = new File(dataFolder, "config.yml");
		try
		{
			if(!configFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create config file!");
		}

		writeSettings(configFile);
	}
	*/

	private void loadMOTD() {
		File motdFile = new File(getDataFolder(), "motd.txt");

		ircd.MOTD.clear();

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
					ircd.MOTD.add(line);
				}
			}
			finally {
				input.close();
				log.info("[BukkitIRCd] Loaded MOTD file.");
			}
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load MOTD file: "+e.toString());
		}
	}

	private void loadBans() {
		File bansFile = new File(getDataFolder(), "bans.txt");

		ircd.ircBans.clear();

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
						try { ircd.ircBans.add(new IrcBan(split[0], split[1], Long.parseLong(split[2]))); }
						catch (NumberFormatException e) { log.warning("[BukkitIRCd] Invalid ban: "+line); }
					}
				}
			}
			finally {
				input.close();
				log.info("[BukkitIRCd] Loaded bans file.");
			}
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load bans file: "+e.toString());
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

			synchronized(ircd.csIrcBans) {
				for (IrcBan ban : ircd.ircBans) {
					bufferWriter.append(ban.fullHost+","+ban.bannedBy+","+ban.banTime);
					bufferWriter.newLine();
				}
			}

			bufferWriter.flush();
			log.info("[BukkitIRCd] Saved bans file.");
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
		log.info("[BukkitIRCd] MOTD file not found, creating new one.");
		dataFolder.mkdirs();

		File motdFile = new File(dataFolder, fileName);
		try
		{
			if(!motdFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create MOTD file!");
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

			bufferWriter.append("Last changed on "+dateFormat.format(curDate));
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
			bufferWriter.append("Welcome to "+ircd_servername+", running "+ircd_version+".");
			bufferWriter.newLine();
			bufferWriter.append("Enjoy your stay!");
			bufferWriter.newLine();

			bufferWriter.flush();
			log.info("[BukkitIRCd] Saved MOTD file.");
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
	
	/*
	private void saveDefaultMessages(File dataFolder, String fileName)
	{
		log.info("[BukkitIRCd] Messages file not found, creating new one.");
		dataFolder.mkdirs();

		File msgFile = new File(dataFolder, fileName);
		try
		{
			if(!msgFile.createNewFile())
				throw new IOException("Failed file creation");
		}
		catch(IOException e)
		{
			log.warning("[BukkitIRCd] Could not create messages file!");
		}

		writeMessages(msgFile);
	}
	*/
	
	private void writeMessages(File messagesFile)
	{
		try
		{
			messages.save(messagesFile);
			log.info("[BukkitIRCd] Saved messages file.");
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
			log.info("[BukkitIRCd] Saved configuration file.");
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
					String players[] = ircd.getIRCNicks();
					String allplayers = "";
					for (String curplayer : players) allplayers += "§7"+curplayer+"§f, ";
					player.sendMessage("§9There are §c"+players.length+" §9users on IRC.");
					if (players.length > 0) player.sendMessage(allplayers.substring(0,allplayers.length()-2));
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("irckick") || commandName.equalsIgnoreCase("ikick")) {
				if (hasPermission(player, "bukkitircd.kick")) {
					if (trimmedArgs.length > 0) {
						String reason = null;
						if (trimmedArgs.length > 1) reason = ircd.join(trimmedArgs, " ", 1);
						IRCUser ircuser = ircd.getIRCUser(trimmedArgs[0]);
						if (ircuser != null) {
							if (ircd.kickIRCUser(ircuser, player.getName(), player.getName(), player.getAddress().getAddress().getHostName(), reason, true))
								player.sendMessage("§cPlayer kicked.");
							else
								player.sendMessage("§cFailed to kick player.");
						}
						else { player.sendMessage("§cThat user is not online."); }
					}
					else { player.sendMessage("§cPlease provide a nickname and optionally a kick reason."); return false; }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
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
							ircuser = ircd.getIRCUser(trimmedArgs[1]);
							ban = trimmedArgs[1];
							banType = trimmedArgs[0];
							if (trimmedArgs.length > 2) reason = ircd.join(trimmedArgs, " ", 2);
						}
						else {
							ircuser = ircd.getIRCUser(trimmedArgs[0]);
							ban = trimmedArgs[0];
							if (trimmedArgs.length > 1) reason = ircd.join(trimmedArgs, " ", 1);
						}
						if (IRCd.wildCardMatch(ban, "*!*@*")) {
							// Full hostmask
							if (ircd.banIRCUser(ban, player.getName()+IRCd.ingameSuffix+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName())) {
								if (ircd.msgIRCBan.length() > 0) getServer().broadcastMessage(ircd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (ircd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage("§cUser banned.");
							}
							else player.sendMessage("§cUser is already banned.");
						}
						else if (countStr(ban, ".") == 3) { // It's an IP
							if (ircd.banIRCUser("*!*@"+ban, player.getName()+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName())) {
								if (ircd.msgIRCBan.length() > 0) getServer().broadcastMessage(ircd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (ircd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage("§cIP banned.");
							}
							else
								player.sendMessage("§cIP is already banned.");
						}
						else {
							if (ircuser != null) {
								if (ircd.kickBanIRCUser(ircuser, player.getName(), player.getName()+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName(), reason, true, banType))
									player.sendMessage("§cUser banned.");
								else
									player.sendMessage("§cUser is already banned.");
							}
							else { player.sendMessage("§cThat user is not online."); }
						}
					}
					else { player.sendMessage("§cPlease provide a nickname or IP and optionally a ban reason."); return false; }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircunban") || commandName.equalsIgnoreCase("iunban")) {
				if (hasPermission(player, "bukkitircd.unban")) {
					if (trimmedArgs.length > 0) {
						String reason = null;
						String ban;
						String banType = null;

						ban = trimmedArgs[0];
						if (trimmedArgs.length > 1) reason = ircd.join(trimmedArgs, " ", 1);

						if (IRCd.wildCardMatch(ban, "*!*@*")) { // Full hostmask
							if (ircd.unBanIRCUser(ban, player.getName()+IRCd.ingameSuffix+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName())) {
								if (ircd.msgIRCUnban.length() > 0) getServer().broadcastMessage(ircd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (ircd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage("§cUser unbanned.");
							}
							else
								player.sendMessage("§cUser is not banned.");
						}
						else if (countStr(ban, ".") == 3) { // It's an IP
							if (ircd.unBanIRCUser("*!*@"+ban, player.getName()+IRCd.ingameSuffix+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName())) {
								if (ircd.msgIRCUnban.length() > 0) getServer().broadcastMessage(ircd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								if ((dynmap != null) && (ircd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
								player.sendMessage("§cIP unbanned.");
							}
							else
								player.sendMessage("§cIP is not banned.");
						} 
						else { player.sendMessage("§cInvalid hostmask."); return false; }
					}
					else { player.sendMessage("§cPlease provide a IP/full hostmask."); return false; }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircwhois") || commandName.equalsIgnoreCase("iwhois")) {
				if (hasPermission(player, "bukkitircd.whois")) {
					if (trimmedArgs.length > 0) {
						IRCUser ircuser = ircd.getIRCUser(trimmedArgs[0]);
						if (ircuser != null) {
							String[] whois = ircd.getIRCWhois(ircuser);
							if (whois != null) {
								for (String whoisline : whois) player.sendMessage(whoisline);
							}
						}
						else { player.sendMessage("§cThat user is not online."); }
					}
					else { player.sendMessage("§cPlease provide a nickname."); return false; }		}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircmsg") || commandName.equalsIgnoreCase("imsg") || commandName.equalsIgnoreCase("im")) {
				if (hasPermission(player, "bukkitircd.msg")) {
					if (trimmedArgs.length > 1) {
						IRCUser ircuser = ircd.getIRCUser(trimmedArgs[0]);
						if (ircuser != null) {
							if (ircd.mode == Modes.STANDALONE) { 
								ircd.writeTo(ircuser.nick, ":"+player.getName()+ircd.ingameSuffix+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName()+" PRIVMSG "+ircuser.nick+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 1), false));
								player.sendMessage("§cMessage sent.");
							}
							else if (ircd.mode == Modes.INSPIRCD) {
								BukkitPlayer bp;
								if ((bp = IRCd.getBukkitUserObject(player.getName())) != null) {
									String UID = IRCd.getUIDFromIRCUser(ircuser);
									if (UID != null) {
										if (IRCd.linkcompleted) {
											IRCd.println(":"+bp.getUID()+" PRIVMSG "+UID+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 1), false));
											player.sendMessage("§cMessage sent.");
										}
										else player.sendMessage("§cFailed to send message, not currently linked to IRC server.");
									}
									else {
										log.severe("UID not found in list: "+UID); // Log this as severe since it should never occur unless something is wrong with the code
										player.sendMessage("§cFailed to send message, UID not found. This should not happen, please report it to Jdbye.");
									}
								}
								else player.sendMessage("§cFailed to send message, you could not be found in the UID list. This should not happen, please report it to Jdbye.");
							}
						}
						else { player.sendMessage("§cThat user is not online."); }
					}
					else { player.sendMessage("§cPlease provide a nickname and a message."); return false; }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}    		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreply") || commandName.equalsIgnoreCase("ireply") || commandName.equalsIgnoreCase("ir")) {
				if (hasPermission(player, "bukkitircd.reply")) {
					if (trimmedArgs.length > 0) {
						String lastReceivedFrom = lastReceived.get(player.getName());
						if (lastReceivedFrom == null) {
							player.sendMessage("§cThere are no messages to reply to!");
						}
						else {
							IRCUser ircuser = ircd.getIRCUser(lastReceivedFrom);
							if (ircuser != null) {
								if (ircd.mode == Modes.STANDALONE) {
									ircd.writeTo(ircuser.nick, ":"+player.getName()+ircd.ingameSuffix+"!"+player.getName()+"@"+player.getAddress().getAddress().getHostName()+" PRIVMSG "+ircuser.nick+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 0), false));
									player.sendMessage("§cMessage sent to "+lastReceivedFrom+".");
								}
								else if (ircd.mode == Modes.INSPIRCD) {
									BukkitPlayer bp;
									if ((bp = IRCd.getBukkitUserObject(player.getName())) != null) {
										String UID = IRCd.getUIDFromIRCUser(ircuser);
										if (UID != null) {
											if (IRCd.linkcompleted) {
												IRCd.println(":"+bp.getUID()+" PRIVMSG "+UID+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 0), false));
												player.sendMessage("§cMessage sent to "+lastReceivedFrom+".");
											} else player.sendMessage("§cFailed to send message, not currently linked to IRC server.");
										}
										else {
											log.severe("UID not found in list: "+UID); // Log this as severe since it should never occur unless something is wrong with the code
											player.sendMessage("§cFailed to send message, UID not found. This should not happen, please report it to Jdbye.");
										}
									}
									else player.sendMessage("§cFailed to send message, you could not be found in the UID list. This should not happen, please report it to Jdbye.");
								}
							}
							else { player.sendMessage("§cThat user is not online."); }
						}
					}
					else { player.sendMessage("§cPlease provide a nickname and a message."); return false; }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}    			
				return true;
			}
			else if (commandName.equalsIgnoreCase("irctopic") || commandName.equalsIgnoreCase("itopic")) {
				if (hasPermission(player, "bukkitircd.topic")) {
					if (trimmedArgs.length > 0) {
						String topic = ircd.join(trimmedArgs, " ", 0);
						String playername = player.getName();
						String playerhost = player.getAddress().getAddress().getHostName();
						ircd.setTopic(ircd.convertColors(topic, false), playername+IRCd.ingameSuffix, playername+IRCd.ingameSuffix+"!"+playername+"@"+playerhost);
						player.sendMessage("§cTopic set to "+topic);
					}
					else { player.sendMessage("§cPlease provide a topic to set."); return false; }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}    		
				return true;
			}
			else if (commandName.equalsIgnoreCase("irclink") || commandName.equalsIgnoreCase("ilink")) {
				if (hasPermission(player, "bukkitircd.link")) {
					if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
						if ((!IRCd.linkcompleted) && (!IRCd.isConnected())) {
							if (IRCd.connect()) player.sendMessage("§cSuccessfully connected to "+IRCd.remoteHost+" on port "+IRCd.remotePort);
							else player.sendMessage("§cFailed to connect to "+IRCd.remoteHost+" on port "+IRCd.remotePort);
						}
						else {
							if (IRCd.linkcompleted) player.sendMessage("§cAlready linked to "+IRCd.linkName+".");
							else player.sendMessage("§cAlready connected to "+IRCd.linkName+", but not linked.");
						}
					}
					else { player.sendMessage("§cNot in linking mode."); }
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreload") || commandName.equalsIgnoreCase("ireload")) {
				if (hasPermission(player, "bukkitircd.reload")) {
					pluginInit(true);
					log.info("[BukkitIRCd] Configuration file reloaded.");
					player.sendMessage("§cConfiguration file reloaded.");
				}
				else {
					player.sendMessage("§cYou don't have access to that command.");
				}
				return true;
			}
			else if (commandName.equalsIgnoreCase("rawsend")) {
				if (enableRawSend) {
					sender.sendMessage("§cRawsend is only usable from console.");
				}
				else { sender.sendMessage("§cRawsend is not enabled."); }
				return true;
			}
		}
		else {
			if (commandName.equalsIgnoreCase("irclist") || commandName.equalsIgnoreCase("ilist")) {
				String players[] = ircd.getIRCNicks();
				String allplayers = "";
				for (String curplayer : players) allplayers += "§7"+curplayer+"§f, ";
				sender.sendMessage("§9There are §c"+players.length+" §9users on IRC.");
				if (players.length > 0) sender.sendMessage(allplayers.substring(0,allplayers.length()-2));
				return true;
			}
			else if (commandName.equalsIgnoreCase("irckick") || commandName.equalsIgnoreCase("ikick")) {
				if (trimmedArgs.length > 0) {
					String reason = null;
					if (trimmedArgs.length > 1) reason = ircd.join(trimmedArgs, " ", 1);
					IRCUser ircuser = ircd.getIRCUser(trimmedArgs[0]);
					if (ircuser != null) {
						if (ircd.kickIRCUser(ircuser, ircd.serverName, ircd.serverName, ircd.serverHostName, reason, false)) sender.sendMessage("§cPlayer kicked.");
						else sender.sendMessage("§cFailed to kick player.");
						
					}
					else { sender.sendMessage("§cThat user is not online."); }
				}
				else { sender.sendMessage("§cPlease provide a nickname and optionally a kick reason."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircban") || commandName.equalsIgnoreCase("iban")) {
				if (trimmedArgs.length > 0) {
					String reason = null;
					IRCUser ircuser;
					String ban;
					String banType = null;
					if ((trimmedArgs[0].equalsIgnoreCase("ip")) || (trimmedArgs[0].equalsIgnoreCase("host")) || (trimmedArgs[0].equalsIgnoreCase("ident")) || (trimmedArgs[0].equalsIgnoreCase("nick"))) {
						ircuser = ircd.getIRCUser(trimmedArgs[1]);
						ban = trimmedArgs[1];
						banType = trimmedArgs[0];
						if (trimmedArgs.length > 2) reason = ircd.join(trimmedArgs, " ", 2);
					}
					else {
						ircuser = ircd.getIRCUser(trimmedArgs[0]);
						ban = trimmedArgs[0];
						if (trimmedArgs.length > 1) reason = ircd.join(trimmedArgs, " ", 1);
					}
					if (IRCd.wildCardMatch(ban, "*!*@*")) {
						// Full hostmask
						if (ircd.banIRCUser(ban, IRCd.serverName+"!"+IRCd.serverName+"@"+IRCd.serverHostName)) {
							if (ircd.msgIRCBan.length() > 0) getServer().broadcastMessage(ircd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (ircd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage("§cUser banned.");
						}
						else
							sender.sendMessage("§cUser is already banned.");
					}
					else if (countStr(ban, ".") == 3) { // It's an IP
						if (ircd.banIRCUser("*!*@"+ban, IRCd.serverName+"!"+IRCd.serverName+"@"+IRCd.serverHostName)) {
							if (ircd.msgIRCBan.length() > 0) getServer().broadcastMessage(ircd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (ircd.msgIRCBanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage("§cIP banned.");
						}
						else
							sender.sendMessage("§cIP is already banned.");
					}
					else {
						if (ircuser != null) {
							if (ircd.kickBanIRCUser(ircuser, "server", IRCd.serverName+"!"+IRCd.serverName+"@"+IRCd.serverHostName, reason, true, banType))
								sender.sendMessage("§cUser banned.");
							else
								sender.sendMessage("§cUser is already banned.");
						}
						else { sender.sendMessage("§cThat user is not online."); }
					}
				}
				else { sender.sendMessage("§cPlease provide a nickname or IP and optionally a ban reason."); return false; }
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircunban") || commandName.equalsIgnoreCase("iunban")) {
				if (trimmedArgs.length > 0) {
					String reason = null;
					String ban;
					String banType = null;

					ban = trimmedArgs[0];
					if (trimmedArgs.length > 1) reason = ircd.join(trimmedArgs, " ", 1);

					if (IRCd.wildCardMatch(ban, "*!*@*")) { // Full hostmask
						if (ircd.unBanIRCUser(ban, IRCd.serverName+"!"+IRCd.serverName+"@"+IRCd.serverHostName)) {
							if (ircd.msgIRCUnban.length() > 0) getServer().broadcastMessage(ircd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (ircd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage("§cUser unbanned.");
						}
						else
							sender.sendMessage("§cUser is not banned.");
					}
					else if (countStr(ban, ".") == 3) { // It's an IP
						if (ircd.unBanIRCUser("*!*@"+ban, IRCd.serverName+"!"+IRCd.serverName+"@"+IRCd.serverHostName)) {
							if (ircd.msgIRCUnban.length() > 0) getServer().broadcastMessage(ircd.msgIRCUnban.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							if ((dynmap != null) && (ircd.msgIRCUnbanDynmap.length() > 0)) dynmap.sendBroadcastToWeb("IRC", ircd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
							sender.sendMessage("§cIP unbanned.");
						}
						else
							sender.sendMessage("§cIP is not banned.");
					} 
					else { sender.sendMessage("§cInvalid hostmask."); return false; }

				}
				else { sender.sendMessage("§cPlease provide a IP/full hostmask."); return false; }
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircwhois") || commandName.equalsIgnoreCase("iwhois")) {
				if (trimmedArgs.length > 0) {
					IRCUser ircuser = ircd.getIRCUser(trimmedArgs[0]);
					if (ircuser != null) {
						String[] whois = ircd.getIRCWhois(ircuser);
						for (String whoisline : whois) sender.sendMessage(whoisline);
					}
					else { sender.sendMessage("§cThat user is not online."); }
				}
				else { sender.sendMessage("§cPlease provide a nickname."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircmsg") || commandName.equalsIgnoreCase("imsg") || commandName.equalsIgnoreCase("im")) {
				if (trimmedArgs.length > 1) {
					IRCUser ircuser = ircd.getIRCUser(trimmedArgs[0]);
					if (ircuser != null) {
						if (ircd.mode == Modes.STANDALONE) {
							ircd.writeTo(ircuser.nick, ":"+ircd.serverName+"!"+ircd.serverName+"@"+ircd.serverHostName+" PRIVMSG "+ircuser.nick+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 1),false));
							sender.sendMessage("§cMessage sent.");
						}
						else if (ircd.mode == Modes.INSPIRCD) {
							String UID = IRCd.getUIDFromIRCUser(ircuser);
							if (UID != null) {
								if (IRCd.linkcompleted) {
									IRCd.println(":"+IRCd.serverUID+" PRIVMSG "+UID+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 1), false));
									sender.sendMessage("§cMessage sent.");
								} else sender.sendMessage("§Failed to send message, not currently linked to IRC server.");
							}
							else {
								log.severe("UID not found in list: "+UID); // Log this as severe since it should never occur unless something is wrong with the code
								sender.sendMessage("§cFailed to send message, UID not found. This should not happen, please report it to Jdbye.");
							}
						}
					}
					else { sender.sendMessage("§cThat user is not online."); }
				}
				else { sender.sendMessage("§cPlease provide a nickname and a message."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreply") || commandName.equalsIgnoreCase("ireply") || commandName.equalsIgnoreCase("ir")) {
				if (trimmedArgs.length > 0) {
					String lastReceivedFrom = lastReceived.get("@CONSOLE@");
					if (lastReceivedFrom == null) {
						sender.sendMessage("§cThere are no messages to reply to!");
					}
					else {
						IRCUser ircuser = ircd.getIRCUser(lastReceivedFrom);
						if (ircuser != null) {
							if (ircd.mode == Modes.STANDALONE) {
								ircd.writeTo(ircuser.nick, ":"+ircd.serverName+"!"+ircd.serverName+"@"+ircd.serverHostName+" PRIVMSG "+ircuser.nick+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 0),false));
								sender.sendMessage("§cMessage sent to "+lastReceivedFrom+".");
							}
							else if (ircd.mode == Modes.INSPIRCD) {
								String UID = IRCd.getUIDFromIRCUser(ircuser);
								if (UID != null) {
									if (IRCd.linkcompleted) {
										IRCd.println(":"+IRCd.serverUID+" PRIVMSG "+UID+" :"+ircd.convertColors(ircd.join(trimmedArgs, " ", 0), false));
										sender.sendMessage("§cMessage sent to "+lastReceivedFrom+".");
									} else sender.sendMessage("§cFailed to send message, not currently linked to IRC server.");
								}
								else {
									log.severe("UID not found in list: "+UID); // Log this as severe since it should never occur unless something is wrong with the code
									sender.sendMessage("§cFailed to send message, UID not found. This should not happen, please report it to Jdbye.");
								}
							}
						}
						else { sender.sendMessage("§cThat user is not online."); }
					}
				}
				else { sender.sendMessage("§cPlease provide a nickname and a message."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("irctopic") || commandName.equalsIgnoreCase("itopic")) {
				if (trimmedArgs.length > 0) {
					String topic = ircd.join(trimmedArgs, " ", 0);
					ircd.setTopic(ircd.convertColors(topic, false), ircd.serverName, ircd_servername+"!"+ircd_servername+"@"+ircd_serverhostname);
					sender.sendMessage("§cTopic set to "+topic);
				}
				else { sender.sendMessage("§cPlease provide a topic to set."); return false; }		
				return true;
			}
			else if (commandName.equalsIgnoreCase("irclink") || commandName.equalsIgnoreCase("ilink")) {
				if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
					if ((!IRCd.linkcompleted) && (!IRCd.isConnected())) {
						if (IRCd.connect()) sender.sendMessage("§cSuccessfully connected to "+IRCd.remoteHost+" on port "+IRCd.remotePort);
						else sender.sendMessage("§cFailed to connect to "+IRCd.remoteHost+" on port "+IRCd.remotePort);
					}
					else {
						if (IRCd.linkcompleted) sender.sendMessage("§cAlready linked to "+IRCd.linkName+".");
						else sender.sendMessage("§cAlready connected to "+IRCd.linkName+", but not linked.");
					}
				}
				else { sender.sendMessage("§cNot in linking mode."); }
				return true;
			}
			else if (commandName.equalsIgnoreCase("ircreload") || commandName.equalsIgnoreCase("ireload")) {
				pluginInit(true);
				log.info("[BukkitIRCd] Configuration file reloaded.");
				sender.sendMessage("§cConfiguration file reloaded.");
				return true;
			}
			else if (commandName.equalsIgnoreCase("rawsend")) {
				if (enableRawSend) {
					if (trimmedArgs.length > 0) {
						if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
							if (IRCd.println(IRCd.join(trimmedArgs, " ", 0))) sender.sendMessage("§cCommand sent to IRC server link.");
							else sender.sendMessage("§cFailed to send command to IRC server link, not currently linked.");
						}
					}
					else { sender.sendMessage("§cPlease provide a command to send."); return false; }
				}
				else { sender.sendMessage("§cRawsend is not enabled."); }
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
		else try {
			if (permissionHandler != null) {
				if (this.permissionHandler.has(player, permission)) {
					//log.info("[BukkitIRCd] "+player.getName()+" has permission "+permission+" (Permissions 2.x)");
					return true;
				}
			}
		} catch (Exception e) { }
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
		} catch (Exception e) { log.severe("[BukkitIRCd] Unable to parse string array "+IRCd.join(sarray, " ", 0)+", invalid number. "+e); }
		return def;
	}
}

