package dev.dmv04.userservice.util;

import dev.dmv04.userservice.dto.UserEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KafkaTestConsumerUtil {

    private KafkaTestConsumerUtil() {
    }

    /**
     * Создаёт Kafka-потребитель для UserEvent с настройками, безопасными для тестов.
     *
     * @param kafka  инстанс KafkaContainer из Testcontainers
     * @param topic  топик для подписки
     * @return настроенный Consumer (должен быть закрыт после использования)
     */
    public static Consumer<String, UserEvent> createConsumerForUserEvent(KafkaContainer kafka, String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonSerializer.TYPE_MAPPINGS, "userEvent:dev.dmv04.userservice.dto.UserEvent");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "dev.dmv04.userservice.dto");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "dev.dmv04.userservice.dto.UserEvent");

        ConsumerFactory<String, UserEvent> factory = new DefaultKafkaConsumerFactory<>(props);
        Consumer<String, UserEvent> consumer = factory.createConsumer();
        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }
}
