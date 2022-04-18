package io.github.rysefoxx.util;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

public class TitleUpdater {

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
            // Constructors.
            Constructor<?> chatMessageConstructor = CHAT_MESSAGE_CLASS.getConstructor(String.class, Object[].class);
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

    public static void updateTitle(@NotNull Player player, @NotNull String newTitle) {
        // Convert title.
        Object title = ReflectionUtils.toIChatBaseComponentPlain(newTitle);
        try {
            // Get EntityPlayer from CraftPlayer.
            assert CRAFT_PLAYER_CLASS != null;
            Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
            Object entityPlayer = getHandle.invoke(craftPlayer);

            if (newTitle.length() > 32) {
                newTitle = newTitle.substring(0, 32);
            }

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
                Bukkit.getLogger().warning(
                        "This container doesn't work on your current version.");
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

    @Contract(pure = true)
    private static boolean useContainers() {
        return ReflectionUtils.VER > 13;
    }


}
