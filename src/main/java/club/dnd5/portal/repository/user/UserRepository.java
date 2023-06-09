package club.dnd5.portal.repository.user;

import club.dnd5.portal.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByName(String name);
	Optional<User> findByEmail(String email);
	Optional<User> findByEmailOrUsername(String email, String username);

	@Query("SELECT count(u) FROM User u LEFT JOIN u.roles r WHERE r.name = :role")
	long countByRoles(@Param("role") String role);

	boolean existsByName(String name);
	boolean existsByUsername(String username);
	boolean existsByEmail(String email);
}
