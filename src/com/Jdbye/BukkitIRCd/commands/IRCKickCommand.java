package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;

public class IRCKickCommand implements CommandExecutor{

	public IRCKickCommand(BukkitIRCdPlugin plugin) {
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.kick")) {
				if (args.length > 0) {
					String reason = null;
					if (args.length > 1) reason = IRCd.join(args, " ", 1);
					IRCUser ircuser = IRCd.getIRCUser(args[0]);
					if (ircuser != null) {
						if (IRCd.kickIRCUser(ircuser, player.getName(), player.getName(), player.getAddress().getAddress().getHostName(), reason, true))
							player.sendMessage(ChatColor.RED + "Player kicked.");
						else
							player.sendMessage(ChatColor.RED + "Failed to kick player.");
					}
					else { player.sendMessage(ChatColor.RED + "That user is not online."); }
				}
				else { player.sendMessage(ChatColor.RED + "Please provide a nickname and optionally a kick reason."); return false; }
			}
			else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}
			return true;
		}else{
			if (args.length > 0) {
				String reason = null;
				if (args.length > 1) reason = IRCd.join(args, " ", 1);
				IRCUser ircuser = IRCd.getIRCUser(args[0]);
				if (ircuser != null) {
					if (IRCd.kickIRCUser(ircuser, IRCd.serverName, IRCd.serverName, IRCd.serverHostName, reason, false)) sender.sendMessage(ChatColor.RED + "Player kicked.");
					else sender.sendMessage(ChatColor.RED + "Failed to kick player.");
					
				}
				else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
			}
			else { sender.sendMessage(ChatColor.RED + "Please provide a nickname and optionally a kick reason."); return false; }		
			return true;
		}
	}

}