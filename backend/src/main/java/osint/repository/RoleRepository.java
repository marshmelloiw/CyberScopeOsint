package osint.repository;

import osint.model.Role;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RoleRepository {
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, Role> idToRole = new ConcurrentHashMap<>();
    private final Map<String, Long> nameToId = new ConcurrentHashMap<>();

    public RoleRepository() {
        // seed default roles
        save(Role.builder().name("ROLE_USER").build());
        save(Role.builder().name("ROLE_ADMIN").build());
    }

    public Role save(Role role) {
        if (role.getId() == null) {
            role.setId(idGenerator.getAndIncrement());
        }
        idToRole.put(role.getId(), role);
        nameToId.put(role.getName(), role.getId());
        return role;
    }

    public Optional<Role> findByName(String name) {
        if (name == null)
            return Optional.empty();
        Long id = nameToId.get(name);
        return id == null ? Optional.empty() : Optional.ofNullable(idToRole.get(id));
    }
}



