package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Modes;

public class RawsendCommand implements CommandExecutor{

	private BukkitIRCdPlugin thePlugin;

	public RawsendCommand(BukkitIRCdPlugin plugin) {
		this.thePlugin = plugin;
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			sender = (Player) sender;
			if (thePlugin.enableRawSend) {
				sender.sendMessage(ChatColor.RED + "[BukkitIRCd] Only the console can use this command.");
			}
			else { sender.sendMessage(ChatColor.RED + "[BukkitIRCd] Sending raw messages is disabled. Please enable them in the config first."); }
			return true;
		
		}else{
			if (thePlugin.enableRawSend) {
				if (args.length > 0) {
					if ((IRCd.mode == Modes.INSPIRCD) || (IRCd.mode == Modes.UNREALIRCD)) {
						if (IRCd.println(IRCd.join(args, " ", 0))) sender.sendMessage(ChatColor.RED + "Command sent to IRC server link.");
						else sender.sendMessage(ChatColor.RED + "Failed to send command to IRC server link, not currently linked.");
					}
				}
				else { sender.sendMessage(ChatColor.RED + "Please provide a command to send."); return false; }
			}
			else { sender.sendMessage(ChatColor.RED + "Rawsend is not enabled."); }
			return true;
		}
	}

}