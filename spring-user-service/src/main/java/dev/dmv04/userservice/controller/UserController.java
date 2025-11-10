package dev.dmv04.userservice.controller;

import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UpdateUserRequest;
import dev.dmv04.userservice.dto.UserDTO;
import dev.dmv04.userservice.service.UserService;
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

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public EntityModel<UserDTO> getUserById(@PathVariable Long id) {
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
    public ResponseEntity<EntityModel<UserDTO>> createUser(@Valid @RequestBody CreateUserRequest request) {
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
    public EntityModel<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
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
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
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
}
