package com.example.QuanLyQuanCafe.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.model.Customer;
import com.example.QuanLyQuanCafe.model.CustomerReview;
import com.example.QuanLyQuanCafe.model.Staff;
import com.example.QuanLyQuanCafe.model.StaffRole;
import com.example.QuanLyQuanCafe.model.StaffStatus;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;
import com.example.QuanLyQuanCafe.repository.CustomerRepository;
import com.example.QuanLyQuanCafe.repository.CustomerReviewRepository;
import com.example.QuanLyQuanCafe.repository.StaffRepository;

/**
 * Seed dữ liệu khi ứng dụng khởi động (idempotent).
 * <ul>
 *   <li><b>admin</b> = chủ quán: {@code role ADMIN}, {@code staffId null} — không hồ sơ {@link Staff}, không chấm công / lương theo giờ trong app.</li>
 *   <li>Nhân viên ca: {@link AppUser} có {@code staffId} trỏ tới {@link Staff} (CASHIER, SERVER, BARISTA, SECURITY).</li>
 * </ul>
 */
@Component
public class DatabaseInitializer {

    private static final String OWNER_ADMIN_USERNAME = "admin";
    private static final String OWNER_ADMIN_EMAIL = "admin@cafe.com";
    private static final String OWNER_ADMIN_PHONE = "0901234567";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerReviewRepository customerReviewRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;

