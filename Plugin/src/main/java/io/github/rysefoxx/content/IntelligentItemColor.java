package io.github.rysefoxx.content;

import io.github.rysefoxx.IntelligentItemColorWrapper;
import io.github.rysefoxx.util.VersionUtils;
import org.bukkit.ChatColor;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx(Rysefoxx # 6772) | eazypaulcode(eazypaulCode#0001) |
 * @since 4/12/2022
 */
public class IntelligentItemColor {

    private static IntelligentItemColorWrapper<net.md_5.bungee.api.ChatColor> colorWrapper;
    private ChatColor bukkitColor;
    private net.md_5.bungee.api.ChatColor bungeeColor;
    private String hexColor;
    private int[] rgbColor = new int[2];
    private final boolean bold;
    private final boolean underline;
    private final boolean italic;
    private final boolean obfuscated;
    private final boolean strikeThrough;

    public IntelligentItemColor(String hexColor, boolean bold, boolean underline, boolean italic, boolean obfuscated, boolean strikeThrough) {
        this.hexColor = hexColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    public IntelligentItemColor(int[] rgbColor, boolean bold, boolean underline, boolean italic, boolean obfuscated, boolean strikeThrough) {
        this.rgbColor = rgbColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    public IntelligentItemColor(net.md_5.bungee.api.ChatColor bungeeColor, boolean bold, boolean underline, boolean italic, boolean obfuscated, boolean strikeThrough) {
        this.bungeeColor = bungeeColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    public IntelligentItemColor(ChatColor bukkitColor, boolean bold, boolean underline, boolean italic, boolean obfuscated, boolean strikeThrough) {
        this.bukkitColor = bukkitColor;
        this.bold = bold;
        this.underline = underline;
        this.italic = italic;
        this.obfuscated = obfuscated;
        this.strikeThrough = strikeThrough;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ChatColor bukkitColor;
        private net.md_5.bungee.api.ChatColor bungeeColor;
        private String hexColor;
        private int[] rgbColor = new int[2];
        private boolean bold;
        private boolean underline;
        private boolean italic;
        private boolean obfuscated;
        private boolean strikeThrough;



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
        public Builder bukkitColor(ChatColor bukkitColor) {
            if (bukkitColor == ChatColor.UNDERLINE
                    || bukkitColor == ChatColor.ITALIC
                    || bukkitColor == ChatColor.STRIKETHROUGH
                    || bukkitColor == ChatColor.RESET
                    || bukkitColor == ChatColor.BOLD
                    || bukkitColor == ChatColor.MAGIC) {
                throw new IllegalArgumentException("Please pass a valid ChatColor.");
            }
            this.bukkitColor = bukkitColor;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param bungeeColor ChatColor from net.md_5.bungee.api
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException If the parameter is not a valid color.
         */
        public Builder bungeeColor(net.md_5.bungee.api.ChatColor bungeeColor) throws IllegalArgumentException {
            if (bungeeColor == net.md_5.bungee.api.ChatColor.UNDERLINE
                    || bungeeColor == net.md_5.bungee.api.ChatColor.ITALIC
                    || bungeeColor == net.md_5.bungee.api.ChatColor.STRIKETHROUGH
                    || bungeeColor == net.md_5.bungee.api.ChatColor.RESET
                    || bungeeColor == net.md_5.bungee.api.ChatColor.BOLD
                    || bungeeColor == net.md_5.bungee.api.ChatColor.MAGIC) {
                throw new IllegalArgumentException("Please pass a valid ChatColor.");
            }

            this.bungeeColor = bungeeColor;
            return this;
        }

        /**
         * The letter gets this color
         *
         * @param red
         * @param green
         * @param blue
         * @return The Builder to perform further editing
         * @throws IllegalArgumentException     If one of the parameters is greater than 255
         * @throws UnsupportedClassVersionError If the server is running under 1.16.
         */
        public Builder rgbColor(@Nonnegative int red, @Nonnegative int green, @Nonnegative int blue) throws IllegalArgumentException, UnsupportedClassVersionError {
            if (!VersionUtils.isAtleast16()) {
                throw new UnsupportedClassVersionError("For RGB color, the server must be running on at least 1.16.");
            }
            if (red > 255 || green > 255 || blue > 255) {
                throw new IllegalArgumentException("The RGB color can not be greater than 255.");
            }

            this.rgbColor[0] = red;
            this.rgbColor[1] = green;
            this.rgbColor[2] = blue;
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
            this.bukkitColor = color;
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
        public Builder paragraph(String paragraph) throws IllegalArgumentException, NullPointerException {
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
         * @throws IllegalArgumentException     If it is not a valid hex string.
         * @throws UnsupportedClassVersionError If the server is running under 1.16.
         */
        public Builder hexColor(String hexColor) throws IllegalArgumentException, UnsupportedClassVersionError {
            if (!VersionUtils.isAtleast16()) {
                throw new UnsupportedClassVersionError("For Hex color, the server must be running on at least 1.16.");
            }
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
            try {
                final Class<?> clazz = Class.forName("io.github.rysefoxx.v1_" + VersionUtils.getSubVersion() + ".ColorHandler");
                if (IntelligentItemColorWrapper.class.isAssignableFrom(clazz)) {
                    colorWrapper = (IntelligentItemColorWrapper<net.md_5.bungee.api.ChatColor>) clazz.getConstructor().newInstance();
                }
            } catch (final Exception ignored) {}

            if (this.hexColor != null) {
                return new IntelligentItemColor(this.hexColor, this.bold, this.underline, this.italic, this.obfuscated, this.strikeThrough);
            }
            if (this.bukkitColor != null) {
                return new IntelligentItemColor(this.bukkitColor, this.bold, this.underline, this.italic, this.obfuscated, this.strikeThrough);
            }
            if (this.bungeeColor != null) {
                return new IntelligentItemColor(this.bungeeColor, this.bold, this.underline, this.italic, this.obfuscated, this.strikeThrough);
            }
            if (this.rgbColor.length > 0) {
                return new IntelligentItemColor(this.rgbColor, this.bold, this.underline, this.italic, this.obfuscated, this.strikeThrough);
            }
            return null;
        }
    }

    public net.md_5.bungee.api.ChatColor getColor() {
        if (this.bungeeColor != null) return this.bungeeColor;
        if (this.bukkitColor != null) return this.bukkitColor.asBungee();


        if(!this.hexColor.isEmpty()){
            return colorWrapper.getColor(this.hexColor, null);
        }
        return colorWrapper.getColor(null, this.rgbColor);
    }


    public boolean isBold() {
        return this.bold;
    }


    public boolean isUnderline() {
        return this.underline;
    }


    public boolean isItalic() {
        return this.italic;
    }


    public boolean isObfuscated() {
        return this.obfuscated;
    }


    public boolean isStrikeThrough() {
        return this.strikeThrough;
    }
}
