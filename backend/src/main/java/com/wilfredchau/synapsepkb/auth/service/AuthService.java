package com.wilfredchau.synapsepkb.auth.service;

import com.wilfredchau.synapsepkb.auth.model.dto.LoginRequest;
import com.wilfredchau.synapsepkb.auth.model.vo.AuthResponse;
import com.wilfredchau.synapsepkb.auth.model.vo.CurrentUserResponse;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    CurrentUserResponse me(AuthenticatedUser currentUser);
}
