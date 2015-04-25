package com.Jdbye.BukkitIRCd.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.Jdbye.BukkitIRCd.IRCUserManagement;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;

public class ListCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		final String players[] = IRCUserManagement.getIRCNicks();

		sender.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED + players.length + ChatColor.BLUE + " users on IRC.");
		if (players.length > 0) {
			sender.sendMessage(ChatColor.GRAY +
					ChatUtils.join(players, ChatColor.WHITE + ", " + ChatColor.GRAY, 0));
		}

		return true;
	}
}
