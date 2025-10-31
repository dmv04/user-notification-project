package dev.dmv04.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EntityScan(basePackages = "dev.dmv04.userservice.entity")
class CrudJpaJunitTestingApplicationTests {
    @Test
    void contextLoads() {
    }
}
