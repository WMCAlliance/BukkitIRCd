package com.Jdbye.BukkitIRCd.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.BukkitPlayer;
import com.Jdbye.BukkitIRCd.BukkitUserManagement;
import com.Jdbye.BukkitIRCd.IRCFunctionality;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Modes;
import com.Jdbye.BukkitIRCd.Configuration.Config;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;

/**
 Handle events for all Player related events
 <p>
 */
public class BukkitIRCdPlayerListener implements Listener {

    private final BukkitIRCdPlugin plugin;

    public BukkitIRCdPlayerListener(BukkitIRCdPlugin instance) {
	plugin = instance;
    }

    /**
     The listener for the player death event, used for displaying death messages in the IRC channel
     <p>
     @param event The event itself, used to get the death message
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
	if (!Config.isIrcdBroadCastDeathMessages()) {
	    return;
	}
	String message = event.getDeathMessage().replace(event.getEntity().getName(), event.getEntity().getName() + Config.getIrcdIngameSuffix());

	if (!Config.isIrcdColorDeathMessagesEnabled()) {
	    message = ChatColor.stripColor(message);
	} else {
	    message = ChatUtils.convertColors(message, false);
	}
	if (IRCd.mode == Modes.INSPIRCD) {
	    if (IRCd.isLinkcompleted()) {
		IRCFunctionality.privmsg(IRCd.serverUID, Config.getIrcdChannel(), message);
	    }
	} else {
	    IRCFunctionality.writeAll(message);
	}

    }

    /**
     Game command tracker, to track kicks
     <p>
     @param event Command event itself
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {

	// Console Command Listener
	final String[] split = event.getCommand().split(" ");

	if (split.length > 1) {

	    if (Config.getKickCommands().contains(split[0].toLowerCase())) {

		// PlayerKickEvent does not give kicker, so we listen to kick commands instead
		// TODO I think kicker is provided now - ~@Mu5
		// Though it doesn't look like it - ~@WizardCM
		final StringBuilder s = new StringBuilder(300);
		for (int i = 2; i < split.length; i++) {
		    s.append(split[i]).append(" ");
		}

		final String kickMessage = s.toString();
		final String kickedPlayer = split[1];
		plugin.removeLastReceivedBy(kickedPlayer);
		BukkitUserManagement.kickUser(kickMessage,
			BukkitUserManagement.getUser(kickedPlayer));
	    }

	    if (split[0].equalsIgnoreCase("say")) {
		final StringBuilder s = new StringBuilder(300);
		for (int i = 1; i < split.length; i++) {
		    s.append(split[i]).append(" ");
		}

		String message = s.toString();

		message = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
		if (Config.isIrcdColorSayMessageEnabled()) {
		    message = (char) 3 + "13" + message;
		}
		if (IRCd.mode == Modes.INSPIRCD) {
		    if (IRCd.isLinkcompleted()) {
			IRCFunctionality.privmsg(IRCd.serverUID, Config.getIrcdChannel(),
				message);
		    }
		} else {
		    IRCFunctionality.writeAll(message);
		}
	    }
	}
    }

    /**
     One of the two most important events to be tracked, the player join event. This creates a user and adds it to the IRC channel.
     <p>
     @param event The event itself containing the player and their name
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
	final Player player = event.getPlayer();
	final String mode = plugin.computePlayerModes(player);
	// TODO add ability to disable hosts and IP lookups altogether to minimise network traffic
	BukkitUserManagement.addBukkitUser(mode, player);
	/*Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
	 @Override
	 public void run() {
                    
	 }
	 }, 20);*/
    }

    /**
     The other most important event to be tracked, the player quit event. This removes the user from IRC.
     <p>
     @param event The event itself containing the name of the player to remove
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
	final String name = event.getPlayer().getName();
	plugin.removeLastReceivedBy(name);
	BukkitUserManagement.removeBukkitUser(BukkitUserManagement.getUser(name));
    }

    /**
     The core chat event of Minecraft, tracked to forward messages to IRC
     <p>
     @param event The event itself, containing the sender and message
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
	BukkitUserManagement.updateUserIdleTime(BukkitUserManagement.getUser(event.getPlayer().getName()));

	switch (IRCd.mode) {
	    case STANDALONE:
		IRCFunctionality.writeAll(ChatUtils.convertColors(event.getMessage(), false), event.getPlayer());
		break;

	    case INSPIRCD:
		final BukkitPlayer bp = BukkitUserManagement.getUserObject(event.getPlayer().getName());
		if (bp != null && IRCd.isLinkcompleted()) {
		    IRCFunctionality.privmsg(bp.getUID(), Config.getIrcdChannel(), ChatUtils.convertColors(event.getMessage(), false));
		}
		break;
	}

	event.setMessage(ChatUtils.stripIRCFormatting(event.getMessage()));
    }

    /**
     Used instead of tracking commands themselves because it's more accurate, this is for the /me and /say ingame commands, to be forwarded to the server.
     <p>
     @param event The event itself, containing the command and sender
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
	BukkitUserManagement.updateUserIdleTime(BukkitUserManagement.getUser(event.getPlayer().getName()));

	String[] split = event.getMessage().split(" ");
	if (split.length > 1) {
	    // ACTION/EMOTE can't be claimed, so use onPlayerCommandPreprocess
	    if (split[0].equalsIgnoreCase("/me")) {
		switch (IRCd.mode) {
		    case STANDALONE:
			IRCFunctionality.writeAll((char) 1 + "ACTION " + ChatUtils.convertColors(ChatUtils.join(event.getMessage().split(" "), " ", 1), false) + (char) 1, event.getPlayer());
			break;

		    case INSPIRCD:
			final BukkitPlayer bp = BukkitUserManagement.getUserObject(event.getPlayer().getName());
			if (bp != null && IRCd.isLinkcompleted()) {
			    IRCFunctionality.action(bp.getUID(), Config.getIrcdChannel(), ChatUtils.convertColors(ChatUtils.join(event.getMessage().split(" "), " ", 1), false));
			}
			break;
		}
		event.setMessage(ChatUtils.stripIRCFormatting(event.getMessage()));
	    }

	    if (Config.getKickCommands().contains(
		    split[0].substring(1).toLowerCase())) {
		// PlayerKickEvent does not give kicker, so we listen to kick // commands instead
		if (event.getPlayer().hasPermission("bukkitircd.kick")) {
		    final StringBuilder s = new StringBuilder(300);
		    for (int i = 2; i < split.length; i++) {
			s.append(split[i]).append(" ");
		    }
		    final String kickMessage = s.toString();
		    final String kickedPlayer = split[1];
		    if ((BukkitUserManagement.getUserObject(event.getPlayer().getName())) != null) {
			plugin.removeLastReceivedBy(kickedPlayer);
			BukkitUserManagement.kickBukkitUser(kickMessage, BukkitUserManagement.getUser(kickedPlayer), BukkitUserManagement.getUser(event.getPlayer().getName()));
			BukkitUserManagement.removeBukkitUser(BukkitUserManagement.getUser(kickedPlayer));
		    }

		}
	    }

	    if (split[0].equalsIgnoreCase("/say")) {
		if (event.getPlayer().hasPermission("bukkit.command.say")) {

		    final StringBuilder s = new StringBuilder(300);
		    for (int i = 1; i < split.length; i++) {
			s.append(split[i]).append(" ");
		    }

		    String message = s.toString();
		    message = ChatColor.stripColor(message);
		    message = ChatColor.translateAlternateColorCodes('&', message);
		    if (Config.isIrcdColorSayMessageEnabled()) {
			message = (char) 3 + "13" + message;
		    }

		    switch (IRCd.mode) {
			case INSPIRCD:
			    if (IRCd.isLinkcompleted()) {
				IRCFunctionality.privmsg(IRCd.serverUID, Config.getIrcdChannel(), message);
			    }
			    break;
			case STANDALONE:
			    IRCFunctionality.writeAll(message);
			    break;
		    }
		}
	    }
	}
    }

    /**
     Tracks player movement to notify IRC whois of whether the player is AFK
     <p>
     @param event The move event itself, containing the player
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
	final Player p = event.getPlayer();
	BukkitUserManagement.updateUserIdleTimeAndWorld(BukkitUserManagement.getUser(p.getName()), p.getWorld().getName());
    }
}
