
package dev.dmv04.notificationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUserEventException extends RuntimeException {
    public InvalidUserEventException(String message) {
        super(message);
    }
}
