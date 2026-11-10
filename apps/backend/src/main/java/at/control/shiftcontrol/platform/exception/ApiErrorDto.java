package at.control.shiftcontrol.platform.exception;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiErrorDto {
    String message;
}
