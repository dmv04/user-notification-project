package dev.dmv04.userservice.controller;

import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UpdateUserRequest;
import dev.dmv04.userservice.dto.UserDTO;
import dev.dmv04.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Операции с пользователями")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить пользователя по ID",
            description = "Возвращает данные пользователя по его уникальному идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь найден",
                    content = @Content(
                            mediaType = "application/hal+json",
                            schema = @Schema(implementation = UserResource.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public EntityModel<UserDTO> getUserById(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id) {

        UserDTO dto = userService.getUserById(id);

        EntityModel<UserDTO> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel());
        resource.add(linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"));
        resource.add(linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"));
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
        resource.add(linkTo(methodOn(UserController.class).createUser(null)).withRel("create-user"));

        return resource;
    }

    @GetMapping
    @Operation(
            summary = "Получить всех пользователей",
            description = "Возвращает список всех зарегистрированных пользователей с HATEOAS ссылками"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список пользователей с HATEOAS",
            content = @Content(
                    mediaType = "application/hal+json",
                    schema = @Schema(implementation = UserCollection.class)
            )
    )
    public CollectionModel<EntityModel<UserDTO>> getAllUsers() {
        List<UserDTO> dtos = userService.getAllUsers();

        List<EntityModel<UserDTO>> userResources = dtos.stream()
                .map(dto -> {
                    EntityModel<UserDTO> resource = EntityModel.of(dto);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(dto.id())).withSelfRel());
                    resource.add(linkTo(methodOn(UserController.class).updateUser(dto.id(), null)).withRel("update"));
                    resource.add(linkTo(methodOn(UserController.class).deleteUser(dto.id())).withRel("delete"));
                    return resource;
                })
                .collect(Collectors.toList());

        CollectionModel<EntityModel<UserDTO>> collectionModel = CollectionModel.of(userResources);
        collectionModel.add(linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel());
        collectionModel.add(linkTo(methodOn(UserController.class).createUser(null)).withRel("create-user"));

        return collectionModel;
    }

    @PostMapping
    @Operation(
            summary = "Создать нового пользователя",
            description = "Создает нового пользователя с указанными данными"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно создан",
                    content = @Content(
                            mediaType = "application/hal+json",
                            schema = @Schema(implementation = UserResource.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email уже существует",
                    content = @Content(
                            mediaType = "application/hal+json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<EntityModel<UserDTO>> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true)
            @Valid @RequestBody CreateUserRequest request) {

        UserDTO dto = userService.createUser(request);

        EntityModel<UserDTO> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(UserController.class).getUserById(dto.id())).withSelfRel());
        resource.add(linkTo(methodOn(UserController.class).updateUser(dto.id(), null)).withRel("update"));
        resource.add(linkTo(methodOn(UserController.class).deleteUser(dto.id())).withRel("delete"));
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));

        return ResponseEntity
                .created(linkTo(methodOn(UserController.class).getUserById(dto.id())).toUri())
                .body(resource);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Обновить пользователя по ID",
            description = "Обновляет данные существующего пользователя"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно обновлён",
                    content = @Content(
                            mediaType = "application/hal+json",
                            schema = @Schema(implementation = UserResource.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public EntityModel<UserDTO> updateUser(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Данные для обновления пользователя", required = true)
            @Valid @RequestBody UpdateUserRequest request) {

        UserDTO dto = userService.updateUser(id, request);

        EntityModel<UserDTO> resource = EntityModel.of(dto);
        resource.add(linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel());
        resource.add(linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"));
        resource.add(linkTo(methodOn(UserController.class).deleteUser(id)).withRel("delete"));
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
        resource.add(linkTo(methodOn(UserController.class).createUser(null)).withRel("create-user"));

        return resource;
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить пользователя по ID",
            description = "Удаляет пользователя по его уникальному идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Пользователь успешно удалён"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id) {

        userService.deleteUser(id);

        String allUsersLink = linkTo(methodOn(UserController.class).getAllUsers()).toString();
        String createUserLink = linkTo(methodOn(UserController.class).createUser(null)).toString();

        String linkHeader = String.format("<%s>; rel=\"all-users\", <%s>; rel=\"create-user\"",
                allUsersLink, createUserLink);

        return ResponseEntity
                .noContent()
                .location(linkTo(methodOn(UserController.class).getAllUsers()).toUri())
                .header("Link", linkHeader)
                .build();
    }

    @GetMapping("/error-test/500")
    @Operation(summary = "Тест 500 ошибки", description = "Всегда возвращает 500 ошибку для тестирования Circuit Breaker")
    public ResponseEntity<Map<String, String>> test500Error() {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "Simulated server error for Circuit Breaker testing");
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(500).body(errorResponse);
    }

    @Schema(name = "UserResource")
    public static class UserResource {
        @Schema(example = "1")
        public Long id;

        @Schema(example = "Иван Иванов")
        public String name;

        @Schema(example = "ivan@example.com", format = "email")
        public String email;

        @Schema(example = "30")
        public Integer age;

        @Schema(example = "2024-01-15T10:30:00", format = "date-time")
        public String createdAt;

        public Object _links;
    }

    @Schema(name = "UserCollection")
    public static class UserCollection {
        public Object _embedded;
        public Object _links;
    }

    @Schema(name = "ErrorResponse")
    public static class ErrorResponse {
        @Schema(example = "2024-01-15T10:30:00", format = "date-time")
        public String timestamp;

        @Schema(example = "400")
        public Integer status;

        @Schema(example = "Bad Request")
        public String error;

        @Schema(example = "Validation failed")
        public String message;

        @Schema(example = "/api/users")
        public String path;

        public Object details;
    }
}
