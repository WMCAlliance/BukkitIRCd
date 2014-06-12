package com.Jdbye.BukkitIRCd.configuration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;
import com.Jdbye.BukkitIRCd.IrcBan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Bans {

    public static void enableBans() {
	if (!(new File(BukkitIRCdPlugin.thePlugin.getDataFolder(), "bans.txt"))
		.exists()) {
	    if (writeBans()) {
		BukkitIRCdPlugin.log
			.info("[BukkitIRCd] Blank bans file created." +
				(Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin204." :
				""));
	    } else {
		BukkitIRCdPlugin.log
			.warning("[BukkitIRCd] Failed to create bans file." +
				(Config.isDebugModeEnabled() ? " Error Code BukkitIRCdPlugin205." :
				""));
	    }
	}
    }

    public static void loadBans() {
	File bansFile = new File(BukkitIRCdPlugin.thePlugin.getDataFolder(),
		"bans.txt");

	IRCd.ircBans.clear();

	try {
	    // use buffering, reading one line at a time
	    // FileReader always assumes default encoding is OK!
	    BufferedReader input = new BufferedReader(new FileReader(bansFile));
	    try {
		String line = null; // not declared within while loop
				/*
		 * readLine is a bit quirky : it returns the content of a line
		 * MINUS the newline. it returns null only for the END of the
		 * stream. it returns an empty String if two newlines appear in
		 * a row.
		 */
		while ((line = input.readLine()) != null) {
		    String[] split = line.split(",");
		    if (!line.trim().startsWith("#")) {
			try {
			    IRCd.ircBans.add(new IrcBan(split[0], split[1],
				    Long.parseLong(split[2])));
			} catch (NumberFormatException e) {
			    BukkitIRCdPlugin.log
				    .warning("[BukkitIRCd] Invalid ban: " +
					    line);
			}
		    }
		}
	    } finally {
		input.close();
		BukkitIRCdPlugin.log
			.info("[BukkitIRCd] Loaded bans file." +
				(Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin551." :
				""));
	    }
	} catch (Exception e) {
	    BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load bans file: " +
		    e.toString());
	}
    }

    // Write the bans file
    public static boolean writeBans() {
	File bansFile = new File(BukkitIRCdPlugin.thePlugin.getDataFolder(),
		"bans.txt");

	boolean result = false;
	OutputStreamWriter fileWriter = null;
	BufferedWriter bufferWriter = null;
	try {
	    if (!bansFile.exists()) {
		bansFile.createNewFile();
	    }

	    fileWriter = new OutputStreamWriter(new FileOutputStream(bansFile),
		    "UTF8");
	    bufferWriter = new BufferedWriter(fileWriter);

	    bufferWriter.append("# wildcard hostmask,banned by,time");
	    bufferWriter.newLine();

	    synchronized (IRCd.csIrcBans) {
		for (IrcBan ban : IRCd.ircBans) {
		    bufferWriter.append(ban.fullHost + "," + ban.bannedBy + "," +
			    ban.banTime);
		    bufferWriter.newLine();
		}
	    }

	    bufferWriter.flush();
	    BukkitIRCdPlugin.log
		    .info("[BukkitIRCd] Saved bans file." +
			    (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin585." :
			    ""));
	    result = true;
	} catch (IOException e) {
	    BukkitIRCdPlugin.log
		    .warning("[BukkitIRCd] Caught exception while writing bans to file: ");
	    e.printStackTrace();
	} finally {
	    try {
		if (bufferWriter != null) {
		    bufferWriter.flush();
		    bufferWriter.close();
		}

		if (fileWriter != null) {
		    fileWriter.close();
		}
	    } catch (IOException e) {
		BukkitIRCdPlugin.log
			.warning("[BukkitIRCd] IO Exception writing file: " +
				bansFile.getName());
	    }
	}
	return result;
    }
}
