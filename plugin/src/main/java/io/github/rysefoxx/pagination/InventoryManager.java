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
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
     * @param uuid
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
     * @implNote Only works if the inventory has also been assigned an identifier.
     */
    public @NotNull Optional<RyseInventory> getInventory(@NotNull Object identifier) {
        return this.inventories.values().stream().filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier)).findFirst();
    }

    /**
     * With this method you can get the inventory content from the player.
     *
     * @param uuid
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
    }

    @Contract(pure = true)
    private boolean hasInventory(@NotNull UUID uuid) {
        return this.inventories.containsKey(uuid);
    }

    protected void removeInventoryFromPlayer(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
        this.content.remove(uuid);
        BukkitTask task = this.updaterTask.remove(uuid);

        if (task != null)
            task.cancel();
    }

    protected void removeInventory(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
    }

    protected void setContents(@NotNull UUID uuid, @NotNull InventoryContents contents) {
        this.content.put(uuid, contents);
    }

    protected void setInventory(@NotNull UUID uuid, @NotNull RyseInventory inventory) {
        this.inventories.put(uuid, inventory);
    }

    protected void setLastInventory(@NotNull UUID uuid, @NotNull RyseInventory inventory, @NotNull RyseInventory newInventory) {
        List<RyseInventory> inventoryList = this.lastInventories.getOrDefault(uuid, new ArrayList<>());

        if (inventory.equals(newInventory)) return;

        inventoryList.add(inventory);

        this.lastInventories.put(uuid, inventoryList);
    }

    protected void stopUpdate(@NotNull UUID uuid) {
        if (!this.updaterTask.containsKey(uuid)) return;
        BukkitTask task = this.updaterTask.remove(uuid);
        task.cancel();
    }

    protected void invokeScheduler(@NotNull Player player, @NotNull RyseInventory inventory) {
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

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryClick(@NotNull InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

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

            if (clickedInventory == bottomInventory && (!list.contains(DisabledInventoryClick.BOTTOM) && !list.contains(DisabledInventoryClick.BOTH))) {
                if (mainInventory.getCloseReasons().contains(CloseReason.CLICK_BOTTOM_INVENTORY)) {
                    mainInventory.close(player);
                    return;
                }
                if (action == InventoryAction.COLLECT_TO_CURSOR
                        || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
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
                if (slot < 0 || (mainInventory.getInventoryOpenerType() == InventoryOpenerType.CHEST && slot > mainInventory.size())) {
                    return;
                }

                InventoryContents contents = content.get(player.getUniqueId());
                SlideAnimation animation = mainInventory.getSlideAnimator();

                if (animation != null && mainInventory.activeSlideAnimatorTasks() > 0 && animation.isBlockClickEvent()) {
                    event.setCancelled(true);
                    return;
                }

                if (!list.contains(DisabledInventoryClick.TOP) && !list.contains(DisabledInventoryClick.BOTH)) {
                    if (event.getClick() == ClickType.DOUBLE_CLICK) {
                        event.setCancelled(true);
                        return;
                    }
                    if (!mainInventory.getIgnoredSlots().contains(slot))
                        event.setCancelled(true);
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

            mainInventory.clearData(player);

            EventCreator<InventoryCloseEvent> customEvent = (EventCreator<InventoryCloseEvent>) mainInventory.getEvent(InventoryCloseEvent.class);
            if (customEvent != null) {
                customEvent.accept(event);
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
    }
}
