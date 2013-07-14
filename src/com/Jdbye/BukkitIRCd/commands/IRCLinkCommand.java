package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Modes;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IRCLinkCommand implements CommandExecutor{

	public IRCLinkCommand(BukkitIRCdPlugin plugin) {
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.link")) {
				if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
					if ((!IRCd.isLinkcompleted()) && (!IRCd.isConnected())) {
						if (IRCd.connect()) player.sendMessage(ChatColor.RED + "Successfully connected to " + Config.getLinkRemoteHost() + " on port " + Config.getLinkRemotePort());
						else player.sendMessage(ChatColor.RED + "Failed to connect to " + Config.getLinkRemoteHost() + " on port " + Config.getLinkRemotePort());
					}
					else {
						if (IRCd.isLinkcompleted()) player.sendMessage(ChatColor.RED + "Already linked to " + Config.getLinkName() + ".");
						else player.sendMessage(ChatColor.RED + "Already connected to " + Config.getLinkName() + ", but not linked.");
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
				if ((!IRCd.isLinkcompleted()) && (!IRCd.isConnected())) {
					if (IRCd.connect()) sender.sendMessage(ChatColor.RED + "Successfully connected to " + Config.getLinkRemoteHost() + " on port " + Config.getLinkRemotePort());
					else sender.sendMessage(ChatColor.RED + "Failed to connect to " + Config.getLinkRemoteHost() + " on port " + Config.getLinkRemotePort());
				}
				else {
					if (IRCd.isLinkcompleted()) sender.sendMessage(ChatColor.RED + "Already linked to " + Config.getLinkName() + ".");
					else sender.sendMessage(ChatColor.RED + "Already connected to " + Config.getLinkName() + ", but not linked.");
				}
			}
			else { sender.sendMessage(ChatColor.RED + "[BukkitIRCd] You are currently in standalone mode. To link to a server, modify the config."); }
			return true;
		}
	}

}
