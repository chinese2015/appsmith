package com.appsmith.server.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "appsmith.featureflags.overrides")
public class FeatureFlagOverrideConfig {

    @Getter
    @Setter
    private Map<String, Boolean> overrides = new HashMap<>();



    @PostConstruct
    public void init() {
        System.out.println("FeatureFlag Overrides: " + overrides);
    }

}
