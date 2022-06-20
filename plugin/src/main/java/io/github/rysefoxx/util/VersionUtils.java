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

package io.github.rysefoxx.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 4/22/2022
 */
public class VersionUtils {

    @Contract(pure = true)
    private VersionUtils(){}

    private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    private static final int SUB_VERSION = Integer.parseInt(VERSION.replaceAll("_R\\d", "").replace("v", "").replaceFirst("1_", ""));

    @Contract(pure = true)
    public static int getSubVersion() {
        return SUB_VERSION;
    }

    @Contract(pure = true)
    public static boolean isAtleast16() {
        return SUB_VERSION >= 16;
    }

    @Contract(pure = true)
    public static boolean isBelowAnd13() {
        return SUB_VERSION <= 13;
    }

}
