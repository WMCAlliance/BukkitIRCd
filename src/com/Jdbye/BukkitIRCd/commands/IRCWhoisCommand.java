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

	public IRCWhoisCommand(BukkitIRCdPlugin plugin) {
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		final boolean oper;

		if (sender instanceof Player){
			final Player player = (Player) sender;
			if (!player.hasPermission("bukkitircd.whois")) {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
				return true;
			}
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
		} else {
			sender.sendMessage(ChatColor.RED + "Please provide a nickname.");
		}
		return true;

	}

}