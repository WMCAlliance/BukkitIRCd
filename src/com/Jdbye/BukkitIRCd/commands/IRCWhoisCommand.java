package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;

public class IRCWhoisCommand implements CommandExecutor{

	private BukkitIRCdPlugin thePlugin;

	public IRCWhoisCommand(BukkitIRCdPlugin plugin) {
		this.thePlugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.whois")) {
				if (args.length > 0) {
					IRCUser ircuser = IRCd.getIRCUser(args[0]);
					if (ircuser != null) {
						String[] whois = IRCd.getIRCWhois(ircuser);
						if (whois != null) {
							for (String whoisline : whois) player.sendMessage(whoisline);
						}
					}
					else { player.sendMessage(ChatColor.RED + "That user is not online."); }
				}
				else { player.sendMessage(ChatColor.RED + "Please provide a nickname."); return false; }		}
			else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}
			return true;
		}else{
			if (args.length > 0) {
				IRCUser ircuser = IRCd.getIRCUser(args[0]);
				if (ircuser != null) {
					String[] whois = IRCd.getIRCWhois(ircuser);
					for (String whoisline : whois) sender.sendMessage(whoisline);
				}
				else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
			}
			else { sender.sendMessage(ChatColor.RED + "Please provide a nickname."); return false; }		
			return true;
		}
	}

}