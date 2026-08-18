package com.pulsetrack.nutrition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code nutrition.*} block. The service's Basic Auth accounts live
 * here rather than in code, so each profile's YAML can supply its own
 * credentials through placeholders.
 */
@ConfigurationProperties(prefix = "nutrition")
public class NutritionProperties {

    private final Security security = new Security();

    public Security getSecurity() {
        return security;
    }

    public static class Security {
        private final Account serviceAccount = new Account();
        private final Account adminAccount = new Account();

        public Account getServiceAccount() {
            return serviceAccount;
        }

        public Account getAdminAccount() {
            return adminAccount;
        }
    }

    public static class Account {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
