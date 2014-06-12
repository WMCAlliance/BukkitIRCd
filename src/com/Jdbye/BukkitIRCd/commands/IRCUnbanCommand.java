package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUserManagement;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Utils;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IRCUnbanCommand implements CommandExecutor {

    private BukkitIRCdPlugin thePlugin;

    public IRCUnbanCommand(BukkitIRCdPlugin plugin) {
	this.thePlugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label,
	    String[] args) {
	if (sender instanceof Player) {
	    Player player = (Player) sender;

	    if (player.hasPermission("bukkitircd.unban")) {
		if (args.length > 0) {
		    String ban;
		    ban = args[0];
		    if (args.length > 1) {
			Utils.join(args, " ", 1);
		    }

		    if (Utils.wildCardMatch(ban, "*!*@*")) { // Full hostmask
			if (IRCUserManagement.unBanIRCUser(ban,
				player.getName() +
				Config.getIrcdIngameSuffix() +
				"!" +
				player.getName() +
				"@" +
				player.getAddress().getAddress()
				.getHostName())) {
			    if (IRCd.msgIRCUnban.length() > 0) {
				thePlugin
					.getServer()
					.broadcastMessage(
						IRCd.msgIRCUnban
						.replace(
							"{BannedUser}",
							ban)
						.replace(
							"{BannedBy}",
							player.getName()));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (IRCd.msgIRCUnbanDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap
					.sendBroadcastToWeb(
						"IRC",
						IRCd.msgIRCUnbanDynmap
						.replace(
							"{BannedUser}",
							ban)
						.replace(
							"{BannedBy}",
							player.getName()));
			    }
			    player.sendMessage(ChatColor.RED + "User unbanned.");
			} else {
			    player.sendMessage(ChatColor.RED +
				    "User is not banned.");
			}
		    } else if (thePlugin.countStr(ban, ".") == 3) { // It's an
			// IP
			if (IRCUserManagement.unBanIRCUser("*!*@" + ban,
				player.getName() +
				Config.getIrcdIngameSuffix() +
				"!" +
				player.getName() +
				"@" +
				player.getAddress().getAddress()
				.getHostName())) {
			    if (IRCd.msgIRCUnban.length() > 0) {
				thePlugin
					.getServer()
					.broadcastMessage(
						IRCd.msgIRCUnban
						.replace(
							"{BannedUser}",
							ban)
						.replace(
							"{BannedBy}",
							player.getName()));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (IRCd.msgIRCUnbanDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap
					.sendBroadcastToWeb(
						"IRC",
						IRCd.msgIRCUnbanDynmap
						.replace(
							"{BannedUser}",
							ban)
						.replace(
							"{BannedBy}",
							player.getName()));
			    }
			    player.sendMessage(ChatColor.RED + "IP unbanned.");
			} else {
			    player.sendMessage(ChatColor.RED +
				    "IP is not banned.");
			}
		    } else {
			player.sendMessage(ChatColor.RED + "Invalid hostmask.");
			return false;
		    }
		} else {
		    player.sendMessage(ChatColor.RED +
			    "Please provide a IP/full hostmask.");
		    return false;
		}
	    } else {
		player.sendMessage(ChatColor.RED +
			"You don't have access to that command.");
	    }
	    return true;

	} else {
	    if (args.length > 0) {
		String ban;
		ban = args[0];
		if (args.length > 1) {
		    Utils.join(args, " ", 1);
		}

		if (Utils.wildCardMatch(ban, "*!*@*")) { // Full hostmask
		    if (IRCUserManagement.unBanIRCUser(
			    ban,
			    Config.getIrcdServerName() + "!" +
			    Config.getIrcdServerName() + "@" +
			    Config.getIrcdServerHostName())) {
			if (IRCd.msgIRCUnban.length() > 0) {
			    thePlugin.getServer().broadcastMessage(
				    IRCd.msgIRCUnban.replace("{BannedUser}",
					    ban).replace("{BannedBy}",
					    "console"));
			}
			if ((BukkitIRCdPlugin.dynmap != null) &&
				(IRCd.msgIRCUnbanDynmap.length() > 0)) {
			    BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
				    "IRC",
				    IRCd.msgIRCUnbanDynmap.replace(
					    "{BannedUser}", ban).replace(
					    "{BannedBy}", "console"));
			}
			sender.sendMessage(ChatColor.RED + "User unbanned.");
		    } else {
			sender.sendMessage(ChatColor.RED +
				"User is not banned.");
		    }
		} else if (thePlugin.countStr(ban, ".") == 3) { // It's an IP
		    if (IRCUserManagement.unBanIRCUser(
			    "*!*@" + ban,
			    Config.getIrcdServerName() + "!" +
			    Config.getIrcdServerName() + "@" +
			    Config.getIrcdServerHostName())) {
			if (IRCd.msgIRCUnban.length() > 0) {
			    thePlugin.getServer().broadcastMessage(
				    IRCd.msgIRCUnban.replace("{BannedUser}",
					    ban).replace("{BannedBy}",
					    "console"));
			}
			if ((BukkitIRCdPlugin.dynmap != null) &&
				(IRCd.msgIRCUnbanDynmap.length() > 0)) {
			    BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
				    "IRC",
				    IRCd.msgIRCUnbanDynmap.replace(
					    "{BannedUser}", ban).replace(
					    "{BannedBy}", "console"));
			}
			sender.sendMessage(ChatColor.RED + "IP unbanned.");
		    } else {
			sender.sendMessage(ChatColor.RED + "IP is not banned.");
		    }
		} else {
		    sender.sendMessage(ChatColor.RED + "Invalid hostmask.");
		    return false;
		}

	    } else {
		sender.sendMessage(ChatColor.RED +
			"Please provide a IP/full hostmask.");
		return false;
	    }
	    return true;

	}
    }

}
