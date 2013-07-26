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
		final StringBuilder allplayers = new StringBuilder();
		boolean first = true;

		for (final String curplayer : players) {
			if (!first) {
				allplayers.append(ChatColor.WHITE + ", ");
				first = false;
			}

			allplayers.append(ChatColor.GRAY + curplayer);
		}

		sender.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
		if (players.length > 0) {
			sender.sendMessage(allplayers.toString());
		}

		return true;
	}
}
