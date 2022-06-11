package io.github.rysefoxx.pagination;

import com.google.common.base.Preconditions;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import io.github.rysefoxx.content.IntelligentItem;
import io.github.rysefoxx.enums.AnimatorDirection;
import io.github.rysefoxx.enums.TimeSetting;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnegative;
import java.util.*;

/**
 * @author Rysefoxx | Rysefoxx#6772
 * @since 4/15/2022
 */
public class SlideAnimation {

    private List<Integer> from = new ArrayList<>();
    private List<Integer> to = new ArrayList<>();
    private List<IntelligentItem> items = new ArrayList<>();
    private int period = 20;
    private int delay = 20;
    private AnimatorDirection direction;
    private Object identifier;
    private InventoryContents contents;
    private boolean blockClickEvent = false;
    private static Plugin plugin;

    private final List<BukkitTask> task = new ArrayList<>();

    private final HashMap<Integer, Integer> timeHandler = new HashMap<>();

    public static Builder builder(Plugin plugin) {
        SlideAnimation.plugin = plugin;
        return new Builder();
    }

    public static class Builder {
        private SlideAnimation preset;
        private List<Integer> from = new ArrayList<>();
        private List<Integer> to = new ArrayList<>();
        private List<IntelligentItem> items = new ArrayList<>();
        private int period = 20;
        private int delay = 20;

        private AnimatorDirection direction;
        private Object identifier;
        private boolean blockClickEvent = false;

        /**
         * This blocks the InventoryClickEvent until the animation is over.
         *
         * @return The Builder to perform further editing.
         */
        public Builder blockClickEvent() {
            this.blockClickEvent = true;
            return this;
        }

        /**
         * Takes over all properties of the passed animator.
         *
         * @param preset The animator to be copied.
         * @return The Builder to perform further editing.
         * @apiNote When copying the animator, the identification is not copied if present!
         */
        public Builder copy(SlideAnimation preset) {
            this.preset = preset;
            return this;
        }

        /**
         * Defines how the item should be animated in the inventory.
         *
         * @param direction
         * @return The Builder to perform further editing.
         */
        public Builder direction(AnimatorDirection direction) {
            this.direction = direction;
            return this;
        }

