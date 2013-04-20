package com.Jdbye.BukkitIRCd;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

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
	public void onServerCommand(ServerCommandEvent event){
		
		//Console Command Listener
		String[] split = event.getCommand().split(" ");
		
		if (split.length > 1){
			
			if (BukkitIRCdPlugin.kickCommands.contains(split[0].toLowerCase())){
			
				//PlayerKickEvent does not give kicker, so we listen to kick commands instead
					StringBuilder s = new StringBuilder(300);
					for(int i = 2; i < split.length;i++){
						 s.append(split[i]).append(" ");
					}
					String kickMessage = s.toString();
					String kickedPlayer = split[1];
					plugin.removeLastReceivedBy(kickedPlayer);
					IRCd.kickBukkitUser(kickMessage, IRCd.getBukkitUser(kickedPlayer));
			}
			
			if (split[0].equalsIgnoreCase("say")){
				StringBuilder s = new StringBuilder(300);
				for(int i = 1; i < split.length;i++){
					 s.append(split[i]).append(" ");
				}
				
				String message = s.toString();
				if(IRCd.mode == Modes.INSPIRCD){
					if (IRCd.linkcompleted)  {
						IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + IRCd.channelName + " :" + ChatColor.stripColor(message));
						}
				}else{
					IRCd.writeAll(ChatColor.stripColor(message));
				}
				
			}
			
		}
		
		
		
	}
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
            String mode = "";
            Player player = event.getPlayer();
            if (plugin.hasPermission(player, "bukkitircd.mode.owner")) mode += "~";
            if (plugin.hasPermission(player, "bukkitircd.mode.protect")) mode += "&";
            //Most IRC networks have support for owner and superop mode. 
            
            
            if (plugin.hasPermission(player, "bukkitircd.mode.op")) mode += "@";
            if (plugin.hasPermission(player, "bukkitircd.mode.halfop")) mode += "%";
            if (plugin.hasPermission(player, "bukkitircd.mode.voice")) mode += "+";

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
		event.setMessage(IRCd.stripIRCFormatting(event.getMessage()));
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
				event.setMessage(IRCd.stripIRCFormatting(event.getMessage()));
			}
			
			
			if (BukkitIRCdPlugin.kickCommands.contains(split[0].substring(1).toLowerCase())){
				//PlayerKickEvent does not give kicker, so we listen to kick commands instead
				if (event.getPlayer().hasPermission("bukkitircd.kick")){
					StringBuilder s = new StringBuilder(300);
					for(int i = 2; i < split.length;i++){
						 s.append(split[i]).append(" ");
					}
					String kickMessage = s.toString();
					String kickedPlayer = split[1];
					BukkitPlayer bp;
					if ((bp = IRCd.getBukkitUserObject(event.getPlayer().getName())) != null) {
						plugin.removeLastReceivedBy(kickedPlayer);
						IRCd.kickBukkitUser(kickMessage, IRCd.getBukkitUser(kickedPlayer), IRCd.getBukkitUser(event.getPlayer().getName()));
						IRCd.removeBukkitUser(IRCd.getBukkitUser(kickedPlayer));
						}
					
					}
				}
			
			if(split[0].equalsIgnoreCase("/say")){
				if(event.getPlayer().hasPermission("bukkit.command.say")){
					
					StringBuilder s = new StringBuilder(300);
					for(int i = 1; i < split.length;i++){
						 s.append(split[i]).append(" ");
					}
					
					String message = s.toString();
					
					if(IRCd.mode == Modes.INSPIRCD){
						
						if (IRCd.linkcompleted) { 
							IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + IRCd.channelName + " :" + ChatColor.stripColor(message));
							}
					}else{
						IRCd.writeAll(ChatColor.stripColor(message));
					}
				}
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

