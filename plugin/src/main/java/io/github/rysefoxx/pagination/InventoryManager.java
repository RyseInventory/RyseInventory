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

package io.github.rysefoxx.pagination;

import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.enums.*;
import io.github.rysefoxx.other.EventCreator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
@RequiredArgsConstructor
public class InventoryManager {

    private final Plugin plugin;
    @Getter(AccessLevel.PROTECTED)
    private boolean invoked = false;

    private final Set<IntelligentItem> items = new HashSet<>();
    private final HashMap<UUID, RyseInventory> inventories = new HashMap<>();
    private final HashMap<UUID, InventoryContents> content = new HashMap<>();
    private final HashMap<UUID, BukkitTask> updaterTask = new HashMap<>();
    private final HashMap<UUID, List<RyseInventory>> lastInventories = new HashMap<>();

    /**
     * Adds the IntelligentItem to the list if this item has an ID.
     *
     * @param item The item to add.
     * @throws NullPointerException If the item ID is null.
     */
    public void register(@NotNull final IntelligentItem item) throws NullPointerException {
        if (item.getId() == null) throw new NullPointerException("The item has no ID!");
        this.items.add(item);
    }

    /**
     * @param id The id of the item
     * @return Returns the first IntelligentItem that matches the ID. If no item is found, null is returned.
     */
    public @Nullable IntelligentItem getItemById(@NotNull Object id) {
        return this.items.stream().filter(item -> item.getId() == id).findFirst().orElse(null);
    }

    /**
     * @param id The id of the item
     * @return Returns all IntelligentItems that match the ID. If no item is found, an empty list is returned.
     */
    public @NotNull List<IntelligentItem> getAllItemsById(@NotNull Object id) {
        List<IntelligentItem> result = new ArrayList<>();
        for (IntelligentItem item : this.items) {
            if (item.getId() != id) continue;

            result.add(item);
        }
        return result;
    }

    /**
     * With this method you can get the inventory from the player.
     *
     * @param uuid The UUID of the player.
     * @return null if the player has no inventory open.
     */
    public @NotNull Optional<RyseInventory> getInventory(@NotNull UUID uuid) {
        if (!hasInventory(uuid)) return Optional.empty();
        return Optional.ofNullable(this.inventories.get(uuid));
    }

