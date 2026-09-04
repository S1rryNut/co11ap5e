package com.personal.site;

import com.personal.site.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
public class PersonalSiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(PersonalSiteApplication.class, args);
    }
}
