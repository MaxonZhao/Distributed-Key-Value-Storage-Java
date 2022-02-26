package com.g10.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.net.URI;

@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class LoggerConfiguration extends ConfigurationFactory {
    private static Configuration configuration;

    public static void initialize(String logFileName) {
        final String pattern = "%d [%pid] [%t] %-5level %logger{36} - %msg%n";

        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        builder
                .setStatusLevel(Level.INFO)
                .add(builder
                        .newAppender("Console", "Console")
                        .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                        .add(builder
                                .newLayout("PatternLayout")
                                .addAttribute("pattern", pattern)
                        )
                )
                .add(builder
                        .newAppender("File", "File")
                        .addAttribute("fileName", logFileName)
                        .add(builder
                                .newLayout("PatternLayout")
                                .addAttribute("pattern", pattern)
                        )
                )
                .add(builder
                        .newRootLogger(Level.INFO)
                        .add(builder.newAppenderRef("Console"))
                        .add(builder.newAppenderRef("File"))
                );

        configuration = builder.build();
        System.setProperty("log4j.configurationFactory", LoggerConfiguration.class.getName());
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        return configuration;
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[]{"*"};
    }
}
