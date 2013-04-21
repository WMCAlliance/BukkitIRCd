package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Modes;

public class IRCLinkCommand implements CommandExecutor{

	private BukkitIRCdPlugin thePlugin;

	public IRCLinkCommand(BukkitIRCdPlugin plugin) {
		this.thePlugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.link")) {
				if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
					if ((!IRCd.linkcompleted) && (!IRCd.isConnected())) {
						if (IRCd.connect()) player.sendMessage(ChatColor.RED + "Successfully connected to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
						else player.sendMessage(ChatColor.RED + "Failed to connect to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
					}
					else {
						if (IRCd.linkcompleted) player.sendMessage(ChatColor.RED + "Already linked to " + IRCd.linkName + ".");
						else player.sendMessage(ChatColor.RED + "Already connected to " + IRCd.linkName + ", but not linked.");
					}
				}
				else { player.sendMessage(ChatColor.RED + "[BukkitIRCd] You are currently in standalone mode. To link to a server, modify the config."); }
			}
			else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}
			return true;
		}else{
			
			if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
				if ((!IRCd.linkcompleted) && (!IRCd.isConnected())) {
					if (IRCd.connect()) sender.sendMessage(ChatColor.RED + "Successfully connected to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
					else sender.sendMessage(ChatColor.RED + "Failed to connect to " + IRCd.remoteHost + " on port " + IRCd.remotePort);
				}
				else {
					if (IRCd.linkcompleted) sender.sendMessage(ChatColor.RED + "Already linked to " + IRCd.linkName + ".");
					else sender.sendMessage(ChatColor.RED + "Already connected to " + IRCd.linkName + ", but not linked.");
				}
			}
			else { sender.sendMessage(ChatColor.RED + "[BukkitIRCd] You are currently in standalone mode. To link to a server, modify the config."); }
			return true;
		}
	}

}