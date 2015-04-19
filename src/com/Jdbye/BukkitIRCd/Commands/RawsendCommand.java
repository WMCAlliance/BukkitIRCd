package com.Jdbye.BukkitIRCd.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Utils;
import com.Jdbye.BukkitIRCd.Configuration.Config;

public class RawsendCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
	    String[] args) {

	if (Config.isEnableRawSend()) {
	    sender.sendMessage(ChatColor.RED +
		    "Sending raw messages is disabled by configuration.");
	    return true;
	}

	if (!(sender instanceof ConsoleCommandSender)) {
	    sender.sendMessage(ChatColor.RED +
		    "Only the console can use this command.");
	    return true;
	}

	if (args.length == 0) {
	    return false;
	}

	switch (IRCd.mode) {
	    case INSPIRCD:
		if (Utils.println(Utils.join(args, " ", 0))) {
		    sender.sendMessage(ChatColor.RED +
			    "Command sent to IRC server link.");
		} else {
		    sender.sendMessage(ChatColor.RED +
			    "Failed to send command to IRC server link, not currently linked.");
		}
		break;

	    case STANDALONE:
		sender.sendMessage(ChatColor.RED +
			"Raw commands are not supported in standalone mode.");
		break;
	}

	return true;
    }
}
