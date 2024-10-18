package com.salescode.dataintegration.config;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import net.logstash.logback.encoder.LogstashEncoder;

@Configuration
public class LoggingConfig {

    @Bean
    public Logger logbackLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(
            Logger.ROOT_LOGGER_NAME
        );
        ConsoleAppender<ILoggingEvent> consoleAppender =
            new ConsoleAppender<>();
        consoleAppender.setContext(logger.getLoggerContext());

        // Use LogstashEncoder for JSON formatting
        LogstashEncoder encoder = new LogstashEncoder();
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // AsyncAppender for asynchronous logging
        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(logger.getLoggerContext());
        asyncAppender.addAppender(consoleAppender);
        asyncAppender.setQueueSize(512);
        asyncAppender.setDiscardingThreshold(0);
        asyncAppender.setIncludeCallerData(true);
        asyncAppender.start();

        logger.addAppender(asyncAppender);
        return logger;
    }
}
