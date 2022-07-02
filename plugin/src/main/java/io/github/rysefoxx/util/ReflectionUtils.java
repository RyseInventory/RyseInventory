package io.github.rysefoxx.util;

import com.google.gson.Gson;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * <b>ReflectionUtils</b> - Reflection handler for NMS and CraftBukkit.<br>
 * Caches the packet related methods and is asynchronous.
 * <p>
 * This class does not handle null checks as most of the requests are from the
 * other utility classes that already handle null checks.
 * <p>
 * <a href="https://wiki.vg/Protocol">Clientbound Packets</a> are considered fake
 * updates to the client without changing the actual data. Since all the data is handled
 * by the server.
 * <p>
 * A useful resource used to compare mappings is <a href="https://minidigger.github.io/MiniMappingViewer/#/spigot">Mini's Mapping Viewer</a>
 *
 * @author Crypto Morin
 * @version 6.0.1
 */
public final class ReflectionUtils {
    /**
     * We use reflection mainly to avoid writing a new class for version barrier.
     * The version barrier is for NMS that uses the Minecraft version as the main package name.
     * <p>
     * E.g. EntityPlayer in 1.15 is in the class {@code net.minecraft.server.v1_15_R1}
     * but in 1.14 it's in {@code net.minecraft.server.v1_14_R1}
     * In order to maintain cross-version compatibility we cannot import these classes.
     * <p>
     * Performance is not a concern for these specific statically initialized values.
     */
    public static final String VERSION;

    static { // This needs to be right below VERSION because of initialization order.
        // This package loop is used to avoid implementation-dependant strings like Bukkit.getVersion() or Bukkit.getBukkitVersion()
        // which allows easier testing as well.
        String found = null;
        for (Package pack : Package.getPackages()) {
            String name = pack.getName();
            if (name.startsWith("org.bukkit.craftbukkit.v") // .v because there are other packages.
                    // As a protection for forge+bukkit implementation that tend to mix versions.
                    // The real CraftPlayer should exist in the package.
                    // Note: Doesn't seem to function properly. Will need to separate the version
                    // handler for NMS and CraftBukkit for softwares like catmc.
                    && name.endsWith("entity")) {
                found = pack.getName().split("\\.")[3];

                // Just a final guard to make sure it finds this important class.
                try {
                    Class.forName("org.bukkit.craftbukkit." + found + ".entity.CraftPlayer");
                    break;
                } catch (ClassNotFoundException e) {
                    found = null;
                }
            }
        }
        if (found == null)
            throw new IllegalArgumentException("Failed to parse server version. Could not find any package starting with name: 'org.bukkit.craftbukkit.v'");
        VERSION = found;
    }

    /**
     * The raw minor version number.
     * E.g. {@code v1_17_R1} to {@code 17}
     *
     * @since 4.0.0
     */
    public static final int VER = Integer.parseInt(VERSION.substring(1).split("_")[1]);
    /**
     * Mojang remapped their NMS in 1.17 https://www.spigotmc.org/threads/spigot-bungeecord-1-17.510208/#post-4184317
     */
    public static final String
            STRING = "net.minecraft.server." + VERSION + '.',
            CRAFTBUKKIT = "org.bukkit.craftbukkit." + VERSION + '.',
            NMS = v(17, "net.minecraft.").orElse("net.minecraft.server." + VERSION + '.');
    /**
     * A nullable public accessible field only available in {@code EntityPlayer}.
     * This can be null if the player is offline.
     */
    private static final MethodHandle PLAYER_CONNECTION;
    /**
     * Responsible for getting the NMS handler {@code EntityPlayer} object for the player.
     * {@code CraftPlayer} is simply a wrapper for {@code EntityPlayer}.
     * Used mainly for handling packet related operations.
     * <p>
     * This is also where the famous player {@code ping} field comes from!
     */
    private static final MethodHandle GET_HANDLE;
    /**
     * Sends a packet to the player's client through a {@code NetworkManager} which
     * is where {@code ProtocolLib} controls packets by injecting channels!
     */
    private static final MethodHandle SEND_PACKET;

