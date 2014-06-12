package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.IRCUserManagement;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Utils;

public class IRCListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
	    String[] args) {

	final String players[] = IRCUserManagement.getIRCNicks();

	sender.sendMessage(ChatColor.BLUE + "There are " + ChatColor.RED +
		players.length + ChatColor.BLUE + " users on IRC.");
	if (players.length > 0) {
	    sender.sendMessage(ChatColor.GRAY +
		    Utils.join(players, ChatColor.WHITE + ", " +
			    ChatColor.GRAY, 0));
	}

	return true;
    }
}
