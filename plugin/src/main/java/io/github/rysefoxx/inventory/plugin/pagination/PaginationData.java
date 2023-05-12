package io.github.rysefoxx.inventory.plugin.pagination;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class PaginationData {

    private final List<Integer> slots = new ArrayList<>();
    private final List<Integer> pages = new ArrayList<>();

    public PaginationData(@NotNull PaginationData paginationData) {
        this.slots.addAll(paginationData.slots);
        this.pages.addAll(paginationData.pages);
    }

    public @NotNull PaginationData newInstance() {
        return new PaginationData(this);
    }

    public void add(@Nonnegative int slot, @Nonnegative int page) {
        this.slots.add(slot);
        this.pages.add(page);
    }

    public int getFirstSlot() {
        if (this.slots.isEmpty())
            return -1;

        return this.slots.remove(0);
    }

    public int getFirstPage() {
        if (this.pages.isEmpty())
            return -1;

        return this.pages.remove(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginationData that = (PaginationData) o;
        return Objects.equals(slots, that.slots) && Objects.equals(pages, that.pages);
    }
}
