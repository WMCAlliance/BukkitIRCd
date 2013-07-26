package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;

public class IRCWhoisCommand implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		final boolean oper;

		if (sender instanceof Player){
			final Player player = (Player) sender;
			oper = player.hasPermission("bukkitircd.oper");
		} else {
			oper = true;
		}

		if (args.length > 0) {
			final IRCUser ircuser = IRCd.getIRCUser(args[0]);
			if (ircuser != null) {
				for (final String whoisline : IRCd.getIRCWhois(ircuser, oper)) {
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