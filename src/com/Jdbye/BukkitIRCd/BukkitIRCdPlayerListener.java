package com.Jdbye.BukkitIRCd;

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

import com.Jdbye.BukkitIRCd.configuration.Config;

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
	public void onPlayerDeath(PlayerDeathEvent event){
		if (!Config.isIrcdBroadCastDeathMessages()){
			return;
		}
		String message = event.getDeathMessage().replace(event.getEntity().getName(),event.getEntity().getName()+Config.getIrcdIngameSuffix());

		if(!Config.isIrcdColorDeathMessagesEnabled()){
			message = ChatColor.stripColor(message);
		}
		else {
			message = IRCd.convertColors(message,false);
		}
		if(IRCd.mode == Modes.INSPIRCD){
			if (IRCd.isLinkcompleted()) {
				IRCd.privmsg(IRCd.serverUID, Config.getIrcdChannel(), message);
			}
		}
		else {
			IRCd.writeAll(message);
		}

	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onServerCommand(ServerCommandEvent event){

		//Console Command Listener
		final String[] split = event.getCommand().split(" ");

		if (split.length > 1){

			if (Config.getKickCommands().contains(split[0].toLowerCase())){

				//PlayerKickEvent does not give kicker, so we listen to kick commands instead
				final StringBuilder s = new StringBuilder(300);
				for(int i = 2; i < split.length;i++){
					 s.append(split[i]).append(" ");
				}

				final String kickMessage = s.toString();
				final String kickedPlayer = split[1];
				plugin.removeLastReceivedBy(kickedPlayer);
				IRCd.kickBukkitUser(kickMessage, IRCd.getBukkitUser(kickedPlayer));
			}

			if (split[0].equalsIgnoreCase("say")){
				final StringBuilder s = new StringBuilder(300);
				for(int i = 1; i < split.length;i++){
					 s.append(split[i]).append(" ");
				}

				String message = s.toString();

				message = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',message));
				if(Config.isIrcdColorSayMessageEnabled()){
					message = (char) 3 + "13" + message;
				}
				if(IRCd.mode == Modes.INSPIRCD){
					if (IRCd.isLinkcompleted()) {
						IRCd.privmsg(IRCd.serverUID, Config.getIrcdChannel(), message);
					}
				}else{
					IRCd.writeAll(message);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        final String mode = plugin.computePlayerModes(player);

        IRCd.addBukkitUser(mode, player);
    }

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		final String name = event.getPlayer().getName();
		plugin.removeLastReceivedBy(name);
		IRCd.removeBukkitUser(IRCd.getBukkitUser(name));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		IRCd.updateBukkitUserIdleTime(IRCd.getBukkitUser(event.getPlayer().getName()));

		switch (IRCd.mode) {
		case STANDALONE:
			IRCd.writeAll(IRCd.convertColors(event.getMessage(),false),event.getPlayer());
			break;

		case INSPIRCD:
			final BukkitPlayer bp = IRCd.getBukkitUserObject(event.getPlayer().getName());
			if (bp != null && IRCd.isLinkcompleted()) {
				IRCd.privmsg(bp.getUID(), Config.getIrcdChannel(), IRCd.convertColors(event.getMessage(), false));
			}
			break;
		}

		event.setMessage(IRCd.stripIRCFormatting(event.getMessage()));
	}


	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
	{
		IRCd.updateBukkitUserIdleTime(IRCd.getBukkitUser(event.getPlayer().getName()));

		String[] split = event.getMessage().split(" ");
		if (split.length > 1) {
			// ACTION/EMOTE can't be claimed, so use onPlayerCommandPreprocess
			if (split[0].equalsIgnoreCase("/me")) {
				switch (IRCd.mode) {
				case STANDALONE:
					IRCd.writeAll((char)1 + "ACTION " + IRCd.convertColors(IRCd.join(event.getMessage().split(" ")," ",1),false) + (char)1, event.getPlayer());
					break;

				case INSPIRCD:
					final BukkitPlayer bp = IRCd.getBukkitUserObject(event.getPlayer().getName());
					if (bp != null && IRCd.isLinkcompleted()) {
						IRCd.action(bp.getUID(), Config.getIrcdChannel(), IRCd.convertColors(IRCd.join(event.getMessage().split(" ")," ",1), false));
					}
					break;
				}
				event.setMessage(IRCd.stripIRCFormatting(event.getMessage()));
			}

			if (Config.getKickCommands().contains(split[0].substring(1).toLowerCase())){
				//PlayerKickEvent does not give kicker, so we listen to kick commands instead
				if (event.getPlayer().hasPermission("bukkitircd.kick")){
					final StringBuilder s = new StringBuilder(300);
					for(int i = 2; i < split.length;i++){
						 s.append(split[i]).append(" ");
					}
					final String kickMessage = s.toString();
					final String kickedPlayer = split[1];
					if ((IRCd.getBukkitUserObject(event.getPlayer().getName())) != null) {
						plugin.removeLastReceivedBy(kickedPlayer);
						IRCd.kickBukkitUser(kickMessage, IRCd.getBukkitUser(kickedPlayer), IRCd.getBukkitUser(event.getPlayer().getName()));
						IRCd.removeBukkitUser(IRCd.getBukkitUser(kickedPlayer));
						}

					}
				}

			if(split[0].equalsIgnoreCase("/say")){
				if(event.getPlayer().hasPermission("bukkit.command.say")){

					final StringBuilder s = new StringBuilder(300);
					for(int i = 1; i < split.length; i++){
						 s.append(split[i]).append(" ");
					}

					String message = s.toString();
					message = ChatColor.stripColor(message);
					message = ChatColor.translateAlternateColorCodes('&',message);
					if(Config.isIrcdColorSayMessageEnabled()) {
						message = (char) 3 + "13" + message;
					}

					switch (IRCd.mode) {
					case INSPIRCD:
						if (IRCd.isLinkcompleted()) {
							IRCd.privmsg(IRCd.serverUID, Config.getIrcdChannel(), message);
						}
						break;
					case STANDALONE:
						IRCd.writeAll(message);
						break;
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event)
	{
		final Player p = event.getPlayer();
		IRCd.updateBukkitUserIdleTimeAndWorld(IRCd.getBukkitUser(p.getName()), p.getWorld().getName());
	}
}

