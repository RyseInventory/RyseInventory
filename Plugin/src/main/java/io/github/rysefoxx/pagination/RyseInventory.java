package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.RyseInventoryPlugin;
import io.github.rysefoxx.SlotIterator;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.enums.*;
import io.github.rysefoxx.other.EventCreator;
import io.github.rysefoxx.util.TitleUpdater;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnegative;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RyseInventory {

    private List<EventCreator<? extends Event>> events = new ArrayList<>();
    private InventoryManager manager;
    private @Getter
    InventoryProvider provider;
    private @Getter
    int size = -1;
    private @Getter
    int rows;
    private @Getter
    int columns = 9;
    private String title;
    private String titleHolder = "§e§oLoading§8...";
    private @Getter
    Inventory inventory;
    private int delay = 0;
    private int openDelay = -1;
    private int period = 1;
    private int closeAfter = -1;
    private int loadDelay = -1;
    private int loadTitle = -1;
    private List<DisabledInventoryClick> ignoreClickEvent = new ArrayList<>();
    private boolean closeAble = true;
    private boolean transferData = true;
    private boolean clearAndSafe;
    private List<InventoryOptions> options = new ArrayList<>();
    private final List<IntelligentItemNameAnimator> itemAnimator = new ArrayList<>();
    private final List<IntelligentMaterialAnimator> materialAnimator = new ArrayList<>();
    private final List<IntelligentTitleAnimator> titleAnimator = new ArrayList<>();
    private final List<IntelligentItemLoreAnimator> loreAnimator = new ArrayList<>();
    private SlideAnimation slideAnimator = null;
    private Object identifier;
    private Plugin plugin;
    private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;
    private final HashMap<UUID, Inventory> privateInventory = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> playerInventory = new HashMap<>();
    protected final List<Player> delayed = new ArrayList<>();
    private List<CloseReason> closeReasons = new ArrayList<>();

    private boolean backward = false;

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public Optional<IntelligentItemLoreAnimator> getLoreAnimation(Object identifier) {
        return this.loreAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public Optional<IntelligentItemNameAnimator> getNameAnimation(Object identifier) {
        return this.itemAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public Optional<IntelligentTitleAnimator> getTitleAnimation(Object identifier) {
        return this.titleAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * This method allows you to retrieve the animation using the animation identifier.
     *
     * @param identifier The ID to identify
     * @return null if no animation with the ID could be found.
     * @implNote Only works if the animation has also been assigned an identifier.
     */
    public Optional<IntelligentMaterialAnimator> getMaterialAnimator(Object identifier) {
        return this.materialAnimator.stream().filter(animator -> Objects.equals(animator.getIdentifier(), identifier)).findFirst();
    }

    /**
     * Closes the inventory from the player. InventoryClickEvent is no longer called here.
     *
     * @param player The player which inventory should be closed.
     */
    public void close(Player player) {
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
    public List<UUID> getOpenedPlayers() {
        List<UUID> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<RyseInventory> optional = this.manager.getInventory(player.getUniqueId());

            optional.ifPresent(savedInventory -> {
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
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<RyseInventory> inventory = this.manager.getInventory(player.getUniqueId());

            inventory.ifPresent(mainInventory -> {
                if (mainInventory != this) return;
                mainInventory.close(player);
            });
        });
    }

    /**
     * Opens the inventory with the first page.
     *
     * @param player The player where the inventory should be opened.
     * @return the Bukkit Inventory object.
     */
    public Inventory open(Player player) {
        return open(player, 1);
    }

    /**
     * Opens the inventory with the first page for multiple players.
     *
     * @param players The players for whom the inventory should be opened.
     */
    public void open(Player... players) {
        for (Player player : players) {
            open(player, 1);
        }
    }

    /**
     * Opens the inventory with the first page for multiple players with defined properties.
     *
     * @param players The players for whom the inventory should be opened.
     * @param keys    The keys
     * @param values  The values
     * @throws IllegalArgumentException if the two arrays do not have the same size.
     */
    public void open(String[] keys, Object[] values, Player... players) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, "String[] and Object[] must have the same size");

        for (Player player : players) {
            open(player, 1, keys, values);
        }
    }

    /**
     * Opens the inventory with a specific page for multiple players.
     *
     * @param players The players for whom the inventory should be opened.
     */
    public void open(@Nonnegative int page, Player... players) {
        for (Player player : players) {
            open(player, page);
        }
    }

    /**
     * Opens an inventory with a specific page.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @return Returns the Bukkit Inventory object.
     */
    public Inventory open(Player player, @Nonnegative int page) {
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
    public Inventory open(Player player, @Nonnegative int page, String[] keys, Object[] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, "String[] and Object[] must have the same size");

        return this.initInventory(player, page, keys, values);
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
    public Inventory open(Player player, String[] keys, Object[] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, "String[] and Object[] must have the same size");

        return this.initInventory(player, 1, keys, values);
    }

    private Inventory initInventory(Player player, @Nonnegative int page, String[] keys, Object[] values) {
        finishSavedInventory(player);
        removeActiveAnimations();

        clearInventoryWhenNeeded(player);

        Inventory inventory = setupInventory();

        InventoryContents contents = new InventoryContents(player, this);
        Optional<InventoryContents> optional = this.manager.getContents(player.getUniqueId());
        page--;

        contents.pagination().setPage(page);

        transferData(optional.orElse(null), contents, keys, values);
        setupData(player, inventory, contents);
        initProvider(player, contents);

        if (optional.isPresent() && optional.get().equals(contents)) return inventory;

        this.manager.stopUpdate(player.getUniqueId());

        Pagination pagination = contents.pagination();
        splitInventory(contents);

        if (!pagination.getPageItems().containsKey(page)) {
            close(player);
            throw new IllegalArgumentException("There is no " + page + " side. Last page is " + pagination.lastPage());
        }

        loadDelay(page, pagination, player);
        closeInventoryWhenEnabled(player);

        finalizeInventoryAndOpen(player);
        return inventory;
    }

    /**
     * Get an EventCreator object based on the Event class.
     *
     * @param event The event what you want to get
     * @return null if there is no custom event matching the event class
     */
    public EventCreator<? extends Event> getEvent(Class<? extends Event> event) {
        if (this.events.isEmpty()) return null;

        return this.events.stream().filter(eventOne -> event == eventOne.getClazz()).findFirst().orElse(null);
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param player   The Player
     * @param newTitle The new title
     * @author <a href="https://www.spigotmc.org/threads/change-inventory-title-reflection-1-8-1-18.489966/">Original code (Slightly Modified)</a>
     */
    public void updateTitle(Player player, String newTitle) {
        TitleUpdater.updateTitle(player, newTitle);
    }

    /**
     * @return the size of the inventory.
     */
    public @Nonnegative
    int size() {
        return this.size == -1 ? this.rows * this.columns : this.size;
    }

    protected void splitInventory(InventoryContents contents) {
        Pagination pagination = contents.pagination();
        SlotIterator iterator = contents.iterator();

        if (iterator == null) return;

        SlotIterator.SlotIteratorType type = iterator.getType();
        boolean useSlot = iterator.getSlot() != -1;
        int itemsSet = 0;
        int page = 0;
        int startSlot = iterator.getSlot();
        int startRow = iterator.getRow();
        int startColumn = iterator.getColumn();
        int slot = startSlot;
        int calculatedSlot = startRow * 9 + startColumn;
        HashMap<Integer, HashMap<Integer, IntelligentItem>> items = contents.pagination().getPageItems();

        for (IntelligentItem item : pagination.getItems()) {
            if (itemsSet >= pagination.getItemsPerPage() || ((slot >= iterator.getEndPosition() || calculatedSlot >= iterator.getEndPosition()) && iterator.getEndPosition() != -1)) {
                itemsSet = 0;
                slot = startSlot;
                calculatedSlot = startRow * 9 + startColumn;
                page++;
            }

            if (!items.containsKey(page)) {
                items.put(page, new HashMap<>());
            }

            if (!iterator.isOverride()) {
                int[] dataArray;
                if (useSlot) {
                    dataArray = nextSlotAlgorithm(contents, type, page, slot, startSlot);
                    slot = dataArray[1];
                } else {
                    dataArray = nextSlotAlgorithm(contents, type, page, calculatedSlot, startRow * 9 + startColumn);
                    calculatedSlot = dataArray[1];
                }
                page = dataArray[0];
            }

            if (!items.containsKey(page)) {
                items.put(page, new HashMap<>());
            }

            items.get(page).put(useSlot ? slot : calculatedSlot, item);
            itemsSet++;

            if (useSlot) {
                slot = updateForNextSlot(type, slot, startSlot);
            } else {
                calculatedSlot = updateForNextSlot(type, calculatedSlot, startRow * 9 + startColumn);
            }
        }

        contents.pagination().setPageItems(items);
    }

    private int updateForNextSlot(SlotIterator.SlotIteratorType type, @Nonnegative int slot, @Nonnegative int startSlot) {
        if (type == SlotIterator.SlotIteratorType.HORIZONTAL) {
            slot++;
        } else if (type == SlotIterator.SlotIteratorType.VERTICAL) {
            if ((slot + 9) > size()) {
                slot = startSlot + 1;
            } else {
                slot += 9;
            }
        }
        return slot;
    }

    private int[] nextSlotAlgorithm(InventoryContents contents, SlotIterator.SlotIteratorType type, @Nonnegative int page, @Nonnegative int calculatedSlot, @Nonnegative int startSlot) {
        SlotIterator iterator = Objects.requireNonNull(contents.iterator());

        int toAdd = 0;
        while (!contents.firstEmpty().isPresent() ? iterator.getBlackList().contains(calculatedSlot) : contents.getInPage(page, calculatedSlot).isPresent() || iterator.getBlackList().contains(calculatedSlot)) {
            if (calculatedSlot >= 53) {
                calculatedSlot = startSlot;
                page++;
            }

            if (type == SlotIterator.SlotIteratorType.HORIZONTAL) {
                calculatedSlot++;
            } else {
                if ((calculatedSlot + 9) > size()) {
                    toAdd++;
                    calculatedSlot = startSlot + toAdd;
                } else {
                    calculatedSlot += 9;
                }
            }
        }
        return new int[]{page, calculatedSlot};
    }

    protected void clearData(Player player) {
        if (this.playerInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(this.playerInventory.remove(player.getUniqueId()));
        }

        this.delayed.remove(player);
        this.privateInventory.remove(player.getUniqueId());
        this.manager.removeInventoryFromPlayer(player.getUniqueId());
    }

    protected void addItemAnimator(IntelligentItemNameAnimator animator) {
        this.itemAnimator.add(animator);
    }

    protected void addMaterialAnimator(IntelligentMaterialAnimator animator) {
        this.materialAnimator.add(animator);
    }

    protected void removeMaterialAnimator(IntelligentMaterialAnimator animator) {
        this.materialAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    protected void removeItemAnimator(IntelligentItemNameAnimator animator) {
        this.itemAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    protected void addTitleAnimator(IntelligentTitleAnimator animator) {
        this.titleAnimator.add(animator);
    }

    protected void removeTitleAnimator(IntelligentTitleAnimator animator) {
        this.titleAnimator.remove(animator);

        if (!Bukkit.getScheduler().isQueued(animator.getTask().getTaskId())) return;
        animator.getTask().cancel();
    }

    protected void addLoreAnimator(IntelligentItemLoreAnimator animator) {
        this.loreAnimator.add(animator);
    }

    protected void removeLoreAnimator(IntelligentItemLoreAnimator animator) {
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

    protected SlideAnimation getSlideAnimator() {
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

    /**
     * Builder to create an inventory.
     *
     * @return The Builder object with several methods
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create an inventory.
     */
    public static class Builder {
        private String titleHolder = "§e§oLoading§8...";
        private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;
        private final List<InventoryOptions> options = new ArrayList<>();
        private final List<EventCreator<? extends Event>> events = new ArrayList<>();
        private boolean closeAble = true;
        private boolean transferData = true;
        private int size = -1;
        private int delay = 0;
        private int openDelay = -1;
        private int period = 1;
        private int closeAfter = -1;
        private int loadDelay = -1;
        private int loadTitle = -1;

        private SlideAnimation slideAnimation;
        private InventoryManager manager;
        private String title;
        private InventoryProvider provider;
        private Object identifier;
        private int rows;
        private final List<DisabledInventoryClick> ignoreClickEvent = new ArrayList<>();
        private boolean clearAndSafe;
        private final List<CloseReason> closeReasons = new ArrayList<>();

        /**
         * Adds a manager to the inventory.
         *
         * @param manager InventoryManager
         * @return The Inventory Builder to set additional options.
         */
        public Builder manager(InventoryManager manager) {
            this.manager = manager;
            return this;
        }

        /**
         * Settings to help ensure that the player is not disturbed while he has the inventory open.
         *
         * @param options All setting options for the inventory
         * @return The Inventory Builder to set additional options.
         */
        public Builder options(InventoryOptions... options) {
            this.options.addAll(new ArrayList<>(Arrays.asList(options)));
            return this;
        }


        /**
         * With this method you can automatically set when to close the inventory.
         *
         * @param time The time in seconds
         * @return The Inventory Builder to set additional options.
         * @deprecated You should now use the {@link #closeAfter(int, TimeSetting)} method.
         */
        @Deprecated
        public Builder closeAfter(@Nonnegative int time) {
            this.closeAfter = time * 20;
            return this;
        }

        /**
         * With this method you can automatically set when to close the inventory.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public Builder closeAfter(@Nonnegative int time, TimeSetting setting) {
            this.closeAfter = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Here you can set possible reasons to automatically close the inventory when the reason takes place.
         * @param reason The reason to close the inventory.
         * @return The Inventory Builder to set additional options.
         */
        public Builder close(CloseReason... reasons) {
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
        public Builder loadDelay(@Nonnegative int time, TimeSetting setting) {
            this.loadDelay = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * With this method the title will be loaded later.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public Builder loadTitle(@Nonnegative int time, TimeSetting setting) {
            this.loadTitle = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Modifies the inventory type.
         *
         * @param type What type of inventory should it be.
         * @return The Inventory Builder to set additional options.
         * @apiNote By default, the type is CHEST
         */
        public Builder type(InventoryOpenerType type) {
            this.inventoryOpenerType = type;
            return this;
        }

        /**
         * When the inventory is opened, the inventory is emptied and saved. When closing the inventory, the inventory will be reloaded.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote By default, the inventory is not emptied and saved.
         */
        public Builder clearAndSafe() {
            this.clearAndSafe = true;
            return this;
        }

        /**
         * @param size The inventory size
         * @return The Inventory Builder to set additional options.
         * @throws IllegalArgumentException if size is smaller than 9 or larger than 54.
         */
        public Builder size(@Nonnegative int size) throws IllegalArgumentException {
            if (size < 9 || size > 54) {
                throw new IllegalArgumentException(size < 9 ? "The size can not be less than 9" : "The size can not be greater than 54");
            }
            this.size = size;
            return this;
        }

        /**
         * Gives the inventory an identification
         *
         * @param identifier The ID through which you can get the inventory
         * @return The Inventory Builder to set additional options.
         */
        public Builder identifier(Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * The provider to fill the inventory with content.
         *
         * @param provider Implement with new InventoryProvider()
         * @return The Inventory Builder to set additional options.
         */
        public Builder provider(InventoryProvider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * This method can be used to prevent the player from closing the inventory.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote The inventory is always closable by default.
         */
        public Builder preventClose() {
            this.closeAble = false;
            return this;
        }

        /**
         * The method can be used so that data is not transferred to the next page as well.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote The data is always transferred by default.
         */
        public Builder preventTransferData() {
            this.transferData = false;
            return this;
        }

        /**
         * Adjusts the delay of the scheduler.
         *
         * @param seconds Time in seconds
         * @return The Inventory Builder to set additional options.
         * @deprecated You should now use the {@link #delay(int, TimeSetting)} method.
         */
        @Deprecated
        public Builder delay(@Nonnegative int seconds) {
            this.delay = seconds * 20;
            return this;
        }

        /**
         * Adjusts the delay of the scheduler.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public Builder delay(@Nonnegative int time, TimeSetting setting) {
            this.delay = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Adjusts the delay before the inventory is opened.
         *
         * @param seconds Time in seconds
         * @return The Inventory Builder to set additional options.
         * @deprecated You should now use the {@link #openDelay(int, TimeSetting)} method.
         */
        @Deprecated
        public Builder openDelay(@Nonnegative int seconds) {
            this.openDelay = seconds * 20;
            return this;
        }

        /**
         * Adjusts the delay before the inventory is opened.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public Builder openDelay(@Nonnegative int time, TimeSetting setting) {
            this.openDelay = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Adjusts the period of the scheduler.
         *
         * @param seconds Time in seconds
         * @return The Inventory Builder to set additional options.
         * @deprecated You should now use the {@link #period(int, TimeSetting)} method.
         */
        @Deprecated
        public Builder period(@Nonnegative int seconds) {
            this.period = seconds * 20;
            return this;
        }

        /**
         * Adjusts the period of the scheduler.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public Builder period(@Nonnegative int time, TimeSetting setting) {
            this.period = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
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
        public Builder rows(@Nonnegative int rows) throws IllegalArgumentException {
            if (rows > 6) {
                throw new IllegalArgumentException("The rows can not be greater than 6");
            }

            this.rows = rows;
            return this;
        }

        /**
         * Assigns a fixed title to the inventory
         *
         * @param title The title
         * @return The Inventory Builder to set additional options.
         * @apiNote The title can also be changed later when the inventory is open.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Based on this animation, the items can appear animated when opening the inventory.
         *
         * @param animation {@link  SlideAnimation#builder(Plugin)}
         * @return The Inventory Builder to set additional options.
         */
        public Builder animation(SlideAnimation animation) {
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
        public Builder titleHolder(String title) {
            this.titleHolder = title;
            return this;
        }

        /**
         * Adds its own event to the inventory.
         *
         * @param event What kind of event
         * @return The Inventory Builder to set additional options.
         */
        public Builder listener(EventCreator<? extends Event> event) {
            this.events.add(event);
            return this;
        }

        /**
         * Set what should be ignored in the InventoryClickEvent.
         *
         * @return The Inventory Builder to set additional options.
         */
        @Deprecated
        public Builder ignoreClickEvent() {
            this.ignoreClickEvent.add(DisabledInventoryClick.BOTH);
            return this;
        }

        /**
         * Set what should be ignored in the InventoryClickEvent.
         *
         * @return The Inventory Builder to set additional options.
         */
        public Builder ignoreClickEvent(DisabledInventoryClick... clicks) {
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
        public RyseInventory build(Plugin plugin) throws IllegalStateException {
            if (this.manager == null) {
                throw new IllegalStateException("No manager could be found. Make sure you pass a manager to the builder.");
            }
            if(!this.closeAble && !this.closeReasons.isEmpty()) {
                throw new IllegalStateException("The #close() method could not be executed because you have forbidden closing the inventory by #preventClose.");
            }

            RyseInventory inventory = new RyseInventory();
            inventory.plugin = plugin;
            inventory.manager = this.manager;
            inventory.size = this.size;
            inventory.closeAble = this.closeAble;
            inventory.rows = this.rows;
            inventory.columns = 9;
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
    public String getTitle() {
        return this.title;
    }

    /**
     * @return how much later the scheduler starts (in milliseconds)
     */
    public int getDelay() {
        return this.delay;
    }

    /**
     * @return how often the scheduler ticks (in milliseconds)
     */
    public int getPeriod() {
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
    public List<DisabledInventoryClick> getIgnoreClickEvent() {
        return this.ignoreClickEvent;
    }

    /**
     * @return the ID from the inventory
     * @apiNote You have to give the inventory itself an ID with {@link Builder#identifier(Object)}
     */
    public Object getIdentifier() {
        return this.identifier;
    }

    /**
     * @return the type.
     */
    public InventoryOpenerType getInventoryOpenerType() {
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
    public List<InventoryOptions> getOptions() {
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
    protected Optional<Inventory> inventoryBasedOnOption(UUID uuid) {
        if (uuid == null) return Optional.empty();
        if (!this.privateInventory.containsKey(uuid)) return Optional.empty();
        return Optional.ofNullable(this.privateInventory.get(uuid));
    }

    protected void load(Pagination pagination, Player player, @Nonnegative int page) {
        pagination.getPermanentItems().forEach((integer, item) -> placeItem(player, integer, item));
        pagination.getPageItems().get(page).forEach((integer, item) -> placeItem(player, integer, item));
    }

    private void placeItem(Player player, int integer, IntelligentItem item) {
        if (integer >= inventory.getSize()) return;
        if (!item.isCanSee()) {
            item.getError().cantSee(player, item);
            return;
        }
        inventory.setItem(integer, item.getItemStack());
    }

    private void closeInventoryWhenEnabled(Player player) throws IllegalStateException {
        if (this.closeAfter == -1) return;
        if (!this.closeAble) {
            throw new IllegalStateException("The #closeAfter() method could not be executed because you have forbidden closing the inventory by #preventClose.");
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> close(player), this.closeAfter);
    }

    private void removeActiveAnimations() {
        for (int i = 0; i < this.itemAnimator.size(); i++) {
            IntelligentItemNameAnimator animator = this.itemAnimator.remove(i);
            removeItemAnimator(animator);
        }
        for (int i = 0; i < this.titleAnimator.size(); i++) {
            IntelligentTitleAnimator animator = this.titleAnimator.remove(i);
            removeTitleAnimator(animator);
        }
        for (int i = 0; i < this.loreAnimator.size(); i++) {
            IntelligentItemLoreAnimator animator = this.loreAnimator.remove(i);
            removeLoreAnimator(animator);
        }
        for (int i = 0; i < this.materialAnimator.size(); i++) {
            IntelligentMaterialAnimator animator = this.materialAnimator.remove(i);
            removeMaterialAnimator(animator);
        }
        removeSlideAnimator();
    }

    protected void setBackward(boolean bool) {
        this.backward = bool;
    }

    protected InventoryManager getManager() {
        return manager;
    }

    private void finishSavedInventory(Player player) {
        Optional<RyseInventory> savedInventory = this.manager.getInventory(player.getUniqueId());

        savedInventory.ifPresent(mainInventory -> {
            if (!this.backward) {
                this.manager.setLastInventory(player.getUniqueId(), mainInventory);
            }
            this.manager.removeInventory(player.getUniqueId());

            if (mainInventory.playerInventory.containsKey(player.getUniqueId())) {
                player.getInventory().setContents(mainInventory.playerInventory.remove(player.getUniqueId()));
            }
        });
    }

    private void clearInventoryWhenNeeded(Player player) {
        if (!this.clearAndSafe) return;

        this.playerInventory.put(player.getUniqueId(), player.getInventory().getContents());
        player.getInventory().clear();
    }

    private Inventory setupInventory() {
        if (this.inventoryOpenerType == InventoryOpenerType.CHEST) {
            return Bukkit.createInventory(null, this.size == -1 ? this.rows * this.columns : this.size, this.loadTitle == -1 ? this.title : this.titleHolder);
        }
        return inventory = Bukkit.createInventory(null, this.inventoryOpenerType.getType(), this.loadTitle == -1 ? this.title : this.titleHolder);
    }

    private void transferData(InventoryContents oldContents, InventoryContents newContents, String[] keys, Object[] values) {
        if (this.transferData && oldContents != null)
            oldContents.transferData(newContents);

        if (keys != null && values != null) {
            Arrays.stream(keys).filter(Objects::nonNull).forEach(s -> Arrays.stream(values).filter(Objects::nonNull).forEach(o -> newContents.setData(s, o)));
        }
    }

    private void setupData(Player player, Inventory inventory, InventoryContents contents) {
        this.manager.setContents(player.getUniqueId(), contents);

        this.inventory = inventory;
        this.privateInventory.put(player.getUniqueId(), inventory);
    }

    private void initProvider(Player player, InventoryContents contents) {
        if (this.slideAnimator == null) {
            this.provider.init(player, contents);
            return;
        }
        this.provider.init(player, contents, this.slideAnimator);

    }

    private void loadDelay(int page, Pagination pagination, Player player) {
        if (this.loadDelay != -1) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> load(pagination, player, page), this.loadDelay);
        } else {
            load(pagination, player, page);
        }

        if (this.loadTitle != -1) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> updateTitle(player, this.title), this.loadTitle);
        }
    }

    protected List<CloseReason> getCloseReasons() {
        return closeReasons;
    }

    private void finalizeInventoryAndOpen(Player player) {
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
}
