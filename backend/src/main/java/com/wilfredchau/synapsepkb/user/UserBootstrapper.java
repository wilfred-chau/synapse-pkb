package com.wilfredchau.synapsepkb.user;

import com.wilfredchau.synapsepkb.config.SecurityProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserBootstrapper implements ApplicationRunner {

    private final PkbUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public UserBootstrapper(
            PkbUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        SecurityProperties.BootstrapUser bootstrapUser = securityProperties.getBootstrapUser();
        PkbUser user = userRepository.findByUsername(bootstrapUser.getUsername())
                .orElseGet(PkbUser::new);

        user.setUsername(bootstrapUser.getUsername());
        user.setPasswordHash(passwordEncoder.encode(bootstrapUser.getPassword()));
        user.setDisplayName(bootstrapUser.getDisplayName());
        user.setSpaceKey(bootstrapUser.getSpaceKey());
        user.setEnabled(true);

        userRepository.save(user);
    }
}
