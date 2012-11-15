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

import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * BukkitIRCd block listener
 * @author Jdbye
 */
public class BukkitIRCdServerListener implements Listener {
    private final BukkitIRCdPlugin plugin;

    public BukkitIRCdServerListener(final BukkitIRCdPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
    	Plugin eventPlugin = event.getPlugin();
    	if (eventPlugin.getClass().toString().equalsIgnoreCase("class com.nijikokun.bukkit.Permissions.Permissions")) {
    		plugin.setupPermissions((Permissions)eventPlugin);
    	}
    	else if (eventPlugin.getClass().toString().equalsIgnoreCase("class org.dynmap.DynmapPlugin")) {
    		plugin.setupDynmap((DynmapAPI)eventPlugin);
    	}
    	//else System.out.println("[BukkitIRCd] Detected plugin load: "+eventPlugin.getClass().toString());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
    	Plugin eventPlugin = event.getPlugin();
    	if (eventPlugin.getClass().toString().equalsIgnoreCase("class com.nijikokun.bukkit.Permissions.Permissions")) {
    		plugin.unloadPermissions();
    	}
    	else if (eventPlugin.getClass().toString().equalsIgnoreCase("class org.dynmap.DynmapPlugin")) {
    		plugin.unloadDynmap();
    	}
    	//else System.out.println("[BukkitIRCd] Detected plugin unload: "+eventPlugin.getClass().toString());
    }

    //put all Block related code here
}
