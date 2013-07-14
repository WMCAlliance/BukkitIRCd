package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IRCTopicCommand implements CommandExecutor{

	public IRCTopicCommand(BukkitIRCdPlugin plugin) {
	}
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player){
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.topic")) {
				if (args.length > 0) {
					String topic = IRCd.join(args, " ", 0);
					String playername = player.getName();
					String playerhost = player.getAddress().getAddress().getHostName();
					IRCd.setTopic(IRCd.convertColors(topic, false), playername + Config.getIrcdIngameSuffix(), playername + Config.getIrcdIngameSuffix() + "!" + playername + "@" + playerhost);
					player.sendMessage(ChatColor.RED + "Topic set to " + topic);
				}
				else { player.sendMessage(ChatColor.RED + "Please provide a topic to set."); return false; }
			}
			else {
				player.sendMessage(ChatColor.RED + "You don't have access to that command.");
			}    		
			return true;
		}else{
			if (args.length > 0) {
				String topic = IRCd.join(args, " ", 0);
				IRCd.setTopic(IRCd.convertColors(topic, false), Config.getIrcdServerName(), Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName());
				sender.sendMessage(ChatColor.RED + "Topic set to " + topic);
			}
			else { sender.sendMessage(ChatColor.RED + "Please provide a topic to set."); return false; }		
			return true;
		}
	}

}
