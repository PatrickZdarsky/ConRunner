package at.conrunner.identity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchRequest {
    private String name;
}
