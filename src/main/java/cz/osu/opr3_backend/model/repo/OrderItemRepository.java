package cz.osu.opr3_backend.model.repo;

import cz.osu.opr3_backend.model.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
