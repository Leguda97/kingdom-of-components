package cz.osu.opr3_backend.model.repo;

import cz.osu.opr3_backend.model.entity.BuildItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildItemRepository extends JpaRepository<BuildItem, Long> {
}
