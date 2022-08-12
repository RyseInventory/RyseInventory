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

package io.github.rysefoxx.v1_18;


import io.github.rysefoxx.IntelligentItemColorWrapper;
import net.md_5.bungee.api.ChatColor;


/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 6/10/2022
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
        if (input == null) {
            int red = rgb[0];
            int green = rgb[1];
            int blue = rgb[2];

            String hex = "#" + toHex(red) + toHex(green) + toHex(blue);

            return ChatColor.of(hex);
        }

        return ChatColor.of(input);
    }

}
