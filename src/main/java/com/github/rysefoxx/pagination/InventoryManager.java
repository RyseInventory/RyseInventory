package com.github.rysefoxx.pagination;

import com.github.rysefoxx.other.EventCreator;
import org.apache.commons.lang.Validate;
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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public class InventoryManager {

    private final JavaPlugin plugin;

    private final HashMap<Player, RyseInventory> inventories;
    private final HashMap<Player, InventoryContents> content;
    private final HashMap<Player, BukkitTask> updaterTask;
    private final HashMap<UUID, RyseInventory> lastInventories;

    @Contract(pure = true)
    public InventoryManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.inventories = new HashMap<>();
        this.content = new HashMap<>();
        this.updaterTask = new HashMap<>();
        this.lastInventories = new HashMap<>();
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
     * Get the last inventory that the player had open.
     * @param uuid Player UUID
     * @return null if there is no final inventory.
     */
    public @Nullable RyseInventory getLastInventory(@NotNull UUID uuid) {
        if (!this.lastInventories.containsKey(uuid)) return null;
        return this.lastInventories.get(uuid);
    }

    /**
     * With this method you can get the inventory from the inventory identifier.
     *
     * @param identifier The ID to identify
     * @return null if no inventory with the ID could be found.
     * @throws IllegalArgumentException when identifier is null
     * @implNote Only works if the inventory has also been assigned an identifier.
     */
    public @Nullable RyseInventory getInventory(@NotNull Object identifier) throws IllegalArgumentException {
        Validate.notNull(identifier, "Object must not be null.");
        return this.inventories.values().stream().filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier)).findFirst().orElse(null);
    }

    /**
     * With this method you can get the inventory content from the player.
     *
     * @param player
     * @return the player inventory content.
     * @throws IllegalArgumentException when player is null
     */
    public Optional<InventoryContents> getContents(@NotNull Player player) throws IllegalArgumentException {
        Validate.notNull(player, "Player must not be null.");
        if (!this.content.containsKey(player)) return Optional.empty();
        return Optional.ofNullable(this.content.get(player));
    }

    /**
     * Registers the standard events
     */
    public void invoke() {
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this.plugin);
    }

    @Contract(pure = true)
    private boolean hasInventory(@NotNull Player player) throws IllegalArgumentException {
        Validate.notNull(player, "Player must not be null.");
        return this.inventories.containsKey(player);
    }

    @Contract(pure = true)
    private boolean hasContents(@NotNull Player player) throws IllegalArgumentException {
        Validate.notNull(player, "Player must not be null.");
        return this.content.containsKey(player);
    }

    protected void removeInventoryFromPlayer(@NotNull Player player) throws IllegalArgumentException {
        Validate.notNull(player, "Player must not be null.");
        this.lastInventories.put(player.getUniqueId(), this.inventories.get(player));
        this.inventories.remove(player);
        this.content.remove(player);
        this.updaterTask.remove(player);
    }

    protected void removeInventory(@NotNull Player player) {
        if (!this.inventories.containsKey(player)) return;
        this.inventories.remove(player);
    }

    protected void setContents(@NotNull Player player, @NotNull InventoryContents contents) {
        this.content.put(player, contents);
    }

    protected void setInventory(@NotNull Player player, @NotNull RyseInventory inventory) {
        this.inventories.put(player, inventory);
    }

    protected void stopUpdate(@NotNull Player player) {
        if (!this.updaterTask.containsKey(player)) return;
        BukkitTask task = this.updaterTask.remove(player);
        task.cancel();
    }

    protected void invokeScheduler(@NotNull Player player, @NotNull RyseInventory inventory) throws IllegalArgumentException {
        Validate.notNull(player, "Player must not be null.");
        Validate.notNull(inventory, "RyseInventory must not be null.");
        if (this.updaterTask.containsKey(player)) return;

        BukkitTask task = new BukkitRunnable() {
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
        this.updaterTask.put(player, task);
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

                if (mainInventory.isIgnoreClickEvent()) return;
                if (!hasContents(player)) return;
                if (slot < 0 || slot > mainInventory.size()) return;
                event.setCancelled(true);

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

            lastInventories.put(player.getUniqueId(), mainInventory);

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
