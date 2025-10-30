package osint.repository;

import osint.model.PasswordResetToken;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordResetTokenRepository {
    private final Map<String, PasswordResetToken> tokenMap = new ConcurrentHashMap<>();

    public PasswordResetToken save(PasswordResetToken token) {
        tokenMap.put(token.getToken(), token);
        return token;
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return Optional.ofNullable(tokenMap.get(token));
    }

    public void deleteByToken(String token) {
        tokenMap.remove(token);
    }

    public void deleteExpiredTokens() {
        Instant now = Instant.now();
        tokenMap.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }
}


