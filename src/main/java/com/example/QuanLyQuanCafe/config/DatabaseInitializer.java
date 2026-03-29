package com.example.QuanLyQuanCafe.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.CustomerReview;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.CustomerReviewRepository;

@Component
public class DatabaseInitializer {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerReviewRepository customerReviewRepository;

    public DatabaseInitializer(AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               CustomerReviewRepository customerReviewRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerReviewRepository = customerReviewRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (userRepository.findByUsername("admin") == null) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Quản trị viên");
            admin.setEmail("admin@cafe.com");
            admin.setPhone("0901 234 567");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("nhanvien") == null) {
            AppUser staff = new AppUser();
            staff.setUsername("nhanvien");
            staff.setPassword(passwordEncoder.encode("123456"));
            staff.setFullName("Nhân viên thu ngân");
            staff.setEmail("nhanvien@cafe.com");
            staff.setPhone("0912 345 678");
            staff.setRole("STAFF");
            userRepository.save(staff);
        }

        if (customerReviewRepository.count() == 0) {
            CustomerReview r1 = new CustomerReview();
            r1.setCustomerName("Minh Anh");
            r1.setCustomerRole("Freelancer");
            r1.setRating(5);
            r1.setComment("Không gian cực kỳ thoải mái, mình thường ngồi làm việc cả buổi sáng. Cà phê trứng ở đây là ngon nhất mình từng thử!");
            r1.setCreatedAt(LocalDateTime.now().minusDays(3));

            CustomerReview r2 = new CustomerReview();
            r2.setCustomerName("Quốc Hưng");
            r2.setCustomerRole("Software Engineer");
            r2.setRating(5);
            r2.setComment("Brew & Co là nơi tôi hay dẫn team đến họp. Đồ uống ngon, không gian yên tĩnh và WiFi cực ổn định.");
            r2.setCreatedAt(LocalDateTime.now().minusDays(2));

            CustomerReview r3 = new CustomerReview();
            r3.setCustomerName("Thu Trang");
            r3.setCustomerRole("Designer");
            r3.setRating(5);
            r3.setComment("Aesthetic thật sự đỉnh cao. Mỗi góc trong quán đều có thể chụp ảnh đẹp. Matcha Cloud là must-try!");
            r3.setCreatedAt(LocalDateTime.now().minusDays(1));

            customerReviewRepository.save(r1);
            customerReviewRepository.save(r2);
            customerReviewRepository.save(r3);
        }
    }
}
