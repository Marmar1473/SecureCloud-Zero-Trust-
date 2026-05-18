package kz.university.securechat.controller;

import kz.university.securechat.config.JwtUtils;
import kz.university.securechat.entity.User;
import kz.university.securechat.repository.GroupMemberRepository;
import kz.university.securechat.repository.MessageRepository;
import kz.university.securechat.repository.UserRepository;
import kz.university.securechat.service.MessageProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageProducer messageProducer;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
                          UserRepository userRepository, PasswordEncoder passwordEncoder,
                          GroupMemberRepository groupMemberRepository, MessageRepository messageRepository,
                          MessageProducer messageProducer) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.groupMemberRepository = groupMemberRepository;
        this.messageRepository = messageRepository;
        this.messageProducer = messageProducer;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.get("username"), request.get("password")));
            String jwt = jwtUtils.generateToken(authentication.getName());
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            return ResponseEntity.ok(Map.of(
                    "token", jwt,
                    "encryptedPrivateKey", user.getEncryptedPrivateKey()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String publicKey = request.get("publicKey");
        String encryptedPrivateKey = request.get("encryptedPrivateKey");

        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,20}$") || password == null || password.length() < 6 || publicKey == null || encryptedPrivateKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "User already exists"));
        }

        userRepository.save(new User(username, passwordEncoder.encode(password), publicKey, encryptedPrivateKey));
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    @DeleteMapping("/delete-account")
    @Transactional
    public ResponseEntity<?> deleteAccount(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user != null) {
            List<String> contacts = messageRepository.findActiveContacts(user.getUuid());
            for (String contactUuid : contacts) {
                userRepository.findByUuid(contactUuid).ifPresent(contact -> {
                    messageProducer.sendMessage("system-notifications",
                            contact.getUsername() + "|user-deleted|" + user.getUuid());
                });
            }
            groupMemberRepository.deleteAll(groupMemberRepository.findByUser(user));
            messageRepository.deleteByRecipientUuidOrSenderUuid(user.getUuid(), user.getUuid());
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of("message", "Account deleted"));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        if (q == null || q.isBlank()) return ResponseEntity.badRequest().build();
        List<Map<String, String>> users = userRepository.findByUsernameContainingIgnoreCase(q)
                .stream()
                .map(user -> Map.of("username", user.getUsername(), "uuid", user.getUuid(), "publicKey", user.getPublicKey()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}