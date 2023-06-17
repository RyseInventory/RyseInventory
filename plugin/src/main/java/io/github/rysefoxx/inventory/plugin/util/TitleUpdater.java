package io.github.rysefoxx.inventory.plugin.util;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * A utility class for update the inventory of a player.
 * This is useful to change the title of an inventory.
 */
@SuppressWarnings("ConstantConditions")
public final class TitleUpdater {

    // Classes.
    private final static Class<?> CRAFT_PLAYER;
    private final static Class<?> PACKET_PLAY_OUT_OPEN_WINDOW;
    private final static Class<?> I_CHAT_BASE_COMPONENT;
    private final static Class<?> CONTAINER;
    private final static Class<?> CONTAINERS;
    private final static Class<?> ENTITY_PLAYER;

    // Methods.
    private final static MethodHandle getHandle;
    private final static MethodHandle getBukkitView;

    // Constructors.
    private final static MethodHandle packetPlayOutOpenWindow;

    // Fields.
    private final static MethodHandle activeContainer;
    private final static MethodHandle windowId;

    // Methods factory.
    private final static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        boolean supports19 = ReflectionUtils.supports(19);

        // Initialize classes.
        CRAFT_PLAYER = ReflectionUtils.getCraftClass("entity.CraftPlayer");
        PACKET_PLAY_OUT_OPEN_WINDOW = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutOpenWindow");
        I_CHAT_BASE_COMPONENT = ReflectionUtils.getNMSClass("network.chat", "IChatBaseComponent");
        // Check if we use containers, otherwise, can throw errors on older versions.
        CONTAINERS = useContainers() ? ReflectionUtils.getNMSClass("world.inventory", "Containers") : null;
        ENTITY_PLAYER = ReflectionUtils.getNMSClass("server.level", "EntityPlayer");
        CONTAINER = ReflectionUtils.getNMSClass("world.inventory", "Container");

        // Initialize methods.
        getHandle = getMethod(CRAFT_PLAYER, "getHandle", MethodType.methodType(ENTITY_PLAYER));
        getBukkitView = getMethod(CONTAINER, "getBukkitView", MethodType.methodType(InventoryView.class));

        // Initialize constructors.
        packetPlayOutOpenWindow =
                (useContainers()) ?
                        getConstructor(int.class, CONTAINERS, I_CHAT_BASE_COMPONENT) :
                        // Older versions use String instead of Containers, and require an int for the inventory size.
                        getConstructor(int.class, String.class, I_CHAT_BASE_COMPONENT, int.class);

        // Initialize fields.
        activeContainer = getField(ENTITY_PLAYER, CONTAINER, "activeContainer", "bV", "bW", "bU", "containerMenu");
        windowId = getField(CONTAINER, int.class, "windowId", "j", "containerId");
    }

    /**
     * Update the player inventory, so you can change the title.
     *
     * @param player   whose inventory will be updated.
     * @param newTitle the new title for the inventory.
     */
    public static void updateInventory(Player player, String newTitle) {
        Preconditions.checkArgument(player != null, "Cannot update inventory to null player.");

        try {
            // Get EntityPlayer from CraftPlayer.
            Object craftPlayer = CRAFT_PLAYER.cast(player);
            Object entityPlayer = getHandle.invoke(craftPlayer);

            // Create new title.
            Object title = ReflectionUtils.toIChatBaseComponentPlain(newTitle);

            // Get activeContainer from EntityPlayer.
            Object activeContainer = TitleUpdater.activeContainer.invoke(entityPlayer);

            // Get windowId from activeContainer.
            Integer windowId = (Integer) TitleUpdater.windowId.invoke(activeContainer);

            // Get InventoryView from activeContainer.
            Object bukkitView = getBukkitView.invoke(activeContainer);
            if (!(bukkitView instanceof InventoryView)) return;

            InventoryView view = (InventoryView) bukkitView;
            InventoryType type = view.getTopInventory().getType();

            // Workbenchs and anvils can change their title since 1.14.
            if ((type == InventoryType.WORKBENCH || type == InventoryType.ANVIL) && !useContainers()) return;

            // You can't reopen crafting, creative and player inventory.
            if (Arrays.asList("CRAFTING", "CREATIVE", "PLAYER").contains(type.name())) return;

            int size = view.getTopInventory().getSize();

            // Get container, check is not null.
            Containers container = Containers.getType(type, size);
            if (container == null) return;

            // If the container was added in a newer version than the current, return.
            if (container.getContainerVersion() > ReflectionUtils.VER && useContainers()) {
                Bukkit.getLogger().warning("The container " + type.name() + " is not supported in this version of Minecraft.");
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
                            packetPlayOutOpenWindow.invoke(windowId, object, title) :
                            packetPlayOutOpenWindow.invoke(windowId, object, title, size);

            // Send packet sync.
            ReflectionUtils.sendPacketSync(player, packet);

            // Update inventory.
            player.updateInventory();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static MethodHandle getField(Class<?> refc, Class<?> instc, String name, String... extraNames) {
        MethodHandle handle = getFieldHandle(refc, instc, name);
        if (handle != null) return handle;

        if (extraNames != null && extraNames.length > 0) {
            if (extraNames.length == 1) return getField(refc, instc, extraNames[0]);
            return getField(refc, instc, extraNames[0], removeFirst(extraNames));
        }

        return null;
    }

    private static String[] removeFirst(String[] array) {
        int length = array.length;

        String[] result = new String[length - 1];
        System.arraycopy(array, 1, result, 0, length - 1);

        return result;
    }

    private static MethodHandle getFieldHandle(Class<?> refc, Class<?> inscofc, String name) {
        try {
            for (Field field : refc.getFields()) {
                field.setAccessible(true);

                if (!field.getName().equalsIgnoreCase(name)) continue;

                if (field.getType().isInstance(inscofc) || field.getType().isAssignableFrom(inscofc)) {
                    return LOOKUP.unreflectGetter(field);
                }
            }
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static MethodHandle getConstructor(Class<?>... types) {
        try {
            Constructor<?> constructor = TitleUpdater.PACKET_PLAY_OUT_OPEN_WINDOW.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return LOOKUP.unreflectConstructor(constructor);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static MethodHandle getMethod(Class<?> refc, String name, MethodType type) {
        return getMethod(refc, name, type, false);
    }

    private static MethodHandle getMethod(Class<?> refc, String name, MethodType type, boolean isStatic) {
        try {
            if (isStatic) return LOOKUP.findStatic(refc, name, type);
            return LOOKUP.findVirtual(refc, name, type);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * Containers were added in 1.14, a String were used in previous versions.
     *
     * @return whether to use containers.
     */
    private static boolean useContainers() {
        return ReflectionUtils.VER > 13;
    }

    /**
     * An enum class for the necessaries containers.
     */
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

        private final static char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        private final int containerVersion;
        private final String minecraftName;
        private final String[] inventoryTypesNames;

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
        public Object getObject() {
            try {
                if (!useContainers()) return getMinecraftName();
                int version = ReflectionUtils.VER;
                String name = (version == 14 && this == CARTOGRAPHY_TABLE) ? "CARTOGRAPHY" : name();
                // Since 1.17, containers go from "a" to "x".
                if (version > 16) name = String.valueOf(alphabet[ordinal()]);
                Field field = CONTAINERS.getField(name);
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