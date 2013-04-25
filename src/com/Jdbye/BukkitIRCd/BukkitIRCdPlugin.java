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
import com.Jdbye.BukkitIRCd.configuration.*;

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

	public static String mode = "standalone";

	public static BukkitIRCdPlugin thePlugin = null;

	private static Date curDate = new Date();
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	public static String ircd_creationdate = dateFormat.format(curDate);

	public Map<String, String> lastReceived = new HashMap<String, String>();


	public static String ircd_version;

	public boolean dynmapEventRegistered = false;

	public static final Logger log = Logger.getLogger("Minecraft");

	public static DynmapAPI dynmap = null;
	

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
		//File configFile = new File(getDataFolder(), "config.yml");
		Config.saveSettings();

		Bans.writeBans();

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
		
		Config.reloadingConfig();
		
		Messages.saveMessages();
		
		Bans.enableBans();
		
		MOTD.enableMOTD();
		MOTD.loadMOTD();

		setupDynmap();
		
		ircd = new IRCd();
		
		Messages.loadMessages(ircd);

		Config.initConfig();
		
		Bans.loadBans();
		
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
	
	
    MOTD.loadMOTD();

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

