package com.example.QuanLyQuanCafe.config;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.BookingStatus;
import com.example.QuanLyQuanCafe.model.TableBooking;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;
import com.example.QuanLyQuanCafe.repository.TableBookingRepository;

/**
 * Seed đặt bàn mẫu (idempotent: cặp SĐT + giờ đặt; mỗi {@code booking_time} duy nhất toàn DB).
 * Lịch đặt bàn dày 28/3–3/4 và thêm 4–6/4/2026.
 * Lịch sử 28/3–3/4: {@link BookingStatus#CHECKED_IN}; 4–6/4: thêm {@link BookingStatus#COMPLETED} và {@link BookingStatus#CANCELLED}
 * (lý do hủy trong ghi chú dạng {@code [Hủy] …}).
 */
@Component
public class BookingDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(BookingDataSeeder.class);

    private final MenuItemRepository menuItemRepository;
    private final TableBookingRepository tableBookingRepository;

    public BookingDataSeeder(MenuItemRepository menuItemRepository, TableBookingRepository tableBookingRepository) {
        this.menuItemRepository = menuItemRepository;
        this.tableBookingRepository = tableBookingRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(201)
    @Transactional
    public void seedBookings() {
        if (menuItemRepository.count() == 0) {
            log.warn("BookingDataSeeder: bỏ qua — chưa có món trong menu.");
            return;
        }

        seedDemoBookings();
        seedBookingsMar28ThroughApr3_2026();
        seedBookingsApr4ThroughApr6_2026();
    }

    private void seedDemoBookings() {
        upsertCompletedBooking(
                "Nguyễn Văn An",
                "0912000101",
                "an.nguyen@demo.local",
                LocalDateTime.of(2026, 3, 8, 18, 0),
                LocalDateTime.of(2026, 3, 1, 10, 0),
                4,
                3,
                "Sinh nhật — dễ uống ngọt nhẹ");

        upsertCompletedBooking(
                "Hoàng Thị Mai",
                "0912000102",
                "mai.hoang@demo.local",
                LocalDateTime.of(2026, 3, 12, 12, 0),
                LocalDateTime.of(2026, 3, 5, 9, 0),
                2,
                5,
                "Gần cửa sổ");

        upsertCompletedBooking(
                "Võ Đức Thịnh",
                "0912000103",
                null,
                LocalDateTime.of(2026, 3, 16, 19, 0),
                LocalDateTime.of(2026, 3, 10, 14, 0),
                6,
                8,
                "Có trẻ em — ghế thêm nếu có");

        upsertCompletedBooking(
                "Bùi Lan Chi",
                "0912000104",
                "chi.bui@demo.local",
                LocalDateTime.of(2026, 3, 20, 15, 0),
                LocalDateTime.of(2026, 3, 12, 11, 0),
                3,
                2,
                "Họp nhóm nhỏ");
    }

    /**
     * Lịch đặt bàn 28/3–3/4/2026: 5 slot/ngày × 7 ngày = 35 bản ghi.
     * Giờ đặt giờ chẵn (phút 0), trong mỗi ngày cách nhau đúng 1 giờ; mỗi {@code booking_time} duy nhất toàn DB.
     * SĐT 0915055001–0915055035; idempotent: bỏ qua nếu trùng giờ đặt hoặc trùng cặp SĐT + giờ.
     */
    private void seedBookingsMar28ThroughApr3_2026() {
        int seq = 1;
        seq = seedCalendarBookings(2026, 3, 28, seq, List.of(
                new CalSlot(10, 0, 2, 5, "Trần Thu Hương", "huong.tran@demo.local", "Uống trà chiều"),
                new CalSlot(11, 0, 4, 7, "Lê Quốc Huy", "huy.le@demo.local", "Sinh nhật bạn"),
                new CalSlot(12, 0, 3, null, "Phạm Ngọc Lan", null, "Chưa chọn bàn — linh hoạt"),
                new CalSlot(13, 0, 8, 12, "Hoàng Đức Anh", "anh.hoang@demo.local", "Họp nhóm lớn"),
                new CalSlot(14, 0, 2, 3, "Võ Thị Mai", null, "Cuối ngày, yên tĩnh")));
        seq = seedCalendarBookings(2026, 3, 29, seq, List.of(
                new CalSlot(11, 0, 3, 4, "Đặng Minh Khôi", "khoi.dang@demo.local", "Brunch cuối tuần"),
                new CalSlot(12, 0, 2, 6, "Bùi Thảo My", null, "Gần cửa sổ"),
                new CalSlot(13, 0, 5, 9, "Nguyễn Hải Nam", "nam.nguyen@demo.local", "Có trẻ nhỏ"),
                new CalSlot(14, 0, 6, 11, "Đinh Thuỳ Linh", "linh.dinh@demo.local", "Tiệc nhỏ"),
                new CalSlot(15, 0, 4, 8, "Mai Phương Đông", null, "Tối thứ bảy")));
        seq = seedCalendarBookings(2026, 3, 30, seq, List.of(
                new CalSlot(9, 0, 2, 2, "Lý Gia Hân", "han.ly@demo.local", "Sáng sớm"),
                new CalSlot(10, 0, 3, 10, "Chu Bảo Long", null, "Trưa vắng"),
                new CalSlot(11, 0, 7, 14, "Tôn Nữ Ánh Tuyết", "tuyet.book@demo.local", "Họp team"),
                new CalSlot(12, 0, 2, 5, "Cao Hoài Nam", null, "Hẹn gặp bạn"),
                new CalSlot(13, 0, 4, 16, "Kiều Bích Ngọc", "ngoc.kieu@demo.local", "Tối xem bóng đá")));
        seq = seedCalendarBookings(2026, 3, 31, seq, List.of(
                new CalSlot(12, 0, 3, 6, "Quách Đình Phúc", "phuc.quach@demo.local", "Cuối tháng"),
                new CalSlot(13, 0, 5, 13, "Hà Thu Trang", null, "Họp phụ huynh xong"),
                new CalSlot(14, 0, 2, 1, "La Tuấn Kiệt", "kiet.la@demo.local", null),
                new CalSlot(15, 0, 8, 18, "Giáp Thị Yến", null, "Tiệc chia tay đồng nghiệp"),
                new CalSlot(16, 0, 3, 4, "Phan Bảo Châu", "chau.phan@demo.local", "Slot muộn")));
        seq = seedCalendarBookings(2026, 4, 1, seq, List.of(
                new CalSlot(10, 0, 2, 3, "Vương Thế Sơn", "son.vuong@demo.local", "Cà phê sáng 1/4"),
                new CalSlot(11, 0, 6, 15, "Thân Minh Tuấn", null, "Đông người"),
                new CalSlot(12, 0, 4, 7, "Uông Thị Hạnh", "hanh.uong@demo.local", "Chiều mát"),
                new CalSlot(13, 0, 2, 2, "Dương Kim Ngân", null, "Đi một mình"),
                new CalSlot(14, 0, 5, 17, "Từ Đức Thịnh", "thinh.tu@demo.local", "Tối muộn")));
        seq = seedCalendarBookings(2026, 4, 2, seq, List.of(
                new CalSlot(11, 0, 3, 8, "Hồ Ngọc Bích", null, "Thử cold brew"),
                new CalSlot(12, 0, 4, 9, "Lương Văn Tài", "tai.luong@demo.local", "Họp dự án"),
                new CalSlot(13, 0, 2, 5, "Tạ Minh Tuệ", null, "Góc làm việc"),
                new CalSlot(14, 0, 7, 12, "Đỗ Quang Huy", "huy.do@demo.local", "Nhóm bạn đông"),
                new CalSlot(15, 0, 3, 6, "Âu Dương Phong", null, "Sau xem phim")));
        seedCalendarBookings(2026, 4, 3, seq, List.of(
                new CalSlot(10, 0, 4, 10, "Khúc Anh Thư", "thu.khuc@demo.local", "Trưa Chủ nhật"),
                new CalSlot(11, 0, 2, 3, "Tiêu Việt Hùng", null, "Thư giãn"),
                new CalSlot(12, 0, 6, 11, "Chế Linh Phụng", "phung.che@demo.local", "Sinh nhật con"),
                new CalSlot(13, 0, 3, 14, "Viên Hoài Thương", null, "Gặp bạn cũ"),
                new CalSlot(14, 0, 5, 19, "Tô Hiếu Nghĩa", "nghia.to@demo.local", "Khách quen — bàn quen")));
    }

    /**
     * Lịch đặt bàn 4–6/4/2026: 5 + 4 + 6 bản ghi (SĐT nối tiếp sau 28/3–3/4).
     * Có {@link BookingStatus#COMPLETED} (hoàn thành) và {@link BookingStatus#CANCELLED} (lý do hủy xoay vòng, ổn định theo chỉ số slot).
     */
    private void seedBookingsApr4ThroughApr6_2026() {
        int seq = 36;
        seq = seedCalendarBookingsMixed(2026, 4, 4, seq, List.of(
                new CalBookingSlot(10, 0, 3, 4, "Ngụy Anh Khoa", "khoa.nguy@demo.local", "Sáng sau nghỉ lễ", BookingStatus.COMPLETED),
                new CalBookingSlot(11, 0, 2, 6, "Thạch Sanh", null, "Góc yên", BookingStatus.CANCELLED),
                new CalBookingSlot(12, 0, 5, 10, "Lý Thông", "ly.thong@demo.local", "Đoàn đông", BookingStatus.COMPLETED),
                new CalBookingSlot(14, 0, 4, 8, "Quỳnh Nga", null, "Hẹn đối tác", BookingStatus.CANCELLED),
                new CalBookingSlot(15, 0, 2, 3, "Mai An Tiêm", "tien.mai@demo.local", null, BookingStatus.COMPLETED)));
        seq = seedCalendarBookingsMixed(2026, 4, 5, seq, List.of(
                new CalBookingSlot(10, 0, 3, 5, "Trương Chi", "chi.truong@demo.local", "Brunch", BookingStatus.COMPLETED),
                new CalBookingSlot(11, 0, 2, 2, "Mỵ Nương", null, "Trưa vắng", BookingStatus.COMPLETED),
                new CalBookingSlot(13, 0, 6, 12, "Thục Phàn", "phan.thuc@demo.local", "Tiệc nhỏ", BookingStatus.CANCELLED),
                new CalBookingSlot(15, 0, 3, 7, "Gia Cát Lượng", null, "Chiều họp nhóm", BookingStatus.CANCELLED)));
        seedCalendarBookingsMixed(2026, 4, 6, seq, List.of(
                new CalBookingSlot(9, 0, 2, 1, "Lưu Bị", "be.luu@demo.local", "Sớm", BookingStatus.COMPLETED),
                new CalBookingSlot(10, 0, 4, 9, "Quan Vũ", null, "Gần cửa", BookingStatus.CANCELLED),
                new CalBookingSlot(11, 0, 3, 6, "Trương Phi", "phi.truong@demo.local", null, BookingStatus.COMPLETED),
                new CalBookingSlot(13, 0, 8, 14, "Triệu Vân", "van.trieu@demo.local", "Đông người", BookingStatus.COMPLETED),
                new CalBookingSlot(14, 0, 2, 4, "Hoàng Trung", null, "Nghỉ trưa", BookingStatus.CANCELLED),
                new CalBookingSlot(16, 0, 5, 11, "Mã Siêu", "sieu.ma@demo.local", "Chiều muộn", BookingStatus.COMPLETED)));
    }

    private static final String[] SEED_BOOKING_CANCEL_REASONS = {
            "Khách báo bận đột xuất",
            "Đổi sang ngày khác",
            "Hết bàn phù hợp số khách",
            "Trùng lịch cá nhân",
            "Không liên lạc được — hủy giữ chỗ",
            "Thời tiết — hoãn cuộc hẹn",
    };

    private static String seedBookingCancelReason(int salt) {
        return SEED_BOOKING_CANCEL_REASONS[Math.floorMod(salt, SEED_BOOKING_CANCEL_REASONS.length)];
    }

    private int seedCalendarBookingsMixed(int year, int month, int day, int phoneSeq, List<CalBookingSlot> slots) {
        int n = phoneSeq;
        for (CalBookingSlot s : slots) {
            LocalDateTime bookingTime = LocalDateTime.of(year, month, day, s.hour(), s.minute());
            LocalDateTime createdAt = bookingTime.minusDays(1).withHour(9).withMinute(0);
            if (!createdAt.isBefore(bookingTime)) {
                createdAt = bookingTime.minusHours(3).withMinute(0);
            }
            String phone = String.format("0915055%03d", n);
            String cancelReason = s.status() == BookingStatus.CANCELLED ? seedBookingCancelReason(n + s.hour()) : null;
            upsertBooking(s.name(), phone, s.email(), bookingTime, createdAt, s.guests(), s.table(), s.note(),
                    s.status(), cancelReason);
            n++;
        }
        return n;
    }

    private int seedCalendarBookings(int year, int month, int day, int phoneSeq, List<CalSlot> slots) {
        int n = phoneSeq;
        for (CalSlot s : slots) {
            LocalDateTime bookingTime = LocalDateTime.of(year, month, day, s.hour(), s.minute());
            LocalDateTime createdAt = bookingTime.minusDays(1).withHour(9).withMinute(0);
            if (!createdAt.isBefore(bookingTime)) {
                createdAt = bookingTime.minusHours(3).withMinute(0);
            }
            String phone = String.format("0915055%03d", n);
            upsertCompletedBooking(s.name(), phone, s.email(), bookingTime, createdAt, s.guests(), s.table(), s.note());
            n++;
        }
        return n;
    }

    private record CalSlot(int hour, int minute, int guests, Integer table, String name, String email, String note) {
    }

    private record CalBookingSlot(int hour, int minute, int guests, Integer table, String name, String email, String note,
            BookingStatus status) {
    }

    private void upsertCompletedBooking(
            String name,
            String phone,
            String email,
            LocalDateTime bookingTime,
            LocalDateTime createdAt,
            int guests,
            Integer reservedTable,
            String note) {
        upsertBooking(name, phone, email, bookingTime, createdAt, guests, reservedTable, note, BookingStatus.CHECKED_IN, null);
    }

    /**
     * @param cancelReason chỉ dùng khi {@code status == CANCELLED}; ghi vào note dạng {@code [Hủy] …} (giống staff hủy trên UI).
     */
    private void upsertBooking(
            String name,
            String phone,
            String email,
            LocalDateTime bookingTime,
            LocalDateTime createdAt,
            int guests,
            Integer reservedTable,
            String note,
            BookingStatus status,
            String cancelReason) {
        if (tableBookingRepository.existsByBookingTime(bookingTime)) {
            return;
        }
        if (tableBookingRepository.existsByPhoneAndBookingTime(phone, bookingTime)) {
            return;
        }
        TableBooking b = new TableBooking();
        b.setName(name);
        b.setPhone(phone);
        b.setEmail(email);
        b.setBookingTime(bookingTime);
        b.setGuests(guests);
        b.setCreatedAt(createdAt);
        b.setStatus(status);
        b.setReservedTableNumber(reservedTable);

        if (status == BookingStatus.CANCELLED) {
            String line = "[Hủy] " + (cancelReason != null && !cancelReason.isBlank() ? cancelReason.trim() : "Khách hủy");
            if (note != null && !note.isBlank()) {
                b.setNote(note.trim() + "\n" + line);
            } else {
                b.setNote(line);
            }
        } else {
            b.setNote(note);
        }

        tableBookingRepository.save(b);
    }
}
