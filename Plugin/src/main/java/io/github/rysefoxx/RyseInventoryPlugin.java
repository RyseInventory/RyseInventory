package io.github.rysefoxx;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RyseInventoryPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("");
        getLogger().info("§aThanks for using RyseInventory :)");
        getLogger().info("");
        getLogger().severe("RyseInventory is no longer supported as a plugin.");
        getLogger().severe("Please use our API which you can find on my Github account.");
        getLogger().severe(" -> https://github.com/Rysefoxx/RyseInventory");
        getLogger().info("");

        Bukkit.getPluginManager().disablePlugin(this);
    }
}
