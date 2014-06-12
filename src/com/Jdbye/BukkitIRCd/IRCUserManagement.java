package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.bukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.channelTS;
import static com.Jdbye.BukkitIRCd.IRCd.csBukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.csIrcBans;
import static com.Jdbye.BukkitIRCd.IRCd.csIrcUsers;
import static com.Jdbye.BukkitIRCd.IRCd.dateFormat;
import static com.Jdbye.BukkitIRCd.IRCd.ircBans;
import static com.Jdbye.BukkitIRCd.IRCd.mode;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCBan;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCBanDynmap;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKick;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKickDynmap;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKickReason;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKickReasonDynmap;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCLeave;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCLeaveDynmap;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCLeaveReason;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCLeaveReasonDynmap;
import static com.Jdbye.BukkitIRCd.IRCd.serverUID;
import static com.Jdbye.BukkitIRCd.IRCd.servers;
import com.Jdbye.BukkitIRCd.configuration.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;

public class IRCUserManagement {

    public static List<ClientConnection> clientConnections = new LinkedList<ClientConnection>();
    public static HashMap<String, IRCUser> uid2ircuser = new HashMap<String, IRCUser>();

    public static IRCUser getIRCUser(String nick) {
	synchronized (csIrcUsers) {
	    int i = 0;
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		while (i < clientConnections.size()) {
		    processor = clientConnections.get(i);
		    if ((processor != null) &&
			    (processor.nick.equalsIgnoreCase(nick))) {
			return new IRCUser(processor.nick, processor.realname,
				processor.ident, processor.hostmask,
				processor.ipaddress, processor.modes,
				processor.customWhois, processor.isRegistered,
				processor.isOper, processor.awayMsg,
				processor.signonTime, processor.lastActivity,
				"");
		    }
		    i++;
		}
	    } else if (mode == Modes.INSPIRCD) {
		IRCUser iuser;
		Iterator<?> iter = uid2ircuser.entrySet().iterator();
		while (iter.hasNext()) {
		    @SuppressWarnings("unchecked")
		    Map.Entry<String, IRCUser> entry = (Map.Entry<String, IRCUser>) iter
			    .next();
		    iuser = entry.getValue();
		    if (iuser.nick.equalsIgnoreCase(nick)) {
			return iuser;
		    }
		}
	    }
	}
	return null;
    }

    public static String getUIDFromIRCUser(IRCUser user) {
	Iterator<Map.Entry<String, IRCUser>> iter = IRCUserManagement.uid2ircuser.entrySet()
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
	Iterator<Map.Entry<String, IRCUser>> iter = IRCUserManagement.uid2ircuser.entrySet()
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
	if ((ircUsers != null) && (ircUsers instanceof IRCUser[])) {
	    return (IRCUser[]) ircUsers;
	} else {
	    return new IRCUser[0];
	}
    }

    public static String getUsers() {
	String users = "";
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		for (ClientConnection processor : clientConnections) {
		    String nick;
		    if (processor.modes.length() > 0) {
			nick = processor.modes.substring(0, 1) + processor.nick;
		    } else {
			nick = processor.nick;
		    }
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
		    if (modes.length() > 0) {
			nick = modes.substring(0, 1) + user.nick;
		    } else {
			nick = user.nick;
		    }
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
		if (modes.length() > 0) {
		    nick2 = modes.substring(0, 1) + nick +
			    Config.getIrcdIngameSuffix();
		} else {
		    nick2 = nick + Config.getIrcdIngameSuffix();
		}
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
			if (processor.modes.length() > 0) {
			    nick = processor.modes.substring(0, 1) +
				    processor.nick;
			} else {
			    nick = processor.nick;
			}
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
			if (modes.length() > 0) {
			    nick = modes.substring(0, 1) + user.nick;
			} else {
			    nick = user.nick;
			}
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
		    if (processor.isIdented && processor.isNickSet) {
			users.add(processor.nick);
		    }
		}
	    } else if (mode == Modes.INSPIRCD) {
		for (Object user : uid2ircuser.values().toArray()) {
		    if (((IRCUser) user).joined) {
			users.add(((IRCUser) user).nick);
		    }
		}
	    }
	}
	String userArray[] = new String[0];
	userArray = users.toArray(userArray);
	Arrays.sort(userArray);
	return userArray;
    }

    public static Collection<String> getIRCWhois(final IRCUser ircuser,
	    final boolean isOper) {
	if (ircuser == null) {
	    return null;
	}
	ArrayList<String> whois = new ArrayList<String>(10);
	synchronized (csIrcUsers) {
	    String idletime = TimeUtils.millisToLongDHMS(ircuser
		    .getSecondsIdle() * 1000);
	    whois.add(ChatColor.DARK_GREEN + "Nickname: " + ChatColor.GRAY +
		    ircuser.nick + ChatColor.WHITE);
	    whois.add(ChatColor.DARK_GREEN + "Ident: " + ChatColor.GRAY +
		    ircuser.ident + ChatColor.WHITE);
	    whois.add(ChatColor.DARK_GREEN + "Hostname: " + ChatColor.GRAY +
		    ircuser.hostmask + ChatColor.WHITE);
	    if (isOper && !ircuser.hostmask.equalsIgnoreCase(ircuser.realhost)) {
		whois.add(ChatColor.DARK_GREEN + "Real Hostname: " +
			ChatColor.GRAY + ircuser.realhost + ChatColor.WHITE);
	    }
	    whois.add(ChatColor.DARK_GREEN + "Realname: " + ChatColor.GRAY +
		    ircuser.realname + ChatColor.WHITE);
	    whois.add(ChatColor.DARK_GREEN + "Is registered: " + ChatColor.GRAY +
		    (ircuser.isRegistered ? "Yes" : "No") + ChatColor.WHITE);
	    if (!ircuser.accountname.isEmpty()) {
		whois.add(ChatColor.DARK_GREEN + "Account name: " +
			ChatColor.GRAY + ircuser.accountname +
			ChatColor.WHITE);
	    }
	    whois.add(ChatColor.DARK_GREEN + "Is operator: " + ChatColor.GRAY +
		    (ircuser.isOper ? "Yes" : "No") + ChatColor.WHITE);
	    whois.add(ChatColor.DARK_GREEN + "Away: " + ChatColor.GRAY +
		    ((!ircuser.awayMsg.equals("")) ? ircuser.awayMsg : "No") +
		    ChatColor.WHITE);
	    whois.add(ChatColor.DARK_GREEN + "Idle " + ChatColor.GRAY +
		    idletime + ChatColor.WHITE);
	    whois.add(ChatColor.DARK_GREEN + "Signed on at " + ChatColor.GRAY +
		    dateFormat.format(ircuser.signonTime * 1000) +
		    ChatColor.WHITE);
	}
	return whois;
    }

    public static boolean removeIRCUser(String nick) {
	return removeIRCUser(nick, null, false);
    }

    public static boolean removeIRCUser(String nick, String reason, boolean IRCToGame) {
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		Iterator<ClientConnection> iter = clientConnections.iterator();
		while (iter.hasNext()) {
		    ClientConnection processor = iter.next();
		    if (processor.nick.equalsIgnoreCase(nick)) {
			if (processor.isIdented && processor.isNickSet) {
			    if ((IRCd.isPlugin) &&
				    (BukkitIRCdPlugin.thePlugin != null)) {
				BukkitIRCdPlugin.thePlugin
					.removeLastReceivedFrom(processor.nick);
				if (reason != null) {
				    if (msgIRCLeave.length() > 0) {
					Utils.broadcastMessage(msgIRCLeaveReason
						.replace("{User}",
							processor.nick)
						.replace(
							"{Suffix}",
							IRCFunctionality.getGroupSuffix(processor.modes))
						.replace(
							"{Prefix}",
							IRCFunctionality.getGroupPrefix(processor.modes))
						.replace(
							"{Reason}",
							Utils.convertColors(reason,
								IRCToGame)));
				    }
				    if ((BukkitIRCdPlugin.dynmap != null) &&
					    (msgIRCLeaveDynmap.length() > 0)) {
					BukkitIRCdPlugin.dynmap
						.sendBroadcastToWeb(
							"IRC",
							msgIRCLeaveReasonDynmap
							.replace(
								"{User}",
								processor.nick)
							.replace(
								"{Reason}",
								Utils.stripIRCFormatting(reason)));
				    }
				} else {
				    if (msgIRCLeave.length() > 0) {
					Utils.broadcastMessage(msgIRCLeave
						.replace("{User}",
							processor.nick)
						.replace(
							"{Suffix}",
							IRCFunctionality.getGroupSuffix(processor.modes))
						.replace(
							"{Prefix}",
							IRCFunctionality.getGroupPrefix(processor.modes)));
				    }
				    if ((BukkitIRCdPlugin.dynmap != null) &&
					    (msgIRCLeaveDynmap.length() > 0)) {
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
			}
			iter.remove();
			if (processor.isConnected()) {
			    processor.disconnect();
			}
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
			if ((IRCd.isPlugin) &&
				(BukkitIRCdPlugin.thePlugin != null)) {
			    BukkitIRCdPlugin.thePlugin
				    .removeLastReceivedFrom(processor.nick);
			    if (msgIRCLeave.length() > 0 && reason != null) {
				Utils.broadcastMessage(msgIRCLeaveReason
					.replace("{User}", processor.nick)
					.replace(
						"{Prefix}",
						IRCFunctionality.getGroupPrefix(processor.modes))
					.replace(
						"{Suffix}",
						IRCFunctionality.getGroupSuffix(processor.modes))
					.replace("{Reason}", reason));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCLeaveDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					"IRC", msgIRCLeaveDynmap.replace(
						"%USER%", processor.nick));
			    }
			}
		    }
		    iter.remove();
		    if (processor.isConnected()) {
			processor.disconnect();
		    }
		    return true;
		}
	    }
	}
	return false;
    }

    public static boolean removeIRCUsersBySID(String serverID) {
	if (mode != Modes.INSPIRCD) {
	    return false;
	}
	IRCServer is = servers.get(serverID);
	if (is != null) {
	    if (Config.isDebugModeEnabled()) {
		BukkitIRCdPlugin.log.info("[BukkitIRCd] Server " + serverID +
			" (" + is.host + ") delinked");
	    }
	    Iterator<Map.Entry<String, IRCUser>> iter = uid2ircuser.entrySet()
		    .iterator();
	    while (iter.hasNext()) {
		Map.Entry<String, IRCUser> entry = iter.next();
		String curUID = entry.getKey();
		IRCUser curUser = entry.getValue();
		if (curUID.startsWith(serverID)) {
		    if (curUser.joined) {

			if (msgIRCLeaveReason.length() > 0) {
			    Utils.broadcastMessage(msgIRCLeaveReason
				    .replace("{User}", curUser.nick)
				    .replace(
					    "{Prefix}",
					    IRCFunctionality.getGroupPrefix(curUser
						    .getTextModes()))
				    .replace(
					    "{Suffix}",
					    IRCFunctionality.getGroupSuffix(curUser
						    .getTextModes()))
				    .replace("{Reason}", is.host + " split"));
			}
			if ((BukkitIRCdPlugin.dynmap != null) &&
				(msgIRCLeaveReasonDynmap.length() > 0)) {
			    BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
				    "IRC",
				    msgIRCLeaveReasonDynmap.replace("{User}",
					    curUser.nick).replace("{Reason}",
					    is.host + " split"));
			}
		    }
		    iter.remove();
		}
	    }
	    servers.remove(serverID);
	    for (String curSID : is.leaves) {
		removeIRCUsersBySID(curSID);
	    }
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
	if (banType == null) {
	    banType = Config.getIrcdBantype();
	}
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
	if (ircuser == null) {
	    return false;
	}
	synchronized (csIrcUsers) {
	    if (mode == Modes.STANDALONE) {
		ClientConnection processor;
		Iterator<ClientConnection> iter = clientConnections.iterator();
		while (iter.hasNext()) {
		    processor = iter.next();
		    if (processor.nick.equalsIgnoreCase(ircuser.nick)) {
			if (processor.isIdented && processor.isNickSet) {
			    if ((IRCd.isPlugin) &&
				    (BukkitIRCdPlugin.thePlugin != null)) {
				if (reason != null) {
				    if (msgIRCKickReason.length() > 0) {
					Utils.broadcastMessage(msgIRCKickReason
						.replace("{KickedUser}",
							processor.nick)
						.replace("{KickedBy}",
							kickedByNick)
						.replace(
							"{Reason}",
							Utils.convertColors(reason,
								true))
						.replace(
							"{KickedPrefix}",
							IRCFunctionality.getGroupPrefix(processor.modes))
						.replace(
							"{KickedSuffix}",
							IRCFunctionality.getGroupSuffix(processor.modes))
						.replace(
							"{KickerPrefix}",
							IRCFunctionality.getGroupPrefix(getIRCUser(
									kickedByNick)
								.getTextModes()))
						.replace(
							"{KickerSuffix}",
							IRCFunctionality.getGroupSuffix(getIRCUser(
									kickedByNick)
								.getTextModes())));
				    }
				    if ((BukkitIRCdPlugin.dynmap != null) &&
					    (msgIRCKickReasonDynmap.length() > 0)) {
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
								Utils.stripIRCFormatting(reason)));
				    }
				} else {
				    if (msgIRCKick.length() > 0) {
					Utils.broadcastMessage(msgIRCKick
						.replace("{KickedUser}",
							processor.nick)
						.replace("{KickedBy}",
							kickedByNick)
						.replace(
							"{KickedPrefix}",
							IRCFunctionality.getGroupPrefix(processor.modes))
						.replace(
							"{KickedSuffix}",
							IRCFunctionality.getGroupSuffix(processor.modes))
						.replace(
							"{KickerPrefix}",
							IRCFunctionality.getGroupPrefix(getIRCUser(
									kickedByNick)
								.getTextModes()))
						.replace(
							"{KickerSuffix}",
							IRCFunctionality.getGroupSuffix(getIRCUser(
									kickedByNick)
								.getTextModes())));
				    }
				    if ((BukkitIRCdPlugin.dynmap != null) &&
					    (msgIRCKickDynmap.length() > 0)) {
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
			}
			if (isIngame) {
			    kickedByNick += Config.getIrcdIngameSuffix();
			    if (reason != null) {
				reason = Utils.convertColors(reason, false);
			    }
			}
			if (reason != null) {
			    IRCFunctionality.writeAll(":" + processor.getFullHost() +
				    " QUIT :Kicked by " + kickedByNick + ": " +
				    reason);
			    processor.writeln(":" + kickedByNick + "!" +
				    kickedByIdent + "@" + kickedByHost +
				    " KILL " + processor.nick + " :" +
				    kickedByHost + "!" + kickedByNick + " (" +
				    reason + ")");
			    processor.writeln("ERROR :Closing Link: " +
				    processor.nick + "[" + processor.hostmask +
				    "] " + kickedByNick + " (Kicked by " +
				    kickedByNick + " (" + reason + "))");
			} else {
			    IRCFunctionality.writeAll(":" + processor.getFullHost() +
				    " QUIT :Kicked by " + kickedByNick);
			    processor.writeln(":" + kickedByNick + "!" +
				    kickedByIdent + "@" + kickedByHost +
				    " KILL " + processor.nick + " :" +
				    kickedByHost + "!" + kickedByNick);
			    processor.writeln("ERROR :Closing Link: " +
				    processor.nick + "[" + processor.hostmask +
				    "] " + kickedByNick + " (Kicked by " +
				    kickedByNick + ")");
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
		    Map.Entry<String, IRCUser> entry = (Map.Entry<String, IRCUser>) iter
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
		    if ((bukkitUser = BukkitUserManagement.getUserObject(kickedByNick)) != null) {
			sourceUID = bukkitUser.getUID();
		    } else {
			sourceUID = serverUID;
		    }

		    boolean returnVal = false;
		    if (iuser.consoleJoined) {
			if (reason != null) {
			    Utils.println(":" + sourceUID + " KICK " +
				    Config.getIrcdConsoleChannel() + " " +
				    uid + " :" + reason);
			} else {
			    Utils.println(":" + sourceUID + " KICK " +
				    Config.getIrcdConsoleChannel() + " " +
				    uid + " :" + kickedByNick);
			}
			returnVal = true;
			iuser.consoleJoined = false;
		    }
		    if (iuser.joined) {
			if (reason != null) {
			    Utils.println(":" + sourceUID + " KICK " +
				    Config.getIrcdChannel() + " " + uid +
				    " :" + reason);
			    if (msgIRCKickReason.length() > 0) {
				Utils.broadcastMessage(msgIRCKickReason
					.replace("{KickedUser}", iuser.nick)
					.replace("{KickedBy}", kickedByNick)
					.replace("{Reason}",
						Utils.convertColors(reason, true))
					.replace(
						"{KickedPrefix}",
						IRCFunctionality.getGroupPrefix(iuser
							.getTextModes()))
					.replace(
						"{KickedSuffix}",
						IRCFunctionality.getGroupSuffix(iuser
							.getTextModes()))
					.replace(
						"{KickerPrefix}",
						IRCFunctionality.getGroupPrefix(getIRCUser(
								kickedByNick)
							.getTextModes()))
					.replace(
						"{KickerSuffix}",
						IRCFunctionality.getGroupSuffix(getIRCUser(
								kickedByNick)
							.getTextModes())));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCKickReasonDynmap.length() > 0)) {
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
							Utils.stripIRCFormatting(reason)));
			    }
			} else {
			    Utils.println(":" + sourceUID + " KICK " +
				    Config.getIrcdChannel() + " " + uid +
				    " :" + kickedByNick);
			    if (msgIRCKick.length() > 0) {
				Utils.broadcastMessage(msgIRCKick
					.replace("{KickedUser}", iuser.nick)
					.replace("{KickedBy}", kickedByNick)
					.replace(
						"{KickedPrefix}",
						IRCFunctionality.getGroupPrefix(iuser
							.getTextModes()))
					.replace(
						"{KickedSuffix}",
						IRCFunctionality.getGroupSuffix(iuser
							.getTextModes()))
					.replace(
						"{KickerPrefix}",
						IRCFunctionality.getGroupPrefix(getIRCUser(
								kickedByNick)
							.getTextModes()))
					.replace(
						"{KickerSuffix}",
						IRCFunctionality.getGroupSuffix(getIRCUser(
								kickedByNick)
							.getTextModes())));
			    }
			    if ((BukkitIRCdPlugin.dynmap != null) &&
				    (msgIRCKickDynmap.length() > 0)) {
				BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					"IRC",
					msgIRCKickDynmap.replace(
						"{KickedUser}", iuser.nick)
					.replace("{KickedBy}",
						kickedByNick));
			    }
			}
			returnVal = true;
			iuser.joined = false;
		    } else {
			BukkitIRCdPlugin.log.info("Player " + kickedByNick +
				" tried to kick IRC user not on channel: " +
				iuser.nick); // Log this as severe since it
		    }					// should never occur unless
		    // something is wrong with the
		    // code

		    return returnVal;
		} else {
		    BukkitIRCdPlugin.log.severe("[BukkitIRCd] User " +
			    ircuser.nick +
			    " not found in UID list. Error code IRCd942."); // Log
		}				// this
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
	if (ircuser == null) {
	    return false;
	}
	synchronized (csIrcUsers) {
	    // ClientConnection processor;
	    IRCUser[] ircusers = getIRCUsers();
	    for (int i = 0; i < ircusers.length; i++) {
		ircuser = ircusers[i];
		if (ircuser.nick.equalsIgnoreCase(ircuser.nick)) {
		    if (isIngame) {
			bannedByHost = bannedBy + Config.getIrcdIngameSuffix() +
				"!" + bannedBy + "@" + bannedByHost;
			bannedBy += Config.getIrcdIngameSuffix();
		    }
		    String banHost;
		    if ((banType.equals("host")) ||
			    (banType.equals("hostname"))) {
			banHost = "*!*@" + ircuser.hostmask;
		    } else if ((banType.equals("ip")) ||
			    (banType.equals("ipaddress"))) {
			banHost = "*!*@" + ircuser.ipaddress;
		    } else if (banType.equals("ident")) {
			banHost = "*!" + ircuser.ident + "@*";
		    } else {
			banHost = ircuser.nick + "!*@*";
		    }
		    boolean result = banIRCUser(banHost, bannedByHost);
		    if (result) {
			if (ircuser.joined) {
			    if ((IRCd.isPlugin) &&
				    (BukkitIRCdPlugin.thePlugin != null)) {
				if (msgIRCBan.length() > 0) {
				    Utils.broadcastMessage(msgIRCBan.replace(
					    "{BannedUser}", ircuser.nick)
					    .replace("{BannedBy}", bannedBy));
				}
				if ((BukkitIRCdPlugin.dynmap != null) &&
					(msgIRCBanDynmap.length() > 0)) {
				    BukkitIRCdPlugin.dynmap.sendBroadcastToWeb(
					    "IRC",
					    msgIRCBanDynmap.replace(
						    "{BannedUser}",
						    ircuser.nick).replace(
						    "{BannedBy}", bannedBy));
				}
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
	    if (isBanned(banHost)) {
		return false;
	    } else {
		if (mode == Modes.STANDALONE) {
		    ircBans.add(new IrcBan(banHost, bannedByHost, System
			    .currentTimeMillis() / 1000L));
		    IRCFunctionality.writeAll(":" + bannedByHost + " MODE " +
			    Config.getIrcdChannel() + " + b " + banHost);
		    return true;
		} else if (mode == Modes.INSPIRCD) {
		    String user = bannedByHost.split("!")[0];
		    if (user.endsWith(Config.getIrcdIngameSuffix())) {
			user = user.substring(0, user.length() -
				Config.getIrcdIngameSuffix().length());
		    }
		    String UID;
		    BukkitPlayer bp = null;
		    if (((UID = getUIDFromIRCUser(user)) != null) ||
			    ((bp = BukkitUserManagement.getUserObject(user)) != null) ||
			    (user.equals(Config.getIrcdServerName()))) {
			if (user.equals(Config.getIrcdServerName())) {
			    UID = serverUID;
			} else if (UID == null) {
			    UID = bp.getUID();
			}
			Utils.println(":" + UID + " FMODE " + Config.getIrcdChannel() +
				" " + channelTS + " + b :" + banHost);
			return true;
		    } else {
			if (Config.isDebugModeEnabled()) {
			    BukkitIRCdPlugin.log
				    .severe("[BukkitIRCd] User " +
					    user +
					    " not found in UID list. Error code IRCd1004."); // Log
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
		if ((ban = getIRCBan(banHost)) < 0) {
		    return false;
		}
		ircBans.remove(ban);
		IRCFunctionality.writeAll(":" + bannedByHost + " MODE " +
			Config.getIrcdChannel() + " -b " + banHost);
		return true;
	    } else if (mode == Modes.INSPIRCD) {
		String user = bannedByHost.split("!")[0];
		if (user.endsWith(Config.getIrcdIngameSuffix())) {
		    user = user.substring(0, user.length() -
			    Config.getIrcdIngameSuffix().length());
		}
		String UID;
		BukkitPlayer bp = null;
		if (((UID = getUIDFromIRCUser(user)) != null) ||
			((bp = BukkitUserManagement.getUserObject(user)) != null) ||
			(user.equals(Config.getIrcdServerName()))) {
		    if (user.equals(Config.getIrcdServerName())) {
			UID = serverUID;
		    } else if (UID == null) {
			UID = bp.getUID();
		    }
		    Utils.println(":" + UID + " FMODE " + Config.getIrcdChannel() +
			    " " + channelTS + " -b :" + banHost);
		    return true;
		} else {
		    if (Config.isDebugModeEnabled()) {
			BukkitIRCdPlugin.log
				.severe("[BukkitIRCd] User " +
					user +
					" not found in UID list. Error code IRCd1034."); // Log
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
		if (Utils.wildCardMatch(fullHost, ircBan.fullHost)) {
		    return true;
		}
	    }
	}
	return false;
    }

    public static int getIRCBan(String fullHost) {
	synchronized (csIrcBans) {
	    int i = 0;
	    while (i < ircBans.size()) {
		if (ircBans.get(i).fullHost.equalsIgnoreCase(fullHost)) {
		    return i;
		}
		i++;
	    }
	}
	return -1;
    }
}
