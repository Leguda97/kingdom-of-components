package cz.osu.opr3_backend.model.repo;

import cz.osu.opr3_backend.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // objednávky aktivního uživatele
    List<Order> findAllByOwner_UsernameOrderByCreatedAtDesc(String username);
    Optional<Order> findByIdAndOwner_Username(Long id, String username);

    // admin – všechny
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusOrderByCreatedAtDesc(Order.Status status);

    List<Order> findByOwner_UsernameAndStatusOrderByCreatedAtDesc(String ownerUsername, Order.Status status);
}
