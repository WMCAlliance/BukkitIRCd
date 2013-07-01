![BukkitIRCd](https://raw.github.com/WMCAlliance/BukkitIRCd/master/bdev/bukkitircd-logo.png "BukkitIRCd")
=============

### Description 
BukkitIRCd is a comprehensive IRC plugin for Bukkit. It allows ingame server chat to be accessed in a standalone IRC channel, or to be linked to a InspIRCd network. Each Bukkit user is represented as his/her own IRC user and can have access to certain IRC privileges. As well, the plugin allows for server control through a IRC channel.

This plugin was [originally](http://dev.bukkit.org/server-mods/bukkitircd) by [Jdbye](http://dev.bukkit.org/profiles/Jdbye/). As he hasn't touched it in months, we decided to make some edits of our own.

What we are trying to do is continue the work of Jdbye. We are starting off with a few minor fixes that we've noticed are needed for this plugin to continue functioning, then will continually add features and support for future versions of Bukkit.

If you know how to code in Java, then by all means take a look at our [Issues](https://github.com/WMCAlliance/BukkitIRCd/issues) page and submit a Pull Request (to the development branch) with code additions as required. We're going to need all the support and teamwork we can get.

To download builds made from the development branch, you can get them at [our Jenkins](http://netbook.home.wizardcm.com:8080/job/BukkitIRCd/). Remember, these can be unstable, though we try to keep them as stable as possible.

If you need to discuss anything with the team, don't hesitate to [join us on IRC](http://widget00.mibbit.com/?server=irc.echelon4.net&amp;channel=%23minecraft-dev) via irc.wma.im #minecraft-dev

#### Developer Thanks

* [peerau](https://github.com/peerau) - uploaded BukkitIRCd to Github so we wouldn't have to
* [SonarBeserk](https://github.com/SonarBeserk/) - fixed up all the old permissions for us, removed old class files
* [jkcclemens](https://github.com/jkcclemens) - for letting us use the 'colorize' code that allowed us to change from section signs to ampersands in the messages.yml, among other things
* [ron975](https://github.com/ron975) - has contributed incredible amounts of code, and continues to do so - huge thanks to him

### Extra Notes

This development is mainly for the benefit of the [Worldwide Minecraft Alliance](http://wma.im), an Australian multi-feature server hosted in Melbourne, Australia. However, if you'd like to use the plugin on your own server, then by all means go ahead.

### Compiling

Maven handles the dependencies of BukkitIRCd, including BukkitAPI and dynmap-api. In order to compile

* Install [Maven 3](http://maven.apache.org/download.html)
* Check out this repo and run: mvn clean install

#### Pull requests
Although we love your input we do have a few dead simple rules.
* We ask you do all work in the 'development' branch
* We ask that you clearly state what the changes are in your request
* Make sure your local branch is as up-to-date as possible before the merge
 
### Licensing
BukkitIRCd is licensed under the GNU General Public License Version 3. A copy of this license is available in this repository with the filename LICENSE.md

### Plugin Statistics
![PluginMetrics](http://api.mcstats.org/signature/BukkitIRCd.png)

More statistics available at [MCStats.org](http://mcstats.org/plugin/BukkitIRCd)
