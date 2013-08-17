package com.Jdbye.BukkitIRCd;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;

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
	private BukkitIRCdDynmapListener dynmapListener = null;

	public static BukkitIRCdPlugin thePlugin = null;

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");

	public Map<String, String> lastReceived = new HashMap<String, String>();


	public static String ircdVersion;

	public boolean dynmapEventRegistered = false;

	public static final Logger log = Logger.getLogger("Minecraft");

	public static DynmapAPI dynmap = null;


	static IRCd ircd = null;
	private Thread thr = null;

	public BukkitIRCdPlugin() {
		thePlugin = this;
	}

	public static Config config = null;
	@Override
	public void onEnable() {
		saveDefaultConfig();

		// Register our events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.playerListener, this);

		PluginDescriptionFile pdfFile = getDescription();
		ircdVersion = pdfFile.getName() + " " + pdfFile.getVersion() + " by " + pdfFile.getAuthors().get(0);
		setupMetrics();
		pluginInit();

		getCommand("ircban").setExecutor(new IRCBanCommand(this));
		getCommand("irckick").setExecutor(new IRCKickCommand());
		getCommand("irclist").setExecutor(new IRCListCommand());
		getCommand("ircunban").setExecutor(new IRCUnbanCommand(this));
		getCommand("ircwhois").setExecutor(new IRCWhoisCommand());
		getCommand("ircmsg").setExecutor(new IRCMsgCommand());
		getCommand("ircreply").setExecutor(new IRCReplyCommand(this));
		getCommand("irctopic").setExecutor(new IRCTopicCommand());
		getCommand("irclink").setExecutor(new IRCLinkCommand(this));
		getCommand("ircreload").setExecutor(new IRCReloadCommand(this));
		getCommand("rawsend").setExecutor(new RawsendCommand());

		log.info(ircdVersion + " is enabled!");
	}

	@Override
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
		Config.saveConfiguration();

		Bans.enableBans();

		MOTD.enableMOTD();
		MOTD.loadMOTD();

		setupDynmap();

		ircd = new IRCd();

		Messages.loadMessages(ircd);
		IRCd.bukkitversion = getServer().getVersion();

		Bans.loadBans();

		IRCd.bukkitPlayers.clear();

		// Set players to different IRC modes based on permission
		for (final Player player : getServer().getOnlinePlayers()) {
            final String mode = computePlayerModes(player);
			IRCd.addBukkitUser(mode, player);
		}

		thr = new Thread(ircd);
		thr.start();

	}

	// check for Dynmap, and if it's installed, register events and hooks
	private void setupDynmap() {
		if (BukkitIRCdPlugin.dynmap == null) {
			final PluginManager pm = getServer().getPluginManager();
			final Plugin plugin = pm.getPlugin("dynmap");

			if (plugin != null) {
				if (dynmapListener == null) {
					dynmapListener = new BukkitIRCdDynmapListener();
				}

				if (!dynmapEventRegistered) {
					pm.registerEvents(dynmapListener, this);
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
			log.info("[BukkitIRCd] Dynmap plugin lost." + (Config.isDebugModeEnabled() ? " Error Code BukkitIRCdPlugin308." : ""));
		}
	}


	/**
     * Converts color codes to processed codes
     *
     * @param message Message with raw color codes
     * @return String with processed colors
     */
    public static String colorize(final String message) {
        if (message == null) return null;
        return ChatColor.translateAlternateColorCodes('&', message);
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

	/**
	 * @param player
	 * @return
	 */
	String computePlayerModes(final Player player) {
		final StringBuffer mode = new StringBuffer(5);

		final char[] modeSigils = { '~', '&', '@', '%', '+' };
		final String[] modeNames = { "owner", "protect", "op", "halfop", "voice" };
		final boolean debug = Config.isDebugModeEnabled();

		for (int i = 0; i < modeSigils.length; i++) {
			if (player.hasPermission("bukkitircd.mode." + modeNames[i])) {
				if (debug) {
					BukkitIRCdPlugin.log.info("Add mode +" + modeSigils[i]
							+ " for player " + player.getName());
				}

				mode.append(modeSigils[i]);

				if (!Config.isIrcdRedundantModes()) {
					break;
				}
			}
		}
		return mode.toString();
	}
}

