package com.Jdbye.BukkitIRCd;

import static com.Jdbye.BukkitIRCd.IRCd.csServer;
import static com.Jdbye.BukkitIRCd.IRCd.out;
import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.ChatColor;

public class Utils {
    
    /**
     Strips IRC Formatting

     @param input

     @return output
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
     Converts colors from Minecraft to IRC, or IRC to Minecraft if specified

     @param input
     @param fromIRCtoGame
     Convert IRC colors to Minecraft colors?

     @return
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
		return Utils.stripIRCFormatting(input);
	    }
	    output = input.replaceAll("(\\d),\\d{1,2}", "$1"); // Remove IRC background color code

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
    public static boolean println(String... parts) {
	final String line = IRCd.join(parts, " ", 0);
	if ((IRCd.server == null) || (!IRCd.server.isConnected()) || (IRCd.server.isClosed()) ||
		(out == null)) {
	    return false;
	}
	synchronized (csServer) {
	    if (Config.isDebugModeEnabled()) {
		System.out.println("[BukkitIRCd]" + ChatColor.DARK_BLUE +
			"[<-] " + line);
	    }
	    out.println(line);
	    return true;
	}
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
}