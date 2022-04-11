package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import io.github.rysefoxx.RyseInventoryPlugin;
import io.github.rysefoxx.SlotIterator;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.content.InventoryProvider;
import io.github.rysefoxx.opener.InventoryOpenerType;
import io.github.rysefoxx.other.EventCreator;
import io.github.rysefoxx.other.InventoryOptions;
import io.github.rysefoxx.util.ReflectionUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;


public class RyseInventory {

    // Classes.
    private final static Class<?> CRAFT_PLAYER_CLASS;
    private final static Class<?> CHAT_MESSAGE_CLASS;
    private final static Class<?> PACKET_PLAY_OUT_OPEN_WINDOW_CLASS;
    private final static Class<?> I_CHAT_BASE_COMPONENT_CLASS;
    private final static Class<?> CONTAINERS_CLASS;
    private final static Class<?> ENTITY_PLAYER_CLASS;
    private final static Class<?> CONTAINER_CLASS;

    // Methods.
    private final static MethodHandle getHandle;
    private final static MethodHandle getBukkitView;

    // Constructors.
    private static Constructor<?> chatMessageConstructor;
    private static Constructor<?> packetPlayOutOpenWindowConstructor;

    // Fields.
    private static Field activeContainerField;
    private static Field windowIdField;

    static {
        // Initialize classes.
        CRAFT_PLAYER_CLASS = ReflectionUtils.getCraftClass("entity.CraftPlayer");
        CHAT_MESSAGE_CLASS = ReflectionUtils.getNMSClass("network.chat", "ChatMessage");
        PACKET_PLAY_OUT_OPEN_WINDOW_CLASS = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutOpenWindow");
        I_CHAT_BASE_COMPONENT_CLASS = ReflectionUtils.getNMSClass("network.chat", "IChatBaseComponent");
        // Check if we use containers, otherwise, can throw errors on older versions.
        CONTAINERS_CLASS = useContainers() ? ReflectionUtils.getNMSClass("world.inventory", "Containers") : null;
        ENTITY_PLAYER_CLASS = ReflectionUtils.getNMSClass("server.level", "EntityPlayer");
        CONTAINER_CLASS = ReflectionUtils.getNMSClass("world.inventory", "Container");

        MethodHandle handle = null, bukkitView = null;

        try {
            int version = ReflectionUtils.VER;
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Initialize methods.
            handle = lookup.findVirtual(CRAFT_PLAYER_CLASS, "getHandle", MethodType.methodType(ENTITY_PLAYER_CLASS));
            bukkitView = lookup.findVirtual(CONTAINER_CLASS, "getBukkitView", MethodType.methodType(InventoryView.class));

            // Initialize constructors.
            assert CHAT_MESSAGE_CLASS != null;
            chatMessageConstructor = CHAT_MESSAGE_CLASS.getConstructor(String.class, Object[].class);
            // Older versions use Strings instead of containers, and require an int for the inventory size.
            assert PACKET_PLAY_OUT_OPEN_WINDOW_CLASS != null;
            if ((useContainers())) {
                packetPlayOutOpenWindowConstructor =
                        PACKET_PLAY_OUT_OPEN_WINDOW_CLASS.getConstructor(int.class, CONTAINERS_CLASS, I_CHAT_BASE_COMPONENT_CLASS);
            } else {
                packetPlayOutOpenWindowConstructor =
                        PACKET_PLAY_OUT_OPEN_WINDOW_CLASS.getConstructor(int.class, String.class, I_CHAT_BASE_COMPONENT_CLASS, int.class);
            }

            // Initialize fields.
            assert ENTITY_PLAYER_CLASS != null;
            if ((version == 17)) {
                activeContainerField = ENTITY_PLAYER_CLASS.getField("bV");
            } else {
                if ((version == 18)) {
                    activeContainerField = ENTITY_PLAYER_CLASS.getField("bW");
                } else {
                    activeContainerField = ENTITY_PLAYER_CLASS.getField("activeContainer");
                }
            }
            assert CONTAINER_CLASS != null;
            if ((version > 16)) {
                windowIdField = CONTAINER_CLASS.getField("j");
            } else {
                windowIdField = CONTAINER_CLASS.getField("windowId");
            }
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }

        getHandle = handle;
        getBukkitView = bukkitView;
    }


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
    Inventory sharedInventory;
    private int delay = 0;
    private int openDelay = -1;
    private int period = 1;
    private int closeAfter = -1;
    private int loadDelay = -1;
    private int loadTitle = -1;
    private boolean ignoreClickEvent;
    private boolean closeAble = true;
    private boolean transferData = true;
    private boolean share;
    private boolean clearAndSafe;
    private List<InventoryOptions> options = new ArrayList<>();
    private Object identifier;
    private JavaPlugin plugin;
    private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;
    private final HashMap<UUID, Inventory> privateInventory = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> playerInventory = new HashMap<>();
    protected final List<Player> delayed = new ArrayList<>();

