package at.conrunner.shift.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchDto {
    private String name;
}
