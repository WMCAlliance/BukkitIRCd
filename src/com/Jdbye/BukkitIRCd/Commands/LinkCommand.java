package com.Jdbye.BukkitIRCd.Commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCFunctionality;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Configuration.Config;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    public LinkCommand(BukkitIRCdPlugin plugin) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
	    String[] args) {

	if (sender instanceof Player) {
	    final Player player = (Player) sender;
	    if (!player.hasPermission("bukkitircd.link")) {
		player.sendMessage(ChatColor.RED +
			"You don't have access to that command.");
		return true;
	    }
	}

	switch (IRCd.mode) {
	    case INSPIRCD:
		if ((!IRCd.isLinkcompleted()) && (!IRCd.isConnected())) {
		    if (IRCFunctionality.connect()) {
			sender.sendMessage(ChatColor.RED +
				"Successfully connected to " +
				Config.getLinkRemoteHost() + " on port " +
				Config.getLinkRemotePort());
		    } else {
			sender.sendMessage(ChatColor.RED + "Failed to connect to " +
				Config.getLinkRemoteHost() + " on port " +
				Config.getLinkRemotePort());
		    }
		} else {
		    if (IRCd.isLinkcompleted()) {
			sender.sendMessage(ChatColor.RED + "Already linked to " +
				Config.getLinkName() + ".");
		    } else {
			sender.sendMessage(ChatColor.RED + "Already connected to " +
				Config.getLinkName() + ", but not linked.");
		    }
		}
		break;
	    case STANDALONE:
		sender.sendMessage(ChatColor.RED +
			"[BukkitIRCd] You are currently in standalone mode. To link to a server, modify the config.");
		break;
	}

	return true;
    }
}
