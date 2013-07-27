package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.BukkitPlayer;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.configuration.Config;

public class IRCMsgCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		final Player player = sender instanceof Player ? (Player)sender : null;

		// Process arguments
		if (args.length < 2) {
			return false; // prints usage
		}
		final String targetNick = args[0];
		final String message    = IRCd.convertColors(IRCd.join(args, " ", 1), false);

		final IRCUser targetIrcUser = IRCd.getIRCUser(targetNick);
		if (targetIrcUser == null) {
			sender.sendMessage(ChatColor.RED + "That user is not online.");
			return true;
		}

		switch (IRCd.mode){
		case STANDALONE:

			// Compute source
			final String sourceNick;
			final String sourceUser;
			final String sourceHost;
			if (player == null) {
				sourceNick = Config.getIrcdServerName();
				sourceUser = sourceNick;
				sourceHost = Config.getIrcdServerHostName();
			} else {
				sourceUser = player.getName();
				sourceNick = sourceUser + Config.getIrcdIngameSuffix();
				sourceHost = player.getAddress().getAddress().getHostName();
			}
			final String sourceHostmask = sourceNick + "!" + sourceUser + "@" + sourceHost;

			IRCd.writeTo(	targetIrcUser.nick,
							":"		+ sourceHostmask
									+ " PRIVMSG "
									+ targetIrcUser.nick
									+ " :"
									+ message);
			break;

		case INSPIRCD:
			if (!IRCd.isLinkcompleted()) {
				sender.sendMessage(ChatColor.RED + "Failed to send message, not currently linked to IRC server.");
				return true;
			}

			// Compute source
			final String sourceUID;
			if (player == null) {
				sourceUID = IRCd.serverUID;
			} else {
				final BukkitPlayer bp = IRCd.getBukkitUserObject(player.getName());
				if (bp == null) {
					sender.sendMessage(ChatColor.RED + "Failed to send message, you could not be found in the UID list. This should not happen, please report it to Jdbye.");
					return true;
				}
				sourceUID = bp.getUID();
			}

			// Compute target
			final String targetUID = IRCd.getUIDFromIRCUser(targetIrcUser);
			if (targetUID == null) {
				BukkitIRCdPlugin.log.severe("ircuser not found in list: " + targetIrcUser.nick);
				// Log this as severe since it should never occur unless something is wrong with the code
				sender.sendMessage(ChatColor.RED + "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
				return true;
			}

			IRCd.privmsg(sourceUID, targetUID, message);
			break;
		}

		final String targetModes = targetIrcUser.getTextModes();
		sender.sendMessage(
				IRCd.msgSendQueryFromIngame
					.replace("{Prefix}", IRCd.getGroupPrefix(targetModes))
					.replace("{Suffix}", IRCd.getGroupSuffix(targetModes))
					.replace("{User}", targetIrcUser.nick)
					.replace("{Message}", message));

		return true;
	}
}
