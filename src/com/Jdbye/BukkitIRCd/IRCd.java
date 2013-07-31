package com.Jdbye.BukkitIRCd;

// BukkitIRCd by Jdbye and WMCAlliance
// A standalone IRC server plugin for Bukkit

// Last changes
// - Minecraft 1.4.6 compatible
// - Changed messages.yml to use & signs
// - Removed unused includes
// - Check our github

/* TODO: (this list was made by Jdbye, we may or may not do this)
 * HeroChat/Towny compatibility.
 * UnrealIRCd/TS5 links
 */

// Features:
// - Standalone IRC server with ingame chat, easily integrated with website using a IRC widget or applet.
// - Whois for both IRC and ingame players, shows current world. Also works ingame.
// - Nickname suffix for ingame players to differentiate between IRC and normal players.
// - Nick changing on IRC, shows up ingame.
// - Public chat from IRC to game, and game to IRC.
// - Private messaging from game to IRC, IRC to game, and IRC to IRC.
// - Kicking, banning and listing IRC users and setting topic from IRC and ingame.
// - Executing server commands from IRC
// - Ingame users show as separate users on IRC.
// - IRC notices from IRC to game, and IRC to IRC.
// - IRC joins/quits show up ingame, and vice versa.
// - Customizable MOTD read from motd.txt
// - User modes (op, protect, voice, etc.) based on permissions nodes.
// - Color code conversion between IRC<->Game.
// - IRC formatting codes are supported ingame using ^B for bold, ^I for italic, ^U for underline, ^O for normal and ^K for color, and are stripped from ingame chat.
// - InspIRCd linking support

// Commands:
// - /irckick nick (reason) - Kicks someone from IRC
// - /ircban (host/ip/ident/nick) nick/ip/fullhost (reason) - Bans a online user from IRC by their host, IP, nick or ident and offline user by IP or full hostmask
// - /ircunban ip/fullhost - Unbans a user from IRC
// - /irclist - Lists all users currently on IRC
// - /ircwhois nick - Looks up any user currently on IRC
// - /ircmsg nick message - Private messages any user currently on IRC
// - /ircreply message - Replies to the last message received from IRC
// - /irctopic newtopic - Changes the IRC topic
// - /irclink - Attempts to link to the remote server if in linking mode.
// - /rawsend - Sends a raw server command in linking mode. Warning, dangerous! Disabled by default in the config file.

// Permission Nodes (SuperPerms):
// - bukkitircd.kick - Permission for /irckick
// - bukkitircd.ban - Permission for /ircban
// - bukkitircd.unban - Permission for /ircunban
// - bukkitircd.list - Permission for /irclist
// - bukkitircd.whois - Permission for /ircwhois
// - bukkitircd.msg - Permission for /ircmsg
// - bukkitircd.topic - Permission for /irctopic
// - bukkitircd.oper - Gives the player IRC Operator status. Currently doesn't do anything apart from show it in /whois
// - bukkitircd.mode.owner, bukkitircd.mode.protect, bukkitircd.mode.op, bukkitircd.mode.halfop, bukkitircd.mode.voice - Gives the player the corresponding IRC user mode.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.Jdbye.BukkitIRCd.configuration.Config;


public class IRCd implements Runnable {

	// Universal settings
	public static String channelTopic = "Welcome to a Bukkit server!";
	public static String channelTopicSet = Config.getIrcdServerName();
	public static long channelTopicSetDate = System.currentTimeMillis() / 1000L;
	public static Modes mode = Modes.STANDALONE;

	// Custom messages
	public static String msgSendQueryFromIngame = "&r[IRC] [me -> &7{Prefix}{User}{Suffix}&r] {Message}";
	public static String msgLinked = "&e[IRC] Linked to server {LinkName}";
	public static String msgDelinked = "&e[IRC] Split from server {LinkName}";
	public static String msgDelinkedReason = "&e[IRC] Split from server {LinkName} ({Reason})";
	public static String msgIRCJoin = "&e[IRC] {User} joined IRC";
	public static String msgIRCJoinDynmap = "{User} joined IRC";
	public static String msgIRCLeave = "&e[IRC] {User} left IRC";
	public static String msgIRCLeaveReason = "&e[IRC] {User} left IRC ({Reason})";
	public static String msgIRCLeaveDynmap = "{User} left IRC";
	public static String msgIRCLeaveReasonDynmap = "{User} left IRC ({Reason})";
	public static String msgIRCKick = "&e[IRC] {KickedUser} was kicked by {KickedBy}";
	public static String msgIRCKickReason = "&e[IRC] {KickedUser} was kicked by {KickedBy} ({Reason})";
	public static String msgIRCKickDisplay = "Kicked by {KickedBy}.";
	public static String msgIRCKickDisplayReason = "Kicked by {KickedBy} for {Reason}.";
	public static String msgIRCKickDynmap = "{KickedUser} was kicked by {KickedBy}";
	public static String msgIRCKickReasonDynmap = "{KickedUser} was kicked by {KickedBy} ({Reason})";
	public static String msgIRCBan = "&e[IRC] {BannedUser} was banned by {BannedBy}";
	public static String msgIRCBanDynmap = "{BannedUser} was banned by {BannedBy}";
	public static String msgIRCUnban = "&e[IRC] {BannedUser} was unbanned by {BannedBy}";
	public static String msgIRCUnbanDynmap = "{BannedUser} was unbanned by {BannedBy}";
	public static String msgIRCNickChange = "&e[IRC] {OldNick} is now known as {NewNick}&f";
	public static String msgIRCNickChangeDynmap = "{OldNick} is now known as {NewNick}";
	public static String msgIRCAction = "[IRC] * &7{User}&f {Message}";
	public static String msgIRCMessage = "[IRC] <&7{User}&f> {Message}";
	public static String msgIRCNotice = "[IRC] -&7{User}&f- {Message}";
	public static String msgIRCPrivateAction = "[IRC] &aTo you&f: * &7{User}&f {Message}";
	public static String msgIRCPrivateMessage = "[IRC] &aTo you&f: <&7{User}&f> {Message}";
	public static String msgIRCPrivateNotice = "[IRC] &aTo you&f: -&7{User}&f- {Message}";
	public static String msgIRCActionDynmap = "* {User} {Message}";
	public static String msgIRCMessageDynmap = "<{User}> {Message}";
	public static String msgIRCNoticeDynmap = "-{User}- {Message}";
	public static String msgDynmapMessage = "[Dynmap] {User}: {Message}";
	public static String msgDisconnectQuitting = "Left the server";
	public static String msgPlayerList = "^BOnline Players ({Count}):^B {Users}";

	public static final long serverStartTime = System.currentTimeMillis() / 1000L;
	public static long channelTS = serverStartTime,
			consoleChannelTS = serverStartTime;
	String remoteSID = null;
	public static String pre; // Prefix for server linking commands
	long linkLastPingPong;
	long linkLastPingSent;
	public static String serverMessagePrefix;
	private static HashMap<String, IRCUser> uid2ircuser = new HashMap<String, IRCUser>();
	public static HashMap<String, IRCServer> servers = new HashMap<String, IRCServer>();
	public static UidGenerator ugen = new UidGenerator();
	public static String serverUID;
    public static boolean linkcompleted = false;
    private static boolean burstSent = false, capabSent = false;
    private static boolean lastconnected = false;
    private static boolean isIncoming = false;
   // private static boolean broadcastDeathMessages = true;
   // private static boolean colorDeathMessages = false;
   // private static boolean colorSayMessages = false;

    private static boolean isPlugin = false;

    // This object registers itself as a console target and needs to be
    // long lived.
    private static IRCCommandSender commandSender = null;

	//private static Date curDate = new Date();
	public static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss yyyy");
   // private static String serverCreationDate = dateFormat.format(curDate);
	public static long serverCreationDateLong = System.currentTimeMillis() / 1000L;

    //private static int[] ircColors = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
	//		13, 14, 15 };
    //private static String[] gameColors = { "0", "f", "1", "2", "c", "4", "5",
	//		"6", "e", "a", "3", "b", "9", "d", "8", "7" };

	private static List<ClientConnection> clientConnections = new LinkedList<ClientConnection>();
	public static List<BukkitPlayer> bukkitPlayers = new LinkedList<BukkitPlayer>();

	public static List<String> MOTD = new ArrayList<String>();
	public static List<String> consoleFilters = new ArrayList<String>();
	public static List<IrcBan> ircBans = new ArrayList<IrcBan>();

	public static ConfigurationSection groupPrefixes = null;
	public static ConfigurationSection groupSuffixes = null;

	public boolean running = true;

	private long tickCount = System.currentTimeMillis();
	private static ServerSocket listener;
	private static Socket server = null;

	static class CriticalSection extends Object {
	}

	static public CriticalSection csStdOut = new CriticalSection();
	static public CriticalSection csBukkitPlayers = new CriticalSection();
	static public CriticalSection csIrcUsers = new CriticalSection();
	static public CriticalSection csIrcBans = new CriticalSection();
	static public CriticalSection csServer = new CriticalSection();

	public static BufferedReader in;
	public static PrintStream out;
	public static String channelName;
	public static String bukkitversion = "Unknown version";

	public IRCd() {
	}

	@Override
	public void run() {
		while (running) {
			try {
				Class<?> c = Class.forName("org.bukkit.plugin.java.JavaPlugin");
				if (c != null)
					isPlugin = true;
			} catch (ClassNotFoundException e) {
				isPlugin = false;
			}



			try {
				if (Config.getMode().equalsIgnoreCase("inspire") || Config.getMode().equalsIgnoreCase("inspircd")) {
					mode = Modes.INSPIRCD;
				} else {
					mode = Modes.STANDALONE;
				}

				if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
					commandSender = new IRCCommandSender(Bukkit.getServer());
				}

				try {
					serverCreationDateLong = dateFormat.parse(
							Config.getServerCreationDate()).getTime() / 1000L;
				} catch (ParseException e) {
					serverCreationDateLong = 0;
				}

				serverMessagePrefix = ":" + Config.getIrcdServerHostName();


				if (MOTD.size() == 0) {
					MOTD.add("_________        __    __   .__        ___________  _____     _");
					MOTD.add("\\______  \\___ __|  |  |  |  |__| __   |_   _| ___ \\/  __ \\   | |");
					MOTD.add(" |   |_\\  \\  |  |  | _|  | _____/  |_   | | | |_/ /| /  \\/ __| |");
					MOTD.add(" |    __ _/  |  \\  |/ /  |/ /  \\   __\\  | | |    / | |    / _` |");
					MOTD.add(" |   |_/  \\  |  /    <|    <|  ||  |   _| |_| |\\ \\ | \\__/\\ (_| |");
					MOTD.add(" |______  /____/|__|_ \\__|_ \\__||__|   \\___/\\_| \\_| \\____/\\__,_|");
					MOTD.add("        \\/           \\/    \\/");
					MOTD.add("");
					MOTD.add("Welcome to " + Config.getIrcdServerName() + ", running "
							+ BukkitIRCdPlugin.ircdVersion + ".");
					MOTD.add("Enjoy your stay!");
				}

				if (mode == Modes.STANDALONE) {
					Thread.currentThread().setName(
							"Thread-BukkitIRCd-StandaloneIRCd");
					clientConnections.clear();
					try {
						try {
							listener = new ServerSocket(Config.getIrcdPort());
							listener.setSoTimeout(1000);
							listener.setReuseAddress(true);
							BukkitIRCdPlugin.log
									.info("[BukkitIRCd] Listening for client connections on port "
											+ Config.getIrcdPort());
						} catch (IOException e) {
							BukkitIRCdPlugin.log
									.severe("Failed to listen on port " + Config.getIrcdPort()
											+ ": " + e);
						}
						while (running) {
							if ((clientConnections.size() < Config.getIrcdMaxConnections())
									|| (Config.getIrcdMaxConnections() == 0)) {
								ClientConnection connection;
								try {
									server = listener.accept();
									if (server.isConnected()) {
										connection = new ClientConnection(
												server);
										connection.lastPingResponse = System
												.currentTimeMillis();
										clientConnections.add(connection);
										Thread t = new Thread(connection);
										t.start();
									}
								} catch (SocketTimeoutException e) {
								}
								if (tickCount + (Config.getIrcdPingInterval() * 1000) < System
										.currentTimeMillis()) {
									tickCount = System.currentTimeMillis();
									writeAll("PING :" + tickCount);
								}
							}
							try {
								Thread.currentThread();
								Thread.sleep(1);
							} catch (InterruptedException e) {
							}
						}
					} catch (IOException e) {
						synchronized (csStdOut) {
							System.out
									.println("[BukkitIRCd] IOException on socket listen: "
											+ e.toString()
											+ ". Error Code IRCd281.");
						}
					}
				} else if (mode == Modes.INSPIRCD) {
					Thread.currentThread()
							.setName("Thread-BukkitIRCd-InspIRCd");
					String line = null;
					serverUID = ugen.generateUID(Config.getLinkServerID());
					pre = ":" + Config.getLinkServerID() + " ";
					lastconnected = false;
					isIncoming = false;
					remoteSID = null;

					try {
						listener = new ServerSocket(Config.getLinkLocalPort());
						listener.setSoTimeout(1000);
						listener.setReuseAddress(true);
						BukkitIRCdPlugin.log
								.info("[BukkitIRCd] Listening for server connections on port "
										+ Config.getLinkLocalPort());
					} catch (IOException e) {
						BukkitIRCdPlugin.log.severe("Failed to listen on port "
								+ Config.getLinkLocalPort() + ": " + e);
					}

					try {
						server = listener.accept();
					} catch (IOException e) {
					}
					if ((server != null) && server.isConnected()
							&& (!server.isClosed())) {
						InetAddress addr = server.getInetAddress();
						BukkitIRCdPlugin.log
								.info("[BukkitIRCd] Got server connection from "
										+ addr.getHostAddress());
						isIncoming = true;
					} else if (Config.isLinkAutoconnect()) {
						connect();
					}

					while (running) {
						try {
							if ((server != null) && server.isConnected()
									&& (!server.isClosed()) && (!lastconnected)) {
								in = new BufferedReader(new InputStreamReader(
										server.getInputStream()));
								out = new PrintStream(server.getOutputStream());
								line = in.readLine();
								if (line == null)
									throw new IOException(
											"Lost connection to server before sending handshake!");
								String[] split = line.split(" ");
								if (Config.isDebugModeEnabled())
									BukkitIRCdPlugin.log
											.info("[BukkitIRCd] "
													+ ChatColor.YELLOW
													+ "[->] " + line);

								if (!isIncoming) {
									sendLinkCAPAB();
									sendLinkBurst();
								}

								while ((!split[0].equalsIgnoreCase("SERVER"))
										&& (server != null)
										&& (!server.isClosed())
										&& server.isConnected() && running) {
									if (!running)
										break;

									if (line.startsWith("CAPAB START")) {
										sendLinkCAPAB();
									}

									if (split[0].equalsIgnoreCase("ERROR")) {
										// ERROR :Invalid password.
										if (split[1].startsWith(":"))
											split[1] = split[1].substring(1);
										try {
											server.close();
										} catch (IOException e) {
										}
										throw new IOException(
												"Remote host rejected connection, probably configured wrong: "
														+ join(split, " ", 1));
									} else {
										line = in.readLine();
										if (line != null) {
											split = line.split(" ");
											if (Config.isDebugModeEnabled())
												BukkitIRCdPlugin.log
														.info("[BukkitIRCd]"
																+ ChatColor.YELLOW
																+ " [->] "
																+ line);
										}
									}
								}
								if (split[0].equalsIgnoreCase("SERVER")) {
									// SERVER test.tempcraft.net password 0 280
									// :TempCraft Testing Server
									if ((!split[2].equals(Config.getLinkReceivePassword()))
											|| (!split[1].equals(Config.getLinkName()))) {
										if (!split[2].equals(Config.getLinkReceivePassword()))
											println("ERROR :Invalid password.");
										else if (!split[1].equals(Config.getLinkName()))
											println("ERROR :No configuration for hostname "
													+ split[1]);
										server.close();

										if (!split[1].equals(Config.getLinkName()))
											throw new IOException(
													"Rejected connection from remote host: Invalid link name.");
										else
											throw new IOException(
													"Rejected connection from remote host: Invalid password.");
									}
									remoteSID = split[4];
								}

								linkLastPingPong = System.currentTimeMillis();
								linkLastPingSent = System.currentTimeMillis();

								if ((IRCd.isPlugin)
										&& (BukkitIRCdPlugin.thePlugin != null)) {
									if (msgLinked.length() > 0)
										IRCd.broadcastMessage(
														msgLinked.replace(
																"{LinkName}",
                                                                Config.getLinkName()));
								}
								server.setSoTimeout(500);
								lastconnected = true;
								linkcompleted = true;
							}

							while (running && (server != null)
									&& server.isConnected()
									&& (!server.isClosed())) {
								try {
									if (linkLastPingPong
											+ (Config.getLinkTimeout() * 1000) < System
												.currentTimeMillis()) {
										// Link ping timeout, disconnect and
										// notify remote server
										println("ERROR :Ping timeout");
										server.close();
									} else {
										if (linkLastPingSent
												+ (Config.getLinkPingInterval() * 1000) < System
													.currentTimeMillis()) {
											println(pre + "PING " + Config.getLinkServerID() + " "
													+ remoteSID);
											linkLastPingSent = System
													.currentTimeMillis();
										}
										line = in.readLine();

										if ((line != null)
												&& (line.trim().length() > 0)) {
											if (line.startsWith("ERROR ")) {
												// ERROR :Invalid password.
												if (Config.isDebugModeEnabled())
													BukkitIRCdPlugin.log
															.info("[BukkitIRCd]"
																	+ ChatColor.YELLOW
																	+ "[->] "
																	+ line);
												String[] split = line
														.split(" ");
												if (split[1].startsWith(":"))
													split[1] = split[1]
															.substring(1);
												try {
													server.close();
												} catch (IOException e) {
												}
												throw new IOException(
														"Remote host rejected connection, probably configured wrong: "
																+ join(split,
																		" ", 1));
											} else
												parseLinkCommand(line);
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
							try {
								Thread.currentThread();
								Thread.sleep(1);
							} catch (InterruptedException e) {
							}
						} catch (IOException e) {
							synchronized (csStdOut) {
								BukkitIRCdPlugin.log
										.warning("[BukkitIRCd] Server link failed: "
												+ e);
							}
						}

						// We exited the while loop so assume the connection was
						// lost.
						if (lastconnected) {
							BukkitIRCdPlugin.log
									.info("[BukkitIRCd] Lost connection to "
											+ Config.getLinkRemoteHost() + ":" + Config.getLinkRemoteHost());
							if ((IRCd.isPlugin)
									&& (BukkitIRCdPlugin.thePlugin != null)
									&& linkcompleted) {
								if (msgDelinked.length() > 0)
									IRCd.broadcastMessage(
													msgDelinked.replace(
															"{LinkName}",
															Config.getLinkName()));
							}
							lastconnected = false;
						}

						if ((server != null) && server.isConnected())
							try {
								server.close();
							} catch (IOException e) {
							}
						linkcompleted = false;
						capabSent = false;
						burstSent = false;
						uid2ircuser.clear();
						servers.clear();
						remoteSID = null;
						if (running) {
							if (Config.isLinkAutoconnect()) {
								BukkitIRCdPlugin.log
										.info("[BukkitIRCd] Waiting "
												+ Config.getLinkDelay()
												+ " seconds before retrying...");
								long endTime = System.currentTimeMillis()
										+ (Config.getLinkDelay() * 1000);
								while (System.currentTimeMillis() < endTime) {
									if ((!running) || isConnected())
										break;
									Thread.currentThread();
									Thread.sleep(10);
									try {
										server = listener.accept();
										if ((server != null)
												&& server.isConnected()
												&& (!server.isClosed())) {
											InetAddress addr = server
													.getInetAddress();
											BukkitIRCdPlugin.log
													.info("[BukkitIRCd] Got server connection from "
															+ addr.getHostAddress());
											isIncoming = true;
											break;
										}
									} catch (IOException e) {
									}
								}
								if ((server == null) || (!server.isConnected())
										|| (server.isClosed())) {
									connect();
								}
							} else {
								try {
									server = listener.accept();
									if ((server != null)
											&& server.isConnected()
											&& (!server.isClosed())) {
										InetAddress addr = server
												.getInetAddress();
										BukkitIRCdPlugin.log
												.info("[BukkitIRCd] Got server connection from "
														+ addr.getHostAddress());
										isIncoming = true;
									}
								} catch (IOException e) {
								}
							}
							Thread.currentThread();
							Thread.sleep(1);
						}
					}
				}
			} catch (InterruptedException e) {
				BukkitIRCdPlugin.log.info("[BukkitIRCd] Thread "
						+ Thread.currentThread().getName() + " interrupted.");
				if (running) {
					disconnectAll("Thread interrupted.");
					running = false;
				}
			} catch (Exception e) {
				BukkitIRCdPlugin.log
						.severe("[BukkitIRCd] Unexpected exception in "
								+ Thread.currentThread().getName() + ": "
								+ e.toString());
				BukkitIRCdPlugin.log.severe("[BukkitIRCd] Error code IRCd473.");
				e.printStackTrace();
			}
		}
		BukkitIRCdPlugin.ircd = null;
		if (running)
			BukkitIRCdPlugin.log
					.warning("[BukkitIRCd] Thread quit unexpectedly. If there are any errors above, please notify WizardCM or Mu5tank05 about them.");
		running = false;
	}

	public static boolean isConnected() {
		return ((server != null) && server.isConnected() && (!server.isClosed()));
	}

	// Connect to InspIRCd link
	public static boolean connect() {
		if (mode == Modes.INSPIRCD) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Attempting connection to "
					+ Config.getLinkRemoteHost() + ":" + Config.getLinkRemoteHost());
			try {
				server = new Socket(Config.getLinkRemoteHost(), Config.getLinkRemotePort());
				if ((server != null) && server.isConnected()) {
					BukkitIRCdPlugin.log.info("[BukkitIRCd] Connected to "
							+ Config.getLinkRemoteHost() + ":" + Config.getLinkRemotePort());
					isIncoming = false;
					return true;
				} else
					BukkitIRCdPlugin.log
							.info("[BukkitIRCd] Failed connection to "
									+ Config.getLinkRemoteHost() + ":" + Config.getLinkRemotePort());
			} catch (IOException e) {
				BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed connection to "
						+ Config.getLinkRemoteHost() + ":" + Config.getLinkRemotePort() + " (" + e + ")");
			}
		}
		return false;
	}

