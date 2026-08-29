package at.control.shiftcontrol.identity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @NotNull
    private String username;

    @NotNull
    private String email;

    @NotNull
    private String avatarUrl;

    @Builder.Default
    private List<ApplicationRole> applicationRoles = List.of();
}
