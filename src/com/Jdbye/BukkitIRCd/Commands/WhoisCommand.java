package com.Jdbye.BukkitIRCd.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCUserManagement;

public class WhoisCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		final boolean oper;

		if (sender instanceof Player) {
			final Player player = (Player) sender;
			oper = player.hasPermission("bukkitircd.oper");
		} else {
			oper = true;
		}

		if (args.length > 0) {
			final IRCUser ircuser = IRCUserManagement.getIRCUser(args[0]);
			if (ircuser != null) {
				for (final String whoisline : IRCUserManagement.getIRCWhois(ircuser, oper)) {
					sender.sendMessage(whoisline);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "That user is not online.");
			}

			return true;
		} else {
			return false;
		}
	}
}
