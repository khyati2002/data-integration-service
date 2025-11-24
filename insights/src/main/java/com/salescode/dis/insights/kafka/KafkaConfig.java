package com.salescode.dis.insights.kafka;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("kafka")
public class KafkaConfig {

    private final Environment environment;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.security.protocol:#{null}}")
    private String securityProtocol;

    @Value("${spring.kafka.sasl.mechanism:#{null}}")
    private String saslMechanism;

    @Value("${spring.kafka.sasl.login.callback.handler.class:#{null}}")
    private String saslLoginCallbackHandlerClass;

    @Value("${spring.kafka.sasl.jaas.config:#{null}}")
    private String saslJaasConfig;

    public KafkaConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Checks if the "gcp" profile is active.
     */
    private boolean isGcpProfileActive() {
        return environment.acceptsProfiles(org.springframework.core.env.Profiles.of("gcp"));
    }

    /**
     * Adds SSL/SASL security properties to the given properties map if configured
     * and the "gcp" profile is active.
     * This method is used for both consumers and producers.
     */
    private void addSecurityProperties(Map<String, Object> props) {
        // Only apply SSL/SASL properties when "gcp" profile is active
        if (!isGcpProfileActive()) {
            return;
        }

        if (securityProtocol != null && !securityProtocol.isEmpty()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        }
        if (saslMechanism != null && !saslMechanism.isEmpty()) {
            props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        }
        if (saslLoginCallbackHandlerClass != null && !saslLoginCallbackHandlerClass.isEmpty()) {
            props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, saslLoginCallbackHandlerClass);
        }
        if (saslJaasConfig != null && !saslJaasConfig.isEmpty()) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }
    }

    private Map<String, Object> baseProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Add SSL/SASL security properties if configured
        addSecurityProperties(props);
        // tune other consumer settings as needed (max.poll.interval, max.poll.records, etc.)
        return props;
    }


    @Bean
    public NewTopic fileUpdatesTopic(
            @Value("${file.progress.update.topic:file-progress-updates}") String fileUpdatesTopicName,
            @Value("${kafka.topic.retention.ms:86400000}") String retentionMs
    ) {
        return TopicBuilder.name(fileUpdatesTopicName).partitions(5).replicas(1).config("retention.ms", retentionMs).build();
    }

    @Bean
    public NewTopic orderUpdatesTopic(
            @Value("${insights.orders.topic:order-progress-updates}") String orderUpdatesTopicName,
            @Value("${kafka.topic.retention.ms:86400000}") String retentionMs
    ) {
        return TopicBuilder.name(orderUpdatesTopicName).partitions(5).replicas(1).config("retention.ms", retentionMs).build();
    }

    /**
     * Creates a custom KafkaAdmin with SSL/SASL security properties if configured.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Add SSL/SASL security properties if configured
        addSecurityProperties(configs);
        return new KafkaAdmin(configs);
    }

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    /**
     * Creates a ProducerFactory with SSL/SASL security properties if configured.
     * This ensures that all KafkaTemplate instances use the security configuration.
     */
    @Bean
    public ProducerFactory<String, FileProgressEvent> fileProgressProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        // Add SSL/SASL security properties if configured
        addSecurityProperties(configProps);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, FileProgressEvent> kafkaTemplate(ProducerFactory<String, FileProgressEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, FileProgressEvent> fileProgressConsumerFactory(
            @Value("${file.progress.group-id:file-progress-processor}") String groupId) {
         Map<String, Object> props = baseProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class); // Use ErrorHandlingDeserializer
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class); // Specify the delegate deserializer
        props.put("spring.json.ignore.unknown", true);
        JsonDeserializer<FileProgressEvent> deserializer = new JsonDeserializer<>(FileProgressEvent.class, new ObjectMapper(), false);
        deserializer.addTrustedPackages("com.salescode.dis.insights", "java.util");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(deserializer));
    }

    @Bean(name = "fileProgressContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, FileProgressEvent> fileProgressContainerFactory(
            ConsumerFactory<String, FileProgressEvent> fileProgressConsumerFactory,
            KafkaTemplate<String, FileProgressEvent> template) {
        ConcurrentKafkaListenerContainerFactory<String, FileProgressEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fileProgressConsumerFactory);
        factory.setBatchListener(true);
        var recoverer = new DeadLetterPublishingRecoverer(template, (r, e) -> new TopicPartition("file-progress-updates-failed", r.partition()));
        var eh = new DefaultErrorHandler(recoverer);
        eh.addNotRetryableExceptions(UnrecognizedPropertyException.class);
        factory.setCommonErrorHandler(eh);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> orderProgressConsumerFactory(
            @Value("${insights.order.group-id:order-progress-processor}") String groupId) {
        Map<String, Object> props = baseProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean(name = "orderProgressContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> orderProgressContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> orderDetailsConsumerFactory(
            @Value("${order.details.group-id:order-details-processor}") String groupId) {
        Map<String, Object> props = baseProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean(name = "orderDetailsContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> orderDetailsContainerFactory(
            ConsumerFactory<String, byte[]> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }


}