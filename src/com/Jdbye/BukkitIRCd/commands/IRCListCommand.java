package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;

public class IRCListCommand implements CommandExecutor{

	private BukkitIRCdPlugin thePlugin;

	public IRCListCommand(BukkitIRCdPlugin plugin) {
		this.thePlugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.list")) {
				String players[] = IRCd.getIRCNicks();
				String allplayers = "";
				for (String curplayer : players) allplayers += ChatColor.GRAY + curplayer + ChatColor.WHITE + ", ";
				player.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
				if (players.length > 0) player.sendMessage(allplayers.substring(0,allplayers.length()-2));
			}
			else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}
			return true;
		}else{
			String players[] = IRCd.getIRCNicks();
			String allplayers = "";
			for (String curplayer : players) allplayers += ChatColor.GRAY + curplayer + ChatColor.WHITE + ", ";
			sender.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
			if (players.length > 0) sender.sendMessage(allplayers.substring(0,allplayers.length()-2));
			return true;
		}
	}

}
