package com.Jdbye.BukkitIRCd.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCUser;
import com.Jdbye.BukkitIRCd.IRCd;

public class LoadConfig {
	
	
	public boolean ircd_redundant_modes = false;
	public int ircd_port = 6667;
	public int ircd_maxconn = 1000;
	public int ircd_pinginterval = 45;
	public int ircd_timeout = 180;
	public int ircd_maxnicklen = 32;
	public String ircd_channel = "#minecraft";
	public static String ircd_servername = "BukkitIRCd";
	public String ircd_serverdescription = "Minecraft BukkitIRCd Server";
	public String ircd_serverhostname = "bukkitircd.localhost";
	public String ircd_ingamesuffix = "/minecraft";
	public static String ircd_topic = "Welcome to a Bukkit server!";
	public static String ircd_topicsetby = ircd_servername;
	public static long ircd_topicsetdate = System.currentTimeMillis() / 1000L;
	public static String ircd_bantype = "ip";
	public boolean ircd_convertcolorcodes = true;
	public static boolean ircd_handleampersandcolors = true;
	public static boolean ircd_enablenotices = true;
	public String ircd_operuser = "";
	public String ircd_operpass = "";
	public String ircd_opermodes = "~&@%+";
	public String ircd_consolechannel = "#staff";
	public String ircd_irc_colors = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15";
	public String ircd_game_colors = "0,f,1,2,c,4,5,6,e,a,3,b,9,d,8,7";
	public boolean ircd_color_death_messages = false;
	public boolean ircd_color_say_messages = false;
	public boolean ircd_broadcast_death_messages = true;
	public static boolean debugmode = false;
	public boolean ircd_strip_ingame_suffix = true;
	
	public static String link_remotehost = "localhost";
	public static int link_remoteport = 7000;
	public static int link_localport = 7000;
	public static boolean link_autoconnect = true;
	public static String link_name = "irc.localhost";
	public static String link_connectpassword = "test";
	public static String link_receivepassword = "test";
	public static int link_pinginterval = 60;
	public static int link_timeout = 180;
	public static int link_delay = 60;
	public static int link_serverid = new Random().nextInt(900) + 100;

	public static List<String> kickCommands = Arrays.asList("/kick");
	
	public boolean enableRawSend = false;
	
	static IRCd ircd = null;
	private Thread thr = null;
	
	FileConfiguration config;
	
	File configFile = new File(getDataFolder(), "config.yml");

}