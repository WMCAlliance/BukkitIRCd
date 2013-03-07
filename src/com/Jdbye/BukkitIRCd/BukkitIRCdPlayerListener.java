package com.Jdbye.BukkitIRCd;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handle events for all Player related events
 * @author Jdbye
 */
public class BukkitIRCdPlayerListener implements Listener {
	private final BukkitIRCdPlugin plugin;

	public BukkitIRCdPlayerListener(BukkitIRCdPlugin instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
            String mode = "";
            Player player = event.getPlayer();
            //if (plugin.hasPermission(player, "bukkitircd.mode.owner")) mode += "~";
            //if (plugin.hasPermission(player, "bukkitircd.mode.protect")) mode += "&";
            if (plugin.hasPermission(player, "bukkitircd.mode.op")) mode += "@";
            if (plugin.hasPermission(player, "bukkitircd.mode.halfop")) mode += "%";
            if (plugin.hasPermission(player, "bukkitircd.mode.voice")) mode += "+";
            //if (plugin.hasPermission("bukkitircd.mode.owner")) mode += "~";
            //if (plugin.hasPermission("bukkitircd.mode.protect")) mode += "&";
            //if (plugin.hasPermission("bukkitircd.mode.op")) mode += "@";
            //if (plugin.hasPermission("bukkitircd.mode.halfop")) mode += "%";
            //if (plugin.hasPermission("bukkitircd.mode.voice")) mode += "+";
            IRCd.addBukkitUser(mode,player);
    }

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		String name = event.getPlayer().getName();
		plugin.removeLastReceivedBy(name);
		IRCd.removeBukkitUser(IRCd.getBukkitUser(name));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent event)
	{
		if (event.isCancelled()) return;
		String name = event.getPlayer().getName();
		plugin.removeLastReceivedBy(name);
		IRCd.kickBukkitUser(event.getReason(), IRCd.getBukkitUser(event.getPlayer().getName()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		if (event.isCancelled()) return;
		IRCd.updateBukkitUserIdleTime(IRCd.getBukkitUser(event.getPlayer().getName()));
		if (IRCd.mode == Modes.STANDALONE) {
			IRCd.writeAll(IRCd.convertColors(event.getMessage(),false),event.getPlayer());
		}
		else {
			BukkitPlayer bp;
			if ((bp = IRCd.getBukkitUserObject(event.getPlayer().getName())) != null) {
				if (IRCd.linkcompleted) IRCd.println(":" + bp.getUID() + " PRIVMSG " + IRCd.channelName + " :" + IRCd.convertColors(event.getMessage(), false));
			}
		}
		event.setMessage(IRCd.stripFormatting(event.getMessage()));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
	{
		if (event.isCancelled()) return;
		IRCd.updateBukkitUserIdleTime(IRCd.getBukkitUser(event.getPlayer().getName()));

		String[] split = event.getMessage().split(" ");
		if (split.length > 1) {
			// ACTION/EMOTE can't be claimed, so use onPlayerCommandPreprocess
			if (split[0].equalsIgnoreCase("/me")) {
				if (IRCd.mode == Modes.STANDALONE) {
					IRCd.writeAll((char)1 + "ACTION " + IRCd.convertColors(IRCd.join(event.getMessage().split(" ")," ",1),false) + (char)1, event.getPlayer());
				}
				else {
					BukkitPlayer bp;
					if ((bp = IRCd.getBukkitUserObject(event.getPlayer().getName())) != null) {
						if (IRCd.linkcompleted) IRCd.println(":" + bp.getUID() + " PRIVMSG " + IRCd.channelName + " :" + (char)1 + "ACTION " + IRCd.convertColors(IRCd.join(event.getMessage().split(" ")," ",1), false) + (char)1);
					}
				}
				event.setMessage(IRCd.stripFormatting(event.getMessage()));
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.isCancelled()) return;
		Player p = event.getPlayer();
		IRCd.updateBukkitUserIdleTimeAndWorld(IRCd.getBukkitUser(p.getName()), p.getWorld().getName());
	}
}

