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

package io.github.rysefoxx;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.enums.Action;
import io.github.rysefoxx.other.Page;
import io.github.rysefoxx.pagination.InventoryContents;
import io.github.rysefoxx.pagination.InventoryManager;
import io.github.rysefoxx.pagination.Pagination;
import io.github.rysefoxx.pagination.RyseInventory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RyseInventoryPlugin extends JavaPlugin {

    private final InventoryManager manager = new InventoryManager(this);

    @Override
    public void onEnable() {
        manager.invoke();

        getLogger().info("");
        getLogger().info("§aThanks for using RyseInventory :)");
        getLogger().info("");
        getLogger().severe("RyseInventory is no longer supported as a plugin.");
        getLogger().severe("Please use our API which you can find on my Github account.");
        getLogger().severe(" -> https://github.com/Rysefoxx/RyseInventory");
        getLogger().info("");

//        Bukkit.getPluginManager().disablePlugin(this);

        int rows = 15;
        int allRows = rows + 1 % 6 == 0
                ? rows + 1
                : rows + 2;
        int divide = allRows / 6;
        int modRows = allRows % 6;
        int pagesCount = modRows == 0 ? divide : divide + 1;
        Page[] pages = new Page[pagesCount];
        for (int page = 0; page < pages.length; page++) {
            int finalRows = page >= pages.length - 1
                    ? modRows == 0
                    ? 6
                    : modRows
                    : 6;

            if (finalRows > 0)
                pages[page] = Page.of(page, finalRows);
        }

        RyseInventory.builder()
                .identifier("SIMPLE_ID")
                .title("Custom Inventory")
                .rows(pages)
                .fixedPageSize(pagesCount)
                .enableAction(() -> true, Action.MOVE_TO_OTHER_INVENTORY)
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        Pagination pagination = contents.pagination();

                        for (int i = 0; i < 50; i++) {
                            int finalI = i;
                            pagination.addItem(IntelligentItem.of(new ItemStack(Material.MAGMA_CREAM),
                                    event -> Bukkit.broadcastMessage("§aClicked on item " + finalI)));
                        }

                        int size = pagination.inventory().size(contents);

                        SlotIterator iterator = SlotIterator.builder()
                                .startPosition(0)
                                .endPosition(size - 9)
                                .type(SlotIterator.SlotIteratorType.HORIZONTAL)
                                .build();
                        pagination.iterator(iterator);

                        int fillerStart = size - 10;

                        contents.fillArea(size - 9, size - 1, IntelligentItem.empty(new ItemStack(Material.STAINED_GLASS_PANE)));
                        contents.set(fillerStart + 5, IntelligentItem.empty(new ItemStack(Material.BOOK)));

                        if (!pagination.isFirst())
                            contents.set(fillerStart + 1, IntelligentItem.of(new ItemStack(Material.ARROW),
                                    event -> pagination.inventory().open(player, pagination.previous().page())));

                        if (!pagination.isLast() && size == 54)
                            contents.set(fillerStart + 9, IntelligentItem.of(new ItemStack(Material.ARROW),
                                    event -> pagination.inventory().open(player, pagination.next().page())));
                    }
                })
                .build(this).openAll();
    }
}
