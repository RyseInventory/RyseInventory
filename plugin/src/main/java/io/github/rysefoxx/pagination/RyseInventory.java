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

import com.google.common.base.Preconditions;
import io.github.rysefoxx.SlotIterator;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.IntelligentItemData;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.enums.*;
import io.github.rysefoxx.events.RyseInventoryCloseEvent;
import io.github.rysefoxx.events.RyseInventoryOpenEvent;
import io.github.rysefoxx.events.RyseInventoryTitleChangeEvent;
import io.github.rysefoxx.other.EventCreator;
import io.github.rysefoxx.pattern.SlotIteratorPattern;
import io.github.rysefoxx.util.StringConstants;
import io.github.rysefoxx.util.TimeUtils;
import io.github.rysefoxx.util.TitleUpdater;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class RyseInventory {

    private InventoryManager manager;

    @Getter
    private InventoryProvider provider;
    private String title;

    @Getter
    @Nullable
    private Inventory inventory;


    private boolean clearAndSafe;
    private SlideAnimation slideAnimator;
    private Object identifier;
    private transient Plugin plugin;

    @Getter
    private int size = -1;
    @Getter
    private int fixedPageSize = -1;
    private int delay = 0;
    private int openDelay = -1;
    private int period = 1;
    private int closeAfter = -1;
    private int loadDelay = -1;
    private int loadTitle = -1;
    private boolean closeAble = true;
    private boolean transferData = true;
    private boolean backward = false;
    
    @NotNull
    private String titleHolder = "§e§oLoading§8...§r";
    @NotNull
    private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;

    protected final List<Player> delayed = new ArrayList<>();
    private List<InventoryOptions> options = new ArrayList<>();
    private List<EventCreator<? extends Event>> events = new ArrayList<>();
    private List<DisabledInventoryClick> ignoreClickEvent = new ArrayList<>();
    private List<CloseReason> closeReasons = new ArrayList<>();
    private List<IntelligentItemNameAnimator> itemAnimator = new ArrayList<>();
    private List<IntelligentMaterialAnimator> materialAnimator = new ArrayList<>();
    private List<IntelligentTitleAnimator> titleAnimator = new ArrayList<>();
    private List<IntelligentItemLoreAnimator> loreAnimator = new ArrayList<>();
    private final List<DisabledEvents> disabledEvents = new ArrayList<>();
    private final HashMap<UUID, Inventory> privateInventory = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> playerInventory = new HashMap<>();

    //Empty constructor for Builder
    private RyseInventory() {
    }

    /**
     * Copy constructor
     * <br>
     * See also: {@link #newInstance()}
     */
    private RyseInventory(@NotNull RyseInventory inventory) {
        this.manager = inventory.manager;
        this.provider = inventory.provider;
        this.title = inventory.title;
        this.inventory = inventory.inventory;
        this.clearAndSafe = inventory.clearAndSafe;
        this.slideAnimator = inventory.slideAnimator;
        this.identifier = inventory.identifier;
        this.plugin = inventory.plugin;
        this.size = inventory.size;
        this.delay = inventory.delay;
        this.openDelay = inventory.openDelay;
        this.period = inventory.period;
        this.closeAfter = inventory.closeAfter;
        this.loadDelay = inventory.loadDelay;
        this.loadTitle = inventory.loadTitle;
        this.closeAble = inventory.closeAble;
        this.transferData = inventory.transferData;
        this.backward = inventory.backward;
        this.titleHolder = inventory.titleHolder;
        this.inventoryOpenerType = inventory.inventoryOpenerType;
        this.delayed.addAll(inventory.delayed);
        this.options.addAll(inventory.options);
        this.events.addAll(inventory.events);
        this.ignoreClickEvent.addAll(inventory.ignoreClickEvent);
        this.closeReasons.addAll(inventory.closeReasons);
        this.itemAnimator.addAll(inventory.itemAnimator);
        this.materialAnimator.addAll(inventory.materialAnimator);
        this.titleAnimator.addAll(inventory.titleAnimator);
        this.loreAnimator.addAll(inventory.loreAnimator);
        this.privateInventory.putAll(inventory.privateInventory);
        this.playerInventory.putAll(inventory.playerInventory);
    }

    /**
     * Serializes the inventory to a map.
     *
     * @return The serialized inventory.
     * @see #deserialize(Map, InventoryManager) to get back the inventory from the hashmap.
     */
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", this.title);
        map.put("size", this.size);
        map.put("delay", this.delay);
        map.put("plugin", this.plugin.getName());
        map.put("open-delay", this.openDelay);
        map.put("period", this.period);
        map.put("close-after", this.closeAfter);
        map.put("load-delay", this.loadDelay);
        map.put("load-title", this.loadTitle);
        map.put("close-able", this.closeAble);
        map.put("transfer-data", this.transferData);
        map.put("backward", this.backward);
        map.put("title-holder", this.titleHolder);
        map.put("inventory-opener-type", this.inventoryOpenerType.toString());
        map.put("options", this.options);
        map.put("events", this.events);
        map.put("ignore-click-event", this.ignoreClickEvent);
        map.put("close-reasons", this.closeReasons);
        map.put("item-animator", this.itemAnimator);
        map.put("material-animator", this.materialAnimator);
        map.put("title-animator", this.titleAnimator);
        map.put("lore-animator", this.loreAnimator);
        map.put("provider", this.provider);
        map.put("identifier", this.identifier);
        map.put("clear-and-safe", this.clearAndSafe);
        return map;
    }

    /**
     * Deserializes the inventory from a hashmap.
     *
     * @param data    The serialized inventory.
     * @param manager The manager that will be used to create the inventory.
     * @return The deserialized inventory.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable RyseInventory deserialize(@NotNull Map<String, Object> data, @NotNull InventoryManager manager) {
        if (data.isEmpty()) return null;

        RyseInventory inventory = new RyseInventory();
        inventory.title = (String) data.get("title");
        inventory.size = (int) data.get("size");
        inventory.delay = (int) data.get("delay");
        inventory.openDelay = (int) data.get("open-delay");
        inventory.period = (int) data.get("period");
        inventory.closeAfter = (int) data.get("close-after");
        inventory.loadDelay = (int) data.get("load-delay");
        inventory.loadTitle = (int) data.get("load-title");
        inventory.closeAble = (boolean) data.get("close-able");
        inventory.transferData = (boolean) data.get("transfer-data");
        inventory.backward = (boolean) data.get("backward");
        inventory.titleHolder = (String) data.get("title-holder");
        inventory.inventoryOpenerType = InventoryOpenerType.valueOf((String) data.get("inventory-opener-type"));
        inventory.options = (List<InventoryOptions>) data.get("options");
        inventory.events = (List<EventCreator<? extends Event>>) data.get("events");
        inventory.ignoreClickEvent = (List<DisabledInventoryClick>) data.get("ignore-click-event");
        inventory.closeReasons = (List<CloseReason>) data.get("close-reasons");
        inventory.itemAnimator = (List<IntelligentItemNameAnimator>) data.get("item-animator");
        inventory.materialAnimator = (List<IntelligentMaterialAnimator>) data.get("material-animator");
        inventory.titleAnimator = (List<IntelligentTitleAnimator>) data.get("title-animator");
        inventory.loreAnimator = (List<IntelligentItemLoreAnimator>) data.get("lore-animator");
        inventory.provider = (InventoryProvider) data.get("provider");
        inventory.identifier = data.get("identifier");
        inventory.clearAndSafe = (boolean) data.get("clear-and-safe");
        inventory.manager = manager;
        inventory.plugin = Bukkit.getPluginManager().getPlugin((String) data.get("plugin"));

        return inventory;
    }

    /**
     * Clones the current RyseInventory and returns the new RyseInventory instance.
     *
     * @return The new RyseInventory instance.
     */
    public @NotNull RyseInventory newInstance() {
        return new RyseInventory(this);
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentItemLoreAnimator> getLoreAnimation(@NotNull Object identifier) {
        return this.loreAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentItemNameAnimator> getNameAnimation(@NotNull Object identifier) {
        return this.itemAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentTitleAnimator> getTitleAnimation(@NotNull Object identifier) {
        return this.titleAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentMaterialAnimator> getMaterialAnimator(@NotNull Object identifier) {
        return this.materialAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * Closes the inventory from the player. InventoryClickEvent is no longer called here.
     *
     * @param player The player which inventory should be closed.
     */
    public void close(@NotNull Player player) {
        RyseInventoryCloseEvent event = new RyseInventoryCloseEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;


        if (this.playerInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(this.playerInventory.remove(player.getUniqueId()));
        }

        removeActiveAnimations();

        this.delayed.remove(player);
        this.privateInventory.remove(player.getUniqueId());
        this.manager.removeInventoryFromPlayer(player.getUniqueId());
        player.closeInventory();
    }

    /**
     * Get all players who have a certain inventory open
     *
     * @return The list with all found players.
     */
    public @NotNull List<UUID> getOpenedPlayers() {
        List<UUID> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> this.manager.getInventory(player.getUniqueId()).ifPresent(savedInventory -> {
            if (!this.equals(savedInventory)) return;
            players.add(player.getUniqueId());
        }));
        return players;
    }

    /**
     * Closes the inventory for all players.
     */
    public void closeAll() {
        getOpenedPlayers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull).forEach(player ->
                        this.manager.getInventory(player.getUniqueId()).ifPresent(mainInventory -> mainInventory.close(player)));
    }

    /**
     * Opens the inventory for all players.
     */
    public void openAll() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            open(onlinePlayer);
    }

    /**
     * Opens the inventory for all players.
     *
     * @param page The page to open.
     */
    public void openAll(@Nonnegative int page) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            open(onlinePlayer, page);
    }

    /**
     * Opens the inventory with the first page for all players with defined properties.
     *
     * @param keys   The keys
     * @param values The values
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public void openAll(String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            open(onlinePlayer, keys, values);
    }

    /**
     * Opens the inventory with the first page for all players.
     *
     * @param data The predefined data.
     */
    public void openAll(@NotNull Map<String, Object> data) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            open(onlinePlayer, data);
    }

    /**
     * Opens the inventory with the first page for all players.
     *
     * @param data The predefined data.
     * @param page The page to open.
     */
    public void openAll(@Nonnegative int page, @NotNull Map<String, Object> data) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            open(onlinePlayer, page, data);
    }

    /**
     * Opens the inventory for all players with defined properties.
     *
     * @param page   The page to open.
     * @param keys   The keys
     * @param values The values
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public void openAll(@Nonnegative int page, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            open(onlinePlayer, page, keys, values);
    }

    /**
     * Opens the inventory with the first page.
     *
     * @param player The player where the inventory should be opened.
     * @return Returns the Bukkit Inventory object. Null if the RyseInventoryOpenEvent is canceled.
     */
    public @Nullable Inventory open(@NotNull Player player) {
        return open(player, 1);
    }

    /**
     * Opens the inventory with the first page for multiple players.
     *
     * @param players The players for whom the inventory should be opened.
     */
    public void open(Player @NotNull ... players) {
        for (Player player : players)
            open(player, 1);
    }

    /**
     * Opens the inventory with the first page for multiple players with defined properties.
     *
     * @param players The players for whom the inventory should be opened.
     * @param keys    The keys
     * @param values  The values
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public void open(String @NotNull [] keys, Object @NotNull [] values, Player @NotNull ... players) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        for (Player player : players)
            open(player, 1, keys, values);
    }

    /**
     * Opens the inventory with the first page for multiple players with defined properties.
     *
     * @param players The players for whom the inventory should be opened.
     * @param data    The predefined data
     */
    public void open(@NotNull Map<String, Object> data, Player @NotNull ... players) {
        for (Player player : players)
            open(player, 1, data);
    }

    /**
     * Opens the inventory with a specific page for multiple players.
     *
     * @param players The players for whom the inventory should be opened.
     */
    public void open(@Nonnegative int page, Player @NotNull ... players) {
        for (Player player : players)
            open(player, page);
    }

    /**
     * Opens an inventory with a specific page.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @return Returns the Bukkit Inventory object. Null if the RyseInventoryOpenEvent is canceled.
     */
    public @Nullable Inventory open(@NotNull Player player, @Nonnegative int page) {
        return initInventory(player, page, null, null);
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @param keys   The keys
     * @param values The values
     * @return Returns the Bukkit Inventory object. Null if the RyseInventoryOpenEvent is canceled.
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public @Nullable Inventory open(@NotNull Player player, @Nonnegative int page, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        return initInventory(player, page, keys, values);
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @param data   The predefined data
     * @return Returns the Bukkit Inventory object. Null if the RyseInventoryOpenEvent is canceled.
     */
    public @Nullable Inventory open(@NotNull Player player, @Nonnegative int page, @NotNull Map<String, Object> data) {
        String[] keys = data.keySet().toArray(new String[0]);
        Object[] values = data.values().toArray();

        return initInventory(player, page, keys, values);
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param keys   The keys
     * @param values The values
     * @return Returns the Bukkit Inventory object. Null if the RyseInventoryOpenEvent is canceled.
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public @Nullable Inventory open(@NotNull Player player, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        return initInventory(player, 1, keys, values);
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param data   The predefined data
     * @return Returns the Bukkit Inventory object. Null if the RyseInventoryOpenEvent is canceled.
     */
    public @Nullable Inventory open(@NotNull Player player, @NotNull Map<String, Object> data) {
        String[] keys = data.keySet().toArray(new String[0]);
        Object[] values = data.values().toArray();

        return initInventory(player, 1, keys, values);
    }

    private @Nullable Inventory initInventory(@NotNull Player player, @Nonnegative int page, @Nullable String[] keys, @Nullable Object[] values) {
        RyseInventoryOpenEvent event = new RyseInventoryOpenEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return null;

        if (!equals(event.getInventory()))
            return event.getInventory().open(player);

        finishSavedInventory(player);
        removeActiveAnimations();

        clearInventoryWhenNeeded(player);

        Inventory setupInventory = setupInventory();

        InventoryContents contents = new InventoryContents(player, this);
        Optional<InventoryContents> optional = this.manager.getContents(player.getUniqueId());
        page--;

        contents.pagination().setPage(page);

        transferData(optional.orElse(null), contents, keys, values);
        setupData(player, setupInventory, contents);
        initProvider(player, contents);

        if (optional.isPresent() && optional.get().equals(contents)) return setupInventory;

        this.manager.stopUpdate(player.getUniqueId());

        Pagination pagination = contents.pagination();
        loadByPage(contents);

        if (page > pagination.lastPage()) {
            close(player);
            throw new IllegalArgumentException("There is no " + page + " side. Last page is " + pagination.lastPage());
        }

        loadDelay(page, pagination, player);
        closeInventoryWhenEnabled(player);

        finalizeInventoryAndOpen(player);
        return setupInventory;
    }

    /**
     * Get an EventCreator object based on the Event class.
     *
     * @param event The event what you want to get
     * @return null if there is no custom event matching the event class
     */
    public @Nullable EventCreator<? extends Event> getEvent(@NotNull Class<? extends Event> event) {
        return this.events.stream().filter(eventOne -> event == eventOne.getClazz()).findFirst().orElse(null);
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param player   The Player
     * @param newTitle The new title
     * @author <a href="https://www.spigotmc.org/threads/change-inventory-title-reflection-1-8-1-18.489966/">Original code (Slightly Modified)</a>
     */
    public void updateTitle(@NotNull Player player, @NotNull String newTitle) {
        RyseInventoryTitleChangeEvent event = new RyseInventoryTitleChangeEvent(player, this.title, newTitle);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        TitleUpdater.updateInventory(player, event.getNewTitle());
    }

    /**
     * @return the size of the inventory.
     */
    @Nonnegative
    public int size() {
        return this.size;
    }

    /**
     * Builder to create an inventory.
     *
     * @return The Builder object with several methods
     */
    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create an inventory.
     */
    public static class Builder {

        private final RyseInventory ryseInventory = new RyseInventory();

        //Empty constructor for Copy Constructor
        private Builder() {
        }

        private Builder(@NotNull Builder builder) {
            this.ryseInventory.slideAnimator = builder.ryseInventory.slideAnimator;
            this.ryseInventory.manager = builder.ryseInventory.manager;
            this.ryseInventory.title = builder.ryseInventory.title;
            this.ryseInventory.provider = builder.ryseInventory.provider;
            this.ryseInventory.identifier = builder.ryseInventory.identifier;
            this.ryseInventory.clearAndSafe = builder.ryseInventory.clearAndSafe;
            this.ryseInventory.titleHolder = builder.ryseInventory.titleHolder;
            this.ryseInventory.inventoryOpenerType = builder.ryseInventory.inventoryOpenerType;
            this.ryseInventory.closeAble = builder.ryseInventory.closeAble;
            this.ryseInventory.transferData = builder.ryseInventory.transferData;
            this.ryseInventory.size = builder.ryseInventory.size;
            this.ryseInventory.delay = builder.ryseInventory.delay;
            this.ryseInventory.openDelay = builder.ryseInventory.openDelay;
            this.ryseInventory.period = builder.ryseInventory.period;
            this.ryseInventory.closeAfter = builder.ryseInventory.closeAfter;
            this.ryseInventory.loadDelay = builder.ryseInventory.loadDelay;
            this.ryseInventory.loadTitle = builder.ryseInventory.loadTitle;
            this.ryseInventory.options.addAll(builder.ryseInventory.options);
            this.ryseInventory.events.addAll(builder.ryseInventory.events);
            this.ryseInventory.ignoreClickEvent.addAll(builder.ryseInventory.ignoreClickEvent);
            this.ryseInventory.closeReasons.addAll(builder.ryseInventory.closeReasons);
        }

        public @NotNull Builder newInstance() {
            return new Builder(this);
        }

        /**
         * Adds a manager to the inventory.
         *
         * @param manager InventoryManager
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder manager(@NotNull InventoryManager manager) {
            this.ryseInventory.manager = manager;
            return this;
        }

        /**
         * Settings to help ensure that the player is not disturbed while he has the inventory open.
         *
         * @param options All setting options for the inventory
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder options(InventoryOptions @NotNull ... options) {
            this.ryseInventory.options.addAll(new ArrayList<>(Arrays.asList(options)));
            return this;
        }

        /**
         * With this method you can automatically set when to close the inventory.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder closeAfter(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.ryseInventory.closeAfter = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Here you can set possible reasons to automatically close the inventory when the reason takes place.
         *
         * @param reasons The reason to close the inventory.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder close(CloseReason @NotNull ... reasons) {
            this.ryseInventory.closeReasons.addAll(new ArrayList<>(Arrays.asList(reasons)));
            return this;
        }

        /**
         * With this method, the content of the inventory is loaded later.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder loadDelay(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.ryseInventory.loadDelay = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * With this method the title will be loaded later.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder loadTitle(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.ryseInventory.loadTitle = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Modifies the inventory type.
         *
         * @param type What type of inventory should it be.
         * @return The Inventory Builder to set additional options.
         * @apiNote By default, the type is CHEST
         */
        public @NotNull Builder type(@NotNull InventoryOpenerType type) {
            this.ryseInventory.inventoryOpenerType = type;
            this.ryseInventory.size = type.getType().getDefaultSize();
            return this;
        }

        /**
         * When the inventory is opened, the inventory is emptied and saved. When closing the inventory, the inventory will be reloaded.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote By default, the inventory is not emptied and saved.
         */
        public @NotNull Builder clearAndSafe() {
            this.ryseInventory.clearAndSafe = true;
            return this;
        }

        /**
         * @param size The inventory size
         * @return The Inventory Builder to set additional options.
         * @throws IllegalArgumentException if size is smaller than 9 or larger than 54.
         */
        public @NotNull Builder size(@Nonnegative int size) throws IllegalArgumentException {
            if (size < 9 || size > 54)
                throw new IllegalArgumentException(size < 9 ? "The size can not be less than 9" : "The size can not be greater than 54");

            this.ryseInventory.size = size;
            return this;
        }

        /**
         * Gives the inventory an identification
         *
         * @param identifier The ID through which you can get the inventory
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder identifier(@NotNull Object identifier) {
            this.ryseInventory.identifier = identifier;
            return this;
        }

        /**
         * The provider to fill the inventory with content.
         *
         * @param provider Implement with new InventoryProvider()
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder provider(@NotNull InventoryProvider provider) {
            this.ryseInventory.provider = provider;
            return this;
        }

        /**
         * This method can be used to prevent the player from closing the inventory.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote The inventory is always closable by default.
         */
        public @NotNull Builder preventClose() {
            this.ryseInventory.closeAble = false;
            return this;
        }

        /**
         * The method can be used so that data is not transferred to the next page as well.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote The data is always transferred by default.
         */
        public @NotNull Builder preventTransferData() {
            this.ryseInventory.transferData = false;
            return this;
        }

        /**
         * Adjusts the delay of the scheduler.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder delay(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.ryseInventory.delay = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Adjusts the delay before the inventory is opened.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder openDelay(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.ryseInventory.openDelay = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Adjusts the period of the scheduler.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder period(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.ryseInventory.period = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * If you do not have a size but a row and column, you can also create an inventory by doing this.
         *
         * @param rows The row
         * @return The Inventory Builder to set additional options.
         * @throws IllegalArgumentException if rows > 6
         * @apiNote If you had to create an inventory with 1 row, do not pass 0 but 1. Also applies to multiple rows.
         */
        public @NotNull Builder rows(@Nonnegative int rows) throws IllegalArgumentException {
            if (rows > 6)
                throw new IllegalArgumentException("The rows can not be greater than 6");

            size(rows * 9);
            return this;
        }

        /**
         * Assigns a fixed title to the inventory
         *
         * @param title The title
         * @return The Inventory Builder to set additional options.
         * @apiNote The title can also be changed later when the inventory is open.
         */
        public @NotNull Builder title(@NotNull String title) {
            this.ryseInventory.title = title;
            return this;
        }

        /**
         * Based on this animation, the items can appear animated when opening the inventory.
         *
         * @param animation {@link  SlideAnimation#builder(Plugin)}
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder animation(@NotNull SlideAnimation animation) {
            this.ryseInventory.slideAnimator = animation;
            return this;
        }

        /**
         * Adds a temporary title to the inventory.
         *
         * @param title The temp title
         * @return The Inventory Builder to set additional options.
         * @apiNote This title is used when the {@link Builder#loadTitle(int, TimeSetting)} method is used.
         */
        public @NotNull Builder titleHolder(@NotNull String title) {
            this.ryseInventory.titleHolder = title;
            return this;
        }

        /**
         * Adds its own event to the inventory.
         *
         * @param event What kind of event
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder listener(@NotNull EventCreator<? extends Event> event) {
            this.ryseInventory.events.add(event);
            return this;
        }

        /**
         * Set what should be ignored in the InventoryClickEvent.
         *
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoreClickEvent(DisabledInventoryClick @NotNull ... clicks) {
            this.ryseInventory.ignoreClickEvent.addAll(new ArrayList<>(Arrays.asList(clicks)));
            return this;
        }

        /**
         * Disables events according to wishes.
         *
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoreEvents(DisabledEvents @NotNull ... events) {
            this.ryseInventory.disabledEvents.addAll(new ArrayList<>(Arrays.asList(events)));
            return this;
        }


        /**
         * Sets a page number. These pages can be opened at any time independently of the item.
         * @param page The page number
         * @return The Inventory Builder to set additional options.
         */
        public Builder fixedPageSize(@Nonnegative int page) {
            this.ryseInventory.fixedPageSize = page;
            return this;
        }

        /**
         * Builds the RyseInventory
         *
         * @param plugin Instance to your main class.
         * @return the RyseInventory
         * @throws IllegalStateException if manager is null or if the provider is null
         */
        public @NotNull RyseInventory build(@NotNull Plugin plugin) throws IllegalStateException {
            if (this.ryseInventory.manager == null)
                throw new IllegalStateException("No manager could be found. Make sure you pass a manager to the builder.");

            if (!this.ryseInventory.closeAble && !this.ryseInventory.closeReasons.isEmpty())
                throw new IllegalStateException("The #close() method could not be executed because you have forbidden closing the inventory by #preventClose.");

            if (this.ryseInventory.provider == null)
                throw new IllegalStateException("No provider could be found. Make sure you pass a provider to the builder.");

            this.ryseInventory.plugin = plugin;

            return this.ryseInventory;
        }
    }

    /**
     * @return inventory title
     */
    public @NotNull String getTitle() {
        return this.title;
    }

    /**
     * @return how much later the scheduler starts (in milliseconds)
     */
    public @Nonnegative int getDelay() {
        return this.delay;
    }

    /**
     * @return how often the scheduler ticks (in milliseconds)
     */
    public @Nonnegative int getPeriod() {
        return this.period;
    }

    /**
     * @return if the inventory can be closed
     */
    public boolean isCloseAble() {
        return this.closeAble;
    }

    /**
     * @return A list of DisabledInventoryClick objects.
     */
    public @NotNull List<DisabledInventoryClick> getIgnoreClickEvent() {
        return this.ignoreClickEvent;
    }

    /**
     * @return A list of DisabledEvents objects.
     */
    public List<DisabledEvents> getDisabledEvents() {
        return disabledEvents;
    }

    /**
     * @return the ID from the inventory
     * @apiNote You have to give the inventory itself an ID with {@link Builder#identifier(Object)}
     */
    public @Nullable Object getIdentifier() {
        return this.identifier;
    }

    /**
     * @return the type.
     */
    public @NotNull InventoryOpenerType getInventoryOpenerType() {
        return this.inventoryOpenerType;
    }

    /**
     * @return true if the {@link Builder#clearAndSafe()} method was called.
     */
    public boolean isClearAndSafe() {
        return this.clearAndSafe;
    }

    /**
     * @return All the setting options that have been set.
     */
    public @NotNull List<InventoryOptions> getOptions() {
        return options;
    }

    /**
     * @return the load delay
     */
    public int getLoadDelay() {
        return loadDelay;
    }

    /**
     * @param uuid Player's uuid
     * @return the correct inventory based on whether it is split or not.
     */
    protected @NotNull Optional<Inventory> inventoryBasedOnOption(@NotNull UUID uuid) {
        if (!this.privateInventory.containsKey(uuid)) return Optional.empty();

        return Optional.of(this.privateInventory.get(uuid));
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RyseInventory)) return false;
        RyseInventory that = (RyseInventory) o;

        return clearAndSafe == that.clearAndSafe && size == that.size && delay == that.delay && openDelay == that.openDelay && period == that.period && closeAfter == that.closeAfter && loadDelay == that.loadDelay && loadTitle == that.loadTitle && closeAble == that.closeAble && transferData == that.transferData && Objects.equals(title, that.title) && Objects.equals(slideAnimator, that.slideAnimator) && Objects.equals(identifier, that.identifier) && Objects.equals(titleHolder, that.titleHolder) && inventoryOpenerType == that.inventoryOpenerType && Objects.equals(options, that.options) && Objects.equals(events, that.events) && Objects.equals(ignoreClickEvent, that.ignoreClickEvent) && Objects.equals(closeReasons, that.closeReasons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this);
    }

    protected void load(@NotNull Pagination pagination, @NotNull Player player, @Nonnegative int page) {
        pagination.getDataByPage(page).forEach(item -> placeItem(player, item.getModifiedSlot(), item.getItem()));
    }

    private void placeItem(@NotNull Player player, @Nonnegative int integer, @NotNull IntelligentItem item) {
        if (this.inventory != null)
            if (integer >= this.inventory.getSize()) return;

        if (!item.isCanSee()) {
            item.getError().cantSee(player, item);
            return;
        }
        this.inventory.setItem(integer, item.getItemStack());
    }

    private void closeInventoryWhenEnabled(@NotNull Player player) throws IllegalStateException {
        if (this.closeAfter == -1) return;
        if (!this.closeAble)
            throw new IllegalStateException("The #closeAfter() method could not be executed because you have forbidden closing the inventory by #preventClose.");

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> close(player), this.closeAfter);
    }

    private void removeActiveAnimations() {
        for (int i = 0; i < this.itemAnimator.size(); i++)
            removeItemAnimator(this.itemAnimator.get(i));

        for (int i = 0; i < this.titleAnimator.size(); i++)
            removeTitleAnimator(this.titleAnimator.get(i));

        for (int i = 0; i < this.loreAnimator.size(); i++)
            removeLoreAnimator(this.loreAnimator.get(i));

        for (int i = 0; i < this.materialAnimator.size(); i++)
            removeMaterialAnimator(this.materialAnimator.get(i));

        removeSlideAnimator();
    }

    protected void setBackward() {
        this.backward = true;
    }

    protected @NotNull InventoryManager getManager() {
        return manager;
    }

    private void finishSavedInventory(@NotNull Player player) {
        Optional<RyseInventory> savedInventory = this.manager.getInventory(player.getUniqueId());

        savedInventory.ifPresent(mainInventory -> {
            if (!this.backward)
                this.manager.setLastInventory(player.getUniqueId(), mainInventory, this);

            this.manager.removeInventory(player.getUniqueId());

            if (mainInventory.playerInventory.containsKey(player.getUniqueId())) {
                player.getInventory().setContents(mainInventory.playerInventory.remove(player.getUniqueId()));
            }
        });
    }

    private void clearInventoryWhenNeeded(@NotNull Player player) {
        if (!this.clearAndSafe) return;

        this.playerInventory.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
    }

    private @NotNull Inventory setupInventory() {
        if (this.inventoryOpenerType == InventoryOpenerType.CHEST) {
            return Bukkit.createInventory(null, this.size, this.loadTitle == -1 ? this.title : this.titleHolder);
        }
        return inventory = Bukkit.createInventory(null, this.inventoryOpenerType.getType(), buildTitle());
    }

    @Contract(pure = true)
    private @NotNull String buildTitle() {
        if (this.loadTitle == -1) return this.title;
        return this.titleHolder;
    }

    private void transferData(InventoryContents oldContents, @NotNull InventoryContents newContents, @Nullable String[] keys, @Nullable Object[] values) {
        if (this.transferData && oldContents != null)
            oldContents.transferData(newContents);

        if (keys != null && values != null) {
            for (int n = 0; n < keys.length; n++) {
                String key = keys[n];
                Object value = values[n];
                if (key == null || value == null) continue;

                newContents.setData(key, value);
            }
        }
    }

    private void setupData(@NotNull Player player, @NotNull Inventory inventory, @NotNull InventoryContents contents) {
        this.manager.setContents(player.getUniqueId(), contents);

        this.inventory = inventory;
        this.privateInventory.put(player.getUniqueId(), inventory);
    }

    private void initProvider(@NotNull Player player, @NotNull InventoryContents contents) {
        if (this.slideAnimator == null) {
            this.provider.init(player, contents);
            return;
        }
        this.provider.init(player, contents, this.slideAnimator);
    }

    private void loadDelay(@Nonnegative int page, @NotNull Pagination pagination, @NotNull Player player) {
        if (this.loadDelay != -1) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> load(pagination, player, page), this.loadDelay);
        } else {
            load(pagination, player, page);
        }

        if (this.loadTitle != -1)
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> updateTitle(player, this.title), this.loadTitle);
    }

    private void finalizeInventoryAndOpen(@NotNull Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.openDelay == -1 || this.delayed.contains(player)) {
                player.openInventory(inventory);
                this.manager.invokeScheduler(player, this);
                this.manager.setInventory(player.getUniqueId(), this);
            } else {
                if (!this.delayed.contains(player)) {
                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        player.openInventory(inventory);
                        this.manager.invokeScheduler(player, this);
                        this.manager.setInventory(player.getUniqueId(), this);
                    }, this.openDelay);
                    this.delayed.add(player);
                }
            }
        });
    }

    protected void loadByPage(@NotNull InventoryContents contents) {
        Pagination pagination = contents.pagination();
        SlotIterator iterator = contents.iterator();

        if (iterator == null) return;

        SlotIterator.SlotIteratorType type = iterator.getType();
        SlotIteratorPattern pattern = iterator.getPatternBuilder();

        if (this.inventoryOpenerType != InventoryOpenerType.CHEST
                && this.inventoryOpenerType != InventoryOpenerType.ENDER_CHEST
                && pattern != null) {
            throw new IllegalStateException("SlotIterator with PatternBuilder is not supported for InventoryOpenerType " + this.inventoryOpenerType.getType().toString());
        }

        int itemsSet = 0;
        int page = 0;
        int startSlot = iterator.getSlot();
        int slot = startSlot;
        List<IntelligentItemData> data = contents.pagination().getInventoryData();

        int patternSlot = -1;
        int patternLineIndex = 0;
        int stoppedAtIndex = 0;

        for (int i = 0; i < data.size(); i++) {
            IntelligentItemData itemData = data.get(i);
            if (itemData.getModifiedSlot() != -1) continue;

            if (itemsSet >= pagination.getItemsPerPage() || (slot >= iterator.getEndPosition() && iterator.getEndPosition() != -1 && pattern == null)) {
                itemsSet = 0;
                patternSlot = -1;
                patternLineIndex = 0;
                stoppedAtIndex = 0;
                slot = startSlot;
                page++;
            }

            if (pattern != null) {
                String line = pattern.getLines().get(patternLineIndex);

                if (stoppedAtIndex >= line.toCharArray().length) stoppedAtIndex = 0;

                for (int j = stoppedAtIndex; j < line.toCharArray().length; j++) {
                    char c = line.charAt(j);
                    patternSlot++;

                    if (patternSlot >= size()) {
                        j = -1;
                        patternLineIndex = 0;
                        patternSlot = -1;
                        page++;
                        itemsSet = 0;
                        stoppedAtIndex = 0;
                        line = pattern.getLines().get(patternLineIndex);
                        continue;
                    }

                    if (j + 1 >= line.toCharArray().length && (c != pattern.getAttachedChar() || get(data, patternSlot, page) != null)) {
                        j = -1;
                        if (patternSlot == 8 || patternSlot == 8 + 9 || patternSlot == 8 + 18 || patternSlot == 8 + 27 || patternSlot == 8 + 36)
                            patternLineIndex++;
                        if (patternSlot == 8 + 45) {
                            patternLineIndex = 0;
                            patternSlot = -1;
                            page++;
                        }
                        itemsSet = 0;
                        stoppedAtIndex = 0;
                        line = pattern.getLines().get(patternLineIndex);
                        continue;
                    }

                    if (c != pattern.getAttachedChar() || get(data, patternSlot, page) != null)
                        continue;

                    itemData.setPage(page);
                    itemData.setModifiedSlot(patternSlot);

                    data.set(i, itemData);
                    itemsSet++;
                    stoppedAtIndex = j + 1;

                    if (patternSlot == 8 + 45) {
                        patternLineIndex = 0;
                        patternSlot = -1;
                        itemsSet = 0;
                        stoppedAtIndex = 0;
                        page++;
                    }
                    break;
                }
                continue;
            }
            if (!iterator.isOverride()) {
                int[] dataArray = nextSlotAlgorithm(contents, type, page, slot, startSlot);
                page = dataArray[0];
                slot = dataArray[1];
            }

            pagination.remove(slot, page);

            itemData.setPage(page);
            itemData.setModifiedSlot(slot);

            data.set(i, itemData);
            itemsSet++;

            slot = updateForNextSlot(type, slot, startSlot);
        }

        contents.pagination().setInventoryData(data);
    }

    private @Nullable IntelligentItem get(@NotNull List<IntelligentItemData> inventoryData, @Nonnegative int slot, @Nonnegative int page) {
        for (IntelligentItemData data : inventoryData) {
            if (data.getPage() == page && data.getModifiedSlot() == slot)
                return data.getItem();
        }
        return null;
    }

    private int updateForNextSlot(@NotNull SlotIterator.SlotIteratorType type, @Nonnegative int slot, @Nonnegative int startSlot) {
        if (type == SlotIterator.SlotIteratorType.HORIZONTAL)
            return ++slot;

        if (type == SlotIterator.SlotIteratorType.VERTICAL) {
            if ((slot + 9) > size()) {
                return startSlot + 1;
            }
            slot += 9;
            return slot;
        }
        return slot;
    }

    @Contract("_, _, _, _, _ -> new")
    private int @NotNull [] nextSlotAlgorithm(@NotNull InventoryContents contents, @NotNull SlotIterator.SlotIteratorType type, @Nonnegative int page, @Nonnegative int calculatedSlot, @Nonnegative int startSlot) {
        SlotIterator iterator = Objects.requireNonNull(contents.iterator());

        int toAdd = 0;
        while (!contents.firstEmpty().isPresent() ? iterator.getBlackList().contains(calculatedSlot) : contents.getInPage(page, calculatedSlot).isPresent() || iterator.getBlackList().contains(calculatedSlot)) {
            if (calculatedSlot >= 53) {
                calculatedSlot = startSlot;
                page++;
            }

            if (type == SlotIterator.SlotIteratorType.HORIZONTAL) {
                calculatedSlot++;
                continue;
            }
            if ((calculatedSlot + 9) > size()) {
                toAdd++;
                calculatedSlot = startSlot + toAdd;
            } else {
                calculatedSlot += 9;
            }
        }
        return new int[]{page, calculatedSlot};
    }

    protected void clearData(@NotNull Player player) {
        if (this.playerInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(this.playerInventory.remove(player.getUniqueId()));
        }

        this.delayed.remove(player);
        this.privateInventory.remove(player.getUniqueId());
        this.manager.removeInventoryFromPlayer(player.getUniqueId());
    }

    protected void addItemAnimator(@NotNull IntelligentItemNameAnimator animator) {
        this.itemAnimator.add(animator);
    }

    protected void addMaterialAnimator(@NotNull IntelligentMaterialAnimator animator) {
        this.materialAnimator.add(animator);
    }

    protected void removeMaterialAnimator(@NotNull IntelligentMaterialAnimator animator) {
        this.materialAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    protected void removeItemAnimator(@NotNull IntelligentItemNameAnimator animator) {
        this.itemAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    protected void addTitleAnimator(@NotNull IntelligentTitleAnimator animator) {
        this.titleAnimator.add(animator);
    }

    protected void removeTitleAnimator(@NotNull IntelligentTitleAnimator animator) {
        this.titleAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    protected void addLoreAnimator(@NotNull IntelligentItemLoreAnimator animator) {
        this.loreAnimator.add(animator);
    }

    protected void removeLoreAnimator(@NotNull IntelligentItemLoreAnimator animator) {
        this.loreAnimator.remove(animator);

        animator.getTasks().forEach(bukkitTask -> {
            if (!Bukkit.getScheduler().isQueued(bukkitTask.getTaskId())) return;
            bukkitTask.cancel();
        });
    }

    protected void removeSlideAnimator() {
        if (this.slideAnimator == null) return;

        this.slideAnimator.getTasks().forEach(bukkitTask -> {
            if (!Bukkit.getScheduler().isQueued(bukkitTask.getTaskId())) return;
            bukkitTask.cancel();
        });
    }

    protected @Nullable SlideAnimation getSlideAnimator() {
        return this.slideAnimator;
    }

    protected @Nonnegative int activeSlideAnimatorTasks() {
        if (this.slideAnimator == null) return 0;
        AtomicInteger counter = new AtomicInteger();

        this.slideAnimator.getTasks().forEach(task -> {
            if (!Bukkit.getScheduler().isQueued(task.getTaskId())) return;
            counter.getAndIncrement();
        });
        return counter.get();
    }

    protected List<CloseReason> getCloseReasons() {
        return closeReasons;
    }
}