    /**
     * Closes the inventory from the player. InventoryClickEvent is no longer called here.
     *
     * @param player The player which inventory should be closed.
     */
    public void close(@NotNull Player player) {
        if (this.playerInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(this.playerInventory.remove(player.getUniqueId()));
        }

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
    public @NotNull Inventory open(@NotNull Player player) {
        return open(player, 1);
    }

    /**
     * Opens the inventory with the first page for multiple players.
     *
     * @param players The players for whom the inventory should be opened.
     */
    public void open(Player @NotNull ... players) {
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
    public void open(String @NotNull [] keys, Object @NotNull [] values, @NotNull Player @NotNull ... players) throws IllegalArgumentException {
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
    public void open(@Nonnegative int page, @NotNull Player @NotNull ... players) {
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
    public @NotNull Inventory open(@NotNull Player player, String @NotNull [] keys, Object @NotNull [] values) throws IllegalArgumentException {
        Preconditions.checkArgument(keys.length == values.length, "String[] and Object[] must have the same size");

        return this.initInventory(player, 1, keys, values);
    }

    private Inventory initInventory(@NotNull Player player, @Nonnegative int page, @Nullable String[] keys, @Nullable Object[] values) {
        Optional<RyseInventory> savedInventory = this.manager.getInventory(player.getUniqueId());

        savedInventory.ifPresent(mainInventory -> {
            this.manager.setLastInventory(player.getUniqueId(), mainInventory);
            this.manager.removeInventory(player.getUniqueId());

            if (mainInventory.playerInventory.containsKey(player.getUniqueId())) {
                player.getInventory().setContents(mainInventory.playerInventory.remove(player.getUniqueId()));
            }
        });

        if (this.clearAndSafe) {
            this.playerInventory.put(player.getUniqueId(), player.getInventory().getContents());
            player.getInventory().clear();
        }

        Inventory inventory;

        if (this.inventoryOpenerType == InventoryOpenerType.CHEST) {
            inventory = Bukkit.createInventory(null, this.size == -1 ? this.rows * this.columns : this.size, Component.text(this.loadTitle == -1 ? this.title : this.titleHolder));
        } else {
            inventory = Bukkit.createInventory(null, this.inventoryOpenerType.getType(), Component.text(this.loadTitle == -1 ? this.title : this.titleHolder));
        }


        InventoryContents contents = new InventoryContents(player, this);
        Optional<InventoryContents> optional = this.manager.getContents(player.getUniqueId());
        page--;

        contents.pagination().setPage(page);

        if (this.transferData) {
            optional.ifPresent(savedContents -> savedContents.transferData(contents));
        }
        if (keys != null && values != null) {
            Arrays.stream(keys).filter(Objects::nonNull).forEach(s -> Arrays.stream(values).filter(Objects::nonNull).forEach(o -> contents.setData(s, o)));
        }

        this.manager.setContents(player.getUniqueId(), contents);
        this.provider.init(player, contents);


        if (optional.isPresent() && optional.get().equals(contents)) return inventory;

        this.manager.stopUpdate(player.getUniqueId());


        Pagination pagination = contents.pagination();
        splitInventory(contents);

        if (!pagination.getPageItems().containsKey(page)) {
            close(player);
            throw new IllegalArgumentException("There is no " + page + " side. Last page is " + pagination.lastPage());
        }

        if (this.loadDelay != -1) {
            int finalPage = page;
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> load(pagination, inventory, player, finalPage), this.loadDelay);
        } else {
            load(pagination, inventory, player, page);
        }

        if (this.loadTitle != -1) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> updateTitle(this.plugin, player, this.title), this.loadTitle);
        }

        closeAfterScheduler(player);

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

        if (this.share) {
            this.sharedInventory = inventory;
        } else {
            this.privateInventory.put(player.getUniqueId(), inventory);
        }
        return this.sharedInventory;
    }

    private void load(@NotNull Pagination pagination, @NotNull Inventory inventory, @NotNull Player player, @Nonnegative int page) {
        pagination.getPermanentItems().forEach((integer, item) -> {
            if (integer >= inventory.getSize()) return;
            if (!item.isCanSee()) {
                item.getError().cantSee(player, item);
                return;
            }
            inventory.setItem(integer, item.getItemStack());
        });
        pagination.getPageItems().get(page).forEach((integer, item) -> {
            if (integer >= inventory.getSize()) return;
            if (!item.isCanSee()) {
                item.getError().cantSee(player, item);
                return;
            }
            inventory.setItem(integer, item.getItemStack());
        });
    }

    private void closeAfterScheduler(@NotNull Player player) {
        if (this.closeAfter == -1) return;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> close(player), this.closeAfter);
    }