    public DatabaseInitializer(AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               CustomerReviewRepository customerReviewRepository,
                               StaffRepository staffRepository,
                               CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerReviewRepository = customerReviewRepository;
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        ensureOwnerAdminAccount();

        // Nghiệp vụ: đa số nhân viên xoay ca theo ngày -> shift mặc định để trống (null).
        // Riêng Bảo vệ thường cố định giờ -> có thể set shift mặc định FULL_DAY.
        seedStaffUser("thungan", "Nguyễn Thị Mai", "mai.thungan@cafe.com", "0912345678", "CASHIER", StaffRole.THU_NGAN, null);
        seedStaffUser("phucvu", "Trần Văn Khôi", "khoi.phucvu@cafe.com", "0912345679", "SERVER", StaffRole.PHUC_VU, null);
        seedStaffUser("phache", "Lê Minh Đức", "duc.phache@cafe.com", "0912345680", "BARISTA", StaffRole.PHA_CHE, null);
        seedStaffUser("baove", "Phạm Quốc Anh", "anh.baove@cafe.com", "0912345681", "SECURITY", StaffRole.BAO_VE, "FULL_DAY");

        // Thêm NV: 2 phục vụ, 1 pha chế, 1 thu ngân (role khớp {@link StaffRole} + Spring role SERVER/BARISTA/CASHIER).
        seedStaffUser("phucvu2", "Trần Thị Lan", "lan.pv2@cafe.com", "0912345682", "SERVER", StaffRole.PHUC_VU, null);
        seedStaffUser("phucvu3", "Lê Văn Hùng", "hung.pv3@cafe.com", "0912345683", "SERVER", StaffRole.PHUC_VU, null);
        seedStaffUser("phache2", "Phạm Thu Hà", "ha.pc2@cafe.com", "0912345684", "BARISTA", StaffRole.PHA_CHE, null);
        seedStaffUser("thungan2", "Đỗ Minh Tuấn", "tuan.tn2@cafe.com", "0912345685", "CASHIER", StaffRole.THU_NGAN, null);

        // Khách đăng ký (role CUSTOMER, khác walk-in guest trên đơn): không gán staff; SĐT 0873001001–015.
        seedCustomerAppUsers();
        // Case 2: mỗi tài khoản CUSTOMER có hồ sơ customers (SĐT) để tích điểm / đăng ký web merge đúng luồng.
        ensureCustomerProfileForEachCustomerAppUser();

        ensureOwnerAdminAccount();

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

    /**
     * Tài khoản {@code admin}: chủ quán — luôn {@code ADMIN}, {@code staffId == null}.
     * Gọi đầu và cuối {@link #init()} để sau mọi bước seed vẫn đúng nghiệp vụ.
     */
    private void ensureOwnerAdminAccount() {
        AppUser admin = userRepository.findByUsername(OWNER_ADMIN_USERNAME);
        if (admin == null) {
            admin = new AppUser();
            admin.setUsername(OWNER_ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("Chủ quán");
            admin.setEmail(OWNER_ADMIN_EMAIL);
            admin.setPhone(OWNER_ADMIN_PHONE);
            admin.setRole("ADMIN");
            admin.setStaffId(null);
            userRepository.save(admin);
            return;
        }
        boolean changed = false;
        if (admin.getStaffId() != null) {
            admin.setStaffId(null);
            changed = true;
        }
        if (admin.getRole() == null || !"ADMIN".equalsIgnoreCase(admin.getRole().trim())) {
            admin.setRole("ADMIN");
            changed = true;
        }
        if (changed) {
            userRepository.save(admin);
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

        if (OWNER_ADMIN_USERNAME.equalsIgnoreCase(username.trim())) {
            return;
        }
        if (OWNER_ADMIN_PHONE.equals(phoneDigits.trim())) {
            return;
        }

        Staff staff = staffRepository.findByPhone(phoneDigits);
        if (staff == null) {
            staff = new Staff();
            staff.setName(fullName);
            staff.setPhone(phoneDigits);
            staff.setRole(staffRole);
            staff.setStatus(StaffStatus.ACTIVE);
            staff.setShift(shiftCode); // ca mặc định (tuỳ chọn)
            staff.setSalary(defaultSalaryForStaffRole(staffRole));
            staff.setStartDate(LocalDate.of(2026, 1, 1));
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
            user.setStaffId(staff.getId());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    /**
     * 15 tài khoản CUSTOMER (bảng {@code users}): username khach1–khach15, email/SĐT unique, {@code staffId} null.
     */
    private void seedCustomerAppUsers() {
        seedCustomerUserIfAbsent("khach1", "Nguyễn Minh An", "khach1@cafe.demo", "0873001001");
        seedCustomerUserIfAbsent("khach2", "Trần Thị Bích", "khach2@cafe.demo", "0873001002");
        seedCustomerUserIfAbsent("khach3", "Lê Hoàng Nam", "khach3@cafe.demo", "0873001003");
        seedCustomerUserIfAbsent("khach4", "Phạm Thu Hà", "khach4@cafe.demo", "0873001004");
        seedCustomerUserIfAbsent("khach5", "Hoàng Đức Thịnh", "khach5@cafe.demo", "0873001005");
        seedCustomerUserIfAbsent("khach6", "Võ Ngọc Linh", "khach6@cafe.demo", "0873001006");
        seedCustomerUserIfAbsent("khach7", "Đặng Quốc Khải", "khach7@cafe.demo", "0873001007");
        seedCustomerUserIfAbsent("khach8", "Bùi Thảo Vy", "khach8@cafe.demo", "0873001008");
        seedCustomerUserIfAbsent("khach9", "Đỗ Phương Chi", "khach9@cafe.demo", "0873001009");
        seedCustomerUserIfAbsent("khach10", "Cao Nhật Long", "khach10@cafe.demo", "0873001010");
        seedCustomerUserIfAbsent("khach11", "Nguyễn Thảo Nhi", "khach11@cafe.demo", "0873001011");
        seedCustomerUserIfAbsent("khach12", "Trần Đức Minh", "khach12@cafe.demo", "0873001012");
        seedCustomerUserIfAbsent("khach13", "Lý Hồng Phúc", "khach13@cafe.demo", "0873001013");
        seedCustomerUserIfAbsent("khach14", "Phan Gia Huy", "khach14@cafe.demo", "0873001014");
        seedCustomerUserIfAbsent("khach15", "Vương Thị Thanh", "khach15@cafe.demo", "0873001015");
    }

    private void seedCustomerUserIfAbsent(String username, String fullName, String email, String phone) {
        if (username == null || username.isBlank()) {
            return;
        }
        if (userRepository.findByUsername(username) != null) {
            return;
        }
        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone)) {
            return;
        }
        if (phone != null && !phone.isBlank() && staffRepository.findByPhone(phone) != null) {
            return;
        }

        AppUser u = new AppUser();
        u.setUsername(username.trim());
        u.setPassword(passwordEncoder.encode("123456"));
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPhone(phone);
        u.setRole("CUSTOMER");
        u.setStaffId(null);
        userRepository.save(u);
        upsertCustomerForAppUserPhone(phone, fullName, email);
    }

    /**
     * Idempotent: với mọi user role CUSTOMER có SĐT (không trùng staff), đảm bảo có một dòng {@code customers}.
     */
    private void ensureCustomerProfileForEachCustomerAppUser() {
        for (AppUser u : userRepository.findAll()) {
            if (u == null || u.getPhone() == null || u.getPhone().isBlank()) {
                continue;
            }
            if (u.getRole() == null || !"CUSTOMER".equalsIgnoreCase(u.getRole().trim())) {
                continue;
            }
            if (staffRepository.findByPhone(u.getPhone().trim()) != null) {
                continue;
            }
            upsertCustomerForAppUserPhone(u.getPhone().trim(), u.getFullName(), u.getEmail());
        }
    }

    private void upsertCustomerForAppUserPhone(String phone, String fullName, String email) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        String p = phone.trim();
        if (customerRepository.findByPhone(p).isPresent()) {
            return;
        }
        Customer c = new Customer();
        c.setPhone(p);
        c.setName(fullName != null && !fullName.isBlank() ? fullName.trim() : "Khách");
        c.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        c.setTotalOrders(0);
        c.setTotalSpent(BigDecimal.ZERO);
        c.setLoyaltyPoints(0);
        c.setLoyaltyRedeemedCount(0);
        customerRepository.save(c);
    }

    /**
     * Mức lương theo giờ (VNĐ/giờ) — khớp nghiệp vụ: chấm công cộng dồn giờ làm, lương tạm tính = giờ × mức này
     * (xem {@code StaffController} dùng {@code staff.getSalary()} làm {@code hourlyRate}).
     */
    private static BigDecimal defaultSalaryForStaffRole(StaffRole role) {
        if (role == null) {
            return BigDecimal.valueOf(25_000L);
        }
        return switch (role) {
            case PHA_CHE -> BigDecimal.valueOf(30_000L);
            case THU_NGAN -> BigDecimal.valueOf(28_000L);
            case PHUC_VU -> BigDecimal.valueOf(25_000L);
            case BAO_VE -> BigDecimal.valueOf(22_000L);
            case QUAN_LY -> BigDecimal.valueOf(35_000L);
        };
    }
}