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

package io.github.rysefoxx.examples.basic;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.pagination.InventoryContents;
import io.github.rysefoxx.pagination.InventoryManager;
import io.github.rysefoxx.pagination.RyseInventory;
import io.github.rysefoxx.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/10/2022
 */
public class Example extends JavaPlugin {

    private InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        this.inventoryManager = new InventoryManager(this);
        this.inventoryManager.invoke();
    }

    private void basicInventory() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("This is a basic inventory")
                .rows(3)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        contents.set(0, IntelligentItem.empty(new ItemBuilder(Material.STONE).displayName("I am a stone with which nothing happens.").build()));
                        contents.set(1, IntelligentItem.ignored(new ItemBuilder(Material.STONE).displayName("I am a stone that can be taken out of the inventory.").build()));
                        contents.set(2, IntelligentItem.of(new ItemBuilder(Material.STONE).displayName("I am a stone where something happens when you click on me.").build(), event -> {
                            event.getWhoClicked().sendMessage("You clicked on a stone!");
                            event.getWhoClicked().sendMessage("Goobye!");
                        }));
                    }
                })
                .build(this);
    }

}
