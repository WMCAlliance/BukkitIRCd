package com.Jdbye.BukkitIRCd;

import com.Jdbye.BukkitIRCd.configuration.Config;
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
			if (IRCd.isLinkcompleted())  {
				
				IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + Config.getIrcdChannel() + " :" + message);
				}
		}
		else {
			IRCd.writeAll(message);
		}
		
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onServerCommand(ServerCommandEvent event){
		
		//Console Command Listener
		String[] split = event.getCommand().split(" ");
		
		if (split.length > 1){
			
			if (Config.getKickCommands().contains(split[0].toLowerCase())){
			
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
				
				
				message = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',message));
				if(Config.isIrcdColorSayMessageEnabled()){
					message = (char) 3 + "13" + message;
				}
				if(IRCd.mode == Modes.INSPIRCD){
					if (IRCd.isLinkcompleted())  {
						IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + Config.getIrcdChannel() + " :" + message);
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
            StringBuffer mode = new StringBuffer();
            Player player = event.getPlayer();
            if (player.hasPermission("bukkitircd.mode.owner")){
            	if (Config.isDebugModeEnabled()) {
            		BukkitIRCdPlugin.log.info("Add mode +q for " + player.getName());
            	}
            	mode.append("~");
            	
            }
            if (player.hasPermission("bukkitircd.mode.protect")){
            	if (Config.isDebugModeEnabled()) {
            		BukkitIRCdPlugin.log.info("Add mode +a for " + player.getName());
            	}
            	mode.append("&");
            }
            if (player.hasPermission("bukkitircd.mode.op")){
            	if (Config.isDebugModeEnabled()) {
            		BukkitIRCdPlugin.log.info("Add mode +o for " + player.getName());
            	}
            	mode.append("@");
            }
            if (player.hasPermission("bukkitircd.mode.halfop")){
            	if (Config.isDebugModeEnabled()) {
            		BukkitIRCdPlugin.log.info("Add mode +h for " + player.getName());
            	}
            	mode.append("%");
            }
            if (player.hasPermission("bukkitircd.mode.voice")){
            	if (Config.isDebugModeEnabled()) {
            		BukkitIRCdPlugin.log.info("Add mode +v for " + player.getName());
            	}
            	mode.append("+");
            }
            if (!Config.isIrcdRedundantModes() && mode.length() > 0){ //TODO Re-add mode.length() > 0 - not sure if that would be exactly the same here
            	mode.delete(1, mode.length()); //Remove all but the mode powerful mode if redundant modes are not allowed
            }
            IRCd.addBukkitUser(mode.toString(),player);
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
				if (IRCd.isLinkcompleted()) IRCd.println(":" + bp.getUID() + " PRIVMSG " + Config.getIrcdChannel() + " :" + IRCd.convertColors(event.getMessage(), false));
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
						if (IRCd.isLinkcompleted()) IRCd.println(":" + bp.getUID() + " PRIVMSG " + Config.getIrcdChannel() + " :" + (char)1 + "ACTION " + IRCd.convertColors(IRCd.join(event.getMessage().split(" ")," ",1), false) + (char)1);
					}
				}
				event.setMessage(IRCd.stripIRCFormatting(event.getMessage()));
			}
			
			
			if (Config.getKickCommands().contains(split[0].substring(1).toLowerCase())){
				//PlayerKickEvent does not give kicker, so we listen to kick commands instead
				if (event.getPlayer().hasPermission("bukkitircd.kick")){
					StringBuilder s = new StringBuilder(300);
					for(int i = 2; i < split.length;i++){
						 s.append(split[i]).append(" ");
					}
					String kickMessage = s.toString();
					String kickedPlayer = split[1];
					if ((IRCd.getBukkitUserObject(event.getPlayer().getName())) != null) {
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
					
					message = ChatColor.translateAlternateColorCodes('&',ChatColor.stripColor(message));
					if(Config.isIrcdColorSayMessageEnabled()){
						message = (char) 3 + "13" + message;
					}
					
					if(IRCd.mode == Modes.INSPIRCD){
						
						if (IRCd.isLinkcompleted()) {
							IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + Config.getIrcdChannel() + " :" + message);
							}
					}else{
						IRCd.writeAll(message);
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

