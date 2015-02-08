package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.csServer;
import static com.Jdbye.BukkitIRCd.IRCd.out;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 A general collection of utilities used within the plugin, sometimes for transferring data between IRC and game.
 */
public class Utils {
	static char IRC_Color = (char) 3; // ETX Control Code (^C)
	static char IRC_Bold = (char) 2; // STX Control Code (^B)
	static char IRC_Ital = (char) 29; // GS Control Code
	static char IRC_Under = (char) 31; // US Control Code (^_)
	static char IRC_Reset = (char) 15; // SI Control Code (^O)
	static char MC_Color = (char) 167; // Section Sign

    /**
     Strips IRC formatting codes.
     <p>
     @param input IRC Message being send to game
     <p>
     @return Stripped down message
     */
    public static String stripIRCFormatting(String input) {

	String output = input.replaceAll("\u0003[0-9]{1,2}(,[0-9]{1,2})?", ""); // Remove IRC background color code
	output = output.replace(IRC_Reset + "", "");
	output = output.replace(IRC_Ital + "", "");
	output = output.replace(IRC_Bold + "", "");
	output = output.replace(IRC_Under + "", "");
	output = output.replace(IRC_Color + "", "");
	return output;
    }

    /**
     Converts colors in a string from Minecraft to IRC, or IRC to Minecraft if specified.
     <p>
     For Minecraft -> IRC, &colour codes are converted to the IRC format.
     <p>
     For IRC -> Minecraft, IRC codes are converted to Minecraft codes with the section sign.
     <p>
     @param input A string, usually a message, containing formatted text.
     @param fromIRCtoGame If true, the string was generated in IRC and needs to be formatted for Minecraft. If false, it was generated in Minecraft and needs to be formatted for IRC.
     <p>
     @return Converted message
     */
    public static String convertColors(String input, boolean fromIRCtoGame) {

	String output = null;
	if (fromIRCtoGame) {
	    if (!Config.isIrcdConvertColorCodes()) {
		return Utils.stripIRCFormatting(input);
	    }
	    output = input.replaceAll("(\\d),\\d{1,2}", "$1"); // Remove IRC background color code

	    output = output.replace(IRC_Reset + "", MC_Color + "r");
	    output = output.replace(IRC_Ital + "", MC_Color + "o");
	    output = output.replace(IRC_Bold + "", MC_Color + "l");
	    output = output.replace(IRC_Under + "", MC_Color + "n");

	    output = output.replace(IRC_Color + "01", MC_Color + "0"); // IRC Black to MC Black
	    output = output.replace(IRC_Color + "02", MC_Color + "1");// IRC Dark Blue to MC Dark Blue
	    output = output.replace(IRC_Color + "03", MC_Color + "2"); // IRC Dark Green to MC Dark Green
	    output = output.replace(IRC_Color + "04", MC_Color + "c"); // IRC Red to MC Red
	    output = output.replace(IRC_Color + "05", MC_Color + "4"); // IRC Dark Red to MC Dark Red
	    output = output.replace(IRC_Color + "06", MC_Color + "5"); // IRC Purple to MC Purple
	    output = output.replace(IRC_Color + "07", MC_Color + "6"); // IRC Dark Yellow to MC Gold
	    output = output.replace(IRC_Color + "08", MC_Color + "e"); // IRC Yellow to MC Yellow
	    output = output.replace(IRC_Color + "09", MC_Color + "a"); // IRC Light Green to MC Green
	    output = output.replace(IRC_Color + "10", MC_Color + "3"); // IRC Teal to MC Dark Aqua
	    output = output.replace(IRC_Color + "11", MC_Color + "b"); // IRC Cyan to MC Aqua
	    output = output.replace(IRC_Color + "12", MC_Color + "9"); // IRC Light Blue to MC Blue
	    output = output.replace(IRC_Color + "13", MC_Color + "d"); // IRC Light Purple to MC Pink
	    output = output.replace(IRC_Color + "14", MC_Color + "8"); // IRC Grey to MC Dark Grey
	    output = output.replace(IRC_Color + "15", MC_Color + "7"); // IRC Light Grey to MC Grey
	    output = output.replace(IRC_Color + "00", MC_Color + "f"); // IRC White to MC White

	    output = output.replace(IRC_Color + "1", MC_Color + "0"); // IRC Black to MC Black
	    output = output.replace(IRC_Color + "2", MC_Color + "1");// IRC Dark Blue to MC Dark Blue
	    output = output.replace(IRC_Color + "3", MC_Color + "2"); // IRC Dark Green to MC Dark Green
	    output = output.replace(IRC_Color + "4", MC_Color + "c"); // IRC Red to MC Red
	    output = output.replace(IRC_Color + "5", MC_Color + "4"); // IRC Dark Red to MC Dark Red
	    output = output.replace(IRC_Color + "6", MC_Color + "5"); // IRC Purple to MC Purple
	    output = output.replace(IRC_Color + "7", MC_Color + "6"); // IRC Dark Yellow to MC Gold
	    output = output.replace(IRC_Color + "8", MC_Color + "e"); // IRC Yellow to MC Yellow
	    output = output.replace(IRC_Color + "9", MC_Color + "a"); // IRC Light Green to MC Green
	    output = output.replace(IRC_Color + "0", MC_Color + "f"); // IRC White to MC White

	    output = output.replace(IRC_Color + "", ""); // Get rid of any remaining ETX Characters
	    output = output.replace(IRC_Ital + "", ""); // Get rid of any remaining GS Characters
	    output = output.replace(IRC_Bold + "", ""); // Get rid of any remaining STX Characters
	    output = output.replace(IRC_Under + "", ""); // Get rid of any remaining US Characters

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
	    output = output.replace(MC_Color + "m", ""); // IRC Does not have support for Strikethrough
	    output = output.replace(MC_Color + "k", ""); // IRC Does not have support for Garbled Text
	    output = output.replace(MC_Color + "0", IRC_Color + "01"); // Minecraft Black to IRC Black
	    output = output.replace(MC_Color + "1", IRC_Color + "02"); // Minecraft Dark Blue to IRC Dark Blue
	    output = output.replace(MC_Color + "2", IRC_Color + "03"); // Minecraft Dark Green to IRC Dark Green
	    output = output.replace(MC_Color + "3", IRC_Color + "10"); // Minecraft Dark Aqua to IRC Teal
	    output = output.replace(MC_Color + "4", IRC_Color + "05"); // Minecraft Dark Red to IRC Dark Red
	    output = output.replace(MC_Color + "5", IRC_Color + "06"); // Minecraft Purple to IRC Purple
	    output = output.replace(MC_Color + "6", IRC_Color + "07"); // Minecraft Gold to IRC Dark Yellow
	    output = output.replace(MC_Color + "7", IRC_Color + "15"); // Minecraft Grey to IRC Light Grey
	    output = output.replace(MC_Color + "8", IRC_Color + "14"); // Minecraft Dark Grey to IRC Grey
	    output = output.replace(MC_Color + "9", IRC_Color + "12"); // Minecraft Blue to IRC Light Blue
	    output = output.replace(MC_Color + "a", IRC_Color + "09"); // Minecraft Green to IRC Light Green
	    output = output.replace(MC_Color + "b", IRC_Color + "11"); // Minecraft Aqua to IRC Cyan
	    output = output.replace(MC_Color + "c", IRC_Color + "04"); // Minecraft Red to IRC Red
	    output = output.replace(MC_Color + "d", IRC_Color + "13"); // Minecraft Light Purple to IRC Pink
	    output = output.replace(MC_Color + "e", IRC_Color + "08"); // Minecraft Yellow to IRC Yellow
	    output = output.replace(MC_Color + "f", IRC_Color + "00"); // Minecraft White to IRC White

	}

	return output;
    }

