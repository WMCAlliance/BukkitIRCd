package com.Jdbye.BukkitIRCd;

import com.Jdbye.BukkitIRCd.configuration.Config;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Monofraps
 */
public class ClientConnection implements Runnable {
	private final Socket server;
	private String line;
	public String nick, realname, ident, hostmask, ipaddress;
	public String modes = "";
	public String customWhois = ""; // Not used yet
	public boolean isIdented = false;
	public boolean isNickSet = false;
	public boolean isRegistered = false;
	public String accountname = "";
	public boolean isOper = false;
	public String awayMsg = "";
	public long lastPingResponse;
	public long signonTime;
	public long lastActivity;
	private BufferedReader in;
	private PrintStream out;
	public boolean running = true;

	ClientConnection(Socket server) {
		this.server = server;
		try {
			this.server.setSoTimeout(3000);
		} catch (SocketException e) {
		}
	}

	@Override
	public void run() {
		if (running) {
			try {
				nick = "";
				in = new BufferedReader(new InputStreamReader(
						server.getInputStream()));
				out = new PrintStream(server.getOutputStream());

				hostmask = server.getInetAddress().getHostName().toString();
				ipaddress = server.getInetAddress().getHostAddress().toString();
				Thread.currentThread().setName(
						"Thread-BukkitIRCd-Connection-" + ipaddress);
				synchronized (IRCd.csStdOut) {
					System.out.println("[BukkitIRCd] Got connection from "
							+ ipaddress);
				}

				lastPingResponse = System.currentTimeMillis();
				lastActivity = lastPingResponse;

				if ((IRCd.isBanned(nick + "!" + ident + "@" + hostmask))
						|| (IRCd.isBanned(nick + "!" + ident + "@" + ipaddress))) {
					writeln("ERROR :Closing Link: [" + ipaddress
							+ "] (You are banned from this server)");
					disconnect();
					synchronized (IRCd.csStdOut) {
						System.out
								.println("[BukkitIRCd] Cleaning up connection from "
										+ getFullHost() + " (banned)");
					}
					if (isIdented && isNickSet)
						IRCd.writeAll(":" + getFullHost()
								+ " QUIT :You are banned from this server");
					IRCd.removeIRCUser(nick, "Banned", true);
				} else
					while (server.isConnected() && (!server.isClosed())) {
						try {
							if (lastPingResponse
									+ (Config.getIrcdPinkTimeoutInterval() * 1000) < System
										.currentTimeMillis()) {
								writeln("ERROR :Closing Link: [" + ipaddress
										+ "] (Ping timeout)");
								writeln("ERROR :Closing Link: [" + ipaddress
										+ "] (Ping timeout)");
								disconnect();
								;
								synchronized (IRCd.csStdOut) {
									System.out
											.println("[BukkitIRCd] Cleaning up connection from "
													+ getFullHost()
													+ " (ping timeout)");
								}
								if (isIdented && isNickSet)
									IRCd.writeAll(":" + getFullHost()
											+ " QUIT :Ping timeout");
								IRCd.removeIRCUser(nick, "Ping timeout", true);
							} else {
								// Get input from the client
								while ((line = in.readLine()) != null
										&& !line.equals(".")) {
									parseMessage(line);
								}
							}
						} catch (SocketTimeoutException e) {
						}
						try {
							Thread.currentThread();
							Thread.sleep(1);
						} catch (InterruptedException e) {
						}
					}
			} catch (IOException ioe) {
				synchronized (IRCd.csStdOut) {
					System.out
							.println("[BukkitIRCd] IOException on socket connection: "
									+ ioe);
				}
			}

			synchronized (IRCd.csStdOut) {
				System.out.println("[BukkitIRCd] Cleaning up connection from "
						+ getFullHost() + " (client quit)");
			}
			IRCd.removeIRCUser(nick);
			running = false;
			synchronized (IRCd.csStdOut) {
				System.out.println("[BukkitIRCd] Lost connection from "
						+ getFullHost());
			}
		}
	}

