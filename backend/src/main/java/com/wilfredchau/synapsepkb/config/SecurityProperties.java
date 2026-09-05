package com.wilfredchau.synapsepkb.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @NotBlank
    private String jwtSecret;

    @Positive
    private long jwtExpirationMinutes = 720;

    @Valid
    private final BootstrapUser bootstrapUser = new BootstrapUser();

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getJwtExpirationMinutes() {
        return jwtExpirationMinutes;
    }

    public void setJwtExpirationMinutes(long jwtExpirationMinutes) {
        this.jwtExpirationMinutes = jwtExpirationMinutes;
    }

    public BootstrapUser getBootstrapUser() {
        return bootstrapUser;
    }

    public static class BootstrapUser {

        @NotBlank
        private String username;

        @NotBlank
        private String password;

        @NotBlank
        private String displayName;

        @NotBlank
        private String spaceKey;

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

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getSpaceKey() {
            return spaceKey;
        }

        public void setSpaceKey(String spaceKey) {
            this.spaceKey = spaceKey;
        }
    }
}
