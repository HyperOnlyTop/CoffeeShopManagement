package com.example.QuanLyQuanCafe.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.CustomerReview;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.CustomerReviewRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

@Component
public class DatabaseInitializer {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerReviewRepository customerReviewRepository;
    private final StaffRepository staffRepository;

    public DatabaseInitializer(AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               CustomerReviewRepository customerReviewRepository,
                               StaffRepository staffRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerReviewRepository = customerReviewRepository;
        this.staffRepository = staffRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (userRepository.findByUsername("admin") == null) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Quản trị viên");
            admin.setEmail("admin@cafe.com");
            admin.setPhone("0901234567");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
        // Nghiệp vụ: đa số nhân viên xoay ca theo ngày -> shift mặc định để trống (null).
        // Riêng Bảo vệ thường cố định giờ -> có thể set shift mặc định FULL_DAY.
        seedStaffUser("nhanvien", "Nhân viên thu ngân", "nhanvien@cafe.com", "0912345678", "CASHIER", StaffRole.THU_NGAN, null);
        seedStaffUser("phucvu", "Nhân viên phục vụ", "phucvu@cafe.com", "0912345679", "SERVER", StaffRole.PHUC_VU, null);
        seedStaffUser("phache", "Nhân viên pha chế", "phache@cafe.com", "0912345680", "BARISTA", StaffRole.PHA_CHE, null);
        seedStaffUser("baove", "Nhân viên bảo vệ", "baove@cafe.com", "0912345681", "SECURITY", StaffRole.BAO_VE, "FULL_DAY");

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

    private void seedStaffUser(
            String username,
            String fullName,
            String email,
            String phoneDigits,
            String appRole,
            StaffRole staffRole,
            String shiftCode
    ) {
        if (username == null || username.isBlank()) return;
        if (phoneDigits == null || phoneDigits.isBlank()) return;

        Staff staff = staffRepository.findByPhone(phoneDigits);
        if (staff == null) {
            staff = new Staff();
            staff.setName(fullName);
            staff.setPhone(phoneDigits);
            staff.setRole(staffRole);
            staff.setStatus(StaffStatus.ACTIVE);
            staff.setShift(shiftCode); // ca mặc định (tuỳ chọn)
            staff = staffRepository.save(staff);
        }

        AppUser user = userRepository.findByUsername(username);
        if (user == null) {
            user = new AppUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phoneDigits);
            user.setRole(appRole);
            user.setStaffId(staff.getId());
            userRepository.save(user);
            return;
        }

        // Nếu đã có user thì đảm bảo role và staffId đúng để không bị lệch data
        boolean changed = false;
        if (user.getRole() == null || !user.getRole().equalsIgnoreCase(appRole)) {
            user.setRole(appRole);
            changed = true;
        }
        if (user.getStaffId() == null || !user.getStaffId().equals(staff.getId())) {
            // tránh gán nhầm nếu staffId đang trỏ tới staff khác
            if (user.getStaffId() == null) {
                user.setStaffId(staff.getId());
                changed = true;
            }
        }
        if (changed) {
            userRepository.save(user);
        }
    }
}