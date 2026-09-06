package com.wilfredchau.synapsepkb.user.service;

import com.wilfredchau.synapsepkb.user.entity.PkbUser;
import java.util.Optional;

public interface PkbUserService {

    Optional<PkbUser> findByUsername(String username);

    void saveOrUpdate(PkbUser user);
}
