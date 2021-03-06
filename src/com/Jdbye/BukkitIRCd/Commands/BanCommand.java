package com.Jdbye.BukkitIRCd.Commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUser;
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

public class BanCommand implements CommandExecutor {

	private BukkitIRCdPlugin thePlugin;

	public BanCommand(BukkitIRCdPlugin plugin) {
		this.thePlugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.ban")) {
				if (args.length > 0) {
					String reason = null;
					IRCUser ircuser;
					String ban;
					String banType = null;
					if ((args[0].equalsIgnoreCase("ip")) ||
							(args[0].equalsIgnoreCase("host")) ||
							(args[0].equalsIgnoreCase("ident")) ||
							(args[0].equalsIgnoreCase("nick"))) {
						ircuser = IRCUserManagement.getIRCUser(args[1]);
						ban = args[1];
						banType = args[0];
						if (args.length > 2) {
							reason = ChatUtils.join(args, " ", 2);
						}
					} else {
						ircuser = IRCUserManagement.getIRCUser(args[0]);
						ban = args[0];
						if (args.length > 1) {
							reason = ChatUtils.join(args, " ", 1);
						}
					}
					if (ChatUtils.wildCardMatch(ban, "*!*@*")) {
						// Full hostmask
						if (IRCUserManagement.banIRCUser(ban,
								player.getName() +
								Config.getIrcdIngameSuffix() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
							if (IRCd.msgIRCBan.length() > 0) {
								thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCBan, ban, player.getName()));	
							}
							if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) {
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", MessageFormatter.banMsg(IRCd.msgIRCBanDynmap, ban, player.getName()));
							}
							player.sendMessage(ChatColor.RED + "User banned.");
						} else {
							player.sendMessage(ChatColor.RED + "User is already banned.");
						}
					} else if (BukkitIRCdPlugin.countStr(ban, ".") == 3) { // It's an IP
						if (IRCUserManagement.banIRCUser("*!*@" + ban, player.getName() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
							if (IRCd.msgIRCBan.length() > 0) {
								thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCBan, ban, player.getName()));
							}
							if ((BukkitIRCdPlugin.dynmap != null) &&
									(IRCd.msgIRCBanDynmap.length() > 0)) {
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", MessageFormatter.banMsg(IRCd.msgIRCBanDynmap, ban, player.getName()));
							}
							player.sendMessage(ChatColor.RED + "IP banned.");
						} else {
							player.sendMessage(ChatColor.RED + "IP is already banned.");
						}
					} else {
						if (ircuser != null) {
							if (IRCUserManagement.kickBanIRCUser(ircuser, player.getName(),  player.getName() + "!" + player.getName() +  "@" + player.getAddress().getAddress().getHostName(), reason, true, banType)) {
								player.sendMessage(ChatColor.RED + "User banned.");
							} else {
								player.sendMessage(ChatColor.RED + "User is already banned.");
							}
						} else {
							player.sendMessage(ChatColor.RED + "That user is not online.");
						}
					}
				} else {
					player.sendMessage(ChatColor.RED +  "Please provide a nickname or IP and optionally a ban reason.");
					return false;
				}
			} else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}
			return true;
		} else {
			if (args.length > 0) {
				String reason = null;
				IRCUser ircuser;
				String ban;
				String banType = null;
				if ((args[0].equalsIgnoreCase("ip")) ||
						(args[0].equalsIgnoreCase("host")) ||
						(args[0].equalsIgnoreCase("ident")) ||
						(args[0].equalsIgnoreCase("nick"))) {
					ircuser = IRCUserManagement.getIRCUser(args[1]);
					ban = args[1];
					banType = args[0];
					if (args.length > 2) {
						reason = ChatUtils.join(args, " ", 2);
					}
				} else {
					ircuser = IRCUserManagement.getIRCUser(args[0]);
					ban = args[0];
					if (args.length > 1) {
						reason = ChatUtils.join(args, " ", 1);
					}
				}
				if (ChatUtils.wildCardMatch(ban, "*!*@*")) {
					// Full hostmask
					if (IRCUserManagement.banIRCUser(
							ban,
							Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" +  Config.getIrcdServerHostName())) {
						if (IRCd.msgIRCBan.length() > 0) {
							thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCBan, ban, "console"));
						}
						if ((BukkitIRCdPlugin.dynmap != null) &&
								(IRCd.msgIRCBanDynmap.length() > 0)) {
							BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC",MessageFormatter.banMsg(IRCd.msgIRCBanDynmap, ban, "console"));
						}
						sender.sendMessage(ChatColor.RED + "User banned.");
					} else {
						sender.sendMessage(ChatColor.RED + "User is already banned.");
					}
				} else if (BukkitIRCdPlugin.countStr(ban, ".") == 3) { // It's an IP
					if (IRCUserManagement.banIRCUser("*!*@" + ban,
							Config.getIrcdServerName() + "!" +
									Config.getIrcdServerName() + "@" +
									Config.getIrcdServerHostName())) {
						if (IRCd.msgIRCBan.length() > 0) {
							thePlugin.getServer().broadcastMessage(MessageFormatter.banMsg(IRCd.msgIRCBan, ban, "console"));
						}
						if ((BukkitIRCdPlugin.dynmap != null) &&
								(IRCd.msgIRCBanDynmap.length() > 0)) {
							BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC",MessageFormatter.banMsg(IRCd.msgIRCBanDynmap, ban, "console"));
						}
						sender.sendMessage(ChatColor.RED + "IP banned.");
					} else {
						sender.sendMessage(ChatColor.RED + "IP is already banned.");
					}
				} else {
					if (ircuser != null) {
						if (IRCUserManagement.kickBanIRCUser(ircuser, "server", Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName(), reason, true, banType)) {
							sender.sendMessage(ChatColor.RED + "User banned.");
						} else {
							sender.sendMessage(ChatColor.RED + "User is already banned.");
						}
					} else {
						sender.sendMessage(ChatColor.RED + "That user is not online.");
					}
				}
			} else {
				sender.sendMessage(ChatColor.RED +
						"Please provide a nickname or IP and optionally a ban reason.");
				return false;
			}
			return true;
		}
	}

}