	// IRC servers are required to send a list of capabilities to the server
	// they're linking to
	public static boolean sendLinkCAPAB() {
		if (capabSent)
			return false;
		println("CAPAB START 1201");
		println("CAPAB CAPABILITIES :NICKMAX="
				+ (Config.getIrcdMaxNickLength() + 1)
				+ " CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
		// println("CAPAB CHANMODES :admin=&a ban=b founder=~q halfop=%h op=@o operonly=O voice=+ v");
		// // Don't send this line, the server will complain that we don't
		// support various modes and refuse to link
		// println("CAPAB USERMODES :bot=B oper=o u_registered=r"); // Don't
		// send this line, the server will complain that we don't support
		// various modes and refuse to link
		println("CAPAB END");
		println("SERVER " + Config.getIrcdServerHostName() + " " + Config.getLinkConnectPassword() + " 0 "
				+ Config.getLinkServerID() + " :" + Config.getIrcdServerDescription());
		capabSent = true;
		return true;
	}

	public static boolean sendLinkBurst() {
		if (burstSent)
			return false;
		println(pre + "BURST " + (System.currentTimeMillis() / 1000L));
		println(pre + "VERSION :" + BukkitIRCdPlugin.ircdVersion);

		println(pre + "UID " + serverUID + " " + serverStartTime + " "
				+ Config.getIrcdServerName() + " " + Config.getIrcdServerHostName() + " " + Config.getIrcdServerHostName()
				+ " " + Config.getIrcdServerName() + " 127.0.0.1 " + serverStartTime
				+ " +Bro :" + BukkitIRCdPlugin.ircdVersion);
		println(":" + serverUID + " OPERTYPE Network_Service");

		for (BukkitPlayer bp : bukkitPlayers) {
			String UID = ugen.generateUID(Config.getLinkServerID());
			bp.setUID(UID);
			if (bp.hasPermission("bukkitircd.oper")) {
				println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " "
						+ bp.nick + Config.getIrcdIngameSuffix() + " " + bp.realhost + " "
						+ bp.host + " " + bp.nick + " " + bp.ip + " "
						+ bp.signedOn + " +or :Minecraft Player");
				println(":" + UID + " OPERTYPE IRC_Operator");
			} else
				println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " "
						+ bp.nick + Config.getIrcdIngameSuffix() + " " + bp.realhost + " "
						+ bp.host + " " + bp.nick + " " + bp.ip + " "
						+ bp.signedOn + " +r :Minecraft Player");

			String world = bp.getWorld();
			if (world != null)
				println(pre + "METADATA " + UID + " swhois :is currently in "
						+ world);
			else
				println(pre + "METADATA " + UID
						+ " swhois :is currently in an unknown world");
		}

