package com.Jdbye.BukkitIRCd;

import com.Jdbye.BukkitIRCd.commands.IRCBanCommand;
import com.Jdbye.BukkitIRCd.commands.IRCKickCommand;
import com.Jdbye.BukkitIRCd.commands.IRCLinkCommand;
import com.Jdbye.BukkitIRCd.commands.IRCListCommand;
import com.Jdbye.BukkitIRCd.commands.IRCMsgCommand;
import com.Jdbye.BukkitIRCd.commands.IRCReloadCommand;
import com.Jdbye.BukkitIRCd.commands.IRCReplyCommand;
import com.Jdbye.BukkitIRCd.commands.IRCTopicCommand;
import com.Jdbye.BukkitIRCd.commands.IRCUnbanCommand;
import com.Jdbye.BukkitIRCd.commands.IRCWhoisCommand;
import com.Jdbye.BukkitIRCd.commands.RawsendCommand;
import com.Jdbye.BukkitIRCd.configuration.Bans;
import com.Jdbye.BukkitIRCd.configuration.Config;
import com.Jdbye.BukkitIRCd.configuration.MOTD;
import com.Jdbye.BukkitIRCd.configuration.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
	
	public final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();

	public static BukkitIRCdPlugin thePlugin = null;

	private static Date curDate = new Date();
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	public static String ircd_creationdate = dateFormat.format(curDate);

	public Map<String, String> lastReceived = new HashMap<String, String>();


	public static String ircdVersion;

	public boolean dynmapEventRegistered = false;
<<<<<<< HEAD

	public static final Logger log = Logger.getLogger("Minecraft");
=======
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

	public static boolean use_host_mask = false;
	public static String mask_prefix = "BukkitIRCd-";
	public static String mask_suffix = ".IP";
	public static String mask_key = "0x00000000";

	public static List<String> kickCommands = Arrays.asList("/kick");
	public static final Logger log = Logger.getLogger("Minecraft");

	public boolean enableRawSend = false;
>>>>>>> development

	public static DynmapAPI dynmap = null;
	

	static IRCd ircd = null;
	private Thread thr = null;

	public BukkitIRCdPlugin() {
		thePlugin = this;
	}
	
	public static Config config = null;
	public void onEnable() {
		// Register our events
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);
		pm.registerEvents(this.serverListener, this);
		
		PluginDescriptionFile pdfFile = getDescription();
		ircdVersion = pdfFile.getName() + " " + pdfFile.getVersion() + " by " + pdfFile.getAuthors().get(0);
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
		
		log.info(ircdVersion + " is enabled!");
		
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
		//File configFile = new File(getDataFolder(), "config.yml");
		Config.saveConfiguration();

		Bans.writeBans();

		log.info(ircdVersion + " is disabled!");
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
		
		Config.reloadConfiguration();
		
		Messages.saveMessages();
		
		Bans.enableBans();
		
		MOTD.enableMOTD();
		MOTD.loadMOTD();

		setupDynmap();
		
		ircd = new IRCd();
		
<<<<<<< HEAD
		Messages.loadMessages(ircd);

		Bans.loadBans();
		
=======
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

		IRCd.useHostMask = use_host_mask;
		IRCd.maskKey = mask_key;
		IRCd.maskPrefix = mask_prefix;
		IRCd.maskSuffix = mask_suffix;

		loadBans();
>>>>>>> development
		IRCd.bukkitPlayers.clear();

		// Set players to different IRC modes based on permission
		for (Player player : getServer().getOnlinePlayers()) {
			StringBuffer mode = new StringBuffer();
            if (player.hasPermission("bukkitircd.mode.owner")){
            	if (Config.isDebugModeEnabled()) {
            		log.info("Add mode +q for " + player.getName());
            	}
            	mode.append("~");
            }
            else if (player.hasPermission("bukkitircd.mode.protect")){
            	if (Config.isDebugModeEnabled()) {
            		log.info("Add mode +a for " + player.getName());
            	}
            	mode.append("&");
            }
            else if (player.hasPermission("bukkitircd.mode.op")){
            	if (Config.isDebugModeEnabled()) {
            		log.info("Add mode +o for " + player.getName());
            	}
            	mode.append("@");
            }
            else if (player.hasPermission("bukkitircd.mode.halfop")){
            	if (Config.isDebugModeEnabled()) {
            		log.info("Add mode +h for " + player.getName());
            	}
            	mode.append("%");
            }
            else if (player.hasPermission("bukkitircd.mode.voice")){
            	if (Config.isDebugModeEnabled()) {
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
			log.info("[BukkitIRCd] Hooked into Dynmap." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin301." : ""));
		}
	}

	public void unloadDynmap() {
		if (BukkitIRCdPlugin.dynmap != null) {
			BukkitIRCdPlugin.dynmap = null;
<<<<<<< HEAD
			log.info("[BukkitIRCd] Dynmap plugin lost." + (Config.isDebugModeEnabled() ? " Error Code BukkitIRCdPlugin308." : ""));
=======
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

			use_host_mask = config.getBoolean("use-host-mask", use_host_mask);
			mask_prefix = config.getString("host-mask-prefix", mask_prefix);
			mask_suffix = config.getString("host-mask-suffix", mask_suffix);
			mask_key = config.getString("host-mask-key", mask_key);

			if (operpass.length() == 0) ircd_operpass = "";
			else if (operpass.startsWith("~")) { ircd_operpass = operpass.substring(1); }
			else { ircd_operpass = Hash.compute(operpass, HashType.SHA_512); }

			log.info("[BukkitIRCd] Loaded configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin363." : ""));

			saveConfig();
			log.info("[BukkitIRCd] Saved initial configuration file." + (IRCd.debugMode ? " Code BukkitIRCdPlugin365." : ""));
		}
		catch (Exception e) {
			log.info("[BukkitIRCd] Failed to load configuration file: " + e.toString());
>>>>>>> development
		}
	}


	/**
     * Converts color codes to processed codes
     *
     * @param message Message with raw color codes
     * @return String with processed colors
	 * Thanks to Jkcclemens (of RoyalDev) for this code
     */
    public static String colorize(final String message) {
        if (message == null) return null;
        return message.replaceAll("&([a-f0-9k-or])", "\u00a7$1");
    }
	
	
   

	// Load the bans file
	



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
	
	public static int[] convertStringArrayToIntArray(String[] sarray, int[] def) {
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

