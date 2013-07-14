package com.Jdbye.BukkitIRCd;

import com.Jdbye.BukkitIRCd.configuration.Config;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.regex.PatternSyntaxException;

// import org.bukkit.ChatColor;

public class IRCCommandSender implements CommandSender {
    private Server server;
    private final PermissibleBase perm = new PermissibleBase(this);
    private boolean enabled = false;
    private String nick;

	public IRCCommandSender(Server server) {
		super();
        this.server = server;
        this.nick = Config.getIrcdServerName();
        enabled = true;
    }

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
		}
    	

    			if (IRCd.mode == Modes.STANDALONE) IRCd.writeOpers(":" + Config.getIrcdServerName() + "!" + Config.getIrcdServerName() + "@" + Config.getIrcdServerHostName() + " PRIVMSG " + Config.getIrcdConsoleChannel() + " :" + IRCd.convertColors(message, false));
    			else if (IRCd.isLinkcompleted()) IRCd.println(":" + IRCd.serverUID + " PRIVMSG " + Config.getIrcdConsoleChannel() + " :" + IRCd.convertColors(message, false));
    		}
    	
    
    
    public void sendMessage(String[] message) {
    	for (String s : message) sendMessage(s);
    }

    public void disable() {
    	enabled = false;
    }

    public void enable() {
    	enabled = true;
    }

    public boolean isOp() {
        //return client.isOper;
    	return true;
    }

    public void setOp(boolean value) {
        //client.isOper = value;
    	throw new UnsupportedOperationException("Cannot change operator status of IRC users");
    }

    public boolean isPlayer() {
        return false;
    }

    public Server getServer() {
        return server;
    }

    public boolean isPermissionSet(String name) {
        return perm.isPermissionSet(name);
    }

    public boolean isPermissionSet(Permission perm) {
        return this.perm.isPermissionSet(perm);
    }

    public boolean hasPermission(String name) {
        return perm.hasPermission(name);
    }

    public boolean hasPermission(Permission perm) {
        return this.perm.hasPermission(perm);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return perm.addAttachment(plugin, name, value);
    }

    public PermissionAttachment addAttachment(Plugin plugin) {
        return perm.addAttachment(plugin);
    }

    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return perm.addAttachment(plugin, name, value, ticks);
    }

    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return perm.addAttachment(plugin, ticks);
    }

    public void removeAttachment(PermissionAttachment attachment) {
        perm.removeAttachment(attachment);
    }

    public void recalculatePermissions() {
        perm.recalculatePermissions();
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return perm.getEffectivePermissions();
    }

    public String getName() {
        return nick;
    }

    public void setName(String name) {
        nick = name;
    }
}
