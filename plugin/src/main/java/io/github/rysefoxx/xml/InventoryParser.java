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

package io.github.rysefoxx.xml;

import io.github.rysefoxx.enums.*;
import io.github.rysefoxx.other.Page;
import io.github.rysefoxx.pagination.RyseInventory;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rysefoxx | Rysefoxx#7880
 * @since 9/25/2022
 */
public class InventoryParser extends DefaultHandler {

    private final StringBuilder currentValue = new StringBuilder();
    private List<RyseInventory.Builder> ryseInventories;

    private final String pathToXml;

    public InventoryParser(String pathToXml) {
        this.pathToXml = pathToXml;
    }

    @Override
    public void startDocument() {
        this.ryseInventories = new ArrayList<>();
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes) {
        qName = qName.toLowerCase();
        currentValue.setLength(0);

        if (qName.equals("inventory"))
            this.ryseInventories.add(RyseInventory.builder().identifier(attributes.getValue("id")));
    }

    @Override
    public void endElement(String uri,
                           String localName,
                           String qName) {
        qName = qName.toLowerCase();

        RyseInventory.Builder builder = this.ryseInventories.get(this.ryseInventories.size() - 1);

        switch (qName) {
            case "fixed_page_size":
                if (!tryParse(currentValue.toString()))
                    return;

                builder.fixedPageSize(Integer.parseInt(currentValue.toString()));
                break;
            case "title":
                builder.title(currentValue.toString());
                break;
            case "clear_and_safe":
                if (currentValue.toString().equalsIgnoreCase("true"))
                    builder.clearAndSafe();
                break;
            case "close_able":
                if (currentValue.toString().equalsIgnoreCase("false"))
                    builder.preventClose();
                break;
            case "ignore_manual_items":
                if (currentValue.toString().equalsIgnoreCase("true"))
                    builder.ignoreManualItems();
                break;
            case "transfer_data":
                if (currentValue.toString().equalsIgnoreCase("false"))
                    builder.preventTransferData();
                break;
            case "title_holder":
                builder.titleHolder(currentValue.toString());
                break;
            case "size":
                if (!tryParse(currentValue.toString()))
                    return;

                builder.size(Integer.parseInt(currentValue.toString()));
                break;
            case "row":
                if (!tryParse(currentValue.toString()))
                    return;

                int row = Integer.parseInt(currentValue.toString());
                if (row < 1)
                    throw new IllegalArgumentException("The number of rows must not be less than 1.");

                builder.rows(row);
                break;
            case "pages":
                if (!currentValue.toString().contains("#")) {
                    addPage(currentValue.toString(), builder);
                    return;
                }
                String[] allPages = currentValue.toString().split("#");
                for (String allPage : allPages)
                    addPage(allPage, builder);

                break;
            case "options":
                if (!currentValue.toString().contains(":")) {
                    InventoryOptions option = InventoryOptions.fromName(currentValue.toString());
                    if (option == null)
                        throw new IllegalArgumentException("The option " + currentValue + " does not exist.");

                    builder.options(option);
                    return;
                }

                String[] options = currentValue.toString().split(",");
                List<InventoryOptions> inventoryOptions = new ArrayList<>();

                for (String optionString : options) {
                    InventoryOptions option = InventoryOptions.fromName(optionString);
                    if (option == null)
                        throw new IllegalArgumentException("The option " + optionString + " does not exist.");

                    inventoryOptions.add(option);
                }

                builder.options(inventoryOptions.toArray(new InventoryOptions[0]));
                break;
            case "ignore_click_event":
                DisabledInventoryClick clickEvent = DisabledInventoryClick.fromName(currentValue.toString());
                if (clickEvent == null)
                    throw new IllegalArgumentException("The click event " + currentValue + " does not exist.");

                builder.ignoreClickEvent(clickEvent);
                break;
            case "close_reason":
                if (!currentValue.toString().contains(":")) {
                    CloseReason closeReason = CloseReason.fromName(currentValue.toString());
                    if (closeReason == null)
                        throw new IllegalArgumentException("The close reason " + currentValue + " does not exist.");

                    builder.close(closeReason);
                    return;
                }

                String[] reasons = currentValue.toString().split(",");
                List<CloseReason> closeReasonList = new ArrayList<>();

                for (String reason : reasons) {
                    CloseReason closeReason = CloseReason.fromName(reason);
                    if (closeReason == null)
                        throw new IllegalArgumentException("The close reason " + reason + " does not exist.");

                    closeReasonList.add(closeReason);
                }

                builder.close(closeReasonList.toArray(new CloseReason[0]));
                break;
            case "delay":
            case "open_delay":
            case "period":
            case "close_after":
            case "load_delay":
            case "load_title":
                int delay;
                TimeSetting timeSetting = null;

                if (currentValue.toString().contains(":")) {
                    String[] data = currentValue.toString().split(":");
                    if (!tryParse(data[0]))
                        return;

                    delay = Integer.parseInt(data[0]);
                    timeSetting = TimeSetting.fromName(data[1]);

                    if (timeSetting == null)
                        throw new IllegalArgumentException("The time setting " + data[1] + " is not valid.");
                } else {
                    if (!tryParse(currentValue.toString()))
                        return;

                    delay = Integer.parseInt(currentValue.toString());
                }

                if (qName.equals("open_delay")) {
                    builder.openDelay(delay, timeSetting);
                    return;
                }
                if (qName.equals("period")) {
                    builder.period(delay, timeSetting);
                    return;
                }
                if (qName.equals("close_after")) {
                    builder.closeAfter(delay, timeSetting);
                    return;
                }
                if (qName.equals("load_delay")) {
                    builder.loadDelay(delay, timeSetting);
                    return;
                }
                if (qName.equals("load_title")) {
                    builder.loadDelay(delay, timeSetting);
                    return;
                }
                builder.delay(delay, timeSetting);
                break;
            case "type":
                InventoryOpenerType type = InventoryOpenerType.fromName(currentValue.toString());

                if (type == null)
                    throw new IllegalArgumentException("The type " + currentValue.toString() + " is not a valid type.");

                builder.type(type);
                break;
        }

    }

    @Override
    public void characters(char[] ch, int start, int length) {
        currentValue.append(ch, start, length);
    }

    /**
     * It parses the XML file and returns a list of RyseInventory.Builder objects.
     *
     * @return A list of RyseInventory.Builder objects.
     */
    public @NotNull List<RyseInventory.Builder> parse() {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try (InputStream is = Files.newInputStream(Paths.get(this.pathToXml))) {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(is, this);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return this.ryseInventories;
    }

    /**
     * This function tries to parse a string into an integer, and if it fails, it prints the stack trace.
     *
     * @param value The value to be parsed.
     * @return true
     */
    private boolean tryParse(@NotNull String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * It adds a page to the inventory
     *
     * @param currentValue The value of the page.
     * @param builder      The builder of the inventory.
     */
    private void addPage(@NotNull String currentValue, @NotNull RyseInventory.Builder builder) {
        String[] data = currentValue.split(",");
        if (!tryParse(data[0]) || !tryParse(data[1]))
            return;

        int page = Integer.parseInt(data[0]);
        int rows = Integer.parseInt(data[1]);
        builder.rows(Page.of(page, rows));
    }
}
