package cz.osu.opr3_backend.service;

import cz.osu.opr3_backend.model.entity.Order;
import cz.osu.opr3_backend.model.entity.OrderItem;
import cz.osu.opr3_backend.model.entity.Product;
import cz.osu.opr3_backend.model.entity.User;
import cz.osu.opr3_backend.model.repo.OrderRepository;
import cz.osu.opr3_backend.model.repo.ProductRepository;
import cz.osu.opr3_backend.model.repo.UserRepository;
import cz.osu.opr3_backend.security.CurrentUser;
import cz.osu.opr3_backend.security.SecurityUtils;
import cz.osu.opr3_backend.web.dto.order.OrderItemAddRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ----------------------------
    // Helpers
    // ----------------------------

    private User currentUserEntity() {
        String username = CurrentUser.username();
        if (username == null) throw new UnauthorizedException("Not authenticated");

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public Order getMy(Long id) {
        String username = CurrentUser.username();
        if (username == null) throw new UnauthorizedException("Not authenticated");

        return orderRepository.findByIdAndOwner_Username(id, username)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }

    public Order getAnyAdmin(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }

    // ----------------------------
    // Create / Read
    // ----------------------------

    public Order create() {
        String actor = SecurityUtils.usernameOrAnonymous();
        User owner = currentUserEntity();

        Order o = Order.builder()
                .owner(owner)
                .build();

        Order saved = orderRepository.save(o);

        log.info("AUDIT ORDER_CREATE actor={} orderId={} owner={} status={}",
                actor, saved.getId(), owner.getUsername(), saved.getStatus());

        return saved;
    }

    public List<Order> listMy() {
        String actor = SecurityUtils.usernameOrAnonymous();
        String username = CurrentUser.username();
        if (username == null) throw new UnauthorizedException("Not authenticated");

        List<Order> res = orderRepository.findAllByOwner_UsernameOrderByCreatedAtDesc(username);
        log.info("AUDIT ORDER_LIST_MY actor={} owner={} count={}", actor, username, res.size());
        return res;
    }

    public List<Order> listAllAdmin() {
        String actor = SecurityUtils.usernameOrAnonymous();
        List<Order> res = orderRepository.findAllByOrderByCreatedAtDesc();
        log.info("AUDIT ORDER_LIST_ALL_ADMIN actor={} count={}", actor, res.size());
        return res;
    }

    public Order get(Long id) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Order o = getMy(id);
        log.info("AUDIT ORDER_GET_MY actor={} orderId={} owner={}", actor, o.getId(), o.getOwner().getUsername());
        return o;
    }

    // ----------------------------
    // Items
    // ----------------------------

    @Transactional
    public Order addItem(Long orderId, OrderItemAddRequest req) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Order order = getMy(orderId);

        if (order.getStatus() != Order.Status.NEW) {
            log.warn("AUDIT ORDER_ADD_ITEM_DENIED actor={} orderId={} owner={} reason=status_not_new status={}",
                    actor, orderId, order.getOwner().getUsername(), order.getStatus());
            throw new OrderStateException("Order " + orderId + " cannot be modified when status is " + order.getStatus());
        }

        if (req.quantity() <= 0) {
            log.warn("AUDIT ORDER_ADD_ITEM_DENIED actor={} orderId={} reason=qty<=0 qty={}",
                    actor, orderId, req.quantity());
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new NotFoundException("Product " + req.productId() + " not found"));

        for (OrderItem oi : order.getItems()) {
            if (oi.getProduct().getId().equals(product.getId())) {
                int oldQty = oi.getQuantity();
                int newQty = oldQty + req.quantity();
                oi.setQuantity(newQty);

                recalcTotal(order);
                Order saved = orderRepository.save(order);

                log.info("AUDIT ORDER_ADD_ITEM_MERGE actor={} orderId={} owner={} productId={} sku={} oldQty={} addQty={} newQty={} totalPrice={}",
                        actor, saved.getId(), saved.getOwner().getUsername(), product.getId(), product.getSku(),
                        oldQty, req.quantity(), newQty, saved.getTotalPrice());

                return saved;
            }
        }

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(req.quantity())
                .unitPrice(product.getPrice())
                .build();

        order.getItems().add(item);
        recalcTotal(order);

        Order saved = orderRepository.save(order);

        log.info("AUDIT ORDER_ADD_ITEM_NEW actor={} orderId={} owner={} productId={} sku={} qty={} unitPrice={} totalPrice={}",
                actor, saved.getId(), saved.getOwner().getUsername(), product.getId(), product.getSku(),
                req.quantity(), product.getPrice(), saved.getTotalPrice());

        return saved;
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Order order = getMy(orderId);

        if (order.getStatus() != Order.Status.NEW) {
            log.warn("AUDIT ORDER_REMOVE_ITEM_DENIED actor={} orderId={} owner={} reason=status_not_new status={}",
                    actor, orderId, order.getOwner().getUsername(), order.getStatus());
            throw new OrderStateException("Order " + orderId + " cannot be modified when status is " + order.getStatus());
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("OrderItem " + itemId + " not found in order " + orderId));

        order.getItems().remove(item);
        recalcTotal(order);
        orderRepository.save(order);

        log.info("AUDIT ORDER_REMOVE_ITEM actor={} orderId={} owner={} itemId={} productId={} sku={} newTotalPrice={}",
                actor, orderId, order.getOwner().getUsername(), itemId,
                item.getProduct().getId(), item.getProduct().getSku(), order.getTotalPrice());
    }

    // ----------------------------
    // Status
    // ----------------------------

    @Transactional
    public Order updateStatus(Long orderId, Order.Status newStatus) {
        String actor = SecurityUtils.usernameOrAnonymous();
        Order order = getMy(orderId);

        Order.Status oldStatus = order.getStatus();

        if (!isAllowedTransition(oldStatus, newStatus)) {
            log.warn("AUDIT ORDER_STATUS_DENIED actor={} orderId={} owner={} from={} to={}",
                    actor, orderId, order.getOwner().getUsername(), oldStatus, newStatus);
            throw new OrderStateException("Invalid status transition: " + oldStatus + " -> " + newStatus);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("AUDIT ORDER_STATUS_CHANGE_MY actor={} orderId={} owner={} from={} to={}",
                actor, saved.getId(), saved.getOwner().getUsername(), oldStatus, newStatus);

        return saved;
    }

    @Transactional
    public Order updateStatusAdmin(Long orderId, Order.Status newStatus) {
        String actor = SecurityUtils.usernameOrAnonymous();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));

        Order.Status oldStatus = order.getStatus();

        if (!isAllowedTransition(oldStatus, newStatus)) {
            log.warn("AUDIT ORDER_STATUS_DENIED_ADMIN actor={} orderId={} from={} to={}",
                    actor, orderId, oldStatus, newStatus);
            throw new OrderStateException("Invalid status transition: " + oldStatus + " -> " + newStatus);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("AUDIT ORDER_STATUS_CHANGE_ADMIN actor={} orderId={} owner={} from={} to={}",
                actor, saved.getId(), saved.getOwner().getUsername(), oldStatus, newStatus);

        return saved;
    }

    private boolean isAllowedTransition(Order.Status from, Order.Status to) {
        return switch (from) {
            case NEW -> (to == Order.Status.PAID || to == Order.Status.CANCELLED);
            case PAID -> (to == Order.Status.SHIPPED || to == Order.Status.CANCELLED);
            case SHIPPED -> false;
            case CANCELLED -> false;
        };
    }

    private void recalcTotal(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem oi : order.getItems()) {
            total = total.add(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
        }
        order.setTotalPrice(total);
    }
}
