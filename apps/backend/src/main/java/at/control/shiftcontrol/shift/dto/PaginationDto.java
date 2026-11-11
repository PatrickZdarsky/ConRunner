package at.control.shiftcontrol.shift.dto;

import java.util.Collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class PaginationDto<T> {
    @NotNull
    private int pages;

    @NotNull
    private int page;

    @NotNull
    private int total;

    @NotNull
    @Valid
    private Collection<T> items;
}
