package com.Jdbye.BukkitIRCd.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UserCommands implements CommandExecutor {

	final String[] userCommands = new String[] {
			"/bircd - Shows the BukkitIRCd version",
			"/irclist - Lists the online users on IRC.",
			"/irckick - Kick a user from IRC.",
			"/ircban - Ban a user from IRC via their host, ip, ident, nick or a full hostmask.",
			"/ircunban - Unban a user from IRC by their IP or full hostmask.",
			"/ircwhois - Perform a lookup on an IRC user.",
			"/ircmsg - Private message a user on IRC.",
			"/ircreply - Reply to the last IRC private message you received.",
			"/irctopic - Change the IRC topic for the main channel",
			"/irclink - Attempt to link to the remote server if in linking mode but not currently linked.",
			"/ircreload - Reload the plugin configuration and MOTD, and restart the IRC server/link.",
			"/rawsend - Sends a raw server command to the linked server, if enabled. Only usable from console.", };

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "BukkitIRCd Commands");
		sender.sendMessage(userCommands);
		return true;
	}
}