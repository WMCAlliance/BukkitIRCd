package com.Jdbye.BukkitIRCd;

import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import com.Jdbye.BukkitIRCd.Configuration.Config;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.util.ArrayList;

public class IRCCommandSender implements CommandSender {

    private final Server server;
    private final PermissibleBase perm = new PermissibleBase(this);
    private boolean enabled = false;
    private String nick;

    public IRCCommandSender(Server server) {
	super();
	this.server = server;
	this.nick = Config.getIrcdServerName();
	enabled = true;
    }

    /**
    Sends a message from the Minecraft console to the Staff IRC channel
    @param message The message to be sent
    */
    @Override
    public void sendMessage(String message) {
	if (enabled) {
	    for (String filter : IRCd.consoleFilters) {
		try {
		    if (message.matches(filter.replace('&', '\u00A7'))) { // Replace Ampersands with section signs																		
			return;
		    }
		} catch (PatternSyntaxException e) {
		    BukkitIRCdPlugin.thePlugin.getLogger().warning("Invalid Regex Found at console-filters");
		    continue;
		}
	    }

	    // TODO Ignore List (this is the staff channel chat, so may be the wrong place for this to go)
			/*for (String nameToIngnore : IRCd.globalNameIgnoreList ) {
	     if ( !(IRCd.serverName.equals(nameToIngnore)) ) {*/
	    
	    ArrayList<String> lns = new ArrayList<String>();
	    lns.add(message);
	    if((message.length() + Config.getIrcdConsoleChannel().length()) > 329) {
		// TODO A string[] of a random size that I've chosen is probably quite inefficient.. look into a way to improve this ~WizardCM
		String[] l = new String[30];
		l = Iterables.toArray(Splitter.fixedLength(329 - Config.getIrcdConsoleChannel().length()).split(message), String.class);
		lns.remove(message);
		for (String i : l) {
		    lns.add(i.toString());
		}
	    }
	    for(int ln = 0; ln < lns.size(); ln++) {
	    switch (IRCd.mode) {
		case STANDALONE:
		    IRCFunctionality.writeOpers(":" + Config.getIrcdServerName() + "!" +
			    Config.getIrcdServerName() + "@" +
			    Config.getIrcdServerHostName() + " PRIVMSG " +
			    Config.getIrcdConsoleChannel() + " :" +
			    ChatUtils.convertColors(lns.get(ln).toString(), false));
		    break;
		case INSPIRCD:
		    if (IRCd.isLinkcompleted()) {
			IRCFunctionality.privmsg(IRCd.serverUID,
				Config.getIrcdConsoleChannel(),
				ChatUtils.convertColors(lns.get(ln).toString(), false));
		    }
		    break;
	    }/*
	     }
	     }*/
	    }
	}
    }

    /**
    If the message to the staff channel is multi-part, it gets looped through
    @param message The messages to be sent
    */
    @Override
    public void sendMessage(String[] message) {
	for (String s : message) {
	    sendMessage(s);
	}
    }
    
    /**
    Overrides isOp to always return true
    @return Always true, not sure if it should
    */
    @Override
    public boolean isOp() {
	// TODO Is this supposed to always return true? Why? Possibly related to setOp which cannot change operator status of IRC users.. but why?
	// return client.isOper;
	return true;
    }

    /**
    Supposed to set the op status, however currently returns that it can't.
    @param value UnsupportedOperationException
    */
    @Override
    public void setOp(boolean value) {
	// client.isOper = value;
	throw new UnsupportedOperationException("Cannot change operator status of IRC users");
    }

    /**
    Gets the server in question
    @return Server
    */
    @Override
    public Server getServer() {
	return server;
    }

    @Override
    public boolean isPermissionSet(String name) {
	return perm.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
	return this.perm.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
	return perm.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
	return this.perm.hasPermission(perm);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name,
	    boolean value) {
	return perm.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
	return perm.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name,
	    boolean value, int ticks) {
	return perm.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
	return perm.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
	perm.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
	perm.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
	return perm.getEffectivePermissions();
    }

    /**
    Gets the name of the console
    @return IRC Nickname of the console bot
    */
    @Override
    public String getName() {
	return nick;
    }

    /**
    Sets the name of the console bot
    @param name New name of the bot
    */
    public void setName(String name) {
	nick = name;
    }
}
