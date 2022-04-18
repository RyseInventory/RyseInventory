package io.github.rysefoxx.content;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.awt.*;

/**
 * @author Rysefoxx(Rysefoxx # 6772) | eazypaulcode(eazypaulCode#0001) |
 * @since 4/12/2022
 */
public record IntelligentItemColor(String color, boolean bold, boolean underline, boolean italic,
                                   boolean obfuscated, boolean strikeThrough) {

    @Contract(pure = true)
    public IntelligentItemColor(@NotNull String color, boolean bold, boolean underline, boolean italic, boolean obfuscated, boolean strikeThrough) {
        this.color = color;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    @Contract(value = " -> new", pure = true)
    public static @NotNull
    Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String hexColor;
        private boolean bold;
        private boolean underline;
        private boolean italic;
        private boolean obfuscated;
        private boolean strikeThrough;

        private @NotNull
        String toHex(int value) {
            StringBuilder hex = new StringBuilder(Integer.toHexString(value));
            while (hex.length() < 2) {
                hex.append("0");
            }
            return hex.toString();
        }

        /**
         * With this method, the letter will be bold.
         *
         * @return The Builder to perform further editing.
         */
        public Builder bold() {
            this.bold = true;
            return this;
        }

        /**
         * In this method, the letter is displayed crossed out.
         *
         * @return The Builder to perform further editing.
         */
        public Builder strikeThrough() {
            this.strikeThrough = true;
            return this;
        }

        /**
         * With this method, the letter will be underlined.
         *
         * @return The Builder to perform further editing.
         */
        public Builder underline() {
            this.underline = true;
            return this;
        }

        /**
         * With this method, the letter will be italic.
         *
         * @return The Builder to perform further editing.
         */
        public Builder italic() {
            this.italic = true;
            return this;
        }

        /**
         * With this method, the letter will be obfuscated.
         *
         * @return The Builder to perform further editing.
         */
        public Builder obfuscate() {
            this.obfuscated = true;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param bukkitColor ChatColor from org.bukkit
         * @return The Builder to perform further editing.
         */
        public Builder bukkitColor(@NotNull ChatColor bukkitColor) {
            return bungeeColor(bukkitColor.asBungee());
        }

        /**
         * The letter gets this color
         *
         * @param bungeeColor ChatColor from net.md_5.bungee.api
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameter is not a valid color.
         */
        public Builder bungeeColor(@NotNull net.md_5.bungee.api.ChatColor bungeeColor) throws IllegalArgumentException {
            if (bungeeColor == net.md_5.bungee.api.ChatColor.UNDERLINE
                    || bungeeColor == net.md_5.bungee.api.ChatColor.ITALIC
                    || bungeeColor == net.md_5.bungee.api.ChatColor.STRIKETHROUGH
                    || bungeeColor == net.md_5.bungee.api.ChatColor.RESET
                    || bungeeColor == net.md_5.bungee.api.ChatColor.BOLD
                    || bungeeColor == net.md_5.bungee.api.ChatColor.MAGIC) {
                throw new IllegalArgumentException("Please pass a valid ChatColor.");
            }

            Color color = bungeeColor.getColor();
            this.hexColor = "#" + toHex(color.getRed()) + toHex(color.getGreen()) + toHex(color.getBlue());
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param red
         * @param green
         * @param blue
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException If one of the parameters is greater than 255
         */
        public Builder rgbColor(@Nonnegative int red, @Nonnegative int green, @Nonnegative int blue) throws IllegalArgumentException {
            if (red > 255 || green > 255 || blue > 255) {
                throw new IllegalArgumentException("The RGB color can not be greater than 255.");
            }

            this.hexColor = "#" + toHex(red) + toHex(green) + toHex(blue);
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param number e.g 4 for DARK_RED
         * @return The Builder to perform further editing
         * @throws NullPointerException if an invalid character was passed.
         * @apiNote Transfer a char from the ChatColor of org.bukkit
         */
        public Builder colorByChar(char number) throws NullPointerException {
            ChatColor color = ChatColor.getByChar(number);

            if (color == null) {
                throw new NullPointerException("No ChatColor with the character " + number + " could be found.");
            }

            return bungeeColor(color.asBungee());
        }

        /**
         * The letter gets this color
         * @param paragraph e.g §4 for DARK_RED
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException if the parameter is not invalid.
         * @throws NullPointerException if no ChatColor could be found.
         */
        public Builder paragraph(@NotNull String paragraph) throws IllegalArgumentException, NullPointerException {
            if (paragraph.length() > 2) {
                throw new IllegalArgumentException("The parameter must not be longer than 2 characters. This is how a transfer could look like paragraph(§3).");
            }
            return colorByChar(paragraph.charAt(1));
        }

        /**
         * The letter gets this color
         *
         * @param hexColor
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException If it is not a valid hex string.
         */
        public Builder hexColor(@NotNull String hexColor) throws IllegalArgumentException {
            if (!hexColor.startsWith("#")) {
                hexColor = "#" + hexColor;
            }

            if (hexColor.length() > 7) {
                throw new IllegalArgumentException("The hex input must not be longer than 7 characters.");
            }

            this.hexColor = hexColor;
            return this;
        }

        public IntelligentItemColor build() {
            return new IntelligentItemColor(this.hexColor, this.bold, this.underline, this.italic, this.obfuscated, this.strikeThrough);
        }
    }

    public @NotNull
    net.md_5.bungee.api.ChatColor getColor() {
        return net.md_5.bungee.api.ChatColor.of(this.color);
    }

    @Contract(pure = true)
    public boolean isBold() {
        return this.bold;
    }

    @Contract(pure = true)
    public boolean isUnderline() {
        return this.underline;
    }

    @Contract(pure = true)
    public boolean isItalic() {
        return this.italic;
    }

    @Contract(pure = true)
    public boolean isObfuscated() {
        return this.obfuscated;
    }

    @Contract(pure = true)
    public boolean isStrikeThrough() {
        return this.strikeThrough;
    }
}
