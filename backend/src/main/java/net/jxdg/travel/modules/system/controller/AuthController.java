package net.jxdg.travel.modules.system.controller;

import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.Audit;
import net.jxdg.travel.common.Result;
import net.jxdg.travel.modules.system.dto.LoginRequest;
import net.jxdg.travel.modules.system.dto.LoginResult;
import net.jxdg.travel.modules.system.dto.SocialLoginRequest;
import net.jxdg.travel.modules.system.service.AuthService;
import net.jxdg.travel.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/member-login")
    public Result<LoginResult> memberLogin(@RequestBody SocialLoginRequest req) {
        return Result.ok(authService.memberLogin(req));
    }

    @PostMapping("/guide-login")
    public Result<LoginResult> guideLogin(@RequestBody SocialLoginRequest req) {
        return Result.ok(authService.guideLogin(req));
    }

    @PostMapping("/scenic-login")
    public Result<LoginResult> scenicLogin(@RequestBody SocialLoginRequest req) {
        return Result.ok(authService.scenicLogin(req));
    }

    @PostMapping("/distributor-login")
    public Result<LoginResult> distributorLogin(@RequestBody SocialLoginRequest req) {
        return Result.ok(authService.distributorLogin(req));
    }

    @PostMapping("/supplier-login")
    public Result<LoginResult> supplierLogin(@RequestBody SocialLoginRequest req) {
        return Result.ok(authService.supplierLogin(req));
    }

    @GetMapping("/me")
    public Result<LoginResult> me() {
        return Result.ok(authService.current());
    }

    @PostMapping("/logout")
    @Audit(module = "认证", action = "退出登录")
    public Result<Void> logout() {
        return Result.ok();
    }
}
