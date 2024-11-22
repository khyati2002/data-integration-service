package com.salescode.dis;

import com.salescode.channelkart.services.SpringContext;
import com.salescode.dis.config.DatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Optional;

@SpringBootApplication
@Import({DatabaseConfig.class})
@ComponentScan(basePackages = {"com.salescode"})
public class FlinkApplication implements CommandLineRunner {

    @Autowired
    private com.salescode.dis.flink.jobs.fromS3ToKafka.Job fromS3ToKafka;

    @Autowired
    private com.salescode.dis.flink.jobs.fromKafkaToDB.KafkaConsumerJob fromKafkaToDB;

    public static void main(String[] args) {
        // if (args.length == 0) {
        //     throw new IllegalArgumentException("No job specified. Please provide a job name to execute.");
        // }
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        SpringApplication.run(FlinkApplication.class, args);
    }

    public static String getEnv() {
        return SpringContext.getBeanSafely(Environment.class)
                .map(environment -> environment.getProperty("channelkart.environment", "dev"))
                .orElseGet(() -> Optional.ofNullable(System.getenv("channelkart.environment")).orElse("dev"));
    }

    public static String getLob() {
        return SpringContext.getBeanSafely(Environment.class)
                .map(environment -> environment.getProperty("channelkart.lobs", "none"))
                .orElseGet(() -> Optional.ofNullable(System.getenv("channelkart.lobs")).orElse("none"));
    }

    @Override
    public void run(String... args) throws Exception {

        // Execute the job based on an argument or configuration
        // String jobName = args[0];
        String jobName = "FromKafkaToDB";
        switch (jobName) {
            case "FromS3ToKafka":
                fromS3ToKafka.executeJob();
                break;
            case "FromKafkaToDB":
                fromKafkaToDB.executeJob();
                break;
                // Add cases for other jobs in the future
            default:
                throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
}