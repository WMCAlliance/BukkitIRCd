package com.Jdbye.BukkitIRCd.Configuration;

import com.Jdbye.BukkitIRCd.BukkitIRCdPlugin;
import com.Jdbye.BukkitIRCd.IRCd;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MOTD extends JavaPlugin {

    public static void enableMOTD() {
	if (!(new File(BukkitIRCdPlugin.thePlugin.getDataFolder(), "motd.txt")).exists()) {
	    saveDefaultMOTD(BukkitIRCdPlugin.thePlugin.getDataFolder(), "motd.txt");
	    BukkitIRCdPlugin.log.info("[BukkitIRCd] Default MOTD file created." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin199." : ""));
	}
    }

    // Set up the MOTD for the standalone BukkitIRCd server
    public static void loadMOTD() {
	File motdFile = new File(BukkitIRCdPlugin.thePlugin.getDataFolder(), "motd.txt");

	IRCd.MOTD.clear();

	try {
	    // use buffering, reading one line at a time
	    // FileReader always assumes default encoding is OK!
	    BufferedReader input = new BufferedReader(new FileReader(motdFile));
	    try {
		String line = null; // not declared within while loop
				/*
		 * readLine is a bit quirky : it returns the content of a line
		 * MINUS the newline. it returns null only for the END of the
		 * stream. it returns an empty String if two newlines appear in
		 * a row.
		 */
		while ((line = input.readLine()) != null) {
		    IRCd.MOTD.add(line);
		}
	    } finally {
		input.close();
		BukkitIRCdPlugin.log.info("[BukkitIRCd] Loaded MOTD file." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin516." : ""));
	    }
	} catch (Exception e) {
	    BukkitIRCdPlugin.log.info("[BukkitIRCd] Failed to load MOTD file: " + e.toString());
	}
    }

    // If a motd is not found, save it
    private static void saveDefaultMOTD(File dataFolder, String fileName) {
	BukkitIRCdPlugin.log.info("[BukkitIRCd] MOTD file not found, creating new one." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin616." : ""));
	dataFolder.mkdirs();

	File motdFile = new File(dataFolder, fileName);
	try {
	    if (!motdFile.createNewFile()) {
		throw new IOException("Failed file creation.");
	    }
	} catch (IOException e) {
	    BukkitIRCdPlugin.log.warning("[BukkitIRCd] Could not create MOTD file!" + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin627." : ""));
	}

	writeMOTD(motdFile);
    }

    private static void writeMOTD(File motdFile) {
	OutputStreamWriter fileWriter = null;
	BufferedWriter bufferWriter = null;
	try {
	    if (!motdFile.exists()) {
		motdFile.createNewFile();
	    }

	    fileWriter = new OutputStreamWriter(new FileOutputStream(motdFile),
		    "UTF8");
	    bufferWriter = new BufferedWriter(fileWriter);
	    // E M d H:m:s
	    Date curDate = new Date();
	    SimpleDateFormat dateFormat = new SimpleDateFormat(
		    "EEE MMM dd HH:mm:ss");

	    bufferWriter
		    .append("Last changed on " + dateFormat.format(curDate));
	    bufferWriter.newLine();
	    bufferWriter.append("");
	    bufferWriter.newLine();
	    bufferWriter.append("_________        __    __   .__        ___________  _____     _");
	    bufferWriter.newLine();
	    bufferWriter.append("\\______  \\___ __|  |  |  |  |__| __   |_   _| ___ \\/  __ \\   | |");
	    bufferWriter.newLine();
	    bufferWriter.append(" |   |_\\  \\  |  |  | _|  | _____/  |_   | | | |_/ /| /  \\/ __| |");
	    bufferWriter.newLine();
	    bufferWriter.append(" |    __ _/  |  \\  |/ /  |/ /  \\   __\\  | | |    / | |    / _` |");
	    bufferWriter.newLine();
	    bufferWriter.append(" |   |_/  \\  |  /    <|    <|  ||  |   _| |_| |\\ \\ | \\__/\\ (_| |");
	    bufferWriter.newLine();
	    bufferWriter.append(" |______  /____/|__|_ \\__|_ \\__||__|   \\___/\\_| \\_| \\____/\\__,_|");
	    bufferWriter.newLine();
	    bufferWriter.append("        \\/           \\/    \\/");
	    bufferWriter.newLine();
	    bufferWriter.append("");
	    bufferWriter.newLine();
	    bufferWriter.append("Welcome to " + Config.getIrcdServerName() + ", running " + BukkitIRCdPlugin.ircdVersion + ".");
	    bufferWriter.newLine();
	    bufferWriter.append("Enjoy your stay!");
	    bufferWriter.newLine();

	    bufferWriter.flush();
	    BukkitIRCdPlugin.log.info("[BukkitIRCd] Saved MOTD file." + (Config.isDebugModeEnabled() ? " Code BukkitIRCdPlugin674" : ""));
	} catch (IOException e) {
	    BukkitIRCdPlugin.log .warning("[BukkitIRCd] Caught exception while writing MOTD to file: ");
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
		BukkitIRCdPlugin.log.warning("[BukkitIRCd] IO Exception writing file: " + motdFile.getName());
	    }
	}
    }

}
