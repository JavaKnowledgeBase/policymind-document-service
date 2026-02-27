package com.policymind.document.security;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
    private JwtService jwtService;
    
    

    public AuthController(JwtService jwtService) {
		this.jwtService = jwtService;
	}



	@PostMapping("/login")
    public String login(@RequestParam String username) {

        // Temporary hardcoded user (replace with DB lookup later)
        String role = "USER";


        return jwtService.generateToken(username, role);
    }

}
