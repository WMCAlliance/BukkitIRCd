package com.Jdbye.BukkitIRCd.Commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUserManagement;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Configuration.Config;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;
import com.Jdbye.BukkitIRCd.Utilities.MessageFormatter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnbanCommand implements CommandExecutor {

	private BukkitIRCdPlugin thePlugin;

	public UnbanCommand(BukkitIRCdPlugin plugin) {
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
						ChatUtils.join(args, " ", 1);
					}

					if (ChatUtils.wildCardMatch(ban, "*!*@*")) { // Full hostmask
						if (IRCUserManagement.unBanIRCUser(ban, player.getName() + Config.getIrcdIngameSuffix() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
							if (IRCd.msgIRCUnban.length() > 0) {
								thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCUnban, ban, player.getName()));
							}
							if ((BukkitIRCdPlugin.dynmap != null) &&
									(IRCd.msgIRCUnbanDynmap.length() > 0)) {
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC",MessageFormatter.banMsg(IRCd.msgIRCUnbanDynmap, ban, player.getName()));
							}
							player.sendMessage(ChatColor.RED + "User unbanned.");
						} else {
							player.sendMessage(ChatColor.RED + "User is not banned.");
						}
					} else if (BukkitIRCdPlugin.countStr(ban, ".") == 3) { // It's an IP
						if (IRCUserManagement.unBanIRCUser("*!*@" + ban,
								player.getName() + Config.getIrcdIngameSuffix() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
							if (IRCd.msgIRCUnban.length() > 0) {
								thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCUnban, ban, player.getName()));
							}
							if ((BukkitIRCdPlugin.dynmap != null) &&
									(IRCd.msgIRCUnbanDynmap.length() > 0)) {
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC",MessageFormatter.banMsg(IRCd.msgIRCUnbanDynmap, ban, player.getName()));
							}
							player.sendMessage(ChatColor.RED + "IP unbanned.");
						} else {
							player.sendMessage(ChatColor.RED + "IP is not banned.");
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
					ChatUtils.join(args, " ", 1);
				}

				if (ChatUtils.wildCardMatch(ban, "*!*@*")) { // Full hostmask
					if (IRCUserManagement.unBanIRCUser(
							ban,
							Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName())) {
						if (IRCd.msgIRCUnban.length() > 0) {
							thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCUnban, ban, "console"));
						}
						if ((BukkitIRCdPlugin.dynmap != null) &&
								(IRCd.msgIRCUnbanDynmap.length() > 0)) {
							BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", MessageFormatter.banMsg(IRCd.msgIRCBanDynmap, ban, "console"));
						}
						sender.sendMessage(ChatColor.RED + "User unbanned.");
					} else {
						sender.sendMessage(ChatColor.RED + "User is not banned.");
					}
				} else if (BukkitIRCdPlugin.countStr(ban, ".") == 3) { // It's an IP
					if (IRCUserManagement.unBanIRCUser( "*!*@" + ban, Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName())) {
						if (IRCd.msgIRCUnban.length() > 0) {
							thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCUnban, ban, "console"));
						}
						if ((BukkitIRCdPlugin.dynmap != null) &&
								(IRCd.msgIRCUnbanDynmap.length() > 0)) {
							BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", MessageFormatter.banMsg(IRCd.msgIRCBanDynmap, ban, "console"));
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
