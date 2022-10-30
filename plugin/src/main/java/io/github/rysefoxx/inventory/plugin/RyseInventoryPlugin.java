/*
 * MIT License
 *
 * Copyright (c) 2022. Rysefoxx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.github.rysefoxx.inventory.plugin;

import io.github.rysefoxx.inventory.plugin.content.InventoryProvider;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryContents;
import io.github.rysefoxx.inventory.plugin.pagination.InventoryManager;
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RyseInventoryPlugin extends JavaPlugin {

    private final InventoryManager inventoryManager = new InventoryManager(this);

    @Override
    public void onEnable() {
        inventoryManager.invoke();
        getLogger().info("");
        getLogger().info("§aThanks for using RyseInventory :)");
        getLogger().info("");
        getLogger().severe("RyseInventory is no longer supported as a plugin.");
        getLogger().severe("Please use our API which you can find on my Github account.");
        getLogger().severe(" -> https://github.com/Rysefoxx/RyseInventory");
        getLogger().info("");

        RyseInventory.builder()
                .title("a")
                .rows(6)
                .ignoredSlots(0)
                .ignoredSlot(1, event -> {
                    if(event.getInventory().getItem(0) == null
                            || event.getInventory().getItem(0).getType() == Material.AIR) {
                        event.setCancelled(true);
                        event.getWhoClicked().sendMessage("IN SLOT 0 MUSS REST EIN ITEM");
                    }
                })
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.addAdvancedSlot(0, event -> {
                            if (event.getCursor() != null
                                    && event.getCursor().getType() != Material.AIR) {
                                if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
                                    return;

                                event.setCancelled(true);
                            }
                        });

                    }
                })
                .build(this)
                .openAll();

//        Bukkit.getPluginManager().disablePlugin(this);
    }
}
