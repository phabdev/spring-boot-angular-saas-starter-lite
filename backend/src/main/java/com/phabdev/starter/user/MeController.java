package com.phabdev.starter.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
    @GetMapping("/api/me")
    public UserView me(@AuthenticationPrincipal UserAccount user) {
        return user.view();
    }
}