    /**
     * Get an EventCreator object based on the Event class.
     *
     * @param event The event what you want to get
     * @return null if there is no custom event matching the event class
     */
    public @Nullable EventCreator<? extends Event> getEvent(Class<? extends Event> event) {
        if (this.events.isEmpty()) return null;

        return this.events.stream().filter(eventOne -> event == eventOne.clazz()).findFirst().orElse(null);
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param plugin   The JavaPlugin
     * @param player   The Player
     * @param newTitle The new title
     * @apiNote https://www.spigotmc.org/threads/change-inventory-title-reflection-1-8-1-18.489966/
     */
    public void updateTitle(@NotNull JavaPlugin plugin, @NotNull Player player, @NotNull String newTitle) {
        try {
            // Get EntityPlayer from CraftPlayer.
            assert CRAFT_PLAYER_CLASS != null;
            Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
            Object entityPlayer = getHandle.invoke(craftPlayer);

            if (newTitle.length() > 32) {
                newTitle = newTitle.substring(0, 32);
            }

            // Create new title.
            Object title = chatMessageConstructor.newInstance(newTitle, new Object[]{});

            // Get activeContainer from EntityPlayer.
            Object activeContainer = activeContainerField.get(entityPlayer);

            // Get windowId from activeContainer.
            Integer windowId = (Integer) windowIdField.get(activeContainer);

            // Get InventoryView from activeContainer.
            Object bukkitView = getBukkitView.invoke(activeContainer);
            if (!(bukkitView instanceof InventoryView view)) return;

            InventoryType type = view.getTopInventory().getType();

            // Workbenchs and anvils can change their title since 1.14.
            if ((type == InventoryType.WORKBENCH || type == InventoryType.ANVIL) && !useContainers())
                return;

            // You can't reopen crafting, creative and player inventory.
            if (Arrays.asList("CRAFTING", "CREATIVE", "PLAYER").contains(type.name())) return;

            int size = view.getTopInventory().getSize();

            // Get container, check is not null.
            Containers container = Containers.getType(type, size);
            if (container == null) return;

            // If the container was added in a newer versions than the current, return.
            if (container.getContainerVersion() > ReflectionUtils.VER && useContainers()) {
                Bukkit.getLogger().warning(String.format(
                        "[%s] This container doesn't work on your current version.",
                        plugin.getDescription().getName()));
                return;
            }

            Object object;
            // Dispensers and droppers use the same container, but in previous versions, use a diferrent minecraft name.
            if (!useContainers() && container == Containers.GENERIC_3X3) {
                object = "minecraft:" + type.name().toLowerCase();
            } else {
                object = container.getObject();
            }

            // Create packet.
            Object packet =
                    (useContainers()) ?
                            packetPlayOutOpenWindowConstructor.newInstance(windowId, object, title) :
                            packetPlayOutOpenWindowConstructor.newInstance(windowId, object, title, size);

            // Send packet sync.
            ReflectionUtils.sendPacketSync(player, packet);

            // Update inventory.
            player.updateInventory();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    /**
     * @return the size of the inventory.
     */
    public @Nonnegative
    int size() {
        return this.size == -1 ? this.rows * this.columns : this.size;
    }

    private void splitInventory(@NotNull InventoryContents contents) {
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

    private int updateForNextSlot(@NotNull SlotIterator.SlotIteratorType type, @Nonnegative int slot, @Nonnegative int startSlot) {
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

    @Contract("_, _, _, _, _ -> new")
    private int @NotNull [] nextSlotAlgorithm(@NotNull InventoryContents contents, @NotNull SlotIterator.SlotIteratorType type, @Nonnegative int page, @Nonnegative int calculatedSlot, @Nonnegative int startSlot) {
        SlotIterator iterator = Objects.requireNonNull(contents.iterator());

        int toAdd = 0;
        while (contents.getInPage(page, calculatedSlot).isPresent() || iterator.getBlackList().contains(calculatedSlot)) {
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

    protected void clearData(@NotNull Player player) {
        if (this.playerInventory.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(this.playerInventory.remove(player.getUniqueId()));
        }

        this.delayed.remove(player);
        this.privateInventory.remove(player.getUniqueId());
        this.manager.removeInventoryFromPlayer(player.getUniqueId());
    }


    /**
     * Builder to create an inventory.
     *
     * @return The Builder object with several methods
     */
    @Contract(value = " -> new", pure = true)
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Builder to create an inventory.
     */
    public static class Builder {
        private InventoryManager manager;
        private int size = -1;
        private int rows;
        private String title;
        private String titleHolder = "§e§oLoading§8...";
        private final List<EventCreator<? extends Event>> events = new ArrayList<>();
        private boolean ignoreClickEvent;
        private boolean closeAble = true;
        private boolean transferData = true;
        private boolean share;
        private boolean clearAndSafe;
        private final List<InventoryOptions> options = new ArrayList<>();
        private InventoryProvider provider;
        private int delay = 0;
        private int openDelay = -1;
        private int period = 1;
        private int closeAfter = -1;
        private int loadDelay = -1;
        private int loadTitle = -1;
        private Object identifier;
        private InventoryOpenerType inventoryOpenerType = InventoryOpenerType.CHEST;

        /**
         * Adds a manager to the inventory.
         *
         * @param manager InventoryManager
         * @return The Inventory Builder to set additional options.
         * @apiNote If the plugin is in the plugins folder, the parameter can be null.
         */
        public Builder manager(@Nullable InventoryManager manager) {
            this.manager = manager == null ? RyseInventoryPlugin.getInventoryManager() : manager;
            return this;
        }

        /**
         * Settings to help ensure that the player is not disturbed while he has the inventory open.
         *
         * @param options All setting options for the inventory
         * @return The Inventory Builder to set additional options.
         */
        public Builder options(@NotNull InventoryOptions @NotNull ... options) {
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
        public Builder closeAfter(@Nonnegative int time, @NotNull TimeSetting setting) {
            this.closeAfter = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * With this method, the content of the inventory is loaded later.
         *
         * @param time    Time
         * @param setting Set your own time type.
         * @return The Inventory Builder to set additional options.
         */
        public Builder loadDelay(@Nonnegative int time, @NotNull TimeSetting setting) {
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
        public Builder loadTitle(@Nonnegative int time, @NotNull TimeSetting setting) {
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
        public Builder type(@NotNull InventoryOpenerType type) {
            this.inventoryOpenerType = type;
            return this;
        }

        /**
         * Changes within the inventory are the same for all players if share is true.
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote By default, the inventory is not shared.
         */
        public Builder share() {
            this.share = true;
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
        public Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * The provider to fill the inventory with content.
         *
         * @param provider Implement with new InventoryProvider()
         * @return The Inventory Builder to set additional options.
         */
        public Builder provider(@NotNull InventoryProvider provider) {
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
        public Builder delay(@Nonnegative int time, @NotNull TimeSetting setting) {
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
        public Builder openDelay(@Nonnegative int time, @NotNull TimeSetting setting) {
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
        public Builder period(@Nonnegative int time, @NotNull TimeSetting setting) {
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
        public Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        /**
         * Adds a temporary title to the inventory.
         *
         * @param title The temp title
         * @return The Inventory Builder to set additional options.
         * @apiNote This title is used when the {@link Builder#loadTitle(int, TimeSetting)} method is used.
         */
        public Builder titleHolder(@NotNull String title) {
            this.titleHolder = title;
            return this;
        }

        /**
         * Adds its own event to the inventory.
         *
         * @param event What kind of event
         * @return The Inventory Builder to set additional options.
         */
        public Builder listener(@NotNull EventCreator<? extends Event> event) {
            this.events.add(event);
            return this;
        }

        /**
         * Ignores the InventoryClickEvent
         *
         * @return The Inventory Builder to set additional options.
         * @apiNote A self-created event via #listener is not ignored.
         */
        public Builder ignoreClickEvent() {
            this.ignoreClickEvent = true;
            return this;
        }

        /**
         * Builds the RyseInventory
         *
         * @param plugin Instance to your main class.
         * @return the RyseInventory
         * @throws IllegalStateException if manager is null
         */
        public RyseInventory build(@NotNull JavaPlugin plugin) throws IllegalStateException {
            if (this.manager == null) {
                throw new IllegalStateException("No manager could be found. Make sure you pass a manager or the plugin is loaded as plugin.");
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
            inventory.share = this.share;
            inventory.clearAndSafe = this.clearAndSafe;
            inventory.options = this.options;
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
     * @return if the InventoryClickEvent should be ignored
     */
    public boolean isIgnoreClickEvent() {
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

    @Contract(pure = true)
    private static boolean useContainers() {
        return ReflectionUtils.VER > 13;
    }


    /**
     * @param uuid Player's uuid
     * @return the correct inventory based on whether it is split or not.
     */
    protected Optional<Inventory> inventoryBasedOnOption(@Nullable UUID uuid) {
        if (this.share) return Optional.ofNullable(this.sharedInventory);

        if (uuid == null) return Optional.empty();
        if (!this.privateInventory.containsKey(uuid)) return Optional.empty();
        return Optional.ofNullable(this.privateInventory.get(uuid));
    }

    private enum Containers {
        GENERIC_9X1(14, "minecraft:chest", "CHEST"),
        GENERIC_9X2(14, "minecraft:chest", "CHEST"),
        GENERIC_9X3(14, "minecraft:chest", "CHEST", "ENDER_CHEST", "BARREL"),
        GENERIC_9X4(14, "minecraft:chest", "CHEST"),
        GENERIC_9X5(14, "minecraft:chest", "CHEST"),
        GENERIC_9X6(14, "minecraft:chest", "CHEST"),
        GENERIC_3X3(14, null, "DISPENSER", "DROPPER"),
        ANVIL(14, "minecraft:anvil", "ANVIL"),
        BEACON(14, "minecraft:beacon", "BEACON"),
        BREWING_STAND(14, "minecraft:brewing_stand", "BREWING"),
        ENCHANTMENT(14, "minecraft:enchanting_table", "ENCHANTING"),
        FURNACE(14, "minecraft:furnace", "FURNACE"),
        HOPPER(14, "minecraft:hopper", "HOPPER"),
        MERCHANT(14, "minecraft:villager", "MERCHANT"),
        // For an unknown reason, when updating a shulker box, the size of the inventory get a little bigger.
        SHULKER_BOX(14, "minecraft:blue_shulker_box", "SHULKER_BOX"),

        // Added in 1.14, so only works with containers.
        BLAST_FURNACE(14, null, "BLAST_FURNACE"),
        CRAFTING(14, null, "WORKBENCH"),
        GRINDSTONE(14, null, "GRINDSTONE"),
        LECTERN(14, null, "LECTERN"),
        LOOM(14, null, "LOOM"),
        SMOKER(14, null, "SMOKER"),
        // CARTOGRAPHY in 1.14, CARTOGRAPHY_TABLE in 1.15 & 1.16 (container), handle in getObject().
        CARTOGRAPHY_TABLE(14, null, "CARTOGRAPHY"),
        STONECUTTER(14, null, "STONECUTTER"),

        // Added in 1.14, functional since 1.16.
        SMITHING(16, null, "SMITHING");

        private final int containerVersion;
        private final String minecraftName;
        private final String[] inventoryTypesNames;

        private final static char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        @Contract(pure = true)
        Containers(int containerVersion, String minecraftName, String... inventoryTypesNames) {
            this.containerVersion = containerVersion;
            this.minecraftName = minecraftName;
            this.inventoryTypesNames = inventoryTypesNames;
        }

        /**
         * Get the container based on the current open inventory of the player.
         *
         * @param type type of inventory.
         * @return the container.
         */
        public static @Nullable Containers getType(InventoryType type, int size) {
            if (type == InventoryType.CHEST) {
                return Containers.valueOf("GENERIC_9X" + size / 9);
            }
            for (Containers container : Containers.values()) {
                for (String bukkitName : container.getInventoryTypesNames()) {
                    if (bukkitName.equalsIgnoreCase(type.toString())) {
                        return container;
                    }
                }
            }
            return null;
        }

        /**
         * Get the object from the container enum.
         *
         * @return a Containers object if 1.14+, otherwise, a String.
         */
        public @Nullable Object getObject() {
            try {
                if (!useContainers()) return getMinecraftName();
                int version = ReflectionUtils.VER;
                String name = (version == 14 && this == CARTOGRAPHY_TABLE) ? "CARTOGRAPHY" : name();
                // Since 1.17, containers go from "a" to "x".
                if (version > 16) name = String.valueOf(alphabet[ordinal()]);
                assert CONTAINERS_CLASS != null;
                Field field = CONTAINERS_CLASS.getField(name);
                return field.get(null);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            return null;
        }

        /**
         * Get the version in which the inventory container was added.
         *
         * @return the version.
         */
        @Contract(pure = true)
        public int getContainerVersion() {
            return containerVersion;
        }

        /**
         * Get the name of the inventory from Minecraft for older versions.
         *
         * @return name of the inventory.
         */
        @Contract(pure = true)
        public String getMinecraftName() {
            return minecraftName;
        }

        /**
         * Get inventory types names of the inventory.
         *
         * @return bukkit names.
         */
        @Contract(pure = true)
        public String[] getInventoryTypesNames() {
            return inventoryTypesNames;
        }
    }
}