	private void parseMessage(String line) {
		String[] split = line.split(" ");
		if (split[0].equalsIgnoreCase("NICK")) {
			if (split.length >= 2) {
				if (split[1].indexOf(":") == 0) {
					split[1] = split[1].substring(1);
				}
			}
			if (split.length < 2) {
				writeln(IRCd.serverMessagePrefix + " 431  :No nickname given");
			} else if (!split[1]
					.matches("\\A[a-zA-Z_\\-\\[\\]\\\\^{}|`][a-zA-Z0-9_\\-\\[\\]\\\\^{}|`]*\\z")) {
				writeln(IRCd.serverMessagePrefix + " 432 " + nick + " "
						+ split[1] + " :Erroneous Nickname: Illegal characters");
			} else {
				if (split[1].length() > Config.getIrcdMaxNickLength()) {
					split[1] = split[1].substring(0,
							Config.getIrcdMaxNickLength());
				}
				if ((IRCd.getIRCUser(split[1]) != null)
						|| (split[1].equalsIgnoreCase(Config
								.getIrcdServerName()))) {
					writeln(IRCd.serverMessagePrefix + " 433 * " + split[1]
							+ " :Nickname is already in use.");
				} else {
					if (!isNickSet) {
						isNickSet = true;
						nick = split[1];
						if (isIdented) {
							sendWelcome();
						}
					} else if (isIdented) {
						lastActivity = System.currentTimeMillis();

						BukkitIRCdPlugin.thePlugin.updateLastReceived(nick,
								split[1]);
						if ((IRCd.isPlugin())
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if (IRCd.msgIRCNickChange.length() > 0)
								IRCd.broadcastMessage(IRCd.msgIRCNickChange
										.replace("{OldNick}", nick)
										.replace("{Prefix}",
												IRCd.getGroupPrefix(modes))
										.replace("{Suffix}",
												IRCd.getGroupSuffix(modes))
										.replace("{NewNick}", split[1]));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (IRCd.msgIRCNickChangeDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap
										.sendBroadcastToWeb(
												"IRC",
												IRCd.msgIRCNickChangeDynmap
														.replace("{OldNick}",
																nick).replace(
																"{NewNick}",
																split[1]));
						}
						IRCd.writeAll(":" + getFullHost() + " NICK " + split[1]);
						nick = split[1];
					}
				}
			}
		} else if (split[0].equalsIgnoreCase("USER")) {
			if (split.length < 2) {
				writeln(IRCd.serverMessagePrefix
						+ " 461  USER :Not enough parameters");
			} else {
				if (split[4].indexOf(":") == 0) {
					split[4] = split[4].substring(1);
				}
				if (!isIdented) {
					isIdented = true;
					ident = split[1];
					realname = split[4];
					if (isNickSet) {
						sendWelcome();
					}
				} else {
					writeln(IRCd.serverMessagePrefix + " 462 " + nick
							+ " :You are already registered");
				}
			}
		} else if (split[0].equalsIgnoreCase("QUIT")) {
			String quitmsg = null;
			if (split.length > 1) {
				if (split[1].indexOf(":") == 0) {
					split[1] = split[1].substring(1);
				}
				quitmsg = IRCd.join(split, " ", 1);
				if (isIdented && isNickSet)
					IRCd.writeAll(":" + getFullHost() + " QUIT :Quit: "
							+ quitmsg);
				synchronized (IRCd.csStdOut) {
					System.out
							.println("[BukkitIRCd] Cleaning up connection from "
									+ getFullHost()
									+ " (Quit: "
									+ quitmsg
									+ ")");
				}
			} else {
				if (isIdented && isNickSet)
					IRCd.writeAll(":" + getFullHost() + " QUIT :Quit");
				synchronized (IRCd.csStdOut) {
					System.out
							.println("[BukkitIRCd] Cleaning up connection from "
									+ getFullHost() + " (Quit)");
				}
			}
			disconnect();
			if (quitmsg != null)
				IRCd.removeIRCUser(nick, quitmsg, true);
			else
				IRCd.removeIRCUser(nick);
		} else if (isIdented && isNickSet) {
			// if (split[0].equalsIgnoreCase("STOP")) {
			// System.exit(0);
			// }
			if (split[0].equalsIgnoreCase("PING")) {
				if (split.length < 2) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					if (split[1].indexOf(":") == 0) {
						split[1] = split[1].substring(1);
					}
					writeln("PONG :" + IRCd.join(split, " ", 1));
				}
			} else if (split[0].equalsIgnoreCase("PONG")) {
				lastPingResponse = System.currentTimeMillis();
			} else if (split[0].equalsIgnoreCase("MOTD")) {
				lastActivity = System.currentTimeMillis();
				sendMOTD();
			} else if (split[0].equalsIgnoreCase("WHOIS")) {
				lastActivity = System.currentTimeMillis();
				if (split.length < 2) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					if (split[1].indexOf(":") == 0) {
						split[1] = split[1].substring(1);
					}
					sendWhois(split[1]);
				}
			} else if (split[0].equalsIgnoreCase("NAMES")) {
				if (split.length > 1) {
					if (!sendChanNames(split[1])) {
						writeln(IRCd.serverMessagePrefix + " 366 " + nick + " "
								+ split[1] + " :End of /NAMES list.");
					}
				}
			} else if (split[0].equalsIgnoreCase("TOPIC")) {
				lastActivity = System.currentTimeMillis();
				if (split.length < 2) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else if (split.length == 2) {
					if (!sendChanTopic(split[1]))
						writeln(IRCd.serverMessagePrefix + " 401 " + nick + " "
								+ split[1] + " :No such nick/channel");
				} else {
					if (split[2].indexOf(":") == 0) {
						split[2] = split[2].substring(1);
					}
					if (split[1].equalsIgnoreCase(Config.getIrcdChannel())) {
						String chantopic = IRCd.join(split, " ", 2);
						if (modes.contains("%") || modes.contains("@")
								|| modes.contains("&") || modes.contains("~")) {
							IRCd.setTopic(chantopic, nick, getFullHost());
						} else {
							// Not op
							writeln(IRCd.serverMessagePrefix + " 482 " + nick
									+ " " + Config.getIrcdChannel()
									+ " :You're not channel operator");
						}
					} else if (split[1].equalsIgnoreCase(Config
							.getIrcdConsoleChannel())) {
					} // Do nothing
					else {
						writeln(IRCd.serverMessagePrefix + " 401 " + nick + " "
								+ split[1] + " :No such nick/channel");
					}
				}
			} else if (split[0].equalsIgnoreCase("MODE")) {
				if (split.length < 2) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else if (split.length == 2) {
					if (!sendChanModes(split[1]))
						writeln(IRCd.serverMessagePrefix + " 401 " + nick + " "
								+ split[1] + " :No such nick/channel");
				} else {
					if (split[1].equalsIgnoreCase(Config
							.getIrcdConsoleChannel())) {
					} // Do nothing
					else if (split[1].equalsIgnoreCase(Config.getIrcdChannel())) {

						if (split[2].indexOf(":") == 0) {
							split[2] = split[2].substring(1);
						}

						int add = -1;
						int i = 0, i2 = 0;
						if (split.length == 3) {
							if ((split[2].equals("+b"))
									|| (split[2].equals("-b"))) {
								// Send list of bans
								synchronized (IRCd.csIrcBans) {
									for (IrcBan ban : IRCd.ircBans) {
										writeln(IRCd.serverMessagePrefix
												+ " 367 " + nick + " "
												+ Config.getIrcdChannel() + " "
												+ ban.fullHost + " "
												+ ban.bannedBy + " "
												+ ban.banTime);
									}
									writeln(IRCd.serverMessagePrefix + " 368 "
											+ nick + " "
											+ Config.getIrcdChannel()
											+ " :End of Channel Ban List");
								}
							}
						} else
							while (i < split[2].length()) {
								if (split[2].substring(i, i + 1).equals("+"))
									add = 1;
								else if (split[2].substring(i, i + 1).equals(
										"-"))
									add = 0;
								else if (split[2].substring(i, i + 1).equals(
										"b")) {
									if (i2 + 3 < split.length) {
										String mask = split[i2 + 3];
										// They actually want to ban/unban
										// someone
										if (modes.contains("%")
												|| modes.contains("@")
												|| modes.contains("&")
												|| modes.contains("~")) {
											// User is op
											String host;
											if (IRCd.wildCardMatch(mask,
													"*!*@*"))
												host = mask;
											else if (IRCd.wildCardMatch(mask,
													"*!*"))
												host = mask + "@*";
											else if (IRCd.wildCardMatch(mask,
													"*@*"))
												host = "*!" + mask;
											else
												host = mask + "!*@*";
											if (add == 1) {
												if (IRCd.msgIRCBan.length() > 0)
													IRCd.broadcastMessage(IRCd.msgIRCBan
															.replace(
																	"{BannedUser}",
																	host)
															.replace(
																	"{BannedBy}",
																	nick));
												if ((BukkitIRCdPlugin.dynmap != null)
														&& (IRCd.msgIRCBanDynmap
																.length() > 0))
													BukkitIRCdPlugin.dynmap
															.sendBroadcastToWeb(
																	"IRC",
																	IRCd.msgIRCBanDynmap
																			.replace(
																					"{BannedUser}",
																					host)
																			.replace(
																					"{BannedBy}",
																					nick));
												IRCd.banIRCUser(host,
														getFullHost());
											} else if (add == 0) {
												if (IRCd.msgIRCUnban.length() > 0)
													IRCd.broadcastMessage(IRCd.msgIRCUnban
															.replace(
																	"{BannedUser}",
																	host)
															.replace(
																	"{BannedBy}",
																	nick));
												if ((BukkitIRCdPlugin.dynmap != null)
														&& (IRCd.msgIRCUnbanDynmap
																.length() > 0))
													BukkitIRCdPlugin.dynmap
															.sendBroadcastToWeb(
																	"IRC",
																	IRCd.msgIRCUnbanDynmap
																			.replace(
																					"{BannedUser}",
																					host)
																			.replace(
																					"{BannedBy}",
																					nick));
												IRCd.unBanIRCUser(host,
														getFullHost());
											}

										} else {
											// Not op
											writeln(IRCd.serverMessagePrefix
													+ " 482 "
													+ nick
													+ " "
													+ Config.getIrcdChannel()
													+ " :You're not channel operator");
											break;
										}
									}
									i2++;
								}
								i++;
							}
					} else if (split[1].equalsIgnoreCase(nick)) {
						if ((isOper) && (split[2].startsWith("-"))
								&& (split[2].contains("o"))) {
							// Deoper
							isOper = false;
							writeln(":" + nick + " MODE " + nick + " :-o");
							IRCd.writeAll(":" + getFullHost() + " PART "
									+ Config.getIrcdConsoleChannel()
									+ " :De-opered");
						}
					} else {
						writeln(IRCd.serverMessagePrefix + " 401 " + nick + " "
								+ split[1] + " :No such nick/channel");
					}
				}

			} else if (split[0].equalsIgnoreCase("USERHOST")) {
				if (split.length < 2) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					int i = 1;
					String hosts = "";
					while (i < split.length) {
						if (split[i].indexOf(":") == 0) {
							split[i] = split[i].substring(1);
						}
						int ID;
						IRCUser ircuser;
						String targethost = null, targetnick = null, targetident = null;
						if (split[i].equalsIgnoreCase(Config
								.getIrcdServerName())) {
							targetnick = Config.getIrcdServerName();
							targetident = Config.getIrcdServerName();
							targethost = Config.getIrcdServerHostName();
							hosts += targetnick + "=+" + targetident + "@"
									+ targethost + " ";
						} else if ((ircuser = IRCd.getIRCUser(split[i])) != null) {
							synchronized (IRCd.csIrcUsers) {
								targetnick = ircuser.nick;
								targetident = ircuser.ident;
								targethost = ircuser.hostmask;
							}
							hosts += targetnick + "=+" + targetident + "@"
									+ targethost + " ";
						} else if ((ID = IRCd.getBukkitUser(split[i])) >= 0) {
							if ((IRCd.isPlugin())
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								synchronized (IRCd.csBukkitPlayers) {
									BukkitPlayer p = IRCd.bukkitPlayers.get(ID);
									targetnick = p.nick
											+ Config.getIrcdIngameSuffix();
									targetident = p.nick;
									targethost = p.host;
								}
							}
							hosts += targetnick + "=+" + targetident + "@"
									+ targethost + " ";
						}
						i++;
						if (i > 5)
							break;
					}
					if (hosts.length() > 0)
						hosts = hosts.substring(0, hosts.length() - 1);
					writeln(IRCd.serverMessagePrefix + " 302 " + nick + " :"
							+ hosts);
				}
			} else if (split[0].equalsIgnoreCase("KICK")) {
				if (split.length < 3) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					// Kick someone
					if (modes.contains("%") || modes.contains("@")
							|| modes.contains("&") || modes.contains("~")) {
						// User is op
						String bannick = split[2];
						String reason = null;
						if (split.length > 3) {
							if (split[3].indexOf(":") == 0) {
								split[3] = split[3].substring(1);
							}
							reason = IRCd.join(split, " ", 3);
						}

						IRCUser ircuser;
						if ((ircuser = IRCd.getIRCUser(bannick)) != null) {
							IRCd.kickIRCUser(ircuser, nick, ident, hostmask,
									reason, false);
						} else if ((IRCd.getBukkitUser(bannick)) >= 0) {
							if ((IRCd.isPlugin())
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								if (bannick.endsWith(Config
										.getIrcdIngameSuffix()))
									bannick = bannick
											.substring(
													0,
													bannick.length()
															- Config.getIrcdIngameSuffix()
																	.length());

								if (reason != null) {
									if (IRCd.msgIRCKickReason.length() > 0)
										IRCd.broadcastMessage(IRCd.msgIRCKickReason
												.replace("{KickedUser}",
														bannick)
												.replace("{KickedBy}", nick)
												.replace(
														"{Reason}",
														IRCd.convertColors(
																reason, true)));
									IRCd.kickPlayerIngame(nick, bannick,
											IRCd.stripIRCFormatting(reason));
								} else {
									if (IRCd.msgIRCKick.length() > 0)
										IRCd.broadcastMessage(IRCd.msgIRCKick
												.replace("{KickedUser}",
														bannick).replace(
														"{KickedBy}", nick));
									IRCd.kickPlayerIngame(nick, bannick, null);
								}
							}
						} else {
							writeln(IRCd.serverMessagePrefix + " 401 " + nick
									+ " " + bannick + " :No such nick/channel");
						}
					} else {
						// Not op
						writeln(IRCd.serverMessagePrefix + " 482 " + nick + " "
								+ Config.getIrcdChannel()
								+ " :You're not channel operator");
					}
				}
			} else if (split[0].equalsIgnoreCase("OPER")) {
				if (split.length < 3) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					String user = split[1];
					String pass = Hash.compute(split[2], HashType.SHA_512);
					if ((Config.getIrcdOperUser().length() > 0)
							&& (Config.getIrcdOperPass().length() > 0)
							&& (user.equals(Config.getIrcdOperUser()))
							&& (pass.equals(Config.getIrcdOperPass()))) {
						// Correct login
						isOper = true;
						writeln(":" + nick + " MODE " + nick + " :+o");
						writeln(IRCd.serverMessagePrefix + " 381 " + nick
								+ " :You are now an IRC Operator");
						if (Config.getIrcdOperModes().length() > 0) {
							modes = Config.getIrcdOperModes();
							String mode1 = "+", mode2 = "";
							if (modes.contains("~")) {
								mode1 += "q";
								mode2 += nick + " ";
							}
							if (modes.contains("&")) {
								mode1 += "a";
								mode2 += nick + " ";
							}
							if (modes.contains("@")) {
								mode1 += "o";
								mode2 += nick + " ";
							}
							if (modes.contains("%")) {
								mode1 += "h";
								mode2 += nick + " ";
							}
							if (modes.contains("+")) {
								mode1 += "v";
								mode2 += nick + " ";
							}

							sendChanWelcome(Config.getIrcdConsoleChannel());
							if (!mode1.equals("+")) {
								IRCd.writeAll(":"
										+ Config.getIrcdServerName()
										+ "!"
										+ Config.getIrcdServerName()
										+ "@"
										+ Config.getIrcdServerHostName()
										+ " MODE "
										+ Config.getIrcdChannel()
										+ " "
										+ mode1
										+ " "
										+ mode2.substring(0, mode2.length() - 1));
								IRCd.writeAll(":"
										+ Config.getIrcdServerName()
										+ "!"
										+ Config.getIrcdServerName()
										+ "@"
										+ Config.getIrcdServerHostName()
										+ " MODE "
										+ Config.getIrcdConsoleChannel()
										+ " "
										+ mode1
										+ " "
										+ mode2.substring(0, mode2.length() - 1));
							}
						}
					} else {
						// Incorrect login
						writeln(IRCd.serverMessagePrefix + " 464 " + nick
								+ " :Password Incorrect");
					}
				}
			} else if (split[0].equalsIgnoreCase("JOIN")) {
				// Do nothing
			} else if (split[0].equalsIgnoreCase("PART")) {
				// Do nothing
			} else if (split[0].equalsIgnoreCase("ISON")) {
				if (split.length < 2) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					int i = 1;
					String nicks = "";
					while (i < split.length) {
						if (split[i].indexOf(":") == 0) {
							split[i] = split[i].substring(1);
						}

						int ID;
						IRCUser ircuser;
						if (split[i].equalsIgnoreCase(Config
								.getIrcdServerName()))
							nicks += Config.getIrcdServerName() + " ";
						else if ((ircuser = IRCd.getIRCUser(split[i])) != null)
							nicks += ircuser.nick + " ";
						else if ((ID = IRCd.getBukkitUser(split[i])) >= 0)
							nicks += IRCd.bukkitPlayers.get(ID).nick
									+ Config.getIrcdIngameSuffix() + " ";
						i++;
					}
					if (nicks.length() > 0)
						nicks = nicks.substring(0, nicks.length() - 1);
					writeln(IRCd.serverMessagePrefix + " 303 " + nick + " :"
							+ nicks);
				}
			} else if (split[0].equalsIgnoreCase("AWAY")) {
				if (split.length > 1) {
					if (split[1].indexOf(":") == 0) {
						split[1] = split[1].substring(1);
					}
					awayMsg = IRCd.join(split, " ", 1);
					writeln(IRCd.serverMessagePrefix + " 306 " + nick
							+ " :You have been marked as being away");
				} else {
					awayMsg = "";
					writeln(IRCd.serverMessagePrefix + " 305 " + nick
							+ " :You are no longer marked as being away");
				}
			} else if (split[0].equalsIgnoreCase("WHO")) {
				boolean operOnly = false;
				String pattern = "";
				if (split.length >= 2)
					pattern = split[1];
				if ((split.length > 2) && (split[2].equalsIgnoreCase("o"))) {
					operOnly = true;
				}
				sendWho(pattern, operOnly);
			} else if (split[0].equalsIgnoreCase("PRIVMSG")) {
				lastActivity = System.currentTimeMillis();
				if (split.length < 3) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					if (split[2].indexOf(":") == 0) {
						split[2] = split[2].substring(1);
					}
					String message = IRCd.join(split, " ", 2);
					boolean isCTCP = (message.startsWith((char) 1 + "") && message
							.endsWith((char) 1 + ""));
					boolean isAction = (message.startsWith((char) 1 + "ACTION") && message
							.endsWith((char) 1 + ""));
					String message2;
					if (isAction)
						message2 = IRCd.join(
								message.substring(1, message.length() - 1)
										.split(" "), " ", 1);
					else
						message2 = message;

					if (split[1].equalsIgnoreCase(Config.getIrcdChannel())) {
						if (IRCd.isBanned(getFullHost())) {
							writeln(IRCd.serverMessagePrefix + " 404 " + nick
									+ " " + Config.getIrcdChannel()
									+ " :You are banned ("
									+ Config.getIrcdChannel() + ")");
						} else {
							if ((IRCd.isPlugin())
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								if (message.equalsIgnoreCase("!players")
										&& (!isCTCP)) {
									if (IRCd.msgPlayerList.length() > 0) {
										String s = "";
										int count = 0;
										for (BukkitPlayer player : IRCd.bukkitPlayers) {
											count++;
											s = s + player.nick + ", ";
										}
										if (s.length() == 0)
											s = "None, ";
										IRCd.writeAll(":"
												+ Config.getIrcdServerName()
												+ "!"
												+ Config.getIrcdServerName()
												+ "@"
												+ Config.getIrcdServerHostName()
												+ " PRIVMSG "
												+ Config.getIrcdChannel()
												+ " :"
												+ IRCd.convertColors(
														IRCd.msgPlayerList
																.replace(
																		"{Count}",
																		Integer.toString(count))
																.replace(
																		"{Users}",
																		s.substring(
																				0,
																				s.length() - 2)),
														false));
									}
								} else if (isAction || (!isCTCP)) {
									if (isAction) {

										if (IRCd.msgIRCAction.length() > 0)
											IRCd.broadcastMessage(IRCd.msgIRCAction
													.replace("{User}", nick)
													.replace(
															"{Suffix}",
															IRCd.getGroupSuffix(modes))
													.replace(
															"{Prefix}",
															IRCd.getGroupPrefix(modes))
													.replace(
															"{Message}",
															IRCd.convertColors(
																	message2,
																	true)));
										if ((BukkitIRCdPlugin.dynmap != null)
												&& (IRCd.msgIRCActionDynmap
														.length() > 0))
											BukkitIRCdPlugin.dynmap
													.sendBroadcastToWeb(
															"IRC",
															IRCd.msgIRCActionDynmap
																	.replace(
																			"{User}",
																			nick)
																	.replace(
																			"{Message}",
																			IRCd.stripIRCFormatting(message2)));
									} else {

										if (IRCd.msgIRCMessage.length() > 0)
											IRCd.broadcastMessage(IRCd.msgIRCMessage
													.replace("{User}", nick)
													.replace(
															"{Suffix}",
															IRCd.getGroupSuffix(modes))
													.replace(
															"{Prefix}",
															IRCd.getGroupPrefix(modes))
													.replace(
															"{Message}",
															IRCd.convertColors(
																	message2,
																	true)));
										if ((BukkitIRCdPlugin.dynmap != null)
												&& (IRCd.msgIRCMessageDynmap
														.length() > 0))
											BukkitIRCdPlugin.dynmap
													.sendBroadcastToWeb(
															"IRC",
															IRCd.msgIRCMessageDynmap
																	.replace(
																			"{User}",
																			nick)
																	.replace(
																			"{Message}",
																			IRCd.stripIRCFormatting(message2)));
									}
								}
							}
							IRCd.writeAllExcept(nick, ":" + getFullHost()
									+ " PRIVMSG " + Config.getIrcdChannel()
									+ " :" + message);
						}
					} else if (split[1].equalsIgnoreCase(Config
							.getIrcdServerName())) {
						if ((isAction || (!isCTCP)) && (IRCd.isPlugin())
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							BukkitIRCdPlugin.thePlugin.setLastReceived(
									"@CONSOLE@", nick);
							if (isAction) {

								if (IRCd.msgIRCPrivateAction.length() > 0)
									BukkitIRCdPlugin.log
											.info(IRCd.msgIRCPrivateAction
													.replace("{User}", nick)
													.replace(
															"{Suffix}",
															IRCd.getGroupSuffix(modes))
													.replace(
															"{Prefix}",
															IRCd.getGroupPrefix(modes))
													.replace(
															"{Message}",
															IRCd.convertColors(
																	message2,
																	true)));
							} else {

								if (IRCd.msgIRCPrivateMessage.length() > 0)
									BukkitIRCdPlugin.log
											.info(IRCd.msgIRCPrivateMessage
													.replace("{User}", nick)
													.replace(
															"{Suffix}",
															IRCd.getGroupSuffix(modes))
													.replace(
															"{Prefix}",
															IRCd.getGroupPrefix(modes))
													.replace(
															"{Message}",
															IRCd.convertColors(
																	message2,
																	true)));
							}
						}
					} else if (split[1].equalsIgnoreCase(Config
							.getIrcdConsoleChannel())) {
						if ((!isAction) && (!isCTCP) && (IRCd.isPlugin())
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if (!isOper) {
								writeln(":" + Config.getIrcdServerName() + "!"
										+ Config.getIrcdServerName() + "@"
										+ Config.getIrcdServerHostName()
										+ " NOTICE " + nick
										+ " :You don't have access.");
							} else {
								// Execute console command here
								IRCd.writeOpersExcept(
										nick,
										":"
												+ getFullHost()
												+ " PRIVMSG "
												+ Config.getIrcdConsoleChannel()
												+ " :" + message);

								if (message.startsWith("!")) {
									message = message.substring(1);
									IRCd.executeCommand(message);
								}
							}
						}
					} else {
						IRCUser ircuser = IRCd.getIRCUser(split[1]);
						int user;
						if (ircuser != null) {
							IRCd.writeTo(ircuser.nick, ":" + getFullHost()
									+ " PRIVMSG " + split[1] + " :" + message);
						} else if ((user = IRCd.getBukkitUser(split[1])) >= 0) {
							if ((isAction || (!isCTCP)) && (IRCd.isPlugin())
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								synchronized (IRCd.csBukkitPlayers) {
									String bukkitnick = IRCd.bukkitPlayers
											.get(user).nick;
									BukkitIRCdPlugin.thePlugin.setLastReceived(
											bukkitnick, nick);
									if (isAction) {
										if (IRCd.msgIRCPrivateAction.length() > 0)
											IRCd.sendMessage(
													bukkitnick,
													IRCd.msgIRCPrivateAction
															.replace("{User}",
																	nick)
															.replace(
																	"{Suffix}",
																	IRCd.getGroupSuffix(modes))
															.replace(
																	"{Prefix}",
																	IRCd.getGroupPrefix(modes))
															.replace(
																	"{Message}",
																	IRCd.convertColors(
																			message2,
																			true)));
									} else {
										if (IRCd.msgIRCPrivateMessage.length() > 0)
											IRCd.sendMessage(
													bukkitnick,
													IRCd.msgIRCPrivateMessage
															.replace("{User}",
																	nick)
															.replace(
																	"{Suffix}",
																	IRCd.getGroupSuffix(modes))
															.replace(
																	"{Prefix}",
																	IRCd.getGroupPrefix(modes))
															.replace(
																	"{Message}",
																	IRCd.convertColors(
																			message2,
																			true)));
									}
								}
							}
						} else {
							writeln(IRCd.serverMessagePrefix + " 401 " + nick
									+ " " + split[1] + " :No such nick/channel");
						}
					}
				}
			} else if (split[0].equalsIgnoreCase("NOTICE")) {
				lastActivity = System.currentTimeMillis();
				if (split.length < 3) {
					writeln(IRCd.serverMessagePrefix + " 461  " + split[0]
							+ " :Not enough parameters");
				} else {
					if (split[2].indexOf(":") == 0) {
						split[2] = split[2].substring(1);
					}
					String message = IRCd.join(split, " ", 2);
					boolean isCTCP = (message.startsWith((char) 1 + "") && message
							.endsWith((char) 1 + ""));
					if (split[1].equalsIgnoreCase(Config
							.getIrcdConsoleChannel())) {
					} // Do nothing
					else if (split[1].equalsIgnoreCase(Config.getIrcdChannel())) {
						if ((IRCd.isPlugin())
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if ((!isCTCP) && Config.isIrcdNoticesEnabled()) {
								if (IRCd.msgIRCNotice.length() > 0)
									IRCd.broadcastMessage(IRCd.msgIRCNotice
											.replace("{User}", nick)
											.replace("{Prefix}",
													IRCd.getGroupPrefix(modes))
											.replace("{Suffix}",
													IRCd.getGroupSuffix(modes))
											.replace(
													"{Message}",
													IRCd.convertColors(message,
															true)));
								if ((BukkitIRCdPlugin.dynmap != null)
										&& (IRCd.msgIRCNoticeDynmap.length() > 0))
									BukkitIRCdPlugin.dynmap
											.sendBroadcastToWeb(
													"IRC",
													IRCd.msgIRCNoticeDynmap
															.replace("{User}",
																	nick)
															.replace(
																	"{Message}",
																	IRCd.stripIRCFormatting(message)));
							}
						}
						IRCd.writeAllExcept(nick, ":" + getFullHost()
								+ " NOTICE " + Config.getIrcdChannel() + " :"
								+ message);
					} else {
						IRCUser ircuser = IRCd.getIRCUser(split[1]);
						int user;
						if (ircuser != null) {
							IRCd.writeTo(ircuser.nick, ":" + getFullHost()
									+ " NOTICE " + split[1] + " :" + message);
						} else if ((user = IRCd.getBukkitUser(split[1])) >= 0) {
							if ((IRCd.isPlugin())
									&& (BukkitIRCdPlugin.thePlugin != null)
									&& (!isCTCP)
									&& (Config.isIrcdNoticesEnabled()))
								synchronized (IRCd.csBukkitPlayers) {
									if (IRCd.msgIRCPrivateMessage.length() > 0)
										IRCd.sendMessage(
												IRCd.bukkitPlayers.get(user).nick,
												IRCd.msgIRCPrivateAction
														.replace("{User}", nick)
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(modes))
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(modes))
														.replace(
																"{Message}",
																IRCd.convertColors(
																		message,
																		true)));
								}
						} else {
							writeln(IRCd.serverMessagePrefix + " 401 " + nick
									+ " " + split[1] + " :No such nick/channel");
						}
					}
				}
			} else {
				writeln(IRCd.serverMessagePrefix + " 421 " + nick + " "
						+ split[0] + " :Unknown command");
			}
		} else {
			writeln(IRCd.serverMessagePrefix + " 451 " + split[0]
					+ " :You have not registered");
		}
	}

	public void sendWelcome() {
		if ((IRCd.isBanned(nick + "!" + ident + "@" + hostmask))
				|| (IRCd.isBanned(nick + "!" + ident + "@" + ipaddress))) {
			writeln("ERROR :Closing Link: [" + ipaddress
					+ "] (You are banned from this server)");
			disconnect();
			synchronized (IRCd.csStdOut) {
				System.out.println("[BukkitIRCd] Cleaning up connection from "
						+ getFullHost() + " (banned)");
			}
			if (isIdented && isNickSet)
				IRCd.writeAll(":" + getFullHost()
						+ " QUIT :You are banned from this server");
			IRCd.removeIRCUser(nick, "Banned", true);
		} else {
			synchronized (IRCd.csStdOut) {
				System.out.println("[BukkitIRCd] " + ipaddress
						+ " registered as " + getFullHost());
			}
			Thread.currentThread().setName(
					"Thread-BukkitIRCd-Connection-" + nick);
			signonTime = System.currentTimeMillis() / 1000L;
			writeln("PING :" + signonTime);
			writeln(IRCd.serverMessagePrefix + " 001 " + nick
					+ " :Welcome to the " + Config.getIrcdServerName()
					+ " IRC network " + getFullHost());
			writeln(IRCd.serverMessagePrefix + " 002 " + nick
					+ " :Your host is " + Config.getIrcdServerHostName()
					+ ", running BukkitIRCdPlugin.ircdVersion "
					+ BukkitIRCdPlugin.ircdVersion);
			writeln(IRCd.serverMessagePrefix + " 003 " + nick
					+ " :This server was created "
					+ Config.getServerCreationDate());
			writeln(IRCd.serverMessagePrefix + " 004 " + nick + " :"
					+ Config.getIrcdServerHostName() + " "
					+ BukkitIRCdPlugin.ircdVersion + " - -");
			writeln(IRCd.serverMessagePrefix
					+ " 005 "
					+ nick
					+ " NICKLEN="
					+ (Config.getIrcdMaxNickLength() + 1)
					+ " CHANNELLEN=50 TOPICLEN=500 PREFIX=(qaohv)~&@%+ CHANTYPES=# CHANMODES=b,k,l,imt CASEMAPPING=ascii NETWORK="
					+ Config.getIrcdServerName()
					+ " :are supported by this server");
			writeln(IRCd.serverMessagePrefix + " 251 " + nick + " :There are "
					+ IRCd.getClientCount() + " users and 0 invisible on "
					+ (IRCd.getServerCount() + 1) + " servers");
			writeln(IRCd.serverMessagePrefix + " 252 " + nick + " "
					+ IRCd.getOperCount() + " :operators online");
			writeln(IRCd.serverMessagePrefix + " 254 " + nick
					+ " 1 :channels formed");
			writeln(IRCd.serverMessagePrefix + " 255 " + nick + " :I have "
					+ IRCd.getClientCount() + " clients and "
					+ IRCd.getServerCount() + " servers.");
			writeln(IRCd.serverMessagePrefix + " 265 " + nick
					+ " :Current local users: " + IRCd.getClientCount()
					+ " Max: " + Config.getIrcdMaxConnections());
			writeln(IRCd.serverMessagePrefix
					+ " 266 "
					+ nick
					+ " :Current global users: "
					+ (IRCd.getClientCount() + IRCd.getRemoteClientCount())
					+ " Max: "
					+ (Config.getIrcdMaxConnections() + IRCd
							.getRemoteMaxConnections()));
			sendMOTD();
			if ((IRCd.isPlugin()) && (BukkitIRCdPlugin.thePlugin != null)) {
				if (IRCd.msgIRCJoin.length() > 0)
					IRCd.broadcastMessage(IRCd.msgIRCJoin
							.replace("{User}", nick)
							.replace("{Suffix}", IRCd.getGroupSuffix(modes))
							.replace("{Prefix}", IRCd.getGroupPrefix(modes)));
				if ((BukkitIRCdPlugin.dynmap != null)
						&& (IRCd.msgIRCJoinDynmap.length() > 0))
					BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC",
							IRCd.msgIRCJoinDynmap.replace("{User}", nick));
			}
			sendChanWelcome(Config.getIrcdChannel());
		}
	}

	public void sendMOTD() {
		writeln(IRCd.serverMessagePrefix + " 375 " + nick + " :"
				+ Config.getIrcdServerHostName() + " Message of the Day");
		for (String motdline : IRCd.MOTD) {
			writeln(IRCd.serverMessagePrefix + " 372 " + nick + " :- "
					+ motdline);
		}
		writeln(IRCd.serverMessagePrefix + " 376 " + nick
				+ " :End of /MOTD command.");
	}

	public void sendWhois(String whoisUser) {
		IRCUser ircuser;
		BukkitPlayer bp;
		if (whoisUser.equalsIgnoreCase(Config.getIrcdServerName())) {
			writeln(IRCd.serverMessagePrefix + " 311 " + nick + " "
					+ Config.getIrcdServerName() + " "
					+ Config.getIrcdServerName() + " "
					+ Config.getIrcdServerHostName() + " * :"
					+ BukkitIRCdPlugin.ircdVersion);
			if (isOper) {
				writeln(IRCd.serverMessagePrefix + " 379 " + nick + " "
						+ Config.getIrcdServerName() + " :is using modes +oS");
				writeln(IRCd.serverMessagePrefix + " 378 " + nick + " "
						+ Config.getIrcdServerName()
						+ " :is connecting from *@"
						+ Config.getIrcdServerHostName() + " 127.0.0.1");
			}
			writeln(IRCd.serverMessagePrefix + " 312 " + nick + " "
					+ Config.getIrcdServerName() + " "
					+ Config.getIrcdServerHostName() + " :"
					+ Config.getIrcdServerDescription());
			writeln(IRCd.serverMessagePrefix + " 313 " + nick + " "
					+ Config.getIrcdServerName() + " :Is a Network Service");
			writeln(IRCd.serverMessagePrefix + " 318 " + nick + " "
					+ Config.getIrcdServerName() + " :End of /WHOIS list.");

		} else if ((ircuser = IRCd.getIRCUser(whoisUser)) != null) {
			synchronized (IRCd.csIrcUsers) {
				String cmodes;
				String modes = ircuser.getModes();
				if (modes.length() > 0)
					cmodes = modes.substring(0, 1);
				else
					cmodes = "";
				writeln(IRCd.serverMessagePrefix + " 311 " + nick + " "
						+ ircuser.nick + " " + ircuser.ident + " "
						+ ircuser.hostmask + " * :" + ircuser.realname);
				if (isOper) {
					if (ircuser.isOper)
						writeln(IRCd.serverMessagePrefix + " 379 " + nick + " "
								+ ircuser.nick + " :is using modes +o");
					writeln(IRCd.serverMessagePrefix + " 378 " + nick + " "
							+ ircuser.nick + " :is connecting from *@"
							+ ircuser.realhost + " " + ircuser.ipaddress);
				}
				if (ircuser.isRegistered)
					writeln(IRCd.serverMessagePrefix + " 307 " + nick + " "
							+ ircuser.nick + " :is a registered nick");
				writeln(IRCd.serverMessagePrefix + " 319 " + nick + " "
						+ ircuser.nick + " :" + cmodes
						+ Config.getIrcdChannel());
				writeln(IRCd.serverMessagePrefix + " 312 " + nick + " "
						+ ircuser.nick + " " + Config.getIrcdServerHostName()
						+ " :" + Config.getIrcdServerDescription());
				if (ircuser.awayMsg.length() > 0)
					writeln(IRCd.serverMessagePrefix + " 301 " + nick + " "
							+ ircuser.nick + " :" + ircuser.awayMsg);
				if (ircuser.isOper)
					writeln(IRCd.serverMessagePrefix + " 313 " + nick + " "
							+ ircuser.nick + " :is an IRC Operator.");
				if (ircuser.customWhois.length() > 0)
					writeln(IRCd.serverMessagePrefix + " 320 " + nick + " "
							+ ircuser.nick + " :" + ircuser.customWhois);
				writeln(IRCd.serverMessagePrefix + " 317 " + nick + " "
						+ ircuser.nick + " " + ircuser.getSecondsIdle() + " "
						+ ircuser.signonTime + " :seconds idle, signon time");
				writeln(IRCd.serverMessagePrefix + " 318 " + nick + " "
						+ ircuser.nick + " :End of /WHOIS list.");
			}
		} else if ((bp = IRCd.getBukkitUserObject(whoisUser)) != null) {
			if ((IRCd.isPlugin()) && (BukkitIRCdPlugin.thePlugin != null)) {
				if ((Config.getIrcdIngameSuffix().length() > 0)
						&& (whoisUser.endsWith(Config.getIrcdIngameSuffix())))
					whoisUser.substring(0, whoisUser.length()
							- Config.getIrcdIngameSuffix().length());
				else {
				}

				String playermodes;
				long playersignon;
				long playeridle;
				String playerident;
				String playernick;
				String playerhost;
				String playerip;
				String playerworld;
				boolean playerisoper;

				String mode = bp.getMode();
				if (mode.length() > 0)
					playermodes = mode.substring(0, 1);
				else
					playermodes = "";
				playersignon = bp.signedOn;
				playeridle = (System.currentTimeMillis() - bp.idleTime) / 1000L;
				playerident = bp.nick;
				playernick = bp.nick + Config.getIrcdIngameSuffix();
				playerhost = bp.host;
				playerip = bp.ip;
				playerworld = bp.world;
				playerisoper = bp.hasPermission("bukkitircd.oper");

				writeln(IRCd.serverMessagePrefix + " 311 " + nick + " "
						+ playernick + " " + playerident + " " + playerhost
						+ " " + " * :Minecraft Player");
				if (isOper)
					writeln(IRCd.serverMessagePrefix + " 378 " + nick + " "
							+ playernick + " :is connecting from *@"
							+ playerhost + " " + playerip);
				writeln(IRCd.serverMessagePrefix + " 319 " + nick + " "
						+ playernick + " :" + playermodes
						+ Config.getIrcdChannel());
				writeln(IRCd.serverMessagePrefix + " 312 " + nick + " "
						+ playernick + " " + Config.getIrcdServerHostName()
						+ " :Bukkit " + IRCd.bukkitversion);
				if (playerisoper)
					writeln(IRCd.serverMessagePrefix + " 313 " + nick + " "
							+ playernick + " :is an IRC Operator.");
				writeln(IRCd.serverMessagePrefix + " 320 " + nick + " "
						+ playernick + " :is currently in " + playerworld);
				writeln(IRCd.serverMessagePrefix + " 317 " + nick + " "
						+ playernick + " " + playeridle + " " + playersignon
						+ " :seconds idle, signon time");
				writeln(IRCd.serverMessagePrefix + " 318 " + nick + " "
						+ playernick + " :End of /WHOIS list.");
			}
		} else {
			writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + whoisUser
					+ " :No such nick/channel");
			writeln(IRCd.serverMessagePrefix + " 318 " + nick + " " + whoisUser
					+ " :End of /WHOIS list.");
		}
	}

	public void sendChanWelcome(String channelName) {
		if (channelName.equalsIgnoreCase(Config.getIrcdConsoleChannel()))
			IRCd.writeOpers(":" + getFullHost() + " JOIN " + channelName);
		else
			IRCd.writeAll(":" + getFullHost() + " JOIN " + channelName);
		sendChanTopic(channelName);
		sendChanModes(channelName);
		sendChanNames(channelName);
	}

	public boolean sendChanTopic(String channelName) {
		String consoleChannelTopic = "BukkitIRCd console channel - prefix commands with ! instead of /";
		if (IRCd.channelTopic.length() > 0) {
			if (channelName.equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
				writeln(IRCd.serverMessagePrefix + " 332 " + nick + " "
						+ channelName + " :" + consoleChannelTopic);
				try {
					writeln(IRCd.serverMessagePrefix
							+ " 333 "
							+ nick
							+ " "
							+ channelName
							+ " "
							+ Config.getIrcdServerName()
							+ " "
							+ (IRCd.dateFormat.parse(
									Config.getServerCreationDate()).getTime() / 1000L));
				} catch (ParseException e) {
					writeln(IRCd.serverMessagePrefix + " 333 " + nick + " "
							+ channelName + " " + Config.getIrcdServerName()
							+ " " + (System.currentTimeMillis() / 1000L));
				}
			} else if (channelName.equalsIgnoreCase(Config.getIrcdChannel())) {
				writeln(IRCd.serverMessagePrefix + " 332 " + nick + " "
						+ channelName + " :" + IRCd.channelTopic);
				writeln(IRCd.serverMessagePrefix + " 333 " + nick + " "
						+ channelName + " " + IRCd.channelTopicSet + " "
						+ IRCd.channelTopicSetDate);
			} else
				return false;
		}
		return true;
	}

	public boolean sendChanModes(String channelName) {
		if (IRCd.channelTopic.length() > 0) {
			if (channelName.equals(Config.getIrcdConsoleChannel()))
				writeln(IRCd.serverMessagePrefix + " 324 " + nick + " "
						+ channelName + " +Ont");
			else
				writeln(IRCd.serverMessagePrefix + " 324 " + nick + " "
						+ channelName + " +nt");
			try {
				writeln(IRCd.serverMessagePrefix
						+ " 329 "
						+ nick
						+ " "
						+ channelName
						+ " "
						+ ((Config.getDateFormat().parse(
								Config.getServerCreationDate()).getTime()) / 1000));
			} catch (ParseException e) {
				writeln(IRCd.serverMessagePrefix + " 329 " + nick + " "
						+ channelName + " "
						+ (System.currentTimeMillis() / 1000L));
			}
		}
		return true;
	}

	public boolean sendChanNames(String channelName) {
		if (channelName.equalsIgnoreCase(Config.getIrcdConsoleChannel()))
			writeln(IRCd.serverMessagePrefix + " 353 = " + nick + " "
					+ channelName + " :~" + Config.getIrcdServerName() + " "
					+ IRCd.getOpers());
		else if (channelName.equalsIgnoreCase(Config.getIrcdChannel()))
			writeln(IRCd.serverMessagePrefix + " 353 = " + nick + " "
					+ channelName + " :~" + Config.getIrcdServerName() + " "
					+ IRCd.getUsers());
		else
			return false;

		writeln(IRCd.serverMessagePrefix + " 366 " + nick + " " + channelName
				+ " :End of /NAMES list.");
		return true;
	}

	public void sendWho(String pattern, boolean opersOnly) {
		boolean addAll = (pattern.equalsIgnoreCase(Config.getIrcdChannel()))
				|| (pattern.length() == 0);
		if (pattern.equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
			opersOnly = true;
			addAll = true;
		}

		String channel = Config.getIrcdChannel();
		List<String> users = new ArrayList<String>();
		synchronized (IRCd.csIrcUsers) {
			for (IRCUser user : IRCd.getIRCUsers()) {
				String onick = user.nick;
				String ohost = user.hostmask;
				String oident = user.ident;
				String away = ((user.awayMsg.length() > 0) ? "G" : "H");
				String oper = (user.isOper ? "*" : "");
				String name = user.realname;

				if ((opersOnly && user.isOper) || (!opersOnly)) {
					if (addAll
							|| IRCd.wildCardMatch(onick, pattern)
							|| IRCd.wildCardMatch(onick + "!" + oident + "@"
									+ ohost, pattern))
						users.add(IRCd.serverMessagePrefix + " 352 " + nick
								+ " " + channel + " " + oident + " " + ohost
								+ " " + Config.getIrcdServerHostName() + " "
								+ onick + " " + away + oper + " :0 " + name);
				}
			}
		}
		if (!opersOnly) {
			synchronized (IRCd.csBukkitPlayers) {
				for (BukkitPlayer bukkitPlayer : IRCd.bukkitPlayers) {
					String onick = bukkitPlayer.nick
							+ Config.getIrcdIngameSuffix();
					String ohost = bukkitPlayer.host;
					String oident = bukkitPlayer.nick;
					String name = bukkitPlayer.nick;
					String away = "H";
					String oper = "";

					if (addAll
							|| IRCd.wildCardMatch(onick, pattern)
							|| IRCd.wildCardMatch(oident, pattern)
							|| IRCd.wildCardMatch(onick + "!" + oident + "@"
									+ ohost, pattern)
							|| IRCd.wildCardMatch(oident + "!" + oident + "@"
									+ ohost, pattern))
						users.add(IRCd.serverMessagePrefix + " 352 " + nick
								+ " " + channel + " " + oident + " " + ohost
								+ " " + Config.getIrcdServerHostName() + " "
								+ onick + " " + away + oper + " :0 " + name);
				}
			}
		}
		for (String line : users)
			writeln(line);
		writeln(IRCd.serverMessagePrefix + " 315 " + nick + " " + pattern
				+ " :End of /WHO list.");
	}

	public boolean isConnected() {
		boolean result;
		result = server.isConnected();
		return result;
	}

	public long getSecondsIdle() {
		return (System.currentTimeMillis() - lastActivity) / 1000L;
	}

	public String getFullHost() {
		return nick + "!" + ident + "@" + hostmask;
	}

	public boolean writeln(String line) {
		if (server.isConnected()) {
			synchronized (csWrite) {
				out.println(line);
				return true;
			}
		} else {
			return false;
		}
	}

	public boolean disconnect() {
		if (server.isConnected()) {
			try {
				server.close();
			} catch (IOException e) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	static class CriticalSection extends Object {
	}

	static public CriticalSection csWrite = new CriticalSection();

}
