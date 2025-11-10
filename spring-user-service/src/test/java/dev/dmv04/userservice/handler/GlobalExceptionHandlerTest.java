package dev.dmv04.userservice.handler;

import dev.dmv04.userservice.dto.ValidationError;
import dev.dmv04.userservice.exception.EmailAlreadyExistsException;
import dev.dmv04.userservice.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleUserNotFound_ShouldReturnNotFoundStatus() {
        UserNotFoundException ex = new UserNotFoundException(1L);
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users/1");

        var response = globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("User with id 1 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/users/1");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void handleUserNotFound_WithDifferentId_ShouldReturnCorrectMessage() {
        UserNotFoundException ex = new UserNotFoundException(999L);
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users/999");

        var response = globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("User with id 999 not found");
        assertThat(response.getBody().path()).isEqualTo("/api/users/999");
    }

    @Test
    void handleEmailAlreadyExists_ShouldReturnConflictStatus() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("test@example.com");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        var response = globalExceptionHandler.handleEmailAlreadyExists(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).isEqualTo("Email 'test@example.com' already exists");
        assertThat(response.getBody().path()).isEqualTo("/api/users");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void handleValidationExceptions_WithNonNullRejectedValue_ShouldReturnValidationErrorWithStringValue() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "email", "invalid-email",
                false, null, null, "Invalid email format");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().path()).isEqualTo("/api/users");
        assertThat(response.getBody().details()).hasSize(1);

        ValidationError validationError = response.getBody().details().get(0);
        assertThat(validationError.rejectedValue()).isEqualTo("invalid-email");
        assertThat(validationError.field()).isEqualTo("email");
        assertThat(validationError.message()).isEqualTo("Invalid email format");
    }

    @Test
    void handleValidationExceptions_WithNullRejectedValue_ShouldReturnValidationErrorWithNull() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "email", null,
                false, null, null, "Email is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).hasSize(1);

        ValidationError validationError = response.getBody().details().get(0);
        assertThat(validationError.rejectedValue()).isNull();
        assertThat(validationError.field()).isEqualTo("email");
        assertThat(validationError.message()).isEqualTo("Email is required");
    }

    @Test
    void handleValidationExceptions_WithMultipleErrors_ShouldReturnAllValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError emailError = new FieldError("user", "email", null,
                false, null, null, "Email is required");
        FieldError nameError = new FieldError("user", "name", "",
                false, null, null, "Name cannot be empty");
        FieldError ageError = new FieldError("user", "age", -5,
                false, null, null, "Age must be positive");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(emailError, nameError, ageError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().details()).hasSize(3);

        ValidationError emailValidationError = response.getBody().details().get(0);
        assertThat(emailValidationError.rejectedValue()).isNull();

        ValidationError nameValidationError = response.getBody().details().get(1);
        assertThat(nameValidationError.rejectedValue()).isEmpty();

        ValidationError ageValidationError = response.getBody().details().get(2);
        assertThat(ageValidationError.rejectedValue()).isEqualTo("-5");
    }

    @Test
    void handleValidationExceptions_WithComplexRejectedValue_ShouldConvertToString() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError complexError = new FieldError("user", "settings",
                new Object(), false, null, null, "Invalid settings");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(complexError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().details()).hasSize(1);

        ValidationError validationError = response.getBody().details().get(0);
        assertThat(validationError.rejectedValue()).isNotNull();
        assertThat(validationError.rejectedValue()).startsWith("java.lang.Object@");
    }

    @Test
    void convertRejectedValueToString_WithNonNullValue_ShouldReturnString() {
        String result = globalExceptionHandler.convertRejectedValueToString("testValue");

        assertThat(result).isEqualTo("testValue");
    }

    @Test
    void convertRejectedValueToString_WithNullValue_ShouldReturnNull() {
        String result = globalExceptionHandler.convertRejectedValueToString(null);

        assertThat(result).isNull();
    }

    @Test
    void convertRejectedValueToString_WithNumber_ShouldReturnStringRepresentation() {
        String result = globalExceptionHandler.convertRejectedValueToString(123);

        assertThat(result).isEqualTo("123");
    }

    @Test
    void convertRejectedValueToString_WithBoolean_ShouldReturnStringRepresentation() {
        String result = globalExceptionHandler.convertRejectedValueToString(true);

        assertThat(result).isEqualTo("true");
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users");

        var response = globalExceptionHandler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().path()).isEqualTo("/api/users");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void getPath_ShouldRemoveUriPrefix() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");

        UserNotFoundException ex = new UserNotFoundException(1L);
        var response = globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void getPath_WithComplexUri_ShouldReturnCleanPath() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/users/1?param=value");

        UserNotFoundException ex = new UserNotFoundException(1L);
        var response = globalExceptionHandler.handleUserNotFound(ex, request);

        assertThat(response.getBody().path()).isEqualTo("/api/users/1?param=value");
    }
}
