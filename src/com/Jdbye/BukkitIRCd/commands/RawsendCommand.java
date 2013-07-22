package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RawsendCommand implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		if (sender instanceof Player){
			if (Config.isEnableRawSend()) {
				sender.sendMessage(ChatColor.RED + "[BukkitIRCd] Only the console can use this command.");
			} else {
				sender.sendMessage(ChatColor.RED + "[BukkitIRCd] Sending raw messages is disabled. Please enable them in the config first.");
			}
		} else {
			if (Config.isEnableRawSend()) {
				if (args.length > 0) {
					switch (IRCd.mode) {
					case INSPIRCD:
						if (IRCd.println(IRCd.join(args, " ", 0))) {
							sender.sendMessage(ChatColor.RED + "Command sent to IRC server link.");
						} else {
							sender.sendMessage(ChatColor.RED + "Failed to send command to IRC server link, not currently linked.");
						}
						break;

					case STANDALONE:
						sender.sendMessage(ChatColor.RED + "Please provide a command to send.");
						break;
					}
				} else {
					return false;
				}
			} else {
					sender.sendMessage(ChatColor.RED + "Rawsend is not enabled.");
			}
		}
		return true;
	}
}