package com.Jdbye.BukkitIRCd.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;

public class IRCReloadCommand implements CommandExecutor {

    private final BukkitIRCdPlugin thePlugin;

    public IRCReloadCommand(BukkitIRCdPlugin plugin) {
        this.thePlugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
            String[] args) {
        thePlugin.pluginInit(true);
        BukkitIRCdPlugin.log.info("[BukkitIRCd] Configuration file reloaded.");
        sender.sendMessage(ChatColor.RED + "Configuration file reloaded.");
        return true;
    }

}
