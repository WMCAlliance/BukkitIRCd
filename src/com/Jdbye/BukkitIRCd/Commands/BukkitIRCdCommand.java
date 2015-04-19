package com.Jdbye.BukkitIRCd.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;

public class BukkitIRCdCommand implements CommandExecutor {
	
	
	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
    	    String[] args) {
		sender.sendMessage(ChatColor.RED + "BukkitIRCd " + ChatColor.RESET + "v" + BukkitIRCdPlugin.thePlugin.getDescription().getVersion());
		return false;
	}

}
