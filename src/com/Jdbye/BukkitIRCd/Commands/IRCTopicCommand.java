package com.Jdbye.BukkitIRCd.Commands;

import com.Jdbye.BukkitIRCd.IRCFunctionality;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.Configuration.Config;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;

public class IRCTopicCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
	    String[] args) {

	final String userNick;
	final String userUser;
	final String userHost;

	if (sender instanceof Player) {
	    final Player player = (Player) sender;
	    final String playerName = player.getName();

	    userNick = playerName + Config.getIrcdIngameSuffix();
	    userUser = playerName;
	    userHost = player.getAddress().getAddress().getHostName();
	} else {
	    userNick = Config.getIrcdServerName();
	    userUser = userNick;
	    userHost = Config.getIrcdServerHostName();
	}

	final String topic = ChatUtils.join(args, " ", 0);
	final String hostmask = userNick + "!" + userUser + "@" + userHost;
	IRCFunctionality.setTopic(ChatUtils.convertColors(topic, false), userNick, hostmask);

	if (topic.length() > 0) {
	    sender.sendMessage(ChatColor.RED + "Topic set to " + topic);
	} else {
	    sender.sendMessage(ChatColor.RED + "Topic cleared");
	}

	return true;
    }
}
