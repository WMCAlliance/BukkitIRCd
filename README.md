BukkitIRCd
=============

### Description 


This plugin was [originally](http://dev.bukkit.org/server-mods/bukkitircd) by Jdbye. As he hasn't touched it in months, we decided to make some edits of our own.

What we are trying to do is continue the work of Jdbye. We are starting off with a few minor fixes that we've noticed are needed for this plugin to continue functioning, then will continually add features and support for future versions of Bukkit.

If you know how to code in Java, then by all means take a look at our [Issues](https://github.com/WMCAlliance/BukkitIRCd/issues) page and submit a Pull Request with code additions as required. We're going to need all the support and teamwork we can get.

#### Developer Thanks

* peerau - uploaded BukkitIRCd to Github so we wouldn't have to
* SonarBeserk - fixed up all the old permissions for us, removed old class files
* jkcclemens - for letting us use the 'colorize' code that allowed us to change from section signs to ampersands in the messages.yml, among other things

### Extra Notes

This development is mainly for the benifit of the [Worldwide Minecraft Alliance](http://wma.im), an Australian multi-feature server hosted in Sydney, Australia. However, if you'd like to use the plugin on your own server, then by all means go ahead.

### Required for Compiling

Maven handles the dependencies of BukkitIRCd, including BukkitAPI and dynmap-api. In order to compile

* Install [Maven 3](http://maven.apache.org/download.html)
* Check out this repo and: `mvn clean install`

 
### Licensing

We will follow the same licensing as written by Jdbye;

"Source is included in the JAR file and is licensed under GPLv3 GNU General Public License. This means you are free to release modified versions as long as the source is included."
