package com.github.rysefoxx.pagination;

import com.github.rysefoxx.opener.InventoryOpenerType;
import com.github.rysefoxx.other.EventCreator;
import com.github.rysefoxx.other.InventoryOptions;
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
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 2/17/2022
 */
public class InventoryManager {

    private final JavaPlugin plugin;

    private final HashMap<UUID, RyseInventory> inventories;
    private final HashMap<UUID, InventoryContents> content;
    private final HashMap<UUID, BukkitTask> updaterTask;
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
     * @param uuid
     * @return null if the player has no inventory open.
     */
    public Optional<RyseInventory> getInventory(@NotNull UUID uuid) {
        if (!hasInventory(uuid)) return Optional.empty();
        return Optional.ofNullable(this.inventories.get(uuid));
    }

    /**
     * Get all players who have a certain inventory open
     *
     * @param inventory The inventory that is filtered by.
     * @return The list with all found players.
     */
    public List<UUID> getOpenedPlayers(@NotNull RyseInventory inventory) {
        List<UUID> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<RyseInventory> optional = getInventory(player.getUniqueId());

            optional.ifPresent(savedInventory -> {
                if (inventory != savedInventory) return;
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
    public Optional<RyseInventory> getLastInventory(@NotNull UUID uuid) {
        if (!this.lastInventories.containsKey(uuid)) return Optional.empty();
        return Optional.ofNullable(this.lastInventories.get(uuid));
    }

    /**
     * With this method you can get the inventory from the inventory identifier.
     *
     * @param identifier The ID to identify
     * @return null if no inventory with the ID could be found.
     * @implNote Only works if the inventory has also been assigned an identifier.
     */
    public Optional<RyseInventory> getInventory(@NotNull Object identifier) {
        return this.inventories.values().stream().filter(inventory -> Objects.equals(inventory.getIdentifier(), identifier)).findFirst();
    }

    /**
     * With this method you can get the inventory content from the player.
     *
     * @param uuid
     * @return the player inventory content.
     */
    public Optional<InventoryContents> getContents(@NotNull UUID uuid) {
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

    @Contract(pure = true)
    private boolean hasContents(@NotNull UUID uuid) {
        return this.content.containsKey(uuid);
    }

    protected void removeInventoryFromPlayer(@NotNull UUID uuid) {
        this.inventories.remove(uuid);
        this.content.remove(uuid);
        BukkitTask task = this.updaterTask.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
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

    protected void setLastInventory(@NotNull UUID uuid, @NotNull RyseInventory inventory) {
        this.lastInventories.put(uuid, inventory);
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

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamage(@NotNull EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;

            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (!mainInventory.getOptions().contains(InventoryOptions.NO_DAMAGE)) return;
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onFoodLevelChange(@NotNull FoodLevelChangeEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;

            if (!hasInventory(player.getUniqueId()))
                return;

            RyseInventory mainInventory = inventories.get(player.getUniqueId());

            if (!mainInventory.getOptions().contains(InventoryOptions.NO_HUNGER)) return;
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerAttemptPickupItem(@NotNull PlayerAttemptPickupItemEvent event) {
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
                if (!(entity instanceof Player player)) continue;
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
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getClickedInventory() == null) return;

            if (!hasInventory(player.getUniqueId()))
                return;


            RyseInventory mainInventory = inventories.get(player.getUniqueId());
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
                if (!hasContents(player.getUniqueId())) return;
                if (slot < 0 || (mainInventory.getInventoryOpenerType() == InventoryOpenerType.CHEST && slot > mainInventory.size()))
                    return;
                event.setCancelled(true);

                InventoryContents contents = content.get(player.getUniqueId());

                contents.get(slot).ifPresent(item -> {
                    if(!item.isCanClick()){
                        item.getError().cantClick(player, item);
                        return;
                    }
                    item.getConsumer().accept(event);
                });
                player.updateInventory();
            }

        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
        @SuppressWarnings("unchecked")
        public void onInventoryDrag(@NotNull InventoryDragEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (!hasInventory(player.getUniqueId()))
                return;

            Inventory topInventory = player.getOpenInventory().getTopInventory();
            RyseInventory mainInventory = inventories.get(player.getUniqueId());

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
            if (!hasInventory(player.getUniqueId()))
                return;
            RyseInventory mainInventory = inventories.get(player.getUniqueId());

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
