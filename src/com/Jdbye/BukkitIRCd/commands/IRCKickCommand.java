package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCUserManagement;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.configuration.Config;

public class IRCKickCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		// Check arguments
		if (args.length == 0) {
			return false; // prints usage
		}
		final String targetNick = args[0];
		final String reason = args.length > 1 ? IRCd.join(args, " ", 1) : null;

		// Compute target
		final IRCUser targetIrcUser = IRCUserManagement.getIRCUser(targetNick);
		if (targetIrcUser == null) {
			sender.sendMessage(ChatColor.RED + "That user is not online.");
			return true;
		}

		// Compute kicker
		final String kickerNick;
		final String kickerHost;
		if (sender instanceof Player) {
			final Player player = (Player) sender;
			kickerNick = player.getName();
			kickerHost = player.getAddress().getAddress().getHostName();
		} else {
			kickerNick = Config.getIrcdServerName();
			kickerHost = Config.getIrcdServerHostName();
		}

		// Execute kick
		if (IRCUserManagement.kickIRCUser(targetIrcUser, kickerNick, kickerNick, kickerHost,
				reason, true)) {
			sender.sendMessage(ChatColor.RED + "Player kicked.");
		} else {
			sender.sendMessage(ChatColor.RED + "Failed to kick player.");
		}

		return true;
	}

}
