package com.example.authserver.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupProfileDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupProfileDiagnostics.class);

    private final Environment environment;

    @Value("${spring.datasource.url:undefined}")
    private String datasourceUrl;

    public StartupProfileDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();

        log.info("Active Spring profiles: {}",
                activeProfiles.length == 0 ? "[]" : Arrays.toString(activeProfiles));
        log.info("Default Spring profiles: {}", Arrays.toString(defaultProfiles));
        log.info("Datasource URL in use: {}", datasourceUrl);

        if (Arrays.asList(activeProfiles).contains("db2")) {
            log.warn("DB2 profile is active. If this is unexpected, unset SPRING_PROFILES_ACTIVE and SPRING_DATASOURCE_* environment variables.");
        }
    }
}
