package fr.piga.booknetwork.auth;

import fr.piga.booknetwork.email.EmailService;
import fr.piga.booknetwork.email.EmailTemplateName;
import fr.piga.booknetwork.role.RoleRepository;

import fr.piga.booknetwork.security.JwtService;
import fr.piga.booknetwork.user.Token;
import fr.piga.booknetwork.user.TokenRepository;
import fr.piga.booknetwork.user.User;
import fr.piga.booknetwork.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final TokenRepository tokenRepository;

    private final EmailService emailService;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    @Transactional(rollbackFor = MessagingException.class)
    public void register(RegistrationRequest request) throws MessagingException {
        var userRole = roleRepository.findByNom("USER") // On récupère le rôle "USER" dans la base de données
                .orElseThrow(() -> new IllegalStateException("Role not found")); // Si on ne le trouve pas, on lève une exception
        var user = User.builder() // On crée un nouvel utilisateur
                .prenom(request.getFirstName())
                .nom(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .compteBloque(false)
                .activer(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user); // On enregistre l'utilisateur dans la base de données
        sendValidationEmail(user); // On envoie un email de validation à l'utilisateur
    }

    @Transactional(rollbackFor = MessagingException.class, propagation = Propagation.REQUIRES_NEW)
    public void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        // send email
        emailService.sendEmail(
                user.getEmail(),
                user.nomComplet(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Activation de votre compte"
        );

    }

    private String generateAndSaveActivationToken(User user) {
        // genrate token
        String generatedToken = generateActivationCode(6); // On génère un code d'activation de 6 caractères aléatoires
        var token = Token.builder() // On crée un nouveau token
                .token(generatedToken) // On lui attribue le code d'activation généré
                .dateCreation(LocalDateTime.now()) // On lui attribue la date et l'heure actuelles
                .dateExpiration(LocalDateTime.now().plusMinutes(15)) // On lui attribue une date d'expiration de 15 minutes après la date et l'heure actuelles
                .user(user) // On lui attribue l'utilisateur concerné
                .build(); // On crée le token
        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateActivationCode(int length) { // Génère un code d'activation de 6 caractères aléatoires pour l'utilisateur lors de son inscription
        String characters = "0123456789"; // On définit les caractères possibles pour le code d'activation
        StringBuilder codeBuilder = new StringBuilder(); // On crée un StringBuilder pour stocker le code d'activation
        SecureRandom secureRandom = new SecureRandom(); // On crée un objet SecureRandom pour générer des nombres aléatoires
        for (int i = 0; i < length; i++) { // On boucle pour générer chaque caractère du code d'activation
            int randomIndex = secureRandom.nextInt(characters.length()); // On génère un nombre aléatoire entre 0 et la longueur de la chaîne de caractères
            codeBuilder.append(characters.charAt(randomIndex)); // On ajoute le caractère correspondant à l'index généré à la fin du code d'activation
        }

        return codeBuilder.toString(); // On retourne le code d'activation généré sous forme de chaîne de caractères
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var claims = new HashMap<String, Object>();
        var user = (User) auth.getPrincipal();
        claims.put("nomComplet", user.nomComplet());
        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    @Transactional(rollbackFor = MessagingException.class, propagation = Propagation.REQUIRED)
    public void activateAccount(String token) throws MessagingException {
        Token activationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if(LocalDateTime.now().isAfter(activationToken.getDateExpiration())) {
            sendValidationEmail(activationToken.getUser());
            throw new RuntimeException("Token d'activation expiré, un nouveau a été envoyé");
        }
        var user = userRepository.findById(activationToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setActiver(true);
        userRepository.save(user);
        activationToken.setDateValidation(LocalDateTime.now());
        tokenRepository.save(activationToken);
    }
}
