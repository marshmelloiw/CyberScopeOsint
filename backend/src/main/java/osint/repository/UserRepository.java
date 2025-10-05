package osint.repository;

import osint.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserRepository {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, User> idToUser = new ConcurrentHashMap<>();
    private final Map<String, Long> emailToId = new ConcurrentHashMap<>();

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        idToUser.put(user.getId(), user);
        emailToId.put(user.getEmail().toLowerCase(), user.getId());
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(idToUser.get(id));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null)
            return Optional.empty();
        Long id = emailToId.get(email.toLowerCase());
        return id == null ? Optional.empty() : Optional.ofNullable(idToUser.get(id));
    }

    public boolean existsByEmail(String email) {
        if (email == null)
            return false;
        return emailToId.containsKey(email.toLowerCase());
    }

    public void deleteByEmail(String email) {
        if (email == null)
            return;
        Long id = emailToId.remove(email.toLowerCase());
        if (id != null) {
            idToUser.remove(id);
        }
    }
}
