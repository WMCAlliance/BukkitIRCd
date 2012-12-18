package com.Jdbye.BukkitIRCd;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.bukkit.permissions.*;

//import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * BukkitIRCd block listener
 * @author Jdbye
 */
public class BukkitIRCdServerListener implements Listener {
    private final BukkitIRCdPlugin plugin;

    public BukkitIRCdServerListener(final BukkitIRCdPlugin plugin) {
        this.plugin = plugin;
    }
}
