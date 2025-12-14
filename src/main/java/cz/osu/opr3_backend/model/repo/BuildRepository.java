package cz.osu.opr3_backend.model.repo;

import cz.osu.opr3_backend.model.entity.Build;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BuildRepository extends JpaRepository<Build, Long> {
    List<Build> findAllByOwner_UsernameOrderByCreatedAtDesc(String username);
    Optional<Build> findByIdAndOwner_Username(Long id, String username);

}
