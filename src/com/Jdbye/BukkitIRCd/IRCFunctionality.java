package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.bukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.channelTS;
import static com.Jdbye.BukkitIRCd.IRCd.channelTopic;
import static com.Jdbye.BukkitIRCd.IRCd.channelTopicSet;
import static com.Jdbye.BukkitIRCd.IRCd.channelTopicSetDate;
import static com.Jdbye.BukkitIRCd.IRCd.consoleChannelTS;
import static com.Jdbye.BukkitIRCd.IRCd.csIrcUsers;
import static com.Jdbye.BukkitIRCd.IRCd.csServer;
import static com.Jdbye.BukkitIRCd.IRCd.isPlugin;
import static com.Jdbye.BukkitIRCd.IRCd.linkcompleted;
import static com.Jdbye.BukkitIRCd.IRCd.listener;
import static com.Jdbye.BukkitIRCd.IRCd.mode;
import static com.Jdbye.BukkitIRCd.IRCd.msgDelinkedReason;
import static com.Jdbye.BukkitIRCd.IRCd.pre;
import static com.Jdbye.BukkitIRCd.IRCd.server;
import static com.Jdbye.BukkitIRCd.IRCd.serverStartTime;
import static com.Jdbye.BukkitIRCd.IRCd.serverUID;
import static com.Jdbye.BukkitIRCd.IRCd.ugen;
import com.Jdbye.BukkitIRCd.configuration.Config;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class IRCFunctionality {

    /**
    Configure the channel topic, both main and staff if it's standalone, only main if it's linked
    @param topic The topic for the channel
    @param user The username of the person setting the topic
    @param userhost The host of the user
    */
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
	    writeAll(":" + userhost + " TOPIC " + Config.getIrcdChannel() + " :" + channelTopic);
	    writeOpers(":" + userhost + " TOPIC " + Config.getIrcdConsoleChannel() + " :" + channelTopic);
	} else if (mode == Modes.INSPIRCD) {
	    BukkitPlayer bp;
	    if ((bp = BukkitUserManagement.getUserObject(user)) != null) {
		Utils.println(":" + bp.getUID() + " TOPIC " + Config.getIrcdChannel() + " :" + channelTopic);
	    }
	}
    }

    /**
    Disconnects/unlinks an Inspircd link
    @param reason The reason for the disconnect
    */
    public static void disconnectServer(String reason) {
	synchronized (csServer) {
	    if (mode == Modes.INSPIRCD) {
		if ((server != null) && server.isConnected()) {
		    Utils.println(pre + "SQUIT " + Config.getLinkServerID() + " :" +
			    reason);
		    if (linkcompleted) {
			if (reason != null && msgDelinkedReason.length() > 0) {
			    Utils.broadcastMessage(msgDelinkedReason.replace(
				    "{LinkName}", Config.getLinkName())
				    .replace("{Reason}", reason));
			}
			linkcompleted = false;
		    }
		    try {
			server.close();
		    } catch (IOException e) {
		    }
		} else if (Config.isDebugModeEnabled()) {
		    System.out.println("[BukkitIRCd] Already disconnected from link, so no need to cleanup.");
		}
	    }
	}
	if (IRCd.listener != null) {
	    try {
		IRCd.listener.close();
	    } catch (IOException e) {
		// TODO Original developer left this blank, we want to catch errors properly, or not cause them at all
	    }
	}
    }

    /**
    Connect to InspIRCd link
    @return Whether the reconnect was successful
    */
    public static boolean connect() {
	if (mode == Modes.INSPIRCD) {
	    BukkitIRCdPlugin.log.info("[BukkitIRCd] Attempting connection to " + Config.getLinkRemoteHost() + ":" + Config.getLinkRemoteHost());
	    try {
		server = new Socket(Config.getLinkRemoteHost(), Config.getLinkRemotePort());
		if ((server != null) && server.isConnected()) {
		    BukkitIRCdPlugin.log.info("[BukkitIRCd] Connected to " + Config.getLinkRemoteHost() + ":" + Config.getLinkRemotePort());
		    IRCd.isIncoming = false;
		    return true;
		} else {
		    BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed connection to " + Config.getLinkRemoteHost() + ":" + Config.getLinkRemotePort());
		}
	    } catch (IOException e) {
		BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed connection to " + Config.getLinkRemoteHost() + ":" + Config.getLinkRemotePort() + " (" + e + ")");
	    }
	}
	return false;
    }

    /**
    Sends the required list of capabilities when linking
    @return Whether the capabilties have been sent
    */
    public static boolean sendLinkCAPAB() {
	if (IRCd.capabSent) {
	    return false;
	}
	Utils.println("CAPAB START 1201");
	Utils.println("CAPAB CAPABILITIES :NICKMAX=" + (Config.getIrcdMaxNickLength() + 1) +
		" CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
	// Utils.println("CAPAB CHANMODES :admin=&a ban=b founder=~q halfop=%h op=@o operonly=O voice=+ v");
	// // Don't send this line, the server will complain that we don't
	// support various modes and refuse to link
	// Utils.println("CAPAB USERMODES :bot=B oper=o u_registered=r"); // Don't
	// send this line, the server will complain that we don't support
	// various modes and refuse to link
	Utils.println("CAPAB END");
	Utils.println("SERVER " + Config.getIrcdServerHostName() + " " +
		Config.getLinkConnectPassword() + " 0 " +
		Config.getLinkServerID() + " :" +
		Config.getIrcdServerDescription());
	IRCd.capabSent = true;
	return true;
    }

    /**
    Sends the required link burst information, including the version of the plugin, UID, and the opertype (network service)
    @return Whether the burst was sent
    */
    public static boolean sendLinkBurst() {
	if (IRCd.burstSent) {
	    return false;
	}
	Utils.println(pre + "BURST " + (System.currentTimeMillis() / 1000L));
	Utils.println(pre + "VERSION :" + BukkitIRCdPlugin.ircdVersion);

	Utils.println(pre + "UID " + serverUID + " " + serverStartTime + " " +
		Config.getIrcdServerName() + " " +
		Config.getIrcdServerHostName() + " " +
		Config.getIrcdServerHostName() + " " +
		Config.getIrcdServerName() + " 127.0.0.1 " + serverStartTime +
		" +Bro :" + BukkitIRCdPlugin.ircdVersion);
	Utils.println(":" + serverUID + " OPERTYPE Network_Service");

	for (BukkitPlayer bp : bukkitPlayers) {
	    String UID = ugen.generateUID(Config.getLinkServerID());
	    bp.setUID(UID);
	    if (bp.hasPermission("bukkitircd.oper")) {
		Utils.println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " " +
			bp.nick + Config.getIrcdIngameSuffix() + " " +
			bp.realhost + " " + bp.host + " " + bp.nick + " " +
			bp.ip + " " + bp.signedOn + " +or :Minecraft Player");
		Utils.println(":" + UID + " OPERTYPE IRC_Operator");
	    } else {
		Utils.println(pre + "UID " + UID + " " + (bp.idleTime / 1000L) + " " +
			bp.nick + Config.getIrcdIngameSuffix() + " " +
			bp.realhost + " " + bp.host + " " + bp.nick + " " +
			bp.ip + " " + bp.signedOn + " +r :Minecraft Player");
	    }

	    String world = bp.getWorld();
	    if (world != null) {
		Utils.println(pre + "METADATA " + UID + " swhois :is currently in " + world);
	    } else {
		Utils.println(pre + "METADATA " + UID + " swhois :is currently in an unknown world");
	    }
	}

	Utils.println(pre + "FJOIN " + Config.getIrcdConsoleChannel() + " " + consoleChannelTS + " +nt :qaohv," + serverUID);
	Utils.println(pre + "FJOIN " + Config.getIrcdChannel() + " " + channelTS + " +nt :qaohv," + serverUID);

	int avail = 0;
	StringBuilder sb = null;
	for (BukkitPlayer bp : bukkitPlayers) {

	    final String nextPart = bp.getTextMode() + "," + bp.getUID();

	    if (nextPart.length() > avail) {
		// flush
		if (sb != null) {
		    Utils.println(sb.toString());
		}

		sb = new StringBuilder(400);
		sb.append(pre).append("FJOIN ").append(Config.getIrcdChannel()).append(' ').append(channelTS).append(" +nt :").append(nextPart);
		avail = 409 - sb.length();
	    } else {
		sb.append(' ').append(nextPart);
		avail -= nextPart.length();
	    }
	}

	// flush
	if (sb != null) {
	    Utils.println(sb.toString());
	}

	Utils.println(pre + "ENDBURST");
	IRCd.burstSent = true;
	return true;
    }

    /**
     Gets group prefix from modes
     <p>
     @param modes
     <p>
     @return
     */
    public static String getGroupPrefix(String modes) {
	// Goes from highest rank to lowest rank
	String prefix; // Owner

	if (IRCd.groupPrefixes == null) {
	    return "";
	}
	if (IRCd.groupPrefixes.contains("q") &&
		(modes.contains("q") || modes.contains("~"))) {
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
	if (IRCd.groupPrefixes.contains("a") &&
		(modes.contains("a") || modes.contains("&"))) {
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
	if (IRCd.groupPrefixes.contains("o") &&
		(modes.contains("o") || modes.contains("@"))) {
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
	if (IRCd.groupPrefixes.contains("h") &&
		(modes.contains("h") || modes.contains("%"))) {
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
	if (IRCd.groupPrefixes.contains("q") &&
		(modes.contains("v") || modes.contains("+"))) {
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
     Gets group suffix from modes
     <p>
     @param modes
     <p>
     @return
     */
    public static String getGroupSuffix(String modes) {
	// Goes from highest rank to lowest rank
	String suffix;

	if (IRCd.groupSuffixes == null) {
	    return "";
	}
	// Owner
	if (IRCd.groupSuffixes.contains("q") &&
		(modes.contains("q") || modes.contains("~"))) {
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
	if (IRCd.groupSuffixes.contains("a") &&
		(modes.contains("a") || modes.contains("&"))) {
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
	if (IRCd.groupSuffixes.contains("o") &&
		(modes.contains("o") || modes.contains("@"))) {
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
	if (IRCd.groupSuffixes.contains("h") &&
		(modes.contains("h") || modes.contains("%"))) {
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
	if (IRCd.groupSuffixes.contains("v") &&
		(modes.contains("v") || modes.contains("+"))) {
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

    /**
     @param source
     UID of sender
     @param target
     name of target
     @param message
     message to send encoded with IRC colors
     */
    public static void privmsg(final String source, final String target, final String message) {
	Utils.println(":" + source + " PRIVMSG " + target + " :" + message);
    }

    /**
    Performs an action (/me) to a channel
     @param source UID of sender
     @param target name of target
     @param message message to send with IRC colors
     */
    public static void action(final String source, final String target, final String message) {
	final String action = (char) 1 + "ACTION " + message + (char) 1;
	privmsg(source, target, action);
    }

    public static boolean writeTo(String nick, String line) {
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		Iterator<ClientConnection> iter = IRCUserManagement.clientConnections.iterator();
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

    public static void disconnectAll() {
	disconnectAll(null);
    }

    /**
    Disconnects all players from IRC
    @param reason 
    */
    public static void disconnectAll(String reason) {
	synchronized (csIrcUsers) {
	    switch (mode) {
		case STANDALONE:
		    try {
			listener.close();
			listener = null;
		    } catch (IOException e) {
		    }
		    IRCUserManagement.removeIRCUsers(reason);
		    break;
		case INSPIRCD:
		    disconnectServer(reason);
		    break;
	    }
	}
    }

    public static void writeAll(String message, Player sender) {
	int i = 0;
	String line = "", host = "unknown", nick = "Unknown";

	synchronized (IRCd.csBukkitPlayers) {
	    int ID = BukkitUserManagement.getUser(sender.getName());
	    if (ID >= 0) {
		BukkitPlayer bp = bukkitPlayers.get(ID);
		host = bp.host;
		nick = bp.nick;
	    }
	}

	line = ":" + nick + Config.getIrcdIngameSuffix() + "!" + nick + "@" + host + " PRIVMSG " + Config.getIrcdChannel() + " :" + message;

	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		while (i < IRCUserManagement.clientConnections.size()) {
		    processor = IRCUserManagement.clientConnections.get(i);
		    if ((processor.isConnected()) && processor.isIdented && processor.isNickSet &&
			    (processor.lastPingResponse + (Config.getIrcdPinkTimeoutInterval() * 1000) > System.currentTimeMillis())) {
			processor.writeln(line);
			i++;
		    } else if (!processor.running) {
			IRCUserManagement.removeIRCUser(processor.nick);
		    } else {
			i++;
		    }
		}
	    }
	}
    }

    public static void writeAll(String line) {
	int i = 0;
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		while (i < IRCUserManagement.clientConnections.size()) {
		    processor = IRCUserManagement.clientConnections.get(i);
		    if ((processor.isConnected()) &&
			    processor.isIdented &&
			    processor.isNickSet &&
			    (processor.lastPingResponse +
			    (Config.getIrcdPinkTimeoutInterval() * 1000) > System
			    .currentTimeMillis())) {
			processor.writeln(line);
			i++;
		    } else if (!processor.running) {
			IRCUserManagement.removeIRCUser(processor.nick);
		    } else {
			i++;
		    }
		}
	    }
	}
    }

    public static void writeOpers(String line) {
	int i = 0;
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		while (i < IRCUserManagement.clientConnections.size()) {
		    processor = IRCUserManagement.clientConnections.get(i);
		    if ((processor.isConnected()) &&
			    processor.isIdented &&
			    processor.isNickSet &&
			    processor.isOper &&
			    (processor.lastPingResponse +
			    (Config.getIrcdPinkTimeoutInterval() * 1000) > System
			    .currentTimeMillis())) {
			processor.writeln(line);
			i++;
		    } else if (!processor.running) {
			IRCUserManagement.removeIRCUser(processor.nick);
		    } else {
			i++;
		    }
		}
	    }
	}
    }

    public static void writeAllExcept(String nick, String line) {
	int i = 0;
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		while (i < IRCUserManagement.clientConnections.size()) {
		    processor = IRCUserManagement.clientConnections.get(i);
		    if (processor.nick.equalsIgnoreCase(nick)) {
			i++;
			continue;
		    }
		    if ((processor.isConnected()) &&
			    processor.isIdented &&
			    processor.isNickSet &&
			    (processor.lastPingResponse +
			    (Config.getIrcdPinkTimeoutInterval() * 1000) > System
			    .currentTimeMillis())) {
			processor.writeln(line);
			i++;
		    } else if (!processor.running) {
			IRCUserManagement.removeIRCUser(processor.nick);
		    } else {
			i++;
		    }
		}
	    }
	}
    }

    public static void writeOpersExcept(String nick, String line) {
	int i = 0;
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		while (i < IRCUserManagement.clientConnections.size()) {
		    processor = IRCUserManagement.clientConnections.get(i);
		    if (processor.nick.equalsIgnoreCase(nick)) {
			i++;
			continue;
		    }
		    if ((processor.isConnected()) &&
			    processor.isIdented &&
			    processor.isNickSet &&
			    processor.isOper &&
			    (processor.lastPingResponse +
			    (Config.getIrcdPinkTimeoutInterval() * 1000) > System
			    .currentTimeMillis())) {
			processor.writeln(line);
			i++;
		    } else if (!processor.running) {
			IRCUserManagement.removeIRCUser(processor.nick);
		    } else {
			i++;
		    }
		}
	    }
	}
    }
}
