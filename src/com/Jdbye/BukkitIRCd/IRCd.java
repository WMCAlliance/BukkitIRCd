package com.Jdbye.BukkitIRCd;

// BukkitIRCd by Jdbye and WMCAlliance
// A standalone IRC server plugin for Bukkit
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
import com.Jdbye.BukkitIRCd.configuration.Config;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    public static String msgIRCJoin = "&e[IRC] {User} Utils.joined IRC";
    public static String msgIRCJoinDynmap = "{User} Utils.joined IRC";
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

    boolean debug = Config.isDebugModeEnabled();

    // server 'names' to not send messages into the channel from
    public static ArrayList<String> globalNameIgnoreList = new ArrayList<String>();

    public static final long serverStartTime = System.currentTimeMillis() / 1000L;
    public static long channelTS = serverStartTime,
	    consoleChannelTS = serverStartTime;
    String remoteSID = null;
    public static String pre; // Prefix for server linking commands
    long linkLastPingPong;
    long linkLastPingSent;
    public static String serverMessagePrefix;
    public static HashMap<String, IRCServer> servers = new HashMap<String, IRCServer>();
    public static UidGenerator ugen = new UidGenerator();
    public static String serverUID;
    public static boolean linkcompleted = false;
    public static boolean burstSent = false, capabSent = false;
    private static boolean lastconnected = false;
    public static boolean isIncoming = false;
    // private static boolean broadcastDeathMessages = true;
    // private static boolean colorDeathMessages = false;
    // private static boolean colorSayMessages = false;

    public static boolean isPlugin = false;

    // This object registers itself as a console target and needs to be
    // long lived.
    public static IRCCommandSender commandSender = null;

    // private static Date curDate = new Date();
    public static SimpleDateFormat dateFormat = new SimpleDateFormat(
	    "EEE MMM dd HH:mm:ss yyyy");
    // private static String serverCreationDate = dateFormat.format(curDate);
    public static long serverCreationDateLong = System.currentTimeMillis() / 1000L;

    // private static int[] ircColors = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    // 12,
    // 13, 14, 15 };
    // private static String[] gameColors = { "0", "f", "1", "2", "c", "4", "5",
    // "6", "e", "a", "3", "b", "9", "d", "8", "7" };
    public static List<BukkitPlayer> bukkitPlayers = new LinkedList<BukkitPlayer>();

    public static List<String> MOTD = new ArrayList<String>();
    public static List<String> consoleFilters = new ArrayList<String>();
    public static List<IrcBan> ircBans = new ArrayList<IrcBan>();

    public static ConfigurationSection groupPrefixes = null;
    public static ConfigurationSection groupSuffixes = null;

    public boolean running = true;

    private long tickCount = System.currentTimeMillis();
    public static ServerSocket listener;
    public static Socket server = null;

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
		if (c != null) {
		    isPlugin = true;
		}
	    } catch (ClassNotFoundException e) {
		isPlugin = false;
	    }

	    try {
		if (Config.getMode().equalsIgnoreCase("inspire") ||
			Config.getMode().equalsIgnoreCase("inspircd")) {
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

		if (mode == Modes.STANDALONE) {
		    Thread.currentThread().setName(
			    "Thread-BukkitIRCd-StandaloneIRCd");
		    IRCUserManagement.clientConnections.clear();
		    try {
			try {
			    listener = new ServerSocket(Config.getIrcdPort());
			    listener.setSoTimeout(1000);
			    listener.setReuseAddress(true);
			    BukkitIRCdPlugin.log
				    .info("[BukkitIRCd] Listening for client connections on port " +
					    Config.getIrcdPort());
			} catch (IOException e) {
			    BukkitIRCdPlugin.log
				    .severe("Failed to listen on port " +
					    Config.getIrcdPort() + ": " + e);
			}
			while (running) {
			    if ((IRCUserManagement.clientConnections.size() < Config
				    .getIrcdMaxConnections()) ||
				    (Config.getIrcdMaxConnections() == 0)) {
				ClientConnection connection;
				try {
				    server = listener.accept();
				    if (server.isConnected()) {
					connection = new ClientConnection(
						server);
					connection.lastPingResponse = System
						.currentTimeMillis();
					IRCUserManagement.clientConnections.add(connection);
					Thread t = new Thread(connection);
					t.start();
				    }
				} catch (SocketTimeoutException e) {
				}
				if (tickCount +
					(Config.getIrcdPingInterval() * 1000) < System
					.currentTimeMillis()) {
				    tickCount = System.currentTimeMillis();
				    IRCFunctionality.writeAll("PING :" + tickCount);
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
				    .println("[BukkitIRCd] IOException on socket listen: " +
					    e.toString() +
					    ". Error Code IRCd281.");
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
				.info("[BukkitIRCd] Listening for server connections on port " +
					Config.getLinkLocalPort());
		    } catch (IOException e) {
			BukkitIRCdPlugin.log.severe("Failed to listen on port " +
				Config.getLinkLocalPort() + ": " + e);
		    }

		    try {
			server = listener.accept();
		    } catch (IOException e) {
		    }
		    if ((server != null) && server.isConnected() &&
			    (!server.isClosed())) {
			InetAddress addr = server.getInetAddress();
			BukkitIRCdPlugin.log
				.info("[BukkitIRCd] Got server connection from " +
					addr.getHostAddress());
			isIncoming = true;
		    } else if (Config.isLinkAutoconnect()) {
			IRCFunctionality.connect();
		    }

		    while (running) {
			try {
			    if ((server != null) && server.isConnected() &&
				    (!server.isClosed()) && (!lastconnected)) {
				in = new BufferedReader(new InputStreamReader(
					server.getInputStream()));
				out = new PrintStream(server.getOutputStream());
				line = in.readLine();
				if (line == null) {
				    throw new IOException(
					    "Lost connection to server before sending handshake!");
				}
				String[] split = line.split(" ");
				if (Config.isDebugModeEnabled()) {
				    BukkitIRCdPlugin.log
					    .info("[BukkitIRCd] " +
						    ChatColor.YELLOW +
						    "[->] " + line);
				}

				if (!isIncoming) {
				    IRCFunctionality.sendLinkCAPAB();
				    IRCFunctionality.sendLinkBurst();
				}

				while ((!split[0].equalsIgnoreCase("SERVER")) &&
					(server != null) &&
					(!server.isClosed()) &&
					server.isConnected() && running) {
				    if (!running) {
					break;
				    }

				    if (line.startsWith("CAPAB START")) {
					IRCFunctionality.sendLinkCAPAB();
				    }

				    if (split[0].equalsIgnoreCase("ERROR")) {
					// ERROR :Invalid password.
					if (split[1].startsWith(":")) {
					    split[1] = split[1].substring(1);
					}
					try {
					    server.close();
					} catch (IOException e) {
					}
					throw new IOException(
						"Remote host rejected connection, probably configured wrong: " +
						Utils.join(split, " ", 1));
				    } else {
					line = in.readLine();
					if (line != null) {
					    split = line.split(" ");
					    if (Config.isDebugModeEnabled()) {
						BukkitIRCdPlugin.log
							.info("[BukkitIRCd]" +
								ChatColor.YELLOW +
								" [->] " +
								line);
					    }
					}
				    }
				}
				if (split[0].equalsIgnoreCase("SERVER")) {
				    // SERVER test.tempcraft.net password 0 280
				    // :TempCraft Testing Server
				    if ((!split[2].equals(Config
					    .getLinkReceivePassword())) ||
					    (!split[1].equals(Config
						    .getLinkName()))) {
					if (!split[2].equals(Config
						.getLinkReceivePassword())) {
					    Utils.println("ERROR :Invalid password.");
					} else if (!split[1].equals(Config
						.getLinkName())) {
					    Utils.println("ERROR :No configuration for hostname " +
						    split[1]);
					}
					server.close();

					if (!split[1].equals(Config
						.getLinkName())) {
					    throw new IOException(
						    "Rejected connection from remote host: Invalid link name.");
					} else {
					    throw new IOException(
						    "Rejected connection from remote host: Invalid password.");
					}
				    }
				    remoteSID = split[4];
				}

				linkLastPingPong = System.currentTimeMillis();
				linkLastPingSent = System.currentTimeMillis();

				if ((IRCd.isPlugin) &&
					(BukkitIRCdPlugin.thePlugin != null)) {
				    if (msgLinked.length() > 0) {
					Utils.broadcastMessage(msgLinked
						.replace("{LinkName}",
							Config.getLinkName()));
				    }
				}
				server.setSoTimeout(500);
				lastconnected = true;
				linkcompleted = true;
			    }

			    while (running && (server != null) &&
				    server.isConnected() &&
				    (!server.isClosed())) {
				try {
				    if (linkLastPingPong +
					    (Config.getLinkTimeout() * 1000) < System
					    .currentTimeMillis()) {
					// Link ping timeout, disconnect and
					// notify remote server
					Utils.println("ERROR :Ping timeout");
					server.close();
				    } else {
					if (linkLastPingSent +
						(Config.getLinkPingInterval() * 1000) < System
						.currentTimeMillis()) {
					    Utils.println(pre + "PING " +
						    Config.getLinkServerID() +
						    " " + remoteSID);
					    linkLastPingSent = System
						    .currentTimeMillis();
					}
					line = in.readLine();

					if ((line != null) &&
						(line.trim().length() > 0)) {
					    if (line.startsWith("ERROR ")) {
						// ERROR :Invalid password.
						if (Config.isDebugModeEnabled()) {
						    BukkitIRCdPlugin.log
							    .info("[BukkitIRCd]" +
								    ChatColor.YELLOW +
								    "[->] " +
								    line);
						}
						String[] split = line
							.split(" ");
						if (split[1].startsWith(":")) {
						    split[1] = split[1]
							    .substring(1);
						}
						try {
						    server.close();
						} catch (IOException e) {
						}
						throw new IOException(
							"Remote host rejected connection, probably configured wrong: " +
							Utils.join(split,
								" ", 1));
					    } else {
						parseLinkCommand(line);
					    }
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
					.warning("[BukkitIRCd] Server link failed: " +
						e);
			    }
			}

			// We exited the while loop so assume the connection was
			// lost.
			if (lastconnected) {
			    BukkitIRCdPlugin.log
				    .info("[BukkitIRCd] Lost connection to " +
					    Config.getLinkRemoteHost() + ":" +
					    Config.getLinkRemoteHost());
			    if ((IRCd.isPlugin) &&
				    (BukkitIRCdPlugin.thePlugin != null) &&
				    linkcompleted) {
				if (msgDelinked.length() > 0) {
				    Utils.broadcastMessage(msgDelinked.replace(
					    "{LinkName}", Config.getLinkName()));
				}
			    }
			    lastconnected = false;
			}

			if ((server != null) && server.isConnected()) {
			    try {
				server.close();
			    } catch (IOException e) {
			    }
			}
			linkcompleted = false;
			capabSent = false;
			burstSent = false;
			IRCUserManagement.uid2ircuser.clear();
			servers.clear();
			remoteSID = null;
			if (running) {
			    if (Config.isLinkAutoconnect()) {
				BukkitIRCdPlugin.log
					.info("[BukkitIRCd] Waiting " +
						Config.getLinkDelay() +
						" seconds before retrying...");
				long endTime = System.currentTimeMillis() +
					(Config.getLinkDelay() * 1000);
				while (System.currentTimeMillis() < endTime) {
				    if ((!running) || isConnected()) {
					break;
				    }
				    Thread.currentThread();
				    Thread.sleep(10);
				    try {
					server = listener.accept();
					if ((server != null) &&
						server.isConnected() &&
						(!server.isClosed())) {
					    InetAddress addr = server
						    .getInetAddress();
					    BukkitIRCdPlugin.log
						    .info("[BukkitIRCd] Got server connection from " +
							    addr.getHostAddress());
					    isIncoming = true;
					    break;
					}
				    } catch (IOException e) {
				    }
				}
				if ((server == null) || (!server.isConnected()) ||
					(server.isClosed())) {
				    IRCFunctionality.connect();
				}
			    } else {
				try {
				    server = listener.accept();
				    if ((server != null) &&
					    server.isConnected() &&
					    (!server.isClosed())) {
					InetAddress addr = server
						.getInetAddress();
					BukkitIRCdPlugin.log
						.info("[BukkitIRCd] Got server connection from " +
							addr.getHostAddress());
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
		BukkitIRCdPlugin.log.info("[BukkitIRCd] Thread " +
			Thread.currentThread().getName() + " interrupted.");
		if (running) {
		    IRCFunctionality.disconnectAll("Thread interrupted.");
		    running = false;
		}
	    } catch (Exception e) {
		BukkitIRCdPlugin.log
			.severe("[BukkitIRCd] Unexpected exception in " +
				Thread.currentThread().getName() + ": " +
				e.toString());
		BukkitIRCdPlugin.log.severe("[BukkitIRCd] Error code IRCd473.");
		e.printStackTrace();
	    }
	}
	BukkitIRCdPlugin.ircd = null;
	if (running) {
	    BukkitIRCdPlugin.log
		    .warning("[BukkitIRCd] Thread quit unexpectedly. If there are any errors above, please notify WizardCM or Mu5tank05 about them.");
	}
	running = false;
    }

    public static boolean isConnected() {
	return ((server != null) && server.isConnected() && (!server.isClosed()));
    }

    public static int getClientCount() {
	if (mode == Modes.STANDALONE) {
	    return IRCUserManagement.clientConnections.size() + bukkitPlayers.size();
	} else {
	    return bukkitPlayers.size();
	}
    }

    public static int getOperCount() {
	int count = 0;
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		for (ClientConnection processor : IRCUserManagement.clientConnections) {
		    if (processor.isOper) {
			count++;
		    }
		}
	    }
	}
	return count;
    }

    public static int getRemoteClientCount() {
	return IRCUserManagement.uid2ircuser.size();
    }

    public static int getRemoteMaxConnections() {
	return 0;
    }

    public static int getServerCount() {
	if (mode == Modes.STANDALONE) {
	    return 0;
	} else {
	    return 1 + servers.size();
	}
    }


    /*
     * public static boolean addBukkitUser(String modes, String nick, String
     * world, String host, String ip) { if (getUser(nick) < 0) {
     * synchronized (csBukkitPlayers) { BukkitPlayer bp = new BukkitPlayer(nick,
     * world, modes, host, host, ip, System.currentTimeMillis() / 1000L,
     * System.currentTimeMillis()); bukkitPlayers.add(bp);
     * 
     * if (mode == Modes.STANDALONE) { writeAll(":" + nick +
     * Config.getIrcdIngameSuffix() + "!" + nick + "@" + host + " JOIN " +
     * Config.getIrcdChannel()); } String mode1 = "+", mode2 = ""; if
     * (modes.contains("~")) { mode1 += "q"; mode2 += nick +
     * Config.getIrcdIngameSuffix() + " "; } if (modes.contains("&")) { mode1 +=
     * "a"; mode2 += nick + Config.getIrcdIngameSuffix() + " "; } if
     * (modes.contains("@")) { mode1 += "o"; mode2 += nick +
     * Config.getIrcdIngameSuffix() + " "; } if (modes.contains("%")) { mode1 +=
     * "h"; mode2 += nick + Config.getIrcdIngameSuffix() + " "; } if
     * (modes.contains("+")) { mode1 += "v"; mode2 += nick +
     * Config.getIrcdIngameSuffix() + " "; } if (!mode1.equals("+")) { if (mode
     * == Modes.STANDALONE) {
     * 
     * writeAll(":" + Config.getIrcdServerName() + "!" +
     * Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName() +
     * " MODE " + Config.getIrcdChannel() } }
     * 
     * if (mode == Modes.INSPIRCD) {
     * 
     * String UID = ugen.generateUID(Config.getLinkServerID()); bp.setUID(UID);
     * synchronized (csBukkitPlayers) { String textMode = bp.getTextMode(); if
     * (bp.hasPermission("bukkitircd.oper")) { Utils.println(pre + "UID " + UID + " "
     * + (bp.idleTime / 1000L) + " " + bp.nick + Config.getIrcdIngameSuffix() +
     * " " + bp.host + " " + bp.host + " " + bp.nick + " " + bp.ip + " " +
     * bp.signedOn + " +or :Minecraft Player"); Utils.println(":" + UID +
     * " OPERTYPE IRC_Operator"); } else Utils.println(pre + "UID " + UID + " " +
     * (bp.idleTime / 1000L) + " " + bp.nick + Config.getIrcdIngameSuffix() +
     * " " + bp.host + " " + bp.host + " " + bp.nick + " " + bp.ip + " " +
     * bp.signedOn + " +r :Minecraft Player");
     * 
     * Utils.println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS +
     * " +nt :," + UID); if (textMode.length() > 0) { String modestr = ""; for
     * (int i = 0; i < textMode.length(); i++) { modestr += UID + " "; } modestr
     * = modestr .substring(0, modestr.length() - 1); Utils.println(":" + serverUID +
     * " FMODE " + Config.getIrcdChannel() + " " + channelTS + " +" + textMode +
     * " " + modestr); } if (world != null) Utils.println(pre + "METADATA " + UID +
     * " swhois :is currently in " + world); else Utils.println(pre + "METADATA " +
     * UID + " swhois :is currently in an unknown world"); } } } return true; }
     * else return false; }
     */
    public void parseLinkCommand(String command) throws IOException {
	if (Config.isDebugModeEnabled()) {
	    BukkitIRCdPlugin.log.info("[BukkitIRCd]" + ChatColor.YELLOW +
		    "[->] " + command);
	}

	String split[] = command.split(" ");
	if (split.length <= 1) {
	    return;
	}
	if (split[0].startsWith(":")) {
	    split[0] = split[0].substring(1);
	}

	if (split[1].equalsIgnoreCase("PING")) {
	    // Incoming ping, respond with pong so we don't get timed out from
	    // the server'
	    // :280 PING 280 123
	    linkLastPingPong = System.currentTimeMillis();
	    if (split.length == 3) {
		Utils.println(pre + "PONG " + split[2]);
	    } else if ((split.length == 4) &&
		    (split[3].equalsIgnoreCase(Integer.toString(Config
				    .getLinkServerID())))) {
		Utils.println(pre + "PONG " + Config.getLinkServerID() + " " +
			split[2]);
	    }
	} else if (split[1].equalsIgnoreCase("PONG")) {
	    // Received a pong, update the last ping pong timestamp.
	    // :280 PONG 280 123
	    linkLastPingPong = System.currentTimeMillis();
	} else if (split[1].equalsIgnoreCase("ERROR")) {
	    // :280 ERROR :Unrecognised or malformed command 'CAPAB' -- possibly
	    // loaded mismatched modules
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    throw new IOException(
		    "Remote host rejected connection, probably configured wrong: " +
		    Utils.join(split, " ", 2));
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
	    if (split[11].startsWith(":")) {
		split[11] = split[11].substring(1);
	    }
	    String realname = Utils.join(split, " ", 11);
	    boolean isRegistered = split[10].contains("r");
	    boolean isOper = split[10].contains("o");
	    IRCUser ircuser = new IRCUser(nick, realname, ident, realhost,
		    vhost, ipaddress, "", "", isRegistered, false, "",
		    signedOn, idleTime, "");
	    ircuser.isRegistered = isRegistered;
	    ircuser.isOper = isOper;
	    IRCUserManagement.uid2ircuser.put(UID, ircuser); // Add it to the hashmap
	} else if (split[1].equalsIgnoreCase("AWAY")) {
	    // Away status updating
	    // :0IJAAAAAE AWAY :Auto Away at Tue Nov 22 13:56:26 2011
	    String UID = split[0];

	    IRCUser iuser;
	    if ((iuser = IRCUserManagement.uid2ircuser.get(UID)) != null) {
		// Found the UID in the hashmap, update away message
		if (split.length > 2) {
		    // New away message
		    if (split[2].startsWith(":")) {
			split[2] = split[2].substring(1);
		    }
		    iuser.awayMsg = Utils.join(split, " ", 2);
		} else {
		    // Remove away status
		    iuser.awayMsg = "";
		}
	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + UID +
			    " not found in list. Error code IRCd1707."); // Log
		}
	    }

	} else if (split[1].equalsIgnoreCase("TIME")) {
	    // TIME request from user
	    // :123AAAAAA TIME :test.tempcraft.net
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    IRCUser iuser;
	    if (split[2].equalsIgnoreCase(Config.getIrcdServerHostName())) { // Double
		// check
		// to
		// make sure
		// this request
		// is for us
		if ((iuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		    Utils.println(pre + "PUSH " + split[0] + " ::" +
			    Config.getIrcdServerHostName() + " 391 " +
			    iuser.nick + " " + Config.getIrcdServerHostName() +
			    " :" +
			    dateFormat.format(System.currentTimeMillis()));
		}
	    }
	} else if (split[1].equalsIgnoreCase("ENDBURST")) {
	    // :280 ENDBURST
	    if (split[0].equalsIgnoreCase(remoteSID) ||
		    split[0].equalsIgnoreCase(Config.getLinkName())) {
		IRCFunctionality.sendLinkBurst();
	    }
	} else if (split[1].equalsIgnoreCase("SERVER")) {
	    // :dev.tempcraft.net SERVER Esper.janus * 1 0JJ Esper
	    String hub;
	    try {
		if (split[0].equalsIgnoreCase(remoteSID) ||
			split[0].equalsIgnoreCase(Config.getLinkName())) {
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
		    if (is != null) {
			is.leaves.add(split[5]);
		    } else {
			BukkitIRCdPlugin.log
				.severe("[BukkitIRCd] Received invalid SERVER command, unknown hub server!");
		    }
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
	    if (quitServer.equalsIgnoreCase(Config.getLinkName()) ||
		    quitServer.equalsIgnoreCase(remoteSID)) {
		IRCFunctionality.disconnectServer("Remote server delinked");
	    } else {
		Iterator<Entry<String, IRCServer>> iter = servers.entrySet()
			.iterator();
		IRCServer is = null;
		while (iter.hasNext()) {
		    Map.Entry<String, IRCServer> entry = iter.next();
		    is = entry.getValue();
		    if (is.host.equalsIgnoreCase(quitServer) ||
			    is.SID.equalsIgnoreCase(quitServer)) {
			// Found the server in the list
			IRCUserManagement.removeIRCUsersBySID(is.SID);
			break;
		    }
		}
	    }
	} else if (split[1].equalsIgnoreCase("OPERTYPE")) {
	    // :123AAAAAA OPERTYPE IRC_Operator
	    IRCUser ircuser;
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		ircuser.isOper = true;
	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] +
			    " not found in list. Error code IRCd1779."); // Log
		    // as
		}
	    }

	} else if (split[1].equalsIgnoreCase("MODE")) {
	    IRCUser ircusertarget;
	    if (split[3].startsWith(":")) {
		split[3] = split[3].substring(1);
	    }

	    if ((ircusertarget = IRCUserManagement.uid2ircuser.get(split[2])) != null) {
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
		    // Log as severe because this situation should never occur
		    // and
		    // points to a bug in the code
		    BukkitIRCdPlugin.log
			    .severe("[BukkitIRCd] UID/Config.getLinkServerID() " + split[0] +
				    " not found in list. Error code IRCd1806.");
		}

	    }
	} else if (split[1].equalsIgnoreCase("FJOIN")) {
	    // :dev.tempcraft.net FJOIN #tempcraft.staff 1321829730 +tnsk
	    // MASTER-RACE :qa,0AJAAAAAA o,0IJAAAAAP v,0IJAAAAAQ
	    if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
		try {
		    long tmp = Long.parseLong(split[3]);
		    if (channelTS > tmp) {
			channelTS = tmp;
		    }
		} catch (NumberFormatException e) {
		}
		// Main channel
		String users[] = command.split(" ");
		for (String user : users) {
		    if (!user.contains(",")) {
			continue;
		    }
		    String usersplit[] = user.split(",");
		    IRCUser ircuser;
		    if ((ircuser = IRCUserManagement.uid2ircuser.get(usersplit[1])) != null) {
			ircuser.setModes(usersplit[0]);
			if ((IRCd.isPlugin) &&
				(BukkitIRCdPlugin.thePlugin != null)) {
			    if (!ircuser.joined) {

				if (msgIRCJoin.length() > 0) // TODO I believe fix for #45 would go here
				{
				    Utils.broadcastMessage(msgIRCJoin
					    .replace("{User}", ircuser.nick)
					    .replace(
						    "{Prefix}",
						    IRCFunctionality.getGroupPrefix(ircuser
							    .getTextModes()))
					    .replace(
						    "{Suffix}",
						    IRCFunctionality.getGroupSuffix(ircuser
							    .getTextModes())));
				}
				if ((BukkitIRCdPlugin.dynmap != null) &&
					(msgIRCJoinDynmap.length() > 0)) {
				    BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					    "IRC", msgIRCJoinDynmap.replace(
						    "{User}", ircuser.nick));
				}
			    }
			}
			ircuser.joined = true;
		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] UID " +
					    usersplit[1] +
					    " not found in list. Error code IRCd1831."); // Log
			}
		    }

		}
	    } else if (split[2]
		    .equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
		try {
		    long tmp = Long.parseLong(split[3]);
		    if (consoleChannelTS > tmp) {
			consoleChannelTS = tmp;
		    }
		} catch (NumberFormatException e) {
		}
		// Console channel
		String users[] = command.split(" ");
		for (String user : users) {
		    if (!user.contains(",")) {
			continue;
		    }
		    String usersplit[] = user.split(",");
		    IRCUser ircuser;
		    if ((ircuser = IRCUserManagement.uid2ircuser.get(usersplit[1])) != null) {
			ircuser.setConsoleModes(usersplit[0]);
			ircuser.consoleJoined = true;
		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] UID " +
					    usersplit[1] +
					    " not found in list. Error code IRCd1849."); // Log
			}
		    }

		}
	    }
	    // Ignore other channels, since this plugin only cares about the
	    // main channel and console channel.
	} else if (split[1].equalsIgnoreCase("FHOST")) {
	    // :0KJAAAAAA FHOST test
	    IRCUser ircuser;
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		ircuser.hostmask = split[2];
	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] +
			    " not found in list. Error code IRCd1861."); // Log
		    // as
		}
	    }

	} else if (split[1].equalsIgnoreCase("FNAME")) {
	    // :0KJAAAAAA FNAME TEST
	    IRCUser ircuser;
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		ircuser.realname = Utils.join(split, " ", 2);
	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] +
			    " not found in list. Error code IRCd1870."); // Log
		    // as
		}
	    }

	} else if (split[1].equalsIgnoreCase("FMODE")) {
	    // :0KJAAAAAA FMODE #tempcraft.staff 1320330110 +o 0KJAAAAAB
	    IRCUser ircuser, ircusertarget;
	    if (split.length >= 6) { // If it's not length 6, it's not a user
		// mode
		if (split[0].startsWith(":")) {
		    split[0] = split[0].substring(1);
		}

		if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
		    try {
			long tmp = Long.parseLong(split[3]);
			if (channelTS > tmp) {
			    channelTS = tmp;
			}
		    } catch (NumberFormatException e) {
		    }
		} else if (split[2].equalsIgnoreCase(Config
			.getIrcdConsoleChannel())) {
		    try {
			long tmp = Long.parseLong(split[3]);
			if (consoleChannelTS > tmp) {
			    consoleChannelTS = tmp;
			}
		    } catch (NumberFormatException e) {
		    }
		}

		Boolean add = true;
		int modecount = 0;
		for (int i = 0; i < split[4].length(); i++) {
		    if (5 + modecount >= split.length) {
			break;
		    }
		    String user = split[5 + modecount];
		    if (user.startsWith(":")) {
			user = user.substring(1);
		    }
		    String mode = split[4].charAt(i) + "";
		    if (mode.equals("+")) {
			add = true;
		    } else if (mode.equals("-")) {
			add = false;
		    } else {
			if ((ircusertarget = IRCUserManagement.uid2ircuser.get(user)) != null) {
			    if (split[2].equalsIgnoreCase(Config
				    .getIrcdChannel())) {
				String textModes = ircusertarget.getTextModes();
				if (add) {
				    System.out.println("Adding mode " + mode +
					    " for " + ircusertarget.nick);
				    if (!textModes.contains(mode)) {
					ircusertarget
						.setModes(textModes + mode);
				    }
				} else {
				    System.out.println("Removing mode " + mode +
					    " for " + ircusertarget.nick);
				    if (textModes.contains(mode)) {
					ircusertarget.setModes(textModes
						.replace(mode, ""));
				    }
				}
			    } else if (split[2].equalsIgnoreCase(Config
				    .getIrcdConsoleChannel())) {
				String consoleTextModes = ircusertarget
					.getConsoleTextModes();
				if (add) {
				    System.out.println("Adding console mode " +
					    mode + " for " +
					    ircusertarget.nick);
				    if (!consoleTextModes.contains(mode)) {
					ircusertarget
						.setConsoleModes(consoleTextModes +
							mode);
				    }
				} else {
				    System.out.println("Removing console mode " +
					    mode + " for " +
					    ircusertarget.nick);
				    if (consoleTextModes.contains(mode)) {
					ircusertarget
						.setConsoleModes(consoleTextModes
							.replace(mode, ""));
				    }
				}
			    }
			} else if (Utils.wildCardMatch(user, "*!*@*")) {
			    if (mode.equals("b")) {
				if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
				    if (add) {
					if (msgIRCBan.length() > 0) {
					    Utils.broadcastMessage(msgIRCBan
						    .replace("{BannedUser}",
							    user).replace(
							    "{BannedBy}",
							    ircuser.nick));
					}
					if ((BukkitIRCdPlugin.dynmap != null) &&
						(msgIRCBanDynmap.length() > 0)) {
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
					}
				    } else {
					if (msgIRCUnban.length() > 0) {
					    Utils.broadcastMessage(msgIRCUnban
						    .replace("{BannedUser}",
							    user).replace(
							    "{BannedBy}",
							    ircuser.nick));
					}
					if ((BukkitIRCdPlugin.dynmap != null) &&
						(msgIRCUnbanDynmap.length() > 0)) {
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
		if (split[5].startsWith(":")) {
		    split[5] = split[5].substring(1);
		}
		String topic = Utils.join(split, " ", 5);

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
	    } else if (split[2]
		    .equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
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
		if (split[3].startsWith(":")) {
		    split[3] = split[3].substring(1);
		}
		String topic = Utils.join(split, " ", 3);

		IRCUser ircuser = null;
		IRCServer server = null;
		if (((ircuser = IRCUserManagement.uid2ircuser.get(UID)) != null) ||
			((server = servers.get(UID)) != null)) {
		    String user;
		    if (ircuser != null) {
			user = ircuser.nick;
		    } else {
			user = server.host;
		    }
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
			BukkitIRCdPlugin.log
				.severe("[BukkitIRCd] UID/Config.getLinkServerID() " +
					UID +
					" not found in list. Error code IRCd1985."); // Log as severe because this situation should never occur and points to a bug in the code
		    }
		}
	    } else if (split[2]
		    .equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
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
	    } else if ((bp = BukkitUserManagement.getUserByUID(target)) != null) {
		idletime = (System.currentTimeMillis() - bp.idleTime) / 1000L;
		signedOn = bp.signedOn;
		success = true;
	    } // The error below can/will happen in the event a player is
	    // /whois'ed from IRC - I'd like to know why and how to fix it
	    else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + target +
			    " not found in list. Error code IRCd1999."); // Log as severe because this situation should never occur and points to a bug in the code															
		}
		success = false;
	    }
	    if (success) {
		Utils.println(":" + target + " IDLE " + source + " " + signedOn + " " +
			idletime);
	    }
	} else if (split[1].equalsIgnoreCase("NICK")) {
	    // :280AAAAAA NICK test 1321981244
	    IRCUser ircuser;
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		BukkitIRCdPlugin.thePlugin.updateLastReceived(ircuser.nick,
			split[2]);
		if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null) &&
			(ircuser.joined)) {
		    if (msgIRCNickChange.length() > 0) {
			Utils.broadcastMessage(msgIRCNickChange
				.replace("{OldNick}", ircuser.nick)
				.replace(
					"{Prefix}",
					IRCFunctionality.getGroupPrefix(ircuser
						.getTextModes()))
				.replace(
					"{Suffix}",
					IRCFunctionality.getGroupSuffix(ircuser
						.getTextModes()))
				.replace("{NewNick}", split[2]));
		    }
		    if ((BukkitIRCdPlugin.dynmap != null) &&
			    (msgIRCNickChangeDynmap.length() > 0)) {
			BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
				"IRC",
				msgIRCNickChangeDynmap.replace("{OldNick}",
					ircuser.nick).replace("{NewNick}",
					split[2]));
		    }
		}
		ircuser.nick = split[2];
		ircuser.isRegistered = false;
	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[2] +
			    " not found in list. Error code IRCd2013."); // Log as severe because this situation should never occur and points to a bug in the code
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
		reason = Utils.join(split, " ", 4);
		if (reason.startsWith(":")) {
		    reason = reason.substring(1);
		}
	    } else {
		reason = null;
	    }

	    if (split[3].startsWith(Integer.toString(Config.getLinkServerID()))) {
		if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
		    if (split[3].equalsIgnoreCase(serverUID)) {
			Utils.println(pre + "FJOIN " + Config.getIrcdChannel() + " " +
				channelTS + " +nt :," + serverUID);
			Utils.println(":" + serverUID + " FMODE " +
				Config.getIrcdChannel() + " " + channelTS +
				" +qaohv " + serverUID + " " + serverUID +
				" " + serverUID + " " + serverUID + " " +
				serverUID);

		    } else if (((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) ||
			    ((server = servers.get(split[0])) != null)) {
			String user;
			if (ircuser != null) {
			    user = ircuser.nick;
			} else {
			    user = server.host;
			}

			BukkitPlayer bp;
			if ((bp = BukkitUserManagement.getUserByUID(split[3])) != null) {
			    if ((IRCd.isPlugin) &&
				    (BukkitIRCdPlugin.thePlugin != null)) {
				BukkitUserManagement.kickPlayerIngame(user, bp.nick, reason);
				BukkitUserManagement.removeBukkitUserByUID(split[3]);
			    }
			} else {
			    if (Config.isDebugModeEnabled()) {
				BukkitIRCdPlugin.log
					.severe("[BukkitIRCd] Bukkit Player UID " +
						split[3] +
						" not found in list. Error code IRCd2051."); // Log
			    }
			}

		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] UID/Config.getLinkServerID() " +
					    split[0] +
					    " not found in list. Error code IRCd2053."); // Log
			}
		    }

		} else if (split[2].equalsIgnoreCase(Config
			.getIrcdConsoleChannel())) {
		    if (split[3].equalsIgnoreCase(serverUID)) {
			Utils.println(pre + "FJOIN " + Config.getIrcdConsoleChannel() +
				" " + consoleChannelTS + " +nt :," +
				serverUID);
			Utils.println(":" + serverUID + " FMODE " +
				Config.getIrcdConsoleChannel() + " " +
				consoleChannelTS + " +qaohv " + serverUID +
				" " + serverUID + " " + serverUID + " " +
				serverUID + " " + serverUID);

		    }
		}
	    } else {
		if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
		    // Main channel
		    if (((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) ||
			    ((server = servers.get(split[0])) != null)) {
			if (ircuser != null) {
			    kicker = ircuser.nick;
			} else {
			    kicker = server.host;
			}
			String modes = "q";
			if (ircuser != null) {
			    modes = ircuser.getTextModes();
			}
			if ((ircvictim = IRCUserManagement.uid2ircuser.get(split[3])) != null) {
			    kicked = ircvictim.nick;
			    if ((IRCd.isPlugin) &&
				    (BukkitIRCdPlugin.thePlugin != null)) {
				if (reason != null) {
				    if (msgIRCKickReason.length() > 0) {
					Utils.broadcastMessage(msgIRCKickReason
						.replace("{KickedUser}", kicked)
						.replace("{KickedBy}", kicker)
						.replace(
							"{Reason}",
							Utils.convertColors(reason,
								true))
						.replace(
							"{KickedPrefix}",
							IRCFunctionality.getGroupPrefix(ircvictim
								.getTextModes()))
						.replace(
							"{KickedSuffix}",
							IRCFunctionality.getGroupSuffix(ircvictim
								.getTextModes()))
						.replace(
							"{KickerPrefix}",
							IRCFunctionality.getGroupPrefix(modes))
						.replace(
							"{KickerSuffix}",
							IRCFunctionality.getGroupSuffix(modes)));
				    }
				    if ((BukkitIRCdPlugin.dynmap != null) &&
					    (msgIRCKickReasonDynmap.length() > 0)) {
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
								Utils.stripIRCFormatting(reason))
							.replace(
								"{KickedPrefix}",
								IRCFunctionality.getGroupPrefix(ircvictim
									.getTextModes()))
							.replace(
								"{KickedSuffix}",
								IRCFunctionality.getGroupSuffix(ircvictim
									.getTextModes()))
							.replace(
								"{KickerPrefix}",
								IRCFunctionality.getGroupPrefix(modes))
							.replace(
								"{KickerSuffix}",
								IRCFunctionality.getGroupSuffix(modes)));
				    }
				} else {
				    if (msgIRCKick.length() > 0) {
					Utils.broadcastMessage(msgIRCKick
						.replace("{KickedUser}", kicked)
						.replace("{KickedBy}", kicker)
						.replace(
							"{KickedPrefix}",
							IRCFunctionality.getGroupPrefix(ircvictim
								.getTextModes()))
						.replace(
							"{KickedSuffix}",
							IRCFunctionality.getGroupSuffix(ircvictim
								.getTextModes()))
						.replace(
							"{KickerPrefix}",
							IRCFunctionality.getGroupPrefix(modes))
						.replace(
							"{KickerSuffix}",
							IRCFunctionality.getGroupSuffix(modes)));
				    }
				    if ((BukkitIRCdPlugin.dynmap != null) &&
					    (msgIRCKickDynmap.length() > 0)) {
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
				}
				ircvictim.joined = false;
			    }
			} else {
			    if (Config.isDebugModeEnabled()) {
				BukkitIRCdPlugin.log
					.severe("[BukkitIRCd] UID " +
						split[3] +
						" not found in list. Error code IRCd2083."); // Log
			    }
			}

		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] UID/Config.getLinkServerID() " +
					    split[0] +
					    " not found in list. Error code IRCd2085."); // Log
			}
		    }

		} else if (split[2].equalsIgnoreCase(Config
			.getIrcdConsoleChannel())) {
		    // Console channel
		    // Only thing important here is to set consolemodes to blank
		    // so they can't execute commands on the console channel
		    // anymore
		    if ((ircvictim = IRCUserManagement.uid2ircuser.get(split[3])) != null) {
			ircvictim.setConsoleModes("");
			ircvictim.consoleJoined = false;
		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] UID " +
					    split[3] +
					    " not found in list. Error code IRCd2094."); // Log
			}
		    }

		}
	    }
	} else if (split[1].equalsIgnoreCase("PART")) {
	    // :280AAAAAA PART #tempcraft.survival :message
	    IRCUser ircuser;
	    String reason;
	    if (split.length > 3) {
		reason = Utils.join(split, " ", 3);
		if (reason.startsWith(":")) {
		    reason = reason.substring(1);
		}
	    } else {
		reason = null;
	    }

	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) {
		// Main channel
		if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		    if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
			if (reason != null) {

			    if (msgIRCLeaveReason.length() > 0) {
				Utils.broadcastMessage(msgIRCLeaveReason
					.replace("{User}", ircuser.nick)
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(ircuser
							.getTextModes()))
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(ircuser
							.getTextModes()))
					.replace("{Reason}",
						Utils.convertColors(reason, true)));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCLeaveReasonDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap
					.sendBroadcastToWeb(
						"IRC",
						msgIRCLeaveReasonDynmap
						.replace("{User}",
							ircuser.nick)
						.replace(
							"{Reason}",
							Utils.stripIRCFormatting(reason)));
			    }
			} else {

			    if (msgIRCLeave.length() > 0) {
				Utils.broadcastMessage(msgIRCLeave
					.replace("{User}", ircuser.nick)
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(ircuser
							.getTextModes()))
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(ircuser
							.getTextModes())));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCLeaveDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					"IRC", msgIRCLeaveDynmap.replace(
						"{User}", ircuser.nick));
			    }
			}
			ircuser.joined = false;
			ircuser.setModes("");
		    }
		} else {
		    if (Config.isDebugModeEnabled()) {
			BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " +
				split[0] +
				" not found in list. Error code IRCd2125."); // Log
		    }
		}

	    } else if (split[2]
		    .equalsIgnoreCase(Config.getIrcdConsoleChannel())) {
		// Console channel
		// Only thing important here is to set oper to false so they
		// can't execute commands on the console channel without being
		// in it
		if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		    ircuser.setConsoleModes("");
		    ircuser.consoleJoined = false;
		} else {
		    if (Config.isDebugModeEnabled()) {
			BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " +
				split[0] +
				" not found in list. Error code IRCd2134."); // Log
		    }
		}

	    }
	} else if (split[1].equalsIgnoreCase("QUIT")) {
	    // :280AAAAAB QUIT :Quit: Connection reset by beer
	    IRCUser ircuser;
	    String reason;
	    if (split.length > 2) {
		reason = Utils.join(split, " ", 2);
		if (reason.startsWith(":")) {
		    reason = reason.substring(1);
		}
	    } else {
		reason = null;
	    }
	    if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		if (ircuser.joined) {
		    // This user is on the plugin channel so broadcast the PART
		    // ingame
		    if ((IRCd.isPlugin) && (BukkitIRCdPlugin.thePlugin != null)) {
			if (reason != null) {

			    if (msgIRCLeaveReason.length() > 0) {
				Utils.broadcastMessage(msgIRCLeaveReason
					.replace("{User}", ircuser.nick)
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(ircuser
							.getTextModes()))
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(ircuser
							.getTextModes()))
					.replace("{Reason}",
						Utils.convertColors(reason, true)));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCLeaveReasonDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap
					.sendBroadcastToWeb(
						"IRC",
						msgIRCLeaveReasonDynmap
						.replace("{User}",
							ircuser.nick)
						.replace(
							"{Reason}",
							Utils.stripIRCFormatting(reason)));
			    }
			} else {

			    if (msgIRCLeave.length() > 0) {
				Utils.broadcastMessage(msgIRCLeave
					.replace("{User}", ircuser.nick)
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(ircuser
							.getTextModes()))
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(ircuser
							.getTextModes())));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCLeaveDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					"IRC", msgIRCLeaveDynmap.replace(
						"{User}", ircuser.nick));
			    }
			}
		    }
		    ircuser.setConsoleModes("");
		    ircuser.setModes("");
		    ircuser.joined = false;
		    ircuser.consoleJoined = false;
		}
		IRCUserManagement.uid2ircuser.remove(split[0]);
	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] +
			    " not found in list. Error code IRCd2166."); // Log
		}
	    }

	} else if (split[1].equalsIgnoreCase("KILL")) {
			// :280AAAAAA KILL 123AAAAAA :Killed (test (testng))

	    // If an ingame user is killed, reconnect them to IRC.
	    IRCUser ircuser, ircuser2;
	    IRCServer server = null;
	    String user;
	    if ((((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null)) ||
		    ((server = servers.get(split[0])) != null)) {
		if (ircuser != null) {
		    user = ircuser.nick;
		} else {
		    user = server.host;
		}
		synchronized (csBukkitPlayers) {
		    BukkitPlayer bp;
		    if (split[2].equalsIgnoreCase(serverUID)) {
			Utils.println(pre + "UID " + serverUID + " " +
				serverStartTime + " " +
				Config.getIrcdServerName() + " " +
				Config.getIrcdServerHostName() + " " +
				Config.getIrcdServerHostName() + " " +
				Config.getIrcdServerName() + " 127.0.0.1 " +
				serverStartTime + " +Bro :" +
				BukkitIRCdPlugin.ircdVersion);
			Utils.println(":" + serverUID + " OPERTYPE Network_Service");
			Utils.println(pre + "FJOIN " + Config.getIrcdConsoleChannel() +
				" " + consoleChannelTS + " +nt :," +
				serverUID);
			Utils.println(":" + serverUID + " FMODE " +
				Config.getIrcdConsoleChannel() + " " +
				consoleChannelTS + " +qaohv " + serverUID +
				" " + serverUID + " " + serverUID + " " +
				serverUID + " " + serverUID);
			Utils.println(pre + "FJOIN " + Config.getIrcdChannel() + " " +
				channelTS + " +nt :," + serverUID);
			Utils.println(":" + serverUID + " FMODE " +
				Config.getIrcdChannel() + " " + channelTS +
				" +qaohv " + serverUID + " " + serverUID +
				" " + serverUID + " " + serverUID + " " +
				serverUID);
		    } else if ((bp = BukkitUserManagement.getUserByUID(split[2])) != null) {
			String UID = bp.getUID();
			String textMode = bp.getTextMode();
			if (bp.hasPermission("bukkitircd.oper")) {
			    Utils.println(pre + "UID " + UID + " " +
				    (bp.idleTime / 1000L) + " " + bp.nick +
				    Config.getIrcdIngameSuffix() + " " +
				    bp.realhost + " " + bp.host + " " +
				    bp.nick + " " + bp.ip + " " + bp.signedOn +
				    " +or :Minecraft Player");
			    Utils.println(":" + UID + " OPERTYPE IRC_Operator");
			} else {
			    Utils.println(pre + "UID " + UID + " " +
				    (bp.idleTime / 1000L) + " " + bp.nick +
				    Config.getIrcdIngameSuffix() + " " +
				    bp.realhost + " " + bp.host + " " +
				    bp.nick + " " + bp.ip + " " + bp.signedOn +
				    " +r :Minecraft Player");
			}

			Utils.println(pre + "FJOIN " + Config.getIrcdChannel() + " " +
				channelTS + " +nt :," + UID);
			if (textMode.length() > 0) {
			    String modestr = "";
			    for (int i = 0; i < textMode.length(); i++) {
				modestr += UID + " ";
			    }
			    modestr = modestr
				    .substring(0, modestr.length() - 1);
			    Utils.println(":" + serverUID + " FMODE " +
				    Config.getIrcdChannel() + " " + channelTS +
				    " + " + textMode + " " + modestr);
			}
			String world = bp.getWorld();
			if (world != null) {
			    Utils.println(pre + "METADATA " + UID +
				    " swhois :is currently in " + world);
			} else {
			    Utils.println(pre +
				    "METADATA " +
				    UID +
				    " swhois :is currently in an unknown world");
			}
		    } else if ((ircuser2 = IRCUserManagement.uid2ircuser.get(split[2])) != null) {
			String reason;
			reason = Utils.join(split, " ", 3);
			if (reason.startsWith(":")) {
			    reason = reason.substring(1);
			}
			if (ircuser2.joined) {

			    if (msgIRCLeaveReason.length() > 0) {
				Utils.broadcastMessage(msgIRCLeaveReason
					.replace("{User}", user)
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(ircuser
							.getTextModes()))
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(ircuser
							.getTextModes()))
					.replace("{Reason}",
						Utils.convertColors(reason, true)));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCLeaveReasonDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					"IRC",
					msgIRCLeaveReasonDynmap.replace(
						"{User}", user).replace(
						"{Reason}",
						Utils.stripIRCFormatting(reason)));
			    }
			    ircuser2.setConsoleModes("");
			    ircuser2.setModes("");
			    ircuser2.joined = false;
			    ircuser2.consoleJoined = false;
			}
			IRCUserManagement.uid2ircuser.remove(split[2]);
		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] UID " +
					    split[2] +
					    " not found in list. Error code IRCd2224."); // Log
			}
		    }

		}

	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log
			    .severe("[BukkitIRCd] UID/Config.getLinkServerID() " +
				    split[0] +
				    " not found in list. Error code IRCd2228."); // Log
		}
	    }

	} else if (split[1].equalsIgnoreCase("PRIVMSG") ||
		split[1].equalsIgnoreCase("NOTICE")) {
	    // :280AAAAAA PRIVMSG 123AAAAAA :test
	    if (split[3].startsWith(":")) {
		split[3] = split[3].substring(1);
	    }
	    if (split[2].startsWith(":")) {
		split[2] = split[2].substring(1);
	    }
	    String message = Utils.join(split, " ", 3);
	    String msgtemplate = "";
	    String msgtemplatedynmap = "";
	    boolean isCTCP = (message.startsWith((char) 1 + "") && message
		    .endsWith((char) 1 + ""));
	    boolean isAction = (message.startsWith((char) 1 + "ACTION") && message
		    .endsWith((char) 1 + ""));
	    boolean isNotice = split[1].equalsIgnoreCase("NOTICE");
	    if (isCTCP && (!isAction)) {
		return; // Ignore CTCP's (except actions)
	    } else if (isCTCP && isNotice) {
		return; // CTCP reply, ignore this
	    }
	    if (isNotice && (!Config.isIrcdNoticesEnabled())) {
		return; // Ignore notices if notices are disabled.
	    }
	    IRCUser ircuser;
	    String uidfrom = split[0];
	    if ((ircuser = IRCUserManagement.uid2ircuser.get(split[0])) != null) {
		synchronized (csBukkitPlayers) {
		    BukkitPlayer bp;
		    if (split[2].equalsIgnoreCase(Config.getIrcdChannel())) { // Messaging
			// the
			// public
			// channel
			if (isAction) {
			    msgtemplate = msgIRCAction;
			    msgtemplatedynmap = msgIRCActionDynmap;
			    message = Utils.join(
				    message.substring(1, message.length() - 1)
				    .split(" "), " ", 1);
			} else if (isNotice) {
			    msgtemplate = msgIRCNotice;
			    msgtemplatedynmap = msgIRCNoticeDynmap;
			} else {
			    msgtemplate = msgIRCMessage;
			    msgtemplatedynmap = msgIRCMessageDynmap;
			}
			if ((IRCd.isPlugin) &&
				(BukkitIRCdPlugin.thePlugin != null)) {
			    if (message.equalsIgnoreCase("!players") &&
				    (!isAction) && (!isNotice)) {
				if (msgPlayerList.length() > 0) {
				    String s = "";
				    int count = 0;
				    for (BukkitPlayer player : bukkitPlayers) {
					count++;
					s = s + player.nick + ", ";
				    }
				    if (s.length() == 0) {
					s = "None, ";
				    }
				    Utils.println(":" +
					    serverUID +
					    " PRIVMSG " +
					    Config.getIrcdChannel() +
					    " :" +
					    Utils.convertColors(
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

				if (msgtemplate.length() > 0) {
				    String msg = msgtemplate
					    .replace("{User}", ircuser.nick)
					    .replace(
						    "{Suffix}",
						    IRCFunctionality.getGroupSuffix(ircuser
							    .getTextModes()))
					    .replace(
						    "{Prefix}",
						    IRCFunctionality.getGroupPrefix(ircuser
							    .getTextModes()))
					    // TODO Player Highlight
					    // .replace(
					    // ,
					    // "&b" + "&r")
					    .replace(
						    "{Message}",
						    Utils.convertColors(message,
							    true));
				    if (Config.isIrcdIngameSuffixStripEnabled()) {
					msg = msg.replace(
						Config.getIrcdIngameSuffix(),
						"");
				    }

				    Utils.broadcastMessage(msg);
				}
				if ((BukkitIRCdPlugin.dynmap != null) &&
					(msgtemplatedynmap.length() > 0)) {
				    BukkitIRCdPlugin.dynmap
					    .sendBroadcastToWeb(
						    "IRC",
						    msgtemplatedynmap
						    .replace(
							    "{User}",
							    ircuser.nick)
						    .replace(
							    "{Message}",
							    Utils.stripIRCFormatting(message)));
				}
			    }
			}
		    } else if (split[2].equalsIgnoreCase(Config
			    .getIrcdConsoleChannel())) { // Messaging
			// the
			// console
			// channel
			if (message.startsWith("!") && (!isAction) &&
				(!isNotice)) {
			    if (!ircuser.getConsoleTextModes().contains("o")) {
				Utils.println(":" +
					serverUID +
					" NOTICE " +
					uidfrom +
					" :You are not a channel operator (or above). Command failed."); // Only
			    } // let them execute commands if they're oper
			    else {
				message = message.substring(1);
				Utils.executeCommand(message);
			    }
			}
		    } else if (split[2].equalsIgnoreCase(serverUID)) { // Messaging
			// the
			// console
			if ((IRCd.isPlugin) &&
				(BukkitIRCdPlugin.thePlugin != null)) {
			    if (isAction) {
				msgtemplate = msgIRCPrivateAction;
				message = Utils.join(
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

			    if (msgtemplate.length() > 0) {
				BukkitIRCdPlugin.log.info(msgtemplate
					.replace("{User}", ircuser.nick)
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(ircuser
							.getTextModes()))
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(ircuser
							.getTextModes()))
					.replace(
						"{Message}",
						Utils.convertColors(message,
							true)));
			    }
			}
		    } else if ((bp = BukkitUserManagement.getUserByUID(split[2])) != null) { // Messaging
			// an
			// ingame
			// user
			if ((isAction || (!isCTCP)) && (IRCd.isPlugin) &&
				(BukkitIRCdPlugin.thePlugin != null)) {
			    if (isAction) {
				msgtemplate = msgIRCPrivateAction;
				message = Utils.join(
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

				if (msgtemplate.length() > 0) {
				    Utils.sendMessage(
					    bukkitnick,
					    msgtemplate
					    .replace("{User}",
						    ircuser.nick)
					    .replace(
						    "{Suffix}",
						    IRCFunctionality.getGroupSuffix(ircuser
							    .getTextModes()))
					    .replace(
						    "{Prefix}",
						    IRCFunctionality.getGroupPrefix(ircuser
							    .getTextModes()))
					    .replace(
						    "{Message}",
						    Utils.convertColors(
							    message,
							    true)));
				}
			    }
			}
		    }
		    // Ignore messages from other channels
		}

	    } else {
		if (Config.isDebugModeEnabled()) {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] UID " + split[0] +
			    " not found in list. Error code IRCd2336."); // Log
		}
	    }

	} else if (split[1].equalsIgnoreCase("METADATA")) {
	    // :00A METADATA 854AAAABZ accountname :glguy
	    final String target = split[2];
	    final String key = split[3];

	    final String value;
	    if (split[4].startsWith(":")) {
		split[4] = split[4].substring(1);
		value = Utils.join(split, " ", 4);
	    } else {
		value = split[4];
	    }

	    final IRCUser user = IRCUserManagement.uid2ircuser.get(target);
	    if (user != null) {
		if (key.equalsIgnoreCase("accountname")) {
		    user.accountname = value;
		}
	    }
	}

	// End of IF command check
    }

    public static boolean isPlugin() {
	return isPlugin;
    }

    public static boolean isLinkcompleted() {
	return linkcompleted;
    }
}