    /**
     Connects 'parts' of a String and prints them.
     <p>
     @param parts Incoming message
     <p>
     @return Whether the message was successfully sent.
     */
    public static boolean println(String... parts) {
	final String line = Utils.join(parts, " ", 0);
	if ((IRCd.server == null) || (!IRCd.server.isConnected()) || (IRCd.server.isClosed()) || (out == null)) {
	    return false;
	}
	synchronized (csServer) {
	    if (Config.isDebugModeEnabled()) {
		System.out.println("[BukkitIRCd]" + ChatColor.DARK_BLUE + "[<-] " + line);
	    }
	    out.println(line);
	    return true;
	}
    }

    /**
     <p>
     @param text Text to be checked
     @param pattern	Regex pattern for matching
     <p>
     @return	Whether a match was found
     */
    public static boolean wildCardMatch(String text, String pattern) {
	//add sentinel so don't need to worry about *'s at end of pattern
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
		if (old[j] && (p == '*')) {
		    old[j + 1] = true;
		}

		if (old[j] && (p == c)) {
		    states[j + 1] = true;
		}
		if (old[j] && (p == '?')) {
		    states[j + 1] = true;
		}
		if (old[j] && (p == '*')) {
		    states[j] = true;
		}
		if (old[j] && (p == '*')) {
		    states[j + 1] = true;
		}
	    }
	    old = states;
	}
	return states[N];
    }

    // TODO Find out what this is supposed to be used for, as literally nothing uses it
    /**
     UNKNOWN
     <p>
     @param line
     <p>
     @return
     */
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
	if (sp1.length == 2) {
	    res[res.length - 1] = sp1[1];
	}
	return res;
    }

    /**
     Executes a Minecraft command asynchronously for the IRC staff channel.
     <p>
     @param command	The command that is to be run.
     */
    public static void executeCommand(final String command) {
	new BukkitRunnable() {

	    @Override
	    public void run() {
		final Server server = Bukkit.getServer();
		try {
		    final ServerCommandEvent commandEvent = new ServerCommandEvent(IRCd.commandSender, command);
		    server.getPluginManager().callEvent(commandEvent);
		    server.dispatchCommand(commandEvent.getSender(), commandEvent.getCommand());
		    IRCd.commandSender.sendMessage(ChatColor.ITALIC + "" + ChatColor.GRAY + "[CONSOLE: Command executed.]");
		} catch (CommandException c) {
		    Throwable e = c.getCause();

		    IRCd.commandSender.sendMessage(ChatColor.RED + "Exception in command \"" + command + "\": " + e);
		    if (Config.isDebugModeEnabled()) {
			for (final StackTraceElement s : e.getStackTrace()) {
			    IRCd.commandSender.sendMessage(s.toString());
			}
		    }
		} catch (Exception e) {
		    IRCd.commandSender.sendMessage(ChatColor.RED + "&cException in command \"" + command + "\": " + e);

		    if (Config.isDebugModeEnabled()) {
			for (final StackTraceElement s : e.getStackTrace()) {
			    IRCd.commandSender.sendMessage(s.toString());
			}
		    }
		}
	    }
	}.runTask(BukkitIRCdPlugin.thePlugin);
    }

    /**
     UNKNOWN
     <p>
     @param strArray
     @param delimiter
     @param start
     <p>
     @return
     */
    public static String join(String[] strArray, String delimiter, int start) {

	if (strArray.length <= start) {
	    return "";
	}
	//Compute buffer length
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

    /**
     Broadcasts a message to the game server asynchronously
     <p>
     @param msg	The message to be broadcast
     <p>
     @return true if able to schedule broadcast
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
     Send a message to a player
     <p>
     @param player The recipient of the message.
     @param msg	The message to be sent
     <p>
     @return Whether the message was successfully sent
     */
    public static boolean sendMessage(final String player, final String msg) {
        // TODO Replace .getPlayer as it seems to be Deprecated. 
	try {
	    new BukkitRunnable() {
		@Override
		public void run() {
		    final Player p = Bukkit.getServer().getPlayer(player);
		    if (p != null) {
			p.sendMessage(msg);
		    }
		}
	    }.runTask(BukkitIRCdPlugin.thePlugin);
	    return true;
	} catch (Exception e) {
	    return false;
	}
    }

}
