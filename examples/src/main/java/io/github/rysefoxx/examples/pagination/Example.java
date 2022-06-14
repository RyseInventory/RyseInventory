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

package io.github.rysefoxx.examples.pagination;

import io.github.rysefoxx.SlotIterator;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.pagination.InventoryContents;
import io.github.rysefoxx.pagination.InventoryManager;
import io.github.rysefoxx.pagination.Pagination;
import io.github.rysefoxx.pagination.RyseInventory;
import io.github.rysefoxx.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

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

    private void paginationInventory() {
        RyseInventory.builder()
                .manager(this.inventoryManager)
                .title("This is a paginated inventory")
                .rows(6)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        Pagination pagination = contents.pagination();
                        pagination.setItemsPerPage(10);
                        pagination.iterator(SlotIterator.builder().slot(2, 2).type(SlotIterator.SlotIteratorType.HORIZONTAL).blackList(Arrays.asList(25, 26, 27, 28)).build());

                        contents.set(5, 3, IntelligentItem.of(new ItemBuilder(Material.ARROW).amount((pagination.isFirst() ? 1 : pagination.page() - 1)).displayName(pagination.isFirst() ? "§c§oThis is the first page" : "§ePage §8⇒ §9" + pagination.copy().previous().page()).build(), event -> {
                            if (pagination.isFirst()) {
                                player.sendMessage("§c§oYou are already on the first page.");
                                return;
                            }

                            RyseInventory currentInventory = pagination.inventory();
                            currentInventory.open(player, pagination.previous().page());
                        }));

                        for (int i = 0; i < 13; i++) {
                            pagination.addItem(IntelligentItem.of(new ItemStack(Material.STONE), event -> {
                                event.getWhoClicked().sendMessage("You clicked on a stone!");
                                event.getWhoClicked().sendMessage("Stone on slot " + event.getSlot());
                            }));
                        }

                        int page = pagination.copy().next().page();
                        contents.set(5, 5, IntelligentItem.of(new ItemBuilder(Material.ARROW).amount((pagination.isLast() ? 1 : page)).displayName((!pagination.isLast() ? "§ePage §8⇒ §9" + page : "§c§oThis is the last page")).build(), event -> {
                            if (pagination.isLast()) {
                                player.sendMessage("§c§oYou are already on the last page.");
                                return;
                            }

                            RyseInventory currentInventory = pagination.inventory();
                            currentInventory.open(player, pagination.next().page());
                        }));
                    }
                })
                .build(this);
    }
}
