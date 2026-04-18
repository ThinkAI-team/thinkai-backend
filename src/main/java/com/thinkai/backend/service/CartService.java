package com.thinkai.backend.service;

import com.thinkai.backend.dto.CartResponse;
import com.thinkai.backend.entity.Cart;
import com.thinkai.backend.entity.CartItem;
import com.thinkai.backend.entity.Course;
import com.thinkai.backend.entity.User;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.CartItemRepository;
import com.thinkai.backend.repository.CartRepository;
import com.thinkai.backend.repository.CourseRepository;
import com.thinkai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toCartResponse(userId, cart.getId());
    }

    @Transactional
    public CartResponse addToCart(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Không tìm thấy khóa học", HttpStatus.NOT_FOUND));

        if (!course.getIsPublished()) {
            throw new ApiException("Khóa học chưa được xuất bản", HttpStatus.BAD_REQUEST);
        }
        if (course.getStatus() == Course.Status.BLOCKED) {
            throw new ApiException("Khóa học đã bị khóa bởi quản trị viên", HttpStatus.BAD_REQUEST);
        }
        if (course.getStatus() != Course.Status.APPROVED) {
            throw new ApiException("Khóa học chưa sẵn sàng để thêm vào giỏ", HttpStatus.BAD_REQUEST);
        }

        Cart cart = getOrCreateCart(userId);

        if (cartItemRepository.existsByCartUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("Khóa học đã có trong giỏ hàng", HttpStatus.BAD_REQUEST);
        }

        String instructorName = "ThinkAI";
        if (course.getInstructorId() != null) {
            User instructor = userRepository.findById(course.getInstructorId()).orElse(null);
            if (instructor != null && instructor.getFullName() != null) {
                instructorName = instructor.getFullName();
            }
        }

        CartItem newItem = CartItem.builder()
                .cart(cart)
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .thumbnailUrl(course.getThumbnailUrl())
                .instructorName(instructorName)
                .priceAtAdd(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO)
                .build();

        cartItemRepository.saveAndFlush(newItem);

        return toCartResponse(userId, cart.getId());
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long courseId) {
        boolean exists = cartItemRepository.existsByCartUserIdAndCourseId(userId, courseId);
        if (!exists) {
            throw new ApiException("Khóa học không có trong giỏ hàng", HttpStatus.NOT_FOUND);
        }

        cartItemRepository.deleteByCartUserIdAndCourseId(userId, courseId);
        cartItemRepository.flush();

        Cart cart = getOrCreateCart(userId);
        return toCartResponse(userId, cart.getId());
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByCartUserId(userId);
        cartItemRepository.flush();
    }

    public List<Long> getCartCourseIds(Long userId) {
        return cartItemRepository.findAllByCartUserIdOrderByAddedAtDesc(userId).stream()
                .map(CartItem::getCourseId)
                .distinct()
                .collect(Collectors.toList());
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toCartResponse(Long userId, Long cartId) {
        List<CartItem> rawItems = cartItemRepository.findAllByCartUserIdOrderByAddedAtDesc(userId);
        Map<Long, CartItem> byCourse = new LinkedHashMap<>();
        for (CartItem item : rawItems) {
            byCourse.putIfAbsent(item.getCourseId(), item);
        }

        List<CartResponse.CartItemDto> items = byCourse.values().stream()
                .map(item -> CartResponse.CartItemDto.builder()
                        .id(item.getId())
                        .courseId(item.getCourseId())
                        .courseTitle(item.getCourseTitle())
                        .thumbnailUrl(item.getThumbnailUrl())
                        .instructorName(item.getInstructorName())
                        .price(item.getPriceAtAdd())
                        .addedAt(item.getAddedAt())
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartResponse.CartItemDto::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cartId)
                .userId(userId)
                .items(items)
                .totalItems(items.size())
                .totalAmount(total)
                .build();
    }
}
