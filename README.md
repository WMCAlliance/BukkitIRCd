BukkitIRCd
=============

This plugin was [originally](http://dev.bukkit.org/server-mods/bukkitircd) by Jdbye. As he hasn't touched it in months, we decided to make some edits of our own.

What we are trying to do is continue the work of Jdbye. We are starting off with a few minor fixes that we've noticed are needed for this plugin to continue functioning.

If you know how to code in Java, then by all means take a look at our [Issues](https://github.com/nathanblaney/BukkitIRCd/issues) page and submit a Pull Request with code additions as required. We're going to need all the support and teamwork we can muster.

### Plugin Developers

* Mu5tank05 (Nathan)
* WizardCM (Matt G)

If you'd like to join the team, let us know and we'll happily add you.

### Extra Notes

This development is mainly for the benifit of the [Worldwide Minecraft Alliance](http://wma.im), an Australian multi-feature server hosted in Sydney, Australia. However, if you'd like to use the plugin on your own server, then by all means go ahead. There currently are no compiled builds of this version, so you will need to compile it yourself.

### Required for Compiling

After looking at the [.classpath](https://github.com/nathanblaney/BukkitIRCd/blob/master/.classpath), it is clear the following files are required. We will not upload them ourselves as they may need to be updated, or some people may prefer to get them from the original source rather than a third party. When compiling, it is imperitive that it is compiled against Java 1.6, and not 1.7, as it could either cause the build to fail OR cause the plugin to not work on servers still running on Java 1.6 (although they should really update to 1.7).

* [Bukkit](http://dl.bukkit.org/downloads/bukkit/) (not to be confused with [CraftBukkit](http://dl.bukkit.org/downloads/craftbukkit/), the server wrapper)
* [Dynmap API](http://dev.bukkit.org/server-mods/dynmap/files/82-dynmap-api-v1-1/)
* Permissions (two notes here, we don't have a download available, and we are going to try and rid ourselves of this requirement, as it is oudated and must make way for SuperPerms)