    static {
        Class<?> entityPlayer = getNMSClass("server.level", "EntityPlayer");
        Class<?> craftPlayer = getCraftClass("entity.CraftPlayer");
        Class<?> playerConnection = getNMSClass("server.network", "PlayerConnection");

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle sendPacket = null, getHandle = null, connection = null;

        try {
            connection = lookup.findGetter(entityPlayer,
                    v(17, "b").orElse("playerConnection"), playerConnection);
            getHandle = lookup.findVirtual(craftPlayer, "getHandle", MethodType.methodType(entityPlayer));
            sendPacket = lookup.findVirtual(playerConnection,
                    v(18, "a").orElse("sendPacket"),
                    MethodType.methodType(void.class, getNMSClass("network.protocol", "Packet")));
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while finding getter", ex);
        }

        PLAYER_CONNECTION = connection;
        SEND_PACKET = sendPacket;
        GET_HANDLE = getHandle;
    }

    private ReflectionUtils() {
    }

    public static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... args) {
        for (final Method method : clazz.getMethods())
            if (method.getName().equals(methodName) && isClassListEqual(args, method.getParameterTypes())) {
                method.setAccessible(true);

                return method;
            }

        return null;
    }

    public static <T> T invoke(final Method method, final Object instance, final Object... params) {

        try {
            return (T) method.invoke(instance, params);

        } catch (final ReflectiveOperationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error during method call", ex);
        }
        return null;
    }

    private static boolean isClassListEqual(final Class<?>[] first, final Class<?>[] second) {
        if (first.length != second.length)
            return false;

        for (int i = 0; i < first.length; i++)
            if (first[i] != second[i])
                return false;

        return true;
    }

    /**
     * This method is purely for readability.
     * No performance is gained.
     *
     * @since 5.0.0
     */
    public static <T> VersionHandler<T> v(int version, T handle) {
        return new VersionHandler<>(version, handle);
    }

    public static <T> CallableVersionHandler<T> v(int version, Callable<T> handle) {
        return new CallableVersionHandler<>(version, handle);
    }

    /**
     * Checks whether the server version is equal or greater than the given version.
     *
     * @param version the version to compare the server version with.
     * @return true if the version is equal or newer, otherwise false.
     * @since 4.0.0
     */
    public static boolean supports(int version) {
        return VER >= version;
    }

    /**
     * Get a NMS (net.minecraft.server) class which accepts a package for 1.17 compatibility.
     *
     * @param newPackage the 1.17 package name.
     * @param name       the name of the class.
     * @return the NMS class or null if not found.
     * @since 4.0.0
     */
    @Nullable
    public static Class<?> getNMSClass(@Nonnull String newPackage, @Nonnull String name) {
        if (supports(17)) name = newPackage + '.' + name;
        return getNMSClass(name);
    }

    /**
     * Get a NMS (net.minecraft.server) class.
     *
     * @param name the name of the class.
     * @return the NMS class or null if not found.
     * @since 1.0.0
     */
    @Nullable
    public static Class<?> getNMSClass(@Nonnull String name) {
        try {
            return Class.forName(NMS + name);
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error finding NMS class.", ex);
            return null;
        }
    }

    @Nullable

    public static Class<?> getNMSClassWithFixed(@Nonnull String name) {
        try {
            return Class.forName(STRING + name);
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error finding fixed NMS class", ex);
            return null;
        }
    }

    /**
     * Sends a packet to the player asynchronously if they're online.
     * Packets are thread-safe.
     *
     * @param player  the player to send the packet to.
     * @param packets the packets to send.
     * @return the async thread handling the packet.
     * @see #sendPacketSync(Player, Object...)
     * @since 1.0.0
     */
    @Nonnull
    public static CompletableFuture<Void> sendPacket(@Nonnull Player player, @Nonnull Object... packets) {
        return CompletableFuture.runAsync(() -> sendPacketSync(player, packets))
                .exceptionally(ex -> {
                    Bukkit.getLogger().log(Level.SEVERE, "Error sending a packet.", ex);
                    return null;
                });
    }

    /**
     * Sends a packet to the player synchronously if they're online.
     *
     * @param player  the player to send the packet to.
     * @param packets the packets to send.
     * @see #sendPacket(Player, Object...)
     * @since 2.0.0
     */
    public static void sendPacketSync(@Nonnull Player player, @Nonnull Object... packets) {
        try {
            Object handle = GET_HANDLE.invoke(player);
            Object connection = PLAYER_CONNECTION.invoke(handle);

            // Checking if the connection is not null is enough. There is no need to check if the player is online.
            if (connection != null) {
                for (Object packet : packets) SEND_PACKET.invoke(connection, packet);
            }
        } catch (Throwable throwable) {
            Bukkit.getLogger().log(Level.SEVERE, "Error sending a packet.", throwable);
        }
    }

    @Nullable
    public static Object getHandle(@Nonnull Player player) {
        Objects.requireNonNull(player, "Cannot get handle of null player");
        try {
            return GET_HANDLE.invoke(player);
        } catch (Throwable throwable) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while getting the object.", throwable);
            return null;
        }
    }


    /**
     * @apiNote https://github.com/kangarko/Foundation
     */
    public static Object toIChatBaseComponentPlain(String text) {
        return toIChatBaseComponent(TextComponent.fromLegacyText(text));
    }

    /**
     * @apiNote https://github.com/kangarko/Foundation
     */
    public static Object toIChatBaseComponent(BaseComponent[] baseComponents) {
        return toIChatBaseComponent(toJson(baseComponents));
    }

    /**
     * @apiNote https://github.com/kangarko/Foundation
     */
    public static String toJson(final BaseComponent... comps) {
        String json;

        try {
            json = ComponentSerializer.toString(comps);

        } catch (final Throwable t) {
            json = new Gson().toJson(new TextComponent(comps).toLegacyText());
        }

        return json;
    }

    /**
     * @apiNote https://github.com/kangarko/Foundation
     */
    public static Object toIChatBaseComponent(String json) {
        final Class<?> chatSerializer = ReflectionUtils.getNMSClass("network.chat", "IChatBaseComponent$ChatSerializer");
        final Method a = ReflectionUtils.getMethod(chatSerializer, "a", String.class);

        return ReflectionUtils.invoke(a, null, json);
    }

    @Nullable
    public static Object getConnection(@Nonnull Player player) {
        Objects.requireNonNull(player, "Cannot get connection of null player");
        try {
            Object handle = GET_HANDLE.invoke(player);
            return PLAYER_CONNECTION.invoke(handle);
        } catch (Throwable throwable) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while getting the connection.", throwable);
            return null;
        }
    }

    /**
     * Get a CraftBukkit (org.bukkit.craftbukkit) class.
     *
     * @param name the name of the class to load.
     * @return the CraftBukkit class or null if not found.
     * @since 1.0.0
     */
    @Nullable
    public static Class<?> getCraftClass(@Nonnull String name) {
        try {
            return Class.forName(CRAFTBUKKIT + name);
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while getting the craftclass.", ex);
            return null;
        }
    }

    public static Class<?> getArrayClass(String clazz, boolean nms) {
        clazz = "[L" + (nms ? NMS : CRAFTBUKKIT) + clazz + ';';
        try {
            return Class.forName(clazz);
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while getting the array class.", ex);
            return null;
        }
    }

    public static Class<?> toArrayClass(Class<?> clazz) {
        try {
            return Class.forName("[L" + clazz.getName() + ';');
        } catch (ClassNotFoundException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while getting the array class.", ex);
            return null;
        }
    }

    public static final class VersionHandler<T> {
        private int version;
        private T handle;

        private VersionHandler(int version, T handle) {
            if (supports(version)) {
                this.version = version;
                this.handle = handle;
            }
        }

        public VersionHandler<T> v(int version, T handle) {
            if (version == this.version)
                throw new IllegalArgumentException("Cannot have duplicate version handles for version: " + version);
            if (version > this.version && supports(version)) {
                this.version = version;
                this.handle = handle;
            }
            return this;
        }

        public T orElse(T handle) {
            return this.version == 0 ? handle : this.handle;
        }
    }

    public static final class CallableVersionHandler<T> {
        private int version;
        private Callable<T> handle;

        private CallableVersionHandler(int version, Callable<T> handle) {
            if (supports(version)) {
                this.version = version;
                this.handle = handle;
            }
        }

        public CallableVersionHandler<T> v(int version, Callable<T> handle) {
            if (version == this.version)
                throw new IllegalArgumentException("Cannot have duplicate version handles for version: " + version);
            if (version > this.version && supports(version)) {
                this.version = version;
                this.handle = handle;
            }
            return this;
        }

        public T orElse(Callable<T> handle) {
            try {
                return (this.version == 0 ? handle : this.handle).call();
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error while getting the generic.", e);
                return null;
            }
        }
    }
}