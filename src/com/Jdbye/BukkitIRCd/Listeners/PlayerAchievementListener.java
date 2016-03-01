package com.Jdbye.BukkitIRCd.Listeners;

import java.io.File;
import java.util.logging.Logger;

import com.Jdbye.BukkitIRCd.IRCd;
import org.bukkit.Achievement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCFunctionality;
import com.Jdbye.BukkitIRCd.Utilities.ChatUtils;

public class PlayerAchievementListener implements Listener {
    public static final Logger log = Logger.getLogger("Minecraft");
    //public static FileConfiguration achivements;


    public PlayerAchievementListener() {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
        //IRCFunctionality.writeAll(ChatUtils.convertColors("ACHIEVEMENT: " + event.getPlayer().getName() + " => " + event.getAchievement(), false), event.getPlayer());
        //achivements = YamlConfiguration.loadConfiguration(achivementsFile);
       // Achievement achievement = event.getAchievement();
       // String achievement_title = achievement.toString();
        //if (achivements.contains(achievement_title)){
           // String achivement_message = achivements.getString(achievement_title);
            //final String new_message = message.replace("{User}", event.getPlayer().getName()).replace("{Message}", achivement_message);
            //IRCFunctionality.writeAll(ChatUtils.convertColors(new_message, false));
        }



    //}

}