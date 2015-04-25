package com.Jdbye.BukkitIRCd.Utilities;
/**

 @author Matt
 */
public class MessageFormatter {
    
    public String chat(String input, String prefix, String user, String suffix, String reason, String message) {
		String output = input;
		output = output.replace("{User}", user);
		output = output.replace("{Prefix}", prefix);
		output = output.replace("{Suffix}", suffix);
		return output;
    }
    
    public static String joinIRC(String input, String prefix, String user, String suffix) {
		String output = input;
		output = output.replace("{Prefix}", prefix);
		output = output.replace("{User}", user);
		output = output.replace("{Suffix}", suffix);
		return output;
    }
    
    public static String changeNick(String input, String oldnick, String newnick, String prefix, String suffix) {
		String output = input;
		output = output.replace("{OldNick}", oldnick);
		output = output.replace("{NewNick}", newnick);
		output = output.replace("{Prefix}", prefix);
		output = output.replace("{Suffix}", suffix);
		return output;
    }
    
    public static String changeNickDynmap(String input, String oldnick, String newnick) {
		String output = input;
		output = output.replace("{OldNick}", oldnick);
		output = output.replace("{NewNick}", newnick);
		return output;
    }
    
    public static String sendMsg(String input, String prefix, String suffix, String user, String message) {
		String output = input;
		output = output.replace("{Prefix}", prefix);
		output = output.replace("{Suffix}", suffix);		
		output = output.replace("{User}", user);
		output = output.replace("{Message}", message);
		return output;
    }
    
    public static String ircleaveMsg(String input, String prefix, String suffix, String user, String reason) {
		String output = input;
		output = output.replace("{Prefix}", prefix);
		output = output.replace("{Suffix}", suffix);		
		output = output.replace("{User}", user);
		output = output.replace("{Reason}", reason);
		return output;
    }

    public static String dynmapMsg(String input, String user, String message) {
		String output = input;		
		output = output.replace("{User}", user);
		output = output.replace("{Message}", message);
		return output;
    }    
    
    public static String ircleaveDynmapMsg(String input, String user, String reason) {
		String output = input;		
		output = output.replace("{User}", user);
		output = output.replace("{Reason}", reason);
		return output;
    }    
    
    public static String banMsg(String input, String banneduser, String bannedby) {
		String output = input;
		output = output.replace("{BannedUser}", banneduser);
		output = output.replace("{BannedBy}", bannedby);		
		return output;
    }
    
    public static String kickMsg(String input, String kickedby, String kickeduser) {
		String output = input;
		output = output.replace("{KickedBy}", kickedby);
		output = output.replace("{KickedUser}", kickeduser);	
		return output;
    }
   
    public static String sendMsgList(String input, String count, String users) {
		String output = input;
		output = output.replace("{Count}", count);
		output = output.replace("{Users}", users);	
		return output;
    }    
    
    public static String kickMsgReason(String input, String kickedby, String kickeduser, String kickreason) {
		String output = input;
		output = output.replace("{KickedBy}", kickedby);
		output = output.replace("{KickedUser}", kickeduser);	
		output = output.replace("{Reason}", kickreason);
		return output;
    }
    
    public static String kickMsgDisplay(String input, String kickedby, String kickreason) {
		String output = input;
		output = output.replace("{KickedBy}", kickedby);	
		output = output.replace("{Reason}", kickreason);
		return output;
    }

    public static String delinkedMsg(String input, String linkname, String reason) {
		String output = input;
		output = output.replace("{LinkNamey}", linkname);	
		output = output.replace("{Reason}", reason);
		return output;
    }    
  
    public static String irckickMsgReason(String input, String kickedby, String kickeduser, String kickreason, String kickedprefix, String kickedsuffix, String kickerprefix, String kickersuffix) {
		String output = input;
		output = output.replace("{KickedBy}", kickedby);
		output = output.replace("{KickedUser}", kickeduser);	
		output = output.replace("{Reason}", kickreason);
		output = output.replace("{KickedPrefix}", kickedprefix);
		output = output.replace("{KickedSuffix}", kickedsuffix);	
		output = output.replace("{KickerPrefix}", kickerprefix);
		output = output.replace("{KickerSuffix}", kickersuffix);			
		return output;
    }  
    
    public static String irckickMsg(String input, String kickedby, String kickeduser, String kickedprefix, String kickedsuffix, String kickerprefix, String kickersuffix) {
		String output = input;
		output = output.replace("{KickedBy}", kickedby);
		output = output.replace("{KickedUser}", kickeduser);
		output = output.replace("{KickedPrefix}", kickedprefix);
		output = output.replace("{KickedSuffix}", kickedsuffix);	
		output = output.replace("{KickerPrefix}", kickerprefix);
		output = output.replace("{KickerSuffix}", kickersuffix);			
		return output;
    }    
}
