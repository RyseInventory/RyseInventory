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

package io.github.rysefoxx.inventory.plugin.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.inventory.plugin.animator.*;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItem;
import io.github.rysefoxx.inventory.plugin.content.IntelligentItemData;
import io.github.rysefoxx.inventory.plugin.content.InventoryContents;
import io.github.rysefoxx.inventory.plugin.content.InventoryProvider;
import io.github.rysefoxx.inventory.plugin.enums.*;
import io.github.rysefoxx.inventory.plugin.events.*;
import io.github.rysefoxx.inventory.plugin.other.EventCreator;
import io.github.rysefoxx.inventory.plugin.other.Page;
import io.github.rysefoxx.inventory.plugin.pattern.SlotIteratorPattern;
import io.github.rysefoxx.inventory.plugin.util.StringConstants;
import io.github.rysefoxx.inventory.plugin.util.TimeUtils;
import io.github.rysefoxx.inventory.plugin.util.TitleUpdater;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RyseInventory {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private RyseInventory originalInventory;

    private PaginationData paginationCache;

    private InventoryManager manager;
    @Getter
    private InventoryProvider provider;
    @Getter
    private @Nullable Inventory inventory;
    private SlideAnimation slideAnimator;
    @Getter(AccessLevel.PROTECTED)
    private AnvilGUI.Builder anvilGUIBuilder;
    private AnvilGUI anvilGUI;
    @Getter(AccessLevel.PROTECTED)
    private transient Plugin plugin;

    @Getter
    private int fixedPageSize = -1;

    private Component title;
    private Object identifier;
    private int size = -1;
    private int delay = 0;
    private int openDelay = -1;
    private int period = 1;
    private int closeAfter = -1;
    private int loadDelay = -1;
    private int loadTitle = -1;

    private boolean backward;
    private boolean ignoreManualItems;
    private boolean clearAndSafe;
    private boolean permanentCache;
    private boolean keepOriginal;
    private boolean closeAble = true;
    private boolean transferData = true;

    @Getter(AccessLevel.PROTECTED)
    private boolean updateTask = true;

    @NotNull
    private Component titleHolder = Component.text("Loading", NamedTextColor.YELLOW).decorate(TextDecoration.ITALIC)
            .append(Component.text("...", NamedTextColor.DARK_GRAY));

    @NotNull
    private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;

    private List<InventoryOptions> options = new ArrayList<>();
    private List<DisabledInventoryClick> ignoreClickEvent = new ArrayList<>();
    private List<CloseReason> closeReasons = new ArrayList<>();
    private List<EventCreator<? extends Event>> events = new CopyOnWriteArrayList<>();
    private List<IntelligentItemNameAnimator> itemAnimator = new CopyOnWriteArrayList<>();
    private List<IntelligentMaterialAnimator> materialAnimator = new CopyOnWriteArrayList<>();
    private List<IntelligentTitleAnimator> titleAnimator = new CopyOnWriteArrayList<>();
    private List<IntelligentItemLoreAnimator> loreAnimator = new CopyOnWriteArrayList<>();
    private List<Action> enabledActions = new ArrayList<>();
    private List<DisabledEvents> disabledEvents = new ArrayList<>();
    private List<Page> pages = new CopyOnWriteArrayList<>();
    protected final List<Player> delayed = new ArrayList<>();

    private final HashMap<UUID, ItemStack[]> playerInventory = new HashMap<>();
    private HashMap<Integer, Consumer<InventoryClickEvent>> ignoredSlotsWithEvents = new HashMap<>();

    /**
     * Copy constructor
     * <br>
     * See also: {@link #newInstance()}
     */
    private RyseInventory(@NotNull RyseInventory inventory) {
        this.manager = inventory.manager;
        this.provider = inventory.provider;
        this.anvilGUIBuilder = inventory.anvilGUIBuilder;
        this.permanentCache = inventory.permanentCache;
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
        this.updateTask = inventory.updateTask;
        this.transferData = inventory.transferData;
        this.backward = inventory.backward;
        this.titleHolder = inventory.titleHolder;
        this.inventoryOpenerType = inventory.inventoryOpenerType;
        this.ignoredSlotsWithEvents = inventory.ignoredSlotsWithEvents;
        this.fixedPageSize = inventory.fixedPageSize;
        this.ignoreManualItems = inventory.ignoreManualItems;
        this.enabledActions.addAll(inventory.enabledActions);
        this.disabledEvents.addAll(inventory.disabledEvents);
        this.delayed.addAll(inventory.delayed);
        this.options.addAll(inventory.options);
        this.events.addAll(inventory.events);
        this.ignoreClickEvent.addAll(inventory.ignoreClickEvent);
        this.closeReasons.addAll(inventory.closeReasons);
        this.itemAnimator.addAll(inventory.itemAnimator);
        this.materialAnimator.addAll(inventory.materialAnimator);
        this.titleAnimator.addAll(inventory.titleAnimator);
        this.loreAnimator.addAll(inventory.loreAnimator);
        this.playerInventory.putAll(inventory.playerInventory);
        this.pages.addAll(inventory.pages);
        this.keepOriginal = inventory.keepOriginal;
        this.originalInventory = inventory;

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
        inventory.manager = manager;

        inventory.clearAndSafe = (boolean) data.get("clear-and-safe");
        inventory.title = SERIALIZER.deserialize((String) data.get("title"));
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
        inventory.titleHolder = SERIALIZER.deserialize((String) data.get("title-holder"));
        inventory.inventoryOpenerType = InventoryOpenerType.valueOf((String) data.get("inventory-opener-type"));
        inventory.options = (List<InventoryOptions>) data.get("options");
        inventory.events = (List<EventCreator<? extends Event>>) data.get("events");
        inventory.ignoreClickEvent = (List<DisabledInventoryClick>) data.get("ignore-click-event");
        inventory.closeReasons = (List<CloseReason>) data.get("close-reasons");
        inventory.itemAnimator = (List<IntelligentItemNameAnimator>) data.get("item-animator");
        inventory.materialAnimator = (List<IntelligentMaterialAnimator>) data.get("material-animator");
        inventory.titleAnimator = (List<IntelligentTitleAnimator>) data.get("title-animator");
        inventory.loreAnimator = (List<IntelligentItemLoreAnimator>) data.get("lore-animator");
        inventory.ignoredSlotsWithEvents = (HashMap<Integer, Consumer<InventoryClickEvent>>) data.get("ignored-slots");
        inventory.disabledEvents = (List<DisabledEvents>) data.get("disabled-events");
        inventory.enabledActions = (List<Action>) data.get("enabled-actions");
        inventory.provider = (InventoryProvider) data.get("provider");
        inventory.identifier = data.get("identifier");
        inventory.plugin = Bukkit.getPluginManager().getPlugin((String) data.get("plugin"));
        inventory.fixedPageSize = (int) data.get("fixed-page-size");
        inventory.ignoreManualItems = (boolean) data.get("ignore-manual-items");
        inventory.pages = (List<Page>) data.get("pages");
        inventory.permanentCache = (boolean) data.get("permanent-cache");
        if (data.containsKey("original-inventory")) {
            inventory.originalInventory = deserialize((Map<String, Object>) data.get("original-inventory"), manager);
        }
        inventory.keepOriginal = (boolean) data.get("keep-original");

        return inventory;
    }

    /**
     * Serializes the inventory to a map.
     *
     * @return The serialized inventory.
     * @see #deserialize(Map, InventoryManager) to get back the inventory from the hashmap.
     */
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        if (this.originalInventory != null) {
            map.put("original-inventory", this.originalInventory.serialize());
        }
        map.put("keep-original", this.keepOriginal);
        map.put("permanent-cache", this.permanentCache);
        map.put("title", SERIALIZER.serialize(this.title));
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
        map.put("title-holder", SERIALIZER.serialize(this.titleHolder));
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
        map.put("ignored-slots", this.ignoredSlotsWithEvents);
        map.put("disabled-events", this.disabledEvents);
        map.put("enabled-actions", this.enabledActions);
        map.put("fixed-page-size", this.fixedPageSize);
        map.put("ignore-manual-items", this.ignoreManualItems);
        map.put("pages", this.pages);

        return map;
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
     * @return empty if no animation with the ID could be found.
     * <p>
     * Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentItemLoreAnimator> getLoreAnimation(@NotNull Object identifier) {
        return this.loreAnimator.stream()
                .filter(animator -> Objects.equals(animator.getIdentifier(), identifier))
                .findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return empty if no animation with the ID could be found.
     * <p>
     * Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentItemNameAnimator> getNameAnimation(@NotNull Object identifier) {
        return this.itemAnimator.stream()
                .filter(animator -> Objects.equals(animator.getIdentifier(), identifier))
                .findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return empty if no animation with the ID could be found.
     * <p>
     * Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentTitleAnimator> getTitleAnimation(@NotNull Object identifier) {
        return this.titleAnimator.stream()
                .filter(animator -> Objects.equals(animator.getIdentifier(), identifier))
                .findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return empty if no animation with the ID could be found.
     * <p>
     * Only works if the animation has also been assigned an identifier.
     */
    public @NotNull Optional<IntelligentMaterialAnimator> getMaterialAnimator(@NotNull Object identifier) {
        return this.materialAnimator.stream()
                .filter(animator -> Objects.equals(animator.getIdentifier(), identifier))
                .findFirst();
    }

    /**
     * Adjusts the period of the scheduler.
     *
     * @param time    Time
     * @param setting Set your own time type.
     */
    public void updatePeriod(@Nonnegative int time, @NotNull TimeSetting setting) {
        this.period = TimeUtils.buildTime(time, setting);
    }

    /**
     * Adjusts the delay of the scheduler.
     *
     * @param time    Time
     * @param setting Set your own time type.
     */
    public void updateDelay(@Nonnegative int time, @NotNull TimeSetting setting) {
        this.delay = TimeUtils.buildTime(time, setting);
    }

    /**
     * Closes the inventory from the player. InventoryClickEvent is no longer called here.
     *
     * @param player The player which inventory should be closed.
     */
    public void close(@NotNull Player player) {
        RyseInventoryPreCloseEvent preCloseEvent = new RyseInventoryPreCloseEvent(player, this);
        Bukkit.getPluginManager().callEvent(preCloseEvent);

        if (preCloseEvent.isCancelled()) return;

        this.paginationCache = null;
        removeActiveAnimations();
        clearData(player);
        player.closeInventory();

        RyseInventoryCloseEvent closeEvent = new RyseInventoryCloseEvent(player, this);
        Bukkit.getPluginManager().callEvent(closeEvent);
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
     */
    public void open(@NotNull Player player) {
        open(player, 1);
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
     * @param page    The page to open.
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
     */
    public void open(@NotNull Player player, @Nonnegative int page) {
        Bukkit.getScheduler().runTask(this.plugin, () -> initInventory(player, page, null, null));
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @param keys   The keys
     * @param values The values
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public void open(@NotNull Player player, @Nonnegative int page, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        Bukkit.getScheduler().runTask(this.plugin, () -> initInventory(player, page, keys, values));
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @param data   The predefined data
     */
    public void open(@NotNull Player player, @Nonnegative int page, @NotNull Map<String, Object> data) {
        String[] keys = data.keySet().toArray(new String[0]);
        Object[] values = data.values().toArray();

        Bukkit.getScheduler().runTask(this.plugin, () -> initInventory(player, page, keys, values));
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param keys   The keys
     * @param values The values
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public void open(@NotNull Player player, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, StringConstants.INVALID_OBJECT);

        Bukkit.getScheduler().runTask(this.plugin, () -> initInventory(player, 1, keys, values));
    }

    /**
     * Opens an inventory with a specific page and defined properties.
     *
     * @param player The player where the inventory should be opened.
     * @param data   The predefined data
     */
    public void open(@NotNull Player player, @NotNull Map<String, Object> data) {
        String[] keys = data.keySet().toArray(new String[0]);
        Object[] values = data.values().toArray();

        Bukkit.getScheduler().runTask(this.plugin, () -> initInventory(player, 1, keys, values));
    }

    /**
     * Allows the inventory to be closed even after it has been opened.
     */
    public void allowClose() {
        this.closeAble = true;
    }

    /**
     * Allows the inventory not to be closed even after it has been opened.
     */
    public void preventClose() {
        this.closeAble = false;
    }

    private void initInventory(@NotNull Player player, @Nonnegative int page, @Nullable String[] keys, @Nullable Object[] values) {
        if (!manager.canOpen(player.getUniqueId())) {
            int finalPage = page;
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> initInventory(player, finalPage, keys, values), 2);
            return;
        }

        this.manager.setLastOpen(player.getUniqueId());

        RyseInventoryPreOpenEvent event = new RyseInventoryPreOpenEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        if (!equals(event.getInventory())) {
            event.getInventory().open(player);
            return;
        }

        finishSavedInventory(player);
        removeActiveAnimations();

        clearInventoryWhenNeeded(player);

        page--;

        this.inventory = setupInventory(page);

        InventoryContents contents = new InventoryContents(player, this, this.plugin);
        Optional<InventoryContents> optional = this.manager.getContents(player.getUniqueId());

        contents.pagination().setPage(page);

        transferData(optional.orElse(null), contents, keys, values);
        setupData(player, contents);
        initProvider(player, contents);

        if (optional.isPresent() && optional.get().equals(contents)) return;

        Pagination pagination = contents.pagination();

        checkIfIllegalPaginationData(pagination);

        this.manager.stopUpdate(player.getUniqueId());

        loadByPage(contents);

        if (page > pagination.lastPage()) {
            close(player);
            throw new IllegalArgumentException("There is no " + page + " side. Last page is " + pagination.lastPage());
        }

        loadDelay(page, pagination, player);
        closeInventoryWhenEnabled(player);

        finalizeInventoryAndOpen(player, contents);
    }

    /**
     * Opens the original inventory to the player.
     *
     * @param player The player to whom the original inventory should be opened.
     * @throws IllegalStateException if the original inventory is not saved.
     */
    public void restoreOriginal(@NotNull Player player) throws IllegalStateException {
        if (!this.keepOriginal) {
            throw new IllegalStateException("To save the original inventory, use the #keepOriginal method in the Builder!");
        }

        this.originalInventory.open(player);
    }

    /**
     * Öffnet das originale Inventar für alle Spieler die aktuell dieses Inventar offen haben
     *
     * @throws IllegalStateException if the original inventory is not saved.
     */
    public void restoreOriginal() throws IllegalStateException {
        if (!this.keepOriginal) {
            throw new IllegalStateException("To save the original inventory, use the #keepOriginal method in the Builder!");
        }

        getOpenedPlayers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> this.originalInventory.open(player));
    }

    /**
     * Get an EventCreator object based on the Event class.
     *
     * @param event The event what you want to get
     * @return null if there is no custom event matching the event class
     */
    public @Nullable EventCreator<? extends Event> getEvent(@NotNull Class<? extends Event> event) {
        return this.events.stream().filter(eventOne -> event == eventOne.getClazz())
                .findFirst()
                .orElse(null);
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param player   The Player
     * @param newTitle The new title
     * @author <a href="https://www.spigotmc.org/threads/change-inventory-title-reflection-1-8-1-18.489966/">Original code (Slightly Modified)</a>
     */
    public void updateTitle(@NotNull Player player, @NotNull String newTitle) {
        updateTitle(player, Component.text(newTitle));
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param player   The Player
     * @param newTitle The new title
     * @author <a href="https://www.spigotmc.org/threads/change-inventory-title-reflection-1-8-1-18.489966/">Original code (Slightly Modified)</a>
     */
    public void updateTitle(@NotNull Player player, @NotNull Component newTitle) {
        RyseInventoryTitleChangeEvent event = new RyseInventoryTitleChangeEvent(player, this.title, newTitle);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        TitleUpdater.updateInventory(player, event.getNewTitle());
    }

    /**
     * @param contents The contents
     * @return the size of the inventory.
     */
    @Nonnegative
    public int size(@NotNull InventoryContents contents) {
        return size(contents, contents.pagination().page() - 1);
    }

    /**
     * @return inventory title
     */
    public @NotNull String getTitle() {
        return SERIALIZER.serialize(this.title);
    }

    /**
     * @return inventory title
     */
    public @NotNull Component title() {
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
     * Returns true if the window is closeable, false otherwise.
     *
     * @return The closeAble variable is being returned.
     */
    public boolean isCloseAble() {
        return this.closeAble;
    }

    /**
     * Returns whether or not the plugin should ignore manual items
     *
     * @return A boolean value.
     */
    public boolean isIgnoreManualItems() {
        return this.ignoreManualItems;
    }

    /**
     * It returns a list of DisabledInventoryClick objects
     *
     * @return A list of DisabledInventoryClick objects.
     */
    public @NotNull List<DisabledInventoryClick> getIgnoreClickEvent() {
        return this.ignoreClickEvent;
    }

    /**
     * Returns a list of slots that are ignored by the plugin.
     *
     * @return A list of integers.
     */
    public @NotNull HashMap<Integer, Consumer<InventoryClickEvent>> getIgnoredSlots() {
        return ignoredSlotsWithEvents;
    }

    /**
     * Returns a list of enabled actions.
     *
     * @return A list of enabled actions.
     */
    public @NotNull List<Action> getEnabledActions() {
        return enabledActions;
    }

    /**
     * This function returns a list of disabled events
     *
     * @return A list of DisabledEvents objects.
     */
    public @NotNull List<DisabledEvents> getDisabledEvents() {
        return disabledEvents;
    }

    /**
     * @return the ID from the inventory
     * <p>
     * You have to give the inventory itself an ID with {@link Builder#identifier(Object)}
     */
    public @Nullable Object getIdentifier() {
        return this.identifier;
    }

    /**
     * Returns the type of inventory opener that opened this inventory.
     *
     * @return The inventoryOpenerType
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
     * Returns a list of all the options that are available for this inventory.
     *
     * @return A list of InventoryOptions
     */
    public @NotNull List<InventoryOptions> getOptions() {
        return options;
    }

    /**
     * This function returns the loadDelay variable
     *
     * @return The loadDelay variable is being returned.
     */
    public int getLoadDelay() {
        return loadDelay;
    }

    /**
     * @return The slideAnimator object.
     */
    @ApiStatus.Internal
    public @Nullable SlideAnimation getSlideAnimator() {
        return this.slideAnimator;
    }

    /**
     * If the slideAnimator is not null, then for each task in the slideAnimator, if the task is queued, then increment the
     * counter.
     *
     * @return The number of active slide animator tasks.
     */
    protected @Nonnegative int activeSlideAnimatorTasks() {
        if (this.slideAnimator == null) return 0;
        AtomicInteger counter = new AtomicInteger();

        this.slideAnimator.getTasks().forEach(task -> {
            if (!Bukkit.getScheduler().isQueued(task.getTaskId())) return;
            counter.getAndIncrement();
        });
        return counter.get();
    }

    /**
     * This function returns a list of close reasons
     *
     * @return A list of CloseReason objects.
     */
    protected @NotNull List<CloseReason> getCloseReasons() {
        return closeReasons;
    }

    /**
     * Returns the InventoryManager instance that this Inventory is associated with.
     *
     * @return The InventoryManager object.
     */
    @ApiStatus.Internal
    public @NotNull InventoryManager getManager() {
        return manager;
    }

    /**
     * Sets the fixed page size for the query
     *
     * @param fixedPageSize How many pages the player can open even though no items are set there.
     */
    @ApiStatus.Internal
    public void setFixedPageSize(@Nonnegative int fixedPageSize) {
        this.fixedPageSize = fixedPageSize;
    }

    /**
     * If the page number is not in the pages list, throw an exception. Otherwise, return the number of rows in the page.
     *
     * @param contents   The InventoryContents object that is passed to the InventoryProvider.
     * @param pageNumber The page number of the inventory.
     * @return The size of the inventory.
     */
    @Nonnegative
    private int size(@NotNull InventoryContents contents,
                     @Nonnegative int pageNumber) {
        if (!this.pages.isEmpty() && this.size == -1) {
            return this.pages.stream()
                    .filter(page -> page.page() == pageNumber)
                    .findFirst()
                    .map(page -> page.rows() * 9)
                    .orElseThrow(() -> new IllegalArgumentException("There is no page with the number " + pageNumber));
        }

        return this.size;
    }

    /**
     * If the page size is -1, then return the highest page number in the pages list, otherwise return the last page
     * number.
     *
     * @param contents The InventoryContents object that contains all the information about the inventory.
     * @return The last page of the pagination.
     */
    private int lastPage(@NotNull InventoryContents contents) {
        if (!this.pages.isEmpty() && this.size == -1) {
            return this.pages.stream()
                    .mapToInt(Page::page)
                    .max()
                    .orElseThrow(() -> new IllegalArgumentException("There is no page with the number " + contents.pagination().page()));
        }

        return contents.pagination().lastPage();
    }

    /**
     * If the privateInventory map contains the UUID, return the inventory associated with it
     *
     * @param uuid The UUID of the player you want to get the inventory of.
     * @return An optional of the inventory.
     */
    @ApiStatus.Internal
    public @NotNull Optional<Inventory> inventoryBasedOnOption(@NotNull UUID uuid) {
        if (this.anvilGUI != null)
            return Optional.of(this.anvilGUI.getInventory());

        return Optional.ofNullable(this.inventory);
    }

    /**
     * It checks if the object is equal to the object that is being compared to.
     *
     * @param o The object to compare to.
     * @return The hashcode of the object.
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RyseInventory)) return false;
        RyseInventory that = (RyseInventory) o;

        return clearAndSafe == that.clearAndSafe && size == that.size && delay == that.delay && openDelay == that.openDelay && period == that.period && closeAfter == that.closeAfter && loadDelay == that.loadDelay && loadTitle == that.loadTitle && closeAble == that.closeAble && transferData == that.transferData && Objects.equals(title, that.title) && Objects.equals(slideAnimator, that.slideAnimator) && Objects.equals(identifier, that.identifier) && Objects.equals(titleHolder, that.titleHolder) && inventoryOpenerType == that.inventoryOpenerType && Objects.equals(options, that.options) && Objects.equals(events, that.events) && Objects.equals(ignoreClickEvent, that.ignoreClickEvent) && Objects.equals(closeReasons, that.closeReasons);
    }

    /**
     * Loads the items of the given page into the inventory of the given player.
     *
     * @param pagination The Pagination object that you created.
     * @param player     The player who's viewing the inventory
     * @param page       The page number to load
     */
    @ApiStatus.Internal
    public void load(@NotNull Pagination pagination,
                     @NotNull Player player,
                     @Nonnegative int page) {
        pagination.getDataByPage(page)
                .forEach(item -> placeItem(player, item.getModifiedSlot(), item.getItem()));
    }

    /**
     * If the player can see the item, place it in the inventory.
     *
     * @param player The player who is viewing the inventory.
     * @param slot   The slot to place the item in.
     * @param item   The item to place in the inventory.
     */
    private void placeItem(@NotNull Player player,
                           int slot,
                           @NotNull IntelligentItem item) {
        if (slot == -1) {
            throw new IllegalArgumentException("An attempt is made to place an item in slot -1. Check your pagination. If you could determine that it is not your error, report it on Github.");
        }

        if (this.inventory != null)
            if (slot >= this.inventory.getSize()) return;

        if (!item.isCanSee()) {
            item.getError().cantSee(player, item);
            return;
        }

        if (this.inventory != null)
            this.inventory.setItem(slot, item.getItemStack());
    }

    /**
     * If the closeAfter variable is not -1, and the closeAble variable is true, then close the inventory after the
     * closeAfter variable amount of ticks
     *
     * @param player The player who will be closing the inventory.
     * @throws IllegalStateException if the closeAble variable is false.
     */
    private void closeInventoryWhenEnabled(@NotNull Player player) throws IllegalStateException {
        if (this.closeAfter == -1) return;
        if (!this.closeAble)
            throw new IllegalStateException("The #closeAfter() method could not be executed because you have forbidden closing the inventory by #preventClose.");

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> close(player), this.closeAfter);
    }

    /**
     * It removes all the active animations
     */
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

    /**
     * This function sets the backward variable to true.
     */
    protected void setBackward() {
        this.backward = true;
    }

    /**
     * If the player has a saved inventory, remove it and restore the player's inventory
     *
     * @param player The player who's inventory is being saved.
     */
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

    /**
     * Save the player's inventory and clear it
     *
     * @param player The player who's inventory is being cleared.
     */
    private void clearInventoryWhenNeeded(@NotNull Player player) {
        if (!this.clearAndSafe) return;

        this.playerInventory.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
    }

    /**
     * It creates an inventory with the title of the menu and the size of the menu
     *
     * @param pageNumber The page number that the inventory is being opened for.
     * @return An Inventory
     */
    private @NotNull Inventory setupInventory(@Nonnegative int pageNumber) {
        int finalSize = this.size;

        if (finalSize == -1 && !this.pages.isEmpty()) {
            Page finalPage = this.pages.stream()
                    .filter(page -> page.page() == pageNumber)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("You seem to be using the #rows(Page) method, unfortunately no data could be found for page " + pageNumber));

            finalSize = finalPage.rows() * 9;
        }

        String title = buildTitle();
        if (this.inventoryOpenerType == InventoryOpenerType.CHEST) {
            return Bukkit.createInventory(null, finalSize, title);
        }

        return inventory = Bukkit.createInventory(null, this.inventoryOpenerType.getType(), title);
    }

    /**
     * If the title is not loaded, return the title, otherwise return the title holder.
     *
     * @return The title of the book.
     */
    @Contract(pure = true)
    private @NotNull String buildTitle() {
        if (this.loadTitle == -1)
            return SERIALIZER.serialize(this.title);

        return SERIALIZER.serialize(this.titleHolder);
    }

    /**
     * It transfers data from the old inventory to the new inventory
     *
     * @param oldContents The old InventoryContents object.
     * @param newContents The new InventoryContents object that will be used for the new inventory.
     * @param keys        The keys to transfer data from the old inventory to the new inventory.
     * @param values      The values that will be passed to the new inventory.
     */
    private void transferData(@Nullable InventoryContents oldContents,
                              @NotNull InventoryContents newContents,
                              @Nullable String[] keys,
                              @Nullable Object[] values) {
        if (oldContents != null) {
            for (IntelligentItemData item : oldContents.pagination().getInventoryData()) {
                if (!item.isTransfer()) continue;
                ItemStack itemStack = item.getItem().getItemStack();
                itemStack.setAmount(item.getAmount());

                item.getItem().update(itemStack);

                newContents.pagination().addInventoryData(item);
            }
        }

        if (this.transferData && oldContents != null)
            oldContents.transferData(newContents);

        if (keys != null && values != null) {
            for (int n = 0; n < keys.length; n++) {
                String key = keys[n];
                Object value = values[n];
                if (key == null || value == null) continue;

                newContents.setProperty(key, value);
            }
        }
    }

    /**
     * It sets the contents of the inventory to the contents of the inventory that the player is viewing
     *
     * @param player   The player who is viewing the inventory.
     * @param contents The InventoryContents object that contains the inventory's contents.
     */
    private void setupData(@NotNull Player player,
                           @NotNull InventoryContents contents) {
        this.manager.setContents(player.getUniqueId(), contents);
    }

    /**
     * If the slideAnimator is null, then the provider is initialized with the player and contents. If the slideAnimator is
     * not null, then the provider is initialized with the player, contents, and slideAnimator
     *
     * @param player   The player who is viewing the inventory.
     * @param contents The InventoryContents object that you can use to set items in the inventory.
     */
    private void initProvider(@NotNull Player player,
                              @NotNull InventoryContents contents) {
        if (this.inventoryOpenerType == InventoryOpenerType.ANVIL) {
            this.anvilGUIBuilder = new AnvilGUI.Builder().title(buildTitle());
            this.provider.anvil(player, this.anvilGUIBuilder);
            return;
        }
        if (this.slideAnimator == null) {
            this.provider.init(player, contents);
            return;
        }
        this.provider.init(player, contents, this.slideAnimator);
    }

    /**
     * It loads the inventory with a delay
     *
     * @param page       The page number to load
     * @param pagination The pagination object that you want to load.
     * @param player     The player who is viewing the inventory
     */
    private void loadDelay(@Nonnegative int page,
                           @NotNull Pagination pagination,
                           @NotNull Player player) {
        if (this.loadDelay != -1) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> load(pagination, player, page), this.loadDelay);
        } else {
            load(pagination, player, page);
        }

        if (this.loadTitle != -1)
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> updateTitle(player, this.title), this.loadTitle);
    }


    /**
     * You cannot use Pagination#setItemsPerPage and SlotIterator#endPosition together. Choose one of them.
     *
     * @param pagination The pagination object that contains the pagination data.
     */
    private void checkIfIllegalPaginationData(@NotNull Pagination pagination) {
        if (pagination.getSlotIterator() != null
                && pagination.isCalledItemsPerPage()
                && pagination.getSlotIterator().getEndPosition() != -1) {
            throw new IllegalArgumentException("You cannot use Pagination#setItemsPerPage and SlotIterator#endPosition together. Choose one of them.");
        }
    }

    /**
     * It opens the inventory
     *
     * @param player The player who will open the inventory.
     */
    private void finalizeInventoryAndOpen(@NotNull Player player, @NotNull InventoryContents contents) {
        if (this.keepOriginal)
            this.originalInventory = this;

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.openDelay == -1 || this.delayed.contains(player)) {
                openInventory(player, contents);
            } else {
                if (!this.delayed.contains(player)) {
                    Bukkit.getScheduler().runTaskLater(this.plugin, () ->
                            openInventory(player, contents), this.openDelay);
                    this.delayed.add(player);
                }
            }
        });
    }

    /**
     * It calls the RyseInventoryOpenEvent event
     *
     * @param player The player who opened the inventory
     */
    private void callOpenEvent(@NotNull Player player) {
        RyseInventoryOpenEvent event = new RyseInventoryOpenEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * It opens the inventory, invokes the scheduler, and sets the inventory
     *
     * @param player The player who will open the inventory.
     */
    private void openInventory(@NotNull Player player, @NotNull InventoryContents contents) {
        if (Objects.requireNonNull(inventory).getType() == InventoryType.ANVIL) {
            this.anvilGUI = this.anvilGUIBuilder.open(player);
        } else {
            player.openInventory(inventory);
        }

        this.manager.invokeScheduler(player, this);
        this.manager.setInventory(player.getUniqueId(), this);

        callOpenEvent(player);
    }

    /**
     * If the inventory opener type is not a chest or ender chest, and the pattern is not null, throw an exception.
     *
     * @param pattern The pattern to use for the SlotIterator.
     */
    private void checkIfInventoryTypeIsValid(@Nullable SlotIteratorPattern pattern) {
        if (this.inventoryOpenerType != InventoryOpenerType.CHEST
                && this.inventoryOpenerType != InventoryOpenerType.ENDER_CHEST
                && pattern != null) {
            throw new IllegalStateException("SlotIterator with PatternBuilder is not supported for InventoryOpenerType " + this.inventoryOpenerType.getType().toString());
        }
    }

    /**
     * Get the item in the given slot on the given page.
     * <p>
     * The first thing you'll notice is that the function is private. This is because it's only used internally by the
     * class
     *
     * @param inventoryData The list of IntelligentItemData objects that are stored in the inventory.
     * @param slot          The slot in the inventory that you want to get the item from.
     * @param page          The page of the inventory.
     * @return The item in the slot and page.
     */
    private @Nullable IntelligentItem get(@NotNull List<IntelligentItemData> inventoryData,
                                          @Nonnegative int slot,
                                          @Nonnegative int page) {
        for (IntelligentItemData data : inventoryData) {
            if (data.getPage() == page && data.getModifiedSlot() == slot)
                return data.getItem();
        }
        return null;
    }

    /**
     * If the iterator type is horizontal, increment the slot by one. If the iterator type is vertical, increment the slot
     * by nine.
     *
     * @param type      The type of iterator you want to use.
     * @param slot      The current slot being iterated over.
     * @param startSlot The slot to start at.
     * @param contents  The InventoryContents object that you're iterating over.
     * @return The next slot to be iterated over.
     */
    private int[] updateForNextSlot(@NotNull SlotIterator.SlotIteratorType type,
                                    @Nonnegative int slot,
                                    @Nonnegative int startSlot,
                                    @Nonnegative int page,
                                    @NotNull InventoryContents contents) {
        int pageSize = size(contents, page);

        if (type == SlotIterator.SlotIteratorType.HORIZONTAL)
            return new int[]{++slot};

        if (slot + 9 >= pageSize)
            return new int[]{++startSlot, 1};

        return new int[]{9 + slot};
    }

    /**
     * It removes the player's inventory from the plugin's memory
     *
     * @param player The player who's inventory is being cleared.
     */
    protected void clearData(@NotNull Player player) {
        if (this.playerInventory.containsKey(player.getUniqueId())) {
            ItemStack[] data = this.playerInventory.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> player.getInventory().setContents(data), 2);
        }

        this.delayed.remove(player);
        this.manager.removeInventoryFromPlayer(player.getUniqueId());
    }

    /**
     * Adds an item animator to the list of item animators.
     *
     * @param animator The animator to add.
     */
    @ApiStatus.Internal
    public void addItemAnimator(@NotNull IntelligentItemNameAnimator animator) {
        this.itemAnimator.add(animator);
    }

    /**
     * Adds an IntelligentMaterialAnimator to the list of IntelligentMaterialAnimators.
     *
     * @param animator The IntelligentMaterialAnimator to add.
     */
    @ApiStatus.Internal
    public void addMaterialAnimator(@NotNull IntelligentMaterialAnimator animator) {
        this.materialAnimator.add(animator);
    }

    /**
     * It removes an IntelligentMaterialAnimator from the list of animators, and if the animator is currently running, it
     * cancels the task
     *
     * @param animator The IntelligentMaterialAnimator to remove.
     */
    @ApiStatus.Internal
    public void removeMaterialAnimator(@NotNull IntelligentMaterialAnimator animator) {
        this.materialAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    /**
     * It removes an item animator from the list of item animators
     *
     * @param animator The IntelligentItemNameAnimator to remove.
     */
    @ApiStatus.Internal
    public void removeItemAnimator(@NotNull IntelligentItemNameAnimator animator) {
        this.itemAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    /**
     * Adds an IntelligentTitleAnimator to the list of IntelligentTitleAnimators.
     *
     * @param animator The animator to add.
     */
    @ApiStatus.Internal
    public void addTitleAnimator(@NotNull IntelligentTitleAnimator animator) {
        this.titleAnimator.add(animator);
    }

    /**
     * It removes a title animator from the list of title animators
     *
     * @param animator The IntelligentTitleAnimator to remove.
     */
    @ApiStatus.Internal
    public void removeTitleAnimator(@NotNull IntelligentTitleAnimator animator) {
        this.titleAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    /**
     * Adds an IntelligentItemLoreAnimator to the list of lore animators
     *
     * @param animator The animator to add.
     */
    @ApiStatus.Internal
    public void addLoreAnimator(@NotNull IntelligentItemLoreAnimator animator) {
        this.loreAnimator.add(animator);
    }

    /**
     * It removes the animator from the list of animators, and cancels all of the tasks that the animator has
     *
     * @param animator The animator to remove.
     */
    @ApiStatus.Internal
    public void removeLoreAnimator(@NotNull IntelligentItemLoreAnimator animator) {
        this.loreAnimator.remove(animator);

        animator.getTasks().forEach(bukkitTask -> {
            if (!Bukkit.getScheduler().isQueued(bukkitTask.getTaskId())) return;
            bukkitTask.cancel();
        });
    }

    /**
     * If the slideAnimator is null, return. If the slideAnimator is not null, get all the tasks in the slideAnimator and
     * for each task, if the task is queued, cancel it
     */
    protected void removeSlideAnimator() {
        if (this.slideAnimator == null) return;

        this.slideAnimator.getTasks().forEach(bukkitTask -> {
            if (!Bukkit.getScheduler().isQueued(bukkitTask.getTaskId())) return;
            bukkitTask.cancel();
        });
        this.slideAnimator.clearTasks();
    }

    /**
     * It takes a list of items, and places them in a paginated inventory
     *
     * @param contents The InventoryContents object that contains all the information about the inventory.
     */
    @ApiStatus.Internal
    public void loadByPage(@NotNull InventoryContents contents) {
        Pagination pagination = contents.pagination();
        SlotIterator iterator = contents.iterator();

        if (iterator == null) return;

        SlotIterator.SlotIteratorType type = iterator.getType();
        SlotIteratorPattern pattern = iterator.getPatternBuilder();

        checkIfInventoryTypeIsValid(pattern);

        int itemsSet = 0;
        int page = 0;
        int startSlot = iterator.getSlot();
        List<IntelligentItemData> data = contents.pagination().getInventoryData();

        if (pattern != null) {
            applyPattern(pagination, iterator, pattern, data, contents, itemsSet, page, startSlot);
            return;
        }

        applyStandardPagination(contents, pagination, iterator, data, startSlot);

        contents.pagination().setInventoryData(data);
    }

    /**
     * It applies the standard pagination algorithm to the given data
     *
     * @param contents   The InventoryContents object that is passed to the Pagination object.
     * @param pagination The pagination object that is being used.
     * @param iterator   The slot iterator that is being used.
     * @param data       The list of items to be paginated.
     * @param startSlot  The slot to start at.
     */
    private void applyStandardPagination(@NotNull InventoryContents contents,
                                         @NotNull Pagination pagination,
                                         @NotNull SlotIterator iterator,
                                         @NotNull List<IntelligentItemData> data,
                                         int startSlot) {

        PaginationData paginationData = this.paginationCache == null
                ? findAllEmptySlots(contents, iterator, data.size())
                : this.paginationCache.newInstance();

        if (this.paginationCache == null)
            this.paginationCache = paginationData.newInstance();

        for (int i = 0; i < data.size(); i++) {
            IntelligentItemData itemData = data.get(i);
            if (itemData.getModifiedSlot() != -1) continue;

            int slot = paginationData.getFirstSlot();
            int page = paginationData.getFirstPage();

            if (slot == -1 || page == -1)
                continue;

            pagination.remove(slot, page);

            itemData.setPage(page);
            itemData.setModifiedSlot(slot);
        }
    }

    private @NotNull PaginationData findAllEmptySlots(@NotNull InventoryContents contents,
                                                      @NotNull SlotIterator iterator,
                                                      @Nonnegative int requiredSlots) {
        PaginationData data = new PaginationData();

        int itemsPerPage = contents.pagination().getItemsPerPage();

        int slot = iterator.getSlot();
        int page = 0;
        int slotsFound = 0;

        while (requiredSlots > 0) {
            if ((!iterator.isOverride() && contents.getWithinPage(slot, page).isPresent()) || iterator.getBlackListInternal().contains(slot)) {
                slot++;
                continue;
            }

            if ((slotsFound >= itemsPerPage && iterator.getEndPosition() == -1) || (slot > iterator.getEndPosition() && iterator.getEndPosition() != -1)) {
                slotsFound = 0;
                page++;
                slot = iterator.getSlot();
            }

            data.add(slot, page);

            requiredSlots--;
            slotsFound++;
            slot++;
        }

        return data;
    }

    /**
     * It applies the pattern to the inventory
     *
     * @param pagination The pagination object that contains the items per page, the page, and the inventory.
     * @param iterator   The iterator that will be used to iterate through the slots.
     * @param pattern    The pattern that will be applied to the inventory.
     * @param data       The list of IntelligentItemData objects that are being applied to the inventory.
     * @param itemsSet   The amount of items that have been set on the current page.
     * @param page       The page number of the inventory
     * @param startSlot  The slot where the pagination starts.
     */
    private void applyPattern(@NotNull Pagination pagination,
                              @NotNull SlotIterator iterator,
                              @NotNull SlotIteratorPattern pattern,
                              @NotNull List<IntelligentItemData> data,
                              @NotNull InventoryContents contents,
                              @Nonnegative int itemsSet,
                              @Nonnegative int page,
                              int startSlot) {
        int slot = startSlot;
        if (startSlot == -1) {
            startSlot = 0;
            slot = startSlot;
        }

        List<String> lines = pattern.getLines();

        do {
            for (int j = 0; j < lines.size(); j++) {
                String line = lines.get(j);
                char[] charArray = line.toCharArray();
                for (char lineChar : charArray) {
                    if ((itemsSet >= pagination.getItemsPerPage() && iterator.getEndPosition() == -1)
                            || slot > pagination.inventory().size(contents) - 1
                            || (slot >= iterator.getEndPosition() && iterator.getEndPosition() != -1)) {
                        itemsSet = 0;
                        slot = startSlot;
                        page++;
                        j = -1;
                        break;
                    }

                    if (lineChar != pattern.getAttachedChar()) {
                        slot++;
                        continue;
                    }

                    if (get(data, slot, page) != null && !iterator.isOverride()) {
                        slot++;
                        continue;
                    }

                    List<IntelligentItemData> dataList = data.stream()
                            .filter(item -> item.getModifiedSlot() == -1)
                            .collect(Collectors.toList());

                    if (dataList.isEmpty())
                        break;

                    IntelligentItemData itemData = dataList.get(0);

                    int index = data.indexOf(itemData);

                    itemData.setPage(page);
                    itemData.setModifiedSlot(slot);

                    data.set(index, itemData);
                    itemsSet++;
                    slot++;
                }
            }
        } while (data.stream().anyMatch(item -> item.getModifiedSlot() == -1));
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
            this.ryseInventory.fixedPageSize = builder.ryseInventory.fixedPageSize;
            this.ryseInventory.ignoredSlotsWithEvents = builder.ryseInventory.ignoredSlotsWithEvents;
            this.ryseInventory.updateTask = builder.ryseInventory.updateTask;
        }

        public @NotNull Builder newInstance() {
            return new Builder(this);
        }

        public @NotNull Builder keepOriginal() {
            this.ryseInventory.keepOriginal = true;
            return this;
        }

        /**
         * This function disables the update task.
         *
         * @return A Builder object.
         */
        public @NotNull Builder disableUpdateTask() {
            this.ryseInventory.updateTask = false;
            return this;
        }

        /**
         * Save the inventory permanently in a list. As a result, you can always get this inventory
         * through the {@link InventoryManager#getInventory(Object)} method.
         * Of course, the inventory must be given an ID by the {@link #identifier(Object)} method
         *
         * @return A Builder object.
         */
        public Builder permanentCache() {
            this.ryseInventory.permanentCache = true;
            return this;
        }

        /**
         * In this slot items can be taken or added by the player. However, you can now add your own logic.
         *
         * @param slot  The slot
         * @param event Your event with your own logic when the item is clicked.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoredSlot(@Nonnegative int slot, @NotNull Consumer<InventoryClickEvent> event) {
            this.ryseInventory.ignoredSlotsWithEvents.put(slot, event);
            return this;
        }

        /**
         * In this slot items can be taken or added by the player. However, you can now add your own logic.
         *
         * @param condition The condition must return true for the slots to be ignored.
         * @param slot      The slot
         * @param event     Your event with your own logic when the item is clicked.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoredSlot(@NotNull BooleanSupplier condition, @Nonnegative int slot, @NotNull Consumer<InventoryClickEvent> event) {
            if (!condition.getAsBoolean()) return this;

            this.ryseInventory.ignoredSlotsWithEvents.put(slot, event);
            return this;
        }

        /**
         * In all these slots items can be taken or added by the player. However, you can now add your own logic.
         *
         * @param slots The slots
         * @param event Your event with your own logic when the item is clicked.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoredSlots(@NotNull List<Integer> slots, @NotNull Consumer<InventoryClickEvent> event) {
            slots.forEach(slot -> this.ryseInventory.ignoredSlotsWithEvents.put(slot, event));
            return this;
        }

        /**
         * In all these slots items can be taken or added by the player. However, you can now add your own logic.
         *
         * @param condition The condition must return true for the slots to be ignored.
         * @param slots     The slots
         * @param event     Your event with your own logic when the item is clicked.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoredSlots(@NotNull BooleanSupplier condition, @NotNull List<Integer> slots, @NotNull Consumer<InventoryClickEvent> event) {
            if (!condition.getAsBoolean()) return this;

            slots.forEach(slot -> this.ryseInventory.ignoredSlotsWithEvents.put(slot, event));
            return this;
        }

        /**
         * In all these slots items can be put in or removed by the user.
         *
         * @param slots The slots
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoredSlots(int @NotNull ... slots) {
            for (int slot : slots)
                this.ryseInventory.ignoredSlotsWithEvents.put(slot, null);
            return this;
        }

        /**
         * In all these slots items can be put in or removed by the user.
         *
         * @param slots     The slots
         * @param condition The condition must return true for the slots to be ignored.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoredSlots(@NotNull BooleanSupplier condition, int... slots) {
            if (!condition.getAsBoolean()) return this;

            for (int slot : slots)
                this.ryseInventory.ignoredSlotsWithEvents.put(slot, null);
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
        public @NotNull Builder closeAfter(@Nonnegative int time, @Nullable TimeSetting setting) {
            if (setting == null) {
                this.ryseInventory.closeAfter = time;
                return this;
            }

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
        public @NotNull Builder loadDelay(@Nonnegative int time, @Nullable TimeSetting setting) {
            if (setting == null) {
                this.ryseInventory.loadDelay = time;
                return this;
            }

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
        public @NotNull Builder loadTitle(@Nonnegative int time, @Nullable TimeSetting setting) {
            if (setting == null) {
                this.ryseInventory.loadTitle = time;
                return this;
            }

            this.ryseInventory.loadTitle = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * Modifies the inventory type.
         *
         * @param type What type of inventory should it be.
         * @return The Inventory Builder to set additional options.
         * <p>
         * By default, the type is CHEST
         */
        public @NotNull Builder type(@NotNull InventoryOpenerType type) {
            this.ryseInventory.inventoryOpenerType = type;
            this.ryseInventory.size = type.getType().getDefaultSize();
            return this;
        }

        /**
         * This allows to get more control over the InventoryAction. E.g. you can say
         * that DOUBLE_CLICK as well as MOVE_TO_OTHER_INVENTORY should be activated and thereby the InventoryClickEvent is not canceled.
         *
         * @param actions   The actions
         * @param condition The condition must return true for the action to be activated.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder enableAction(@NotNull BooleanSupplier condition, Action @NotNull ... actions) {
            if (!condition.getAsBoolean()) return this;

            this.ryseInventory.enabledActions.addAll(Arrays.asList(actions));
            return this;
        }

        /**
         * This allows to get more control over the InventoryAction. E.g. you can say
         * that DOUBLE_CLICK as well as MOVE_TO_OTHER_INVENTORY should be activated and thereby the InventoryClickEvent is not canceled.
         *
         * @param actions The actions
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder enableAction(Action @NotNull ... actions) {
            this.ryseInventory.enabledActions.addAll(Arrays.asList(actions));
            return this;
        }

        /**
         * When the inventory is opened, the inventory is emptied and saved. When closing the inventory, the inventory will be reloaded.
         *
         * @return The Inventory Builder to set additional options.
         * <p>
         * By default, the inventory is not emptied and saved.
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
         * <p>
         * The inventory is always closable by default.
         */
        public @NotNull Builder preventClose() {
            this.ryseInventory.closeAble = false;
            return this;
        }

        /**
         * The method can be used so that data is not transferred to the next page as well.
         *
         * @return The Inventory Builder to set additional options.
         * <p>
         * The data is always transferred by default.
         */
        public @NotNull Builder preventTransferData() {
            this.ryseInventory.transferData = false;
            return this;
        }

        /**
         * If this method is called, all items that the player can set himself will not be saved.
         *
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoreManualItems() {
            this.ryseInventory.ignoreManualItems = true;
            return this;
        }

        /**
         * Adjusts the delay of the scheduler.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder delay(@Nonnegative int time, @Nullable TimeSetting setting) {
            if (setting == null) {
                this.ryseInventory.delay = time;
                return this;
            }

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
        public @NotNull Builder openDelay(@Nonnegative int time, @Nullable TimeSetting setting) {
            if (setting == null) {
                this.ryseInventory.openDelay = time;
                return this;
            }

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
        public @NotNull Builder period(@Nonnegative int time, @Nullable TimeSetting setting) {
            if (setting == null) {
                this.ryseInventory.period = time;
                return this;
            }

            this.ryseInventory.period = TimeUtils.buildTime(time, setting);
            return this;
        }

        /**
         * If you do not have a size but a row, you can also create an inventory by doing this.
         * This number of rows is used for each page.
         *
         * @param rows The row
         * @return The Inventory Builder to set additional options.
         * @throws IllegalArgumentException if rows greater than 6
         *                                  <p>
         *                                  If you had to create an inventory with 1 row, do not pass 0 but 1. Also applies to multiple rows. <br>
         *                                  If this method is used, the method {@link Builder#rows(Page)} is ignored.
         */
        public @NotNull Builder rows(@Nonnegative int rows) throws IllegalArgumentException {
            if (rows > 6)
                throw new IllegalArgumentException("The rows can not be greater than 6");

            size(rows * 9);
            return this;
        }

        /**
         * Add to a page the rows you want.
         *
         * @param page The page
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder rows(@NotNull Page page) {
            this.ryseInventory.pages.add(page);
            return this;
        }

        /**
         * Add to a page the rows you want.
         *
         * @param pages The pages
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder rows(Page @NotNull ... pages) {
            for (Page page : pages)
                rows(page);

            return this;
        }

        /**
         * Assigns a fixed title to the inventory
         *
         * @param title The title
         * @return The Inventory Builder to set additional options.
         * <p>
         * The title can also be changed later when the inventory is open.
         */
        public @NotNull Builder title(@NotNull String title) {
            return title(Component.text(title));
        }

        /**
         * Assigns a fixed title to the inventory
         *
         * @param title The title
         * @return The Inventory Builder to set additional options.
         * <p>
         * The title can also be changed later when the inventory is open.
         */
        public @NotNull Builder title(@NotNull Component title) {
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
         * <p>
         * This title is used when the {@link Builder#loadTitle(int, TimeSetting)} method is used.
         */
        public @NotNull Builder titleHolder(@NotNull String title) {
            return titleHolder(Component.text(title));
        }

        /**
         * Adds a temporary title to the inventory.
         *
         * @param title The temp title
         * @return The Inventory Builder to set additional options.
         * <p>
         * This title is used when the {@link Builder#loadTitle(int, TimeSetting)} method is used.
         */
        public @NotNull Builder titleHolder(@NotNull Component title) {
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
         * @param clicks What should be ignored
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoreClickEvent(DisabledInventoryClick @NotNull ... clicks) {
            this.ryseInventory.ignoreClickEvent.addAll(new ArrayList<>(Arrays.asList(clicks)));
            return this;
        }

        /**
         * Disables events according to wishes.
         *
         * @param events The events
         * @return The Inventory Builder to set additional options.
         */
        public @NotNull Builder ignoreEvents(DisabledEvents @NotNull ... events) {
            this.ryseInventory.disabledEvents.addAll(new ArrayList<>(Arrays.asList(events)));
            return this;
        }


        /**
         * Sets a page number. These pages can be opened at any time independently of the item.
         *
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
         * @param plugin         Instance to your main class.
         * @param defaultManager The default manager if the player does not have a custom manager.
         * @return the RyseInventory
         * @throws IllegalStateException if manager is null or if the provider is null
         */
        public @NotNull RyseInventory build(@NotNull Plugin plugin, @Nullable InventoryManager defaultManager) throws IllegalStateException {
            readOutInventoryManager(plugin);

            if (this.ryseInventory.manager == null) {
                if (defaultManager != null) {
                    this.ryseInventory.manager = defaultManager;
                } else {
                    throw new IllegalStateException("No manager could be found. Please create an InventoryManager field in your main class.");
                }
            }

            if (!this.ryseInventory.closeAble && !this.ryseInventory.closeReasons.isEmpty())
                throw new IllegalStateException("The #close() method could not be executed because you have forbidden closing the inventory by #preventClose.");

            if (this.ryseInventory.provider == null)
                throw new IllegalStateException("No provider could be found. Make sure you pass a provider to the builder.");

            validate();

            this.ryseInventory.plugin = plugin;

            if (this.ryseInventory.size != -1 && !this.ryseInventory.pages.isEmpty()) {
                plugin.getLogger().warning("You use the #rows(Integer) and #rows(Page) method in the RyseInventory Builder. " +
                        "Here #rows(Integer) is always prioritized, resulting in #rows(Page) being ignored and unnecessary data having to be cached." +
                        "It will still work, but it is recommended to fix this bug.");
            }

            if (this.ryseInventory.permanentCache)
                this.ryseInventory.manager.addToCache(this.ryseInventory);

            return this.ryseInventory;
        }

        /**
         * If it is an Anvil look if extensions are used that are not supported.
         */
        private void validate() {
            if (this.ryseInventory.inventoryOpenerType != InventoryOpenerType.ANVIL)
                return;

            if (this.ryseInventory.slideAnimator != null)
                throw new UnsupportedOperationException("The animation is not supported for the anvil inventory.");

            if (this.ryseInventory.fixedPageSize != -1)
                throw new UnsupportedOperationException("The fixed page size is not supported for the anvil inventory.");
        }

        /**
         * Builds the RyseInventory
         *
         * @param plugin Instance to your main class.
         * @return the RyseInventory
         * @throws IllegalStateException if manager is null or if the provider is null
         */
        public @NotNull RyseInventory build(@NotNull Plugin plugin) throws IllegalStateException {
            return build(plugin, null);
        }

        /**
         * It finds the InventoryManager field in the plugin class and sets it to the RyseInventory.manager field
         *
         * @param plugin The plugin instance.
         */
        private void readOutInventoryManager(@NotNull Plugin plugin) {
            for (Field field : ryseInventory.getAllFields(new ArrayList<>(), plugin.getClass())) {
                field.setAccessible(true);

                if (!field.getType().isAssignableFrom(InventoryManager.class))
                    continue;

                try {
                    InventoryManager inventoryManager = (InventoryManager) field.get(plugin);
                    if (!inventoryManager.isInvoked())
                        throw new IllegalStateException("The InventoryManager is not invoked. Please invoke it in the onEnable method.");

                    this.ryseInventory.manager = inventoryManager;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }
}
