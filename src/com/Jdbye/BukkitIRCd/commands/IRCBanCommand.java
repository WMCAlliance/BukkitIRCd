package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;

public class IRCBanCommand implements CommandExecutor{

	private BukkitIRCdPlugin thePlugin;

	public IRCBanCommand(BukkitIRCdPlugin plugin) {
		this.thePlugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.ban")) {
				if (args.length > 0) {
					String reason = null;
					IRCUser ircuser;
					String ban;
					String banType = null;
					if ((args[0].equalsIgnoreCase("ip")) || (args[0].equalsIgnoreCase("host")) || (args[0].equalsIgnoreCase("ident")) || (args[0].equalsIgnoreCase("nick"))) {
						ircuser = IRCd.getIRCUser(args[1]);
						ban = args[1];
						banType = args[0];
						if (args.length > 2) reason = IRCd.join(args, " ", 2);
					}
					else {
						ircuser = IRCd.getIRCUser(args[0]);
						ban = args[0];
						if (args.length > 1) reason = IRCd.join(args, " ", 1);
					}
					if (IRCd.wildCardMatch(ban, "*!*@*")) {
						// Full hostmask
						if (IRCd.banIRCUser(ban, player.getName() + IRCd.ingameSuffix + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
							if (IRCd.msgIRCBan.length() > 0) thePlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
							if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
							player.sendMessage(ChatColor.RED + "User banned.");
						}
						else player.sendMessage(ChatColor.RED + "User is already banned.");
					}
					else if (thePlugin.countStr(ban, ".") == 3) { // It's an IP
						if (IRCd.banIRCUser("*!*@" + ban, player.getName() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName())) {
							if (IRCd.msgIRCBan.length() > 0) thePlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
							if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%",player.getName()));
							player.sendMessage(ChatColor.RED + "IP banned.");
						}
						else
							player.sendMessage(ChatColor.RED + "IP is already banned.");
					}
					else {
						if (ircuser != null) {
							if (IRCd.kickBanIRCUser(ircuser, player.getName(), player.getName() + "!" + player.getName() + "@" + player.getAddress().getAddress().getHostName(), reason, true, banType))
								player.sendMessage(ChatColor.RED + "User banned.");
							else
								player.sendMessage(ChatColor.RED + "User is already banned.");
						}
						else { player.sendMessage(ChatColor.RED + "That user is not online."); }
					}
				}
				else { player.sendMessage(ChatColor.RED + "Please provide a nickname or IP and optionally a ban reason."); return false; }
			}
			else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}
			return true;
		}else{
			if (args.length > 0) {
				String reason = null;
				IRCUser ircuser;
				String ban;
				String banType = null;
				if ((args[0].equalsIgnoreCase("ip")) || (args[0].equalsIgnoreCase("host")) || (args[0].equalsIgnoreCase("ident")) || (args[0].equalsIgnoreCase("nick"))) {
					ircuser = IRCd.getIRCUser(args[1]);
					ban = args[1];
					banType = args[0];
					if (args.length > 2) reason = IRCd.join(args, " ", 2);
				}
				else {
					ircuser = IRCd.getIRCUser(args[0]);
					ban = args[0];
					if (args.length > 1) reason = IRCd.join(args, " ", 1);
				}
				if (IRCd.wildCardMatch(ban, "*!*@*")) {
					// Full hostmask
					if (IRCd.banIRCUser(ban, IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName)) {
						if (IRCd.msgIRCBan.length() > 0) thePlugin.getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
						if ((thePlugin.dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) thePlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
						sender.sendMessage(ChatColor.RED + "User banned.");
					}
					else
						sender.sendMessage(ChatColor.RED + "User is already banned.");
				}
				else if (thePlugin.countStr(ban, ".") == 3) { // It's an IP
					if (IRCd.banIRCUser("*!*@" + ban, IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName)) {
						if (IRCd.msgIRCBan.length() > 0) thePlugin.getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
						if ((thePlugin.dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) thePlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", ban).replace("%BANNEDBY%","console"));
						sender.sendMessage(ChatColor.RED + "IP banned.");
					}
					else
						sender.sendMessage(ChatColor.RED + "IP is already banned.");
				}
				else {
					if (ircuser != null) {
						if (IRCd.kickBanIRCUser(ircuser, "server", IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName, reason, true, banType))
							sender.sendMessage(ChatColor.RED + "User banned.");
						else
							sender.sendMessage(ChatColor.RED + "User is already banned.");
					}
					else { sender.sendMessage(ChatColor.RED + "That user is not online."); }
				}
			}
			else { sender.sendMessage(ChatColor.RED + "Please provide a nickname or IP and optionally a ban reason."); return false; }
			return true;
		}
	}

}