package com.example.QuanLyQuanCafe.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.QuanLyQuanCafe.model.CustomerReview;
import com.example.QuanLyQuanCafe.repository.CustomerReviewRepository;

@RestController
@RequestMapping("/api/public/reviews")
public class PublicReviewApiController {

    private final CustomerReviewRepository customerReviewRepository;

    public PublicReviewApiController(CustomerReviewRepository customerReviewRepository) {
        this.customerReviewRepository = customerReviewRepository;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestParam(value = "name", required = false) String name,
                                    @RequestParam(value = "role", required = false) String role,
                                    @RequestParam(value = "phone", required = false) String phone,
                                    @RequestParam("rating") int rating,
                                    @RequestParam("comment") String comment) {
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "code", "missingInfo",
                            "message", "Vui lòng nhập họ tên và số điện thoại."));
        }

        if (rating < 1) {
            rating = 1;
        } else if (rating > 5) {
            rating = 5;
        }

        if (comment == null || comment.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "code", "missingComment", "message", "Vui lòng nhập cảm nhận."));
        }

        CustomerReview review = new CustomerReview();
        review.setCustomerName(name != null ? name.trim() : "Khách hàng");
        review.setCustomerRole((role == null || role.isBlank()) ? "Khách hàng" : role.trim());
        review.setRating(rating);
        review.setComment(comment.trim());
        if (phone != null && !phone.isBlank()) {
            review.setPhone(phone.trim());
        }
        customerReviewRepository.save(review);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Cảm ơn bạn! Đánh giá của bạn đã được ghi nhận."));
    }
}
