package com.Jdbye.BukkitIRCd;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;

import com.Jdbye.BukkitIRCd.Commands.BukkitIRCdCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCBanCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCKickCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCLinkCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCListCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCMsgCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCReloadCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCReplyCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCTopicCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCUnbanCommand;
import com.Jdbye.BukkitIRCd.Commands.IRCUserCommands;
import com.Jdbye.BukkitIRCd.Commands.IRCWhoisCommand;
import com.Jdbye.BukkitIRCd.Commands.RawsendCommand;
import com.Jdbye.BukkitIRCd.Configuration.Bans;
import com.Jdbye.BukkitIRCd.Configuration.Config;
import com.Jdbye.BukkitIRCd.Configuration.MOTD;
import com.Jdbye.BukkitIRCd.Configuration.Messages;
import com.Jdbye.BukkitIRCd.Listeners.BukkitIRCdDynmapListener;
import com.Jdbye.BukkitIRCd.Listeners.BukkitIRCdPlayerListener;

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

	/**
     The core enable task, which loads configuration, registers events and commands, and depending on config, either launches an IRCd instance or connects to an external server
	 */
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

		getCommand("irc").setExecutor(new IRCUserCommands());
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
		getCommand("bircd").setExecutor(new BukkitIRCdCommand());

		log.info(ircdVersion + " is now enabled");
		new BukkitRunnable() {

			@Override
			public void run() {
				Utils.println("The server is still loading, so you cannot connect yet!");
			}

		}.runTaskLater(BukkitIRCdPlugin.thePlugin, 20);
	}

	/**
     Core disable task, shuts down IRCd instance, saves config.
	 */
	@Override
	public void onDisable() {
		if (ircd != null) {
			ircd.running = false;
			IRCFunctionality.disconnectAll();
			ircd = null;
		}
		if (thr != null) {
			thr.interrupt();
			thr = null;
		}

		dynmapEventRegistered = false;
		// File configFile = new File(getDataFolder(), "config.yml");
		Config.saveConfiguration();

		Bans.writeBans();

		log.info(ircdVersion + " is now disabled!");
	}

	private void pluginInit() {
		pluginInit(false);
	}

	/**
     Initialises the plugin. On a reload, it restarts the IRCd.
     <p>
     @param reload	Whether it's a reload or it's the first initialisation of the session.
	 */
	public void pluginInit(boolean reload) {
		if (reload) {
			if (ircd != null) {
				ircd.running = false;
				IRCFunctionality.disconnectAll("Reloading configuration.");
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

		if (IRCd.globalNameIgnoreList == null) {
			IRCd.globalNameIgnoreList = new ArrayList<String>();
		}

		// TODO Ignore List loading
		/*try
	 {
	 Scanner ignoreListScanner = new Scanner(new File(getDataFolder(), "ignoreList.yml"));

	 while (ignoreListScanner.hasNext()){
	 IRCd.globalNameIgnoreList.add(ignoreListScanner.next());
	 }

	 ignoreListScanner.close();
	 }
	 catch ( java.io.FileNotFoundException e )
	 {
	 // we don't care if it exists or not currently
	 // if it doesn't exist, everything carries on as normal

	 // this is seperated should we wish to add to it...
	 }
	 catch (Exception e)
	 {
	 // same as FileNotFoundException
	 }*/
		Messages.loadMessages(ircd);
		IRCd.bukkitversion = getServer().getVersion();

		Bans.loadBans();

		IRCd.bukkitPlayers.clear();

		// Set players to different IRC modes based on permission
		for (final Player player : getServer().getOnlinePlayers()) {
			final String mode = computePlayerModes(player);
			BukkitUserManagement.addBukkitUser(mode, player);
		}

		thr = new Thread(ircd);
		thr.start();

	}

	/**
     Check for Dynmap, and if it's installed, register events and hooks.
	 */
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
				log.info("[BukkitIRCd] Hooked into Dynmap.");
			}
		}
	}

	/**
     Unloads Dynmap API links
	 */
	public void unloadDynmap() {
		if (BukkitIRCdPlugin.dynmap != null) {
			BukkitIRCdPlugin.dynmap = null;
			log.info("[BukkitIRCd] Dynmap plugin unloaded.");
		}
	}

	/**
     Primarily used for certain console things in IRCd and ClientConnection, contains senders and receivers of messages.
     <p>
     @param sender The sender of a message (probably)
     @param recipeient The recipient of a message (probably)
	 */
	public void setLastReceived(String recipient, String sender) {
		synchronized (csLastReceived) {
			lastReceived.put(recipient, sender);
		}
	}

	/**
     Updates the stored sender/recipient combo.
     <p>
     @param oldSender Old sender to search and replace
     @param newSender The replacement sender name
	 */
	public void updateLastReceived(String oldSender, String newSender) {
		List<String> update = new ArrayList<String>();
		synchronized (csLastReceived) {
			for (Map.Entry<String, String> lastReceivedEntry : lastReceived.entrySet()) {
				if (lastReceivedEntry.getValue().equalsIgnoreCase(oldSender)) {
					update.add(lastReceivedEntry.getKey());
				}
			}
			for (String toUpdate : update) {
				lastReceived.put(toUpdate, newSender);
			}
		}
	}

	/**
     Removes the sender/recipient combo.
     <p>
     @param sender Name to remove
	 */
	public void removeLastReceivedBy(String recipient) {
		synchronized (csLastReceived) {
			lastReceived.remove(recipient);
		}
	}

	/**
     Remove the last known sender from the sender/recipient combo
     <p>
     @param sender Name to remove
	 */
	public void removeLastReceivedFrom(String sender) {
		List<String> remove = new ArrayList<String>();
		synchronized (csLastReceived) {
			for (Map.Entry<String, String> lastReceivedEntry : lastReceived.entrySet()) {
				if (lastReceivedEntry.getValue().equalsIgnoreCase(sender)) {
					remove.add(lastReceivedEntry.getKey());
				}
			}
			for (String toRemove : remove) {
				lastReceived.remove(toRemove);
			}
		}
	}

	/**
     Counts how many times a certain character appears in a string. CURRENTLY ONLY USED FOR BANS - TO CHECK IF THE STRING IS AN IP (THOUGH ONLY LIMITED TO IPV4
     // TODO This is gross. Only works for IPV4. Fix it.
     <p>
     @param text The name/IP to read
     @param search The character to search for and count
     <p>
     @return
	 */
	public static int countStr(String text, String search) {
		int count = 0;
		for (int fromIndex = 0; fromIndex > -1; count++) {
			fromIndex = text.indexOf(search, fromIndex + ((count > 0) ? 1 : 0));
		}
		return count - 1;
	}

	/**
	 Setup PluginMetrics
	 */
	// TODO Custom Metrics data (Such as IRC server type)
	private void setupMetrics() {
		try {
			Metrics metrics = new Metrics(this);
			Metrics.Graph features = metrics.createGraph("IRC Server Type");

			features.addPlotter(new Metrics.Plotter("INSPIRCD") {
				@Override
				public int getValue() {
					if (Config.getMode().equalsIgnoreCase("inspire") || Config.getMode().equalsIgnoreCase("inspircd"))
						return 1;
					return 0;
				}
			});

			features.addPlotter(new Metrics.Plotter("STANDALONE") {
				@Override
				public int getValue() {
					if (Config.getMode().equalsIgnoreCase("standalone"))
						return 1;
					return 0;
				}
			});            
			metrics.start();
		} catch (IOException e) {
			log.info("Unable to load metrics");
		}
	}

	/**
     Determines what IRC modes a player should have, and gives it to them. If redundant modes are disabled, only the first is stored.
     <p>
     @param player The player who the modes shall belong to
     <p>
     @return List of modes they should have, based on permissions
     // TODO Enum modes
	 */
	public String computePlayerModes(final Player player) {
		final StringBuffer mode = new StringBuffer(5);

		final char[] modeSigils = {'~', '&', '@', '%', '+'};
		final String[] modeNames = {"owner", "protect", "op", "halfop", "voice"};

		for (int i = 0; i < modeSigils.length; i++) {
			if (player.hasPermission("bukkitircd.mode." + modeNames[i])) {
				mode.append(modeSigils[i]);
				if (!Config.isIrcdRedundantModes()) {
					break;
				}
			}
		}
		if (Config.isDebugModeEnabled()) {
			BukkitIRCdPlugin.log.info("Add mode +" + mode.toString() + " for player " + player.getName());
		}
		return mode.toString();
	}
}
