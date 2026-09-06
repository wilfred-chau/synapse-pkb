package com.wilfredchau.synapsepkb.user.service;

import com.wilfredchau.synapsepkb.user.entity.PkbUserEntity;
import java.util.Optional;

public interface PkbUserService {

    Optional<PkbUserEntity> findByUsername(String username);

    void saveOrUpdate(PkbUserEntity user);
}
