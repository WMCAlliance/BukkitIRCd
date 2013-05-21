package com.Jdbye.BukkitIRCd.configuration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.Hash;
import com.Jdbye.BukkitIRCd.HashType;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class Config
{
    private static String mode = "standalone";
    private static Date curDate = new Date();
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
    private static String serverCreationDate = dateFormat.format(curDate);
    private static boolean ircdRedundantModes = false;
    private static int ircdPort = 6667;
    private static int ircdMaxConnections = 1000;
    private static int ircdPingInterval = 45;
    private static int ircdPinkTimeoutInterval = 180;
    private static int ircdMaxNickLength = 32;
    private static String ircdChannel = "#minecraft";
    private static String ircdServerName = "BukkitIRCd";
    private static String ircdServerDescription = "Minecraft BukkitIRCd Server";
    private static String ircdServerHostName = "bukkitircd.localhost";
    private static String ircdIngameSuffix = "/minecraft";
    private static String ircdTopic = "Welcome to a Bukkit server!";
    private static String ircdTopicSetBy = ircdServerName;
    private static long ircdTopicSetDate = System.currentTimeMillis() / 1000L;
    private static String ircdBantype = "ip";
    private static boolean ircdConvertColorCodes = true;
    private static boolean ircdHandleAmpersandColors = true;
    private static boolean ircdNoticesEnabled = true;
    private static String ircdOperUser = "";
    private static String ircdOperPass = "";
    private static String ircdOperModes = "~&@%+";
    private static String ircdConsoleChannel = "#staff";
    private static String ircdIrcColors = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15";
    private static String ircdGameColors = "0,f,1,2,c,4,5,6,e,a,3,b,9,d,8,7";
    private static boolean ircdColorDeathMessagesEnabled = false;
    private static boolean ircdColorSayMessageEnabled = false;
    private static boolean ircdBroadCastDeathMessages = true;
    private static boolean ircdIngameSuffixStripEnabled = true;
    private static boolean debugModeEnabled = false;
    private static String linkRemoteHost = "localhost";
    private static int linkRemotePort = 7000;
    private static int linkLocalPort = 7000;
    private static boolean linkAutoconnect = true;
    private static String linkName = "irc.localhost";
    private static String linkConnectPassword = "test";
    private static String linkReceivePassword = "test";
    private static int linkPingInterval = 60;
    private static int linkTimeout = 180;
    private static int linkDelay = 60;
    private static int linkServerID = new Random().nextInt(900) + 100;
    private static List<String> kickCommands = Arrays.asList("/kick");
    private static boolean enableRawSend = false;
    private static BukkitIRCdPlugin plugin = BukkitIRCdPlugin.thePlugin;
    private static FileConfiguration config;

    /**
     * Saves the configuration file.
     */
    public static void saveConfiguration()
    {
        try
        {
            plugin.saveConfig();
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Saved configuration file." + (isDebugModeEnabled() ? " Code BukkitIRCdPlugin742." : ""));
        }
        catch (final Exception e)
        {
            BukkitIRCdPlugin.log.warning("[BukkitIRCd] Caught exception while writing settings to file: ");
            e.printStackTrace();
        }
    }

    /**
     * Reloads the configuration file.
     */
    public static void reloadConfiguration()
    {
        plugin.reloadConfig();
        config = plugin.getConfig();
        // IF config file doesn't exist, create it
        if (!(new File(plugin.getDataFolder(), "config.yml")).exists())
        {
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Creating default configuration file." + (isDebugModeEnabled() ? " Code BukkitIRCdPlugin183." : ""));
            config.options().copyDefaults(true);
        }
        loadConfiguration();
    }

    /**
     * Loads the configuration.
     */
    public static void loadConfiguration()
    {
        try
        {
            ircdRedundantModes = config.getBoolean("redundant-modes", ircdRedundantModes);
            ircdIngameSuffixStripEnabled = config.getBoolean("strip-ingame-suffix", ircdIngameSuffixStripEnabled);
            ircdColorDeathMessagesEnabled = config.getBoolean("color-death-messages", ircdColorDeathMessagesEnabled);
            ircdColorSayMessageEnabled = config.getBoolean("color-say-messages", ircdColorSayMessageEnabled);
            BukkitIRCdPlugin.mode = config.getString("mode", BukkitIRCdPlugin.mode);
            ircdIngameSuffix = config.getString("ingame-suffix", ircdIngameSuffix);
            ircdNoticesEnabled = config.getBoolean("enable-notices", ircdNoticesEnabled);
            ircdConvertColorCodes = config.getBoolean("convert-color-codes", ircdConvertColorCodes);
            ircdHandleAmpersandColors = config.getBoolean("handle-ampersand-colors", ircdHandleAmpersandColors);
            ircdIrcColors = config.getString("irc-colors", ircdIrcColors);
            ircdGameColors = config.getString("game-colors", ircdGameColors);
            ircdChannel = config.getString("channel-name", ircdChannel);
            ircdConsoleChannel = config.getString("console-channel-name", ircdConsoleChannel);
            BukkitIRCdPlugin.ircd_creationdate = config.getString("server-creation-date", BukkitIRCdPlugin.ircd_creationdate);
            ircdServerName = config.getString("server-name", ircdServerName);
            ircdServerDescription = config.getString("server-description", ircdServerDescription);
            ircdServerHostName = config.getString("server-host", ircdServerHostName);
            ircdBantype = config.getString("ban-type", ircdBantype);
            debugModeEnabled = config.getBoolean("debug-mode", debugModeEnabled);
            enableRawSend = config.getBoolean("enable-raw-send", enableRawSend);
            kickCommands = config.getStringList("kick-commands");

            String operpass = "";

            ircdPort = config.getInt("standalone.port", ircdPort);
            ircdMaxConnections = config.getInt("standalone.max-connections", ircdMaxConnections);
            ircdPingInterval = config.getInt("standalone.ping-interval", ircdPingInterval);
            ircdPinkTimeoutInterval = config.getInt("standalone.timeout", ircdPinkTimeoutInterval);
            ircdMaxNickLength = config.getInt("standalone.max-nick-length", ircdMaxNickLength);
            ircdOperUser = config.getString("standalone.oper-username", ircdOperUser);
            operpass = config.getString("standalone.oper-password", ircdOperPass);
            ircdOperModes = config.getString("standalone.oper-modes", ircdOperModes);
            ircdTopic = config.getString("standalone.channel-topic", ircdTopic).replace("^K", (char) 3 + "").replace("^B", (char) 2 + "").replace("^I", (char) 29 + "").replace("^O", (char) 15 + "").replace("^U", (char) 31 + "");
            ircdTopicSetBy = config.getString("standalone.channel-topic-set-by", ircdTopicSetBy);
            ircdBroadCastDeathMessages = config.getBoolean("broadcast-death-messages", ircdBroadCastDeathMessages);
            try
            {
                ircdTopicSetDate = BukkitIRCdPlugin.dateFormat.parse(config.getString("standalone.channel-topic-set-date", BukkitIRCdPlugin.dateFormat.format(ircdTopicSetDate))).getTime();
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }

            linkRemoteHost = config.getString("inspircd.remote-host", linkRemoteHost);
            linkRemotePort = config.getInt("inspircd.remote-port", linkRemotePort);
            linkLocalPort = config.getInt("inspircd.local-port", linkLocalPort);
            linkAutoconnect = config.getBoolean("inspircd.auto-connect", linkAutoconnect);
            linkName = config.getString("inspircd.link-name", linkName);
            linkConnectPassword = config.getString("inspircd.connect-password", linkConnectPassword);
            linkReceivePassword = config.getString("inspircd.receive-password", linkReceivePassword);
            linkPingInterval = config.getInt("inspircd.ping-interval", linkPingInterval);
            linkTimeout = config.getInt("inspircd.timeout", linkTimeout);
            linkDelay = config.getInt("inspircd.connect-delay", linkDelay);
            linkServerID = config.getInt("inspircd.server-id", linkServerID);

            if (operpass.length() == 0) ircdOperPass = "";
            else if (operpass.startsWith("~"))
            {
                ircdOperPass = operpass.substring(1);
            }
            else
            {
                ircdOperPass = Hash.compute(operpass, HashType.SHA_512);
            }

            BukkitIRCdPlugin.log.info("[BukkitIRCd] Loaded configuration file." + (isDebugModeEnabled() ? " Code BukkitIRCdPlugin363." : ""));

            //saveConfig();
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Saved initial configuration file." + (isDebugModeEnabled() ? " Code BukkitIRCdPlugin365." : ""));
        }
        catch (Exception e)
        {
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load configuration file: " + e.toString());
        }

    }

    public static String getMode()
    {
        return mode;
    }

    public static void setMode(final String mode)
    {
        Config.mode = mode;
    }

    public static Date getCurDate()
    {
        return curDate;
    }

    public static void setCurDate(final Date curDate)
    {
        Config.curDate = curDate;
    }

    public static SimpleDateFormat getDateFormat()
    {
        return dateFormat;
    }

    public static void setDateFormat(final SimpleDateFormat dateFormat)
    {
        Config.dateFormat = dateFormat;
    }

    public static String getServerCreationDate()
    {
        return serverCreationDate;
    }

    public static void setServerCreationDate(final String serverCreationDate)
    {
        Config.serverCreationDate = serverCreationDate;
    }

    public static boolean isIrcdRedundantModes()
    {
        return ircdRedundantModes;
    }

    public static void setIrcdRedundantModes(final boolean ircdRedundantModes)
    {
        Config.ircdRedundantModes = ircdRedundantModes;
    }

    public static int getIrcdPort()
    {
        return ircdPort;
    }

    public static void setIrcdPort(final int ircdPort)
    {
        Config.ircdPort = ircdPort;
    }

    public static int getIrcdMaxConnections()
    {
        return ircdMaxConnections;
    }

    public static void setIrcdMaxConnections(final int ircdMaxConnections)
    {
        Config.ircdMaxConnections = ircdMaxConnections;
    }

    public static int getIrcdPingInterval()
    {
        return ircdPingInterval;
    }

    public static void setIrcdPingInterval(final int ircdPingInterval)
    {
        Config.ircdPingInterval = ircdPingInterval;
    }

    public static int getIrcdPinkTimeoutInterval()
    {
        return ircdPinkTimeoutInterval;
    }

    public static void setIrcdPinkTimeoutInterval(final int ircdPinkTimeoutInterval)
    {
        Config.ircdPinkTimeoutInterval = ircdPinkTimeoutInterval;
    }

    public static int getIrcdMaxNickLength()
    {
        return ircdMaxNickLength;
    }

    public static void setIrcdMaxNickLength(final int ircdMaxNickLength)
    {
        Config.ircdMaxNickLength = ircdMaxNickLength;
    }

    public static String getIrcdChannel()
    {
        return ircdChannel;
    }

    public static void setIrcdChannel(final String ircdChannel)
    {
        Config.ircdChannel = ircdChannel;
    }

    public static String getIrcdServerName()
    {
        return ircdServerName;
    }

    public static void setIrcdServerName(final String ircdServerName)
    {
        Config.ircdServerName = ircdServerName;
    }

    public static String getIrcdServerDescription()
    {
        return ircdServerDescription;
    }

    public static void setIrcdServerDescription(final String ircdServerDescription)
    {
        Config.ircdServerDescription = ircdServerDescription;
    }

    public static String getIrcdServerHostName()
    {
        return ircdServerHostName;
    }

    public static void setIrcdServerHostName(final String ircdServerHostName)
    {
        Config.ircdServerHostName = ircdServerHostName;
    }

    public static String getIrcdIngameSuffix()
    {
        return ircdIngameSuffix;
    }

    public static void setIrcdIngameSuffix(final String ircdIngameSuffix)
    {
        Config.ircdIngameSuffix = ircdIngameSuffix;
    }

    public static String getIrcdTopic()
    {
        return ircdTopic;
    }

    public static void setIrcdTopic(final String ircdTopic)
    {
        Config.ircdTopic = ircdTopic;
    }

    public static String getIrcdTopicSetBy()
    {
        return ircdTopicSetBy;
    }

    public static void setIrcdTopicSetBy(final String ircdTopicSetBy)
    {
        Config.ircdTopicSetBy = ircdTopicSetBy;
    }

    public static long getIrcdTopicSetDate()
    {
        return ircdTopicSetDate;
    }

    public static void setIrcdTopicSetDate(final long ircdTopicSetDate)
    {
        Config.ircdTopicSetDate = ircdTopicSetDate;
    }

    public static String getIrcdBantype()
    {
        return ircdBantype;
    }

    public static void setIrcdBantype(final String ircdBantype)
    {
        Config.ircdBantype = ircdBantype;
    }

    public static boolean isIrcdConvertColorCodes()
    {
        return ircdConvertColorCodes;
    }

    public static void setIrcdConvertColorCodes(final boolean ircdConvertColorCodes)
    {
        Config.ircdConvertColorCodes = ircdConvertColorCodes;
    }

    public static boolean isIrcdHandleAmpersandColors()
    {
        return ircdHandleAmpersandColors;
    }

    public static void setIrcdHandleAmpersandColors(final boolean ircdHandleAmpersandColors)
    {
        Config.ircdHandleAmpersandColors = ircdHandleAmpersandColors;
    }

    public static boolean isIrcdNoticesEnabled()
    {
        return ircdNoticesEnabled;
    }

    public static void setIrcdNoticesEnabled(final boolean ircdNoticesEnabled)
    {
        Config.ircdNoticesEnabled = ircdNoticesEnabled;
    }

    public static String getIrcdOperUser()
    {
        return ircdOperUser;
    }

    public static void setIrcdOperUser(final String ircdOperUser)
    {
        Config.ircdOperUser = ircdOperUser;
    }

    public static String getIrcdOperPass()
    {
        return ircdOperPass;
    }

    public static void setIrcdOperPass(final String ircdOperPass)
    {
        Config.ircdOperPass = ircdOperPass;
    }

    public static String getIrcdOperModes()
    {
        return ircdOperModes;
    }

    public static void setIrcdOperModes(final String ircdOperModes)
    {
        Config.ircdOperModes = ircdOperModes;
    }

    public static String getIrcdConsoleChannel()
    {
        return ircdConsoleChannel;
    }

    public static void setIrcdConsoleChannel(final String ircdConsoleChannel)
    {
        Config.ircdConsoleChannel = ircdConsoleChannel;
    }

    public static String getIrcdIrcColors()
    {
        return ircdIrcColors;
    }

    public static void setIrcdIrcColors(final String ircdIrcColors)
    {
        Config.ircdIrcColors = ircdIrcColors;
    }

    public static String getIrcdGameColors()
    {
        return ircdGameColors;
    }

    public static void setIrcdGameColors(final String ircdGameColors)
    {
        Config.ircdGameColors = ircdGameColors;
    }

    public static boolean isIrcdColorDeathMessagesEnabled()
    {
        return ircdColorDeathMessagesEnabled;
    }

    public static void setIrcdColorDeathMessagesEnabled(final boolean ircdColorDeathMessagesEnabled)
    {
        Config.ircdColorDeathMessagesEnabled = ircdColorDeathMessagesEnabled;
    }

    public static boolean isIrcdColorSayMessageEnabled()
    {
        return ircdColorSayMessageEnabled;
    }

    public static void setIrcdColorSayMessageEnabled(final boolean ircdColorSayMessageEnabled)
    {
        Config.ircdColorSayMessageEnabled = ircdColorSayMessageEnabled;
    }

    public static boolean isIrcdBroadCastDeathMessages()
    {
        return ircdBroadCastDeathMessages;
    }

    public static void setIrcdBroadCastDeathMessages(final boolean ircdBroadCastDeathMessages)
    {
        Config.ircdBroadCastDeathMessages = ircdBroadCastDeathMessages;
    }

    public static boolean isDebugModeEnabled()
    {
        return debugModeEnabled;
    }

    public static void setDebugModeEnabled(final boolean debugModeEnabled)
    {
        Config.debugModeEnabled = debugModeEnabled;
    }

    public static boolean isIrcdIngameSuffixStripEnabled()
    {
        return ircdIngameSuffixStripEnabled;
    }

    public static void setIrcdIngameSuffixStripEnabled(final boolean ircdIngameSuffixStripEnabled)
    {
        Config.ircdIngameSuffixStripEnabled = ircdIngameSuffixStripEnabled;
    }

    public static String getLinkRemoteHost()
    {
        return linkRemoteHost;
    }

    public static void setLinkRemoteHost(final String linkRemoteHost)
    {
        Config.linkRemoteHost = linkRemoteHost;
    }

    public static int getLinkRemotePort()
    {
        return linkRemotePort;
    }

    public static void setLinkRemotePort(final int linkRemotePort)
    {
        Config.linkRemotePort = linkRemotePort;
    }

    public static int getLinkLocalPort()
    {
        return linkLocalPort;
    }

    public static void setLinkLocalPort(final int linkLocalPort)
    {
        Config.linkLocalPort = linkLocalPort;
    }

    public static boolean isLinkAutoconnect()
    {
        return linkAutoconnect;
    }

    public static void setLinkAutoconnect(final boolean linkAutoconnect)
    {
        Config.linkAutoconnect = linkAutoconnect;
    }

    public static String getLinkName()
    {
        return linkName;
    }

    public static void setLinkName(final String linkName)
    {
        Config.linkName = linkName;
    }

    public static String getLinkConnectPassword()
    {
        return linkConnectPassword;
    }

    public static void setLinkConnectPassword(final String linkConnectPassword)
    {
        Config.linkConnectPassword = linkConnectPassword;
    }

    public static String getLinkReceivePassword()
    {
        return linkReceivePassword;
    }

    public static void setLinkReceivePassword(final String linkReceivePassword)
    {
        Config.linkReceivePassword = linkReceivePassword;
    }

    public static int getLinkPingInterval()
    {
        return linkPingInterval;
    }

    public static void setLinkPingInterval(final int linkPingInterval)
    {
        Config.linkPingInterval = linkPingInterval;
    }

    public static int getLinkTimeout()
    {
        return linkTimeout;
    }

    public static void setLinkTimeout(final int linkTimeout)
    {
        Config.linkTimeout = linkTimeout;
    }

    public static int getLinkDelay()
    {
        return linkDelay;
    }

    public static void setLinkDelay(final int linkDelay)
    {
        Config.linkDelay = linkDelay;
    }

    public static int getLinkServerID()
    {
        return linkServerID;
    }

    public static void setLinkServerID(final int linkServerID)
    {
        Config.linkServerID = linkServerID;
    }

    public static List<String> getKickCommands()
    {
        return kickCommands;
    }

    public static void setKickCommands(final List<String> kickCommands)
    {
        Config.kickCommands = kickCommands;
    }

    public static boolean isEnableRawSend()
    {
        return enableRawSend;
    }

    public static void setEnableRawSend(final boolean enableRawSend)
    {
        Config.enableRawSend = enableRawSend;
    }

    public static BukkitIRCdPlugin getPlugin()
    {
        return plugin;
    }

    public static void setPlugin(final BukkitIRCdPlugin plugin)
    {
        Config.plugin = plugin;
    }

    public static FileConfiguration getConfig()
    {
        return config;
    }

    public static void setConfig(final FileConfiguration config)
    {
        Config.config = config;
    }
}
