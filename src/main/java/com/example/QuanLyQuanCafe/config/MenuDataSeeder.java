package com.example.QuanLyQuanCafe.config;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.MenuCategory;
import com.example.QuanLyQuanCafe.model.MenuItem;
import com.example.QuanLyQuanCafe.model.MenuItemStatus;
import com.example.QuanLyQuanCafe.repository.MenuCategoryRepository;
import com.example.QuanLyQuanCafe.repository.MenuItemRepository;

@Component
public class MenuDataSeeder {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuDataSeeder(MenuCategoryRepository menuCategoryRepository, MenuItemRepository menuItemRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedMenu() {
        Map<String, MenuCategory> categories = seedCategories();
        seedItems(categories);
    }

    private Map<String, MenuCategory> seedCategories() {
        Map<String, MenuCategory> byName = new LinkedHashMap<>();

        byName.put("Cà phê", upsertCategory("Cà phê", 1, true));
        byName.put("Trà", upsertCategory("Trà", 2, true));
        byName.put("Matcha", upsertCategory("Matcha", 3, true));
        byName.put("Chocolate", upsertCategory("Chocolate", 4, true));
        byName.put("Đá xay", upsertCategory("Đá xay", 5, true));
        byName.put("Nước ép", upsertCategory("Nước ép", 6, true));
        byName.put("Soda", upsertCategory("Soda", 7, true));
        byName.put("Sinh tố", upsertCategory("Sinh tố", 8, true));
        byName.put("Sữa chua", upsertCategory("Sữa chua", 9, true));
        byName.put("Bánh ngọt", upsertCategory("Bánh ngọt", 10, true));
        byName.put("Thức ăn nhẹ", upsertCategory("Thức ăn nhẹ", 11, true));

        return byName;
    }

    private void seedItems(Map<String, MenuCategory> categories) {
        MenuCategory caPhe = categories.get("Cà phê");
        MenuCategory tra = categories.get("Trà");
        MenuCategory matcha = categories.get("Matcha");
        MenuCategory chocolate = categories.get("Chocolate");
        MenuCategory daXay = categories.get("Đá xay");
        MenuCategory nuocEp = categories.get("Nước ép");
        MenuCategory soda = categories.get("Soda");
        MenuCategory sinhTo = categories.get("Sinh tố");
        MenuCategory suaChua = categories.get("Sữa chua");
        MenuCategory banhngot = categories.get("Bánh ngọt");
        MenuCategory anNhe = categories.get("Thức ăn nhẹ");

        List<Runnable> seeds = List.of(
                () -> upsertItem(caPhe, "Espresso", "Cà phê đậm vị, chiết xuất 30ml.", "35000", "12000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(caPhe, "Americano", "Espresso pha loãng, thơm và nhẹ.", "39000", "14000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(caPhe, "Cappuccino", "Espresso, sữa nóng, bọt sữa mịn.", "49000", "18000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(caPhe, "Latte", "Cà phê sữa béo nhẹ, dễ uống.", "52000", "20000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(caPhe, "Cà phê sữa đá", "Robusta rang đậm + sữa đặc + đá.", "35000", "12000", null, MenuItemStatus.AVAILABLE, 5),
                () -> upsertItem(caPhe, "Bạc xỉu", "Nhiều sữa, cà phê nhẹ, thơm ngọt.", "39000", "14000", null, MenuItemStatus.AVAILABLE, 6),
                () -> upsertItem(caPhe, "Cà phê trứng", "Kem trứng béo mịn, thơm vani.", "55000", "22000", null, MenuItemStatus.AVAILABLE, 7),
                () -> upsertItem(caPhe, "Cold Brew", "Ủ lạnh 12–16h, hậu vị ngọt dịu.", "52000", "18000", null, MenuItemStatus.AVAILABLE, 8),
                () -> upsertItem(caPhe, "Cold Brew cam", "Cold brew + cam tươi, thơm mát.", "59000", "22000", null, MenuItemStatus.AVAILABLE, 9),
                () -> upsertItem(caPhe, "Mocha", "Espresso + chocolate + sữa, cân bằng.", "59000", "24000", null, MenuItemStatus.AVAILABLE, 10),

                () -> upsertItem(tra, "Trà đào cam sả", "Trà đen, đào, cam tươi, sả thơm.", "49000", "17000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(tra, "Trà vải", "Trà thơm, vị vải ngọt thanh.", "45000", "16000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(tra, "Trà chanh mật ong", "Chanh tươi, mật ong, trà ấm/lạnh.", "42000", "14000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(tra, "Trà sữa truyền thống", "Trà đen, sữa, vị ngọt vừa.", "45000", "17000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(tra, "Trà tắc", "Quất tươi, thơm mát, chua ngọt.", "39000", "13000", null, MenuItemStatus.AVAILABLE, 5),
                () -> upsertItem(tra, "Trà gừng mật ong", "Gừng ấm, mật ong dịu, tốt cho cổ họng.", "42000", "15000", null, MenuItemStatus.AVAILABLE, 6),
                () -> upsertItem(tra, "Trà hoa cúc", "Hương hoa cúc dịu nhẹ, thư giãn.", "42000", "14000", null, MenuItemStatus.AVAILABLE, 7),

                () -> upsertItem(matcha, "Matcha Latte", "Matcha Nhật, sữa tươi, ít ngọt.", "56000", "23000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(matcha, "Matcha Cloud", "Matcha béo mịn, lớp kem sữa bồng bềnh.", "59000", "25000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(matcha, "Matcha đá xay", "Matcha xay lạnh, kem sữa mịn.", "65000", "28000", null, MenuItemStatus.AVAILABLE, 3),

                () -> upsertItem(chocolate, "Chocolate nóng", "Chocolate đậm, ấm áp, thơm cacao.", "52000", "22000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(chocolate, "Chocolate đá", "Chocolate mát lạnh, vị cacao rõ.", "52000", "22000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(chocolate, "Chocolate đá xay", "Chocolate xay lạnh, béo mịn.", "65000", "29000", null, MenuItemStatus.AVAILABLE, 3),

                () -> upsertItem(daXay, "Cookies & Cream", "Đá xay kem sữa, bánh quy nghiền.", "59000", "26000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(daXay, "Mocha Frappe", "Cà phê + chocolate, xay lạnh.", "62000", "28000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(daXay, "Caramel Frappe", "Kem sữa xay, caramel thơm ngọt.", "62000", "28000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(daXay, "Dâu đá xay", "Dâu xay lạnh, chua ngọt dễ uống.", "62000", "28000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(daXay, "Xoài đá xay", "Xoài chín xay lạnh, thơm ngọt.", "62000", "28000", null, MenuItemStatus.AVAILABLE, 5),

                () -> upsertItem(nuocEp, "Nước ép cam", "Cam tươi vắt, không hương liệu.", "45000", "20000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(nuocEp, "Nước ép dứa", "Dứa tươi, vị chua ngọt cân bằng.", "45000", "20000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(nuocEp, "Nước ép ổi", "Ổi tươi, thơm, giàu vitamin C.", "45000", "20000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(nuocEp, "Nước ép cà rốt", "Cà rốt tươi, nhẹ bụng, dễ uống.", "45000", "19000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(nuocEp, "Nước ép táo", "Táo tươi, thanh mát.", "49000", "22000", null, MenuItemStatus.AVAILABLE, 5),
                () -> upsertItem(nuocEp, "Nước ép dưa hấu", "Dưa hấu tươi, giải khát.", "45000", "20000", null, MenuItemStatus.AVAILABLE, 6),

                () -> upsertItem(soda, "Soda chanh", "Soda mát lạnh, chanh tươi.", "42000", "15000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(soda, "Soda việt quất", "Soda, syrup việt quất, chua ngọt.", "45000", "16000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(soda, "Soda đào", "Soda, đào, thơm nhẹ.", "45000", "16000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(soda, "Soda dâu", "Soda, dâu, vị chua ngọt.", "45000", "16000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(soda, "Soda chanh dây", "Soda + chanh dây, thơm nồng.", "49000", "18000", null, MenuItemStatus.AVAILABLE, 5),

                () -> upsertItem(sinhTo, "Sinh tố bơ", "Bơ sánh mịn, béo thơm.", "59000", "26000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(sinhTo, "Sinh tố xoài", "Xoài chín, thơm ngọt.", "59000", "26000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(sinhTo, "Sinh tố dâu", "Dâu tươi, chua ngọt cân bằng.", "59000", "26000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(sinhTo, "Sinh tố chuối", "Chuối chín, mịn, dễ uống.", "52000", "23000", null, MenuItemStatus.AVAILABLE, 4),

                () -> upsertItem(suaChua, "Sữa chua đá", "Sữa chua xay lạnh, chua nhẹ.", "49000", "22000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(suaChua, "Sữa chua dâu", "Sữa chua + dâu, chua ngọt.", "52000", "24000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(suaChua, "Sữa chua việt quất", "Sữa chua + việt quất, thơm mát.", "52000", "24000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(suaChua, "Sữa chua chanh dây", "Sữa chua + chanh dây, thơm nồng.", "52000", "24000", null, MenuItemStatus.AVAILABLE, 4),

                () -> upsertItem(banhngot, "Croissant bơ", "Bánh sừng bò bơ, vỏ giòn, ruột mềm.", "35000", "15000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(banhngot, "Pain au chocolat", "Croissant nhân chocolate.", "39000", "17000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(banhngot, "Tiramisu", "Bánh tiramisu béo nhẹ, thơm cà phê.", "59000", "28000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(banhngot, "Cheesecake", "Cheesecake mịn, vị phô mai rõ.", "59000", "28000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(banhngot, "Brownie", "Brownie chocolate đậm, mềm ẩm.", "42000", "20000", null, MenuItemStatus.AVAILABLE, 5),
                () -> upsertItem(banhngot, "Muffin việt quất", "Muffin mềm xốp, việt quất thơm.", "42000", "20000", null, MenuItemStatus.AVAILABLE, 6),

                () -> upsertItem(anNhe, "Sandwich gà", "Bánh mì sandwich, gà, rau, sốt.", "65000", "33000", null, MenuItemStatus.AVAILABLE, 1),
                () -> upsertItem(anNhe, "Sandwich trứng", "Trứng, sốt mayo, rau tươi.", "59000", "30000", null, MenuItemStatus.AVAILABLE, 2),
                () -> upsertItem(anNhe, "Salad ức gà", "Rau xanh + ức gà, sốt mè.", "69000", "35000", null, MenuItemStatus.AVAILABLE, 3),
                () -> upsertItem(anNhe, "Khoai tây chiên", "Khoai tây chiên giòn, kèm tương.", "45000", "22000", null, MenuItemStatus.AVAILABLE, 4),
                () -> upsertItem(anNhe, "Gà nuggets", "Gà viên chiên giòn, sốt chấm.", "49000", "25000", null, MenuItemStatus.AVAILABLE, 5),
                () -> upsertItem(anNhe, "Hạt mix", "Hạt tổng hợp rang, ăn kèm đồ uống.", "39000", "18000", null, MenuItemStatus.AVAILABLE, 6)
        );

        seeds.forEach(Runnable::run);
    }

    private MenuCategory upsertCategory(String name, int sortOrder, boolean active) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không hợp lệ");
        }

        MenuCategory category = menuCategoryRepository.findByName(trimmed).orElseGet(MenuCategory::new);
        category.setName(trimmed);
        category.setSlug(slugify(trimmed));
        category.setSortOrder(sortOrder);
        category.setActive(active);
        return menuCategoryRepository.save(category);
    }

    private MenuItem upsertItem(
            MenuCategory category,
            String name,
            String description,
            String price,
            String cost,
            String imageUrl,
            MenuItemStatus status,
            int sortOrder
    ) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Tên món không hợp lệ");
        }

        MenuItem item = menuItemRepository.findByName(trimmed).orElseGet(MenuItem::new);
        if (category != null) {
            item.setCategory(category);
        }
        item.setName(trimmed);
        item.setDescription(description);
        item.setPrice(toMoney(price));
        item.setCost(toMoney(cost));
        item.setImageUrl(imageUrl);
        item.setStatus(status != null ? status : MenuItemStatus.AVAILABLE);
        item.setSortOrder(sortOrder);

        return menuItemRepository.save(item);
    }

    private static BigDecimal toMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private static String slugify(String input) {
        String base = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return base;
    }
}
