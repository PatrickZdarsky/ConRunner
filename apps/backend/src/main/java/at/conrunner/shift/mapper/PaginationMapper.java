package at.conrunner.shift.mapper;

import java.util.Collection;

import lombok.NoArgsConstructor;

import at.conrunner.shift.dto.PaginationDto;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class PaginationMapper {
    public static <T> PaginationDto<T> toPaginationDto(int pageSize, int page, long total, Collection<T> items) {
        int pages = pageSize == 0
            ? 0
            : ((int) total + pageSize - 1) / pageSize;
        return PaginationDto.<T>builder()
            .pages(pages)
            .page(page)
            .total((int) total)
            .items(items)
            .build();
    }
}
