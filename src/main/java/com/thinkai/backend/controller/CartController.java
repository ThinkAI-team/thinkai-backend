package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.dto.CartResponse;
import com.thinkai.backend.exception.ApiException;
import com.thinkai.backend.repository.UserRepository;
import com.thinkai.backend.security.StudentOnly;
import com.thinkai.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/cart", "/api/cart", "/v1/cart", "/api/v1/cart"})
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ApiException("Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED))
                .getId();
    }

    @StudentOnly
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication auth) {
        CartResponse cart = cartService.getCart(getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Lấy giỏ hàng thành công", cart));
    }

    @StudentOnly
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            Authentication auth,
            @RequestBody Map<String, Long> body) {
        Long courseId = body.get("courseId");
        if (courseId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<CartResponse>builder()
                            .status(400)
                            .message("Thiếu courseId")
                            .data(null)
                            .build());
        }
        CartResponse cart = cartService.addToCart(getUserId(auth), courseId);
        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ hàng thành công", cart));
    }

    @StudentOnly
    @DeleteMapping("/items/{courseId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            Authentication auth,
            @PathVariable Long courseId) {
        CartResponse cart = cartService.removeFromCart(getUserId(auth), courseId);
        return ResponseEntity.ok(ApiResponse.success("Xóa khỏi giỏ hàng thành công", cart));
    }

    @StudentOnly
    @PostMapping("/items/{courseId}/remove")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCartPostFallback(
            Authentication auth,
            @PathVariable Long courseId) {
        CartResponse cart = cartService.removeFromCart(getUserId(auth), courseId);
        return ResponseEntity.ok(ApiResponse.success("Xóa khỏi giỏ hàng thành công", cart));
    }

    @StudentOnly
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication auth) {
        cartService.clearCart(getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success("Xóa giỏ hàng thành công", null));
    }
}
