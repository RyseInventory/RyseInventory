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

package io.github.rysefoxx.inventory.plugin.content;

import io.github.rysefoxx.inventory.api.IntelligentItemColorWrapper;
import io.github.rysefoxx.inventory.plugin.util.VersionUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx(Rysefoxx # 6772) | eazypaulCode(eazypaulCode # 0001) |
 * @since 4/12/2022
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IntelligentItemColor {

    private static IntelligentItemColorWrapper<net.md_5.bungee.api.ChatColor> colorWrapper;
    private ChatColor bukkitColor;
    private net.md_5.bungee.api.ChatColor bungeeColor;
    private String hexColor;
    private int[] rgbColor = new int[3];
    private boolean bold;
    private boolean underline;
    private boolean italic;
    private boolean obfuscated;
    private boolean strikeThrough;

    @Contract(pure = true)
    private IntelligentItemColor(@NotNull String hexColor,
                                 boolean bold,
                                 boolean underline,
                                 boolean italic,
                                 boolean obfuscated,
                                 boolean strikeThrough) {
        this.hexColor = hexColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    @Contract(pure = true)
    private IntelligentItemColor(int[] rgbColor,
                                 boolean bold,
                                 boolean underline,
                                 boolean italic,
                                 boolean obfuscated,
                                 boolean strikeThrough) {
        this.rgbColor = rgbColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    @Contract(pure = true)
    private IntelligentItemColor(@NotNull net.md_5.bungee.api.ChatColor bungeeColor,
                                 boolean bold,
                                 boolean underline,
                                 boolean italic,
                                 boolean obfuscated,
                                 boolean strikeThrough) {
        this.bungeeColor = bungeeColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    @Contract(pure = true)
    private IntelligentItemColor(@NotNull ChatColor bukkitColor,
                                 boolean bold,
                                 boolean underline,
                                 boolean italic,
                                 boolean obfuscated,
                                 boolean strikeThrough) {
        this.bukkitColor = bukkitColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }


    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * If the color is already set, return it. If it's not set, try to get it from the color wrapper. If the color wrapper
     * is null, throw an exception
     * <br> <br>
     * <font color="red">This is an internal method! <b>ANYTHING</b> about this method can change. It is not recommended to use this method.</font>
     * <br> <br>
     *
     * @return The color of the text.
     */
    @ApiStatus.Internal
    public @NotNull net.md_5.bungee.api.ChatColor getColor() {
        if (this.bungeeColor != null) return this.bungeeColor;
        if (this.bukkitColor != null) return this.bukkitColor.asBungee();

        if (colorWrapper == null)
            throw new IllegalStateException("The color wrapper is null. Please report this to the developer. Your server is running " + VersionUtils.getSubVersion() + ".");

        if (this.hexColor != null && !this.hexColor.isEmpty())
            return colorWrapper.getColor(this.hexColor, null);

        return colorWrapper.getColor(null, this.rgbColor);
    }

    /**
     * @return true if the color is bold
     */
    public boolean isBold() {
        return this.bold;
    }

    /**
     * @return true if the color is underlined
     */
    public boolean isUnderline() {
        return this.underline;
    }

    /**
     * @return true if the color is italic
     */
    public boolean isItalic() {
        return this.italic;
    }

    /**
     * @return true if the color is obfuscated
     */
    public boolean isObfuscated() {
        return this.obfuscated;
    }

    /**
     * @return true if the color is striked through
     */
    public boolean isStrikeThrough() {
        return this.strikeThrough;
    }

    public static class Builder {
        private final IntelligentItemColor color = new IntelligentItemColor();
        private boolean needWrapper;

        @SneakyThrows
        @SuppressWarnings("unchecked")
        private static void tryToSetWrapper() {
            final Class<?> clazz = Class.forName("io.github.rysefoxx.v1_" + VersionUtils.getSubVersion() + ".ColorHandler");
            if (IntelligentItemColorWrapper.class.isAssignableFrom(clazz)) {
                colorWrapper = (IntelligentItemColorWrapper<net.md_5.bungee.api.ChatColor>) clazz.getConstructor().newInstance();
            }
        }

        /**
         * With this method, the letter will be bold.
         *
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder bold() {
            this.color.bold = true;
            return this;
        }

        /**
         * In this method, the letter is displayed crossed out.
         *
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder strikeThrough() {
            this.color.strikeThrough = true;
            return this;
        }

        /**
         * With this method, the letter will be underlined.
         *
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder underline() {
            this.color.underline = true;
            return this;
        }

        /**
         * With this method, the letter will be italic.
         *
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder italic() {
            this.color.italic = true;
            return this;
        }

        /**
         * With this method, the letter will be obfuscated.
         *
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder obfuscate() {
            this.color.obfuscated = true;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param bukkitColor ChatColor from org.bukkit
         * @return The Builder to perform further editing.
         */
        public @NotNull Builder bukkitColor(@NotNull ChatColor bukkitColor) {
            if (bukkitColor == ChatColor.UNDERLINE
                    || bukkitColor == ChatColor.ITALIC
                    || bukkitColor == ChatColor.STRIKETHROUGH
                    || bukkitColor == ChatColor.RESET
                    || bukkitColor == ChatColor.BOLD
                    || bukkitColor == ChatColor.MAGIC) {
                throw new IllegalArgumentException("Please pass a valid ChatColor.");
            }
            this.color.bukkitColor = bukkitColor;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param bungeeColor ChatColor from net.md_5.bungee.api
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameter is not a valid color.
         */
        public @NotNull Builder bungeeColor(@NotNull net.md_5.bungee.api.ChatColor bungeeColor) throws IllegalArgumentException {
            if (bungeeColor == net.md_5.bungee.api.ChatColor.UNDERLINE
                    || bungeeColor == net.md_5.bungee.api.ChatColor.ITALIC
                    || bungeeColor == net.md_5.bungee.api.ChatColor.STRIKETHROUGH
                    || bungeeColor == net.md_5.bungee.api.ChatColor.RESET
                    || bungeeColor == net.md_5.bungee.api.ChatColor.BOLD
                    || bungeeColor == net.md_5.bungee.api.ChatColor.MAGIC) {
                throw new IllegalArgumentException("Please pass a valid ChatColor.");
            }
            this.color.bungeeColor = bungeeColor;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param red   The red value of the color.
         * @param green The green value of the color.
         * @param blue  The blue value of the color.
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException     If one of the parameters is greater than 255
         * @throws UnsupportedClassVersionError If the server is running under 1.16.
         */
        public @NotNull Builder rgbColor(@Nonnegative int red, @Nonnegative int green, @Nonnegative int blue)
                throws IllegalArgumentException, UnsupportedClassVersionError {
            if (!VersionUtils.isAtleast16())
                throw new UnsupportedClassVersionError("For RGB color, the server must be running on at least 1.16.");

            if (red > 255 || green > 255 || blue > 255)
                throw new IllegalArgumentException("The RGB color can not be greater than 255.");

            this.needWrapper = true;

            this.color.rgbColor[0] = red;
            this.color.rgbColor[1] = green;
            this.color.rgbColor[2] = blue;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param number e.g 4 for DARK_RED
         * @return The Builder to perform further editing
         * @throws NullPointerException if an invalid character was passed.
         *                              <p>
         *                              Transfer a char from the ChatColor of org.bukkit
         */
        public @NotNull Builder colorByChar(char number) throws NullPointerException {
            ChatColor color = ChatColor.getByChar(number);

            if (color == null)
                throw new NullPointerException("No ChatColor with the character " + number + " could be found.");

            this.color.bukkitColor = color;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param paragraph e.g §4 for DARK_RED
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException if the parameter is not invalid.
         * @throws NullPointerException     if no ChatColor could be found.
         */
        public @NotNull Builder paragraph(@NotNull String paragraph) throws IllegalArgumentException, NullPointerException {
            if (paragraph.length() > 2)
                throw new IllegalArgumentException("The parameter must not be longer than 2 characters. This is how a transfer could look like paragraph(§3).");

            return colorByChar(paragraph.charAt(1));
        }

        /**
         * The letter gets this color
         *
         * @param hexColor e.g #FF0000 for RED
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException     If it is not a valid hex string.
         * @throws UnsupportedClassVersionError If the server is running under 1.16.
         */
        public @NotNull Builder hexColor(@NotNull String hexColor) throws IllegalArgumentException, UnsupportedClassVersionError {
            if (!VersionUtils.isAtleast16())
                throw new UnsupportedClassVersionError("For Hex color, the server must be running on at least 1.16.");

            if (!hexColor.startsWith("#"))
                hexColor = "#" + hexColor;
            if (hexColor.length() > 7)
                throw new IllegalArgumentException("The hex input must not be longer than 7 characters.");

            this.needWrapper = true;

            this.color.hexColor = hexColor;
            return this;
        }

        public @Nullable IntelligentItemColor build() {
            if (this.needWrapper)
                tryToSetWrapper();

            if (this.color.hexColor != null)
                return new IntelligentItemColor(
                        this.color.hexColor, this.color.bold, this.color.underline, this.color.italic,
                        this.color.obfuscated, this.color.strikeThrough);

            if (this.color.bukkitColor != null)
                return new IntelligentItemColor(
                        this.color.bukkitColor, this.color.bold, this.color.underline, this.color.italic,
                        this.color.obfuscated, this.color.strikeThrough);

            if (this.color.bungeeColor != null)
                return new IntelligentItemColor(
                        this.color.bungeeColor, this.color.bold, this.color.underline, this.color.italic,
                        this.color.obfuscated, this.color.strikeThrough);

            if (this.color.rgbColor.length > 0)
                return new IntelligentItemColor(
                        this.color.rgbColor, this.color.bold, this.color.underline, this.color.italic,
                        this.color.obfuscated, this.color.strikeThrough);

            return null;
        }
    }
}
