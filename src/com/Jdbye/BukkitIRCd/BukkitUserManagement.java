package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.bukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.channelTS;
import static com.Jdbye.BukkitIRCd.IRCd.csBukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.mode;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKick;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKickDisplay;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKickDisplayReason;
import static com.Jdbye.BukkitIRCd.IRCd.msgIRCKickReason;
import static com.Jdbye.BukkitIRCd.IRCd.pre;
import static com.Jdbye.BukkitIRCd.IRCd.serverUID;
import static com.Jdbye.BukkitIRCd.IRCd.ugen;

import com.Jdbye.BukkitIRCd.Configuration.Config;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 Performs most of the heavy lifting in managing a BukkitPlayer. Mostly pulled from IRCd.java to help clean it up. Very rough-cut, needs work.
 */
public class BukkitUserManagement {

    /**

     @param nick The current player's IRC nickname

     @return Gets the numbered ID of the user
     */
    public static int getUser(String nick) {
	synchronized (csBukkitPlayers) {
	    int i = 0;
	    String curnick;
	    while (i < bukkitPlayers.size()) {
		curnick = bukkitPlayers.get(i).nick;
		if ((curnick.equalsIgnoreCase(nick)) || ((curnick + Config.getIrcdIngameSuffix()).equalsIgnoreCase(nick))) {
		    return i;
		} else {
		    i++;
		}
	    }
	    return -1;
	}
    }

    /**
     Used for Console Kicks
     <p>
     @param kickReason
     @param kickedID
     <p>
     @return
     */
    public static boolean kickUser(String kickReason, int kickedID) {
	if (kickedID >= 0) {
	    synchronized (csBukkitPlayers) {
		BukkitPlayer kickedBukkitPlayer = bukkitPlayers.get(kickedID);
		if (!kickReason.isEmpty()) {
		    kickReason = " :" + ChatUtils.convertColors(kickReason, false);
		}
		if (mode == Modes.STANDALONE) {
		    IRCFunctionality.writeAll(":" + Config.getIrcdServerName() + "!" +
			    Config.getIrcdServerName() + "@" +
			    Config.getIrcdServerHostName() + " KICK " +
			    Config.getIrcdChannel() + " " +
			    kickedBukkitPlayer.nick +
			    Config.getIrcdIngameSuffix() + kickReason);
		} else {

		    // KICK
		    ChatUtils.println(":" + serverUID + " KICK " +
			    Config.getIrcdChannel() + " " +
			    kickedBukkitPlayer.nick +
			    Config.getIrcdIngameSuffix() + kickReason);
		}
		return true;
	    }
	} else {
	    return false;
	}
    }

    /**
     Used for player kicks
     <p>
     @param kickReason
     @param kickedID
     @param kickerID
     <p>
     @return
     */
    public static boolean kickBukkitUser(String kickReason, int kickedID, int kickerID) {
	if (kickedID >= 0) {
	    synchronized (csBukkitPlayers) {
		BukkitPlayer kickedBukkitPlayer = bukkitPlayers.get(kickedID);

		BukkitPlayer kickerBukkitPlayer = bukkitPlayers.get(kickerID);
		String kickerHost = kickedBukkitPlayer.host;
		String kickerName = kickerBukkitPlayer.nick;

		if (!kickReason.isEmpty()) {
		    kickReason = " :" + ChatUtils.convertColors(kickReason, false);
		}
		if (mode == Modes.STANDALONE) {
		    IRCFunctionality.writeAll(":" + kickerName + Config.getIrcdIngameSuffix() +
			    "!" + kickerName + "@" + kickerHost + " KICK " +
			    Config.getIrcdChannel() + " " +
			    kickedBukkitPlayer.nick +
			    Config.getIrcdIngameSuffix() +
			    ChatUtils.convertColors(kickReason, false));
		} else {

		    // KICK
		    ChatUtils.println(":" + kickerBukkitPlayer.getUID() + " KICK " +
			    Config.getIrcdChannel() + " " +
			    kickedBukkitPlayer.nick +
			    Config.getIrcdIngameSuffix() +
			    ChatUtils.convertColors(kickReason, false));
		}
		return true;
	    }
	} else {
	    return false;
	}
    }

