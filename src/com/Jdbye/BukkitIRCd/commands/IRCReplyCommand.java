package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.BukkitPlayer;
import com.Jdbye.BukkitIRCd.BukkitUserManagement;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCUserManagement;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IRCReplyCommand implements CommandExecutor {

    private final BukkitIRCdPlugin thePlugin;

    public IRCReplyCommand(BukkitIRCdPlugin plugin) {
	this.thePlugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
	    String[] args) {

	if (args.length < 1) {
	    return false; // Print usage message
	}

	final Player player = sender instanceof Player ? (Player) sender : null;

	// Determine sender name
	final String senderName;
	if (player == null) {
	    senderName = "@CONSOLE@";
	} else {
	    senderName = player.getName();
	}

	// Determine target name
	final String lastReceivedFrom = thePlugin.lastReceived.get(senderName);

	// Verify target
	if (lastReceivedFrom == null) {
	    sender.sendMessage(ChatColor.RED +
		    "There are no messages to reply to!");
	    return true;
	}

	// Lookup target's user object
	final IRCUser ircuser = IRCUserManagement.getIRCUser(lastReceivedFrom);
	if (ircuser == null) {
	    sender.sendMessage(ChatColor.RED + "Player offline");
	    return true;
	}

	// Attempt to send IRC message
	switch (IRCd.mode) {
	    case STANDALONE:
		if (player == null) {
		    IRCd.writeTo(
			    ircuser.nick,
			    ":" +
			    Config.getIrcdServerName() +
			    "!" +
			    Config.getIrcdServerName() +
			    "@" +
			    Config.getIrcdServerHostName() +
			    " PRIVMSG " +
			    ircuser.nick +
			    " :" +
			    IRCd.convertColors(IRCd.join(args, " ", 0),
				    false));
		} else {
		    IRCd.writeTo(
			    ircuser.nick,
			    ":" +
			    player.getName() +
			    Config.getIrcdIngameSuffix() +
			    "!" +
			    player.getName() +
			    "@" +
			    player.getAddress().getAddress()
			    .getHostName() +
			    " PRIVMSG " +
			    ircuser.nick +
			    " :" +
			    IRCd.convertColors(IRCd.join(args, " ", 0),
				    false));
		}
		break;
	    case INSPIRCD:
		if (!IRCd.isLinkcompleted()) {
		    sender.sendMessage(ChatColor.RED +
			    "Failed to send message, not currently linked to IRC server.");
		    return true;
		}

		final String UID = IRCUserManagement.getUIDFromIRCUser(ircuser);
		if (UID == null) {
		    sender.sendMessage(ChatColor.RED +
			    "Failed to reply, UID not found");
		    return true;
		}

		if (player == null) {
		    IRCd.privmsg(IRCd.serverUID, UID,
			    IRCd.convertColors(IRCd.join(args, " ", 0), false));
		} else {
		    final BukkitPlayer bp = BukkitUserManagement.getUserObject(player
			    .getName());
		    if (bp == null) {
			sender.sendMessage(ChatColor.RED +
				"Internal error, unable to reply");
			return true;
		    }

		    IRCd.privmsg(bp.getUID(), UID,
			    IRCd.convertColors(IRCd.join(args, " ", 0), false));
		}

		break;
	}

	// Report command to sender's chat
	sender.sendMessage(IRCd.msgSendQueryFromIngame
		.replace("{Prefix}",
			IRCd.getGroupPrefix(ircuser.getTextModes()))
		.replace("{Suffix}",
			IRCd.getGroupSuffix(ircuser.getTextModes()))
		.replace("{User}", ircuser.nick)
		.replace(
			"{Message}",
			ChatColor.translateAlternateColorCodes('&',
				IRCd.join(args, " ", 0))));

	return true;
    }

}
