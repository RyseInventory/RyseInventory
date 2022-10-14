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
import io.github.rysefoxx.enums.DisabledEvents;
import io.github.rysefoxx.other.EventCreator;
import io.github.rysefoxx.other.Page;
import io.github.rysefoxx.pagination.InventoryContents;
import io.github.rysefoxx.pagination.InventoryManager;
import io.github.rysefoxx.pagination.Pagination;
import io.github.rysefoxx.pagination.RyseInventory;
import io.github.rysefoxx.pattern.SlotIteratorPattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class RyseInventoryPlugin extends JavaPlugin implements CommandExecutor {

    private final InventoryManager manager = new InventoryManager(this);

    @Override
    public void onEnable() {
        manager.invoke();
        getCommand("a").setExecutor(this);

        getLogger().info("");
        getLogger().info("§aThanks for using RyseInventory :)");
        getLogger().info("");
        getLogger().severe("RyseInventory is no longer supported as a plugin.");
        getLogger().severe("Please use our API which you can find on my Github account.");
        getLogger().severe(" -> https://github.com/Rysefoxx/RyseInventory");
        getLogger().info("");

//        Bukkit.getPluginManager().disablePlugin(this);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

//        RyseInventory.builder()
//                .title("a")
//                .rows(6)
//                .enableAction(Action.MOVE_TO_OTHER_INVENTORY)
//                .ignoredSlots(IntStream.range(0,53).toArray())
//                .listener(new EventCreator<>(InventoryClickEvent.class, new Consumer<InventoryClickEvent>() {
//                    @Override
//                    public void accept(InventoryClickEvent event) {
//                        List<Material> set = Arrays.asList(Material.GLASS, Material.GRASS);
//                        Inventory clicked = event.getClickedInventory();
//                        if (clicked == null) return;
//
//                        InventoryAction action = event.getAction();
//                        switch (action) {
//                            case PLACE_ALL:
//                            case PLACE_ONE:
//                            case PLACE_SOME: {
//                                if (clicked.getType() == InventoryType.CHEST) {
//                                    if (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
//                                        if (set.contains(event.getCursor().getType())) {
//                                            System.out.println("cancel");
//                                            event.setCancelled(true);
//                                        }
//                                }
//                            }
//                            case MOVE_TO_OTHER_INVENTORY: {
//                                if (clicked.getType() == InventoryType.PLAYER) {
//                                    if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
//                                        if (set.contains(event.getCurrentItem().getType()) || event.getSlot() == 0) {
//                                            System.out.println("cancel 2");
//                                            event.setResult(Event.Result.DENY);
//                                            event.setCancelled(true);
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }))
//                .provider(new InventoryProvider() {
//                    @Override
//                    public void init(Player player, InventoryContents contents) {
//
//                    }
//                })
//                .build(this)
//                .openAll();

        RyseInventory.builder()
                .identifier("some")
                .title("title")
                .rows(6)
                .ignoreManualItems()
                .ignoreEvents(DisabledEvents.INVENTORY_DRAG)
//                .fixedPageSize(1)
//                .listener(new EventCreator<>(InventoryClickEvent.class, event -> {
//                    Inventory clicked = event.getClickedInventory();
//                    if (clicked != null && clicked.getType() != InventoryType.PLAYER
//                            && event.getAction().name().startsWith("PLACE")) {
//                        event.setCancelled(true);
//                    }
//                }))
                .provider(new InventoryProvider() {
                    @Override
                    public void init(Player player, InventoryContents contents) {
                        Pagination pagination = contents.pagination();

                        pagination.setItemsPerPage(5);


                        contents.set(5, 3, IntelligentItem.of(new ItemBuilder(Material.ARROW).amount((pagination.isFirst() ? 1 : pagination.page() - 1)).displayName(pagination.isFirst() ? "§c§oThis is the first page" : "§ePage §8⇒ §9" + pagination.newInstance(pagination).previous().page()).build(), event -> {
                            if (pagination.isFirst()) {
                                player.sendMessage("§c§oYou are already on the first page.");
                                return;
                            }

                            RyseInventory currentInventory = pagination.inventory();
                            currentInventory.open(player, pagination.previous().page());
                        }));

                        for (int i = 0; i < 50; i++) {
                            pagination.addItem(IntelligentItem.empty(new ItemStack(Material.MOB_SPAWNER)));
                        }

                        pagination.iterator(slotIterator());

                        int page = pagination.newInstance(pagination).next().page();
                        contents.set(5, 5, IntelligentItem.of(new ItemBuilder(Material.ARROW).amount((pagination.isLast() ? 1 : page)).displayName((!pagination.isLast() ? "§ePage §8⇒ §9" + page : "§c§oThis is the last page")).build(), event -> {
                            if (pagination.isLast()) {
                                player.sendMessage("§c§oYou are already on the last page.");
                                return;
                            }

                            RyseInventory currentInventory = pagination.inventory();
                            currentInventory.open(player, pagination.next().page());
                        }));
//
//                        Pagination pagination = contents.pagination();
//                        List<ItemStack> items = new ArrayList<>();
//                        items.add(new ItemBuilder(Material.GRASS).build());
//
//                        pagination.setItems(items
//                                .stream()
//                                .filter(Objects::nonNull)
//                                .map(true ? IntelligentItem::ignored : IntelligentItem::empty)
//                                .collect(Collectors.toList()));
//
//                        int size = pagination.inventory().size(contents);
//
//                        SlotIterator iterator = SlotIterator.builder()
//                                .startPosition(0)
//                                .endPosition(size - 9)
//                                .type(SlotIterator.SlotIteratorType.HORIZONTAL)
//                                .build();
//                        pagination.iterator(iterator);
//
//                        int fillerStart = size - 10;
//
//                        if (true) {
//                            contents.removeIgnoredSlots(IntStream.rangeClosed(0, 54).toArray());
//                            contents.addIgnoredSlots(IntStream.rangeClosed(0, fillerStart).toArray());
//                        }
//
//                        contents.fillArea(size - 9, size - 1, IntelligentItem.empty(new ItemStack(Material.STAINED_GLASS_PANE)));
//                        contents.set(fillerStart + 4, IntelligentItem.empty(new ItemStack(Material.BOOK)));
                    }
                })
                .build(this).openAll();
        return false;
    }
    private SlotIterator slotIterator() {
        SlotIteratorPattern slotIteratorPattern = SlotIteratorPattern
                .builder()
                .define("XXXXXXXXX",
                        "XOOOOOOXX",
                        "XOOOOOOXX",
                        "XOOOOOOXX",
                        "XOOOOOOXX",
                        "XXXXXXXXX")
                .attach('O')
                .buildPattern();

        return SlotIterator
                .builder()
                .withPattern(slotIteratorPattern)
                .build();
    }
}