        /**
         * Specifies the delay before the animation starts.
         *
         * @param time
         * @param setting
         * @return The Builder to perform further editing.
         */
        public Builder delay(@Nonnegative int time, TimeSetting setting) {
            this.delay = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Sets the speed of the animation in the scheduler.
         *
         * @param time
         * @param setting
         * @return The Builder to perform further editing.
         */
        public Builder period(@Nonnegative int time, TimeSetting setting) {
            this.period = setting == TimeSetting.MILLISECONDS ? time : setting == TimeSetting.SECONDS ? time * 20 : setting == TimeSetting.MINUTES ? (time * 20) * 60 : time;
            return this;
        }

        /**
         * Adds a single start position.
         *
         * @param slot
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is > 53
         */
        public Builder from(@Nonnegative int slot) throws IllegalArgumentException {
            if (slot > 53) {
                throw new IllegalArgumentException("The slot must not be larger than 53.");
            }
            this.from.add(slot);
            return this;
        }

        /**
         * Adds a single start position.
         *
         * @param row
         * @param column
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if row is > 5 or if column > 8
         */
        public Builder from(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
            if (row > 5) {
                throw new IllegalArgumentException("The row must not be larger than 5.");
            }
            if (column > 8) {
                throw new IllegalArgumentException("The column must not be larger than 9.");
            }

            this.from.add(row * 9 + column);
            return this;
        }

        /**
         * Add multiple start positions.
         *
         * @param slots
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is > 53
         */
        public Builder from(Integer... slots) throws IllegalArgumentException {
            for (Integer slot : slots) {
                from(slot);
            }
            return this;
        }

        /**
         * Add multiple start positions.
         *
         * @param rows
         * @param columns
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if row is > 5 or if column > 8
         */
        public Builder from(Integer[] rows, Integer[] columns) throws IllegalArgumentException {
            Preconditions.checkArgument(rows.length == columns.length, "Rows must have the same length as columns.");


            for (int i = 0; i < rows.length; i++) {
                from(rows[i], columns[i]);
            }

            return this;
        }

        /**
         * Add multiple start positions.
         *
         * @param slots
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is > 53
         * @apiNote We recommend passing the list from small to large. e.g .from(Arrays.asList(1, 1, 4)) NOT .from(Arrays.asList(4,1,1))
         */
        public Builder from(List<Integer> slots) throws IllegalArgumentException {
            slots.forEach(this::from);
            return this;
        }

        /**
         * Add multiple start positions.
         *
         * @param rows
         * @param columns
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if row is > 5 or if column > 8
         */
        public Builder from(List<Integer> rows, List<Integer> columns) throws IllegalArgumentException {
            Preconditions.checkArgument(rows.size() == columns.size(), "Rows must have the same length as columns.");

            for (int i = 0; i < rows.size(); i++) {
                from(rows.get(i), columns.get(i));
            }
            return this;
        }

        /**
         * Adds a single end position
         *
         * @param slot
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is > 53
         */
        public Builder to(@Nonnegative int slot) throws IllegalArgumentException {
            if (slot > 53) {
                throw new IllegalArgumentException("The slot must not be larger than 53.");
            }
            this.to.add(slot);
            return this;
        }

        /**
         * Adds a single end position.
         *
         * @param row
         * @param column
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if row is > 5 or if column > 8
         */
        public Builder to(@Nonnegative int row, @Nonnegative int column) throws IllegalArgumentException {
            if (row > 5) {
                throw new IllegalArgumentException("The row must not be larger than 5.");
            }
            if (column > 8) {
                throw new IllegalArgumentException("The column must not be larger than 9.");
            }

            this.to.add(row * 9 + column);
            return this;
        }

        /**
         * Add multiple end positions.
         *
         * @param slots
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is > 53
         */
        public Builder to(Integer... slots) throws IllegalArgumentException {
            for (Integer slot : slots) {
                to(slot);
            }
            return this;
        }

        /**
         * Add multiple end positions.
         *
         * @param rows
         * @param columns
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if row is > 5 or if column > 8
         */
        public Builder to(Integer[] rows, Integer[] columns) throws IllegalArgumentException {
            Preconditions.checkArgument(rows.length == columns.length, "Rows must have the same length as columns.");

            for (int i = 0; i < rows.length; i++) {
                to(rows[i], columns[i]);
            }
            return this;
        }

        /**
         * Add multiple end positions.
         *
         * @param slots
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if slot is > 53
         */
        public Builder to(List<Integer> slots) throws IllegalArgumentException {
            slots.forEach(this::to);
            return this;
        }

        /**
         * Add multiple end positions.
         *
         * @param rows
         * @param columns
         * @return The Builder to perform further editing.
         * @throws IllegalArgumentException if row is > 5 or if column > 8
         */
        public Builder to(List<Integer> rows, List<Integer> columns) throws IllegalArgumentException {
            Preconditions.checkArgument(rows.size() == columns.size(), "Rows must have the same length as columns.");

            for (int i = 0; i < rows.size(); i++) {
                to(rows.get(i), columns.get(i));
            }
            return this;
        }

        /**
         * Add an item, which will appear animated in the inventory.
         *
         * @param item
         * @return The Builder to perform further editing.
         */
        public Builder item(IntelligentItem item) {
            ItemStack itemStack = item.getItemStack();

            NBTItem nbtItem = new NBTItem(itemStack);
            nbtItem.setString("RYSEINVENTORY_SLIDE_ANIMATION_KEY", UUID.randomUUID().toString());

            item.update(nbtItem.getItem());

            this.items.add(item);
            return this;
        }

        /**
         * Add multiple items that will appear animated in the inventory.
         *
         * @param items
         * @return The Builder to perform further editing.
         */
        public Builder item(IntelligentItem... items) {
            for (IntelligentItem item : items) {
                item(item);
            }
            return this;
        }

        /**
         * Add multiple items that will appear animated in the inventory.
         *
         * @param items
         * @return The Builder to perform further editing.
         */
        public Builder items(IntelligentItem... items) {
            return item(items);
        }

        /**
         * Add multiple items that will appear animated in the inventory.
         *
         * @param items
         * @return The Builder to perform further editing.
         */
        public Builder item(List<IntelligentItem> items) {
            items.forEach(this::item);
            return this;
        }

        /**
         * Add multiple items that will appear animated in the inventory.
         *
         * @param items
         * @return The Builder to perform further editing.
         */
        public Builder items(List<IntelligentItem> items) {
            return item(items);
        }


        /**
         * Gives the Animation an identification
         *
         * @param identifier The ID through which you can get the animation
         * @return The Builder to perform further editing
         */
        public Builder identifier(Object identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * This creates the animation class but does not start it yet! {@link SlideAnimation#animate(InventoryContents)}
         *
         * @return The animation class
         * @throws IllegalArgumentException if no start position was specified, if no end position was specified, if period is empty or if items is empty.
         * @throws IllegalStateException    if manager is null
         * @throws NullPointerException     if direction is null.
         */
        public SlideAnimation build() throws IllegalArgumentException, NullPointerException {
            if (this.preset != null) {
                this.to = this.preset.to;
                this.from = this.preset.from;
                this.items = this.preset.items;
                this.direction = this.preset.direction;
                this.period = this.preset.period;
                this.delay = this.preset.delay;
                this.blockClickEvent = this.preset.blockClickEvent;
            }

            if (this.to.isEmpty()) {
                throw new IllegalArgumentException("No start positions were found. Please add start positions to make the animation work.");
            }
            if (this.from.isEmpty()) {
                throw new IllegalArgumentException("No end positions were found. Please add end positions to make the animation work.");
            }
            if (this.items.isEmpty()) {
                throw new IllegalArgumentException("No items were found. Please add items to make the animation work.");
            }
            if (this.direction == null) {
                throw new NullPointerException("Direction is null. Please specify a direction for the animation.");
            }
            Preconditions.checkArgument(this.from.size() == this.to.size(), "from and to must have the same size");
            Preconditions.checkArgument(this.from.size() == this.items.size(), "from and items must have the same size");


            SlideAnimation slideAnimation = new SlideAnimation();
            slideAnimation.to = this.to;
            slideAnimation.from = this.from;
            slideAnimation.items = this.items;
            slideAnimation.direction = this.direction;
            slideAnimation.period = this.period;
            slideAnimation.identifier = this.identifier;
            slideAnimation.delay = this.delay;
            slideAnimation.blockClickEvent = this.blockClickEvent;
            return slideAnimation;
        }
    }

    /**
     * This starts the animation for the inventory.
     *
     * @param contents
     * @throws IllegalArgumentException if invalid data was passed in the builder.
     */
    public void animate(InventoryContents contents) throws IllegalArgumentException {
        this.contents = contents;
        RyseInventory inventory = contents.pagination().inventory();

        for (int i = 0; i < this.from.size(); i++) {
            int from = this.from.get(i);
            int to = this.to.get(i);

            checkIfInvalid(from, to, inventory);
        }

        animateByTyp();
    }

    /**
     * This starts the animation for the inventory.
     * @param plugin   Your main class that extends the JavaPlugin.
     * @param contents The inventory contents.
     * @throws IllegalArgumentException if invalid data was passed in the builder.
     * @deprecated Use {@link #animate(InventoryContents)} instead.
     */
    @Deprecated
    public void animate(JavaPlugin plugin, InventoryContents contents) throws IllegalArgumentException {
        animate(contents);
    }

    private void animateByTyp() {
        if (this.direction == AnimatorDirection.HORIZONTAL_LEFT_RIGHT || this.direction == AnimatorDirection.HORIZONTAL_RIGHT_LEFT) {
            animateHorizontal();
            return;
        }
        if (this.direction == AnimatorDirection.VERTICAL_UP_DOWN || this.direction == AnimatorDirection.VERTICAL_DOWN_UP) {
            animateVertical();
            return;
        }
        if (this.direction == AnimatorDirection.DIAGONAL_TOP_LEFT || this.direction == AnimatorDirection.DIAGONAL_BOTTOM_LEFT) {
            animateDiagonalTopBottomLeft();
            return;
        }
        if (this.direction == AnimatorDirection.DIAGONAL_TOP_RIGHT || this.direction == AnimatorDirection.DIAGONAL_BOTTOM_RIGHT) {
            animateDiagonalTopBottomRight();
        }
    }

    private void animateDiagonalTopBottomLeft() {
        for (int i = 0; i < this.items.size(); i++) {
            boolean moreDelay = i != 0 && Objects.equals(this.from.get(i), this.from.get(i - 1));

            final int[] wait = {this.timeHandler.getOrDefault(this.from.get(i), 0) + 2};

            if (moreDelay) {
                this.timeHandler.put(this.from.get(i), wait[0]);
            }

            int finalI = i;

            BukkitTask task = new BukkitRunnable() {
                int fromIndex = from.get(finalI);
                final int toIndex = to.get(finalI);
                int previousIndex = fromIndex;
                final IntelligentItem item = items.get(finalI);
                final boolean isTopLeft = direction == AnimatorDirection.DIAGONAL_TOP_LEFT;

                @Override
                public void run() {
                    if (moreDelay && wait[0] > 0) {
                        wait[0]--;
                        return;
                    }

                    if (this.isTopLeft) {
                        if (this.fromIndex > this.toIndex) {
                            cancel();
                            return;
                        }
                    } else if (this.fromIndex < this.toIndex) {
                        cancel();
                        return;
                    }

                    if (this.fromIndex == from.get(finalI)) {
                        contents.set(this.fromIndex, this.item);
                    } else {

                        Optional<IntelligentItem> optionalPrevious = contents.get(this.previousIndex);
                        String currentKey = getKey(item);

                        optionalPrevious.ifPresent(intelligentItem -> {
                            String previousKey = getKey(intelligentItem);

                            if (previousKey.equals(currentKey)) {
                                contents.removeItemWithConsumer(this.previousIndex);
                            }
                        });
                        contents.set(this.fromIndex, this.item);
                    }

                    this.previousIndex = this.fromIndex;
                    if (this.isTopLeft) {
                        this.fromIndex += 10;
                        return;
                    }
                    this.fromIndex -= 8;
                }
            }.runTaskTimer(plugin, this.delay, this.period);
            this.task.add(task);
        }
    }

    private void animateDiagonalTopBottomRight() {
        for (int i = 0; i < this.items.size(); i++) {
            boolean moreDelay = i != 0 && Objects.equals(this.from.get(i), this.from.get(i - 1));

            final int[] wait = {this.timeHandler.getOrDefault(this.from.get(i), 0) + 2};

            if (moreDelay) {
                this.timeHandler.put(this.from.get(i), wait[0]);
            }

            int finalI = i;

            BukkitTask task = new BukkitRunnable() {
                int fromIndex = from.get(finalI);
                final int toIndex = to.get(finalI);
                int previousIndex = fromIndex;
                final IntelligentItem item = items.get(finalI);
                final boolean isTopRight = direction == AnimatorDirection.DIAGONAL_TOP_RIGHT;

                @Override
                public void run() {
                    if (moreDelay && wait[0] > 0) {
                        wait[0]--;
                        return;
                    }

                    if (this.isTopRight) {
                        if (this.fromIndex > this.toIndex) {
                            cancel();
                            return;
                        }
                    } else if (this.fromIndex < this.toIndex) {
                        cancel();
                        return;
                    }

                    if (this.fromIndex == from.get(finalI)) {
                        contents.set(this.fromIndex, this.item);
                    } else {

                        Optional<IntelligentItem> optionalPrevious = contents.get(this.previousIndex);
                        String currentKey = getKey(item);

                        optionalPrevious.ifPresent(intelligentItem -> {
                            String previousKey = getKey(intelligentItem);

                            if (previousKey.equals(currentKey)) {
                                contents.removeItemWithConsumer(this.previousIndex);
                            }
                        });
                        contents.set(this.fromIndex, this.item);
                    }

                    this.previousIndex = this.fromIndex;
                    if (this.isTopRight) {
                        this.fromIndex += 8;
                        return;
                    }
                    this.fromIndex -= 10;
                }
            }.runTaskTimer(plugin, this.delay, this.period);
            this.task.add(task);
        }
    }

    private void animateHorizontal() {
        for (int i = 0; i < this.items.size(); i++) {
            boolean moreDelay = i != 0 && Objects.equals(this.from.get(i), this.from.get(i - 1));

            final int[] wait = {this.timeHandler.getOrDefault(this.from.get(i), 0) + 2};

            if (moreDelay) {
                this.timeHandler.put(this.from.get(i), wait[0]);
            }

            int finalI = i;
            BukkitTask task = new BukkitRunnable() {
                int fromIndex = from.get(finalI);
                final int toIndex = to.get(finalI);
                int previousIndex = fromIndex;
                final IntelligentItem item = items.get(finalI);

                final boolean leftToRight = direction == AnimatorDirection.HORIZONTAL_LEFT_RIGHT;

                @Override
                public void run() {
                    if (moreDelay && wait[0] > 0) {
                        wait[0]--;
                        return;
                    }

                    if (this.leftToRight) {
                        if (this.fromIndex > this.toIndex) {
                            cancel();
                            return;
                        }
                    } else if (this.fromIndex < this.toIndex) {
                        cancel();
                        return;
                    }

                    if (this.fromIndex == from.get(finalI)) {
                        contents.set(this.fromIndex, this.item);
                    } else {

                        Optional<IntelligentItem> optionalPrevious = contents.get(this.previousIndex);
                        String currentKey = getKey(item);

                        optionalPrevious.ifPresent(intelligentItem -> {
                            String previousKey = getKey(intelligentItem);

                            if (previousKey.equals(currentKey)) {
                                contents.removeItemWithConsumer(this.previousIndex);
                            }
                        });
                        contents.set(this.fromIndex, this.item);
                    }


                    this.previousIndex = this.fromIndex;
                    if (this.leftToRight) {
                        this.fromIndex++;
                        return;
                    }
                    this.fromIndex--;

                }
            }.runTaskTimer(plugin, this.delay, this.period);
            this.task.add(task);
        }
    }

    private void animateVertical() {
        for (int i = 0; i < this.items.size(); i++) {
            boolean moreDelay = i != 0 && Objects.equals(this.from.get(i), this.from.get(i - 1));

            final int[] wait = {this.timeHandler.getOrDefault(this.from.get(i), 0) + 2};

            if (moreDelay) {
                this.timeHandler.put(this.from.get(i), wait[0]);
            }

            int finalI = i;

            BukkitTask task = new BukkitRunnable() {
                int fromIndex = from.get(finalI);
                final int toIndex = to.get(finalI);
                int previousIndex = fromIndex;
                final IntelligentItem item = items.get(finalI);
                final boolean upToDown = direction == AnimatorDirection.VERTICAL_UP_DOWN;

                @Override
                public void run() {
                    if (moreDelay && wait[0] > 0) {
                        wait[0]--;
                        return;
                    }

                    if (this.upToDown) {
                        if (this.fromIndex > this.toIndex) {
                            cancel();
                            return;
                        }
                    } else if (this.fromIndex < this.toIndex) {
                        cancel();
                        return;
                    }

                    if (this.fromIndex == from.get(finalI)) {
                        contents.set(this.fromIndex, this.item);
                    } else {

                        Optional<IntelligentItem> optionalPrevious = contents.get(this.previousIndex);
                        String currentKey = getKey(item);

                        optionalPrevious.ifPresent(intelligentItem -> {
                            String previousKey = getKey(intelligentItem);

                            if (previousKey.equals(currentKey)) {
                                contents.removeItemWithConsumer(this.previousIndex);
                            }
                        });
                        contents.set(this.fromIndex, this.item);
                    }

                    this.previousIndex = this.fromIndex;
                    if (this.upToDown) {
                        this.fromIndex += 9;
                        return;
                    }
                    this.fromIndex -= 9;

                }
            }.runTaskTimer(plugin, this.delay, this.period);
            this.task.add(task);
        }
    }

    private String getKey(IntelligentItem item) {
        ItemStack previousItemStack = item.getItemStack();

        NBTContainer container = NBTItem.convertItemtoNBT(previousItemStack);
        return container.hasKey("RYSEINVENTORY_SLIDE_ANIMATION_KEY") ? container.getString("RYSEINVENTORY_SLIDE_ANIMATION_KEY") : "";
    }

    private void checkIfInvalid(@Nonnegative int from, @Nonnegative int to, RyseInventory inventory) {
        if (this.direction == AnimatorDirection.HORIZONTAL_LEFT_RIGHT || this.direction == AnimatorDirection.HORIZONTAL_RIGHT_LEFT) {
            if ((from - 1) / 9 != (to - 1) / 9 && from / 9 != to / 9) {
                throw new IllegalArgumentException("The start position " + from + " and the end position " + to + " are not on the same row.");
            }
            if (this.direction == AnimatorDirection.HORIZONTAL_LEFT_RIGHT) {
                if (from < to) return;
                throw new IllegalArgumentException("An animation from left to right requires that the values in to() are larger than the values in from()");
            } else {
                if (to < from) return;
                throw new IllegalArgumentException("An animation from right to left requires that the values in from() are larger than the values in to()");
            }
        } else if (this.direction == AnimatorDirection.VERTICAL_UP_DOWN || this.direction == AnimatorDirection.VERTICAL_DOWN_UP) {
            if (from % 9 != to % 9) {
                throw new IllegalArgumentException("The start position " + from + " and the end position " + to + " are not on the same column.");
            }
            if (this.direction == AnimatorDirection.VERTICAL_UP_DOWN) {
                if (from < to) return;
                throw new IllegalArgumentException("An animation from up to down requires that the values in to() are larger than the values in from()");
            } else {
                if (to < from) return;
                throw new IllegalArgumentException("An animation from down to up requires that the values in from() are larger than the values in to()");
            }
        }

        if (from == to) {
            throw new IllegalArgumentException("The animation could not be started! from " + from + " and to " + to + " have the same values. from must be smaller than " + to + ".");
        }
        if (from > inventory.size()) {
            throw new IllegalArgumentException("The start slot must not be larger than the inventory size.");
        }
        if (to > inventory.size()) {
            throw new IllegalArgumentException("The end slot must not be larger than the inventory size.");
        }
    }

    protected boolean isBlockClickEvent() {
        return this.blockClickEvent;
    }

    protected List<BukkitTask> getTasks() {
        return this.task;
    }

    protected Object getIdentifier() {
        return this.identifier;
    }
}
