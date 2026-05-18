package kz.university.securechat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping({"/login", "/register"})
    public String authPage() {
        return "forward:/login.html";
    }
}