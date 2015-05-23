package com.Jdbye.BukkitIRCd.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.Configuration.Config;

public class IRCInfoCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "BukkitIRCd Status");
		sender.sendMessage(ChatColor.RED + "Current Mode is: " + ChatColor.RESET + Config.getMode());
		sender.sendMessage(ChatColor.RED + "Server name is: " + ChatColor.RESET + Config.getIrcdServerName());
		sender.sendMessage(ChatColor.RED + "Channel name is: " + ChatColor.RESET + Config.getIrcdChannel());
		sender.sendMessage(ChatColor.RED + "Current debug status: " + ChatColor.RESET + Config.isDebugModeEnabled());
		sender.sendMessage(ChatColor.RED + "Is dynamp on : " + ChatColor.RESET + BukkitIRCdPlugin.dynmap);
		
		return true;
	}
}