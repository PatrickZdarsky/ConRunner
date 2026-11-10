package at.control.shiftcontrol.platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotificationSettingAlreadyExistsException extends RuntimeException {
    public NotificationSettingAlreadyExistsException(String message) {
        super(message);
    }
}
