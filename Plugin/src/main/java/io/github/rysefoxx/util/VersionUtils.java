package io.github.rysefoxx.util;

import org.bukkit.Bukkit;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 4/22/2022
 */
public class VersionUtils {

    private static final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    private static final int subVersion = Integer.parseInt(version.replaceAll("_R\\d", "").replace("v", "").replaceFirst("1_", ""));

    public static int getSubVersion() {
        return subVersion;
    }

    public static boolean isAtleast16() {
        return subVersion >= 16;
    }

    public static boolean isBelowAnd13() {
        return subVersion <= 13;
    }

}
