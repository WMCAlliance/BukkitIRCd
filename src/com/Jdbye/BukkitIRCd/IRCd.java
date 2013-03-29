package com.Jdbye.BukkitIRCd;

// BukkitIRCd by Jdbye and WMCAlliance
// A standalone IRC server plugin for Bukkit

// Last changes
// - Minecraft 1.4.6 compatible
// - Changed messages.yml to use & signs
// - Removed unused includes
// - Check our github

/* TODO:
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public class IRCd implements Runnable {
	
	
	// Universal settings
	public static boolean debugMode = false;
	public static String version = "BukkitIRCd by Jdbye edited by WMCAlliance";
	public static String serverName = "Minecraft"; 
	public static String serverDescription ="Minecraft BukkitIRCd Server"; 
	public static String serverHostName = "bukkitircd.localhost"; 
	public static String ingameSuffix = "-mc"; 
	public static String channelName = "#minecraft"; 
	public static String channelTopic = "Welcome to a Bukkit server!";
	public static String channelTopicSet = serverName;
	public static String consoleChannelName = "#minecraft-staff";
	public static String ircBanType = "ip";
	public static long channelTopicSetDate = System.currentTimeMillis() / 1000L;
	public static boolean enableNotices = true;
	public static boolean convertColorCodes = true;
	public String modestr = "standalone";
	public static Modes mode;
	
	// Standalone server settings
	public static int port = 6667;
	public static int maxConnections = 1000;
	public static int pingInterval = 60;
	public static int timeoutInterval = 180;
	public static int nickLen = 32;
	public static String operUser = "", operPass = "";
	public static String operModes = "@";
	
	// Link settings
	public static String remoteHost = "localhost";
	public static int remotePort = 7000;
	public static int localPort = 7000;
	public static boolean autoConnect = true;
	public static String linkName = "irc.localhost";
	public static String connectPassword = "test";
	public static String receivePassword = "test";
	public static int linkPingInterval = 60;
	public static int linkTimeoutInterval = 180;
	public static int linkDelay = 60;
	public static int SID = 111;
	
	// Custom messages
	public static String msgLinked = "&e[IRC] Linked to server %LINKNAME%";
	public static String msgDelinked = "&e[IRC] Split from server %LINKNAME%";
	public static String msgDelinkedReason = "&e[IRC] Split from server %LINKNAME% (%REASON%)";
	public static String msgIRCJoin = "&e[IRC] %USER% joined IRC";
	public static String msgIRCJoinDynmap = "%USER% joined IRC";
	public static String msgIRCLeave = "&e[IRC] %USER% left IRC";
	public static String msgIRCLeaveReason = "&e[IRC] %USER% left IRC (%REASON%)";
	public static String msgIRCLeaveDynmap = "%USER% left IRC";
	public static String msgIRCLeaveReasonDynmap = "%USER% left IRC (%REASON%)";
	public static String msgIRCKick = "&e[IRC] %KICKEDUSER% was kicked by %KICKEDBY%";
	public static String msgIRCKickReason = "&e[IRC] %KICKEDUSER% was kicked by %KICKEDBY% (%REASON%)";
	public static String msgIRCKickDynmap = "%KICKEDUSER% was kicked by %KICKEDBY%";
	public static String msgIRCKickReasonDynmap = "%KICKEDUSER% was kicked by %KICKEDBY% (%REASON%)";
	public static String msgIRCBan = "&e[IRC] %BANNEDUSER% was banned by %BANNEDBY%";
	public static String msgIRCBanDynmap = "%BANNEDUSER% was banned by %BANNEDBY%";
	public static String msgIRCUnban = "&e[IRC] %BANNEDUSER% was unbanned by %BANNEDBY%";
	public static String msgIRCUnbanDynmap = "%BANNEDUSER% was unbanned by %BANNEDBY%";
	public static String msgIRCNickChange = "&e[IRC] %OLDNICK% is now known as %NEWNICK%&f";
	public static String msgIRCNickChangeDynmap = "%OLDNICK% is now known as %NEWNICK%";
	public static String msgIRCAction = "[IRC] * &7%USER%&f %MESSAGE%";
	public static String msgIRCMessage = "[IRC] <&7%USER%&f> %MESSAGE%";
	public static String msgIRCNotice = "[IRC] -&7%USER%&f- %MESSAGE%";
	public static String msgIRCPrivateAction = "[IRC] &aTo you&f: * &7%USER%&f %MESSAGE%";
	public static String msgIRCPrivateMessage = "[IRC] &aTo you&f: <&7%USER%&f> %MESSAGE%";
	public static String msgIRCPrivateNotice = "[IRC] &aTo you&f: -&7%USER%&f- %MESSAGE%";
	public static String msgIRCActionDynmap = "* %USER% %MESSAGE%";
	public static String msgIRCMessageDynmap = "<%USER%> %MESSAGE%";
	public static String msgIRCNoticeDynmap = "-%USER%- %MESSAGE%";
	public static String msgDynmapMessage = "[Dynmap] %USER%: %MESSAGE%";
	public static String msgPlayerList = "^BOnline Players (%COUNT%):^B %USERS%";
	
	public static final long serverStartTime = System.currentTimeMillis() / 1000L;
	public static long channelTS = serverStartTime, consoleChannelTS = serverStartTime;
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
	public static boolean burstSent = false, capabSent = false;
	public static boolean lastconnected = false;
	public static boolean isIncoming = false;
	
	public static boolean isPlugin = false;

	private static Date curDate = new Date();
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
	public static String serverCreationDate = dateFormat.format(curDate);
	public static long serverCreationDateLong = System.currentTimeMillis() / 1000L;

	public static int[] ircColors     = { 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15};
	public static String[] gameColors = {"0","f","1","2","c","4","5","6","e","a","3","b","9","d","8","7"};

	private static List<ClientConnection> clientConnections = new LinkedList<ClientConnection>();
	public static List<BukkitPlayer> bukkitPlayers = new LinkedList<BukkitPlayer>();

	public static List<String> MOTD = new ArrayList<String>();
	public static List<IrcBan> ircBans = new ArrayList<IrcBan>();

	public boolean running = true;

	private long tickCount = System.currentTimeMillis();
	private static ServerSocket listener;
	private static Socket server = null;

	private static IRCCommandSender commandSender = null;
	private static Server bukkitServer = null;

	static class CriticalSection extends Object {
	}
	static public CriticalSection csStdOut = new CriticalSection();
	static public CriticalSection csBukkitPlayers = new CriticalSection();
	static public CriticalSection csIrcUsers = new CriticalSection();
	static public CriticalSection csIrcBans = new CriticalSection();
	static public CriticalSection csServer = new CriticalSection();
	
	public static BufferedReader in;
	public static PrintStream out;

	public IRCd()
	{
	}

	public void run() {
		while (running) {
			try {
				Class<?> c = Class.forName("org.bukkit.plugin.java.JavaPlugin");
				if (c != null) isPlugin = true;
			} catch (ClassNotFoundException e) { isPlugin = false; }

			try {
				if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
					bukkitServer = BukkitIRCdPlugin.thePlugin.getServer();
					commandSender = new IRCCommandSender(bukkitServer);
				}

				try { serverCreationDateLong = dateFormat.parse(serverCreationDate).getTime() / 1000L; }
				catch (ParseException e) { serverCreationDateLong = 0; }

				serverMessagePrefix = ":" + serverHostName;
				if (modestr.equalsIgnoreCase("unreal") || modestr.equalsIgnoreCase("unrealircd")) mode = Modes.UNREALIRCD;
				else if (modestr.equalsIgnoreCase("inspire") || modestr.equalsIgnoreCase("inspircd")) mode = Modes.INSPIRCD;
				else mode = Modes.STANDALONE;

				if (MOTD.size() == 0) {
					MOTD.add("_________        __    __   .__        ___________  _____     _");
					MOTD.add("\\______  \\___ __|  |  |  |  |__| __   |_   _| ___ \\/  __ \\   | |");
					MOTD.add(" |   |_\\  \\  |  |  | _|  | _____/  |_   | | | |_/ /| /  \\/ __| |");
					MOTD.add(" |    __ _/  |  \\  |/ /  |/ /  \\   __\\  | | |    / | |    / _` |");
					MOTD.add(" |   |_/  \\  |  /    <|    <|  ||  |   _| |_| |\\ \\ | \\__/\\ (_| |");
					MOTD.add(" |______  /____/|__|_ \\__|_ \\__||__|   \\___/\\_| \\_| \\____/\\__,_|");
					MOTD.add("        \\/           \\/    \\/");
					MOTD.add("");
					MOTD.add("Welcome to " + IRCd.serverName + ", running " + IRCd.version + ".");
					MOTD.add("Enjoy your stay!");
				}

				if (mode == Modes.STANDALONE) {
					Thread.currentThread().setName("Thread-BukkitIRCd-StandaloneIRCd");
					clientConnections.clear();
					try {
						try {
							listener = new ServerSocket(port);
							listener.setSoTimeout(1000);
							listener.setReuseAddress(true);
							BukkitIRCdPlugin.log.info("[BukkitIRCd] Listening for client connections on port " + port);
						} catch (IOException e) { BukkitIRCdPlugin.log.severe("Failed to listen on port " + port+ ": " +e); }
						while (running) {
							if ((clientConnections.size() < maxConnections) || (maxConnections == 0)) {
								ClientConnection connection;
								try {
									server = listener.accept();
									if (server.isConnected()) {
										connection = new ClientConnection(server);
										connection.lastPingResponse = System.currentTimeMillis();
										clientConnections.add(connection);
										Thread t = new Thread(connection);
										t.start();
									}
								} catch (SocketTimeoutException e) { }
								if (tickCount+(pingInterval*1000) < System.currentTimeMillis()) {
									tickCount = System.currentTimeMillis();
									writeAll("PING :" +tickCount);
								}
							}
							try { Thread.currentThread();
							Thread.sleep(1); } catch (InterruptedException e) { }
						}
					} catch (IOException e) {
						synchronized(csStdOut) {
							System.out.println("[BukkitIRCd] IOException on socket listen: " + e.toString());
						}
					}
				}
				else if (mode == Modes.INSPIRCD) {
					Thread.currentThread().setName("Thread-BukkitIRCd-InspIRCd");
					String line = null;
					serverUID = ugen.generateUID(SID);
					pre = ":" + sID + " ";
					lastconnected = false;
					isIncoming = false;
					remoteSID = null;

					try {
						listener = new ServerSocket(localPort);
						listener.setSoTimeout(1000);
						listener.setReuseAddress(true);
						BukkitIRCdPlugin.log.info("[BukkitIRCd] Listening for server connections on port " +localPort);
					} catch (IOException e) { BukkitIRCdPlugin.log.severe("Failed to listen on port " +localPort+ ": " +e); }

					try { server = listener.accept(); } catch (IOException e) { }
					if ((server != null) && server.isConnected() && (!server.isClosed())) {
						InetAddress addr = server.getInetAddress();
						BukkitIRCdPlugin.log.info("[BukkitIRCd] Got server connection from " + addr.getHostAddress());
						isIncoming = true;
					}
					else if (autoConnect) {
						connect();
					}

					while (running) {
						try {
							if ((server != null) && server.isConnected() && (!server.isClosed())&& (!lastconnected)) {
								in = new BufferedReader(new InputStreamReader(server.getInputStream()));
								out = new PrintStream(server.getOutputStream());
								line = in.readLine();
								if (line == null) throw new IOException("Lost connection to server before sending handshake!");
								String[] split = line.split(" ");
								if (debugMode) BukkitIRCdPlugin.log.info("[BukkitIRCd]" + ChatColor.YELLOW + "[->] " + line);
								

								if (!isIncoming) {
									sendLinkCAPAB();
									sendLinkBurst();
								}


								while ((!split[0].equalsIgnoreCase("SERVER")) && (server != null) && (!server.isClosed()) && server.isConnected() && running) {
									if (!running) break;

									if (line.startsWith("CAPAB START")) {
										sendLinkCAPAB();
									}

									if (split[0].equalsIgnoreCase("ERROR")) {
										// ERROR :Invalid password.
										if (split[1].startsWith(":")) split[1] = split[1].substring(1);
										try { server.close(); } catch (IOException e) { }
										throw new IOException("Remote host rejected connection, probably configured wrong: " + join(split, " ", 1)); 
									}
									else {
										line = in.readLine();
										if (line != null) {
											split = line.split(" ");
											if (debugMode) BukkitIRCdPlugin.log.info("[BukkitIRCd]§e[->] " +line);
										}
									}
								}
								if (split[0].equalsIgnoreCase("SERVER")) {
									// SERVER test.tempcraft.net password 0 280 :TempCraft Testing Server
									if ((!split[2].equals(receivePassword)) || (!split[1].equals(linkName))) {
										if (!split[2].equals(receivePassword)) println("ERROR :Invalid password.");
										else if (!split[1].equals(linkName)) println("ERROR :No configuration for hostname " + split[1]);
										server.close();

										if (!split[1].equals(linkName)) throw new IOException("Rejected connection from remote host: Invalid link name.");
										else throw new IOException("Rejected connection from remote host: Invalid password.");
									}
									remoteSID = split[4];
								}

								linkLastPingPong = System.currentTimeMillis();
								linkLastPingSent = System.currentTimeMillis();

								if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
									if (msgLinked.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgLinked.replace("%LINKNAME%", linkName));
								}
								server.setSoTimeout(500);
								lastconnected = true;
								linkcompleted = true;
							}

							while (running && (server != null) && server.isConnected() && (!server.isClosed())) {
								try {
									if (linkLastPingPong+(linkTimeoutInterval * 1000) < System.currentTimeMillis()) {
										// Link ping timeout, disconnect and notify remote server
										println("ERROR :Ping timeout");
										server.close();
									}
									else {
										if (linkLastPingSent+(linkPingInterval * 1000) < System.currentTimeMillis()) {
											println(pre + "PING " + sID + " " +remoteSID);
											linkLastPingSent = System.currentTimeMillis();
										}
										line = in.readLine();

										if ((line != null) && (line.trim().length() > 0)) {
											if (line.startsWith("ERROR ")) {
												// ERROR :Invalid password.
												if (debugMode) BukkitIRCdPlugin.log.info("[BukkitIRCd]§e[->] " +line);
												String[] split = line.split(" ");
												if (split[1].startsWith(":")) split[1] = split[1].substring(1);
												try { server.close(); } catch (IOException e) { } 
												throw new IOException("Remote host rejected connection, probably configured wrong: " + join(split, " ", 1));
											}
											else parseLinkCommand(line);
										}
									}
								} catch (SocketTimeoutException e) { }
								try { Thread.currentThread();
								Thread.sleep(1); } catch (InterruptedException e) { }
							}
							try { Thread.currentThread();
							Thread.sleep(1); } catch (InterruptedException e) { }
						} catch (IOException e) {
							synchronized(csStdOut) {
								BukkitIRCdPlugin.log.warning("[BukkitIRCd] Server link failed: " + e);
							}
						}

						// We exited the while loop so assume the connection was lost.
						if (lastconnected) {
							BukkitIRCdPlugin.log.info("[BukkitIRCd] Lost connection to " +remoteHost+ ":" +remotePort);
							if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null) && linkcompleted) {
								if (msgDelinked.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgDelinked.replace("%LINKNAME%",linkName));
							}
							lastconnected = false;
						}

						if ((server != null) && server.isConnected()) try { server.close(); } catch (IOException e) { }
						linkcompleted = false;
						capabSent = false;
						burstSent = false;
						uid2ircuser.clear();
						servers.clear();
						remoteSID = null;
						if (running) {
							if (autoConnect) {
								BukkitIRCdPlugin.log.info("[BukkitIRCd] Waiting " +linkDelay+ " seconds before retrying...");
								long endTime = System.currentTimeMillis()+(linkDelay*1000);
								while (System.currentTimeMillis() < endTime) {
									if ((!running) || isConnected()) break;
									Thread.currentThread();
									Thread.sleep(10);
									try {
										server = listener.accept();
										if ((server != null) && server.isConnected() && (!server.isClosed())) {
											InetAddress addr = server.getInetAddress();
											BukkitIRCdPlugin.log.info("[BukkitIRCd] Got server connection from " +addr.getHostAddress());
											isIncoming = true;
											break;
										}
									} catch (IOException e) { }
								}
								if ((server == null) || (!server.isConnected()) || (server.isClosed())) {
									connect();
								}
							}
							else {
								try {
									server = listener.accept();
									if ((server != null) && server.isConnected() && (!server.isClosed())) {
										InetAddress addr = server.getInetAddress();
										BukkitIRCdPlugin.log.info("[BukkitIRCd] Got server connection from " +addr.getHostAddress());
										isIncoming = true;
									}							
								} catch (IOException e) { }
							}
							Thread.currentThread();
							Thread.sleep(1);
						}
					}		
				}
			}
			catch (InterruptedException e) {
				BukkitIRCdPlugin.log.info("[BukkitIRCd] Thread " +Thread.currentThread().getName() + " interrupted.");
				if (running) {
					disconnectAll("Thread interrupted.");
					running = false;
				}
			}
			catch (Exception e) {
				BukkitIRCdPlugin.log.severe("[BukkitIRCd] Unexpected exception in " +Thread.currentThread().getName() + ": " +e.toString());
				BukkitIRCdPlugin.log.severe("[BukkitIRCd] Error code IRCd473.");
				e.printStackTrace();
			}
		}
		BukkitIRCdPlugin.ircd = null;
		if (running) BukkitIRCdPlugin.log.warning("[BukkitIRCd] Thread quit unexpectedly. If there are any errors above, please notify Jdbye about them.");
		running = false;
	}
	
	public static boolean isConnected() {
		return ((server != null) && server.isConnected() && (!server.isClosed()));
	}
	
	public static boolean connect() {
		if (mode == Modes.INSPIRCD) {
			BukkitIRCdPlugin.log.info("[BukkitIRCd] Attempting connection to " +remoteHost+ ":" +remotePort);
			try {
				server = new Socket(remoteHost, remotePort);
				if ((server != null) && server.isConnected()) {
					BukkitIRCdPlugin.log.info("[BukkitIRCd] Connected to " +remoteHost+ ":" +remotePort);
					isIncoming = false;
					return true;
				}
				else BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed connection to " +remoteHost+ ":" +remotePort);
			}
			catch (IOException e) { BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed connection to " +remoteHost+ ":" +remotePort+ " (" +e + ")"); }
		}
		return false;
	}
	
	public static boolean sendLinkCAPAB() {
		if (capabSent) return false;
		println("CAPAB START 1201");
		println("CAPAB CAPABILITIES :NICKMAX=" +(nickLen +1) + " CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
		//println("CAPAB CHANMODES :admin=&a ban=b founder=~q halfop=%h op=@o operonly=O voice=+v"); // Don't send this line, the server will complain that we don't support various modes and refuse to link
		//println("CAPAB USERMODES :bot=B oper=o u_registered=r"); // Don't send this line, the server will complain that we don't support various modes and refuse to link
		println("CAPAB END");
		println("SERVER " + serverHostName + " " + connectPassworD + " 0 " + sID + " :" + serverDescription);
		capabSent = true;
		return true;
	}
	
	public static boolean sendLinkBurst() {
		if (burstSent) return false;
		println(pre + "BURST " +(System.currentTimeMillis() / 1000L));
		println(pre + "VERSION :" +version);

		println(pre + "UID " + serverUID + " " + serverStartTime + " " + serverName + " " + serverHostName + " " + serverHostName + " " + serverName + " 127.0.0.1 " + serverStartTime + " +Bro :" +version);
		println(":" + serverUID + " OPERTYPE Network_Service");

		for (BukkitPlayer bp : bukkitPlayers) {
			String UID = ugen.generateUID(SID);
			bp.setUID(UID);
			if (bp.hasPermission("bukkitircd.oper")) {
				println(pre + "UID " + uID + " " +(bp.idleTime / 1000L) + " " +bp.nick + ingameSuffix + " " +bp.host+ " " +bp.host+ " " +bp.nick + " " +bp.ip+ " " +bp.signedOn + " +or :Minecraft Player");
				println(":" + uID + " OPERTYPE IRC_Operator");
			}
			else println(pre + "UID " + uID + " " +(bp.idleTime / 1000L) + " " +bp.nick + ingameSuffix + " " +bp.host+ " " +bp.host+ " " +bp.nick + " " +bp.ip+ " " +bp.signedOn + " +r :Minecraft Player");

			String world = bp.getWorld();
			if (world != null) println(pre + "METADATA " + UID + " swhois :is currently in " + world);
			else println(pre + "METADATA " + UID + " swhois :is currently in an unknown world");
		}

		println(pre + "FJOIN " + consoleChannelName + " " + consoleChannelTS + " + nt :," + serverUID);
		println(":" + serverUID + " FMODE " + consoleChannelName + " " + consoleChannelTS + " +qaohv " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID);
		println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + serverUID);
		println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " +qaohv " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID);
		
		for (BukkitPlayer bp : bukkitPlayers) {
			String UID = bp.getUID();
			String textMode = bp.getTextMode();
			println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + uID);
			if (textMode.length() > 0) {
				String modestr = "";
				for (int i = 0; i < textMode.length(); i++) {
					modestr += UID + " ";
				}
				modestr = modestr.substring(0, modestr.length()-1);
				println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " + " +textMode + " " +modestr);
			}
		}
		
		println(pre + "ENDBURST");
		burstSent = true;
		return true;
	}

	public static int getClientCount() {
		if (mode == Modes.STANDALONE) return clientConnections.size()+bukkitPlayers.size();
		else return bukkitPlayers.size();
	}

	public static int getOperCount() {
		int count=0;
		synchronized(csIrcUsers) {
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
		if (mode == Modes.STANDALONE) return 0;
		else return 1+servers.size();
	}

	public static IRCUser getIRCUser(String nick) {
		synchronized(csIrcUsers) {
			int i = 0;
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor != null) && (processor.nick.equalsIgnoreCase(nick))) { return new IRCUser(processor.nick, processor.realname, processor.ident, processor.hostmask, processor.ipaddress, processor.modes, processor.customWhois, processor.isRegistered, processor.isOper, processor.awayMsg, processor.signonTime, processor.lastActivity); }
					i++;
				}
			}
			else if (mode == Modes.INSPIRCD) {
				IRCUser iuser;
				Iterator<?> iter = uid2ircuser.entrySet().iterator();
				while (iter.hasNext()) {
					@SuppressWarnings("unchecked")
					Map.Entry<String, IRCUser> entry = (Entry<String, IRCUser>)iter.next();
					iuser = entry.getValue();
					if (iuser.nick.equalsIgnoreCase(nick)) return iuser;
				}
			}
		}
		return null;
	}
	
	public static IRCUser[] getIRCUsers() {
		List<IRCUser> users = new ArrayList<IRCUser>();
		Object[] ircUsers = null;
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					IRCUser iu = new IRCUser(processor.nick, processor.realname, processor.ident, processor.hostmask, processor.ipaddress, processor.modes, processor.customWhois, processor.isRegistered, processor.isOper, processor.awayMsg, processor.signonTime, processor.lastActivity);
					iu.joined = (processor.isIdented && processor.isNickSet);
					users.add(iu);
				}
				ircUsers = users.toArray();
			}
			else if (mode == Modes.INSPIRCD) {
				ircUsers = uid2ircuser.values().toArray();
			}
		}
		if ((ircUsers != null) && (ircUsers instanceof IRCUser[])) return (IRCUser[]) ircUsers;
		else return new IRCUser[0];
	}

	public static String getUsers() {
		String users = "";
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					String nick;
					if (processor.modes.length() > 0) nick = processor.modes.substring(0,1)+ processor.nick;
					else nick = processor.nick;
					if (users.length() == 0) { users = nick; }
					else { users = userS + " " + nick; }
				}
			}
			else if (mode == Modes.INSPIRCD) {
				for (IRCUser user : (IRCUser[]) uid2ircuser.values().toArray()) {
					String nick;
					String modes = user.getModes();
					if (modes.length() > 0) nick = modes.substring(0,1)+user.nick;
					else nick = user.nick;
					if (users.length() == 0) { users = nick; }
					else { users = userS + " " + nick; }					
				}
			}
		}
		synchronized(csBukkitPlayers) {
			int i = 0;
			while (i < bukkitPlayers.size()) {
				BukkitPlayer bukkitPlayer = bukkitPlayers.get(i);
				String nick = bukkitPlayer.nick;
				String modes = bukkitPlayer.getMode();
				String nick2;
				if (modes.length() > 0) nick2 = modes.substring(0,1)+ nick + ingameSuffix;
				else nick2 = nick + ingameSuffix;
				if (users.length() == 0) { users = nick2; }
				else { users = userS + " " + nick2; }
				i++;
			}
		}
		return users;
	}

	public static String getOpers() {
		String users = "";
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					if (processor.isOper) {
						String nick;
						if (processor.modes.length() > 0) nick = processor.modes.substring(0,1)+ processor.nick;
						else nick = processor.nick;
						if (users.length() == 0) { users = nick; }
						else { users = userS + " " + nick; }
					}
				}
			}
			else if (mode == Modes.INSPIRCD) {
				for (IRCUser user : (IRCUser[]) uid2ircuser.values().toArray()) {
					if (user.isOper) {
						String nick;
						String modes = user.getModes();
						if (modes.length() > 0) nick = modes.substring(0,1)+user.nick;
						else nick = user.nick;
						if (users.length() == 0) { users = nick; }
						else { users = userS + " " + nick; }
					}
				}
			}
		}
		return users;
	}

	public static String[] getIRCNicks() {
		List<String> users = new ArrayList<String>();
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				for (ClientConnection processor : clientConnections) {
					if (processor.isIdented && processor.isNickSet) users.add(processor.nick); 
				}
			}
			else if (mode == Modes.INSPIRCD) {
				for (Object user : uid2ircuser.values().toArray()) {
					if (((IRCUser)user).joined) users.add(((IRCUser)user).nick); 
				}
			}
		}
		String userArray[] = new String[0];
		userArray = users.toArray(userArray);
		Arrays.sort(userArray);
		return userArray;
	}

	public static String[] getIRCWhois(IRCUser ircuser) {
		if (ircuser == null) return null;
		String whois[]=null;
		synchronized(csIrcUsers) {
			whois = new String[8];
			String idletime = TimeUtils.millisToLongDHMS(ircuser.getSecondsIdle()*1000);
			whois[0] = "§2Nickname: §7" + ircuser.nick + "§f";
			whois[1] = "§2Ident: §7" + ircuser.ident+ "§f";
			whois[2] = "§2Hostname: §7" + ircuser.hostmask + "§f";
			whois[3] = "§2Realname: §7" + ircuser.realname + "§f";
			whois[4] = "§2Is registered: §7" +(ircuser.isRegistered ? "Yes" : "No") + "§f";
			whois[5] = "§2Is operator: §7" +(ircuser.isOper ? "Yes" : "No") + "§f";
			whois[5] = "§2Away: §7" +((!ircuser.awayMsg.equals("")) ? ircuser.awayMsg : "No") + "§f";
			whois[6] = "§2Idle §7" + idletime + "§f";
			whois[7] = "§2Signed on at §7" +dateFormat.format(ircuser.signonTime*1000) + "§f";
		}
		return whois;
	}
	
	public static boolean removeIRCUser(String nick) {
		return removeIRCUser(nick, null, false);
	}

	public static boolean removeIRCUser(String nick, String reason, boolean IRCToGame) {
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					ClientConnection processor = iter.next();
					if (processor.nick.equalsIgnoreCase(nick)) {
						if (processor.isIdented && processor.isNickSet) {
							if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								BukkitIRCdPlugin.thePlugin.removeLastReceivedFrom(processor.nick);
								if (reason != null) {
									if (msgIRCLeave.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeaveReason.replace("%USER%", processor.nick).replace("%REASON%", convertColors(reason, IRCToGame)));
									if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveReasonDynmap.replace("%USER%", processor.nick).replace("%REASON%", stripFormatting(reason)));
								}
								else {
									if (msgIRCLeave.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeave.replace("%USER%", processor.nick));
									if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveDynmap.replace("%USER%", processor.nick));									
								}
							}
						}
						iter.remove();
						if (processor.isConnected()) processor.disconnect();
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean removeIRCUsers() {
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					ClientConnection processor = iter.next();
					if (processor.isIdented && processor.isNickSet) {
						if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
							BukkitIRCdPlugin.thePlugin.removeLastReceivedFrom(processor.nick);
							if (msgIRCLeave.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeave.replace("%USER%", processor.nick));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveDynmap.replace("%USER%", processor.nick));
						}
					}
					iter.remove();
					if (processor.isConnected()) processor.disconnect();
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean removeIRCUsersBySID(String serverID) {
		if (mode != Modes.INSPIRCD) return false;
		IRCServer is = servers.get(serverID);
		if (is != null) {
			if (debugMode) BukkitIRCdPlugin.log.info("[BukkitIRCd] Server " + serverID + " (" + is.host + ") delinked");
			Iterator<Entry<String, IRCUser>> iter = uid2ircuser.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, IRCUser> entry = iter.next();
				String curUID = entry.getKey();
				IRCUser curUser = entry.getValue();
				if (curUID.startsWith(serverID)) {
					if (curUser.joined) {
						if (msgIRCLeaveReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeaveReason.replace("%USER%", curUser.nick).replace("%REASON%", is.host+ " split"));
						if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveReasonDynmap.replace("%USER%",curUser.nick).replace("%REASON%", is.host+ " split"));
					}
					iter.remove();
				}
			}
			servers.remove(serverID);
			for (String curSID : is.leaves) removeIRCUsersBySID(curSID);
			return true;
		}
		return false;
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy, String kickBannedByHost,boolean isIngame) {
		return kickBanIRCUser(ircuser, kickBannedBy, kickBannedByHost, null,isIngame,ircBanType);
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy, String kickBannedByHost,boolean isIngame,String banType) {
		return kickBanIRCUser(ircuser, kickBannedBy, kickBannedByHost, null,isIngame,banType);
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy, String kickBannedByHost,String reason,boolean isIngame) {
		return kickBanIRCUser(ircuser, kickBannedBy, kickBannedByHost, null,isIngame,ircBanType);
	}

	public static boolean kickBanIRCUser(IRCUser ircuser, String kickBannedBy, String kickBannedByHost,String reason,boolean isIngame,String banType) {
		if (banType == null) banType = ircBanType;
		String split[] = kickBannedByHost.split("!")[1].split("@");
		String kickedByIdent = split[0];
		String kickedByHostname = split[1];
		return (banIRCUser(ircuser, kickBannedBy, kickBannedByHost, isIngame, banType) && kickIRCUser(ircuser, kickBannedBy, kickedByIdent, kickedByHostname, reason, isIngame));
	}

	public static boolean kickIRCUser(IRCUser ircuser, String kickedByNick,String kickedByIdent, String kickedByHost, boolean isIngame) {
		return kickIRCUser(ircuser, kickedByNick, kickedByIdent, kickedByHost, null,isIngame);
	}

	@SuppressWarnings("unchecked")
	public static boolean kickIRCUser(IRCUser ircuser, String kickedByNick,String kickedByIdent, String kickedByHost,String reason,boolean isIngame) {
		if (ircuser == null) return false;
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					processor = iter.next();
					if (processor.nick.equalsIgnoreCase(ircuser.nick)) {
						if (processor.isIdented && processor.isNickSet) {
							if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								if (reason != null) {
									if (msgIRCKickReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCKickReason.replace("%KICKEDUSER%",processor.nick).replace("%KICKEDBY%",kickedByNick).replace("%REASON%",convertColors(reason,true)));
									if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCKickReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCKickReasonDynmap.replace("%KICKEDUSER%",processor.nick).replace("%KICKEDBY%",kickedByNick).replace("%REASON%",stripFormatting(reason)));								
								}
								else {
									if (msgIRCKick.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCKick.replace("%KICKEDUSER%",processor.nick).replace("%KICKEDBY%",kickedByNick));
									if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCKickDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCKickDynmap.replace("%KICKEDUSER%",processor.nick).replace("%KICKEDBY%",kickedByNick));
								}
							}
						}
						if (isIngame) {
							kickedByNick += ingameSuffix;
							if (reason != null) reason = convertColors(reason, false);
						}
						if (reason != null) {
							writeAll(":" + processor.getFullHost() + " QUIT :Kicked by " +kickedByNick + ": " +reason);
							processor.writeln(":" +kickedByNick + "!" +kickedByIdent+ "@" +kickedByHost+ " KILL " + processor.nick + " :" +kickedByHost+ "!" +kickedByNick + " (" +reason + ")");
							processor.writeln("ERROR :Closing Link: " + processor.nick + "[" + processor.hostmask + "] " +kickedByNick + " (Kicked by " +kickedByNick + " (" +reason + "))");
						}
						else {
							writeAll(":" + processor.getFullHost() + " QUIT :Kicked by " +kickedByNick);
							processor.writeln(":" +kickedByNick + "!" +kickedByIdent+ "@" +kickedByHost+ " KILL " + processor.nick + " :" +kickedByHost+ "!" +kickedByNick);
							processor.writeln("ERROR :Closing Link: " + processor.nick + "[" + processor.hostmask + "] " +kickedByNick + " (Kicked by " +kickedByNick + ")");
						}
						processor.disconnect();
						iter.remove();
						return true;
					}
				}
			}
			else if (mode == Modes.INSPIRCD) {
				IRCUser iuser = null;
				Iterator<?> iter = uid2ircuser.entrySet().iterator();
				String uid = null;
				while (iter.hasNext()) {
					Map.Entry<String, IRCUser> entry = (Entry<String, IRCUser>)iter.next();
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
					if ((bukkitUser = getBukkitUserObject(kickedByNick)) != null) sourceUID = bukkitUser.getUID();
					else sourceUID = serverUID;
					
					boolean returnVal = false;
					if (iuser.consoleJoined) {
						if (reason != null) {
							println(":" + sourceUID + " KICK " + consoleChannelName + " " + uid + " :" + reason);
						}
						else {
							println(":" + sourceUID + " KICK " + consoleChannelName + " " + uid + " :" + kickedByNick);
						}
						returnVal = true;
						iuser.consoleJoined = false;
					}
					if (iuser.joined) {
						if (reason != null) {
							println(":" + sourceUID + " KICK " + channelName + " " + uiD + " :" +reason);
							if (msgIRCKickReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCKickReason.replace("%KICKEDUSER%",iuser.nick).replace("%KICKEDBY%",kickedByNick).replace("%REASON%",convertColors(reason,true)));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCKickReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCKickReasonDynmap.replace("%KICKEDUSER%",iuser.nick).replace("%KICKEDBY%",kickedByNick).replace("%REASON%",stripFormatting(reason)));
						}
						else {
							println(":" + sourceUID + " KICK " + channelName + " " + uiD + " :" +kickedByNick);						
							if (msgIRCKick.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCKick.replace("%KICKEDUSER%",iuser.nick).replace("%KICKEDBY%",kickedByNick));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCKickDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCKickDynmap.replace("%KICKEDUSER%",iuser.nick).replace("%KICKEDBY%",kickedByNick));
						}
						returnVal = true;
						iuser.joined = false;
					}
					else BukkitIRCdPlugin.log.info("Player " + kickedByNick + " tried to kick IRC user not on channel: " + iuser.nick); // Log this as severe since it should never occur unless something is wrong with the code
					
					return returnVal;
				}
				else BukkitIRCdPlugin.log.severe("[BukkitIRCd] User " + ircuser.nick + " not found in UID list. Error code IRCd942."); // Log this as severe since it should never occur unless something is wrong with the code
			}
		}
		return false;

	}

	public static boolean banIRCUser(IRCUser ircuser, String bannedBy,String bannedByHost,boolean isIngame,String banType) {
		// TODO: Add support for banning in linking mode
		if (ircuser == null) return false;
		synchronized(csIrcUsers) {
			//ClientConnection processor;
			IRCUser[] ircusers = getIRCUsers();
			for (int i = 0; i < ircusers.length; i++) {
				ircuser = ircusers[i];
				if (ircuser.nick.equalsIgnoreCase(ircuser.nick)) {
					if (isIngame) {
						bannedByHost = bannedBy+ ingameSuffix + "!" +bannedBy+ "@" +bannedByHost;
						bannedBy += ingameSuffix;
					}
					String banHost;
					if ((banType.equals("host")) || (banType.equals("hostname"))) banHost = "*!*@" + ircuser.hostmask;
					else if ((banType.equals("ip")) || (banType.equals("ipaddress"))) banHost = "*!*@" + ircuser.ipaddress;
					else if (banType.equals("ident")) banHost = "*!" + ircuser.ident+ "@*";
					else banHost = ircuser.nick + "!*@*";
					boolean result = banIRCUser(banHost, bannedByHost);
					if (result) {
						if (ircuser.joined) {
							if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								if (msgIRCBan.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCBan.replace("%BANNEDUSER%", ircuser.nick).replace("%BANNEDBY%", bannedBy));
								if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCBanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCBanDynmap.replace("%BANNEDUSER%", ircuser.nick).replace("%BANNEDBY%", bannedBy));
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
		synchronized(csIrcBans) {
			if (isBanned(banHost)) return false;
			else {
				if (mode == Modes.STANDALONE) {
					ircBans.add(new IrcBan(banHost, bannedByHost, System.currentTimeMillis()/1000L));
					writeAll(":" +bannedByHost+ " MODE " + IRCd.channelName + " +b " +banHost);
					return true;
				}
				else if (mode == Modes.INSPIRCD) {
					String user = bannedByHost.split("!")[0];
					if (user.endsWith(ingameSuffix)) user = user.substring(0, user.length()-ingameSuffix.length());
					String UID;
					BukkitPlayer bp = null;
					if (((UID = getUIDFromIRCUser(user)) != null) || ((bp = getBukkitUserObject(user)) != null) || (user.equals(serverName))) {
						if (user.equals(serverName)) UID = serverUID;
						else if (UID == null) UID = bp.getUID();
						println(":" + uID + " FMODE " + channelName + " " + channelTS + " +b :" +banHost);
						return true;
					}
					else {
						BukkitIRCdPlugin.log.severe("[BukkitIRCd] User " + user + " not found in UID list. Error code IRCd1004."); // Log this as severe since it should never occur unless something is wrong with the code
						return false;
					}
				}
				return false;
			}
		}
	}

	public static boolean unBanIRCUser(String banHost, String bannedByHost) {
		synchronized(csIrcBans) {
			int ban = -1;
			if (mode == Modes.STANDALONE) {
				if ((ban = getIRCBan(banHost)) < 0) return false;
				ircBans.remove(ban);
				IRCd.writeAll(":" + bannedByHost + " MODE " + IRCd.channelName + " -b " + banHost);
				return true;
			}
			else if (mode == Modes.INSPIRCD) {
				String user = bannedByHost.split("!")[0];
				if (user.endsWith(ingameSuffix)) user = user.substring(0, user.length()-ingameSuffix.length());
				String UID;
				BukkitPlayer bp = null;
				if (((UID = getUIDFromIRCUser(user)) != null) || ((bp = getBukkitUserObject(user)) != null) || (user.equals(serverName))) {
					if (user.equals(serverName)) UID = serverUID;
					else if (UID == null) UID = bp.getUID();
					println(":" + UID + " FMODE " + channelName + " " + channelTS + " -b :" + banHost);
					return true;
				}
				else {
					BukkitIRCdPlugin.log.severe("[BukkitIRCd] User " + user + " not found in UID list. Error code IRCd1034."); // Log this as severe since it should never occur unless something is wrong with the code
					return false;
				}
			}
			return false;
		}
	}

	public static boolean isBanned(String fullHost) {
		synchronized(csIrcBans) {
			for (IrcBan ircBan : ircBans) {
				if (wildCardMatch(fullHost, ircBan.fullHost)) return true;
			}
		}		
		return false;
	}

	public static int getIRCBan(String fullHost) {
		synchronized(csIrcBans) {
			int i = 0;
			while (i < ircBans.size()) {
				if (ircBans.get(i).fullHost.equalsIgnoreCase(fullHost)) return i;
				i++;
			}
		}		
		return -1;
	}

	public static boolean wildCardMatch(String text, String pattern)
	{
		// add sentinel so don't need to worry about *'s at end of pattern
		text    += '\0';
		pattern += '\0';

		int N = pattern.length();

		boolean[] states = new boolean[N+1];
		boolean[] old = new boolean[N+1];
		old[0] = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			states = new boolean[N+1];       // initialized to false
			for (int j = 0; j < N; j++) {
				char p = pattern.charAt(j);

				// hack to handle *'s that match 0 characters
				if (old[j] && (p == '*')) old[j+1] = true;

				if (old[j] && (p ==  c )) states[j+1] = true;
				if (old[j] && (p == '?')) states[j+1] = true;
				if (old[j] && (p == '*')) states[j]   = true;
				if (old[j] && (p == '*')) states[j+1] = true;
			}
			old = states;
		}
		return states[N];
	}



	public static boolean addBukkitUser(String modes,String nick,String world,String host, String ip) {
		if (getBukkitUser(nick) < 0) {
			synchronized(csBukkitPlayers) {
				BukkitPlayer bp = new BukkitPlayer(nick, world, modes, host, ip, System.currentTimeMillis() / 1000L, System.currentTimeMillis());
				bukkitPlayers.add(bp);

				if (mode == Modes.STANDALONE) {
					writeAll(":" + nick + ingameSuffix + "!" + nick + "@" + host + " JOIN " + IRCd.channelName);
				}
				String mode1=" + ", mode2="";
				if (modes.contains("~")) { mode1+="q"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains("&")) { mode1+="a"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains("@")) { mode1+="o"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains("%")) { mode1+="h"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains(" + ")) { mode1+="v"; mode2+=nick + ingameSuffix + " "; }
				if (!mode1.equals(" + ")) {
					if (mode == Modes.STANDALONE) {
						writeAll(":" + serverName + "!" + serverName + "@" + serverHostName + " MODE " + IRCd.channelName + " " + mode1 + " " + mode2.substring(0, mode2.length()-1));
					}
				}
				
				if (mode == Modes.INSPIRCD) {
					
					String UID = ugen.generateUID(SID);
					bp.setUID(UID);
					synchronized(csBukkitPlayers) {
						String textMode = bp.getTextMode();
						if (bp.hasPermission("bukkitircd.oper")) {
							println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " " + bp.nick + ingameSuffix + " " + bp.host + " " + bp.host + " " + bp.nick + " " + bp.ip + " " + bp.signedOn + " +or :Minecraft Player");
							println(":" + UID + " OPERTYPE IRC_Operator");
						}
						else println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " " + bp.nick + ingameSuffix + " " + bp.host + " " + bp.host + " " + bp.nick + " " + bp.ip + " " + bp.signedOn + " +r :Minecraft Player");

						println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + UID);
						if (textMode.length() > 0) {
							String modestr = "";
							for (int i = 0; i < textMode.length(); i++) {
								modestr += UID + " ";
							}
							modestr = modestr.substring(0, modestr.length()-1);
							println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " + " + textMode + " " +modestr);
						}
						if (world != null) println(pre + "METADATA " + UID + " swhois :is currently in " + world);
						else println(pre + "METADATA " + UID + " swhois :is currently in an unknown world");
					}
				}
			}
			return true;
		}
		else return false;
	}

	public static boolean addBukkitUser(String modes,Player player) {
		String nick = player.getName();
		String host = player.getAddress().getAddress().getHostName();
		String ip = player.getAddress().getAddress().getHostAddress();
		String world = player.getWorld().getName();
		if (getBukkitUser(nick) < 0) {
			synchronized(csBukkitPlayers) {
				BukkitPlayer bp = new BukkitPlayer(nick, world, modes, host, ip, System.currentTimeMillis() / 1000L, System.currentTimeMillis());
				bukkitPlayers.add(bp);
				if (mode == Modes.STANDALONE) {
					writeAll(":" + nick + ingameSuffix + "!" + nick + "@" + host + " JOIN " + IRCd.channelName);
				}
				String mode1=" + ", mode2="";
				if (modes.contains("~")) { mode1+="q"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains("&")) { mode1+="a"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains("@")) { mode1+="o"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains("%")) { mode1+="h"; mode2+=nick + ingameSuffix + " "; }
				if (modes.contains(" + ")) { mode1+="v"; mode2+=nick + ingameSuffix + " "; }
				if (!mode1.equals(" + ")) {
					if (mode == Modes.STANDALONE) {
						writeAll(":" + serverName + "!" + serverName + "@" + serverHostName + " MODE " + IRCd.channelName + " " + mode1 + " " + mode2.substring(0, mode2.length()-1));
					}
				}
				
				if (mode == Modes.INSPIRCD) {
					String UID = ugen.generateUID(SID);
					bp.setUID(UID);
					synchronized(csBukkitPlayers) {
						String textMode = bp.getTextMode();
						if (bp.hasPermission("bukkitircd.oper")) {
							println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " " + bp.nick + ingameSuffix + " " + bp.host + " " + bp.host + " " + bp.nick + " " + bp.ip + " " + bp.signedOn + " +or :Minecraft Player");
							println(":" + UID + " OPERTYPE IRC_Operator");
						}
						else println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " " + bp.nick + ingameSuffix + " " + bp.host + " " + bp.host + " " + bp.nick + " " + bp.ip + " " + bp.signedOn + " +r :Minecraft Player");

						println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + UID);
						if (textMode.length() > 0) {
							String modestr = "";
							for (int i = 0; i < textMode.length(); i++) {
								modestr += UID + " ";
							}
							modestr = modestr.substring(0, modestr.length()-1);
							println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " + " + textMode + " " + modestr);
						}
						if (world != null) println(pre + "METADATA " + UID + " swhois :is currently in " + world);
						else println(pre + "METADATA " + UID + " swhois :is currently in an unknown world");
					}
				}
				return true;
			}
		}
		else return false;
	}

	public static boolean removeBukkitUser(int ID) {
		synchronized(csBukkitPlayers) {
			if (ID >= 0) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				if (mode == Modes.STANDALONE) {
					writeAll(":" + bp.nick + ingameSuffix + "!" + bp.nick + "@" + bp.host + " QUIT :Left the server");
				}
				else if (mode == Modes.INSPIRCD) {
					println(":" + bp.getUID() + " QUIT :Left the server");
				}
				bukkitPlayers.remove(ID);
				return true;
			}
			else return false;
		}
	}

	public static boolean removeBukkitUserByUID(String UID) {
		synchronized(csBukkitPlayers) {
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

	public static boolean kickBukkitUser(String kickReason, int ID) {
		if (ID >= 0) {
			synchronized(csBukkitPlayers) {
				BukkitPlayer bukkitPlayer = bukkitPlayers.get(ID);
				String host = bukkitPlayer.host;
				String name = bukkitPlayer.nick;
				if (mode == Modes.STANDALONE) {
					writeAll(":" + name + ingameSuffix + "!" + name + "@" + host+ " QUIT :Kicked: " + convertColors(kickReason,false));
				}
				else {
					println(":" +bukkitPlayer.getUID() + " QUIT :Kicked: " + convertColors(kickReason,false));
				}
				bukkitPlayers.remove(ID);
				return true;
			}
		}
		else return false;
	}

	public static int getBukkitUser(String nick) {
		synchronized(csBukkitPlayers) {
			int i = 0;
			String curnick;
			while (i < bukkitPlayers.size()) {
				curnick = bukkitPlayers.get(i).nick;
				if ((curnick.equalsIgnoreCase(nick)) || ((curnick + ingameSuffix).equalsIgnoreCase(nick))) { return i; }
				else i++;
			}
			return -1;
		}
	}

	public static BukkitPlayer getBukkitUserObject(String nick) {
		synchronized(csBukkitPlayers) {
			int i = 0;
			String curnick;
			while (i < bukkitPlayers.size()) {
				BukkitPlayer bp = bukkitPlayers.get(i); 
				curnick = bp.nick;
				if ((curnick.equalsIgnoreCase(nick)) || ((curnick + ingameSuffix).equalsIgnoreCase(nick))) { return bp; }
				i++;
			}
			return null;
		}
	}

	public static BukkitPlayer getBukkitUserByUID(String UID) {
		synchronized(csBukkitPlayers) {
			int i = 0;
			BukkitPlayer bp;
			while (i < bukkitPlayers.size()) {
				bp = bukkitPlayers.get(i);
				if (bp.getUID().equalsIgnoreCase(UID)) { return bp; }
				else i++;
			}
			return null;
		}
	}

	public static boolean updateBukkitUserIdleTimeAndWorld(int ID, String world) {
		if (ID >= 0) {
			synchronized(csBukkitPlayers) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				bp.idleTime = System.currentTimeMillis();
				if (!bp.world.equals(world)) {
					bp.world = world;
					if (mode == Modes.INSPIRCD) {
						println(pre + "METADATA " + bp.getUID() + " swhois :is currently in " + world);
					}
				}
				return true;
			}
		}
		else return false;
	}

	public static boolean updateBukkitUserIdleTime(int ID) {
		if (ID >= 0) {
			synchronized(csBukkitPlayers) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				bp.idleTime = System.currentTimeMillis();
				return true;
			}
		}
		else return false;
	}

	public static void writeAll(String message, Player sender) {
		int i = 0;
		String line="", host="unknown", nick="Unknown";

		synchronized(csBukkitPlayers) {
			int ID = getBukkitUser(sender.getName());
			if (ID >= 0) {
				BukkitPlayer bp = bukkitPlayers.get(ID);
				host = bp.host;
				nick = bp.nick;
			}
		}

		line = ":" + nick + ingameSuffix + "!" + nick + "@" + host+ " PRIVMSG " + IRCd.channelName + " :" +message;

		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor.isConnected()) && processor.isIdented && processor.isNickSet && (processor.lastPingResponse +(timeoutInterval*1000) > System.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					}
					else if (!processor.running) { removeIRCUser(processor.nick); } 
					else i++;
				}
			}
		}
	}

	public static void writeAll(String line) {
		int i = 0;
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor.isConnected()) && processor.isIdented && processor.isNickSet && (processor.lastPingResponse +(timeoutInterval*1000) > System.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					}
					else if (!processor.running) { removeIRCUser(processor.nick); } 
					else i++;
				}
			}
		}
	}

	public static void writeOpers(String line) {
		int i = 0;
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if ((processor.isConnected()) && processor.isIdented && processor.isNickSet && processor.isOper && (processor.lastPingResponse +(timeoutInterval*1000) > System.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					}
					else if (!processor.running) { removeIRCUser(processor.nick); } 
					else i++;
				}
			}
		}
	}

	public static void disconnectAll() {
		disconnectAll(null);
	}
	
	public static void disconnectAll(String reason) {
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				try {
					listener.close();
					listener = null;
				} catch (IOException e) {}
				removeIRCUsers();
			}
			else if ((mode == Modes.INSPIRCD) || (mode == Modes.UNREALIRCD)) {
				disconnectServer(reason);
			}
		}
	}

	public static void writeAllExcept(String nick, String line) {
		int i = 0;
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if (processor.nick.equalsIgnoreCase(nick)) { i++; continue; }
					if ((processor.isConnected()) && processor.isIdented && processor.isNickSet && (processor.lastPingResponse +(timeoutInterval*1000) > System.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					}
					else if (!processor.running) { removeIRCUser(processor.nick); } 
					else i++;
				}
			}
		}
	}

	public static void writeOpersExcept(String nick, String line) {
		int i = 0;
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				ClientConnection processor;
				while (i < clientConnections.size()) {
					processor = clientConnections.get(i);
					if (processor.nick.equalsIgnoreCase(nick)) { i++; continue; }
					if ((processor.isConnected()) && processor.isIdented && processor.isNickSet && processor.isOper && (processor.lastPingResponse +(timeoutInterval*1000) > System.currentTimeMillis())) {
						processor.writeln(line);
						i++;
					}
					else if (!processor.running) { removeIRCUser(processor.nick); } 
					else i++;
				}
			}
		}
	}

	public static boolean writeTo(String nick, String line) {
		synchronized(csIrcUsers) {
			if (mode == Modes.STANDALONE) {
				Iterator<ClientConnection> iter = clientConnections.iterator();
				while (iter.hasNext()) {
					ClientConnection processor = iter.next();
					if (processor.nick.equalsIgnoreCase(nick)) { processor.writeln(line); return true; }
				}
			}
		}
		return false;
	}

	public static String convertColors(String input, boolean fromIRCtoGame) {
		if (!convertColorCodes) {
			String output = input;
			int i = 16;
			if (fromIRCtoGame) {
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) {
						output = output.replace(((char)3) + "0" + Integer.toString(ircColors[i]), "");
					}
					output = output.replace(((char)3)+ Integer.toString(ircColors[i]), "");
				}
				output = output.replace((char)3+ "", "").replace((char)2+ "", "").replace((char)29+ "", "").replace((char)15+ "", "").replace((char)31+ "", "");
			}
			else {
				String irccolor;
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) irccolor = "0" + ircColors[i];
					else irccolor=Integer.toString(ircColors[i]);
					output = output.replace("§" + gameColors[i].toLowerCase(), ((char)3)+ irccolor);
					output = output.replace("&" + gameColors[i].toLowerCase(), ((char)3)+ irccolor);
					output = output.replace("§" + gameColors[i].toUpperCase(), ((char)3)+ irccolor);
					output = output.replace("&" + gameColors[i].toUpperCase(), ((char)3)+ irccolor);
				}
				output = output.replace("^K", (char)3+ "").replace("^B", (char)2+ "").replace("^I", (char)29+ "").replace("^O", (char)15+ "").replace("^U", (char)31+ "");
			}
			return output;

		}
		else {
			String output = input;
			int i = 16;
			if (fromIRCtoGame) {
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) {
						output = output.replace(((char)3) + "0" + Integer.toString(ircColors[i]), "§" + gameColors[i]);
					}
					output = output.replace(((char)3)+ Integer.toString(ircColors[i]), "§" + gameColors[i]);
				}
				output = output.replace((char)3+ "", ChatColor.WHITE.toString()).replace((char)2+ "", "").replace((char)29+ "", "").replace((char)15+ "", "").replace((char)31+ "", "");
			}
			else {
				String irccolor;
				while (i > 0) {
					i--;
					if (ircColors[i] < 10) irccolor = "0" + ircColors[i];
					else irccolor=Integer.toString(ircColors[i]);
					output = output.replace(ChatColor.COLOR_CHAR+ gameColors[i].toLowerCase(), ((char)3)+ irccolor);
					output = output.replace("&" + gameColors[i].toLowerCase(), ((char)3)+ irccolor);
					output = output.replace(ChatColor.COLOR_CHAR+ gameColors[i].toUpperCase(), ((char)3)+ irccolor);
					output = output.replace("&" + gameColors[i].toUpperCase(), ((char)3)+ irccolor);
				}
				output = output.replace("^K", (char)3+ "").replace("^B", (char)2+ "").replace("^I", (char)29+ "").replace("^O", (char)15+ "").replace("^U", (char)31+ "");
			}
			return output;
		}
	}

	public static String stripFormatting(String input)
	{
		String output = input;
		int i = 16;
		while (i > 0) {
			i--;
			if (ircColors[i] < 10) output=output.replace("^K0" + i,"");
			output=output.replace("^K" + i,"");
		}
		output = output.replace("^K", "").replace("^B", "").replace("^I", "").replace("^O", "").replace("^U", "");
		return output;
	}

	public static void setTopic(String topic, String user, String userhost) {
		channelTopic = topic;
		channelTopicSetDate = System.currentTimeMillis() / 1000L;
		if (user.length() > 0) { channelTopicSet = user; }
		if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
			BukkitIRCdPlugin.ircd_topic = topic;
			BukkitIRCdPlugin.ircd_topicsetdate = System.currentTimeMillis();
			if (user.length() > 0) { BukkitIRCdPlugin.ircd_topicsetby = user; }
		}

		if (mode == Modes.STANDALONE) {
			writeAll(":" + userhost+ " TOPIC " + channelName + " :" + channelTopic);
			writeOpers(":" + userhost+ " TOPIC " + consoleChannelName + " :" + channelTopic);
		}
		else if (mode == Modes.INSPIRCD) {
			BukkitPlayer bp;
			if ((bp = getBukkitUserObject(user)) != null) {
				println(":" +bp.getUID() + " TOPIC " + channelName + " :" + channelTopic);
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
		String joined = "";
		int noOfItems = 0;
		for (String item : strArray) {
			if (noOfItems < start) { noOfItemS ++; continue; }
			joined += item;
			if (++ noOfItems < strArray.length)
				joined += delimiter;
		}
		return joined;
	}

	public static boolean executeCommand(String command) {
		try {
			if ((commandSender != null) && (bukkitServer != null)) {
				return bukkitServer.dispatchCommand(commandSender, convertColors(command, true));
			}
			else return false;
		} catch (Exception e) {
			commandSender.sendMessage("Exception in command \"" + command + "\": " +e);
			return false;
		}
	}

	public static boolean println(String line) {
		if ((server == null) || (!server.isConnected()) || (server.isClosed()) || (out == null)) return false;
		synchronized(csServer) {
			if (debugMode) System.out.println("[BukkitIRCd]§1[<-] " +line);
			out.println(line);
			return true;
		}
	}

	public static void disconnectServer(String reason) {
		if (reason == null) reason = "Disabling Plugin";
		synchronized(csServer) {
			if (mode == Modes.INSPIRCD) {
				 if ((server != null) && server.isConnected()) {
					println(pre + "SQUIT " + sID + " :" +reason);
					if (linkcompleted) {
						if (msgDelinkedReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgDelinkedReason.replace("%LINKNAME%",linkName).replace("%REASON%",reason));
						linkcompleted = false;
					}
					try { server.close(); } catch (IOException e) { }
				 }
				 else if (debugMode) System.out.println("[BukkitIRCd] Already disconnected from link, so no need to cleanup.");
			}
		}
		if (listener != null) try { listener.close(); } catch (IOException e) { }
	}
	
	public static String getUIDFromIRCUser(IRCUser user) {
		Iterator<Entry<String, IRCUser>> iter = uid2ircuser.entrySet().iterator();
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
		Iterator<Entry<String, IRCUser>> iter = uid2ircuser.entrySet().iterator();
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
		if (debugMode) BukkitIRCdPlugin.log.info("[BukkitIRCd]" + ChatColor.YELLOW + "[->] " + command);
				
		String split[] = command.split(" ");
		if (split.length <= 1) return;
		if (split[0].startsWith(":")) split[0] = split[0].substring(1);
		
		if (split[1].equalsIgnoreCase("PING")) {
			// Incoming ping, respond with pong so we don't get timed out from the server'
			// :280 PING 280 123
			linkLastPingPong = System.currentTimeMillis();
			if (split.length == 3) println( pre + "PONG " + split[2]);
			else if ((split.length == 4) && (split[3].equalsIgnoreCase(Integer.toString(SID)))) println( pre + "PONG " + SID + " " + split[2]);
		}
		else if (split[1].equalsIgnoreCase("PONG")) {
			// Received a pong, update the last ping pong timestamp.
			// :280 PONG 280 123
			linkLastPingPong = System.currentTimeMillis();
		}
		else if (split[1].equalsIgnoreCase("ERROR")) {
			// :280 ERROR :Unrecognised or malformed command 'CAPAB' -- possibly loaded mismatched modules
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			throw new IOException("Remote host rejected connection, probably configured wrong: " + join(split, " ", 2));
		}
		else if (split[1].equalsIgnoreCase("UID")) {
			// New user connected, add to IRC user list by UID);
			// :0IJ UID 0IJAAAAAP 1321966480 qlum ip565fad97.direct-adsl.nl 2ast9v.direct-adsl.nl qlum 86.95.173.151 1321966457 + i :purple
			String UID=split[2];
			long idleTime = Long.parseLong(split[3]) * 1000;
			String nick = split[4];
			String realhost = split[5];
			String vhost = split[6];
			String ident = split[7];
			String ipaddress = split[8];
			long signedOn = Long.parseLong(split[9]);
			if (split[11].startsWith(":")) split[11] = split[11].substring(1);
			String realname = join(split, " ", 11);
			boolean isRegistered = split[10].contains("r");
			boolean isOper = split[10].contains("o");
			IRCUser ircuser = new IRCUser(nick, realname, ident, realhost, vhost, ipaddress, "", "", isRegistered, false, "", signedOn, idleTime);
			ircuser.isRegistered = isRegistered;
			ircuser.isOper = isOper;
			uid2ircuser.put(UID, ircuser); // Add it to the hashmap
		}
		else if (split[1].equalsIgnoreCase("AWAY")) {
			// Away status updating
			// :0IJAAAAAE AWAY :Auto Away at Tue Nov 22 13:56:26 2011
			String UID = split[0];

			IRCUser iuser;
			if ((iuser = uid2ircuser.get(UID)) != null) {
				// Found the UID in the hashmap, update away message
				if (split.length > 2) {
					// New away message
					if (split[2].startsWith(":")) split[2] = split[2].substring(1);
					iuser.awayMsg = IRCd.join(split, " ", 2);
				}
				else {
					// Remove away status
					iuser.awayMsg = "";
				}
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + UID + " not found in list. Error code IRCd1707."); // Log this as severe since it should never occur unless something is wrong with the code
		}
		else if (split[1].equalsIgnoreCase("TIME")) {
			// TIME request from user
			// :123AAAAAA TIME :test.tempcraft.net
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			IRCUser iuser;
			if (split[2].equalsIgnoreCase(serverHostName)) { // Double check to make sure this request is for us
				if ((iuser = uid2ircuser.get(split[0])) != null) {
					println(pre + "PUSH " + split[0] + " ::" + serverHostName + " 391 " + iuser.nick + " " + serverHostName + " :" + dateFormat.format(System.currentTimeMillis()));
				}
			}
		}
		else if (split[1].equalsIgnoreCase("ENDBURST")) {
			// :280 ENDBURST
			if (split[0].equalsIgnoreCase(remoteSID) || split[0].equalsIgnoreCase(linkName)) {
				sendLinkBurst();
			}
		}
		else if (split[1].equalsIgnoreCase("SERVER")) {
			// :dev.tempcraft.net SERVER Esper.janus * 1 0JJ Esper
			String hub;
			try {
				if (split[0].equalsIgnoreCase(remoteSID) || split[0].equalsIgnoreCase(linkName)) {
					hub = remoteSID;
				}
				else {
					hub = split[0];
					IRCServer is = servers.get(hub);
					if (is == null) {
						Iterator<Entry<String, IRCServer>> iter = servers.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry<String, IRCServer> entry = iter.next();
							entry.getKey();
							IRCServer curServer = entry.getValue();
							if (curServer.host.equalsIgnoreCase(split[0])) { is = curServer; break; }
						}
					}
					if (is != null) is.leaves.add(split[5]);
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] Received invalid SERVER command, unknown hub server!"); 
				}
			} catch (NumberFormatException e) {
				hub = remoteSID;
				BukkitIRCdPlugin.log.severe("[BukkitIRCd] Received invalid SERVER command, unknown hub server!");
			}
			servers.put(split[5], new IRCServer(split[2], split[6], split[5], hub));
		}
		else if (split[1].equalsIgnoreCase("SQUIT")) {
			// :test.tempcraft.net SQUIT dev.tempcraft.net :Remote host closed connection
			String quitServer = split[2];
			if (quitServer.equalsIgnoreCase(linkName) || quitServer.equalsIgnoreCase(remoteSID)) disconnectServer("Remote server delinked");
			else {
				Iterator<Entry<String, IRCServer>> iter = servers.entrySet().iterator();
				IRCServer is = null;
				while (iter.hasNext()) {
					Map.Entry<String, IRCServer> entry = iter.next();
					is = entry.getValue();
					if (is.host.equalsIgnoreCase(quitServer) || is.SID.equalsIgnoreCase(quitServer)) {
						// Found the server in the list
						removeIRCUsersBySID(is.SID);
						break;
					}
				}
			}
		}
		else if (split[1].equalsIgnoreCase("OPERTYPE")) {
			// :123AAAAAA OPERTYPE IRC_Operator
			IRCUser ircuser;
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				ircuser.isOper = true;
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd1779."); // Log as severe because this situation should never occur and points to a bug in the code			
		}
		else if (split[1].equalsIgnoreCase("MODE")) {
			// :0KJAAAAAA MODE 0KJAAAAAA + w
			// Ignores other modes than o and r for now.
			// Also ignores the source UID - it's not needed and the UID/SID detection was buggy
			IRCUser ircusertarget;
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			//if (((ircusersource = uid2ircuser.get(split[0])) != null) || ((server = servers.get(split[0])) != null)) {
				if ((ircusertarget = uid2ircuser.get(split[2])) != null) {
					String modes = split[3];
					boolean add = true;
					for (int i = 0; i < modes.length(); i++) {
						if ((modes.charAt(i) + "").equals(" + ")) add = true;
						else if ((modes.charAt(i) + "").equals("-")) add = false;
						else if ((modes.charAt(i) + "").equals("o")) {
							if (add) ircusertarget.isOper = true;
							else ircusertarget.isOper = false;
						}
						else if ((modes.charAt(i) + "").equals("r")) {
							if (add) ircusertarget.isRegistered = true;
							else ircusertarget.isRegistered = false;
						}
					}
				}
				else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[2] + " not found in list. Error code IRCd1804."); // Log as severe because this situation should never occur and points to a bug in the code
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/SID " + split[0] + " not found in list. Error code IRCd1806."); // Log as severe because this situation should never occur and points to a bug in the code			
		}
		else if (split[1].equalsIgnoreCase("FJOIN")) {
			// :dev.tempcraft.net FJOIN #tempcraft.staff 1321829730 +tnsk MASTER-RACE :qa,0AJAAAAAA o,0IJAAAAAP v,0IJAAAAAQ
			if (split[2].equalsIgnoreCase(channelName)) {
				try {
					long tmp = Long.parseLong(split[3]);
					if (channelTS > tmp) channelTS = tmp; 
				} catch (NumberFormatException e) { }
				// Main channel
				String users[] = command.split(" ");
				for (String user : users) {
					if (!user.contains(",")) continue;
					String userSplit[] = user.split(",");
					IRCUser ircuser;
					if ((ircuser = uid2ircuser.get(userSplit[1])) != null) {
						ircuser.setModes(userSplit[0]);
						if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
							if (!ircuser.joined) {
								if (msgIRCJoin.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCJoin.replace("%USER%", ircuser.nick));
								if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCJoinDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCJoinDynmap.replace("%USER%", ircuser.nick));
							}
						}
						ircuser.joined = true;
					}
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + userSplit[1] + " not found in list. Error code IRCd1831."); // Log as severe because this situation should never occur and points to a bug in the code
				}
			}
			else if (split[2].equalsIgnoreCase(consoleChannelName)) {
				try {
					long tmp = Long.parseLong(split[3]);
					if (consoleChannelTS > tmp) consoleChannelTS = tmp; 
				} catch (NumberFormatException e) { }
				// Console channel
				String users[] = command.split(" ");
				for (String user : users) {
					if (!user.contains(",")) continue;
					String userSplit[] = user.split(",");
					IRCUser ircuser;
					if ((ircuser = uid2ircuser.get(userSplit[1])) != null) {
						ircuser.setConsoleModes(userSplit[0]);
						ircuser.consoleJoined = true;
					}
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + userSplit[1] + " not found in list. Error code IRCd1849."); // Log as severe because this situation should never occur and points to a bug in the code
				}
			}
			// Ignore other channels, since this plugin only cares about the main channel and console channel.
		}
		else if (split[1].equalsIgnoreCase("FHOST")) {
			// :0KJAAAAAA FHOST test
			IRCUser ircuser;
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				ircuser.hostmask = split[2];
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd1861."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		else if (split[1].equalsIgnoreCase("FNAME")) {
			// :0KJAAAAAA FNAME TEST
			IRCUser ircuser;
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				ircuser.realname = join(split, " ", 2);
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd1870."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		else if (split[1].equalsIgnoreCase("FMODE")) {
			// :0KJAAAAAA FMODE #tempcraft.staff 1320330110 +o 0KJAAAAAB
			IRCUser ircuser,ircusertarget;
			if (split.length >= 6) { // If it's not length 6, it's not a user mode
				if (split[0].startsWith(":")) split[0] = split[0].substring(1);

				if (split[2].equalsIgnoreCase(channelName)) try {
					long tmp = Long.parseLong(split[3]);
					if (channelTS > tmp) channelTS = tmp; 
				} catch (NumberFormatException e) { }
				else if (split[2].equalsIgnoreCase(consoleChannelName)) try {
					long tmp = Long.parseLong(split[3]);
					if (consoleChannelTS > tmp) consoleChannelTS = tmp; 
				} catch (NumberFormatException e) { }

				Boolean add = true;
				int modecount = 0;
				for (int i = 0; i < split[4].length(); i++) {
					if (5+modecount >= split.length) break;
					String user = split[5+modecount];
					if (user.startsWith(":")) user = user.substring(1);
					String mode = split[4].charAt(i) + "";
					if (mode.equals(" + ")) add = true;
					else if (mode.equals("-")) add = false;
					else {
						if ((ircusertarget = uid2ircuser.get(user)) != null) {
							if (split[2].equalsIgnoreCase(channelName)) {
								String textModes = ircusertarget.getTextModes();
								if (add) {
									System.out.println("Adding mode " + mode + " for " + ircusertarget.nick);
									if (!textModes.contains(mode)) ircusertarget.setModes(textModeS +mode);
								}
								else {
									System.out.println("Removing mode " + mode + " for " + ircusertarget.nick);
									if (textModes.contains(mode)) ircusertarget.setModes(textModes.replace(mode,""));
								}
							}
							else if (split[2].equalsIgnoreCase(consoleChannelName)) {
								String consoleTextModes = ircusertarget.getConsoleTextModes();
								if (add) {
									System.out.println("Adding console mode " + mode + " for " + ircusertarget.nick);
									if (!consoleTextModes.contains(mode)) ircusertarget.setConsoleModes(consoleTextModeS +mode);
								}
								else {
									System.out.println("Removing console mode " + mode + " for " + ircusertarget.nick);
									if (consoleTextModes.contains(mode)) ircusertarget.setConsoleModes(consoleTextModes.replace(mode,""));
								}
							}
						}
						else if (IRCd.wildCardMatch(user, "*!*@*")) {
							if (mode.equals("b")) {
								if ((ircuser = uid2ircuser.get(split[0])) != null) {
									if (add) {
										if (msgIRCBan.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCBan.replace("%BANNEDUSER%", user).replace("%BANNEDBY%", ircuser.nick));
										if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCBanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCBanDynmap.replace("%BANNEDUSER%", user).replace("%BANNEDBY%", ircuser.nick));
									}
									else {
										if (msgIRCUnban.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCUnban.replace("%BANNEDUSER%", user).replace("%BANNEDBY%", ircuser.nick));
										if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCUnbanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCUnbanDynmap.replace("%BANNEDUSER%", user).replace("%BANNEDBY%", ircuser.nick));
									}
								}
							}
						}
						modecount++;
					}
				}
			}
		}
		else if (split[1].equalsIgnoreCase("FTOPIC")) {
			// :dev.tempcraft.net FTOPIC #tempcraft.survival 1322061484 Jdbye/ingame '4HI'"
			if (split[2].equalsIgnoreCase(channelName)) {
				// Main channel
				String user = split[4];
				if (split[5].startsWith(":")) split[5] = split[5].substring(1);
				String topic = join(split, " ", 5);
				
				channelTopic = topic;
				try { channelTopicSetDate = Long.parseLong(split[3]); } catch (NumberFormatException e) { }
				channelTopicSet = user;
				if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
					BukkitIRCdPlugin.ircd_topic = topic;
					BukkitIRCdPlugin.ircd_topicsetdate = channelTopicSetDate * 1000;
					BukkitIRCdPlugin.ircd_topicsetby = user;
				}
			}
			else if (split[2].equalsIgnoreCase(consoleChannelName)) {
				// This is of no interest to us
			}
			// Ignore other channels, since this plugin only cares about the main channel and console channel.
		}
		else if (split[1].equalsIgnoreCase("TOPIC")) {
			// :0KJAAAAAA TOPIC #tempcraft.survival :7Welcome to #tempcraft.survival! | 10Server's 3ONLINE | 3Visit our site: 14http://TempCraft.net/ | 4Vote for us: 14http://tempcraft.net/?act=vote | 4Join our forums: 14http://stormbit.net/ | Don't change the separators to white
			if (split[2].equalsIgnoreCase(channelName)) {
				// Main channel
				String UID = split[0];
				if (split[3].startsWith(":")) split[3] = split[3].substring(1);
				String topic = join(split, " ", 3);

				IRCUser ircuser = null;
				IRCServer server = null;
				if (((ircuser = uid2ircuser.get(UID)) != null) || ((server = servers.get(UID)) != null)) {
					String user;
					if (ircuser != null) user = ircuser.nick;
					else user = server.host;
					channelTopic = topic;
					channelTopicSetDate = System.currentTimeMillis() / 1000L;
					channelTopicSet = user;
					if ((isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
						BukkitIRCdPlugin.ircd_topic = topic;
						BukkitIRCdPlugin.ircd_topicsetdate = channelTopicSetDate * 1000;
						BukkitIRCdPlugin.ircd_topicsetby = user;
					}
				}
				else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/SID " + uID + " not found in list. Error code IRCd1985."); // Log as severe because this situation should never occur and points to a bug in the code
			}
			else if (split[2].equalsIgnoreCase(consoleChannelName)) {
				// This is of no interest to us
			}
			// Ignore other channels, since this plugin only cares about the main channel and console channel.
		}
		else if (split[1].equalsIgnoreCase("IDLE")) {
			// IN  :<uuid> IDLE <target uuid>
			// OUT :<uuid> IDLE <target uuid> <signon> <seconds idle>
			IRCUser ircuser;
			if ((ircuser = uid2ircuser.get(split[2])) != null) {
				println(":" + split[2] + " IDLE " + split[0] + " " + ircuser.signonTime + " " + ircuser.getSecondsIdle());
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[2] + " not found in list. Error code IRCd1999."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		else if (split[1].equalsIgnoreCase("NICK")) {
			// :280AAAAAA NICK test 1321981244
			IRCUser ircuser;
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				BukkitIRCdPlugin.thePlugin.updateLastReceived(ircuser.nick, split[2]);
				if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null) && (ircuser.joined)) {
					if (msgIRCNickChange.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCNickChange.replace("%OLDNICK%",ircuser.nick).replace("%NEWNICK%",split[2]));
					if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCNickChangeDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCNickChangeDynmap.replace("%OLDNICK%",ircuser.nick).replace("%NEWNICK%",split[2]));
				}
				ircuser.nick = split[2];
			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[2] + " not found in list. Error code IRCd2013."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		else if (split[1].equalsIgnoreCase("KICK")) {
			// :280AAAAAA KICK #tempcraft.survival 280AAAAAB :reason
			IRCUser ircuser;
			IRCUser ircvictim;
			IRCServer server = null;
			String kicker,kicked;
			String reason;
			if (split.length > 4) {
				reason = join(split, " ", 4);
				if (reason.startsWith(":")) reason = reason.substring(1);
			}
			else reason = null;

			if (split[3].startsWith(Integer.toString(SID))) {
				if (split[2].equalsIgnoreCase(channelName)) {
					if (split[3].equalsIgnoreCase(serverUID)) {
						println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + serverUID);
						println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " +qaohv " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID);

					}
					else if (((ircuser = uid2ircuser.get(split[0])) != null) || ((server = servers.get(split[0])) != null)) {
						String user;
						if (ircuser != null) user = ircuser.nick;
						else user = server.host;
						
						BukkitPlayer bp;
						if ((bp = getBukkitUserByUID(split[3])) != null) {
							if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								Player p = BukkitIRCdPlugin.thePlugin.getServer().getPlayer(bp.nick);
								if (p != null) {
									if (reason != null) p.kickPlayer("Kicked by " + user + " on IRC: " +reason);
									else p.kickPlayer("Kicked by " + user + " on IRC");
								}
								removeBukkitUserByUID(split[3]);
							}
						}
						else BukkitIRCdPlugin.log.severe("[BukkitIRCd] Bukkit Player UID " + split[3] + " not found in list. Error code IRCd2051."); // Log as severe because this situation should never occur and points to a bug in the code
					}
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/SID " + split[0] + " not found in list. Error code IRCd2053."); // Log as severe because this situation should never occur and points to a bug in the code
				}
				else if (split[2].equalsIgnoreCase(consoleChannelName)) {
					if (split[3].equalsIgnoreCase(serverUID)) {
						println(pre + "FJOIN " + consoleChannelName + " " + consoleChannelTS + " + nt :," + serverUID);
						println(":" + serverUID + " FMODE " + consoleChannelName + " " + consoleChannelTS + " +qaohv " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID);

					}
				}
			}
			else {
				if (split[2].equalsIgnoreCase(channelName)) {
					// Main channel
					if (((ircuser = uid2ircuser.get(split[0])) != null) || ((server = servers.get(split[0])) != null)) {
						if (ircuser != null) kicker = ircuser.nick;
						else kicker = server.host;
						if ((ircvictim = uid2ircuser.get(split[3])) != null) {
							kicked = ircvictim.nick;
							if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								if (reason != null) {
									if (msgIRCKickReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCKickReason.replace("%KICKEDUSER%",kicked).replace("%KICKEDBY%", kicker).replace("%REASON%", convertColors(reason,true)));
									if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCKickReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCKickReasonDynmap.replace("%KICKEDUSER%",kicked).replace("%KICKEDBY%", kicker).replace("%REASON%", stripFormatting(reason)));
								}
								else {
									if (msgIRCKick.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCKick.replace("%KICKEDUSER%",kicked).replace("%KICKEDBY%", kicker));
									if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCKickDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCKickDynmap.replace("%KICKEDUSER%",kicked).replace("%KICKEDBY%", kicker));
								}
								ircvictim.joined = false;
							}
						}
						else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[3] + " not found in list. Error code IRCd2083."); // Log as severe because this situation should never occur and points to a bug in the code
					}
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/SID " + split[0] + " not found in list. Error code IRCd2085."); // Log as severe because this situation should never occur and points to a bug in the code
				}
				else if (split[2].equalsIgnoreCase(consoleChannelName)) {
					// Console channel
					// Only thing important here is to set consolemodes to blank so they can't execute commands on the console channel anymore
					if ((ircvictim = uid2ircuser.get(split[3])) != null) {
						ircvictim.setConsoleModes("");
						ircvictim.consoleJoined = false;
					}
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[3] + " not found in list. Error code IRCd2094."); // Log as severe because this situation should never occur and points to a bug in the code
				}
			}
		}
		else if (split[1].equalsIgnoreCase("PART")) {
			// :280AAAAAA PART #tempcraft.survival :message
			IRCUser ircuser;
			String reason;
			if (split.length > 3) {
				reason = join(split, " ", 3);
				if (reason.startsWith(":")) reason = reason.substring(1);
			}
			else reason = null;
			
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if (split[2].equalsIgnoreCase(channelName)) {
				// Main channel
				if ((ircuser = uid2ircuser.get(split[0])) != null) {
					if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
						if (reason != null) {
							if (msgIRCLeaveReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeaveReason.replace("%USER%", ircuser.nick).replace("%REASON%", convertColors(reason, true)));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveReasonDynmap.replace("%USER%", ircuser.nick).replace("%REASON%", stripFormatting(reason)));
						}
						else {
							if (msgIRCLeave.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeave.replace("%USER%", ircuser.nick));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveDynmap.replace("%USER%", ircuser.nick));
						}
						ircuser.joined = false;
						ircuser.setModes("");
					}
				}
				else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd2125."); // Log as severe because this situation should never occur and points to a bug in the code
			}
			else if (split[2].equalsIgnoreCase(consoleChannelName)) {
				// Console channel
				// Only thing important here is to set oper to false so they can't execute commands on the console channel without being in it
				if ((ircuser = uid2ircuser.get(split[0])) != null) {
					ircuser.setConsoleModes("");
					ircuser.consoleJoined = false;
				}
				else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd2134."); // Log as severe because this situation should never occur and points to a bug in the code
			}
		}
		else if (split[1].equalsIgnoreCase("QUIT")) {
			// :280AAAAAB QUIT :Quit: Connection reset by beer
			IRCUser ircuser;
			String reason;
			if (split.length > 2) {
				reason = join(split, " ", 2);
				if (reason.startsWith(":")) reason = reason.substring(1);
			}
			else reason = null;
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				if (ircuser.joined) {
					// This user is on the plugin channel so broadcast the PART ingame
					if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
						if (reason != null) {
							if (msgIRCLeaveReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeaveReason.replace("%USER%", ircuser.nick).replace("%REASON%", convertColors(reason, true)));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveReasonDynmap.replace("%USER%", ircuser.nick).replace("%REASON%", stripFormatting(reason)));
						}
						else {
							if (msgIRCLeave.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeave.replace("%USER%", ircuser.nick));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveDynmap.replace("%USER%", ircuser.nick));
						}
					}
					ircuser.setConsoleModes("");
					ircuser.setModes("");
					ircuser.joined = false;
					ircuser.consoleJoined = false;
				}
				uid2ircuser.remove(split[0]);
			}			
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd2166."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		else if (split[1].equalsIgnoreCase("KILL")) {
			// :280AAAAAA KILL 123AAAAAA :Killed (test (testng))
			
			// If an ingame user is killed, reconnect them to IRC.
			IRCUser ircuser,ircuser2;
			IRCServer server = null;
			String user;
			if ((((ircuser = uid2ircuser.get(split[0])) != null)) || ((server = servers.get(split[0])) != null)) {
				if (ircuser != null) user = ircuser.nick;
				else user = server.host;
				synchronized(csBukkitPlayers) {
					BukkitPlayer bp;
					if (split[2].equalsIgnoreCase(serverUID)) {
						println(pre + "UID " + serverUID + " " + serverStartTime + " " + serverName + " " + serverHostName + " " + serverHostName + " " + serverName + " 127.0.0.1 " + serverStartTime + " +Bro :" +version);
						println(":" + serverUID + " OPERTYPE Network_Service");
						println(pre + "FJOIN " + consoleChannelName + " " + consoleChannelTS + " + nt :," + serverUID);
						println(":" + serverUID + " FMODE " + consoleChannelName + " " + consoleChannelTS + " +qaohv " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID);
						println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + serverUID);
						println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " +qaohv " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID + " " + serverUID);
					}
					else if ((bp = getBukkitUserByUID(split[2])) != null) {
						String UID = bp.getUID();
						String textMode = bp.getTextMode();
						if (bp.hasPermission("bukkitircd.oper")) {
							println(pre + "UID " + uID + " " +(bp.idleTime / 1000L) + " " +bp.nick + ingameSuffix + " " +bp.host+ " " +bp.host+ " " +bp.nick + " " +bp.ip+ " " +bp.signedOn + " +or :Minecraft Player");
							println(":" + uID + " OPERTYPE IRC_Operator");
						}
						else println(pre + "UID " + uID + " " +(bp.idleTime / 1000L) + " " +bp.nick + ingameSuffix + " " +bp.host+ " " +bp.host+ " " +bp.nick + " " +bp.ip+ " " +bp.signedOn + " +r :Minecraft Player");

						println(pre + "FJOIN " + channelName + " " + channelTS + " + nt :," + uID);
						if (textMode.length() > 0) {
							String modestr = "";
							for (int i = 0; i < textMode.length(); i++) {
								modestr += UID + " ";
							}
							modestr = modestr.substring(0, modestr.length()-1);
							println(":" + serverUID + " FMODE " + channelName + " " + channelTS + " + " +textMode + " " +modestr);
						}
						String world = bp.getWorld();
						if (world != null) println(pre + "METADATA " + UID + " swhois :is currently in " + world);
						else println(pre + "METADATA " + UID + " swhois :is currently in an unknown world");
					}
					else if ((ircuser2 = uid2ircuser.get(split[2])) != null) {
						String reason;
						reason = join(split, " ", 3);
						if (reason.startsWith(":")) reason = reason.substring(1);
						if (ircuser2.joined) {
							if (msgIRCLeaveReason.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgIRCLeaveReason.replace("%USER%", user).replace("%REASON%", convertColors(reason, true)));
							if ((BukkitIRCdPlugin.dynmap != null) && (msgIRCLeaveReasonDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgIRCLeaveReasonDynmap.replace("%USER%", user).replace("%REASON%", stripFormatting(reason)));
							ircuser2.setConsoleModes("");
							ircuser2.setModes("");
							ircuser2.joined = false;
							ircuser2.consoleJoined = false;
						}
						uid2ircuser.remove(split[2]);
					}
					else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[2] + " not found in list. Error code IRCd2224."); // Log as severe because this situation should never occur and points to a bug in the code
				}

			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID/SID " + split[0] + " not found in list. Error code IRCd2228."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		else if (split[1].equalsIgnoreCase("PRIVMSG") || split[1].equalsIgnoreCase("NOTICE")) {
			// :280AAAAAA PRIVMSG 123AAAAAA :test
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			String message = join(split, " ", 3);
			String msgtemplate = "";
			String msgtemplatedynmap = "";
			boolean isCTCP = (message.startsWith((char)1+ "") && message.endsWith((char)1+ ""));
			boolean isAction = (message.startsWith((char)1+ "ACTION") && message.endsWith((char)1+ ""));
			boolean isNotice = split[1].equalsIgnoreCase("NOTICE");
			if (isCTCP && (!isAction)) return; // Ignore CTCP's (except actions)
			else if (isCTCP && isNotice) return; // CTCP reply, ignore this
			
			if (isNotice && (!enableNotices)) return; // Ignore notices if notices are disabled.
			
			IRCUser ircuser;
			String uidfrom = split[0];
			if ((ircuser = uid2ircuser.get(split[0])) != null) {
				synchronized(csBukkitPlayers) {
					BukkitPlayer bp;
					if (split[2].equalsIgnoreCase(channelName)) { // Messaging the public channel
						if (isAction) {
							msgtemplate = msgIRCAction;
							msgtemplatedynmap = msgIRCActionDynmap;
							message = IRCd.join(message.substring(1,message.length()-1).split(" "), " ", 1);
						}
						else if (isNotice) {
							msgtemplate = msgIRCNotice;
							msgtemplatedynmap = msgIRCNoticeDynmap;
						}
						else {
							msgtemplate = msgIRCMessage;
							msgtemplatedynmap = msgIRCMessageDynmap;
						}
						if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
							if (message.equalsIgnoreCase("!players") && (!isAction) && (!isNotice)) {
								if (msgPlayerList.length() > 0) {
									String s = "";
									int count = 0;
									for (BukkitPlayer player : bukkitPlayers) { count++; s = s + player.nick + ", "; }
									if (s.length() == 0) s = "None, ";
									println(":" + serverUID + " PRIVMSG " + IRCd.channelName + " :" + convertColors(msgPlayerList.replace("%COUNT%", Integer.toString(count)).replace("%USERS%", s.substring(0, s.length()-2)), false));
								}
							}
							else {
								if (msgtemplate.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(msgtemplate.replace("%USER%", ircuser.nick).replace("%MESSAGE%", IRCd.convertColors(message,true)));
								if ((BukkitIRCdPlugin.dynmap != null) && (msgtemplatedynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", msgtemplatedynmap.replace("%USER%", ircuser.nick).replace("%MESSAGE%", stripFormatting(message)));
							}
						}
					}
					else if (split[2].equalsIgnoreCase(consoleChannelName)) { // Messaging the console channel
						if (message.startsWith("!") && (!isAction) && (!isNotice)) {
							if (!ircuser.getConsoleTextModes().contains("o")) println(":" + serverUID + " NOTICE " + uidfrom + " :You don't have access."); // Only let them execute commands if they're oper
							else {
								message = message.substring(1);
								if (!executeCommand(message)) {
									println(":" + serverUID + " PRIVMSG " + IRCd.consoleChannelName + " :Failed to execute command.");
								}
								else {
									println(":" + serverUID + " PRIVMSG " + IRCd.consoleChannelName + " :Command executed.");
								}
							}
						}
					}
					else if (split[2].equalsIgnoreCase(serverUID)) { // Messaging the console
						if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
							if (isAction) {
								msgtemplate = msgIRCPrivateAction;
								message = IRCd.join(message.substring(1,message.length()-1).split(" "), " ", 1);
							}
							else if (isNotice) {
								msgtemplate = msgIRCPrivateNotice;
							}
							else {
								msgtemplate = msgIRCPrivateMessage;
							}
							
							BukkitIRCdPlugin.thePlugin.setLastReceived("@CONSOLE@", ircuser.nick);
							if (msgtemplate.length() > 0) BukkitIRCdPlugin.log.info(msgtemplate.replace("%USER%", ircuser.nick).replace("%MESSAGE%", IRCd.convertColors(message,true)));
						}	
					}
					else if ((bp = getBukkitUserByUID(split[2])) != null) { // Messaging an ingame user
						if ((isAction || (!isCTCP)) && (IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
							if (isAction) {
								msgtemplate = msgIRCPrivateAction;
								message = IRCd.join(message.substring(1,message.length()-1).split(" "), " ", 1);
							}
							else if (isNotice) {
								msgtemplate = msgIRCPrivateNotice;
							}
							else {
								msgtemplate = msgIRCPrivateMessage;
							}

							synchronized(IRCd.csBukkitPlayers) {
								Player player = BukkitIRCdPlugin.thePlugin.getServer().getPlayer(bp.nick);
								String bukkitnick = player.getName();
								BukkitIRCdPlugin.thePlugin.setLastReceived(bukkitnick, ircuser.nick);
								if (msgtemplate.length() > 0) player.sendMessage(msgtemplate.replace("%USER%", ircuser.nick).replace("%MESSAGE%", IRCd.convertColors(message,true)));
							}
						}
					}
					// Ignore messages from other channels
				}

			}
			else BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] + " not found in list. Error code IRCd2336."); // Log as severe because this situation should never occur and points to a bug in the code
		}
		// End of IF command check
	}
}

	class ClientConnection implements Runnable {
		private Socket server;
		private String line;
		public String nick,realname,ident,hostmask,ipaddress;
		public String modes="";
		public String customWhois=""; // Not used yet
		public boolean isIdented = false;
		public boolean isNickSet = false;
		public boolean isRegistered = false;
		public boolean isOper = false;
		public String awayMsg = "";
		public long lastPingResponse;
		public long signonTime;
		public long lastActivity;
		private BufferedReader in;
		private PrintStream out;
		public boolean running = true;


		Server bukkitServer = null;
		IRCCommandSender commandSender = null;

		ClientConnection(Socket server) {
			this.server=server;
			try { this.server.setSoTimeout(3000); } catch (SocketException e) { }
		}

		public void run () {
			if (running) {
				try {
					nick = "";
					in = new BufferedReader(new InputStreamReader(server.getInputStream()));
					out = new PrintStream(server.getOutputStream());

					hostmask = server.getInetAddress().getHostName().toString();
					ipaddress = server.getInetAddress().getHostAddress().toString();
					Thread.currentThread().setName("Thread-BukkitIRCd-Connection-" + ipaddress);
					synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Got connection from " + ipaddress); }

					lastPingResponse = System.currentTimeMillis();
					lastActivity = lastPingResponse;

					if ((IRCd.isBanned(nick + "!" + ident + "@" + hostmask)) || (IRCd.isBanned(nick + "!" + ident + "@" + ipaddress))) {
						writeln("ERROR :Closing Link: [" + ipaddress + "] (You are banned from this server)");
						disconnect();
						synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Cleaning up connection from " + getFullHost() + " (banned)"); }
						if (isIdented && isNickSet) IRCd.writeAll(":" + getFullHost() + " QUIT :You are banned from this server");
						IRCd.removeIRCUser(nick, "Banned", true);
					}
					else while (server.isConnected() && (!server.isClosed())) {
						try {
							if (lastPingResponse +(IRCd.timeoutInterval*1000) < System.currentTimeMillis()) {
								writeln("ERROR :Closing Link: [" + ipaddress + "] (Ping timeout)");writeln("ERROR :Closing Link: [" + ipaddress + "] (Ping timeout)");
								disconnect();;
								synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Cleaning up connection from " + getFullHost() + " (ping timeout)"); }
								if (isIdented && isNickSet) IRCd.writeAll(":" + getFullHost() + " QUIT :Ping timeout");
								IRCd.removeIRCUser(nick, "Ping timeout", true);
							}
							else {
								// Get input from the client
								while((line = in.readLine()) != null && !line.equals(".")) {
									parseMessage(line);
								}
							}
						} catch (SocketTimeoutException e) { }
						try { Thread.currentThread();
						Thread.sleep(1); } catch(InterruptedException e){ }
					}
				} catch (IOException ioe) {
					synchronized(IRCd.csStdOut) {
						System.out.println("[BukkitIRCd] IOException on socket connection: " + ioe);
					}
				} 

				synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Cleaning up connection from " + getFullHost() + " (client quit)"); }
				IRCd.removeIRCUser(nick);
				running = false;
				synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Lost connection from " + getFullHost()); }
			}
		}

		private void parseMessage(String line)
		{
			String[] split = line.split(" ");
			if (split[0].equalsIgnoreCase("NICK")) {
				if (split.length >= 2) { if (split[1].indexOf(":") == 0) { split[1] = split[1].substring(1); } }
				if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 431  :No nickname given"); }
				else if (!split[1].matches("\\A[a-zA-Z_\\-\\[\\]\\\\^{}|`][a-zA-Z0-9_\\-\\[\\]\\\\^{}|`]*\\z")) { writeln(IRCd.serverMessagePrefix + " 432 " + nick + " " + split[1] + " :Erroneous Nickname: Illegal characters"); }
				else {
					if (split[1].length() > IRCd.nickLen) { split[1] = split[1].substring(0, IRCd.nickLen); }
					if ((IRCd.getIRCUser(split[1]) != null) || (split[1].equalsIgnoreCase(IRCd.serverName))) { writeln(IRCd.serverMessagePrefix + " 433 * " + split[1] + " :Nickname is already in use."); }
					else {
						if (!isNickSet) {
							isNickSet = true; nick = split[1];
							if (isIdented) {
								sendWelcome();
							}
						}
						else if (isIdented) {
							lastActivity = System.currentTimeMillis();

							BukkitIRCdPlugin.thePlugin.updateLastReceived(nick, split[1]);
							if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								if (IRCd.msgIRCNickChange.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCNickChange.replace("%OLDNICK%",nick).replace("%NEWNICK%",split[1]));
								if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCNickChangeDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCNickChangeDynmap.replace("%OLDNICK%",nick).replace("%NEWNICK%",split[1]));
							}
							IRCd.writeAll(":" + getFullHost() + " NICK " + split[1]);
							nick = split[1];
						}
					}
				}
			}
			else if (split[0].equalsIgnoreCase("USER")) {
				if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  USER :Not enough parameters"); }
				else {
					if (split[4].indexOf(":") == 0) { split[4] = split[4].substring(1); }
					if (!isIdented) {
						isIdented = true;
						ident = split[1];
						realname = split[4];
						if (isNickSet) {
							sendWelcome();
						}
					}
					else { writeln(IRCd.serverMessagePrefix + " 462 " + nick + " :You are already registered"); }
				}
			}
			else if (split[0].equalsIgnoreCase("QUIT")) {
				String quitmsg = null;
				if (split.length > 1) {
					if (split[1].indexOf(":") == 0) { split[1] = split[1].substring(1); }
					quitmsg = IRCd.join(split, " ", 1);
					if (isIdented && isNickSet) IRCd.writeAll(":" + getFullHost() + " QUIT :Quit: " +quitmsg);
					synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Cleaning up connection from " + getFullHost() + " (Quit: " +quitmsg+ ")"); }
				}
				else {
					if (isIdented && isNickSet) IRCd.writeAll(":" + getFullHost() + " QUIT :Quit");
					synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Cleaning up connection from " + getFullHost() + " (Quit)"); }
				}
				disconnect();
				if (quitmsg != null) IRCd.removeIRCUser(nick, quitmsg, true);
				else IRCd.removeIRCUser(nick);
			}
			else if (isIdented && isNickSet) {
				//if (split[0].equalsIgnoreCase("STOP")) {
				//	System.exit(0);
				//}
				if (split[0].equalsIgnoreCase("PING")) {
					if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						if (split[1].indexOf(":") == 0) { split[1] = split[1].substring(1); }
						writeln("PONG :" + IRCd.join(split," ",1));
					}
				}
				else if (split[0].equalsIgnoreCase("PONG")) {
					lastPingResponse = System.currentTimeMillis();
				}
				else if (split[0].equalsIgnoreCase("MOTD")) {
					lastActivity = System.currentTimeMillis();
					sendMOTD();
				}
				else if (split[0].equalsIgnoreCase("WHOIS")) {
					lastActivity = System.currentTimeMillis();
					if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						if (split[1].indexOf(":") == 0) { split[1] = split[1].substring(1); }
						sendWhois(split[1]);
					}
				}
				else if (split[0].equalsIgnoreCase("NAMES")) {
					if (split.length > 1) {
						if (!sendChanNames(split[1])) { writeln(IRCd.serverMessagePrefix + " 366 " + nick + " " + split[1] + " :End of /NAMES list."); }
					}
				}
				else if (split[0].equalsIgnoreCase("TOPIC")) {
					lastActivity = System.currentTimeMillis();
					if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else if (split.length == 2) {
						if (!sendChanTopic(split[1])) writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + split[1] + " :No such nick/channel");
					}
					else {
						if (split[2].indexOf(":") == 0) { split[2] = split[2].substring(1); }
						if (split[1].equalsIgnoreCase(IRCd.channelName)) {
							String chantopic = IRCd.join(split, " ", 2);
							if (modes.contains("%") || modes.contains("@") || modes.contains("&") || modes.contains("~")) {
								IRCd.setTopic(chantopic, nick, getFullHost());
							}
							else {
								// Not op
								writeln(IRCd.serverMessagePrefix + " 482 " + nick + " " + IRCd.channelName + " :You're not channel operator");	
							}
						}
						else if (split[1].equalsIgnoreCase(IRCd.consoleChannelName)) { } // Do nothing
						else { writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + split[1] + " :No such nick/channel"); }
					}
				}
				else if (split[0].equalsIgnoreCase("MODE")) {
					if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else if (split.length == 2) { 
						if (!sendChanModes(split[1])) writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + split[1] + " :No such nick/channel");
					}
					else {
						if (split[1].equalsIgnoreCase(IRCd.consoleChannelName)) { } // Do nothing
						else if (split[1].equalsIgnoreCase(IRCd.channelName)) {

							if (split[2].indexOf(":") == 0) { split[2] = split[2].substring(1); }

							int add = -1;
							int i = 0, i2 = 0;
							if (split.length == 3) {
								if ((split[2].equals(" +b")) || (split[2].equals("-b"))) {
									// Send list of bans
									synchronized(IRCd.csIrcBans) {
										for (IrcBan ban : IRCd.ircBans) {
											writeln(IRCd.serverMessagePrefix + " 367 " + nick + " " + IRCd.channelName + " " +ban.fullHost+ " " +ban.bannedBy+ " " +ban.banTime);
										}
										writeln(IRCd.serverMessagePrefix + " 368 " + nick + " " + IRCd.channelName + " :End of Channel Ban List");
									}
								}
							}
							else while (i < split[2].length()) {
								if (split[2].substring(i,i+1).equals(" + ")) add = 1;
								else if (split[2].substring(i,i+1).equals("-")) add = 0;
								else if (split[2].substring(i,i+1).equals("b")) {
									if (i2+3 < split.length) {
										String mask = split[i2+3];
										// They actually want to ban/unban someone
										if (modes.contains("%") || modes.contains("@") || modes.contains("&") || modes.contains("~")) {
											// User is op
											String host;
											if (IRCd.wildCardMatch(mask, "*!*@*")) host = mask;
											else if (IRCd.wildCardMatch(mask, "*!*")) host = mask + "@*";
											else if (IRCd.wildCardMatch(mask, "*@*")) host = "*!" +mask;
											else host = mask + "!*@*";
											if (add == 1) {
												if (IRCd.msgIRCBan.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCBan.replace("%BANNEDUSER%", host).replace("%BANNEDBY%", nick));
												if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCBanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCBanDynmap.replace("%BANNEDUSER%", host).replace("%BANNEDBY%", nick));
												IRCd.banIRCUser(host, getFullHost());
											}
											else if (add == 0) {
												if (IRCd.msgIRCUnban.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCUnban.replace("%BANNEDUSER%", host).replace("%BANNEDBY%", nick));
												if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCUnbanDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCUnbanDynmap.replace("%BANNEDUSER%", host).replace("%BANNEDBY%", nick));
												IRCd.unBanIRCUser(host, getFullHost());										
											}

										}
										else {
											// Not op
											writeln(IRCd.serverMessagePrefix + " 482 " + nick + " " + IRCd.channelName + " :You're not channel operator");	
											break;
										}
									}
									i2++;
								}
								i++;
							}
						}
						else if (split[1].equalsIgnoreCase(nick)) {
							if ((isOper) && (split[2].startsWith("-")) && (split[2].contains("o"))) {
								// Deoper
								isOper = false;
								writeln(":" + nick + " MODE " + nick + " :-o");
								IRCd.writeAll(":" + getFullHost() + " PART " + IRCd.consoleChannelName + " :De-opered");
							}
						}
						else { writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + split[1] + " :No such nick/channel"); }
					}

				}
				else if (split[0].equalsIgnoreCase("USERHOST")) {
					if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						int i = 1;
						String hosts = "";
						while (i < split.length) {
							if (split[i].indexOf(":") == 0) { split[i] = split[i].substring(1); }
							int ID;
							IRCUser ircuser;
							String targethost = null, targetnick = null, targetident = null;
							if (split[i].equalsIgnoreCase(IRCd.serverName)) {
								targetnick = IRCd.serverName;
								targetident = IRCd.serverName;
								targethost = IRCd.serverHostName;
								hosts += targetnick + "=+ " +targetident+ "@" + targethost + " ";
							}
							else if ((ircuser = IRCd.getIRCUser(split[i])) != null) {
								synchronized(IRCd.csIrcUsers) {
									targetnick = ircuser.nick;
									targetident = ircuser.ident;
									targethost = ircuser.hostmask;
								}
								hosts += targetnick + "=+ " +targetident+ "@" + targethost + " ";
							}
							else if ((ID = IRCd.getBukkitUser(split[i])) >= 0) {
								if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) { 
									synchronized(IRCd.csBukkitPlayers) {
										BukkitPlayer p = IRCd.bukkitPlayers.get(ID);
										targetnick = p.nick + IRCd.ingameSuffix;
										targetident = p.nick;
										targethost = p.host;
									}
								}
								hosts += targetnick + "=+ " +targetident+ "@" + targethost + " ";
							}
							i++;
							if (i > 5) break;
						}
						if (hosts.length() > 0) hosts = hosts.substring(0, hosts.length()-1);
						writeln(IRCd.serverMessagePrefix + " 302 " + nick + " :" + hosts);
					}
				}
				else if (split[0].equalsIgnoreCase("KICK")) {
					if (split.length < 3) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						// Kick someone
						if (modes.contains("%") || modes.contains("@") || modes.contains("&") || modes.contains("~")) {
							// User is op
							String bannick = split[2];
							String reason = null;
							if (split.length > 3) {
								if (split[3].indexOf(":") == 0) { split[3] = split[3].substring(1); }
								reason = IRCd.join(split, " ", 3);
							}

							IRCUser ircuser;
							if ((ircuser = IRCd.getIRCUser(bannick)) != null) {
								IRCd.kickIRCUser(ircuser, nick, ident, hostmask, reason, false);
							}
							else if ((IRCd.getBukkitUser(bannick)) >= 0) {
								if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
									if (bannick.endsWith(IRCd.ingameSuffix)) bannick = bannick.substring(0, bannick.length()-IRCd.ingameSuffix.length()); 
									Server s = BukkitIRCdPlugin.thePlugin.getServer();
									Player p = s.getPlayer(bannick);
									if (p != null) {
										if (reason != null) {
											if (IRCd.msgIRCKickReason.length() > 0) s.broadcastMessage(IRCd.msgIRCKickReason.replace("%KICKEDUSER%", bannick).replace("%KICKEDBY%", nick).replace("%REASON%", IRCd.convertColors(reason, true)));
											p.kickPlayer("Kicked by " + nick + " on IRC: " + IRCd.stripFormatting(reason));
										}
										else {
											if (IRCd.msgIRCKick.length() > 0) s.broadcastMessage(IRCd.msgIRCKick.replace("%KICKEDUSER%", bannick).replace("%KICKEDBY%", nick));
											p.kickPlayer("Kicked by " + nick + " on IRC");
										}
									}
								}
							}
							else { writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " +bannick + " :No such nick/channel"); }
						}
						else {
							// Not op
							writeln(IRCd.serverMessagePrefix + " 482 " + nick + " " + IRCd.channelName + " :You're not channel operator");	
						}
					}
				}
				else if (split[0].equalsIgnoreCase("OPER")) {
					if (split.length < 3) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						String user = split[1];
						String pass = Hash.compute(split[2],HashType.SHA_512);
						if ((IRCd.operUser.length() > 0) && (IRCd.operPass.length() > 0) && (user.equals(IRCd.operUser)) && (pass.equals(IRCd.operPass))) {
							// Correct login
							isOper = true;
							writeln(":" + nick + " MODE " + nick + " :+o");
							writeln(IRCd.serverMessagePrefix + " 381 " + nick + " :You are now an IRC Operator");
							if (IRCd.operModes.length() > 0) {
								modes = IRCd.operModes;
								String mode1=" + ", mode2="";
								if (modes.contains("~")) { mode1+="q"; mode2+=nick + " "; }
								if (modes.contains("&")) { mode1+="a"; mode2+=nick + " "; }
								if (modes.contains("@")) { mode1+="o"; mode2+=nick + " "; }
								if (modes.contains("%")) { mode1+="h"; mode2+=nick + " "; }
								if (modes.contains(" + ")) { mode1+="v"; mode2+=nick + " "; }
								
								sendChanWelcome(IRCd.consoleChannelName);
								if (!mode1.equals(" + ")) {
									IRCd.writeAll(":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " MODE " + IRCd.channelName + " " +mode1+ " " +mode2.substring(0, mode2.length()-1));
									IRCd.writeAll(":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " MODE " + IRCd.consoleChannelName + " " +mode1+ " " +mode2.substring(0, mode2.length()-1));
								}
							}
						}
						else {
							// Incorrect login
							writeln(IRCd.serverMessagePrefix + " 464 " + nick + " :Password Incorrect");
						}
					}
				}
				else if (split[0].equalsIgnoreCase("JOIN")) {
					// Do nothing
				}
				else if (split[0].equalsIgnoreCase("PART")) {
					// Do nothing
				}
				else if (split[0].equalsIgnoreCase("ISON")) {
					if (split.length < 2) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						int i = 1;
						String nicks = "";
						while (i < split.length) {
							if (split[i].indexOf(":") == 0) { split[i] = split[i].substring(1); }

							int ID;
							IRCUser ircuser;
							if (split[i].equalsIgnoreCase(IRCd.serverName)) nicks += IRCd.serverName + " ";
							else if ((ircuser = IRCd.getIRCUser(split[i])) != null) nicks += ircuser.nick + " ";
							else if ((ID = IRCd.getBukkitUser(split[i])) >= 0) nicks += IRCd.bukkitPlayers.get(ID).nick + IRCd.ingameSuffix + " ";
							i++;
						}
						if (nicks.length() > 0) nicks = nicks.substring(0, nicks.length()-1);
						writeln(IRCd.serverMessagePrefix + " 303 " + nick + " :" + nicks);
					}
				}
				else if (split[0].equalsIgnoreCase("AWAY")) {
					if (split.length > 1) {
						if (split[1].indexOf(":") == 0) { split[1] = split[1].substring(1); }
						awayMsg = IRCd.join(split, " ", 1);
						writeln(IRCd.serverMessagePrefix + " 306 " + nick + " :You have been marked as being away"); 
					}
					else {
						awayMsg = "";
						writeln(IRCd.serverMessagePrefix + " 305 " + nick + " :You are no longer marked as being away");
					}
				}
				else if (split[0].equalsIgnoreCase("WHO")) {
					boolean operOnly = false;
					String pattern = "";
					if (split.length >= 2) pattern = split[1];
					if ((split.length > 2) && (split[2].equalsIgnoreCase("o"))) { operOnly = true; }
					sendWho(pattern, operOnly);
				}
				else if (split[0].equalsIgnoreCase("PRIVMSG")) {
					lastActivity = System.currentTimeMillis();
					if (split.length < 3) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						if (split[2].indexOf(":") == 0) { split[2] = split[2].substring(1); }
						String message = IRCd.join(split, " ", 2);
						boolean isCTCP = (message.startsWith((char)1+ "") && message.endsWith((char)1+ ""));
						boolean isAction = (message.startsWith((char)1+ "ACTION") && message.endsWith((char)1+ ""));
						String message2;
						if (isAction) message2 = IRCd.join(message.substring(1,message.length()-1).split(" "), " ", 1);
						else  message2 = message;

						if (split[1].equalsIgnoreCase(IRCd.channelName)) {
							if (IRCd.isBanned(getFullHost())) {writeln(IRCd.serverMessagePrefix + " 404 " + nick + " " + IRCd.channelName + " :You are banned (" + IRCd.channelName + ")"); }
							else {
								if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
									if (message.equalsIgnoreCase("!players") && (!isCTCP)) {
										if (IRCd.msgPlayerList.length() > 0) {
											String s = "";
											int count = 0;
											for (BukkitPlayer player : IRCd.bukkitPlayers) { count++; s = s + player.nick + ", "; }
											if (s.length() == 0) s = "None, ";
											IRCd.writeAll(":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " PRIVMSG " + IRCd.channelName + " :" + IRCd.convertColors(IRCd.msgPlayerList.replace("%COUNT%", Integer.toString(count)).replace("%USERS%", s.substring(0, s.length()-2)), false));
										}
									}
									else if (isAction || (!isCTCP)) { 
										if (isAction) {
											if (IRCd.msgIRCAction.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCAction.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message2,true)));
											if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCActionDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCActionDynmap.replace("%USER%", nick).replace("%MESSAGE%", IRCd.stripFormatting(message2)));
										}
										else {
											if (IRCd.msgIRCMessage.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCMessage.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message2,true)));
											if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCMessageDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCMessageDynmap.replace("%USER%", nick).replace("%MESSAGE%", IRCd.stripFormatting(message2)));
										}
									}
								}
								IRCd.writeAllExcept(nick,":" + getFullHost() + " PRIVMSG " + IRCd.channelName + " :" +message);
							}
						}
						else if (split[1].equalsIgnoreCase(IRCd.serverName)) {
							if ((isAction || (!isCTCP)) && (IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								BukkitIRCdPlugin.thePlugin.setLastReceived("@CONSOLE@", nick);
								if (isAction) {
									if (IRCd.msgIRCPrivateAction.length() > 0) BukkitIRCdPlugin.log.info(IRCd.msgIRCPrivateAction.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message2,true)));
								}
								else {
									if (IRCd.msgIRCPrivateMessage.length() > 0) BukkitIRCdPlugin.log.info(IRCd.msgIRCPrivateMessage.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message2,true)));
								}
							}						
						}
						else if (split[1].equalsIgnoreCase(IRCd.consoleChannelName)) {
							if ((!isAction) && (!isCTCP) && (IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								if (!isOper) { writeln(":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " NOTICE " + nick + " :You don't have access."); }
								else {
									// Execute console command here
									IRCd.writeOpersExcept(nick,":" + getFullHost() + " PRIVMSG " + IRCd.consoleChannelName + " :" +message);

									if (message.startsWith("!")) {
										message = message.substring(1);
										if (!IRCd.executeCommand(message)) {
											IRCd.writeOpers(":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " PRIVMSG " + IRCd.consoleChannelName + " :Failed to execute command.");
										}
										else {
											IRCd.writeOpers(":" + IRCd.serverName + "!" + IRCd.serverName + "@" + IRCd.serverHostName + " PRIVMSG " + IRCd.consoleChannelName + " :Command executed.");
										}
									}
								}
							}						
						}
						else {
							IRCUser ircuser = IRCd.getIRCUser(split[1]);
							int user;
							if (ircuser != null) {
								IRCd.writeTo(ircuser.nick,":" + getFullHost() + " PRIVMSG " + split[1] + " :" +message);
							}
							else if ((user = IRCd.getBukkitUser(split[1])) >= 0) {
								if ((isAction || (!isCTCP)) && (IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
									synchronized(IRCd.csBukkitPlayers) {
										String bukkitnick = BukkitIRCdPlugin.thePlugin.getServer().getPlayer(IRCd.bukkitPlayers.get(user).nick).getName();
										BukkitIRCdPlugin.thePlugin.setLastReceived(bukkitnick, nick);
										if (isAction) {
											if (IRCd.msgIRCPrivateAction.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().getPlayer(bukkitnick).sendMessage(IRCd.msgIRCPrivateAction.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message2,true)));
										}
										else {
											if (IRCd.msgIRCPrivateMessage.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().getPlayer(bukkitnick).sendMessage(IRCd.msgIRCPrivateMessage.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message2,true)));
										}
									}
								}
							}
							else { writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + split[1] + " :No such nick/channel"); }
						}
					}
				}
				else if (split[0].equalsIgnoreCase("NOTICE")) {
					lastActivity = System.currentTimeMillis();
					if (split.length < 3) { writeln(IRCd.serverMessagePrefix + " 461  " + split[0] + " :Not enough parameters"); }
					else {
						if (split[2].indexOf(":") == 0) { split[2] = split[2].substring(1); }
						String message = IRCd.join(split, " ", 2);
						boolean isCTCP = (message.startsWith((char)1+ "") && message.endsWith((char)1+ ""));
						if (split[1].equalsIgnoreCase(IRCd.consoleChannelName)) { } // Do nothing
						else if (split[1].equalsIgnoreCase(IRCd.channelName)) {
							if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
								if ((!isCTCP) && IRCd.enableNotices) {
									if (IRCd.msgIRCNotice.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCNotice.replace("%USER%", nick).replace("%MESSAGE%",IRCd.convertColors(message,true)));
									if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCNoticeDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCNoticeDynmap.replace("%USER%", nick).replace("%MESSAGE%",IRCd.stripFormatting(message)));
								}
							}
							IRCd.writeAllExcept(nick,":" + getFullHost() + " NOTICE " + IRCd.channelName + " :" +message);
						}
						else {
							IRCUser ircuser = IRCd.getIRCUser(split[1]);
							int user;
							if (ircuser != null) {
								IRCd.writeTo(ircuser.nick,":" + getFullHost() + " NOTICE " + split[1] + " :" +message);
							}
							else if ((user = IRCd.getBukkitUser(split[1])) >= 0) {
								if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null) && (!isCTCP) && (IRCd.enableNotices)) synchronized(IRCd.csBukkitPlayers) {
									if (IRCd.msgIRCPrivateMessage.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().getPlayer(IRCd.bukkitPlayers.get(user).nick).sendMessage(IRCd.msgIRCPrivateAction.replace("%USER%", nick).replace("%MESSAGE%", IRCd.convertColors(message,true)));
								}
							}
							else { writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + split[1] + " :No such nick/channel"); }
						}
					}
				}
				else { writeln(IRCd.serverMessagePrefix + " 421 " + nick + " " + split[0] + " :Unknown command"); }
			}
			else { writeln(IRCd.serverMessagePrefix + " 451 " + split[0] + " :You have not registered"); }
		}

		public void sendWelcome()
		{
			if ((IRCd.isBanned(nick + "!" + ident + "@" + hostmask)) || (IRCd.isBanned(nick + "!" + ident + "@" + ipaddress))) {
				writeln("ERROR :Closing Link: [" + ipaddress + "] (You are banned from this server)");
				disconnect();
				synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] Cleaning up connection from " + getFullHost() + " (banned)"); }
				if (isIdented && isNickSet) IRCd.writeAll(":" + getFullHost() + " QUIT :You are banned from this server");
				IRCd.removeIRCUser(nick, "Banned", true);
			}
			else {
				synchronized(IRCd.csStdOut) { System.out.println("[BukkitIRCd] " + ipaddress + " registered as " + getFullHost()); }
				Thread.currentThread().setName("Thread-BukkitIRCd-Connection-" + nick);
				signonTime = System.currentTimeMillis() / 1000L;
				writeln("PING :" + signonTime);
				writeln(IRCd.serverMessagePrefix + " 001 " + nick + " :Welcome to the " + IRCd.serverName + " IRC network " + getFullHost());
				writeln(IRCd.serverMessagePrefix + " 002 " + nick + " :Your host is " + IRCd.serverHostName + ", running version " + IRCd.version);
				writeln(IRCd.serverMessagePrefix + " 003 " + nick + " :This server was created " + IRCd.serverCreationDate);
				writeln(IRCd.serverMessagePrefix + " 004 " + nick + " :" + IRCd.serverHostName + " " + IRCd.version + " - -");
				writeln(IRCd.serverMessagePrefix + " 005 " + nick + " NICKLEN=" +(IRCd.nickLen +1) + " CHANNELLEN=50 TOPICLEN=500 PREFIX=(qaohv)~&@%+ CHANTYPES=# CHANMODES=b,k,l,imt CASEMAPPING=ascii NETWORK=" + IRCd.serverName + " :are supported by this server");
				writeln(IRCd.serverMessagePrefix + " 251 " + nick + " :There are " + IRCd.getClientCount() + " users and 0 invisible on " +(IRCd.getServerCount()+1) + " servers");
				writeln(IRCd.serverMessagePrefix + " 252 " + nick + " " + IRCd.getOperCount() + " :operators online");
				writeln(IRCd.serverMessagePrefix + " 254 " + nick + " 1 :channels formed");
				writeln(IRCd.serverMessagePrefix + " 255 " + nick + " :I have " + IRCd.getClientCount() + " clients and " + IRCd.getServerCount() + " servers.");
				writeln(IRCd.serverMessagePrefix + " 265 " + nick + " :Current local users: " + IRCd.getClientCount() + " Max: " + IRCd.maxConnections);
				writeln(IRCd.serverMessagePrefix + " 266 " + nick + " :Current global users: " +(IRCd.getClientCount()+ IRCd.getRemoteClientCount()) + " Max: " +(IRCd.maxConnectionS + IRCd.getRemoteMaxConnections()));
				sendMOTD();
				if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
					if (IRCd.msgIRCJoin.length() > 0) BukkitIRCdPlugin.thePlugin.getServer().broadcastMessage(IRCd.msgIRCJoin.replace("%USER%", nick));
					if ((BukkitIRCdPlugin.dynmap != null) && (IRCd.msgIRCJoinDynmap.length() > 0)) BukkitIRCdPlugin.dynmap.sendBroadcastToWeb("IRC", IRCd.msgIRCJoinDynmap.replace("%USER%", nick));
				}
				sendChanWelcome(IRCd.channelName);
			}
		}

		public void sendMOTD()
		{
			writeln(IRCd.serverMessagePrefix + " 375 " + nick + " :" + IRCd.serverHostName + " Message of the Day");
			for (String motdline : IRCd.MOTD) {
				writeln(IRCd.serverMessagePrefix + " 372 " + nick + " :- " +motdline);
			}
			writeln(IRCd.serverMessagePrefix + " 376 " + nick + " :End of /MOTD command.");
		}

		public void sendWhois(String whoisUser)
		{
			IRCUser ircuser;
			BukkitPlayer bp;
			if (whoisUser.equalsIgnoreCase(IRCd.serverName)) {
				writeln(IRCd.serverMessagePrefix + " 311 " + nick + " " + IRCd.serverName + " " + IRCd.serverName + " " + IRCd.serverHostName + " * :" + IRCd.version);
				if (isOper) {
					writeln(IRCd.serverMessagePrefix + " 379 " + nick + " " + IRCd.serverName + " :is using modes +oS");
					writeln(IRCd.serverMessagePrefix + " 378 " + nick + " " + IRCd.serverName + " :is connecting from *@" + IRCd.serverHostName + " 127.0.0.1");
				}
				writeln(IRCd.serverMessagePrefix + " 312 " + nick + " " + IRCd.serverName + " " + IRCd.serverHostName + " :" + IRCd.serverDescription);
				writeln(IRCd.serverMessagePrefix + " 313 " + nick + " " + IRCd.serverName + " :Is a Network Service");
				writeln(IRCd.serverMessagePrefix + " 318 " + nick + " " + IRCd.serverName + " :End of /WHOIS list.");

			}
			else if ((ircuser = IRCd.getIRCUser(whoisUser)) != null) {
				synchronized(IRCd.csIrcUsers) {
					String cmodes;
					String modes = ircuser.getModes();
					if (modes.length() > 0) cmodes = modes.substring(0,1);
					else cmodes = "";
					writeln(IRCd.serverMessagePrefix + " 311 " + nick + " " + ircuser.nick + " " + ircuser.ident+ " " + ircuser.hostmask + " * :" + ircuser.realname);
					if (isOper) {
						if (ircuser.isOper) writeln(IRCd.serverMessagePrefix + " 379 " + nick + " " + ircuser.nick + " :is using modes +o");
						writeln(IRCd.serverMessagePrefix + " 378 " + nick + " " + ircuser.nick + " :is connecting from *@" + ircuser.realhost+ " " + ircuser.ipaddress);
					}
					if (ircuser.isRegistered) writeln(IRCd.serverMessagePrefix + " 307 " + nick + " " + ircuser.nick + " :is a registered nick");
					writeln(IRCd.serverMessagePrefix + " 319 " + nick + " " + ircuser.nick + " :" + cmodeS + IRCd.channelName);
					writeln(IRCd.serverMessagePrefix + " 312 " + nick + " " + ircuser.nick + " " + IRCd.serverHostName + " :" + IRCd.serverDescription);
					if (ircuser.awayMsg.length() > 0) writeln(IRCd.serverMessagePrefix + " 301 " + nick + " " + ircuser.nick + " :" + ircuser.awayMsg);
					if (ircuser.isOper) writeln(IRCd.serverMessagePrefix + " 313 " + nick + " " + ircuser.nick + " :is an IRC Operator.");
					if (ircuser.customWhois.length() > 0) writeln(IRCd.serverMessagePrefix + " 320 " + nick + " " + ircuser.nick + " :" + ircuser.customWhois);
					writeln(IRCd.serverMessagePrefix + " 317 " + nick + " " + ircuser.nick + " " + ircuser.getSecondsIdle() + " " + ircuser.signonTime + " :seconds idle, signon time");
					writeln(IRCd.serverMessagePrefix + " 318 " + nick + " " + ircuser.nick + " :End of /WHOIS list.");
				}
			}
			else if ((bp = IRCd.getBukkitUserObject(whoisUser)) != null) {
				if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
					if ((IRCd.ingameSuffix.length() > 0) && (whoisUser.endsWith(IRCd.ingameSuffix)))
						whoisUser.substring(0,whoisUser.length()-IRCd.ingameSuffix.length());
					else {
					}
					String bukkitversion = BukkitIRCdPlugin.thePlugin.getServer().getVersion();

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
					if (mode.length() > 0) playermodes = mode.substring(0,1);
					else playermodes = "";
					playersignon = bp.signedOn;
					playeridle = (System.currentTimeMillis() - bp.idleTime) / 1000L;
					playerident = bp.nick;
					playernick = bp.nick + IRCd.ingameSuffix;
					playerhost = bp.host;
					playerip = bp.ip;
					playerworld = bp.world;
					playerisoper = bp.hasPermission("bukkitircd.oper");

					writeln(IRCd.serverMessagePrefix + " 311 " + nick + " " + playernick + " " + playerident+ " " + playerhost+ " " + " * :Minecraft Player");
					if (isOper) writeln(IRCd.serverMessagePrefix + " 378 " + nick + " " + playernick + " :is connecting from *@" + playerhost+ " " + playerip);
					writeln(IRCd.serverMessagePrefix + " 319 " + nick + " " + playernick + " :" + playermodeS + IRCd.channelName);
					writeln(IRCd.serverMessagePrefix + " 312 " + nick + " " + playernick + " " + IRCd.serverHostName + " :Bukkit " +bukkitversion);
					if (playerisoper) writeln(IRCd.serverMessagePrefix + " 313 " + nick + " " + playernick + " :is an IRC Operator.");
					writeln(IRCd.serverMessagePrefix + " 320 " + nick + " " + playernick + " :is currently in " + playerworld);
					writeln(IRCd.serverMessagePrefix + " 317 " + nick + " " + playernick + " " + playeridle + " " + playersignon + " :seconds idle, signon time");
					writeln(IRCd.serverMessagePrefix + " 318 " + nick + " " + playernick + " :End of /WHOIS list.");
				}
			}
			else { writeln(IRCd.serverMessagePrefix + " 401 " + nick + " " + whoisUser + " :No such nick/channel"); writeln(IRCd.serverMessagePrefix + " 318 " + nick + " " + whoisUser + " :End of /WHOIS list."); }
		}

		public void sendChanWelcome(String channelName)
		{
			if (channelName.equalsIgnoreCase(IRCd.consoleChannelName)) IRCd.writeOpers(":" + getFullHost() + " JOIN " + channelName);
			else IRCd.writeAll(":" + getFullHost() + " JOIN " + channelName);
			sendChanTopic(channelName);
			sendChanModes(channelName);
			sendChanNames(channelName);
		}

		public boolean sendChanTopic(String channelName)
		{
			String consoleChannelTopic = "BukkitIRCd console channel - prefix commands with !";
			if (IRCd.channelTopic.length()>0) {
				if (channelName.equalsIgnoreCase(IRCd.consoleChannelName)) {
					writeln(IRCd.serverMessagePrefix + " 332 " + nick + " " + channelName + " :" + consoleChannelTopic);
					try { writeln(IRCd.serverMessagePrefix + " 333 " + nick + " " + channelName + " " + IRCd.serverName + " " +(IRCd.dateFormat.parse(IRCd.serverCreationDate).getTime() / 1000L)); }
					catch (ParseException e) { writeln(IRCd.serverMessagePrefix + " 333 " + nick + " " + channelName + " " + IRCd.serverName + " " +(System.currentTimeMillis() / 1000L)); }
				}
				else if (channelName.equalsIgnoreCase(IRCd.channelName)) {
					writeln(IRCd.serverMessagePrefix + " 332 " + nick + " " + channelName + " :" + IRCd.channelTopic);
					writeln(IRCd.serverMessagePrefix + " 333 " + nick + " " + channelName + " " + IRCd.channelTopicSet+ " " + IRCd.channelTopicSetDate);
				}
				else return false;
			}
			return true;
		}

		public boolean sendChanModes(String channelName)
		{
			if (IRCd.channelTopic.length()>0) {
				if (channelName.equals(IRCd.consoleChannelName)) writeln(IRCd.serverMessagePrefix + " 324 " + nick + " " + channelName + " +Ont");
				else writeln(IRCd.serverMessagePrefix + " 324 " + nick + " " + channelName + " + nt");
				try { writeln(IRCd.serverMessagePrefix + " 329 " + nick + " " + channelName + " " +((IRCd.dateFormat.parse(IRCd.serverCreationDate).getTime()) / 1000)); }
				catch (ParseException e) { writeln(IRCd.serverMessagePrefix + " 329 " + nick + " " + channelName + " " +(System.currentTimeMillis() / 1000L)); }
			}
			return true;
		}

		public boolean sendChanNames(String channelName)
		{
			if (channelName.equalsIgnoreCase(IRCd.consoleChannelName)) writeln(IRCd.serverMessagePrefix + " 353 = " + nick + " " + channelName + " :~" + IRCd.serverName + " " + IRCd.getOpers());
			else if (channelName.equalsIgnoreCase(IRCd.channelName)) writeln(IRCd.serverMessagePrefix + " 353 = " + nick + " " + channelName + " :~" + IRCd.serverName + " " + IRCd.getUsers());
			else return false;

			writeln(IRCd.serverMessagePrefix + " 366 " + nick + " " + channelName + " :End of /NAMES list.");
			return true;
		}

		public void sendWho(String pattern, boolean opersOnly)
		{
			boolean addAll = (pattern.equalsIgnoreCase(IRCd.channelName)) || (pattern.length() == 0);
			if (pattern.equalsIgnoreCase(IRCd.consoleChannelName)) { opersOnly = true; addAll = true; }

			String channel = IRCd.channelName;
			List<String> users = new ArrayList<String>();
			synchronized(IRCd.csIrcUsers) {
				for (IRCUser user : IRCd.getIRCUsers()) {
					String onick = user.nick;
					String ohost = user.hostmask;
					String oident = user.ident;
					String away = ((user.awayMsg.length() > 0) ? "G" : "H");
					String oper = (user.isOper ? "*" : "");
					String name = user.realname;

					if ((opersOnly && user.isOper) || (!opersOnly)) {
						if (addAll || IRCd.wildCardMatch(onick,pattern) || IRCd.wildCardMatch(onick + "!" +oident+ "@" +ohost, pattern)) users.add(IRCd.serverMessagePrefix + " 352 " + nick + " " + channel+ " " +oident+ " " +ohost+ " " + IRCd.serverHostName + " " +onick + " " +away+oper + " :0 " + name);
					}
				}
			}
			if (!opersOnly) { 
				synchronized(IRCd.csBukkitPlayers) {
					for (BukkitPlayer bukkitPlayer : IRCd.bukkitPlayers) {
						String onick = bukkitPlayer.nick + IRCd.ingameSuffix;
						String ohost = bukkitPlayer.host;
						String oident = bukkitPlayer.nick;
						String name = bukkitPlayer.nick;
						String away = "H";
						String oper = "";

						if (addAll || IRCd.wildCardMatch(onick,pattern) || IRCd.wildCardMatch(oident,pattern) || IRCd.wildCardMatch(onick + "!" +oident+ "@" +ohost, pattern) || IRCd.wildCardMatch(oident+ "!" +oident+ "@" +ohost, pattern)) users.add(IRCd.serverMessagePrefix + " 352 " + nick + " " + channel+ " " +oident+ " " +ohost+ " " + IRCd.serverHostName + " " +onick + " " +away+oper + " :0 " + name);
					}
				}
			}
			for (String line : users) writeln(line);
			writeln(IRCd.serverMessagePrefix + " 315 " + nick + " " + pattern + " :End of /WHO list.");
		}

		public boolean isConnected()
		{
			boolean result;
			result = server.isConnected();
			return result;
		}

		public long getSecondsIdle()
		{
			return (System.currentTimeMillis() - lastActivity) / 1000L;
		}

		public String getFullHost()
		{
			return nick + "!" + ident + "@" + hostmask;
		}

		public boolean writeln(String line)
		{
			if (server.isConnected()) { synchronized(csWrite) { out.println(line); return true; } }
			else { return false; }
		}

		public boolean disconnect()
		{
			if (server.isConnected()) {
				try { server.close(); } catch (IOException e) { return false; }
				return true;
			}
			else { return false; }
		}

		static class CriticalSection extends Object {
		}
		static public CriticalSection csWrite = new CriticalSection();

	}