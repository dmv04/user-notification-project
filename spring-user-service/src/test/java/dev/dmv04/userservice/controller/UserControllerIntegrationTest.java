package dev.dmv04.userservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dmv04.userservice.config.TestConfig;
import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UpdateUserRequest;
import dev.dmv04.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_shouldReturn201AndPersistInDb() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Alice", "alice@test.com", 30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.age").value(30));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userDTOList").isArray())
                .andExpect(jsonPath("$._embedded.userDTOList.length()").value(1))
                .andExpect(jsonPath("$._embedded.userDTOList[0].name").value("Alice"))
                .andExpect(jsonPath("$._embedded.userDTOList[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$._embedded.userDTOList[0].age").value(30));
    }

    @Test
    void createUser_shouldReturn400WhenEmailExists() throws Exception {
        CreateUserRequest request1 = new CreateUserRequest("Alice", "alice@test.com", 30);
        CreateUserRequest request2 = new CreateUserRequest("Bob", "alice@test.com", 25);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUser_shouldModifyUser() throws Exception {
        CreateUserRequest createRequest = new CreateUserRequest("Old", "old@test.com", 40);
        var createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(response).get("id").asLong();

        UpdateUserRequest updateRequest = new UpdateUserRequest("New", "new@test.com", 50);
        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.age").value(50))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists())
                .andExpect(jsonPath("$._links.all-users.href").exists())
                .andExpect(jsonPath("$._links.create-user.href").exists());

    }

    @Test
    void deleteUser_shouldRemoveUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("ToDelete", "del@test.com", 33);

        var createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long userId = jsonNode.get("id").asLong();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userDTOList").exists())
                .andExpect(jsonPath("$._embedded.userDTOList").isArray())
                .andExpect(jsonPath("$._embedded.userDTOList.length()").value(1))
                .andExpect(jsonPath("$._embedded.userDTOList[0].id").value(userId.intValue()));

        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.create-user.href").exists())
                .andExpect(jsonPath("$._embedded.userDTOList").doesNotExist());
    }

    @Test
    void getAllUsers_shouldReturnCorrectHateoasLinks() throws Exception {
        CreateUserRequest user1 = new CreateUserRequest("User1", "user1@test.com", 25);
        CreateUserRequest user2 = new CreateUserRequest("User2", "user2@test.com", 30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.create-user.href").exists())

                .andExpect(jsonPath("$._embedded.userDTOList").isArray())
                .andExpect(jsonPath("$._embedded.userDTOList.length()").value(2))

                .andExpect(jsonPath("$._embedded.userDTOList[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.userDTOList[0]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.userDTOList[0]._links.delete.href").exists())

                .andExpect(jsonPath("$._embedded.userDTOList[1]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.userDTOList[1]._links.update.href").exists())
                .andExpect(jsonPath("$._embedded.userDTOList[1]._links.delete.href").exists());
    }

    @Test
    void getUserById_shouldReturnCorrectHateoasLinks() throws Exception {
        CreateUserRequest request = new CreateUserRequest("TestUser", "test@test.com", 28);

        var createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long userId = jsonNode.get("id").asLong();

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.intValue()))
                .andExpect(jsonPath("$.name").value("TestUser"))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.age").value(28))

                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.update.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists())
                .andExpect(jsonPath("$._links.all-users.href").exists());
    }
}
