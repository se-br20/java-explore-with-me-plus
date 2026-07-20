package ru.practicum.ewm.comments.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

public final class OffsetBasedPageRequest
        implements Pageable {

    private final long offset;
    private final int pageSize;
    private final Sort sort;

    public OffsetBasedPageRequest(
            long offset,
            int pageSize
    ) {
        this(
                offset,
                pageSize,
                Sort.unsorted()
        );
    }

    public OffsetBasedPageRequest(
            long offset,
            int pageSize,
            Sort sort
    ) {
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "Offset must not be negative"
            );
        }

        if (pageSize < 1) {
            throw new IllegalArgumentException(
                    "Page size must be positive"
            );
        }

        this.offset = offset;
        this.pageSize = pageSize;
        this.sort = Objects.requireNonNull(
                sort,
                "Sort must not be null"
        );
    }

    public static OffsetBasedPageRequest of(
            long offset,
            int pageSize
    ) {
        return new OffsetBasedPageRequest(
                offset,
                pageSize
        );
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(
                offset / pageSize
        );
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetBasedPageRequest(
                offset + pageSize,
                pageSize,
                sort
        );
    }

    @Override
    public Pageable previousOrFirst() {
        if (!hasPrevious()) {
            return first();
        }

        return new OffsetBasedPageRequest(
                Math.max(
                        0L,
                        offset - pageSize
                ),
                pageSize,
                sort
        );
    }

    @Override
    public Pageable first() {
        return new OffsetBasedPageRequest(
                0L,
                pageSize,
                sort
        );
    }

    @Override
    public Pageable withPage(
            int pageNumber
    ) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException(
                    "Page index must not be negative"
            );
        }

        return new OffsetBasedPageRequest(
                (long) pageNumber * pageSize,
                pageSize,
                sort
        );
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0L;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object
                instanceof OffsetBasedPageRequest other)) {

            return false;
        }

        return offset == other.offset
                && pageSize == other.pageSize
                && sort.equals(other.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                offset,
                pageSize,
                sort
        );
    }

    @Override
    public String toString() {
        return "OffsetBasedPageRequest{"
                + "offset="
                + offset
                + ", pageSize="
                + pageSize
                + ", sort="
                + sort
                + '}';
    }
}