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

public class RyseInventory {

    private InventoryManager manager;
    private @Getter
    InventoryProvider provider;
    private String title;
    private @Getter
    Inventory inventory;
    private boolean clearAndSafe;
    private SlideAnimation slideAnimator;
    private Object identifier;
    private Plugin plugin;

    private @Getter
    int size = -1;
    private int delay = 0;
    private int openDelay = -1;
    private int period = 1;
    private int closeAfter = -1;
    private int loadDelay = -1;
    private int loadTitle = -1;
    private boolean closeAble = true;
    private boolean transferData = true;
    private boolean backward = false;
    private String titleHolder = "§e§oLoading§8...";
    private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;

    protected final List<Player> delayed = new ArrayList<>();
    private List<InventoryOptions> options = new ArrayList<>();
    private List<EventCreator<? extends Event>> events = new ArrayList<>();
    private List<DisabledInventoryClick> ignoreClickEvent = new ArrayList<>();
    private List<CloseReason> closeReasons = new ArrayList<>();
    private final List<IntelligentItemNameAnimator> itemAnimator = new ArrayList<>();
    private final List<IntelligentMaterialAnimator> materialAnimator = new ArrayList<>();
    private final List<IntelligentTitleAnimator> titleAnimator = new ArrayList<>();
    private final List<IntelligentItemLoreAnimator> loreAnimator = new ArrayList<>();
    private final HashMap<UUID, Inventory> privateInventory = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> playerInventory = new HashMap<>();

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
        Bukkit.getOnlinePlayers().forEach(player -> {
            this.manager.getInventory(player.getUniqueId()).ifPresent(savedInventory -> {
                if (this != savedInventory) return;
                players.add(player.getUniqueId());
            });
        });
        return players;
    }

    /**
     * Closes the inventory for all players.
     */
    public void closeAll() {
        getOpenedPlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(player -> {
            this.manager.getInventory(player.getUniqueId()).ifPresent(mainInventory -> mainInventory.close(player));
        });
    }

    /**
     * Opens the inventory with the first page.
     *
     * @param player The player where the inventory should be opened.
     * @return the Bukkit Inventory object.
     */
    public @NotNull Inventory open(@NotNull Player player) {
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
     * @return Returns the Bukkit Inventory object.
     */
    public @NotNull Inventory open(@NotNull Player player, @Nonnegative int page) {
        return initInventory(player, page, null, null);
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @param keys   The keys
     * @param values The values
     * @return Returns the Bukkit Inventory object.
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public @NotNull Inventory open(@NotNull Player player, @Nonnegative int page, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        return initInventory(player, page, keys, values);
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param keys   The keys
     * @param values The values
     * @return Returns the Bukkit Inventory object.
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public @NotNull Inventory open(@NotNull Player player, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        return initInventory(player, 1, keys, values);
    }

    private @NotNull Inventory initInventory(@NotNull Player player, @Nonnegative int page, @Nullable String[] keys, @Nullable Object[] values) {
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
        TitleUpdater.updateTitle(player, newTitle);
    }

    /**
     * @return the size of the inventory.
     */
    public @Nonnegative
    int size() {
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
        private SlideAnimation slideAnimation;
        private InventoryManager manager;
        private String title;
        private InventoryProvider provider;
        private Object identifier;
        private boolean clearAndSafe;

        private String titleHolder = "§e§oLoading§8...";
        private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;
        private boolean closeAble = true;
        private boolean transferData = true;
        private int size = -1;
        private int delay = 0;
        private int openDelay = -1;
        private int period = 1;
        private int closeAfter = -1;
        private int loadDelay = -1;
        private int loadTitle = -1;

        private final List<InventoryOptions> options = new ArrayList<>();
        private final List<EventCreator<? extends Event>> events = new ArrayList<>();
        private final List<DisabledInventoryClick> ignoreClickEvent = new ArrayList<>();
        private final List<CloseReason> closeReasons = new ArrayList<>();

        /**
         * Adds a manager to the inventory.
         *
         * @param manager InventoryManager
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder manager(@NotNull InventoryManager manager) {
            this.manager = manager;
            return this;
        }

        /**
         * Settings to help ensure that the player is not disturbed while he has the inventory open.
         *
         * @param options All setting options for the inventory
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder options(InventoryOptions @NotNull ... options) {
            this.options.addAll(new ArrayList<>(Arrays.asList(options)));
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
            this.closeAfter = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Here you can set possible reasons to automatically close the inventory when the reason takes place.
         *
         * @param reasons The reason to close the inventory.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder close(CloseReason @NotNull ... reasons) {
            this.closeReasons.addAll(new ArrayList<>(Arrays.asList(reasons)));
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
            this.loadDelay = TimeUtils.buildTime(time, setting);
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
            this.loadTitle = TimeUtils.buildTime(time, setting);
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
            this.inventoryOpenerType = type;
            this.size = type.getType().getDefaultSize();
            return this;
        }

        /**
         * When the inventory is opened, the inventory is emptied and saved. When closing the inventory, the inventory will be reloaded.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote By default, the inventory is not emptied and saved.
         */
        public @NotNull Builder clearAndSafe() {
            this.clearAndSafe = true;
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

            this.size = size;
            return this;
        }

        /**
         * Gives the inventory an identification
         *
         * @param identifier The ID through which you can get the inventory
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * The provider to fill the inventory with content.
         *
         * @param provider Implement with new InventoryProvider()
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder provider(@NotNull InventoryProvider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * This method can be used to prevent the player from closing the inventory.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote The inventory is always closable by default.
         */
        public @NotNull Builder preventClose() {
            this.closeAble = false;
            return this;
        }

        /**
         * The method can be used so that data is not transferred to the next page as well.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote The data is always transferred by default.
         */
        public @NotNull Builder preventTransferData() {
            this.transferData = false;
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
            this.delay = TimeUtils.buildTime(time, setting);
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
            this.openDelay = TimeUtils.buildTime(time, setting);
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
            this.period = TimeUtils.buildTime(time, setting);
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
            this.title = title;
            return this;
        }

        /**
         * Based on this animation, the items can appear animated when opening the inventory.
         *
         * @param animation {@link  SlideAnimation#builder(Plugin)}
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder animation(@NotNull SlideAnimation animation) {
            this.slideAnimation = animation;
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
            this.titleHolder = title;
            return this;
        }

        /**
         * Adds its own event to the inventory.
         *
         * @param event What kind of event
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder listener(@NotNull EventCreator<? extends Event> event) {
            this.events.add(event);
            return this;
        }

        /**
         * Set what should be ignored in the InventoryClickEvent.
         *
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoreClickEvent(DisabledInventoryClick @NotNull ... clicks) {
            this.ignoreClickEvent.addAll(new ArrayList<>(Arrays.asList(clicks)));
            return this;
        }

        /**
         * Builds the RyseInventory
         *
         * @param plugin Instance to your main class.
         * @return the RyseInventory
         * @throws IllegalStateException if manager is null
         */
        public @NotNull RyseInventory build(@NotNull Plugin plugin) throws IllegalStateException {
            if (this.manager == null)
                throw new IllegalStateException("No manager could be found. Make sure you pass a manager to the builder.");

            if (!this.closeAble && !this.closeReasons.isEmpty())
                throw new IllegalStateException("The #close() method could not be executed because you have forbidden closing the inventory by #preventClose.");

            RyseInventory inventory = new RyseInventory();
            inventory.plugin = plugin;
            inventory.manager = this.manager;
            inventory.size = this.size;
            inventory.closeAble = this.closeAble;
            inventory.title = this.title;
            inventory.events = this.events;
            inventory.loadTitle = this.loadTitle;
            inventory.ignoreClickEvent = this.ignoreClickEvent;
            inventory.provider = this.provider;
            inventory.delay = this.delay;
            inventory.openDelay = this.openDelay;
            inventory.period = this.period;
            inventory.identifier = this.identifier;
            inventory.closeAfter = this.closeAfter;
            inventory.loadDelay = this.loadDelay;
            inventory.transferData = this.transferData;
            inventory.inventoryOpenerType = this.inventoryOpenerType;
            inventory.titleHolder = this.titleHolder;
            inventory.clearAndSafe = this.clearAndSafe;
            inventory.options = this.options;
            inventory.slideAnimator = this.slideAnimation;
            inventory.closeReasons = this.closeReasons;
            return inventory;
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

    protected void load(@NotNull Pagination pagination, @NotNull Player player, @Nonnegative int page) {
        pagination.getDataByPage(page).forEach(item -> placeItem(player, item.getModifiedSlot(), item.getItem()));
    }

    private void placeItem(@NotNull Player player, @Nonnegative int integer, @NotNull IntelligentItem item) {
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
        for (IntelligentItemNameAnimator intelligentItemNameAnimator : this.itemAnimator)
            removeItemAnimator(intelligentItemNameAnimator);

        for (IntelligentTitleAnimator intelligentTitleAnimator : this.titleAnimator)
            removeTitleAnimator(intelligentTitleAnimator);

        for (IntelligentItemLoreAnimator intelligentItemLoreAnimator : this.loreAnimator)
            removeLoreAnimator(intelligentItemLoreAnimator);

        for (IntelligentMaterialAnimator intelligentMaterialAnimator : this.materialAnimator)
            removeMaterialAnimator(intelligentMaterialAnimator);

        removeSlideAnimator();
    }

    protected void setBackward(boolean bool) {
        this.backward = bool;
    }

    protected @NotNull InventoryManager getManager() {
        return manager;
    }

    private void finishSavedInventory(@NotNull Player player) {
        Optional<RyseInventory> savedInventory = this.manager.getInventory(player.getUniqueId());

        savedInventory.ifPresent(mainInventory -> {
            if (!this.backward)
                this.manager.setLastInventory(player.getUniqueId(), mainInventory);

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
            Arrays.stream(keys).filter(Objects::nonNull).forEach(s -> Arrays.stream(values).filter(Objects::nonNull).forEach(o -> newContents.setData(s, o)));
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

            itemData.setPage(page);
            itemData.setModifiedSlot(slot);

            data.set(i, itemData);
            itemsSet++;

            slot = updateForNextSlot(type, slot, startSlot);
        }

        contents.pagination().setInventoryData(data);
    }

    private @Nullable IntelligentItem get(List<IntelligentItemData> inventoryData, @Nonnegative int slot, @Nonnegative int page) {
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
