package io.github.rysefoxx.v1_16;


import io.github.rysefoxx.IntelligentItemColorWrapper;
import net.md_5.bungee.api.ChatColor;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 4/22/2022
 */
public class ColorHandler implements IntelligentItemColorWrapper<ChatColor> {
    private String toHex(int value) {
        StringBuilder hex = new StringBuilder(Integer.toHexString(value));
        while (hex.length() < 2) {
            hex.append("0");
        }
        return hex.toString();
    }
    @Override
    public ChatColor getColor(String input, int[] rgb) {
        if(input == null) {
            int red = rgb[0];
            int green = rgb[1];
            int blue = rgb[2];

            String hex = "#" + toHex(red) + toHex(green) + toHex(blue);

           return ChatColor.of(hex);
        }

        return ChatColor.of(input);
    }

}
