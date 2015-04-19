package com.Jdbye.BukkitIRCd.Utilities;

/**

 @author Matt
 */
public class MessageFormatter {
    
    public String chat(String input, String prefix, String user, String suffix, String reason, String message) {
	String output = input;
	output = input.replace("{User}", user);
	output = input.replace("{Prefix}", prefix);
	output = input.replace("{Suffix}", suffix);
	return output;
    }
    
    // JOIN msgIRCJoin.replace("{User}", ircuser.nick).replace("{Prefix}",IRCFunctionality.getGroupPrefix(ircuser.getTextModes())).replace("{Suffix}",IRCFunctionality.getGroupSuffix(ircuser.getTextModes()))
    
    public static String joinIRC(String input, String prefix, String user, String suffix) {
	String output = input;
	output = input.replace("{Prefix}", prefix);
	output = input.replace("{User}", user);
	output = input.replace("{Suffix}", suffix);
	return output;
    }
    
    public static String changeNick(String input, String oldnick, String newnick, String prefix, String suffix) {
	String output = input;
	output = input.replace("{OldNick}", oldnick);
	output = input.replace("{NewNick}", newnick);
	output = input.replace("{Prefix}", prefix);
	output = input.replace("{Suffix}", suffix);
	return output;
    }
}