		println(pre + "FJOIN " + Config.getIrcdConsoleChannel() + " " + consoleChannelTS
				+ " +nt :qaohv," + serverUID);
		println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS + " +nt :qaohv," + serverUID);

		int avail = 0;
		StringBuilder sb = null;
		for (BukkitPlayer bp : bukkitPlayers) {

			final String nextPart = bp.getTextMode() + "," + bp.getUID();

			if (nextPart.length() > avail) {
				//flush
				if (sb != null) {
					println(sb.toString());
				}

				sb = new StringBuilder(400);
				sb.append(pre).append("FJOIN ").append(Config.getIrcdChannel()).append(' ').append(channelTS) .append(" +nt :").append(nextPart);
				avail = 409 - sb.length();
			} else {
				sb.append(' ').append(nextPart);
				avail -= nextPart.length();
			}
		}

		//flush
		if (sb != null) {
			println(sb.toString());
		}

		println(pre + "ENDBURST");
		burstSent = true;
		return true;
	}

	public static int getClientCount() {
		if (mode == Modes.STANDALONE)
			return clientConnections.size() + bukkitPlayers.size();
		else
			return bukkitPlayers.size();
	}

	public static int getOperCount() {
		int count = 0;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					if (processor.isOper) {
						count++;
					}
				}
			}
		}
		return count;
	}

	public static int getRemoteClientCount() {
		return uid2ircuser.size();
	}

	public static int getRemoteMaxConnections() {
		return 0;
	}

	public static int getServerCount() {
		if (mode == Modes.STANDALONE)
			return 0;
		else
			return 1 + servers.size();
	}

	public static IRCUser getIRCUser(String nick) {
		synchronized (csIrcUsers) {
			int i = 0;
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor != null)
							&& (processor.nick.equalsIgnoreCase(nick))) {
						return new IRCUser(processor.nick, processor.realname,
								processor.ident, processor.hostmask,
								processor.ipaddress, processor.modes,
								processor.customWhois, processor.isRegistered,
								processor.isOper, processor.awayMsg,
								processor.signonTime, processor.lastActivity, "");
					}
					i++;
				}
			} else if (mode == Modes.INSPIRCD) {
				IRCUser iuser;
				Iterator<?> iter = uid2ircuser.entrySet().iterator();
				while (iter.hasNext()) {
					@SuppressWarnings("unchecked")
					Map.Entry<String, IRCUser> entry = (Entry<String, IRCUser>) iter
							.next();
					iuser = entry.getValue();
					if (iuser.nick.equalsIgnoreCase(nick))
						return iuser;
				}
			}
		}
		return null;
	}

	public static IRCUser[] getIRCUsers() {
		List<IRCUser> users = new ArrayList<IRCUser>();
		Object[] ircUsers = null;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					IRCUser iu = new IRCUser(processor.nick,
							processor.realname, processor.ident,
							processor.hostmask, processor.ipaddress,
							processor.modes, processor.customWhois,
							processor.isRegistered, processor.isOper,
							processor.awayMsg, processor.signonTime,
							processor.lastActivity, "");
					iu.joined = (processor.isIdented && processor.isNickSet);
					users.add(iu);
				}
				ircUsers = users.toArray();
			} else if (mode == Modes.INSPIRCD) {
				ircUsers = uid2ircuser.values().toArray();
			}
		}
		if ((ircUsers != null) && (ircUsers instanceof IRCUser[]))
			return (IRCUser[]) ircUsers;
		else
			return new IRCUser[0];
	}

	public static String getUsers() {
		String users = "";
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					String nick;
					if (processor.modes.length() > 0)
						nick = processor.modes.substring(0, 1) + processor.nick;
					else
						nick = processor.nick;
					if (users.length() == 0) {
						users = nick;
					} else {
						users = users + " " + nick;
					}
				}
			} else if (mode == Modes.INSPIRCD) {
				for (IRCUser user : (IRCUser[]) uid2ircuser.values().toArray()) {
					String nick;
					String modes = user.getModes();
					if (modes.length() > 0)
						nick = modes.substring(0, 1) + user.nick;
					else
						nick = user.nick;
					if (users.length() == 0) {
						users = nick;
					} else {
						users = users + " " + nick;
					}
				}
			}
		}
		synchronized (csBukkitPlayers) {
			int i = 0;
			while (i < bukkitPlayers.size()) {
				BukkitPlayer bukkitPlayer = bukkitPlayers.get(i);
				String nick = bukkitPlayer.nick;
				String modes = bukkitPlayer.getMode();
				String nick2;
				if (modes.length() > 0)
					nick2 = modes.substring(0, 1) + nick + Config.getIrcdIngameSuffix();
				else
					nick2 = nick + Config.getIrcdIngameSuffix();
				if (users.length() == 0) {
					users = nick2;
				} else {
					users = users + " " + nick2;
				}
				i++;
			}
		}
		return users;
	}

	public static String getOpers() {
		String users = "";
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					if (processor.isOper) {
						String nick;
						if (processor.modes.length() > 0)
							nick = processor.modes.substring(0, 1)
									+ processor.nick;
						else
							nick = processor.nick;
						if (users.length() == 0) {
							users = nick;
						} else {
							users = users + " " + nick;
						}
					}
				}
			} else if (mode == Modes.INSPIRCD) {
				for (IRCUser user : (IRCUser[]) uid2ircuser.values().toArray()) {
					if (user.isOper) {
						String nick;
						String modes = user.getModes();
						if (modes.length() > 0)
							nick = modes.substring(0, 1) + user.nick;
						else
							nick = user.nick;
						if (users.length() == 0) {
							users = nick;
						} else {
							users = users + " " + nick;
						}
					}
				}
			}
		}
		return users;
	}

	public static String[] getIRCNicks() {
		List<String> users = new ArrayList<String>();
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					if (processor.isIdented && processor.isNickSet)
						users.add(processor.nick);
				}
			} else if (mode == Modes.INSPIRCD) {
				for (Object user : uid2ircuser.values().toArray()) {
					if (((IRCUser) user).joined)
						users.add(((IRCUser) user).nick);
				}
			}
		}
		String userArray[] = new String[0];
		userArray = users.toArray(userArray);
		Arrays.sort(userArray);
		return userArray;
	}

	// This doesn't seem to work - find out why
	public static Collection<String> getIRCWhois(final IRCUser ircuser, final boolean isOper) {
		if (ircuser == null)
			return null;
		ArrayList<String> whois = new ArrayList<String>(10);
		synchronized (csIrcUsers) {
			String idletime = TimeUtils.millisToLongDHMS(ircuser
					.getSecondsIdle() * 1000);
			whois.add(ChatColor.DARK_GREEN + "Nickname: " + ChatColor.GRAY
					+ ircuser.nick + ChatColor.WHITE);
			whois.add(ChatColor.DARK_GREEN + "Ident: " + ChatColor.GRAY
					+ ircuser.ident + ChatColor.WHITE);
			whois.add(ChatColor.DARK_GREEN + "Hostname: " + ChatColor.GRAY
					+ ircuser.hostmask + ChatColor.WHITE);
			if (isOper && !ircuser.hostmask.equalsIgnoreCase(ircuser.realhost)) {
				whois.add(ChatColor.DARK_GREEN + "Real Hostname: " + ChatColor.GRAY
						+ ircuser.realhost + ChatColor.WHITE);
			}
			whois.add(ChatColor.DARK_GREEN + "Realname: " + ChatColor.GRAY
					+ ircuser.realname + ChatColor.WHITE);
			whois.add(ChatColor.DARK_GREEN + "Is registered: "
					+ ChatColor.GRAY + (ircuser.isRegistered ? "Yes" : "No")
					+ ChatColor.WHITE);
			if (!ircuser.accountname.isEmpty()) {
				whois.add(ChatColor.DARK_GREEN + "Account name: "
					+ ChatColor.GRAY + ircuser.accountname
					+ ChatColor.WHITE);
			}
			whois.add(ChatColor.DARK_GREEN + "Is operator: " + ChatColor.GRAY
					+ (ircuser.isOper ? "Yes" : "No") + ChatColor.WHITE);
			whois.add(ChatColor.DARK_GREEN + "Away: " + ChatColor.GRAY
					+ ((!ircuser.awayMsg.equals("")) ? ircuser.awayMsg : "No")
					+ ChatColor.WHITE);
			whois.add(ChatColor.DARK_GREEN + "Idle " + ChatColor.GRAY
					+ idletime + ChatColor.WHITE);
			whois.add(ChatColor.DARK_GREEN + "Signed on at " + ChatColor.GRAY
					+ dateFormat.format(ircuser.signonTime * 1000)
					+ ChatColor.WHITE);
		}
		return whois;
	}

	public static boolean removeIRCUser(String nick) {
		return removeIRCUser(nick, null, false);
	}

	public static boolean removeIRCUser(String nick, String reason,
			boolean IRCToGame) {
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					ClientConnection processor = iter.next();
					if (processor.nick.equalsIgnoreCase(nick)) {
						if (processor.isIdented && processor.isNickSet) {
							if ((IRCd.isPlugin)
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								BukkitIRCdPlugin.thePlugin
										.removeLastReceivedFrom(processor.nick);
								if (reason != null) {
									if (msgIRCLeave.length() > 0)
										IRCd.broadcastMessage(
														msgIRCLeaveReason
																.replace(
																		"{User}",
																		processor.nick)
																.replace(
																		"{Suffix}",
																		IRCd.getGroupSuffix(processor.modes))
																.replace(
																		"{Prefix}",
																		IRCd.getGroupPrefix(processor.modes))
																.replace(
																		"{Reason}",
																		convertColors(
																				reason,
																				IRCToGame)));
									if ((BukkitIRCdPlugin.dynmap != null)
											&& (msgIRCLeaveDynmap.length() > 0))
										BukkitIRCdPlugin.dynmap
												.sendBroadcastToWeb(
														"IRC",
														msgIRCLeaveReasonDynmap
																.replace(
																		"{User}",
																		processor.nick)
																.replace(
																		"{Reason}",
																		stripIRCFormatting(reason)));
								} else {
									if (msgIRCLeave.length() > 0)
										IRCd.broadcastMessage(
														msgIRCLeave
																.replace(
																		"{User}",
																		processor.nick)
																.replace(
																		"{Suffix}",
																		IRCd.getGroupSuffix(processor.modes))
																.replace(
																		"{Prefix}",
																		IRCd.getGroupPrefix(processor.modes)));
									if ((BukkitIRCdPlugin.dynmap != null)
											&& (msgIRCLeaveDynmap.length() > 0))
										BukkitIRCdPlugin.dynmap
												.sendBroadcastToWeb(
														"IRC",
														msgIRCLeaveDynmap
																.replace(
																		"{User}",
																		processor.nick));
								}
							}
						}
						iter.remove();
						if (processor.isConnected())
							processor.disconnect();
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean removeIRCUsers(String reason) {
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					ClientConnection processor = iter.next();
					if (processor.isIdented && processor.isNickSet) {
						if ((IRCd.isPlugin)
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							BukkitIRCdPlugin.thePlugin
									.removeLastReceivedFrom(processor.nick);
							if (msgIRCLeave.length() > 0 && reason != null)
								IRCd.broadcastMessage(
												msgIRCLeaveReason
														.replace("{User}",
																processor.nick)
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(processor.modes))
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(processor.modes))
														.replace(
																"{Reason}",
																reason));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCLeaveDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
										"IRC", msgIRCLeaveDynmap.replace(
												"%USER%", processor.nick));
						}
					}
					iter.remove();
					if (processor.isConnected())
						processor.disconnect();
					return true;
				}
			}
		}
		return false;
	}

	public static boolean removeIRCUsersBySID(String serverID) {
		if (mode != Modes.INSPIRCD)
			return false;
		IRCServer is = servers.get(serverID);
		if (is != null) {
			if (Config.isDebugModeEnabled())
				BukkitIRCdPlugin.log.info("[BukkitIRCd] Server " + serverID
						+ " (" + is.host + ") delinked");
			Iterator<Entry<String, IRCUser>> iter = uid2ircuser.entrySet()
					.iterator();
			while (iter.hasNext()) {
				Map.Entry<String, IRCUser> entry = iter.next();
				String curUID = entry.getKey();
				IRCUser curUser = entry.getValue();
				if (curUID.startsWith(serverID)) {
					if (curUser.joined) {

						if (msgIRCLeaveReason.length() > 0)
							IRCd.broadcastMessage(
											msgIRCLeaveReason
													.replace("{User}",
															curUser.nick)
													.replace(
															"{Prefix}",
															IRCd.getGroupPrefix(curUser
																	.getTextModes()))
													.replace(
															"{Suffix}",
															IRCd.getGroupSuffix(curUser
																	.getTextModes()))
													.replace("{Reason}",
															is.host + " split"));
						if ((BukkitIRCdPlugin.dynmap != null)
								&& (msgIRCLeaveReasonDynmap.length() > 0))
							BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
									"IRC",
									msgIRCLeaveReasonDynmap.replace("{User}",
											curUser.nick).replace("{Reason}",
											is.host + " split"));
					}
					iter.remove();
				}
			}
			servers.remove(serverID);
			for (String curSID : is.leaves)
				removeIRCUsersBySID(curSID);
			return true;
		}
		return false;
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy,
			String kickBannedByHost, boolean isIngame) {
		return kickBanIRCUser(ircuser, kickBannedBy, kickBannedByHost, null,
				isIngame, Config.getIrcdBantype());
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy,
			String kickBannedByHost, boolean isIngame, String banType) {
		return kickBanIRCUser(ircuser, kickBannedBy, kickBannedByHost, null,
				isIngame, banType);
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy,
			String kickBannedByHost, String reason, boolean isIngame) {
		return kickBanIRCUser(ircuser, kickBannedBy, kickBannedByHost, null,
				isIngame, Config.getIrcdBantype());
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy,
			String kickBannedByHost, String reason, boolean isIngame,
			String banType) {
		if (banType == null)
			banType = Config.getIrcdBantype();
		String split[] = kickBannedByHost.split("!")[1].split("@");
		String kickedByIdent = split[0];
		String kickedByHostname = split[1];
		return (banIRCUser(ircuser, kickBannedBy, kickBannedByHost, isIngame,
				banType) && kickIRCUser(ircuser, kickBannedBy, kickedByIdent,
				kickedByHostname, reason, isIngame));
	}

	public static boolean kickIRCUser(IRCUser ircuser, String kickedByNick,
			String kickedByIdent, String kickedByHost, boolean isIngame) {
		return kickIRCUser(ircuser, kickedByNick, kickedByIdent, kickedByHost,
				null, isIngame);
	}

	@SuppressWarnings("unchecked")
	public static boolean kickIRCUser(IRCUser ircuser, String kickedByNick,
			String kickedByIdent, String kickedByHost, String reason,
			boolean isIngame) {
		if (ircuser == null)
			return false;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					processor = iter.next();
					if (processor.nick.equalsIgnoreCase(ircuser.nick)) {
						if (processor.isIdented && processor.isNickSet) {
							if ((isPlugin)
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								if (reason != null) {
									if (msgIRCKickReason.length() > 0)
										IRCd.broadcastMessage(
														msgIRCKickReason
																.replace(
																		"{KickedUser}",
																		processor.nick)
																.replace(
																		"{KickedBy}",
																		kickedByNick)
																.replace(
																		"{Reason}",
																		convertColors(
																				reason,
																				true))
																.replace(
																		"{KickedPrefix}",
																		IRCd.getGroupPrefix(processor.modes))
																.replace(
																		"{KickedSuffix}",
																		IRCd.getGroupSuffix(processor.modes))
																.replace(
																		"{KickerPrefix}",
																		IRCd.getGroupPrefix(IRCd
																				.getIRCUser(
																						kickedByNick)
																				.getTextModes()))
																.replace(
																		"{KickerSuffix}",
																		IRCd.getGroupSuffix(IRCd
																				.getIRCUser(
																						kickedByNick)
																				.getTextModes())));
									if ((BukkitIRCdPlugin.dynmap != null)
											&& (msgIRCKickReasonDynmap.length() > 0))
										BukkitIRCdPlugin.dynmap
												.sendBroadcastToWeb(
														"IRC",
														msgIRCKickReasonDynmap
																.replace(
																		"{KickedUser}",
																		processor.nick)
																.replace(
																		"{KickedBy}",
																		kickedByNick)
																.replace(
																		"{Reason}",
																		stripIRCFormatting(reason)));
								} else {
									if (msgIRCKick.length() > 0)
										IRCd.broadcastMessage(
														msgIRCKick
																.replace(
																		"{KickedUser}",
																		processor.nick)
																.replace(
																		"{KickedBy}",
																		kickedByNick)
																.replace(
																		"{KickedPrefix}",
																		IRCd.getGroupPrefix(processor.modes))
																.replace(
																		"{KickedSuffix}",
																		IRCd.getGroupSuffix(processor.modes))
																.replace(
																		"{KickerPrefix}",
																		IRCd.getGroupPrefix(IRCd
																				.getIRCUser(
																						kickedByNick)
																				.getTextModes()))
																.replace(
																		"{KickerSuffix}",
																		IRCd.getGroupSuffix(IRCd
																				.getIRCUser(
																						kickedByNick)
																				.getTextModes())));
									if ((BukkitIRCdPlugin.dynmap != null)
											&& (msgIRCKickDynmap.length() > 0))
										BukkitIRCdPlugin.dynmap
												.sendBroadcastToWeb(
														"IRC",
														msgIRCKickDynmap
																.replace(
																		"{KickedUser}",
																		processor.nick)
																.replace(
																		"{KickedBy}",
																		kickedByNick));
								}
							}
						}
						if (isIngame) {
							kickedByNick += Config.getIrcdIngameSuffix();
							if (reason != null)
								reason = convertColors(reason, false);
						}
						if (reason != null) {
							writeAll(":" + processor.getFullHost()
									+ " QUIT :Kicked by " + kickedByNick + ": "
									+ reason);
							processor.writeln(":" + kickedByNick + "!"
									+ kickedByIdent + "@" + kickedByHost
									+ " KILL " + processor.nick + " :"
									+ kickedByHost + "!" + kickedByNick + " ("
									+ reason + ")");
							processor.writeln("ERROR :Closing Link: "
									+ processor.nick + "[" + processor.hostmask
									+ "] " + kickedByNick + " (Kicked by "
									+ kickedByNick + " (" + reason + "))");
						} else {
							writeAll(":" + processor.getFullHost()
									+ " QUIT :Kicked by " + kickedByNick);
							processor.writeln(":" + kickedByNick + "!"
									+ kickedByIdent + "@" + kickedByHost
									+ " KILL " + processor.nick + " :"
									+ kickedByHost + "!" + kickedByNick);
							processor.writeln("ERROR :Closing Link: "
									+ processor.nick + "[" + processor.hostmask
									+ "] " + kickedByNick + " (Kicked by "
									+ kickedByNick + ")");
						}
						processor.disconnect();
						iter.remove();
						return true;
					}
				}
			} else if (mode == Modes.INSPIRCD) {
				IRCUser iuser = null;
				Iterator<?> iter = uid2ircuser.entrySet().iterator();
				String uid = null;
				while (iter.hasNext()) {
					Map.Entry<String, IRCUser> entry = (Entry<String, IRCUser>) iter
							.next();
					iuser = entry.getValue();
					if (iuser.nick.equalsIgnoreCase(ircuser.nick)) {
						uid = entry.getKey();
						break;
					}
				}
				if (uid != null) {
					// :280AAAAAA KICK #tempcraft.survival 280AAAAAB :reason
					BukkitPlayer bukkitUser;
					String sourceUID;
					if ((bukkitUser = getBukkitUserObject(kickedByNick)) != null)
						sourceUID = bukkitUser.getUID();
					else
						sourceUID = serverUID;

					boolean returnVal = false;
					if (iuser.consoleJoined) {
						if (reason != null) {
							println(":" + sourceUID + " KICK "
									+ Config.getIrcdConsoleChannel() + " " + uid + " :"
									+ reason);
						} else {
							println(":" + sourceUID + " KICK "
									+ Config.getIrcdConsoleChannel() + " " + uid + " :"
									+ kickedByNick);
						}
						returnVal = true;
						iuser.consoleJoined = false;
					}
					if (iuser.joined) {
						if (reason != null) {
							println(":" + sourceUID + " KICK " + Config.getIrcdChannel()
									+ " " + uid + " :" + reason);
							if (msgIRCKickReason.length() > 0)
								IRCd.broadcastMessage(
												msgIRCKickReason
														.replace(
																"{KickedUser}",
																iuser.nick)
														.replace("{KickedBy}",
																kickedByNick)
														.replace(
																"{Reason}",
																convertColors(
																		reason,
																		true))
														.replace(
																"{KickedPrefix}",
																IRCd.getGroupPrefix(iuser
																		.getTextModes()))
														.replace(
																"{KickedSuffix}",
																IRCd.getGroupSuffix(iuser
																		.getTextModes()))
														.replace(
																"{KickerPrefix}",
																IRCd.getGroupPrefix(IRCd
																		.getIRCUser(
																				kickedByNick)
																		.getTextModes()))
														.replace(
																"{KickerSuffix}",
																IRCd.getGroupSuffix(IRCd
																		.getIRCUser(
																				kickedByNick)
																		.getTextModes())));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCKickReasonDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap
										.sendBroadcastToWeb(
												"IRC",
												msgIRCKickReasonDynmap
														.replace(
																"{KickedUser}",
																iuser.nick)
														.replace("{KickedBy}",
																kickedByNick)
														.replace(
																"{Reason}",
																stripIRCFormatting(reason)));
						} else {
							println(":" + sourceUID + " KICK " + Config.getIrcdChannel()
									+ " " + uid + " :" + kickedByNick);
							if (msgIRCKick.length() > 0)
								IRCd.broadcastMessage(
												msgIRCKick
														.replace(
																"{KickedUser}",
																iuser.nick)
														.replace("{KickedBy}",
																kickedByNick)
														.replace(
																"{KickedPrefix}",
																IRCd.getGroupPrefix(iuser
																		.getTextModes()))
														.replace(
																"{KickedSuffix}",
																IRCd.getGroupSuffix(iuser
																		.getTextModes()))
														.replace(
																"{KickerPrefix}",
																IRCd.getGroupPrefix(IRCd
																		.getIRCUser(
																				kickedByNick)
																		.getTextModes()))
														.replace(
																"{KickerSuffix}",
																IRCd.getGroupSuffix(IRCd
																		.getIRCUser(
																				kickedByNick)
																		.getTextModes())));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCKickDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
										"IRC",
										msgIRCKickDynmap.replace(
												"{KickedUser}", iuser.nick)
												.replace("{KickedBy}",
														kickedByNick));
						}
						returnVal = true;
						iuser.joined = false;
					} else
						BukkitIRCdPlugin.log.info("Player " + kickedByNick
								+ " tried to kick IRC user not on channel: "
								+ iuser.nick); // Log this as severe since it
					// should never occur unless
					// something is wrong with the
					// code

					return returnVal;
				} else
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] User "
							+ ircuser.nick
							+ " not found in UID list. Error code IRCd942."); // Log
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
			}
		}
		return false;

	}

	public static boolean banIRCUser(IRCUser ircuser, String bannedBy,
			String bannedByHost, boolean isIngame, String banType) {
		// TODO: Add support for banning in linking mode
		if (ircuser == null)
			return false;
		synchronized (csIrcUsers) {
			// ClientConnection processor;
			IRCUser[] ircusers = getIRCUsers();
			for (int i = 0; i < ircusers.length; i++) {
				ircuser = ircusers[i];
				if (ircuser.nick.equalsIgnoreCase(ircuser.nick)) {
					if (isIngame) {
						bannedByHost = bannedBy + Config.getIrcdIngameSuffix() + "!" + bannedBy
								+ "@" + bannedByHost;
						bannedBy += Config.getIrcdIngameSuffix();
					}
					String banHost;
					if ((banType.equals("host"))
							|| (banType.equals("hostname")))
						banHost = "*!*@" + ircuser.hostmask;
					else if ((banType.equals("ip"))
							|| (banType.equals("ipaddress")))
						banHost = "*!*@" + ircuser.ipaddress;
					else if (banType.equals("ident"))
						banHost = "*!" + ircuser.ident + "@*";
					else
						banHost = ircuser.nick + "!*@*";
					boolean result = banIRCUser(banHost, bannedByHost);
					if (result) {
						if (ircuser.joined) {
							if ((isPlugin)
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								if (msgIRCBan.length() > 0)
									IRCd.broadcastMessage(
													msgIRCBan
															.replace(
																	"{BannedUser}",
																	ircuser.nick)
															.replace(
																	"{BannedBy}",
																	bannedBy));
								if ((BukkitIRCdPlugin.dynmap != null)
										&& (msgIRCBanDynmap.length() > 0))
									BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
											"IRC",
											msgIRCBanDynmap.replace(
													"{BannedUser}",
													ircuser.nick).replace(
													"{BannedBy}", bannedBy));
							}
						}
					}
					return result;
				}
			}
		}
		return false;
	}

	public static boolean banIRCUser(String banHost, String bannedByHost) {
		synchronized (csIrcBans) {
			if (isBanned(banHost))
				return false;
			else {
				if (mode == Modes.STANDALONE) {
					ircBans.add(new IrcBan(banHost, bannedByHost, System
							.currentTimeMillis() / 1000L));
					writeAll(":" + bannedByHost + " MODE " + Config.getIrcdChannel()
							+ " + b " + banHost);
					return true;
				} else if (mode == Modes.INSPIRCD) {
					String user = bannedByHost.split("!")[0];
					if (user.endsWith(Config.getIrcdIngameSuffix()))
						user = user.substring(0,
								user.length() - Config.getIrcdIngameSuffix().length());
					String UID;
					BukkitPlayer bp = null;
					if (((UID = getUIDFromIRCUser(user)) != null)
							|| ((bp = getBukkitUserObject(user)) != null)
							|| (user.equals(Config.getIrcdServerName()))) {
						if (user.equals(Config.getIrcdServerName()))
							UID = serverUID;
						else if (UID == null)
							UID = bp.getUID();
						println(":" + UID + " FMODE " + Config.getIrcdChannel() + " "
								+ channelTS + " + b :" + banHost);
						return true;
					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log
							.severe("[BukkitIRCd] User "
									+ user
									+ " not found in UID list. Error code IRCd1004."); // Log
						}

						return false;
					}
				}
				return false;
			}
		}
	}

	public static boolean unBanIRCUser(String banHost, String bannedByHost) {
		synchronized (csIrcBans) {
			int ban = -1;
			if (mode == Modes.STANDALONE) {
				if ((ban = getIRCBan(banHost)) < 0)
					return false;
				ircBans.remove(ban);
				IRCd.writeAll(":" + bannedByHost + " MODE " + Config.getIrcdChannel()
						+ " -b " + banHost);
				return true;
			} else if (mode == Modes.INSPIRCD) {
				String user = bannedByHost.split("!")[0];
				if (user.endsWith(Config.getIrcdIngameSuffix()))
					user = user.substring(0,
							user.length() - Config.getIrcdIngameSuffix().length());
				String UID;
				BukkitPlayer bp = null;
				if (((UID = getUIDFromIRCUser(user)) != null)
						|| ((bp = getBukkitUserObject(user)) != null)
						|| (user.equals(Config.getIrcdServerName()))) {
					if (user.equals(Config.getIrcdServerName()))
						UID = serverUID;
					else if (UID == null)
						UID = bp.getUID();
					println(":" + UID + " FMODE " + Config.getIrcdChannel() + " "
							+ channelTS + " -b :" + banHost);
					return true;
				} else {
					if (Config.isDebugModeEnabled()) {
						BukkitIRCdPlugin.log.severe("[BukkitIRCd] User " + user
								+ " not found in UID list. Error code IRCd1034."); // Log
					}


					return false;
				}
			}
			return false;
		}
	}

	public static boolean isBanned(String fullHost) {
		synchronized (csIrcBans) {
			for (IrcBan ircBan : ircBans) {
				if (wildCardMatch(fullHost, ircBan.fullHost))
					return true;
			}
		}
		return false;
	}

	public static int getIRCBan(String fullHost) {
		synchronized (csIrcBans) {
			int i = 0;
			while (i < ircBans.size()) {
				if (ircBans.get(i).fullHost.equalsIgnoreCase(fullHost))
					return i;
				i++;
			}
		}
		return -1;
	}

	public static boolean wildCardMatch(String text, String pattern) {
		// add sentinel so don't need to worry about *'s at end of pattern
		text += '\0';
		pattern += '\0';

		int N = pattern.length();

		boolean[] states = new boolean[N + 1];
		boolean[] old = new boolean[N + 1];
		old[0] = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			states = new boolean[N + 1]; // initialized to false
			for (int j = 0; j < N; j++) {
				char p = pattern.charAt(j);

				// hack to handle *'s that match 0 characters
				if (old[j] && (p == '*'))
					old[j + 1] = true;

				if (old[j] && (p == c))
					states[j + 1] = true;
				if (old[j] && (p == '?'))
					states[j + 1] = true;
				if (old[j] && (p == '*'))
					states[j] = true;
				if (old[j] && (p == '*'))
					states[j + 1] = true;
			}
			old = states;
		}
		return states[N];
	}

	/*
	public static boolean addBukkitUser(String modes, String nick,
			String world, String host, String ip) {
		if (getBukkitUser(nick) < 0) {
			synchronized (csBukkitPlayers) {
				BukkitPlayer bp = new BukkitPlayer(nick, world, modes, host, host,
						ip, System.currentTimeMillis() / 1000L,
						System.currentTimeMillis());
				bukkitPlayers.add(bp);

				if (mode == Modes.STANDALONE) {
					writeAll(":" + nick + Config.getIrcdIngameSuffix() + "!" + nick + "@"
							+ host + " JOIN " + Config.getIrcdChannel());
				}
				String mode1 = "+", mode2 = "";
				if (modes.contains("~")) {
					mode1 += "q";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("&")) {
					mode1 += "a";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("@")) {
					mode1 += "o";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("%")) {
					mode1 += "h";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("+")) {
					mode1 += "v";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (!mode1.equals("+")) {
					if (mode == Modes.STANDALONE) {

							writeAll(":" + Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@"
								+ Config.getIrcdServerHostName() + " MODE " + Config.getIrcdChannel()
					}
				}

				if (mode == Modes.INSPIRCD) {

					String UID = ugen.generateUID(Config.getLinkServerID());
					bp.setUID(UID);
					synchronized (csBukkitPlayers) {
						String textMode = bp.getTextMode();
						if (bp.hasPermission("bukkitircd.oper")) {
							println(pre + "UID " + UID + " "
									+ (bp.idleTime / 1000L) + " " + bp.nick
									+ Config.getIrcdIngameSuffix() + " " + bp.host + " "
									+ bp.host + " " + bp.nick + " " + bp.ip
									+ " " + bp.signedOn
									+ " +or :Minecraft Player");
							println(":" + UID + " OPERTYPE IRC_Operator");
						} else
							println(pre + "UID " + UID + " "
									+ (bp.idleTime / 1000L) + " " + bp.nick
									+ Config.getIrcdIngameSuffix() + " " + bp.host + " "
									+ bp.host + " " + bp.nick + " " + bp.ip
									+ " " + bp.signedOn
									+ " +r :Minecraft Player");

						println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS
								+ " +nt :," + UID);
						if (textMode.length() > 0) {
							String modestr = "";
							for (int i = 0; i < textMode.length(); i++) {
								modestr += UID + " ";
							}
							modestr = modestr
									.substring(0, modestr.length() - 1);
							println(":" + serverUID + " FMODE " + Config.getIrcdChannel()
									+ " " + channelTS + " +" + textMode + " "
									+ modestr);
						}
						if (world != null)
							println(pre + "METADATA " + UID
									+ " swhois :is currently in " + world);
						else
							println(pre
									+ "METADATA "
									+ UID
									+ " swhois :is currently in an unknown world");
					}
				}
			}
			return true;
		} else
			return false;
	}
	*/

	private static String hashPart(byte itemId, byte[] item, int itemLen, int outLen) throws NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("MD5");

		md.update(itemId);
		md.update(Config.getHostMaskKey().getBytes());
		md.update((byte)0);
		md.update(item, 0, itemLen);

		final byte[] d = md.digest();

		final String alphabet = "0123456789abcdefghijklmnopqrstuv";

		String output = "";
		for (int i = 0; i < outLen; i++) {
			output = output + alphabet.charAt((d[i] + 256) % 32);
		}

		return output;
	}

	public static String maskHost(InetAddress ip) {
		if (Config.isUseHostMask()) {
			final byte[] bytes = ip.getAddress();
			try {
				String maskPrefix = Config.getHostMaskPrefix();
				String maskSuffix = Config.getHostMaskSuffix();
				return maskPrefix + hashPart ((byte)10, bytes, 4, 3)
						    + "." + hashPart ((byte)11, bytes, 3, 3)
					        + "." + hashPart ((byte)13, bytes, 2, 6)
					        + maskSuffix;
			} catch (NoSuchAlgorithmException e) {
				return ip.getHostName();
			}
		} else {
			return ip.getHostName();
		}
	}

	public static boolean addBukkitUser(String modes, Player player) {
		String nick = player.getName();
		String host = maskHost(player.getAddress().getAddress());
		String realhost = player.getAddress().getAddress().getHostName();
		String ip = player.getAddress().getAddress().getHostAddress();
		String world = player.getWorld().getName();
		if (getBukkitUser(nick) < 0) {
			synchronized (csBukkitPlayers) {
				BukkitPlayer bp = new BukkitPlayer(nick, world, modes, realhost, host,
						ip, System.currentTimeMillis() / 1000L,
						System.currentTimeMillis());
				bukkitPlayers.add(bp);
				if (mode == Modes.STANDALONE) {
					writeAll(":" + nick + Config.getIrcdIngameSuffix() + "!" + nick + "@"
							+ host + " JOIN " + Config.getIrcdChannel());
				}
				String mode1 = "+", mode2 = "";
				if (modes.contains("~")) {
					mode1 += "q";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("&")) {
					mode1 += "a";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("@")) {
					mode1 += "o";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("%")) {
					mode1 += "h";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (modes.contains("+")) {
					mode1 += "v";
					mode2 += nick + Config.getIrcdIngameSuffix() + " ";
				}
				if (!mode1.equals("+")) {
					if (mode == Modes.STANDALONE) {
						writeAll(":" + Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@"
								+ Config.getIrcdServerHostName() + " MODE " + Config.getIrcdChannel()
								+ " " + mode1 + " "
								+ mode2.substring(0, mode2.length() - 1));
					}
				}

				if (mode == Modes.INSPIRCD) {
					String UID = ugen.generateUID(Config.getLinkServerID());
					bp.setUID(UID);
					synchronized (csBukkitPlayers) {

						final boolean isOper = bp.hasPermission("bukkitircd.oper");

						// Register new UID
						final String userModes = isOper ? "+or" : "+r";
						println(pre + "UID",
								UID,
								Long.toString(bp.idleTime / 1000L),
								bp.nick+Config.getIrcdIngameSuffix(),
								bp.realhost,
								bp.host,
								bp.nick, // user
								bp.ip,
								Long.toString(bp.signedOn),
								userModes,
								":Minecraft Player");

						// Set oper type if appropriate
						if (isOper) {
							println(":" + UID, "OPERTYPE", "IRC_Operator");
						}

						// Game client uses encrypted connection
						println(pre + "METADATA", UID, "ssl_cert", ":vtrsE The peer did not send any certificate.");

						// Join in-game channel with modes set
						println(pre + "FJOIN", Config.getIrcdChannel(), Long.toString(channelTS),
								"+nt",
								":" + bp.getTextMode() + "," + UID);

						// Send swhois field (extra metadata used for current world here)
						final String worldString = world == null ? "an unknown world" : world;
						println(pre + "METADATA ", UID, "swhois" , ":is currently in " + worldString);
					}
				}
				return true;
			}
		} else
			return false;
	}

	// Run when a player disconnects (maybe make the quit message configurable
	public static boolean removeBukkitUser(int ID) {
		synchronized (csBukkitPlayers) {
			if (ID >= 0) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				if (mode == Modes.STANDALONE) {
					writeAll(":" + bp.nick + Config.getIrcdIngameSuffix() + "!" + bp.nick + "@"
							+ bp.host + " QUIT :" + msgDisconnectQuitting);
				} else if (mode == Modes.INSPIRCD) {
					println(":" + bp.getUID() + " QUIT :" + msgDisconnectQuitting);
				}
				bukkitPlayers.remove(ID);
				return true;
			} else
				return false;
		}
	}

	public static boolean removeBukkitUserByUID(String UID) {
		synchronized (csBukkitPlayers) {
			Iterator<BukkitPlayer> iter = bukkitPlayers.iterator();
			while (iter.hasNext()) {
				BukkitPlayer bp = iter.next();
				if (bp.getUID().equalsIgnoreCase(UID)) {
					iter.remove();
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Used for Console Kicks
	 *
	 * @param kickReason
	 * @param kickedID
	 * @return
	 */
	public static boolean kickBukkitUser(String kickReason, int kickedID) {
		if (kickedID >= 0) {
			synchronized (csBukkitPlayers) {
				BukkitPlayer kickedBukkitPlayer = bukkitPlayers.get(kickedID);
				if (!kickReason.isEmpty()) {
					kickReason = " :" + convertColors(kickReason, false);
				}
				if (mode == Modes.STANDALONE) {
					writeAll(":" + Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@"
							+ Config.getIrcdServerHostName() + " KICK " + Config.getIrcdChannel()
							+ " " + kickedBukkitPlayer.nick + Config.getIrcdIngameSuffix()
							+ kickReason);
				} else {

					// KICK
					println(":" + serverUID + " KICK " + Config.getIrcdChannel() + " "
							+ kickedBukkitPlayer.nick + Config.getIrcdIngameSuffix()
							+ kickReason);
				}
				return true;
			}
		} else
			return false;
	}

	/**
	 * Used for player kicks
	 *
	 * @param kickReason
	 * @param kickedID
	 * @param kickerID
	 * @return
	 */
	public static boolean kickBukkitUser(String kickReason, int kickedID,
			int kickerID) {
		if (kickedID >= 0) {
			synchronized (csBukkitPlayers) {
				BukkitPlayer kickedBukkitPlayer = bukkitPlayers.get(kickedID);

				BukkitPlayer kickerBukkitPlayer = bukkitPlayers.get(kickerID);
				String kickerHost = kickedBukkitPlayer.host;
				String kickerName = kickerBukkitPlayer.nick;

				if (!kickReason.isEmpty()) {
					kickReason = " :" + convertColors(kickReason, false);
				}
				if (mode == Modes.STANDALONE) {
					writeAll(":" + kickerName + Config.getIrcdIngameSuffix() + "!" + kickerName
							+ "@" + kickerHost + " KICK " + Config.getIrcdChannel()
							+ " " + kickedBukkitPlayer.nick + Config.getIrcdIngameSuffix()
							+ convertColors(kickReason, false));
				} else {

					// KICK
					println(":" + kickerBukkitPlayer.getUID() + " KICK "
							+ Config.getIrcdChannel() + " " + kickedBukkitPlayer.nick
							+ Config.getIrcdIngameSuffix() + convertColors(kickReason, false));
				}
				return true;
			}
		} else
			return false;
	}

	/**
	 * Kicks player synchronously
	 *
	 * @param player
	 * @param kickReason
	 */
	public static boolean kickPlayerIngame(final String kicker, final String kickee, final String kickReason) {
		int IRCUser = getBukkitUser(kickee);
		IRCd.kickBukkitUser(kickReason, IRCUser);
		IRCd.removeBukkitUser(IRCUser);

		try {
			new BukkitRunnable() {
				@Override
				public void run() {
					final Server server = Bukkit.getServer();
					final Player player = server.getPlayer(kickee);
					if (player != null) {

						if (kickReason == null || kickReason == kicker) {
							server.broadcastMessage(msgIRCKick.replace(
									"{KickedBy}", kicker).replace(
									"{KickedUser}", player.getDisplayName()));
						} else {
							server.broadcastMessage(msgIRCKickReason
									.replace("{KickedBy}", kicker)
									.replace("{KickedUser}",
											player.getDisplayName())
									.replace("{Reason}", kickReason));
						}

						final String kickText;
						if (kickReason == null || kickReason == kicker) {
							kickText = msgIRCKickDisplay
									.replace("{KickedBy}", kicker)
									.replace("{Reason}", kickReason);
									
						} else {
							kickText = msgIRCKickDisplayReason
									.replace("{KickedBy}", kicker)
									.replace("{Reason}", kickReason);
						}
						player.kickPlayer(kickText);
					}
				}
			}.runTask(BukkitIRCdPlugin.thePlugin);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Broadcasts a message
	 *
	 * @param msg
	 * @return true if able to schedule broadcast
	 */
	public static boolean broadcastMessage(final String msg) {

		try {
			new BukkitRunnable() {
				@Override
				public void run() {
					Bukkit.getServer().broadcastMessage(msg);
				}
			}.runTask(BukkitIRCdPlugin.thePlugin);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Send a message to a player
	 *
	 * @param msg
	 */
	public static boolean sendMessage(final String player, final String msg) {

		try {
		new BukkitRunnable() {
			@Override
			public void run() {
				final Player p = Bukkit.getServer().getPlayer(player);
				if (p != null) p.sendMessage(msg);
			}
		}.runTask(BukkitIRCdPlugin.thePlugin);
		return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static int getBukkitUser(String nick) {
		synchronized (csBukkitPlayers) {
			int i = 0;
			String curnick;
			while (i < bukkitPlayers.size()) {
				curnick = bukkitPlayers.get(i).nick;
				if ((curnick.equalsIgnoreCase(nick))
						|| ((curnick + Config.getIrcdIngameSuffix()).equalsIgnoreCase(nick))) {
					return i;
				} else
					i++;
			}
			return -1;
		}
	}

	public static BukkitPlayer getBukkitUserObject(String nick) {
		synchronized (csBukkitPlayers) {
			int i = 0;
			String curnick;
			while (i < bukkitPlayers.size()) {
				BukkitPlayer bp = bukkitPlayers.get(i);
				curnick = bp.nick;
				if ((curnick.equalsIgnoreCase(nick))
						|| ((curnick + Config.getIrcdIngameSuffix()).equalsIgnoreCase(nick))) {
					return bp;
				}
				i++;
			}
			return null;
		}
	}

	public static BukkitPlayer getBukkitUserByUID(String UID) {
		synchronized (csBukkitPlayers) {
			int i = 0;
			BukkitPlayer bp;
			while (i < bukkitPlayers.size()) {
				bp = bukkitPlayers.get(i);
				if (bp.getUID().equalsIgnoreCase(UID)) {
					return bp;
				} else
					i++;
			}
			return null;
		}
	}

	public static boolean updateBukkitUserIdleTimeAndWorld(int ID, String world) {
		if (ID >= 0) {
			synchronized (csBukkitPlayers) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				bp.idleTime = System.currentTimeMillis();
				if (!bp.world.equals(world)) {
					bp.world = world;
					if (mode == Modes.INSPIRCD) {
						println(pre + "METADATA " + bp.getUID()
								+ " swhois :is currently in " + world);
					}
				}
				return true;
			}
		} else
			return false;
	}

	public static boolean updateBukkitUserIdleTime(int ID) {
		if (ID >= 0) {
			synchronized (csBukkitPlayers) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				bp.idleTime = System.currentTimeMillis();
				return true;
			}
		} else
			return false;
	}

	public static void writeAll(String message, Player sender) {
		int i = 0;
		String line = "", host = "unknown", nick = "Unknown";

		synchronized (csBukkitPlayers) {
			int ID = getBukkitUser(sender.getName());
			if (ID >= 0) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				host = bp.host;
				nick = bp.nick;
			}
		}

		line = ":" + nick + Config.getIrcdIngameSuffix() + "!" + nick + "@" + host
				+ " PRIVMSG " + Config.getIrcdChannel() + " :" + message;

		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor.isConnected())
							&& processor.isIdented
							&& processor.isNickSet
							&& (processor.lastPingResponse
									+ (Config.getIrcdPinkTimeoutInterval() * 1000) > System
										.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					} else if (!processor.running) {
						removeIRCUser(processor.nick);
					} else
						i++;
				}
			}
		}
	}

	public static void writeAll(String line) {
		int i = 0;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor.isConnected())
							&& processor.isIdented
							&& processor.isNickSet
							&& (processor.lastPingResponse
									+ (Config.getIrcdPinkTimeoutInterval() * 1000) > System
										.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					} else if (!processor.running) {
						removeIRCUser(processor.nick);
					} else
						i++;
				}
			}
		}
	}

	public static void writeOpers(String line) {
		int i = 0;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor.isConnected())
							&& processor.isIdented
							&& processor.isNickSet
							&& processor.isOper
							&& (processor.lastPingResponse
									+ (Config.getIrcdPinkTimeoutInterval() * 1000) > System
										.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					} else if (!processor.running) {
						removeIRCUser(processor.nick);
					} else
						i++;
				}
			}
		}
	}

	public static void disconnectAll() {
		disconnectAll(null);
	}

	public static void disconnectAll(String reason) {
		synchronized (csIrcUsers) {
			switch (mode) {
			case STANDALONE:
				try {
					listener.close();
					listener = null;
				} catch (IOException e) {
				}
				removeIRCUsers(reason);
				break;
			case INSPIRCD:
				disconnectServer(reason);
				break;
			}
		}
	}

	public static void writeAllExcept(String nick, String line) {
		int i = 0;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if (processor.nick.equalsIgnoreCase(nick)) {
						i++;
						continue;
					}
					if ((processor.isConnected())
							&& processor.isIdented
							&& processor.isNickSet
							&& (processor.lastPingResponse
									+ (Config.getIrcdPinkTimeoutInterval() * 1000) > System
										.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					} else if (!processor.running) {
						removeIRCUser(processor.nick);
					} else
						i++;
				}
			}
		}
	}

	public static void writeOpersExcept(String nick, String line) {
		int i = 0;
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if (processor.nick.equalsIgnoreCase(nick)) {
						i++;
						continue;
					}
					if ((processor.isConnected())
							&& processor.isIdented
							&& processor.isNickSet
							&& processor.isOper
							&& (processor.lastPingResponse
									+ (Config.getIrcdPinkTimeoutInterval() * 1000) > System
										.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					} else if (!processor.running) {
						removeIRCUser(processor.nick);
					} else
						i++;
				}
			}
		}
	}

	public static boolean writeTo(String nick, String line) {
		synchronized (csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					ClientConnection processor = iter.next();
					if (processor.nick.equalsIgnoreCase(nick)) {
						processor.writeln(line);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Converts colors from Minecrat to IRC, or IRC to Minecraft if specified
	 *
	 * @param input
	 * @param fromIRCtoGame
	 *            Convert IRC colors to Minecraft colors?
	 * @return
	 */
	public static String convertColors(String input, boolean fromIRCtoGame) {

		String output = null;
		char IRC_Color = (char) 3; // ETX Control Code (^C)
		char IRC_Bold = (char) 2; // STX Control Code (^B)
		char IRC_Ital = (char) 29; // GS Control Code
		char IRC_Under = (char) 31; // US Control Code (^_)
		char IRC_Reset = (char) 15; // SI Control Code (^O)
		char MC_Color = (char) 167; // Section Sign
		if (fromIRCtoGame) {
			if (!Config.isIrcdConvertColorCodes()) {
				return IRCd.stripIRCFormatting(input);
			}
			output = input.replaceAll("(\\d),\\d{1,2}", "$1"); // Remove IRC
			// background
			// color code

			output = output.replace(IRC_Reset + "", MC_Color + "r");
			output = output.replace(IRC_Ital + "", MC_Color + "o");
			output = output.replace(IRC_Bold + "", MC_Color + "l");
			output = output.replace(IRC_Under + "", MC_Color + "n");

			output = output.replace(IRC_Color + "01", MC_Color + "0"); // IRC
			// Black
			// to MC
			// Black
			output = output.replace(IRC_Color + "02", MC_Color + "1");// IRC
			// Dark
			// Blue
			// to MC
			// Dark
			// Blue
			output = output.replace(IRC_Color + "03", MC_Color + "2"); // IRC
			// Dark
			// Green
			// to MC
			// Dark
			// Green
			output = output.replace(IRC_Color + "04", MC_Color + "c"); // IRC
			// Red
			// to MC
			// Red
			output = output.replace(IRC_Color + "05", MC_Color + "4"); // IRC
			// Dark
			// Red
			// to MC
			// Dark
			// Red
			output = output.replace(IRC_Color + "06", MC_Color + "5"); // IRC
			// Purple
			// to MC
			// Purple
			output = output.replace(IRC_Color + "07", MC_Color + "6"); // IRC
			// Dark
			// Yellow
			// to MC
			// Gold
			output = output.replace(IRC_Color + "08", MC_Color + "e"); // IRC
			// Yellow
			// to MC
			// Yellow
			output = output.replace(IRC_Color + "09", MC_Color + "a"); // IRC
			// Light
			// Green
			// to MC
			// Green
			output = output.replace(IRC_Color + "10", MC_Color + "3"); // IRC
			// Teal
			// to MC
			// Dark
			// Aqua
			output = output.replace(IRC_Color + "11", MC_Color + "b"); // IRC
			// Cyan
			// to MC
			// Aqua
			output = output.replace(IRC_Color + "12", MC_Color + "9"); // IRC
			// Light
			// Blue
			// to MC
			// Blue
			output = output.replace(IRC_Color + "13", MC_Color + "d"); // IRC
			// Light
			// Purple
			// to MC
			// Pink
			output = output.replace(IRC_Color + "14", MC_Color + "8"); // IRC
			// Grey
			// to MC
			// Dark
			// Grey
			output = output.replace(IRC_Color + "15", MC_Color + "7"); // IRC
			// Light
			// Grey
			// to MC
			// Grey

			output = output.replace(IRC_Color + "1", MC_Color + "0"); // IRC
			// Black
			// to MC
			// Black
			output = output.replace(IRC_Color + "2", MC_Color + "1");// IRC Dark
			// Blue
			// to MC
			// Dark
			// Blue
			output = output.replace(IRC_Color + "3", MC_Color + "2"); // IRC
			// Dark
			// Green
			// to MC
			// Dark
			// Green
			output = output.replace(IRC_Color + "4", MC_Color + "c"); // IRC Red
			// to MC
			// Red
			output = output.replace(IRC_Color + "5", MC_Color + "4"); // IRC
			// Dark
			// Red
			// to MC
			// Dark
			// Red
			output = output.replace(IRC_Color + "6", MC_Color + "5"); // IRC
			// Purple
			// to MC
			// Purple
			output = output.replace(IRC_Color + "7", MC_Color + "6"); // IRC
			// Dark
			// Yellow
			// to MC
			// Gold
			output = output.replace(IRC_Color + "8", MC_Color + "e"); // IRC
			// Yellow
			// to MC
			// Yellow
			output = output.replace(IRC_Color + "9", MC_Color + "a"); // IRC
			// Light
			// Green
			// to MC
			// Green
			output = output.replace(IRC_Color + "0", MC_Color + "f"); // IRC
			// White
			// to MC
			// White

			output = output.replace(IRC_Color + "", ""); // Get rid of any
			// remaining ETX
			// Characters
			output = output.replace(IRC_Ital + "", ""); // Get rid of any
			// remaining GS
			// Characters
			output = output.replace(IRC_Bold + "", ""); // Get rid of any
			// remaining STX
			// Characters
			output = output.replace(IRC_Under + "", ""); // Get rid of any
			// remaining US
			// Characters

		} else {
			if (!Config.isIrcdConvertColorCodes()) {
				return ChatColor.stripColor(input);
			}
			if (Config.isIrcdHandleAmpersandColors()) {
				output = ChatColor.translateAlternateColorCodes('&', input);
			} else {
				output = input;
			}
			output = output.replace(MC_Color + "n", IRC_Under + "");
			output = output.replace(MC_Color + "o", IRC_Ital + "");
			output = output.replace(MC_Color + "l", IRC_Bold + "");
			output = output.replace(MC_Color + "r", IRC_Reset + "");
			output = output.replace(MC_Color + "m", ""); // IRC Does not have
			// support for
			// Strikethrough
			output = output.replace(MC_Color + "k", ""); // IRC Does not have
			// support for
			// Garbled Text

			output = output.replace(MC_Color + "0", IRC_Color + "01"); // Minecraft
			// Black
			// to
			// IRC
			// Black
			output = output.replace(MC_Color + "1", IRC_Color + "02"); // Minecraft
			// Dark
			// Blue
			// to
			// IRC
			// Dark
			// Blue
			output = output.replace(MC_Color + "2", IRC_Color + "03"); // Minecraft
			// Dark
			// Green
			// to
			// IRC
			// Dark
			// Green
			output = output.replace(MC_Color + "3", IRC_Color + "10"); // Minecraft
			// Dark
			// Aqua
			// to
			// IRC
			// Teal
			output = output.replace(MC_Color + "4", IRC_Color + "05"); // Minecraft
			// Dark
			// Red
			// to
			// IRC
			// Dark
			// Red
			output = output.replace(MC_Color + "5", IRC_Color + "06"); // Minecraft
			// Purple
			// to
			// IRC
			// Purple
			output = output.replace(MC_Color + "6", IRC_Color + "07"); // Minecraft
			// Gold
			// to
			// IRC
			// Dark
			// Yellow
			output = output.replace(MC_Color + "7", IRC_Color + "15"); // Minecraft
			// Grey
			// to
			// IRC
			// Light
			// Grey
			output = output.replace(MC_Color + "8", IRC_Color + "14"); // Minecraft
			// Dark
			// Grey
			// to
			// IRC
			// Grey
			output = output.replace(MC_Color + "9", IRC_Color + "12"); // Minecraft
			// Blue
			// to
			// IRC
			// Light
			// Blue
			output = output.replace(MC_Color + "a", IRC_Color + "09"); // Minecraft
			// Green
			// to
			// IRC
			// Light
			// Green
			output = output.replace(MC_Color + "b", IRC_Color + "11"); // Minecraft
			// Aqua
			// to
			// IRC
			// Cyan
			output = output.replace(MC_Color + "c", IRC_Color + "04"); // Minecraft
			// Red
			// to
			// IRC
			// Red
			output = output.replace(MC_Color + "d", IRC_Color + "13"); // Minecraft
			// Light
			// Purple
			// to
			// IRC
			// Pink
			output = output.replace(MC_Color + "e", IRC_Color + "08"); // Minecraft
			// Yellow
			// to
			// IRC
			// Yellow
			output = output.replace(MC_Color + "f", IRC_Color + "00"); // Minecraft
			// White
			// to
			// IRC
			// White

		}

		return output;
	}

	/**
	 * Strips IRC Formatting
	 *
	 * @param input
	 * @return
	 */
	public static String stripIRCFormatting(String input) {
		char IRC_Color = (char) 3; // ETX Control Code (^C)
		char IRC_Bold = (char) 2; // STX Control Code (^B)
		char IRC_Ital = (char) 29; // GS Control Code
		char IRC_Under = (char) 31; // US Control Code (^_)
		char IRC_Reset = (char) 15; // SI Control Code (^O)

		String output = input.replaceAll("\u0003[0-9]{1,2}(,[0-9]{1,2})?", ""); // Remove
		// IRC
		// background
		// color
		// code
		output = output.replace(IRC_Reset + "", "");
		output = output.replace(IRC_Ital + "", "");
		output = output.replace(IRC_Bold + "", "");
		output = output.replace(IRC_Under + "", "");
		output = output.replace(IRC_Color + "", "");
		return output;
	}

	/**
	 * Gets group prefix from modes
	 *
	 * @param modes
	 * @return
	 */
	public static String getGroupPrefix(String modes) {
		// Goes from highest rank to lowest rank
		String prefix;
		// Owner

		if (IRCd.groupPrefixes == null) {
			return "";
		}
		if (IRCd.groupPrefixes.contains("q")
				&& (modes.contains("q") || modes.contains("~"))) {
			try {
				prefix = IRCd.groupPrefixes.getString("q");
			} catch (NullPointerException e) {
				return "";
			}
			if (!prefix.isEmpty() || prefix != null) {
				return ChatColor.translateAlternateColorCodes('&', prefix);
			}
		}

		// replace("@", "o").replace("%", "h").replace("+", "v");

		// Super Op
		if (IRCd.groupPrefixes.contains("a")
				&& (modes.contains("a") || modes.contains("&"))) {
			try {
				prefix = IRCd.groupPrefixes.getString("a");
			} catch (NullPointerException e) {
				return "";
			}
			if (!prefix.isEmpty() || prefix != null) {
				return ChatColor.translateAlternateColorCodes('&', prefix);
			}
		}

		// Op
		if (IRCd.groupPrefixes.contains("o")
				&& (modes.contains("o") || modes.contains("@"))) {
			try {
				prefix = IRCd.groupPrefixes.getString("o");
			} catch (NullPointerException e) {
				return "";
			}
			if (!prefix.isEmpty() || prefix != null) {
				return ChatColor.translateAlternateColorCodes('&', prefix);
			}
		}

		// Half Op
		if (IRCd.groupPrefixes.contains("h")
				&& (modes.contains("h") || modes.contains("%"))) {
			try {
				prefix = IRCd.groupPrefixes.getString("h");
			} catch (NullPointerException e) {
				return "";
			}
			if (!prefix.isEmpty() || prefix != null) {
				return ChatColor.translateAlternateColorCodes('&', prefix);
			}
		}

		// Voice
		if (IRCd.groupPrefixes.contains("q")
				&& (modes.contains("v") || modes.contains("+"))) {
			try {
				prefix = IRCd.groupPrefixes.getString("v");
			} catch (NullPointerException e) {
				return "";
			}
			if (!prefix.isEmpty() || prefix != null) {
				return ChatColor.translateAlternateColorCodes('&', prefix);
			}
		}

		// User
		if (IRCd.groupPrefixes.contains("user")) {
			try {
				prefix = IRCd.groupPrefixes.getString("user");
			} catch (NullPointerException e) {
				return "";
			}
			if (!prefix.isEmpty() || prefix != null) {
				return ChatColor.translateAlternateColorCodes('&', prefix);
			}

		}
		return "";

	}

	/**
	 * Gets group suffix from modes
	 *
	 * @param modes
	 * @return
	 */
	public static String getGroupSuffix(String modes) {
		// Goes from highest rank to lowest rank
		String suffix;

		if (IRCd.groupSuffixes == null) {
			return "";
		}
		// Owner
		if (IRCd.groupSuffixes.contains("q")
				&& (modes.contains("q") || modes.contains("~"))) {
			try {
				suffix = IRCd.groupSuffixes.getString("q");
			} catch (NullPointerException e) {
				return "";
			}
			if (!suffix.isEmpty() || suffix != null) {
				return ChatColor.translateAlternateColorCodes('&', suffix);
			}
		}

		// replace("@", "o").replace("%", "h").replace("+", "v");

		// Super Op
		if (IRCd.groupSuffixes.contains("a")
				&& (modes.contains("a") || modes.contains("&"))) {
			try {
				suffix = IRCd.groupPrefixes.getString("a");
			} catch (NullPointerException e) {
				return "";
			}
			if (!suffix.isEmpty() || suffix != null) {
				return ChatColor.translateAlternateColorCodes('&', suffix);
			}
		}

		// Op
		if (IRCd.groupSuffixes.contains("o")
				&& (modes.contains("o") || modes.contains("@"))) {
			try {
				suffix = IRCd.groupPrefixes.getString("o");
			} catch (NullPointerException e) {
				return "";
			}
			if (!suffix.isEmpty() || suffix != null) {
				return ChatColor.translateAlternateColorCodes('&', suffix);
			}
		}

		// Half Op
		if (IRCd.groupSuffixes.contains("h")
				&& (modes.contains("h") || modes.contains("%"))) {
			try {
				suffix = IRCd.groupPrefixes.getString("h");
			} catch (NullPointerException e) {
				return "";
			}
			if (!suffix.isEmpty() || suffix != null) {
				return ChatColor.translateAlternateColorCodes('&', suffix);
			}
		}

		// Voice
		if (IRCd.groupSuffixes.contains("v")
				&& (modes.contains("v") || modes.contains("+"))) {
			try {
				suffix = IRCd.groupPrefixes.getString("v");
			} catch (NullPointerException e) {
				return "";
			}
			if (!suffix.isEmpty() || suffix != null) {
				return ChatColor.translateAlternateColorCodes('&', suffix);
			}
		}

		// User
		if (IRCd.groupSuffixes.contains("user")) {
			try {
				suffix = IRCd.groupSuffixes.getString("user");
			} catch (NullPointerException e) {
				return "";
			}
			if (!suffix.isEmpty() || suffix != null) {
				return ChatColor.translateAlternateColorCodes('&', suffix);
			}
		}
		return "";

	}

	// This is where the channel topic is configured
	public static void setTopic(String topic, String user, String userhost) {
		channelTopic = topic;
		channelTopicSetDate = System.currentTimeMillis() / 1000L;
		if (user.length() > 0) {
			channelTopicSet = user;
		}
		if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
			Config.setIrcdTopic(topic);
			Config.setIrcdTopicSetDate(System.currentTimeMillis());
			if (user.length() > 0) {
				Config.setIrcdTopicSetBy(user);
			}
		}

		if (mode == Modes.STANDALONE) {
			writeAll(":" + userhost + " TOPIC " + Config.getIrcdChannel() + " :"
					+ channelTopic);
			writeOpers(":" + userhost + " TOPIC " + Config.getIrcdConsoleChannel() + " :"
					+ channelTopic);
		} else if (mode == Modes.INSPIRCD) {
			BukkitPlayer bp;
			if ((bp = getBukkitUserObject(user)) != null) {
				println(":" + bp.getUID() + " TOPIC " + Config.getIrcdChannel() + " :"
						+ channelTopic);
			}
		}
	}

	public static String[] split(String line) {
		String[] sp1 = line.split(" :", 2);
		String[] sp2 = sp1[0].split(" ");
		String[] res;
		if (!sp2[0].startsWith(":")) {
			res = new String[sp1.length + sp2.length];
			System.arraycopy(sp2, 0, res, 1, sp2.length);
		} else {
			res = new String[sp1.length + sp2.length - 1];
			System.arraycopy(sp2, 0, res, 0, sp2.length);
			res[0] = res[0].substring(1);
		}
		if (sp1.length == 2)
			res[res.length - 1] = sp1[1];
		return res;
	}

	public static String join(String[] strArray, String delimiter, int start) {

		if (strArray.length <= start) {
			return "";
		}

		// Compute buffer length
		int size = delimiter.length() * (strArray.length - start - 1);
		for (final String s : strArray) {
			size += s.length();
		}

		final StringBuilder builder = new StringBuilder(size);
		builder.append(strArray[start]);
		for (int i = start + 1; i < strArray.length; i++) {
			builder.append(delimiter).append(strArray[i]);
		}

		return builder.toString();
	}

	public static void executeCommand(final String command) {
		new BukkitRunnable() {

			@Override
			public void run() {
				final Server server = Bukkit.getServer();
				try {
					final ServerCommandEvent commandEvent = new ServerCommandEvent(commandSender,command);
					server.getPluginManager().callEvent(commandEvent);
					server.dispatchCommand(commandEvent.getSender(), commandEvent.getCommand());
					commandSender.sendMessage("Command Executed");
				} catch (CommandException c) {
					Throwable e = c.getCause();

					commandSender.sendMessage("Exception in command \"" + command + "\": " + e);
					if (Config.isDebugModeEnabled()) {
						for (final StackTraceElement s : e.getStackTrace()) {
							commandSender.sendMessage(s.toString());
						}
					}
				} catch (Exception e) {
					commandSender.sendMessage("Exception in command \"" + command + "\": " + e);

					if (Config.isDebugModeEnabled()) {
						for (final StackTraceElement s : e.getStackTrace()) {
							commandSender.sendMessage(s.toString());
						}
					}
				}
			}
		}.runTask(BukkitIRCdPlugin.thePlugin);
	}

	public static boolean println(String ... parts) {
		final String line = IRCd.join(parts, " ", 0);
		if ((server == null) || (!server.isConnected()) || (server.isClosed())
				|| (out == null))
			return false;
		synchronized (csServer) {
			if (Config.isDebugModeEnabled())
				System.out.println("[BukkitIRCd]" + ChatColor.DARK_BLUE
						+ "[<-] " + line);
			out.println(line);
			return true;
		}
	}

	/**
	 *
	 * @param source UID of sender
	 * @param target name of target
	 * @param message message to send encoded with IRC colors
	 */
	public static void privmsg(final String source, final String target, final String message) {
		IRCd.println(":" + source + " PRIVMSG " + target + " :" + message);
	}

	/**
	 *
	 * @param source UID of sender
	 * @param target name of target
	 * @param message message to send with IRC colors
	 */
	public static void action(final String source, final String target, final String message) {
		final String action = (char)1 + "ACTION " + message + (char)1;
		IRCd.privmsg(source, target, action);
	}

	public static void disconnectServer(String reason) {
		synchronized (csServer) {
			if (mode == Modes.INSPIRCD) {
				if ((server != null) && server.isConnected()) {
					println(pre + "SQUIT " + Config.getLinkServerID() + " :" + reason);
					if (linkcompleted) {
						if (reason != null && msgDelinkedReason.length() > 0)
							IRCd.broadcastMessage(
											msgDelinkedReason
													.replace("{LinkName}",
															Config.getLinkName())
													.replace("{Reason}", reason));
						linkcompleted = false;
					}
					try {
						server.close();
					} catch (IOException e) {
					}
				} else if (Config.isDebugModeEnabled())
					System.out
							.println("[BukkitIRCd] Already disconnected from link, so no need to cleanup.");
			}
		}
		if (listener != null)
			try {
				listener.close();
			} catch (IOException e) {
			}
	}

	public static String getUIDFromIRCUser(IRCUser user) {
		Iterator<Entry<String, IRCUser>> iter = uid2ircuser.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Map.Entry<String, IRCUser> entry = iter.next();
			String UID = entry.getKey();
			IRCUser ircuser = entry.getValue();
			if (ircuser.nick.equalsIgnoreCase(user.nick)) {
				return UID;
			}
		}
		return null;
	}

	public static String getUIDFromIRCUser(String user) {
		Iterator<Entry<String, IRCUser>> iter = uid2ircuser.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Map.Entry<String, IRCUser> entry = iter.next();
			String UID = entry.getKey();
			IRCUser ircuser = entry.getValue();
			if (ircuser.nick.equalsIgnoreCase(user)) {
				return UID;
			}
		}
		return null;
	}

	public void parseLinkCommand(String command) throws IOException {
		if (Config.isDebugModeEnabled())
			BukkitIRCdPlugin.log.info("[BukkitIRCd]" + ChatColor.YELLOW
					+ "[->] " + command);

		String split[] = command.split(" ");
		if (split.length <= 1)
			return;
		if (split[0].startsWith(":"))
			split[0] = split[0].substring(1);

		if (split[1].equalsIgnoreCase("PING")) {
			// Incoming ping, respond with pong so we don't get timed out from
			// the server'
			// :280 PING 280 123
			linkLastPingPong = System.currentTimeMillis();
			if (split.length == 3)
				println(pre + "PONG " + split[2]);
			else if ((split.length == 4)
					&& (split[3].equalsIgnoreCase(Integer.toString(Config.getLinkServerID()))))
				println(pre + "PONG " + Config.getLinkServerID() + " " + split[2]);
		} else if (split[1].equalsIgnoreCase("PONG")) {
			// Received a pong, update the last ping pong timestamp.
			// :280 PONG 280 123
			linkLastPingPong = System.currentTimeMillis();
		} else if (split[1].equalsIgnoreCase("ERROR")) {
			// :280 ERROR :Unrecognised or malformed command 'CAPAB' -- possibly
			// loaded mismatched modules
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			throw new IOException(
					"Remote host rejected connection, probably configured wrong: "
							+ join(split, " ", 2));
		} else if (split[1].equalsIgnoreCase("UID")) {
			// New user connected, add to IRC user list by UID);
			// :0IJ UID 0IJAAAAAP 1321966480 qlum ip565fad97.direct-adsl.nl
			// 2ast9v.direct-adsl.nl qlum 86.95.173.151 1321966457 + i :purple
			String UID = split[2];
			long idleTime = Long.parseLong(split[3]) * 1000;
			String nick = split[4];
			String realhost = split[5];
			String vhost = split[6];
			String ident = split[7];
			String ipaddress = split[8];
			long signedOn = Long.parseLong(split[9]);
			if (split[11].startsWith(":"))
				split[11] = split[11].substring(1);
			String realname = join(split, " ", 11);
			boolean isRegistered = split[10].contains("r");
			boolean isOper = split[10].contains("o");
			IRCUser ircuser = new IRCUser(nick, realname, ident, realhost,
					vhost, ipaddress, "", "", isRegistered, false, "",
					signedOn, idleTime, "");
			ircuser.isRegistered = isRegistered;
			ircuser.isOper = isOper;
			uid2ircuser.put(UID, ircuser); // Add it to the hashmap
		} else if (split[1].equalsIgnoreCase("AWAY")) {
			// Away status updating
			// :0IJAAAAAE AWAY :Auto Away at Tue Nov 22 13:56:26 2011
			String UID = split[0];

			IRCUser iuser;
			if ((iuser = uid2ircuser.get(UID)) != null) {
				// Found the UID in the hashmap, update away message
				if (split.length > 2) {
					// New away message
					if (split[2].startsWith(":"))
						split[2] = split[2].substring(1);
					iuser.awayMsg = IRCd.join(split, " ", 2);
				} else {
					// Remove away status
					iuser.awayMsg = "";
				}
			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + UID
							+ " not found in list. Error code IRCd1707."); // Log
				}
			}


		} else if (split[1].equalsIgnoreCase("TIME")) {
			// TIME request from user
			// :123AAAAAA TIME :test.tempcraft.net
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			IRCUser iuser;
			if (split[2].equalsIgnoreCase(Config.getIrcdServerHostName())) { // Double check to
				// make sure
				// this request
				// is for us
				if ((iuser = uid2ircuser.get(split[0])) != null) {
					println(pre + "PUSH " + split[0] + " ::" + Config.getIrcdServerHostName()
							+ " 391 " + iuser.nick + " " + Config.getIrcdServerHostName()
							+ " :"
							+ dateFormat.format(System.currentTimeMillis()));
				}
			}
		} else if (split[1].equalsIgnoreCase("ENDBURST")) {
			// :280 ENDBURST
			if (split[0].equalsIgnoreCase(remoteSID)
					|| split[0].equalsIgnoreCase(Config.getLinkName())) {
				sendLinkBurst();
			}
		} else if (split[1].equalsIgnoreCase("SERVER")) {
			// :dev.tempcraft.net SERVER Esper.janus * 1 0JJ Esper
			String hub;
			try {
				if (split[0].equalsIgnoreCase(remoteSID)
						|| split[0].equalsIgnoreCase(Config.getLinkName())) {
					hub = remoteSID;
				} else {
					hub = split[0];
					IRCServer is = servers.get(hub);
					if (is == null) {
						Iterator<Entry<String, IRCServer>> iter = servers
								.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry<String, IRCServer> entry = iter.next();
							entry.getKey();
							IRCServer curServer = entry.getValue();
							if (curServer.host.equalsIgnoreCase(split[0])) {
								is = curServer;
								break;
							}
						}
					}
					if (is != null)
						is.leaves.add(split[5]);
					else
						BukkitIRCdPlugin.log
								.severe("[BukkitIRCd] Received invalid SERVER command, unknown hub server!");
				}
			} catch (NumberFormatException e) {
				hub = remoteSID;
				BukkitIRCdPlugin.log
						.severe("[BukkitIRCd] Received invalid SERVER command, unknown hub server!");
			}
			servers.put(split[5], new IRCServer(split[2], split[6], split[5],
					hub));
		} else if (split[1].equalsIgnoreCase("SQUIT")) {
			// :test.tempcraft.net SQUIT dev.tempcraft.net :Remote host closed
			// connection
			String quitServer = split[2];
			if (quitServer.equalsIgnoreCase(Config.getLinkName())
					|| quitServer.equalsIgnoreCase(remoteSID))
				disconnectServer("Remote server delinked");
			else {
				Iterator<Entry<String, IRCServer>> iter = servers.entrySet()
						.iterator();
				IRCServer is = null;
				while (iter.hasNext()) {
					Map.Entry<String, IRCServer> entry = iter.next();
					is = entry.getValue();
					if (is.host.equalsIgnoreCase(quitServer)
							|| is.SID.equalsIgnoreCase(quitServer)) {
						// Found the server in the list
						removeIRCUsersBySID(is.SID);
						break;
					}
				}
			}
		} else if (split[1].equalsIgnoreCase("OPERTYPE")) {
			// :123AAAAAA OPERTYPE IRC_Operator
			IRCUser ircuser;
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				ircuser.isOper = true;
			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
							+ " not found in list. Error code IRCd1779."); // Log as
				}
			}


		}

		else if (split[1].equalsIgnoreCase("MODE")) {
			IRCUser ircusertarget;
			if (split[3].startsWith(":"))
				split[3] = split[3].substring(1);

			if ((ircusertarget = uid2ircuser.get(split[2])) != null) {
				String modes = split[3];
				boolean add = true;
				for (int i = 0; i < modes.length(); i++) {
					if ((modes.charAt(i) + "").equals("+")) {
						add = true;
					} else if ((modes.charAt(i) + "").equals("-")) {
						add = false;
					} else if ((modes.charAt(i) + "").equals("o")) {
						if (add) {
							ircusertarget.isOper = true;
						} else {
							ircusertarget.isOper = false;
						}
					} else if ((modes.charAt(i) + "").equals("r")) {
						if (add) {
							ircusertarget.isRegistered = true;
						} else {
							ircusertarget.isRegistered = false;
						}
					}
				}
			} else {
				if (Config.isDebugModeEnabled()) {
					// Log as severe because this situation should never occur and
					// points to a bug in the code
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/Config.getLinkServerID() " + split[0]
							+ " not found in list. Error code IRCd1806.");
				}

			}
		} else if (split[1].equalsIgnoreCase("FJOIN")) {
			// :dev.tempcraft.net FJOIN #tempcraft.staff 1321829730 +tnsk
			// MASTER-RACE :qa,0AJAAAAAA o,0IJAAAAAP v,0IJAAAAAQ
			if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
				try {
					long tmp = Long.parseLong(split[3]);
					if (channelTS > tmp)
						channelTS = tmp;
				} catch (NumberFormatException e) {
				}
				// Main channel
				String users[] = command.split(" ");
				for (String user : users) {
					if (!user.contains(","))
						continue;
					String usersplit[] = user.split(",");
					IRCUser ircuser;
					if ((ircuser = uid2ircuser.get(usersplit[1])) != null) {
						ircuser.setModes(usersplit[0]);
						if ((IRCd.isPlugin)
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if (!ircuser.joined) {

								if (msgIRCJoin.length() > 0)
									//TODO I believe fix for #45 would go here
									IRCd.broadcastMessage(
													msgIRCJoin
															.replace(
																	"{User}",
																	ircuser.nick)
															.replace(
																	"{Prefix}",
																	IRCd.getGroupPrefix(ircuser
																			.getTextModes()))
															.replace(
																	"{Suffix}",
																	IRCd.getGroupSuffix(ircuser
																			.getTextModes())));
								if ((BukkitIRCdPlugin.dynmap != null)
										&& (msgIRCJoinDynmap.length() > 0))
									BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
											"IRC", msgIRCJoinDynmap.replace(
													"{User}", ircuser.nick));
							}
						}
						ircuser.joined = true;
					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID "
									+ usersplit[1]
									+ " not found in list. Error code IRCd1831."); // Log
						}
					}

				}
			} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
				try {
					long tmp = Long.parseLong(split[3]);
					if (consoleChannelTS > tmp)
						consoleChannelTS = tmp;
				} catch (NumberFormatException e) {
				}
				// Console channel
				String users[] = command.split(" ");
				for (String user : users) {
					if (!user.contains(","))
						continue;
					String usersplit[] = user.split(",");
					IRCUser ircuser;
					if ((ircuser = uid2ircuser.get(usersplit[1])) != null) {
						ircuser.setConsoleModes(usersplit[0]);
						ircuser.consoleJoined = true;
					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID "
									+ usersplit[1]
									+ " not found in list. Error code IRCd1849."); // Log
						}
					}

				}
			}
			// Ignore other channels, since this plugin only cares about the
			// main channel and console channel.
		} else if (split[1].equalsIgnoreCase("FHOST")) {
			// :0KJAAAAAA FHOST test
			IRCUser ircuser;
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				ircuser.hostmask = split[2];
			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
							+ " not found in list. Error code IRCd1861."); // Log as
				}
			}


		} else if (split[1].equalsIgnoreCase("FNAME")) {
			// :0KJAAAAAA FNAME TEST
			IRCUser ircuser;
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				ircuser.realname = join(split, " ", 2);
			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
							+ " not found in list. Error code IRCd1870."); // Log as
				}
			}


		} else if (split[1].equalsIgnoreCase("FMODE")) {
			// :0KJAAAAAA FMODE #tempcraft.staff 1320330110 +o 0KJAAAAAB
			IRCUser ircuser, ircusertarget;
			if (split.length >= 6) { // If it's not length 6, it's not a user
				// mode
				if (split[0].startsWith(":"))
					split[0] = split[0].substring(1);

				if (split[2].equalsIgnoreCase(Config.getIrcdChannel()))
					try {
						long tmp = Long.parseLong(split[3]);
						if (channelTS > tmp)
							channelTS = tmp;
					} catch (NumberFormatException e) {
					}
				else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel()))
					try {
						long tmp = Long.parseLong(split[3]);
						if (consoleChannelTS > tmp)
							consoleChannelTS = tmp;
					} catch (NumberFormatException e) {
					}

				Boolean add = true;
				int modecount = 0;
				for (int i = 0; i < split[4].length(); i++) {
					if (5 + modecount >= split.length)
						break;
					String user = split[5 + modecount];
					if (user.startsWith(":"))
						user = user.substring(1);
					String mode = split[4].charAt(i) + "";
					if (mode.equals("+"))
						add = true;
					else if (mode.equals("-"))
						add = false;
					else {
						if ((ircusertarget = uid2ircuser.get(user)) != null) {
							if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
								String textModes = ircusertarget.getTextModes();
								if (add) {
									System.out.println("Adding mode " + mode
											+ " for " + ircusertarget.nick);
									if (!textModes.contains(mode))
										ircusertarget
												.setModes(textModes + mode);
								} else {
									System.out.println("Removing mode " + mode
											+ " for " + ircusertarget.nick);
									if (textModes.contains(mode))
										ircusertarget.setModes(textModes
												.replace(mode, ""));
								}
							} else if (split[2]
									.equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
								String consoleTextModes = ircusertarget
										.getConsoleTextModes();
								if (add) {
									System.out.println("Adding console mode "
											+ mode + " for "
											+ ircusertarget.nick);
									if (!consoleTextModes.contains(mode))
										ircusertarget
												.setConsoleModes(consoleTextModes
														+ mode);
								} else {
									System.out.println("Removing console mode "
											+ mode + " for "
											+ ircusertarget.nick);
									if (consoleTextModes.contains(mode))
										ircusertarget
												.setConsoleModes(consoleTextModes
														.replace(mode, ""));
								}
							}
						} else if (IRCd.wildCardMatch(user, "*!*@*")) {
							if (mode.equals("b")) {
								if ((ircuser = uid2ircuser.get(split[0])) != null) {
									if (add) {
										if (msgIRCBan.length() > 0)
											IRCd.broadcastMessage(
															msgIRCBan
																	.replace(
																			"{BannedUser}",
																			user)
																	.replace(
																			"{BannedBy}",
																			ircuser.nick));
										if ((BukkitIRCdPlugin.dynmap != null)
												&& (msgIRCBanDynmap.length() > 0))
											BukkitIRCdPlugin.dynmap
													.sendBroadcastToWeb(
															"IRC",
															msgIRCBanDynmap
																	.replace(
																			"{BannedUser}",
																			user)
																	.replace(
																			"{BannedBy}",
																			ircuser.nick));
									} else {
										if (msgIRCUnban.length() > 0)
											IRCd.broadcastMessage(
															msgIRCUnban
																	.replace(
																			"{BannedUser}",
																			user)
																	.replace(
																			"{BannedBy}",
																			ircuser.nick));
										if ((BukkitIRCdPlugin.dynmap != null)
												&& (msgIRCUnbanDynmap.length() > 0))
											BukkitIRCdPlugin.dynmap
													.sendBroadcastToWeb(
															"IRC",
															msgIRCUnbanDynmap
																	.replace(
																			"{BannedUser}",
																			user)
																	.replace(
																			"{BannedBy}",
																			ircuser.nick));
									}
								}
							}
						}
						modecount++;
					}
				}
			}
		} else if (split[1].equalsIgnoreCase("FTOPIC")) {
			// :dev.tempcraft.net FTOPIC #tempcraft.survival 1322061484
			// Jdbye/ingame '4HI'"
			if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
				// Main channel
				String user = split[4];
				if (split[5].startsWith(":"))
					split[5] = split[5].substring(1);
				String topic = join(split, " ", 5);

				channelTopic = topic;
				try {
					channelTopicSetDate = Long.parseLong(split[3]);
				} catch (NumberFormatException e) {
				}
				channelTopicSet = user;
				if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
					Config.setIrcdTopic(topic);
					Config.setIrcdTopicSetDate(channelTopicSetDate * 1000);
					Config.setIrcdTopicSetBy(user);
				}
			} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
				// This is of no interest to us
			}
			// Ignore other channels, since this plugin only cares about the
			// main channel and console channel.
		} else if (split[1].equalsIgnoreCase("TOPIC")) {
			// :0KJAAAAAA TOPIC #tempcraft.survival :7Welcome to
			// #tempcraft.survival! | 10Server's 3ONLINE | 3Visit our site:
			// 14http://TempCraft.net/ | 4Vote for us:
			// 14http://tempcraft.net/?act=vote | 4Join our forums:
			// 14http://stormbit.net/ | Don't change the separators to white
			if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
				// Main channel
				String UID = split[0];
				if (split[3].startsWith(":"))
					split[3] = split[3].substring(1);
				String topic = join(split, " ", 3);

				IRCUser ircuser = null;
				IRCServer server = null;
				if (((ircuser = uid2ircuser.get(UID)) != null)
						|| ((server = servers.get(UID)) != null)) {
					String user;
					if (ircuser != null)
						user = ircuser.nick;
					else
						user = server.host;
					channelTopic = topic;
					channelTopicSetDate = System.currentTimeMillis() / 1000L;
					channelTopicSet = user;
					if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
						Config.setIrcdTopic(topic);
						Config.setIrcdTopicSetDate(channelTopicSetDate * 1000);
						Config.setIrcdTopicSetBy(user);
					}
				} else {
					if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/Config.getLinkServerID() " + UID
							+ " not found in list. Error code IRCd1985."); // Log
					}
				}
				// as
				// severe
				// because
				// this
				// situation
				// should
				// never
				// occur
				// and
				// points
				// to
				// a
				// bug
				// in
				// the
				// code
			} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
				// This is of no interest to us
			}
			// Ignore other channels, since this plugin only cares about the
			// main channel and console channel.
		} else if (split[1].equalsIgnoreCase("IDLE")) {
			// IN :<uuid> IDLE <target uuid>
			// OUT :<uuid> IDLE <target uuid> <signon> <seconds idle>
			final BukkitPlayer bp;
			long idletime = 0;
			long signedOn = 0;
			final String source = split[0];
			final String target = split[2];
			final boolean success;
			if (target.equalsIgnoreCase(serverUID)) {
				signedOn = serverStartTime;
				idletime = 0;
				success = true;
			} else if ((bp = getBukkitUserByUID(target)) != null) {
				idletime = (System.currentTimeMillis() - bp.idleTime) / 1000L;
				signedOn = bp.signedOn;
				success = true;
			}
			// The error below can/will happen in the event a player is /whois'ed from IRC - I'd like to know why and how to fix it
			else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + target + " not found in list. Error code IRCd1999."); // Log as severe because this situation should never occur and points to a bug in the code
				}
				success = false;
			}
			if (success) {
				println(":" + target + " IDLE " + source + " " + signedOn + " " + idletime);
			}
		} else if (split[1].equalsIgnoreCase("NICK")) {
			// :280AAAAAA NICK test 1321981244
			IRCUser ircuser;
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				BukkitIRCdPlugin.thePlugin.updateLastReceived(ircuser.nick,
						split[2]);
				if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)
						&& (ircuser.joined)) {
					if (msgIRCNickChange.length() > 0)
						IRCd.broadcastMessage(
										msgIRCNickChange
												.replace("{OldNick}",
														ircuser.nick)
												.replace(
														"{Prefix}",
														IRCd.getGroupPrefix(ircuser
																.getTextModes()))
												.replace(
														"{Suffix}",
														IRCd.getGroupSuffix(ircuser
																.getTextModes()))
												.replace("{NewNick}", split[2]));
					if ((BukkitIRCdPlugin.dynmap != null)
							&& (msgIRCNickChangeDynmap.length() > 0))
						BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
								"IRC",
								msgIRCNickChangeDynmap.replace("{OldNick}",
										ircuser.nick).replace("{NewNick}",
										split[2]));
				}
				ircuser.nick = split[2];
				ircuser.isRegistered = false;
			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[2]
							+ " not found in list. Error code IRCd2013."); // Log as severe, points to a bug in the code
				}
			}

		} else if (split[1].equalsIgnoreCase("KICK")) {
			// :280AAAAAA KICK #tempcraft.survival 280AAAAAB :reason
			IRCUser ircuser;
			IRCUser ircvictim;
			IRCServer server = null;
			String kicker, kicked;
			String reason;
			if (split.length > 4) {
				reason = join(split, " ", 4);
				if (reason.startsWith(":"))
					reason = reason.substring(1);
			} else
				reason = null;

			if (split[3].startsWith(Integer.toString(Config.getLinkServerID()))) {
				if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
					if (split[3].equalsIgnoreCase(serverUID)) {
						println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS
								+ " +nt :," + serverUID);
						println(":" + serverUID + " FMODE " + Config.getIrcdChannel() + " "
								+ channelTS + " +qaohv " + serverUID + " "
								+ serverUID + " " + serverUID + " " + serverUID
								+ " " + serverUID);

					} else if (((ircuser = uid2ircuser.get(split[0])) != null)
							|| ((server = servers.get(split[0])) != null)) {
						String user;
						if (ircuser != null)
							user = ircuser.nick;
						else
							user = server.host;

						BukkitPlayer bp;
						if ((bp = getBukkitUserByUID(split[3])) != null) {
							if ((IRCd.isPlugin)
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								kickPlayerIngame(user, bp.nick, reason);
								removeBukkitUserByUID(split[3]);
							}
						} else {
							if (Config.isDebugModeEnabled()) {
								BukkitIRCdPlugin.log
								.severe("[BukkitIRCd] Bukkit Player UID "
										+ split[3]
										+ " not found in list. Error code IRCd2051."); // Log
							}
						}

					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/Config.getLinkServerID() "
									+ split[0]
									+ " not found in list. Error code IRCd2053."); // Log
						}
					}

				} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
					if (split[3].equalsIgnoreCase(serverUID)) {
						println(pre + "FJOIN " + Config.getIrcdConsoleChannel() + " "
								+ consoleChannelTS + " +nt :," + serverUID);
						println(":" + serverUID + " FMODE "
								+ Config.getIrcdConsoleChannel() + " " + consoleChannelTS
								+ " +qaohv " + serverUID + " " + serverUID
								+ " " + serverUID + " " + serverUID + " "
								+ serverUID);

					}
				}
			} else {
				if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
					// Main channel
					if (((ircuser = uid2ircuser.get(split[0])) != null)
							|| ((server = servers.get(split[0])) != null)) {
						if (ircuser != null)
							kicker = ircuser.nick;

						else
							kicker = server.host;
						String modes = "q";
						if (ircuser != null)
							modes = ircuser.getTextModes();
						if ((ircvictim = uid2ircuser.get(split[3])) != null) {
							kicked = ircvictim.nick;
							if ((IRCd.isPlugin)
									&& (BukkitIRCdPlugin.thePlugin != null)) {
								if (reason != null) {
									if (msgIRCKickReason.length() > 0)
										IRCd.broadcastMessage(
														msgIRCKickReason
																.replace(
																		"{KickedUser}",
																		kicked)
																.replace(
																		"{KickedBy}",
																		kicker)
																.replace(
																		"{Reason}",
																		convertColors(
																				reason,
																				true))
																.replace(
																		"{KickedPrefix}",
																		IRCd.getGroupPrefix(ircvictim
																				.getTextModes()))
																.replace(
																		"{KickedSuffix}",
																		IRCd.getGroupSuffix(ircvictim
																				.getTextModes()))
																.replace(
																		"{KickerPrefix}",
																		IRCd.getGroupPrefix(modes))
																.replace(
																		"{KickerSuffix}",
																		IRCd.getGroupSuffix(modes)));
									if ((BukkitIRCdPlugin.dynmap != null)
											&& (msgIRCKickReasonDynmap.length() > 0))
										BukkitIRCdPlugin.dynmap
												.sendBroadcastToWeb(
														"IRC",
														msgIRCKickReasonDynmap
																.replace(
																		"{KickedUser}",
																		kicked)
																.replace(
																		"{KickedBy}",
																		kicker)
																.replace(
																		"{Reason}",
																		stripIRCFormatting(reason))
																.replace(
																		"{KickedPrefix}",
																		IRCd.getGroupPrefix(ircvictim
																				.getTextModes()))
																.replace(
																		"{KickedSuffix}",
																		IRCd.getGroupSuffix(ircvictim
																				.getTextModes()))
																.replace(
																		"{KickerPrefix}",
																		IRCd.getGroupPrefix(modes))
																.replace(
																		"{KickerSuffix}",
																		IRCd.getGroupSuffix(modes)));
								} else {
									if (msgIRCKick.length() > 0)
										IRCd.broadcastMessage(
														msgIRCKick
																.replace(
																		"{KickedUser}",
																		kicked)
																.replace(
																		"{KickedBy}",
																		kicker)
																.replace(
																		"{KickedPrefix}",
																		IRCd.getGroupPrefix(ircvictim
																				.getTextModes()))
																.replace(
																		"{KickedSuffix}",
																		IRCd.getGroupSuffix(ircvictim
																				.getTextModes()))
																.replace(
																		"{KickerPrefix}",
																		IRCd.getGroupPrefix(modes))
																.replace(
																		"{KickerSuffix}",
																		IRCd.getGroupSuffix(modes)));
									if ((BukkitIRCdPlugin.dynmap != null)
											&& (msgIRCKickDynmap.length() > 0))
										BukkitIRCdPlugin.dynmap
												.sendBroadcastToWeb(
														"IRC",
														msgIRCKickDynmap
																.replace(
																		"{KickedUser}",
																		kicked)
																.replace(
																		"{KickedBy}",
																		kicker));
								}
								ircvictim.joined = false;
							}
						} else {
							if (Config.isDebugModeEnabled()) {
								BukkitIRCdPlugin.log
								.severe("[BukkitIRCd] UID "
										+ split[3]
										+ " not found in list. Error code IRCd2083."); // Log
							}
						}

					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/Config.getLinkServerID() "
									+ split[0]
									+ " not found in list. Error code IRCd2085."); // Log
						}
					}


				} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
					// Console channel
					// Only thing important here is to set consolemodes to blank
					// so they can't execute commands on the console channel
					// anymore
					if ((ircvictim = uid2ircuser.get(split[3])) != null) {
						ircvictim.setConsoleModes("");
						ircvictim.consoleJoined = false;
					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID "
									+ split[3]
									+ " not found in list. Error code IRCd2094."); // Log
						}
					}

				}
			}
		} else if (split[1].equalsIgnoreCase("PART")) {
			// :280AAAAAA PART #tempcraft.survival :message
			IRCUser ircuser;
			String reason;
			if (split.length > 3) {
				reason = join(split, " ", 3);
				if (reason.startsWith(":"))
					reason = reason.substring(1);
			} else
				reason = null;

			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
				// Main channel
				if ((ircuser = uid2ircuser.get(split[0])) != null) {
					if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
						if (reason != null) {

							if (msgIRCLeaveReason.length() > 0)
								IRCd.broadcastMessage(
												msgIRCLeaveReason
														.replace("{User}",
																ircuser.nick)
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(ircuser
																		.getTextModes()))
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(ircuser
																		.getTextModes()))
														.replace(
																"{Reason}",
																convertColors(
																		reason,
																		true)));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCLeaveReasonDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap
										.sendBroadcastToWeb(
												"IRC",
												msgIRCLeaveReasonDynmap
														.replace("{User}",
																ircuser.nick)
														.replace(
																"{Reason}",
																stripIRCFormatting(reason)));
						} else {

							if (msgIRCLeave.length() > 0)
								IRCd.broadcastMessage(
												msgIRCLeave
														.replace("{User}",
																ircuser.nick)
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(ircuser
																		.getTextModes()))
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(ircuser
																		.getTextModes())));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCLeaveDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
										"IRC", msgIRCLeaveDynmap.replace(
												"{User}", ircuser.nick));
						}
						ircuser.joined = false;
						ircuser.setModes("");
					}
				} else {
					if (Config.isDebugModeEnabled()) {
						BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
								+ " not found in list. Error code IRCd2125."); // Log
					}
				}

			} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
				// Console channel
				// Only thing important here is to set oper to false so they
				// can't execute commands on the console channel without being
				// in it
				if ((ircuser = uid2ircuser.get(split[0])) != null) {
					ircuser.setConsoleModes("");
					ircuser.consoleJoined = false;
				} else {
					if (Config.isDebugModeEnabled()) {
						BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
								+ " not found in list. Error code IRCd2134."); // Log
					}
				}


			}
		} else if (split[1].equalsIgnoreCase("QUIT")) {
			// :280AAAAAB QUIT :Quit: Connection reset by beer
			IRCUser ircuser;
			String reason;
			if (split.length > 2) {
				reason = join(split, " ", 2);
				if (reason.startsWith(":"))
					reason = reason.substring(1);
			} else
				reason = null;
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				if (ircuser.joined) {
					// This user is on the plugin channel so broadcast the PART
					// ingame
					if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
						if (reason != null) {

							if (msgIRCLeaveReason.length() > 0)
								IRCd.broadcastMessage(
												msgIRCLeaveReason
														.replace("{User}",
																ircuser.nick)
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(ircuser
																		.getTextModes()))
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(ircuser
																		.getTextModes()))
														.replace(
																"{Reason}",
																convertColors(
																		reason,
																		true)));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCLeaveReasonDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap
										.sendBroadcastToWeb(
												"IRC",
												msgIRCLeaveReasonDynmap
														.replace("{User}",
																ircuser.nick)
														.replace(
																"{Reason}",
																stripIRCFormatting(reason)));
						} else {

							if (msgIRCLeave.length() > 0)
								IRCd.broadcastMessage(
												msgIRCLeave
														.replace("{User}",
																ircuser.nick)
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(ircuser
																		.getTextModes()))
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(ircuser
																		.getTextModes())));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCLeaveDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
										"IRC", msgIRCLeaveDynmap.replace(
												"{User}", ircuser.nick));
						}
					}
					ircuser.setConsoleModes("");
					ircuser.setModes("");
					ircuser.joined = false;
					ircuser.consoleJoined = false;
				}
				uid2ircuser.remove(split[0]);
			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
							+ " not found in list. Error code IRCd2166."); // Log
				}
			}


		} else if (split[1].equalsIgnoreCase("KILL")) {
			// :280AAAAAA KILL 123AAAAAA :Killed (test (testng))

			// If an ingame user is killed, reconnect them to IRC.
			IRCUser ircuser, ircuser2;
			IRCServer server = null;
			String user;
			if ((((ircuser = uid2ircuser.get(split[0])) != null))
					|| ((server = servers.get(split[0])) != null)) {
				if (ircuser != null)
					user = ircuser.nick;
				else
					user = server.host;
				synchronized (csBukkitPlayers) {
					BukkitPlayer bp;
					if (split[2].equalsIgnoreCase(serverUID)) {
						println(pre + "UID " + serverUID + " "
								+ serverStartTime + " " + Config.getIrcdServerName() + " "
								+ Config.getIrcdServerHostName() + " " + Config.getIrcdServerHostName() + " "
								+ Config.getIrcdServerName() + " 127.0.0.1 " + serverStartTime
								+ " +Bro :" + BukkitIRCdPlugin.ircdVersion);
						println(":" + serverUID + " OPERTYPE Network_Service");
						println(pre + "FJOIN " + Config.getIrcdConsoleChannel() + " "
								+ consoleChannelTS + " +nt :," + serverUID);
						println(":" + serverUID + " FMODE "
								+ Config.getIrcdConsoleChannel() + " " + consoleChannelTS
								+ " +qaohv " + serverUID + " " + serverUID
								+ " " + serverUID + " " + serverUID + " "
								+ serverUID);
						println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS
								+ " +nt :," + serverUID);
						println(":" + serverUID + " FMODE " + Config.getIrcdChannel() + " "
								+ channelTS + " +qaohv " + serverUID + " "
								+ serverUID + " " + serverUID + " " + serverUID
								+ " " + serverUID);
					} else if ((bp = getBukkitUserByUID(split[2])) != null) {
						String UID = bp.getUID();
						String textMode = bp.getTextMode();
						if (bp.hasPermission("bukkitircd.oper")) {
							println(pre + "UID " + UID + " "
									+ (bp.idleTime / 1000L) + " " + bp.nick
									+ Config.getIrcdIngameSuffix() + " " + bp.realhost + " "
									+ bp.host + " " + bp.nick + " " + bp.ip
									+ " " + bp.signedOn
									+ " +or :Minecraft Player");
							println(":" + UID + " OPERTYPE IRC_Operator");
						} else
							println(pre + "UID " + UID + " "
									+ (bp.idleTime / 1000L) + " " + bp.nick
									+ Config.getIrcdIngameSuffix() + " " + bp.realhost + " "
									+ bp.host + " " + bp.nick + " " + bp.ip
									+ " " + bp.signedOn
									+ " +r :Minecraft Player");

						println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS
								+ " +nt :," + UID);
						if (textMode.length() > 0) {
							String modestr = "";
							for (int i = 0; i < textMode.length(); i++) {
								modestr += UID + " ";
							}
							modestr = modestr
									.substring(0, modestr.length() - 1);
							println(":" + serverUID + " FMODE " + Config.getIrcdChannel()
									+ " " + channelTS + " + " + textMode + " "
									+ modestr);
						}
						String world = bp.getWorld();
						if (world != null)
							println(pre + "METADATA " + UID
									+ " swhois :is currently in " + world);
						else
							println(pre
									+ "METADATA "
									+ UID
									+ " swhois :is currently in an unknown world");
					} else if ((ircuser2 = uid2ircuser.get(split[2])) != null) {
						String reason;
						reason = join(split, " ", 3);
						if (reason.startsWith(":"))
							reason = reason.substring(1);
						if (ircuser2.joined) {

							if (msgIRCLeaveReason.length() > 0)
								IRCd.broadcastMessage(
												msgIRCLeaveReason
														.replace("{User}", user)
														.replace(
																"{Suffix}",
																IRCd.getGroupSuffix(ircuser
																		.getTextModes()))
														.replace(
																"{Prefix}",
																IRCd.getGroupPrefix(ircuser
																		.getTextModes()))
														.replace(
																"{Reason}",
																convertColors(
																		reason,
																		true)));
							if ((BukkitIRCdPlugin.dynmap != null)
									&& (msgIRCLeaveReasonDynmap.length() > 0))
								BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
										"IRC",
										msgIRCLeaveReasonDynmap.replace(
												"{User}", user).replace(
												"{Reason}",
												stripIRCFormatting(reason)));
							ircuser2.setConsoleModes("");
							ircuser2.setModes("");
							ircuser2.joined = false;
							ircuser2.consoleJoined = false;
						}
						uid2ircuser.remove(split[2]);
					} else {
						if (Config.isDebugModeEnabled()) {
							BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID "
									+ split[2]
									+ " not found in list. Error code IRCd2224."); // Log
						}
					}

				}

			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/Config.getLinkServerID() " + split[0]
							+ " not found in list. Error code IRCd2228."); // Log
				}
			}


		} else if (split[1].equalsIgnoreCase("PRIVMSG")
				|| split[1].equalsIgnoreCase("NOTICE")) {
			// :280AAAAAA PRIVMSG 123AAAAAA :test
			if (split[3].startsWith(":"))
				split[3] = split[3].substring(1);
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			String message = join(split, " ", 3);
			String msgtemplate = "";
			String msgtemplatedynmap = "";
			boolean isCTCP = (message.startsWith((char) 1 + "") && message
					.endsWith((char) 1 + ""));
			boolean isAction = (message.startsWith((char) 1 + "ACTION") && message
					.endsWith((char) 1 + ""));
			boolean isNotice = split[1].equalsIgnoreCase("NOTICE");
			if (isCTCP && (!isAction))
				return; // Ignore CTCP's (except actions)
			else if (isCTCP && isNotice)
				return; // CTCP reply, ignore this

			if (isNotice && (!Config.isIrcdNoticesEnabled()))
				return; // Ignore notices if notices are disabled.

			IRCUser ircuser;
			String uidfrom = split[0];
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				synchronized (csBukkitPlayers) {
					BukkitPlayer bp;
					if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) { // Messaging
						// the
						// public
						// channel
						if (isAction) {
							msgtemplate = msgIRCAction;
							msgtemplatedynmap = msgIRCActionDynmap;
							message = IRCd.join(
									message.substring(1, message.length() - 1)
											.split(" "), " ", 1);
						} else if (isNotice) {
							msgtemplate = msgIRCNotice;
							msgtemplatedynmap = msgIRCNoticeDynmap;
						} else {
							msgtemplate = msgIRCMessage;
							msgtemplatedynmap = msgIRCMessageDynmap;
						}
						if ((IRCd.isPlugin)
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if (message.equalsIgnoreCase("!players")
									&& (!isAction) && (!isNotice)) {
								if (msgPlayerList.length() > 0) {
									String s = "";
									int count = 0;
									for (BukkitPlayer player : bukkitPlayers) {
										count++;
										s = s + player.nick + ", ";
									}
									if (s.length() == 0)
										s = "None, ";
									println(":"
											+ serverUID
											+ " PRIVMSG "
											+ Config.getIrcdChannel()
											+ " :"
											+ convertColors(
													msgPlayerList
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
							} else {

								if (msgtemplate.length() > 0){
									String msg = msgtemplate
											.replace(
													"{User}",
													ircuser.nick)
											.replace(
													"{Suffix}",
													IRCd.getGroupSuffix(ircuser
															.getTextModes()))
											.replace(
													"{Prefix}",
													IRCd.getGroupPrefix(ircuser
															.getTextModes()))
											// TODO Player Highlight
											// .replace(
											// ,
											// "&b" + "&r")
											.replace(
													"{Message}",
													IRCd.convertColors(
															message,
															true));
							if(Config.isIrcdIngameSuffixStripEnabled()){
								msg = msg.replace(Config.getIrcdIngameSuffix(),"");
							}

									IRCd.broadcastMessage(msg
													);}
								if ((BukkitIRCdPlugin.dynmap != null)
										&& (msgtemplatedynmap.length() > 0))
									BukkitIRCdPlugin.dynmap
											.sendBroadcastToWeb(
													"IRC",
													msgtemplatedynmap
															.replace(
																	"{User}",
																	ircuser.nick)
															.replace(
																	"{Message}",
																	stripIRCFormatting(message)));
							}
						}
					} else if (split[2].equalsIgnoreCase(Config.getIrcdConsoleChannel())) { // Messaging
						// the
						// console
						// channel
						if (message.startsWith("!") && (!isAction)
								&& (!isNotice)) {
							if (!ircuser.getConsoleTextModes().contains("o"))
								println(":"
										+ serverUID
										+ " NOTICE "
										+ uidfrom
										+ " :You are not a channel operator (or above). Command failed."); // Only
							// let
							// them
							// execute
							// commands
							// if
							// they're
							// oper
							else {
								message = message.substring(1);
								executeCommand(message);
							}
						}
					} else if (split[2].equalsIgnoreCase(serverUID)) { // Messaging
						// the
						// console
						if ((IRCd.isPlugin)
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if (isAction) {
								msgtemplate = msgIRCPrivateAction;
								message = IRCd.join(
										message.substring(1,
												message.length() - 1)
												.split(" "), " ", 1);
							} else if (isNotice) {
								msgtemplate = msgIRCPrivateNotice;
							} else {
								msgtemplate = msgIRCPrivateMessage;
							}

							BukkitIRCdPlugin.thePlugin.setLastReceived(
									"@CONSOLE@", ircuser.nick);

							if (msgtemplate.length() > 0)
								BukkitIRCdPlugin.log.info(msgtemplate
										.replace("{User}", ircuser.nick)
										.replace(
												"{Suffix}",
												IRCd.getGroupSuffix(ircuser
														.getTextModes()))
										.replace(
												"{Prefix}",
												IRCd.getGroupPrefix(ircuser
														.getTextModes()))
										.replace(
												"{Message}",
												IRCd.convertColors(message,
														true)));
						}
					} else if ((bp = getBukkitUserByUID(split[2])) != null) { // Messaging
						// an
						// ingame
						// user
						if ((isAction || (!isCTCP)) && (IRCd.isPlugin)
								&& (BukkitIRCdPlugin.thePlugin != null)) {
							if (isAction) {
								msgtemplate = msgIRCPrivateAction;
								message = IRCd.join(
										message.substring(1,
												message.length() - 1)
												.split(" "), " ", 1);
							} else if (isNotice) {
								msgtemplate = msgIRCPrivateNotice;
							} else {
								msgtemplate = msgIRCPrivateMessage;
							}

							synchronized (IRCd.csBukkitPlayers) {
								String bukkitnick = bp.nick;
								BukkitIRCdPlugin.thePlugin.setLastReceived(
										bukkitnick, ircuser.nick);

								if (msgtemplate.length() > 0)
									sendMessage(bukkitnick, msgtemplate
											.replace("{User}", ircuser.nick)
											.replace(
													"{Suffix}",
													IRCd.getGroupSuffix(ircuser
															.getTextModes()))
											.replace(
													"{Prefix}",
													IRCd.getGroupPrefix(ircuser
															.getTextModes()))
											.replace(
													"{Message}",
													IRCd.convertColors(message,
															true)));
							}
						}
					}
					// Ignore messages from other channels
				}

			} else {
				if (Config.isDebugModeEnabled()) {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0]
							+ " not found in list. Error code IRCd2336."); // Log
				}
			}


		} else if (split[1].equalsIgnoreCase("METADATA")) {
			// :00A METADATA 854AAAABZ accountname :glguy
			final String target = split[2];
			final String key = split[3];

			final String value;
			if (split[4].startsWith(":")) {
				split[4] = split[4].substring(1);
				value = join(split, " ", 4);
			} else {
				value = split[4];
			}

			final IRCUser user = uid2ircuser.get(target);
			if (user != null) {
				if (key.equalsIgnoreCase("accountname")) {
					user.accountname = value;
				}
			}
		}

		// End of IF command check
	}

    public static boolean isPlugin()
    {
        return isPlugin;
    }

    public static boolean isLinkcompleted()
    {
        return linkcompleted;
    }
}
