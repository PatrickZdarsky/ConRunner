package at.control.shiftcontrol.platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FileExportException extends RuntimeException {
    public FileExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