    /**
     * Get all players who have a certain inventory open
     *
     * @param inventory The inventory that is filtered by.
     * @return The list with all found players.
     */
    public @NotNull List<UUID> getOpenedPlayers(@NotNull RyseInventory inventory) {
        List<UUID> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<RyseInventory> optional = getInventory(player.getUniqueId());

            optional.ifPresent(savedInventory -> {
                if (!inventory.equals(savedInventory)) return;
                players.add(player.getUniqueId());
            });
        });
        return players;
    }

    /**
     * Get the last inventory that the player had open.
     *
     * @param uuid Player UUID
     * @return null if there is no final inventory.
     */
    public @NotNull Optional<RyseInventory> getLastInventory(@NotNull UUID uuid) {
        if (!this.lastInventories.containsKey(uuid)) return Optional.empty();
        if (this.lastInventories.get(uuid).isEmpty()) return Optional.empty();
        RyseInventory inventory = this.lastInventories.get(uuid).remove(this.lastInventories.get(uuid).size() - 1);
        inventory.setBackward();

        return Optional.of(inventory);
    }

    /**
     * With this method you can get the inventory from the inventory identifier.
     *
     * @param identifier The ID to identify
     * @return null if no inventory with the ID could be found.
     * <p>
     * Only works if the inventory has also been assigned an identifier.
     */
    public @NotNull Optional<RyseInventory> getInventory(@NotNull Object identifier) {
        return this.inventories.values().stream().filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier)).findFirst();
    }

    /**
     * With this method you can get the inventory content from the player.
     *
     * @param uuid The UUID of the player.
     * @return the player inventory content.
     */
    public @NotNull Optional<InventoryContents> getContents(@NotNull UUID uuid) {
        if (!this.content.containsKey(uuid)) return Optional.empty();
        return Optional.ofNullable(this.content.get(uuid));
    }

    /**
     * Registers the standard events
     */
    public void invoke() {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this.plugin);
        invoked = true;
    }

    /**
     * Returns true if the given UUID has an inventory.
     *
     * @param uuid The UUID of the player to check for.
     * @return A boolean value.
     */
    @Contract(pure = true)
    private boolean hasInventory(@NotNull UUID uuid) {
        return this.inventories.containsKey(uuid);
    }

    /**
     * Removes the inventory from the player
     *
     * @param uuid The UUID of the player to remove the inventory from.
     */
    protected void removeInventoryFromPlayer(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
        this.content.remove(uuid);
        BukkitTask task = this.updaterTask.remove(uuid);

        if (task != null)
            task.cancel();
    }

    /**
     * It removes the inventory of the player with the given UUID from the HashMap
     *
     * @param uuid The UUID of the player to remove the inventory of.
     */
    protected void removeInventory(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
    }

    /**
     * It puts the contents of the inventory into a HashMap
     *
     * @param uuid The UUID of the player who's inventory you want to set.
     * @param contents The InventoryContents object that you want to set.
     */
    protected void setContents(@NotNull UUID uuid, @NotNull InventoryContents contents) {
        this.content.put(uuid, contents);
    }

    /**
     * This function sets the inventory of a player.
     *
     * @param uuid The UUID of the player
     * @param inventory The inventory to set.
     */
    protected void setInventory(@NotNull UUID uuid,
                                @NotNull RyseInventory inventory) {
        this.inventories.put(uuid, inventory);
    }

    /**
     * It adds the player's current inventory to a list of inventories
     *
     * @param uuid The UUID of the player
     * @param inventory The inventory that the player is currently in.
     * @param newInventory The new inventory that the player is switching to.
     */
    protected void setLastInventory(@NotNull UUID uuid,
                                    @NotNull RyseInventory inventory,
                                    @NotNull RyseInventory newInventory) {
        List<RyseInventory> inventoryList = this.lastInventories.getOrDefault(uuid, new ArrayList<>());

        if (inventory.equals(newInventory)) return;

        inventoryList.add(inventory);

        this.lastInventories.put(uuid, inventoryList);
    }

    /**
     * It stops the update task for the specified player
     *
     * @param uuid The UUID of the player to stop updating.
     */
    protected void stopUpdate(@NotNull UUID uuid) {
        if (!this.updaterTask.containsKey(uuid)) return;
        BukkitTask task = this.updaterTask.remove(uuid);
        task.cancel();
    }

    /**
     * If the player has an inventory, and the inventory is the same as the one passed in, then update the inventory
     *
     * @param player The player who's inventory is being updated.
     * @param inventory The inventory that will be updated.
     */
    protected void invokeScheduler(@NotNull Player player,
                                   @NotNull RyseInventory inventory) {
        if (this.updaterTask.containsKey(player.getUniqueId())) return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!hasInventory(player.getUniqueId())) {
                    cancel();
                    return;
                }
                RyseInventory savedInventory = inventories.get(player.getUniqueId());
                if (savedInventory != inventory) {
                    cancel();
                    return;
                }
                savedInventory.getProvider().update(player, content.get(player.getUniqueId()));
            }
        }.runTaskTimer(this.plugin, inventory.getDelay(), inventory.getPeriod());
        this.updaterTask.put(player.getUniqueId(), task);
    }

    /**
     * It's a class that listens for events and cancels them if the player is viewing a RyseInventory
     */
    public class InventoryListener implements Listener {

        @Contract(pure = true)
        private boolean hasContents(@NotNull UUID uuid) {
            return content.containsKey(uuid);
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamage(@NotNull EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player)) return;
            Player player = (Player) event.getEntity();

            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (!mainInventory.getOptions().contains(InventoryOptions.NO_DAMAGE)) return;
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onFoodLevelChange(@NotNull FoodLevelChangeEvent event) {
            if (!(event.getEntity() instanceof Player)) return;
            Player player = (Player) event.getEntity();

            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (!mainInventory.getOptions().contains(InventoryOptions.NO_HUNGER)) return;
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerPickupItem(@NotNull PlayerPickupItemEvent event) {
            Player player = event.getPlayer();
            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (!mainInventory.getOptions().contains(InventoryOptions.NO_ITEM_PICKUP)) return;
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPotionSplash(@NotNull PotionSplashEvent event) {

            for (LivingEntity entity : event.getAffectedEntities()) {
                if (!(entity instanceof Player)) continue;
                Player player = (Player) entity;
                if (!hasInventory(player.getUniqueId()))
                    continue;

                RyseInventory mainInventory = inventories.get(player.getUniqueId());

                if (!mainInventory.getOptions().contains(InventoryOptions.NO_POTION_EFFECT)) continue;
                event.setCancelled(true);

            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockBreak(@NotNull BlockBreakEvent event) {
            Block block = event.getBlock();
            Location toCheck = block.getLocation().clone().add(0, 1, 0);

            List<Player> onBlock = new ArrayList<>();

            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                if (onlinePlayer.getLocation().getBlockX() == toCheck.getBlockX() &&
                        onlinePlayer.getLocation().getBlockY() == toCheck.getBlockY() &&
                        onlinePlayer.getLocation().getBlockZ() == toCheck.getBlockZ()) {
                    onBlock.add(onlinePlayer);
                }
            });

            if (!onBlock.isEmpty()) {
                onBlock.forEach(affectedPlayer -> {
                    if (!hasInventory(affectedPlayer.getUniqueId()))
                        return;

                    RyseInventory mainInventory = inventories.get(affectedPlayer.getUniqueId());

                    if (!mainInventory.getOptions().contains(InventoryOptions.NO_BLOCK_BREAK)) return;
                    event.setCancelled(true);
                });
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryClick(@NotNull InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack itemStack = event.getCurrentItem();

            if (!hasInventory(player.getUniqueId()))
                return;
            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (event.getClickedInventory() == null) {
                if (mainInventory.getCloseReasons().contains(CloseReason.CLICK_OUTSIDE))
                    player.closeInventory();
                return;
            }

            EventCreator<InventoryClickEvent> customEvent = (EventCreator<InventoryClickEvent>) mainInventory.getEvent(InventoryClickEvent.class);
            if (customEvent != null) {
                customEvent.accept(event);
            }

            List<DisabledInventoryClick> list = mainInventory.getIgnoreClickEvent();

            InventoryAction action = event.getAction();
            Inventory clickedInventory = event.getClickedInventory();
            Inventory bottomInventory = player.getOpenInventory().getBottomInventory();
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            int slot = event.getSlot();
            ClickType clickType = event.getClick();
            InventoryContents contents = content.get(player.getUniqueId());

            if (clickedInventory == bottomInventory && (!list.contains(DisabledInventoryClick.BOTTOM) && !list.contains(DisabledInventoryClick.BOTH))) {
                if (mainInventory.getCloseReasons().contains(CloseReason.CLICK_BOTTOM_INVENTORY)) {
                    mainInventory.close(player);
                    return;
                }

                if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    if (!mainInventory.getEnabledActions().contains(Action.MOVE_TO_OTHER_INVENTORY)) {
                        event.setCancelled(true);
                        return;
                    }

                    int[] data = checkForExistingItem(topInventory, itemStack, mainInventory);
                    int targetSlot = data[0];
                    int targetAmount = data[1];

                    Optional<IntelligentItem> itemOptional = contents.get(targetSlot);

                    if (cancelEventIfItemHasConsumer(event, mainInventory, targetSlot, itemOptional)) return;

                    if (adjustItemStackAmount(topInventory, event, mainInventory, contents, targetSlot, targetAmount))
                        return;

                    event.setCancelled(true);
                    adjustItemStackAmountToMaxStackSize(itemStack, mainInventory, topInventory, contents, targetSlot, targetAmount);
                    return;
                }

                if (action == InventoryAction.COLLECT_TO_CURSOR) {
                    event.setCancelled(true);
                    return;
                }

                if (action == InventoryAction.NOTHING && clickType != ClickType.MIDDLE)
                    event.setCancelled(true);

                return;
            }

            if (clickedInventory == topInventory) {
                if (!hasContents(player.getUniqueId()))
                    return;
                if (slot < 0 || (mainInventory.getInventoryOpenerType() == InventoryOpenerType.CHEST && slot > mainInventory.size(contents))) {
                    return;
                }

                SlideAnimation animation = mainInventory.getSlideAnimator();

                if (animation != null && mainInventory.activeSlideAnimatorTasks() > 0 && animation.isBlockClickEvent()) {
                    event.setCancelled(true);
                    return;
                }

                if (!list.contains(DisabledInventoryClick.TOP) && !list.contains(DisabledInventoryClick.BOTH)) {
                    if (event.getClick() == ClickType.DOUBLE_CLICK
                            && !mainInventory.getEnabledActions().contains(Action.DOUBLE_CLICK)) {
                        event.setCancelled(true);
                        return;
                    }
                    if (!mainInventory.getIgnoredSlots().contains(slot))
                        event.setCancelled(true);
                }

                if (mainInventory.getIgnoredSlots().contains(slot)) {
                    modifyItemStackAmountViaCursor(event, itemStack, mainInventory, slot, clickType, contents);
                    subtractItemStackAmountWhenRightClick(event, itemStack, mainInventory, slot, clickType, contents);
                }

                Optional<IntelligentItem> optional = contents.get(slot);

                if (!optional.isPresent() && mainInventory.getCloseReasons().contains(CloseReason.CLICK_EMPTY_SLOT)) {
                    event.setCancelled(true);
                    mainInventory.close(player);
                    return;
                }

                optional.ifPresent(item -> {
                    if (item.getConsumer() == null) {
                        event.setCancelled(false);
                        return;
                    }

                    if (list.contains(DisabledInventoryClick.TOP) || list.contains(DisabledInventoryClick.BOTH))
                        event.setCancelled(true);

                    if (!item.isCanClick()) {
                        item.getError().cantClick(player, item);
                        return;
                    }
                    item.getConsumer().accept(event);
                    player.updateInventory();
                });
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryDrag(@NotNull InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            if (!hasInventory(player.getUniqueId()))
                return;

            Inventory topInventory = player.getOpenInventory().getTopInventory();
            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            EventCreator<InventoryDragEvent> customEvent = (EventCreator<InventoryDragEvent>) mainInventory.getEvent(InventoryDragEvent.class);
            if (customEvent != null) {
                customEvent.accept(event);
                return;
            }

            if (mainInventory.getDisabledEvents().contains(DisabledEvents.INVENTORY_DRAG))
                return;

            event.getRawSlots().forEach(integer -> {
                if (integer >= topInventory.getSize()) return;
                event.setCancelled(true);
            });
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryClose(@NotNull InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (!mainInventory.isCloseAble()) {
                Bukkit.getScheduler().runTask(plugin, () -> mainInventory.open(player));
                return;
            }


            EventCreator<InventoryCloseEvent> customEvent = (EventCreator<InventoryCloseEvent>) mainInventory.getEvent(InventoryCloseEvent.class);
            if (customEvent != null) {
                customEvent.accept(event);
                mainInventory.clearData(player);
                return;
            }

            mainInventory.getProvider().close(player, mainInventory);
            mainInventory.close(player);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            EventCreator<PlayerQuitEvent> customEvent = (EventCreator<PlayerQuitEvent>) mainInventory.getEvent(PlayerQuitEvent.class);
            if (customEvent == null) return;

            customEvent.accept(event);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPluginDisable(@NotNull PluginDisableEvent event) {
            Plugin disabledPlugin = event.getPlugin();

            if (disabledPlugin != plugin) return;


            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!hasInventory(player.getUniqueId())) return;

                RyseInventory inventory = inventories.get(player.getUniqueId());
                inventory.close(player);
            });

        }

        /**
         * This function checks if an item exists in an inventory, and if it does, it returns the slot number and the
         * amount of the item in that slot.
         *
         * @param topInventory The inventory to check for the item in.
         * @param itemStack    The item you want to check for.
         * @return An array of integers.
         */
        private int @NotNull [] checkForExistingItem(@NotNull Inventory topInventory,
                                                     @Nullable ItemStack itemStack,
                                                     @NotNull RyseInventory mainInventory) {
            int[] data = new int[2];
            data[0] = -1;
            for (int i = 0; i < topInventory.getSize(); i++) {
                ItemStack inventoryItem = topInventory.getItem(i);
                if (!mainInventory.getIgnoredSlots().contains(i)) continue;

                if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                    data[0] = i;
                    break;
                }

                if (inventoryItem.isSimilar(itemStack) && inventoryItem.getAmount() < inventoryItem.getMaxStackSize()) {
                    data[0] = i;
                    data[1] = inventoryItem.getAmount();
                    break;
                }
            }
            return data;
        }

        /**
         * If the item in the target slot has a consumer, cancel the event
         *
         * @param event         The InventoryClickEvent that was called.
         * @param mainInventory The inventory that the player is currently viewing.
         * @param targetSlot    The slot that the player is trying to click on.
         * @param itemOptional  The item that is being clicked on.
         * @return A boolean value.
         */
        private boolean cancelEventIfItemHasConsumer(@NotNull InventoryClickEvent event,
                                                     @NotNull RyseInventory mainInventory,
                                                     int targetSlot,
                                                     Optional<IntelligentItem> itemOptional) {
            if (!mainInventory.getIgnoredSlots().contains(targetSlot)) {
                if (itemOptional.isPresent() && itemOptional.get().getConsumer() != null) return true;

                event.setCancelled(true);
                return true;
            }
            return false;
        }

        /**
         * If the amount of the item in the target slot plus the amount of the item in the cursor slot is less than or
         * equal to the max stack size of the item, then set the amount of the item in the target slot to the amount of the
         * item in the target slot plus the amount of the item in the cursor slot
         *
         * @param mainInventory The RyseInventory instance
         * @param contents      The InventoryContents object that contains all the information about the inventory.
         * @param targetSlot    The slot in the inventory that the item is being moved to.
         * @param targetAmount  The amount of the item that you want to add to the target slot.
         * @return A boolean value.
         */
        private boolean adjustItemStackAmount(@NotNull Inventory topInventory,
                                              InventoryClickEvent event,
                                              RyseInventory mainInventory,
                                              InventoryContents contents,
                                              int targetSlot, int targetAmount) {
            ItemStack itemStack = event.getCurrentItem();
            if (itemStack.getAmount() + targetAmount <= itemStack.getMaxStackSize()) {
                event.setCancelled(true);

                ItemStack topItem = topInventory.getItem(targetSlot);

                if (topItem != null && topItem.getType() != Material.AIR)
                    if (!itemStack.isSimilar(topInventory.getItem(targetSlot)))
                        return true;


                event.setCurrentItem(null);

                ItemStack finalItemStack = itemStack.clone();
                finalItemStack.setAmount(itemStack.getAmount() + targetAmount);

                topInventory.setItem(targetSlot, finalItemStack);

                if (!mainInventory.isIgnoreManualItems())
                    contents.pagination().setItem(
                            targetSlot,
                            contents.pagination().page() - 1,
                            IntelligentItem.ignored(finalItemStack),
                            true);
                return true;
            }
            return false;
        }


        /**
         * If the item stack is greater than the max stack size, set the item stack to the max stack size and subtract the
         * max stack size from the original item stack
         *
         * @param itemStack     The itemstack that is being moved
         * @param mainInventory The RyseInventory instance
         * @param topInventory  The inventory that the player is currently viewing.
         * @param contents      The InventoryContents object that is passed to the InventoryListener.
         * @param targetSlot    The slot in the top inventory that the item is being moved to.
         * @param targetAmount  The amount of items to be moved to the target slot.
         */
        private void adjustItemStackAmountToMaxStackSize(@NotNull ItemStack itemStack,
                                                         @NotNull RyseInventory mainInventory,
                                                         @NotNull Inventory topInventory,
                                                         InventoryContents contents,
                                                         int targetSlot, int targetAmount) {
            ItemStack toSet = new ItemStack(itemStack.getType(), itemStack.getMaxStackSize());
            topInventory.setItem(targetSlot, toSet);

            if (!mainInventory.isIgnoreManualItems())
                contents.pagination().setItem(
                        targetSlot,
                        contents.pagination().page() - 1,
                        IntelligentItem.ignored(toSet),
                        true);

            itemStack.setAmount(itemStack.getAmount() - targetAmount);
        }


        /**
         * If the cursor is not empty, and the item in the slot is similar to the cursor, then set the amount of the item
         * in the slot to the amount of the item in the slot plus the amount of the cursor
         *
         * @param event         The InventoryClickEvent that was fired.
         * @param itemStack     The itemstack in the slot that was clicked.
         * @param mainInventory The inventory that the player is currently viewing.
         * @param slot          The slot that was clicked
         * @param clickType     The type of click that was performed.
         * @param contents      The InventoryContents object that contains all the information about the inventory.
         */
        private void modifyItemStackAmountViaCursor(@NotNull InventoryClickEvent event,
                                                    ItemStack itemStack,
                                                    RyseInventory mainInventory,
                                                    int slot,
                                                    ClickType clickType,
                                                    InventoryContents contents) {
            if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) return;

            ItemStack cursor = event.getCursor().clone();
            if (clickType == ClickType.RIGHT) {
                cursor.setAmount(itemStack != null && itemStack.isSimilar(cursor)
                        ? itemStack.getAmount() + 1
                        : 1);
            } else {
                cursor.setAmount(itemStack != null && itemStack.isSimilar(cursor)
                        ? itemStack.getAmount() + cursor.getAmount()
                        : cursor.getAmount());
            }

            if (mainInventory.isIgnoreManualItems())
                return;

            contents.pagination().setItem(
                    slot,
                    contents.pagination().page() - 1,
                    IntelligentItem.ignored(cursor),
                    true);
        }


        /**
         * When the player right clicks on an item, the item's amount is halved. Otherwise the item is removed.
         *
         * @param event         The InventoryClickEvent that was called.
         * @param itemStack     The itemstack that was clicked
         * @param mainInventory The inventory that is being opened.
         * @param slot          The slot that was clicked
         * @param clickType     The type of click that was performed.
         * @param contents      The InventoryContents object that contains all the information about the inventory.
         */
        private void subtractItemStackAmountWhenRightClick(@NotNull InventoryClickEvent event,
                                                           ItemStack itemStack,
                                                           RyseInventory mainInventory,
                                                           int slot,
                                                           ClickType clickType,
                                                           InventoryContents contents) {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
                return;

            if (clickType == ClickType.RIGHT) {
                if (mainInventory.isIgnoreManualItems()) return;
                if (itemStack == null) return;

                ItemStack finalItemStack = itemStack.clone();
                finalItemStack.setAmount(itemStack.getAmount() / 2);
                contents.pagination().setItem(
                        slot,
                        contents.pagination().page() - 1,
                        IntelligentItem.ignored(finalItemStack),
                        true);
                return;
            }

            if (itemStack != null && itemStack.getType() != Material.AIR)
                contents.pagination().remove(slot);
        }
    }
}
