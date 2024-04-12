package fr.piga.booknetwork.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;

import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public JwtService() {
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject); // Récupération du nom d'utilisateur du token JWT
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token); // Récupération de toutes les informations du token JWT
        return claimResolver.apply(claims); // Récupération d'une information spécifique du token JWT
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder() // Création d'un parseur pour extraire les informations du token JWT
                .setSigningKey(getSignInKey()) // Clé secrète pour vérifier la signature du token JWT
                .build() // Construction du parseur
                .parseClaimsJws(token) // Extraction des informations du token JWT
                .getBody(); // Récupération de toutes les informations du token JWT
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails); // Générer un token JWT sans informations supplémentaires (claims)
    }

    public String generateToken(HashMap<String, Object> claims, UserDetails userDetails) {
        return buildToken(claims, userDetails, jwtExpiration); // Générer un token JWT avec des informations supplémentaires (claims)
    }

    private String buildToken(HashMap<String, Object> extraClaims, UserDetails userDetails, long jwtExpiration) {
        var authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .setClaims(extraClaims) // Informations supplémentaires
                .setSubject(userDetails.getUsername()) // Nom d'utilisateur
                .setIssuedAt(new Date(System.currentTimeMillis())) // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Date d'expiration
                .claim("authorities", authorities) // Rôles de l'utilisateur (admin, user, etc.)
                .signWith(getSignInKey()) // Signature de la clé secrète pour sécuriser le token JWT
                .compact(); // Construction du token JWT sous forme de chaîne de caractères compacte (String) pour l'envoyer au client (navigateur, application mobile, etc.)
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token); // Récupération du nom d'utilisateur du token JWT
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token)); // Vérification de la validité du token JWT

    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date()); // Vérification de la date d'expiration du token JWT
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration); // Récupération de la date d'expiration du token JWT
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey); // Décodage de la clé secrète en base64 pour obtenir un tableau de bytes
        return Keys.hmacShaKeyFor(keyBytes); // Création d'une clé secrète pour signer le token JWT
    }
}
