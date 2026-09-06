package com.wilfredchau.synapsepkb.user;

import com.wilfredchau.synapsepkb.config.SecurityProperties;
import com.wilfredchau.synapsepkb.user.entity.PkbUserEntity;
import com.wilfredchau.synapsepkb.user.service.PkbUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserBootstrapper implements ApplicationRunner {

    private final PkbUserService pkbUserService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public UserBootstrapper(
            PkbUserService pkbUserService,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties) {
        this.pkbUserService = pkbUserService;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        SecurityProperties.BootstrapUser bootstrapUser = securityProperties.getBootstrapUser();
        PkbUserEntity user = pkbUserService.findByUsername(bootstrapUser.getUsername())
                .orElseGet(PkbUserEntity::new);

        user.setUsername(bootstrapUser.getUsername());
        user.setPasswordHash(passwordEncoder.encode(bootstrapUser.getPassword()));
        user.setDisplayName(bootstrapUser.getDisplayName());
        user.setSpaceKey(bootstrapUser.getSpaceKey());
        user.setEnabled(true);

        pkbUserService.saveOrUpdate(user);
    }
}
