package com.Jdbye.BukkitIRCd.configuration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.Hash;
import com.Jdbye.BukkitIRCd.HashType;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Provides a simple configuration interface.
 */
public final class Config
{
    //private static final String HashType = null;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
    private static String serverCreationDate = dateFormat.format(new Date());
    private static String ircdOperPass = "";  // caching only
    private static BukkitIRCdPlugin plugin = BukkitIRCdPlugin.thePlugin;
    private static FileConfiguration config;

    private Config()
    {

    }

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


        loadConfiguration();
    }

    /**
     * Loads the configuration.
     */
    public static void loadConfiguration()
    {
        config = plugin.getConfig();

        if (!(new File(plugin.getDataFolder(), "config.yml")).exists())
        {
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Creating default configuration file." + (isDebugModeEnabled() ? " Code BukkitIRCdPlugin183." : ""));
            config.options().copyDefaults(true);
        }

        try
        {
            BukkitIRCdPlugin.ircd_creationdate = config.getString("server-creation-date", BukkitIRCdPlugin.ircd_creationdate);
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Loaded configuration file." + (isDebugModeEnabled() ? " Code BukkitIRCdPlugin363." : ""));
        }
        catch (Exception e)
        {
            BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load configuration file: " + e.toString());
        }

    }

    public static String getMode()
    {
        final String mode = "standalone";
        return config.getString("mode", mode);
    }

    public static void setMode(final String mode)
    {
        config.set("mode", mode);
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

    public static boolean isIrcdRedundantModes()
    {
        final boolean ircdRedundantModes = false;
        return config.getBoolean("redundant-modes", ircdRedundantModes);
    }

    public static void setIrcdRedundantModes(final boolean ircdRedundantModes)
    {
        config.set("redundant-modes", ircdRedundantModes);
    }

    public static int getIrcdPort()
    {
        final int ircdPort = 6667;
        return config.getInt("standalone.port", ircdPort);
    }

    public static void setIrcdPort(final int ircdPort)
    {
        config.set("standalone.port", ircdPort);
    }

    public static int getIrcdMaxConnections()
    {
        final int ircdMaxConnections = 1000;
        return config.getInt("standalone.max-connections", ircdMaxConnections);
    }

    public static void setIrcdMaxConnections(final int ircdMaxConnections)
    {
        config.set("standalone.max-connections", ircdMaxConnections);
    }

    public static int getIrcdPingInterval()
    {
        final int ircdPingInterval = 45;
        return config.getInt("standalone.ping-interval", ircdPingInterval);
    }

    public static void setIrcdPingInterval(final int ircdPingInterval)
    {
        config.set("standalone.ping-interval", ircdPingInterval);
    }

    public static int getIrcdPinkTimeoutInterval()
    {
        final int ircdPinkTimeoutInterval = 180;
        return config.getInt("standalone.timeout", ircdPinkTimeoutInterval);
    }

    public static void setIrcdPinkTimeoutInterval(final int ircdPinkTimeoutInterval)
    {
        config.set("standalone.timeout", ircdPinkTimeoutInterval);
    }

    public static int getIrcdMaxNickLength()
    {
        final int ircdMaxNickLength = 32;
        return config.getInt("standalone.max-nick-length", ircdMaxNickLength);
    }

    public static void setIrcdMaxNickLength(final int ircdMaxNickLength)
    {
        config.set("standalone.max-nick-length", ircdMaxNickLength);
    }

    public static String getIrcdChannel()
    {
        final String ircdChannel = "#minecraft";
        return config.getString("channel-name", ircdChannel);
    }

    public static void setIrcdChannel(final String ircdChannel)
    {
        config.set("channel-name", ircdChannel);
    }

    public static String getIrcdServerName()
    {
        final String ircdServerName = "BukkitIRCd";
        return config.getString("server-name", ircdServerName);
    }

    public static void setIrcdServerName(final String ircdServerName)
    {
        config.set("server-name", ircdServerName);
    }

    public static String getIrcdServerDescription()
    {
        final String ircdServerDescription = "Minecraft BukkitIRCd Server";
        return config.getString("server-description", ircdServerDescription);
    }

    public static void setIrcdServerDescription(final String ircdServerDescription)
    {
        config.set("server-description", ircdServerDescription);
    }

    public static String getIrcdServerHostName()
    {
        final String ircdServerHostName = "bukkitircd.localhost";
        return config.getString("server-host", ircdServerHostName);
    }

    public static void setIrcdServerHostName(final String ircdServerHostName)
    {
        config.set("server-host", ircdServerHostName);
    }

    public static String getIrcdIngameSuffix()
    {
        final String ircdIngameSuffix = "/minecraft";
        return config.getString("ingame-suffix", ircdIngameSuffix);
    }

    public static void setIrcdIngameSuffix(final String ircdIngameSuffix)
    {
        config.set("ingame-suffix", ircdIngameSuffix);
    }

    public static String getIrcdTopic()
    {
        final String ircdTopic = "Welcome to a Bukkit server!";
        return config.getString("standalone.channel-topic", ircdTopic).replace("^K", (char) 3 + "").replace("^B", (char) 2 + "").replace("^I", (char) 29 + "").replace("^O", (char) 15 + "").replace("^U", (char) 31 + "");
    }

    public static void setIrcdTopic(final String ircdTopic)
    {
        config.set("standalone.channel-topic", ircdTopic.replace("^K", (char) 3 + "").replace("^B", (char) 2 + "").replace("^I", (char) 29 + "").replace("^O", (char) 15 + "").replace("^U", (char) 31 + ""));
    }

    public static String getIrcdTopicSetBy()
    {
        return config.getString("standalone.channel-topic-set-by", getIrcdServerName());
    }

    public static void setIrcdTopicSetBy(final String ircdTopicSetBy)
    {
        config.set("standalone.channel-topic-set-by", ircdTopicSetBy);
    }

    public static long getIrcdTopicSetDate()
    {
        try
        {
            return BukkitIRCdPlugin.dateFormat.parse(config.getString("standalone.channel-topic-set-date", BukkitIRCdPlugin.dateFormat.format(System.currentTimeMillis() / 1000L))).getTime();
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return (new Date()).getTime();
    }

    public static void setIrcdTopicSetDate(final long ircdTopicSetDate)
    {
        config.set("standalone.channel-topic-set-date", ircdTopicSetDate);
    }

    public static String getIrcdBantype()
    {
        final String ircdBantype = "ip";
        return config.getString("ban-type", ircdBantype);
    }

    public static void setIrcdBantype(final String ircdBantype)
    {
        config.set("ban-type", ircdBantype);
    }

    public static boolean isIrcdConvertColorCodes()
    {
        final boolean ircdConvertColorCodes = true;
        return config.getBoolean("convert-color-codes", ircdConvertColorCodes);
    }

    public static void setIrcdConvertColorCodes(final boolean ircdConvertColorCodes)
    {
        config.set("convert-color-codes", ircdConvertColorCodes);
    }

    public static boolean isIrcdHandleAmpersandColors()
    {
        final boolean ircdHandleAmpersandColors = true;
        return config.getBoolean("handle-ampersand-colors", ircdHandleAmpersandColors);
    }

    public static void setIrcdHandleAmpersandColors(final boolean ircdHandleAmpersandColors)
    {
        config.set("handle-ampersand-colors", ircdHandleAmpersandColors);
    }

    public static boolean isIrcdNoticesEnabled()
    {
        final boolean ircdNoticesEnabled = true;
        return config.getBoolean("enable-notices", ircdNoticesEnabled);
    }

    public static void setIrcdNoticesEnabled(final boolean ircdNoticesEnabled)
    {
        config.set("enable-notices", ircdNoticesEnabled);
    }

    public static String getIrcdOperUser()
    {
        final String ircdOperUser = "";
        return config.getString("standalone.oper-username", ircdOperUser);
    }

    public static void setIrcdOperUser(final String ircdOperUser)
    {
        config.set("standalone.oper-username", ircdOperUser);
    }

    public static String getIrcdOperPass()
    {
        if (!ircdOperPass.isEmpty())
        {
            return ircdOperPass;
        }

        final String operpass = config.getString("standalone.oper-password", ircdOperPass);
        if (operpass.isEmpty())
        {
            ircdOperPass = operpass;
        }
        else if (operpass.startsWith("~"))
        {
            ircdOperPass = operpass.substring(1);
        }
        else if (!operpass.isEmpty())
        {
            ircdOperPass = HashType.compute(operpass, HashType.SHA_512);
        }
        return ircdOperPass;
    }

    public static void setIrcdOperPass(final String ircdOperPass)
    {
        config.set("standalone.oper-password", ircdOperPass);
    }

    public static String getIrcdOperModes()
    {
        final String ircdOperModes = "~&@%+";
        return config.getString("standalone.oper-modes", ircdOperModes);
    }

    public static void setIrcdOperModes(final String ircdOperModes)
    {
        config.set("standalone.oper-modes", ircdOperModes);
    }

    public static String getIrcdConsoleChannel()
    {
        final String ircdConsoleChannel = "#staff";
        return config.getString("console-channel-name", ircdConsoleChannel);
    }

    public static void setIrcdConsoleChannel(final String ircdConsoleChannel)
    {
        config.set("console-channel-name", ircdConsoleChannel);
    }

    public static String getIrcdIrcColors()
    {
        final String ircdIrcColors = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15";
        return config.getString("irc-colors", ircdIrcColors);
    }

    public static void setIrcdIrcColors(final String ircdIrcColors)
    {
        config.set("irc-colors", ircdIrcColors);
    }

    public static String getIrcdGameColors()
    {
        final String ircdGameColors = "0,f,1,2,c,4,5,6,e,a,3,b,9,d,8,7";
        return config.getString("game-colors", ircdGameColors);
    }

    public static void setIrcdGameColors(final String ircdGameColors)
    {
        config.set("game-colors", ircdGameColors);
    }

    public static boolean isIrcdColorDeathMessagesEnabled()
    {
        final boolean ircdColorDeathMessagesEnabled = false;
        return config.getBoolean("color-death-messages", ircdColorDeathMessagesEnabled);
    }

    public static void setIrcdColorDeathMessagesEnabled(final boolean ircdColorDeathMessagesEnabled)
    {
        config.set("color-death-messages", ircdColorDeathMessagesEnabled);
    }

    public static boolean isIrcdColorSayMessageEnabled()
    {
        final boolean ircdColorSayMessageEnabled = false;
        return config.getBoolean("color-say-messages", ircdColorSayMessageEnabled);
    }

    public static void setIrcdColorSayMessageEnabled(final boolean ircdColorSayMessageEnabled)
    {
        config.set("color-say-messages", ircdColorSayMessageEnabled);
    }

    public static boolean isIrcdBroadCastDeathMessages()
    {
        final boolean ircdBroadCastDeathMessages = true;
        return config.getBoolean("broadcast-death-messages", ircdBroadCastDeathMessages);
    }

    public static void setIrcdBroadCastDeathMessages(final boolean ircdBroadCastDeathMessages)
    {
        config.set("broadcast-death-messages", ircdBroadCastDeathMessages);
    }

    public static boolean isDebugModeEnabled()
    {
        final boolean debugModeEnabled = false;
        return config.getBoolean("debug-mode", debugModeEnabled);
    }

    public static void setDebugModeEnabled(final boolean debugModeEnabled)
    {
        config.set("debug-mode", debugModeEnabled);
    }

    public static boolean isIrcdIngameSuffixStripEnabled()
    {
        final boolean ircdIngameSuffixStripEnabled = true;
        return config.getBoolean("strip-ingame-suffix", ircdIngameSuffixStripEnabled);
    }

    public static void setIrcdIngameSuffixStripEnabled(final boolean ircdIngameSuffixStripEnabled)
    {
        config.set("strip-ingame-suffix", ircdIngameSuffixStripEnabled);
    }

    public static String getLinkRemoteHost()
    {
        final String linkRemoteHost = "localhost";
        return config.getString("inspircd.remote-host", linkRemoteHost);
    }

    public static void setLinkRemoteHost(final String linkRemoteHost)
    {
        config.set("inspircd.remote-host", linkRemoteHost);
    }

    public static int getLinkRemotePort()
    {
        final int linkRemotePort = 7000;
        return config.getInt("inspircd.remote-port", linkRemotePort);
    }

    public static void setLinkRemotePort(final int linkRemotePort)
    {
        config.set("inspircd.remote-port", linkRemotePort);
    }

    public static int getLinkLocalPort()
    {
        final int linkLocalPort = 7000;
        return config.getInt("inspircd.local-port", linkLocalPort);
    }

    public static void setLinkLocalPort(final int linkLocalPort)
    {
        config.set("inspircd.local-port", linkLocalPort);
    }

    public static boolean isLinkAutoconnect()
    {
        final boolean linkAutoconnect = true;
        return config.getBoolean("inspircd.auto-connect", linkAutoconnect);
    }

    public static void setLinkAutoconnect(final boolean linkAutoconnect)
    {
        config.set("inspircd.auto-connect", linkAutoconnect);
    }

    public static String getLinkName()
    {
        final String linkName = "irc.localhost";
        return config.getString("inspircd.link-name", linkName);
    }

    public static void setLinkName(final String linkName)
    {
        config.set("inspircd.link-name", linkName);
    }

    public static String getLinkConnectPassword()
    {
        final String linkConnectPassword = "test";
        return config.getString("inspircd.connect-password", linkConnectPassword);
    }

    public static void setLinkConnectPassword(final String linkConnectPassword)
    {
        config.set("inspircd.connect-password", linkConnectPassword);
    }

    public static String getLinkReceivePassword()
    {
        final String linkReceivePassword = "test";
        return config.getString("inspircd.receive-password", linkReceivePassword);
    }

    public static void setLinkReceivePassword(final String linkReceivePassword)
    {
        config.set("inspircd.receive-password", linkReceivePassword);
    }

    public static int getLinkPingInterval()
    {
        final int linkPingInterval = 60;
        return config.getInt("inspircd.ping-interval", linkPingInterval);
    }

    public static void setLinkPingInterval(final int linkPingInterval)
    {
        config.set("inspircd.ping-interval", linkPingInterval);
    }

    public static int getLinkTimeout()
    {
        final int linkTimeout = 180;
        return config.getInt("inspircd.timeout", linkTimeout);
    }

    public static void setLinkTimeout(final int linkTimeout)
    {
        config.set("inspircd.timeout", linkTimeout);
    }

    public static int getLinkDelay()
    {
        final int linkDelay = 60;
        return config.getInt("inspircd.connect-delay", linkDelay);
    }

    public static void setLinkDelay(final int linkDelay)
    {
        config.set("inspircd.connect-delay", linkDelay);
    }

    public static int getLinkServerID()
    {
        return config.getInt("inspircd.server-id", new Random().nextInt(900) + 100);
    }

    public static void setLinkServerID(final int linkServerID)
    {
        config.set("inspircd.server-id", linkServerID);
    }

    public static List<String> getKickCommands()
    {
        return config.getStringList("kick-commands");
    }

    public static void setKickCommands(final List<String> kickCommands)
    {
        config.set("kick-commands", kickCommands);
    }

    public static boolean isEnableRawSend()
    {
        final boolean enableRawSend = false;
        return config.getBoolean("enable-raw-send", enableRawSend);
    }

    public static void setEnableRawSend(final boolean enableRawSend)
    {
        config.set("enable-raw-send", enableRawSend);
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
