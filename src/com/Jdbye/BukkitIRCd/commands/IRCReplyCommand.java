package com.Jdbye.BukkitIRCd.commands;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.BukkitPlayer;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.Modes;
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
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (player.hasPermission("bukkitircd.reply")) {
				if (args.length > 0) {
					String lastReceivedFrom = thePlugin.lastReceived.get(player
							.getName());
					if (lastReceivedFrom == null) {
						player.sendMessage(ChatColor.RED
								+ "There are no messages to reply to!");
					} else {
						IRCUser ircuser = IRCd.getIRCUser(lastReceivedFrom);
						if (ircuser != null) {
							if (IRCd.mode == Modes.STANDALONE) {
								IRCd.writeTo(
										ircuser.nick,
										":"
												+ player.getName()
												+ Config.getIrcdIngameSuffix()
												+ "!"
												+ player.getName()
												+ "@"
												+ player.getAddress()
														.getAddress()
														.getHostName()
												+ " PRIVMSG "
												+ ircuser.nick
												+ " :"
												+ IRCd.convertColors(
														IRCd.join(args, " ", 0),
														false));
								player.sendMessage(IRCd.msgSendQueryFromIngame
										.replace(
												"{Prefix}",
												IRCd.getGroupPrefix(ircuser
														.getTextModes()))
										.replace(
												"{Suffix}",
												IRCd.getGroupSuffix(ircuser
														.getTextModes()))
										.replace("{User}", ircuser.nick)
										.replace(
												"{Message}",
												IRCd.convertColors(
														IRCd.join(args, " ", 0),
														false)));
							} else if (IRCd.mode == Modes.INSPIRCD) {
								BukkitPlayer bp;
								if ((bp = IRCd.getBukkitUserObject(player
										.getName())) != null) {
									String UID = IRCd
											.getUIDFromIRCUser(ircuser);
									if (UID != null) {
										if (IRCd.isLinkcompleted()) {
											IRCd.privmsg(bp.getUID(), UID, IRCd.convertColors(
															IRCd.join(args,
																	" ", 0),
															false));

											player.sendMessage(IRCd.msgSendQueryFromIngame
													.replace(
															"{Prefix}",
															IRCd.getGroupPrefix(ircuser
																	.getTextModes()))
													.replace(
															"{Suffix}",
															IRCd.getGroupSuffix(ircuser
																	.getTextModes()))
													.replace("{User}",
															ircuser.nick)
													.replace(
															"{Message}",
															IRCd.convertColors(
																	IRCd.join(
																			args,
																			" ",
																			0),
																	false)));
											;
										} else
											player.sendMessage(ChatColor.RED
													+ "Failed to send message, not currently linked to IRC server.");
									} else {
										BukkitIRCdPlugin.log
												.severe("UID not found in list: "
														+ UID); // Log this as
																// severe since
																// it should
																// never occur
																// unless
																// something is
																// wrong with
																// the code
										player.sendMessage(ChatColor.RED
												+ "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
									}
								} else
									player.sendMessage(ChatColor.RED
											+ "Failed to send message, you could not be found in the UID list. This should not happen, please report it to Jdbye.");
							}
						} else {
							player.sendMessage(ChatColor.RED
									+ "That user is not online.");
						}
					}
				} else {
					player.sendMessage(ChatColor.RED
							+ "Please provide a nickname and a message.");
					return false;
				}
			} else {
				player.sendMessage(ChatColor.RED
						+ "You don't have access to that command.");
			}
			return true;
		} else {
			if (args.length > 0) {
				String lastReceivedFrom = thePlugin.lastReceived
						.get("@CONSOLE@");
				if (lastReceivedFrom == null) {
					sender.sendMessage(ChatColor.RED
							+ "There are no messages to reply to!");
				} else {
					IRCUser ircuser = IRCd.getIRCUser(lastReceivedFrom);
					if (ircuser != null) {
						if (IRCd.mode == Modes.STANDALONE) {
							IRCd.writeTo(
									ircuser.nick,
									":"
											+ Config.getIrcdServerName()
											+ "!"
											+ Config.getIrcdServerName()
											+ "@"
											+ Config.getIrcdServerHostName()
											+ " PRIVMSG "
											+ ircuser.nick
											+ " :"
											+ IRCd.convertColors(
													IRCd.join(args, " ", 0),
													false));
							sender.sendMessage(IRCd.msgSendQueryFromIngame
									.replace(
											"{Prefix}",
											IRCd.getGroupPrefix(ircuser
													.getTextModes()))
									.replace(
											"{Suffix}",
											IRCd.getGroupSuffix(ircuser
													.getTextModes()))
									.replace("{User}", ircuser.nick)
									.replace(
											"{Message}",
											IRCd.convertColors(
													IRCd.join(args, " ", 0),
													false)));
							;
						} else if (IRCd.mode == Modes.INSPIRCD) {
							String UID = IRCd.getUIDFromIRCUser(ircuser);
							if (UID != null) {
								if (IRCd.isLinkcompleted()) {
									IRCd.privmsg(IRCd.serverUID, UID, IRCd.convertColors(
											IRCd.join(args, " ", 0),
											false));
									sender.sendMessage(IRCd.msgSendQueryFromIngame
											.replace(
													"{Prefix}",
													IRCd.getGroupPrefix(ircuser
															.getTextModes()))
											.replace(
													"{Suffix}",
													IRCd.getGroupSuffix(ircuser
															.getTextModes()))
											.replace("{User}", ircuser.nick)
											.replace(
													"{Message}",
													IRCd.convertColors(
															IRCd.join(args,
																	" ", 0),
															false)));
									;
								} else
									sender.sendMessage(ChatColor.RED
											+ "Failed to send message, not currently linked to IRC server.");
							} else {
								BukkitIRCdPlugin.log
										.severe("UID not found in list: " + UID); // Log
																					// this
																					// as
																					// severe
																					// since
																					// it
																					// should
																					// never
																					// occur
																					// unless
																					// something
																					// is
																					// wrong
																					// with
																					// the
																					// code
								sender.sendMessage(ChatColor.RED
										+ "Failed to send message, UID not found. This should not happen, please report it to Jdbye.");
							}
						}
					} else {
						sender.sendMessage(ChatColor.RED
								+ "That user is not online.");
					}
				}
			} else {
				sender.sendMessage(ChatColor.RED
						+ "Please provide a nickname and a message.");
				return false;
			}
			return true;
		}
	}

}
