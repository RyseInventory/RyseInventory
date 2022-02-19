package com.github.rysefoxx.pagination;

import com.github.rysefoxx.other.EventCreator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public class InventoryManager {

    private final JavaPlugin plugin;

    private final HashMap<Player, RyseInventory> inventories;
    private final HashMap<Player, InventoryContents> content;

    @Contract(pure = true)
    public InventoryManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.inventories = new HashMap<>();
        this.content = new HashMap<>();
    }

    /**
     * With this method you can get the inventory from the player.
     *
     * @param player
     * @return null if the player has no inventory open.
     */
    public @Nullable RyseInventory getInventory(@NotNull Player player) {
        if (!hasInventory(player)) return null;
        return this.inventories.get(player);
    }

    /**
     * With this method you can get the inventory from the inventory identifier.
     *
     * @param identifier The ID to identify
     * @return null if no inventory with the ID could be found.
     * @implNote Only works if the inventory has also been assigned an identifier.
     */
    public @Nullable RyseInventory getInventory(@NotNull Object identifier) {
        return this.inventories.values().stream().filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier)).findFirst().orElse(null);
    }

    /**
     * Registers the standard events
     */
    public void invoke() {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this.plugin);
    }

    private void invokeScheduler(@NotNull Player player, @NotNull RyseInventory inventory) {
        new BukkitRunnable() {
            final InventoryContents contents = content.get(player);

            @Override
            public void run() {
                if (!hasInventory(player)) {
                    cancel();
                    return;
                }
                RyseInventory savedInventory = inventories.get(player);
                if (savedInventory != inventory) {
                    cancel();
                    return;
                }
                inventory.getProvider().update(player, this.contents);
            }
        }.runTaskTimer(this.plugin, inventory.getDelay(), inventory.getPeriod());
    }

    @Contract(pure = true)
    private boolean hasInventory(@NotNull Player player) {
        return this.inventories.containsKey(player);
    }

    @Contract(pure = true)
    private boolean hasContents(@NotNull Player player) {
        return this.content.containsKey(player);
    }

    protected void removeInventoryFromPlayer(@NotNull Player player) {
        this.inventories.remove(player);
        this.content.remove(player);
    }

    protected void addInventoryToPlayer(@NotNull Player player, @NotNull RyseInventory inventory) {
        if (hasInventory(player)) {
            RyseInventory savedInventory = this.inventories.get(player);
            savedInventory.close(player);
        }

        InventoryContents inventoryContents = new InventoryContents(player);
        inventory.getProvider().init(player, inventoryContents);

        this.inventories.put(player, inventory);
        this.content.put(player, inventoryContents);

        invokeScheduler(player, inventory);
    }

    protected InventoryContents getContents(@NotNull Player player) {
        return this.content.get(player);
    }

    public class InventoryListener implements Listener {

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryClick(@NotNull InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getClickedInventory() == null) return;

            if (!hasInventory(player))
                return;

            RyseInventory mainInventory = inventories.get(player);
            InventoryAction action = event.getAction();
            Inventory clickedInventory = event.getClickedInventory();
            Inventory bottomInventory = player.getOpenInventory().getBottomInventory();
            Inventory topInventory = player.getOpenInventory().getTopInventory();
            int slot = event.getSlot();
            ClickType clickType = event.getClick();

            if (clickedInventory == bottomInventory) {
                if (action == InventoryAction.COLLECT_TO_CURSOR
                        || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                    return;
                }
                if (action == InventoryAction.NOTHING && clickType != ClickType.MIDDLE) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (clickedInventory == topInventory) {
                EventCreator<InventoryClickEvent> customEvent = (EventCreator<InventoryClickEvent>) mainInventory.getEvent(InventoryClickEvent.class);
                if (customEvent != null) {
                    customEvent.accept(event);
                    return;
                }

                if (!mainInventory.isIgnoreClickEvent()) {
                    event.setCancelled(true);
                }

                if (slot < 0 || slot > mainInventory.size()) return;
                if (!hasContents(player)) return;

                InventoryContents contents = content.get(player);

                contents.get(slot).ifPresent(item -> item.getConsumer().accept(event));
                player.updateInventory();
            }

        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryDrag(@NotNull InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!hasInventory(player))
                return;

            Inventory topInventory = player.getOpenInventory().getTopInventory();
            RyseInventory mainInventory = inventories.get(player);

            EventCreator<InventoryDragEvent> customEvent = (EventCreator<InventoryDragEvent>) mainInventory.getEvent(InventoryDragEvent.class);
            if (customEvent != null) {
                customEvent.accept(event);
                return;
            }

            event.getRawSlots().forEach(integer -> {
                if (integer >= topInventory.getSize()) return;
                event.setCancelled(true);
            });
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryClose(@NotNull InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player player)) return;
            if (!hasInventory(player))
                return;
            RyseInventory mainInventory = inventories.get(player);

            if (!mainInventory.isCloseAble()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(event.getInventory()));
            }

            EventCreator<InventoryCloseEvent> customEvent = (EventCreator<InventoryCloseEvent>) mainInventory.getEvent(InventoryCloseEvent.class);
            if (customEvent != null) {
                customEvent.accept(event);
                return;
            }

            mainInventory.close(player);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if (!hasInventory(player))
                return;

            RyseInventory mainInventory = inventories.get(player);

            EventCreator<PlayerQuitEvent> customEvent = (EventCreator<PlayerQuitEvent>) mainInventory.getEvent(PlayerQuitEvent.class);
            if (customEvent == null) return;

            customEvent.accept(event);
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        public void onPluginDisable(@NotNull PluginDisableEvent event) {
            Plugin disabledPlugin = event.getPlugin();

            if (disabledPlugin != plugin) return;


            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!hasInventory(player)) return;

                RyseInventory inventory = inventories.get(player);
                inventory.close(player);
            });

        }
    }
}
