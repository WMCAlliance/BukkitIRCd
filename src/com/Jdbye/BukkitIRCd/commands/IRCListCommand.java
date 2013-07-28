package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.Jdbye.BukkitIRCd.IRCd;

public class IRCListCommand implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		final String players[] = IRCd.getIRCNicks();

		sender.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
		if (players.length > 0) {
			sender.sendMessage(ChatColor.GRAY + IRCd.join(players, ChatColor.WHITE + ", " + ChatColor.GRAY, 0));
		}

		return true;
	}
}