    /**
    Kicks player synchronously
    @param kicker The IRC user who kicked the player
    @param kickee The player to get kicked
    @param kickReason The kick reason
    @return Whether kicking the player was successful
    */
    public static boolean kickPlayerIngame(final String kicker, final String kickee, final String kickReason) {
	int IRCUser = BukkitUserManagement.getUser(kickee);
	kickUser(kickReason, IRCUser);
	BukkitUserManagement.removeBukkitUser(IRCUser);
    // TODO Replace .getPlayer as it seems to be Deprecated. 
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
			    kickText = msgIRCKickDisplay.replace("{KickedBy}",
				    kicker).replace("{Reason}", kickReason);

			} else {
			    kickText = msgIRCKickDisplayReason.replace(
				    "{KickedBy}", kicker).replace("{Reason}",
					    kickReason);
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
    Provides the BukkitPlayer object based on the nickname provided
    @param nick Name of the person to find
    @return BukkitPlayer of the nick found
    */
    public static BukkitPlayer getUserObject(String nick) {
	synchronized (csBukkitPlayers) {
	    int i = 0;
	    String curnick;
	    while (i < bukkitPlayers.size()) {
		BukkitPlayer bp = bukkitPlayers.get(i);
		curnick = bp.nick;
		if ((curnick.equalsIgnoreCase(nick)) || ((curnick + Config.getIrcdIngameSuffix()).equalsIgnoreCase(nick))) {
		    return bp;
		}
		i++;
	    }
	    return null;
	}
    }

    /**
    Returns the BukkitPlayer based on the UID provided
    @param UID
    @return 
    */
    public static BukkitPlayer getUserByUID(String UID) {
	synchronized (csBukkitPlayers) {
	    int i = 0;
	    BukkitPlayer bp;
	    while (i < bukkitPlayers.size()) {
		bp = bukkitPlayers.get(i);
		if (bp.getUID().equalsIgnoreCase(UID)) {
		    return bp;
		} else {
		    i++;
		}
	    }
	    return null;
	}
    }

    /**
    Updates the player's idle time and world, for WHOIS.
    @param ID The ID of the IRC user
    @param world The player's current world
    @return Whether updating the time and world was a success
    */
    public static boolean updateUserIdleTimeAndWorld(int ID, String world) {
	if (ID >= 0) {
	    synchronized (csBukkitPlayers) {
		BukkitPlayer bp = bukkitPlayers.get(ID);
		bp.idleTime = System.currentTimeMillis();
		if (!bp.world.equals(world)) {
		    bp.world = world;
		    if (mode == Modes.INSPIRCD) {
			ChatUtils.println(pre + "METADATA " + bp.getUID() +	" swhois :is currently in " + world);
		    }
		}
		return true;
	    }
	} else {
	    return false;
	}
    }

    /**
    Updates the player's idle time, for WHOIS.
    @param ID The ID of the IRC user
    @return Whether updating the time was a success
    */
    public static boolean updateUserIdleTime(int ID) {
	if (ID >= 0) {
	    synchronized (csBukkitPlayers) {
		BukkitPlayer bp = bukkitPlayers.get(ID);
		bp.idleTime = System.currentTimeMillis();
		return true;
	    }
	} else {
	    return false;
	}
    }

    /**
    Creates a has on the player, using the mask key in the configuration.
    @param itemId
    @param item
    @param itemLen
    @param outLen
    @return The created hash
    @throws NoSuchAlgorithmException 
    */
    private static String hashPart(byte itemId, byte[] item, int itemLen, int outLen) throws NoSuchAlgorithmException {
	final MessageDigest md = MessageDigest.getInstance("MD5");

	md.update(itemId);
	md.update(Config.getHostMaskKey().getBytes());
	md.update((byte) 0);
	md.update(item, 0, itemLen);

	final byte[] d = md.digest();

	final String alphabet = "0123456789abcdefghijklmnopqrstuv";

	StringBuilder output = new StringBuilder();

	for (int i = 0; i < outLen; i++) {
	    output.append(alphabet.charAt((d[i] + 256) % 32));
	}

	return output.toString();
    }

    /**
    Masks the IP of the player into something less identifiable
    @param ip The player's current IP address
    @return The created masked host
    */
    public static String maskHost(InetAddress ip) {
	if (Config.isUseHostMask()) {
	    final byte[] bytes = ip.getAddress();
	    try {
		String maskPrefix = Config.getHostMaskPrefix();
		String maskSuffix = Config.getHostMaskSuffix();
		return maskPrefix + hashPart((byte) 10, bytes, 4, 3) + "." +
			hashPart((byte) 11, bytes, 3, 3) + "." +
			hashPart((byte) 13, bytes, 2, 6) + maskSuffix;
	    } catch (NoSuchAlgorithmException e) {
		return ip.getHostName();
	    }
	} else {
	    return ip.getHostName();
	}
    }

    /**
    Adds the Bukkit user, with modes, to IRC
    @param modes The IRC modes that the player should have
    @param player The player in question to be added
    @return Whether adding the player was successful
    */
    public static boolean addBukkitUser(String modes, Player player) {
	StringBuilder maskedrealhost = new StringBuilder();
	String nick = player.getName();
	String host = maskHost(player.getAddress().getAddress());

	// TODO This right here is the primary cause of login lag
	//String realhost = player.getAddress().getAddress().getHostName();
	// TODO This is a temporary solution
	maskedrealhost.append("Masked-to-avoid-lag-" + host);
	String realhost = maskedrealhost.toString();

	String ip = player.getAddress().getAddress().getHostAddress();
	String world = player.getWorld().getName();
	if (getUser(nick) < 0) {
	    synchronized (csBukkitPlayers) {
		BukkitPlayer bp = new BukkitPlayer(nick, world, modes, realhost, host, ip, System.currentTimeMillis() / 1000L,	System.currentTimeMillis());
		bukkitPlayers.add(bp);
		if (mode == Modes.STANDALONE) {
		    IRCFunctionality.writeAll(":" + nick + Config.getIrcdIngameSuffix() + "!" + nick + "@" + host + " JOIN " + Config.getIrcdChannel());
		}
		StringBuilder mode1 = new StringBuilder();
		mode1.append("+");
		if (modes.contains("~")) {
		    mode1.append("q");
		}
		if (modes.contains("&")) {
		    mode1.append("a");
		}
		if (modes.contains("@")) {
		    mode1.append("o");
		}
		if (modes.contains("%")) {
		    mode1.append("h");
		}
		if (modes.contains("+")) {
		    mode1.append("v");
		}
		String mode2 = nick + Config.getIrcdIngameSuffix() + " ";
		if (!mode1.equals("+")) {
		    if (mode == Modes.STANDALONE) {
			IRCFunctionality.writeAll(":" + Config.getIrcdServerName() + "!" +
				Config.getIrcdServerName() + "@" +
				Config.getIrcdServerHostName() + " MODE " +
				Config.getIrcdChannel() + " " + mode1 + " " +
				mode2.substring(0, mode2.length() - 1));
		    }
		}

		if (mode == Modes.INSPIRCD) {
		    String UID = ugen.generateUID(Config.getLinkServerID());
		    bp.setUID(UID);
		    synchronized (csBukkitPlayers) {
			final boolean isOper = bp.hasPermission("bukkitircd.oper");

			// Register new UID
			final String userModes = isOper ? "+or" : "+r";
			ChatUtils.println(pre + "UID", UID,
				Long.toString(bp.idleTime / 1000L), bp.nick +
				Config.getIrcdIngameSuffix(),
				bp.realhost, bp.host,
				bp.nick, // user
				bp.ip, Long.toString(bp.signedOn), userModes, IRCd.userModeMsg);

			// Set oper type if appropriate
			if (isOper) {
			    ChatUtils.println(":" + UID, "OPERTYPE", "IRC_Operator");
			}

			// Game client uses encrypted connection
			ChatUtils.println(pre + "METADATA", UID, "ssl_cert",
				":vtrsE The peer did not send any certificate.");

			// Join in-game channel with modes set
			ChatUtils.println(pre + "FJOIN", Config.getIrcdChannel(),
				Long.toString(channelTS), "+nt",
				":" + bp.getTextMode() + "," + UID);

			// Send swhois field (extra metadata used for current world here)
			final String worldString = world == null ? "an unknown world" :
				world;
			ChatUtils.println(pre + "METADATA ", UID, "swhois", ":is currently in " + worldString);
		    }
		}
		return true;
	    }
	} else {
	    return false;
	}
    }

    /**
    Removes the user from IRC and the bukkitPlayers list, based on the ID
    @param ID ID of the user
    @return Whether removing the user was successful
    // TODO Make the quit message dependant on how/why they disconnect
    */
    public static boolean removeBukkitUser(int ID) {
	synchronized (csBukkitPlayers) {
	    if (ID >= 0) {
		BukkitPlayer bp = bukkitPlayers.get(ID);
		if (mode == Modes.STANDALONE) {
		    IRCFunctionality.writeAll(":" + bp.nick + Config.getIrcdIngameSuffix() + "!" +
			    bp.nick + "@" + bp.host + " QUIT :" + IRCd.userDisconnectMsg);
		} else if (mode == Modes.INSPIRCD) {
		    ChatUtils.println(":" + bp.getUID() + " QUIT :" + IRCd.userDisconnectMsg);
		}
		bukkitPlayers.remove(ID);
		return true;
	    } else {
		return false;
	    }
	}
    }

    /**
    Removes the user from IRC and the bukkitPlayers list, based on the UID
    @param UID The UID of the IRC user
    @return Whether removing the user was successful
    */
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
}
