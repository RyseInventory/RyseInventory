package com.github.rysefoxx.pagination;

import com.github.rysefoxx.SlotIterator;
import com.github.rysefoxx.content.IntelligentItem;
import com.github.rysefoxx.content.InventoryProvider;
import com.github.rysefoxx.other.EventCreator;
import com.github.rysefoxx.util.ReflectionUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


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
    int size;
    private @Getter
    int rows;
    private @Getter
    int columns = 9;
    private String title;
    private @Getter
    Inventory inventory;
    private int delay = 0;
    private int period = 20;
    private boolean ignoreClickEvent;
    private boolean closeAble = true;
    private Object identifier;


    /**
     * Closes the inventory from the player. InventoryClickEvent is no longer called here.
     *
     * @param player The player which inventory should be closed.
     */
    public void close(@NotNull Player player) {
        this.manager.removeInventoryFromPlayer(player);
        player.closeInventory();
    }

    /**
     * Opens the inventory with the first page.
     *
     * @param player The player where the inventory should be opened.
     * @return Returns the Bukkit Inventory object.
     * @throws IllegalArgumentException Throws IllegalArgumentException if there is no 1 page.
     */
    public Inventory open(@NotNull Player player) throws IllegalArgumentException {
        return open(player, 1);
    }

    /**
     * Opens the inventory with the page.
     *
     * @param player The player where the inventory should be opened.
     * @param page   Which page should be opened?
     * @return Returns the Bukkit Inventory object.
     * @throws IllegalArgumentException Throws IllegalArgumentException if the page does not exist.
     */
    public Inventory open(@NotNull Player player, @Nonnegative int page) throws IllegalArgumentException {
        this.manager.addInventoryToPlayer(player, this);

        Inventory inventory = Bukkit.createInventory(null, this.size == -1 ? this.rows * this.columns : this.size, Component.text(this.title));
        InventoryContents contents = this.manager.getContents(player);
        Pagination pagination = contents.pagination();
        pagination.pageItems = splitInventory(contents);

        if (!pagination.pageItems.containsKey(page)) {
            close(player);
            throw new IllegalArgumentException("§cCan not find page §9" + page);
        }

        if (contents.getFillBorder() != null)
            contents.fillBorders(inventory);


        contents.getItems().forEach((integer, item) -> inventory.setItem(integer, item.getItemStack()));
        pagination.pageItems.get(page).forEach((integer, item) -> inventory.setItem(integer, item.getItemStack()));

        player.openInventory(inventory);
        this.inventory = inventory;
        return this.inventory;
    }

    /**
     * Get an EventCreator object based on the Event class.
     *
     * @param event The event what you want to get
     * @return null if there is no custom event matching the event class
     */
    public @Nullable EventCreator<? extends Event> getEvent(Class<? extends Event> event) {
        if (this.events.isEmpty()) return null;

        return this.events.stream().filter(eventOne -> event == eventOne.aClass()).findFirst().orElse(null);
    }

    /**
     * With this method you can update the inventory title.
     *
     * @param plugin   The JavaPlugin
     * @param player   The Player
     * @param newTitle The new title
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
            if ((type == InventoryType.WORKBENCH || type == InventoryType.ANVIL) && !useContainers()) return;

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

    private @NotNull HashMap<Integer, HashMap<Integer, IntelligentItem>> splitInventory(@NotNull InventoryContents contents) {
        HashMap<Integer, HashMap<Integer, IntelligentItem>> items = new HashMap<>();
        Pagination pagination = contents.pagination();
        SlotIterator iterator = contents.iterator();
        SlotIterator.SlotIteratorType type = iterator.getType();
        boolean useSlot = iterator.getSlot() != -1;
        int itemsSet = 0;
        int page = 1;
        int startSlot = iterator.getSlot();
        int startRow = iterator.getRow();
        int startColumn = iterator.getColumn();
        int slot = startSlot;
        int calculatedSlot = startRow * 9 + startColumn;

        for (IntelligentItem item : pagination.getItems()) {
            if (itemsSet >= pagination.getItemsPerPage()) {
                itemsSet = 0;
                page++;
            }

            if (!items.containsKey(page)) {
                items.put(page, new HashMap<>());
                slot = startSlot;
                calculatedSlot = startRow * 9 + startColumn;
            }


            if (useSlot) {
                if (!iterator.isOverride()) {
                    slot = nextSlotAlgorithm(contents, type, slot, startSlot);
                }

                items.get(page).put(slot, item);
                itemsSet++;
                slot = updateForNextSlot(type, slot, startSlot);
                continue;
            }

            if (!iterator.isOverride()) {
                calculatedSlot = nextSlotAlgorithm(contents, type, calculatedSlot, startRow * 9 + startColumn);
            }

            items.get(page).put(calculatedSlot, item);
            itemsSet++;

            calculatedSlot = updateForNextSlot(type, calculatedSlot, startRow * 9 + startColumn);
        }

        return items;
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

    private int nextSlotAlgorithm(@NotNull InventoryContents contents, @NotNull SlotIterator.SlotIteratorType type, @Nonnegative int calculatedSlot, @Nonnegative int startSlot) {
        while (contents.get(calculatedSlot).isPresent()) {
            if (type == SlotIterator.SlotIteratorType.HORIZONTAL) {
                calculatedSlot++;
            } else {
                if ((calculatedSlot + 9) > size()) {
                    calculatedSlot = startSlot + 1;
                } else {
                    calculatedSlot += 9;
                }
            }
        }
        return calculatedSlot;
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
        private final List<EventCreator<? extends Event>> events = new ArrayList<>();
        private boolean ignoreClickEvent;
        private boolean closeAble = true;
        private InventoryProvider provider;
        private int delay = 0;
        private int period = 20;
        private Object identifier;

        public Builder manager(@NotNull InventoryManager manager) {
            this.manager = manager;
            return this;
        }

        public Builder size(@Nonnegative int size) throws IllegalArgumentException {
            if (size < 9 || size > 54) {
                throw new IllegalArgumentException(size < 9 ? "The size can not be less than 9" : "The size can not be greater than 54");
            }
            this.size = size;
            return this;
        }

        public Builder identifier(@NotNull Object identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder provider(@NotNull InventoryProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder closeAble(Boolean bool) {
            this.closeAble = bool;
            return this;
        }

        public Builder delay(@Nonnegative int seconds) {
            this.delay = seconds * 20;
            return this;
        }

        public Builder period(@Nonnegative int seconds) {
            this.period = seconds * 20;
            return this;
        }

        public Builder rows(@Nonnegative int rows) throws IllegalArgumentException {
            rows--;
            if (rows > 6) {
                throw new IllegalArgumentException("The rows can not be greater than 6");
            }

            this.rows = rows;
            return this;
        }

        public Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        public Builder listener(@NotNull EventCreator<? extends Event> event) {
            this.events.add(event);
            return this;
        }

        public Builder ignoreClickEvent(boolean bool) {
            this.ignoreClickEvent = bool;
            return this;
        }

        public RyseInventory build() {
            RyseInventory inventory = new RyseInventory();
            inventory.manager = this.manager;
            inventory.size = this.size;
            inventory.closeAble = this.closeAble;
            inventory.rows = this.rows;
            inventory.columns = 9;
            inventory.title = this.title;
            inventory.events = this.events;
            inventory.ignoreClickEvent = this.ignoreClickEvent;
            inventory.provider = this.provider;
            inventory.delay = this.delay;
            inventory.period = this.period;
            inventory.identifier = this.identifier;
            return inventory;
        }
    }


    public String getTitle() {
        return title;
    }

    /**
     * @return how much later the scheduler starts (in milliseconds)
     */
    public int getDelay() {
        return delay;
    }

    /**
     * @return how often the scheduler ticks (in milliseconds)
     */
    public int getPeriod() {
        return period;
    }

    /**
     * @return if the inventory can be closed
     */
    public boolean isCloseAble() {
        return closeAble;
    }

    /**
     * @return if the InventoryClickEvent should be ignored
     */
    public boolean isIgnoreClickEvent() {
        return ignoreClickEvent;
    }

    /**
     * @return the ID from the inventory
     * @apiNote You have to give the inventory itself an ID with #identifier(Object)
     */
    public Object getIdentifier() {
        return identifier;
    }

    @Contract(pure = true)
    private static boolean useContainers() {
        return ReflectionUtils.VER > 13;
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
        public static Containers getType(InventoryType type, int size) {
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
        public int getContainerVersion() {
            return containerVersion;
        }

        /**
         * Get the name of the inventory from Minecraft for older versions.
         *
         * @return name of the inventory.
         */
        public String getMinecraftName() {
            return minecraftName;
        }

        /**
         * Get inventory types names of the inventory.
         *
         * @return bukkit names.
         */
        public String[] getInventoryTypesNames() {
            return inventoryTypesNames;
        }
    }
}
