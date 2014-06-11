package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.bukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.channelTS;
import static com.Jdbye.BukkitIRCd.IRCd.csBukkitPlayers;
import static com.Jdbye.BukkitIRCd.IRCd.mode;
import static com.Jdbye.BukkitIRCd.IRCd.msgDisconnectQuitting;
import static com.Jdbye.BukkitIRCd.IRCd.pre;
import static com.Jdbye.BukkitIRCd.IRCd.println;
import static com.Jdbye.BukkitIRCd.IRCd.ugen;
import static com.Jdbye.BukkitIRCd.IRCd.writeAll;
import com.Jdbye.BukkitIRCd.configuration.Config;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import org.bukkit.entity.Player;

public class BukkitUserManagement {

    public static int getUser(String nick) {
        synchronized (csBukkitPlayers) {
            int i = 0;
            String curnick;
            while (i < bukkitPlayers.size()) {
                curnick = bukkitPlayers.get(i).nick;
                if ((curnick.equalsIgnoreCase(nick)) ||
                         ((curnick + Config.getIrcdIngameSuffix())
                        .equalsIgnoreCase(nick))) {
                    return i;
                } else {
                    i++;
                }
            }
            return -1;
        }
    }

    public static BukkitPlayer getUserObject(String nick) {
        synchronized (csBukkitPlayers) {
            int i = 0;
            String curnick;
            while (i < bukkitPlayers.size()) {
                BukkitPlayer bp = bukkitPlayers.get(i);
                curnick = bp.nick;
                if ((curnick.equalsIgnoreCase(nick)) ||
                         ((curnick + Config.getIrcdIngameSuffix())
                        .equalsIgnoreCase(nick))) {
                    return bp;
                }
                i++;
            }
            return null;
        }
    }

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

    public static boolean updateUserIdleTimeAndWorld(int ID, String world) {
        if (ID >= 0) {
            synchronized (csBukkitPlayers) {
                BukkitPlayer bp = bukkitPlayers.get(ID);
                bp.idleTime = System.currentTimeMillis();
                if (!bp.world.equals(world)) {
                    bp.world = world;
                    if (mode == Modes.INSPIRCD) {
                        println(pre + "METADATA " + bp.getUID() +
                                 " swhois :is currently in " + world);
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

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

    private static String hashPart(byte itemId, byte[] item, int itemLen,
            int outLen) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("MD5");

        md.update(itemId);
        md.update(Config.getHostMaskKey().getBytes());
        md.update((byte) 0);
        md.update(item, 0, itemLen);

        final byte[] d = md.digest();

        final String alphabet = "0123456789abcdefghijklmnopqrstuv";

	// BIG NONO (as stated by @tjetson)
		/*
         String output = "";
         for (int i = 0; i < outLen; i++) {
         output = output + alphabet.charAt((d[i] + 256) % 32);
         }*/
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < outLen; i++) {
            output.append(alphabet.charAt((d[i] + 256) % 32));
        }

        return output.toString();
    }

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

    public static boolean addBukkitUser(String modes, Player player) {
        String nick = player.getName();
        String host = maskHost(player.getAddress().getAddress());
        String realhost = player.getAddress().getAddress().getHostName();
        String ip = player.getAddress().getAddress().getHostAddress();
        String world = player.getWorld().getName();
        if (getUser(nick) < 0) {
            synchronized (csBukkitPlayers) {
                BukkitPlayer bp = new BukkitPlayer(nick, world, modes,
                        realhost, host, ip, System.currentTimeMillis() / 1000L,
                        System.currentTimeMillis());
                bukkitPlayers.add(bp);
                if (mode == Modes.STANDALONE) {
                    writeAll(":" + nick + Config.getIrcdIngameSuffix() + "!" +
                             nick + "@" + host + " JOIN " +
                             Config.getIrcdChannel());
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
                        writeAll(":" + Config.getIrcdServerName() + "!" +
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

                        final boolean isOper = bp
                                .hasPermission("bukkitircd.oper");

                        // Register new UID
                        final String userModes = isOper ? "+or" : "+r";
                        println(pre + "UID", UID,
                                Long.toString(bp.idleTime / 1000L), bp.nick +
                                 Config.getIrcdIngameSuffix(),
                                bp.realhost, bp.host,
                                bp.nick, // user
                                bp.ip, Long.toString(bp.signedOn), userModes,
                                ":Minecraft Player");

                        // Set oper type if appropriate
                        if (isOper) {
                            println(":" + UID, "OPERTYPE", "IRC_Operator");
                        }

                        // Game client uses encrypted connection
                        println(pre + "METADATA", UID, "ssl_cert",
                                ":vtrsE The peer did not send any certificate.");

                        // Join in-game channel with modes set
                        println(pre + "FJOIN", Config.getIrcdChannel(),
                                Long.toString(channelTS), "+nt",
                                ":" + bp.getTextMode() + "," + UID);

			// Send swhois field (extra metadata used for current
                        // world here)
                        final String worldString = world == null ? "an unknown world" :
                                 world;
                        println(pre + "METADATA ", UID, "swhois",
                                ":is currently in " + worldString);
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    // Run when a player disconnects (maybe make the quit message configurable
    public static boolean removeBukkitUser(int ID) {
        synchronized (csBukkitPlayers) {
            if (ID >= 0) {
                BukkitPlayer bp = bukkitPlayers.get(ID);
                if (mode == Modes.STANDALONE) {
                    writeAll(":" + bp.nick + Config.getIrcdIngameSuffix() + "!" +
                             bp.nick + "@" + bp.host + " QUIT :" +
                             msgDisconnectQuitting);
                } else if (mode == Modes.INSPIRCD) {
                    println(":" + bp.getUID() + " QUIT :" +
                             msgDisconnectQuitting);
                }
                bukkitPlayers.remove(ID);
                return true;
            } else {
                return false;
            }
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
}
