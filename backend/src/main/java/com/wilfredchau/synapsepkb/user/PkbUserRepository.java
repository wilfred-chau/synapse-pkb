package com.wilfredchau.synapsepkb.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PkbUserRepository extends JpaRepository<PkbUser, Long> {

    Optional<PkbUser> findByUsername(String username);
}
