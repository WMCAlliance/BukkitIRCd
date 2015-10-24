package com.Jdbye.BukkitIRCd.Listeners;

import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCFunctionality;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;

public class PlayerAchievementListener implements Listener {

    public static final Logger log = Logger.getLogger("Minecraft");
    private BukkitIRCdPlugin thePlugin;

    public PlayerAchievementListener(BukkitIRCdPlugin plugin) {
        this.thePlugin = plugin;
    }


    /**
     *
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
        IRCFunctionality.writeAll(ChatUtils.convertColors("ACHIEVEMENT: " + event.getPlayer().getName() + " => " + event.getAchievement(), false), event.getPlayer());
        //event.getPlayer()
        //event.getAchievement());

    }



}