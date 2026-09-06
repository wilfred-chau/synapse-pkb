package com.wilfredchau.synapsepkb.user.service.impl;

import com.wilfredchau.synapsepkb.user.entity.PkbUserEntity;
import com.wilfredchau.synapsepkb.user.mapper.PkbUserMapper;
import com.wilfredchau.synapsepkb.user.service.PkbUserService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PkbUserServiceImpl implements PkbUserService {

    private final PkbUserMapper pkbUserMapper;

    public PkbUserServiceImpl(PkbUserMapper pkbUserMapper) {
        this.pkbUserMapper = pkbUserMapper;
    }

    @Override
    public Optional<PkbUserEntity> findByUsername(String username) {
        return Optional.ofNullable(pkbUserMapper.selectByUsername(username));
    }

    @Override
    public void saveOrUpdate(PkbUserEntity user) {
        if (user.getId() == null) {
            pkbUserMapper.insert(user);
            return;
        }
        pkbUserMapper.updateById(user);
    }
}
