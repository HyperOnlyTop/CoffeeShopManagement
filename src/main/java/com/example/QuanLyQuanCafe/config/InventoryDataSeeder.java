package com.example.QuanLyQuanCafe.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.InventoryCategory;
import com.example.QuanLyQuanCafe.model.InventoryItem;
import com.example.QuanLyQuanCafe.model.InventoryStatus;
import com.example.QuanLyQuanCafe.model.Supplier;
import com.example.QuanLyQuanCafe.repository.InventoryCategoryRepository;
import com.example.QuanLyQuanCafe.repository.InventoryItemRepository;
import com.example.QuanLyQuanCafe.repository.SupplierRepository;

/**
 * Seed danh mục kho, nhà cung cấp và nguyên liệu mẫu (idempotent theo tên mặt hàng).
 */
@Component
public class InventoryDataSeeder {

    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public InventoryDataSeeder(
            InventoryCategoryRepository inventoryCategoryRepository,
            SupplierRepository supplierRepository,
            InventoryItemRepository inventoryItemRepository) {
        this.inventoryCategoryRepository = inventoryCategoryRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInventory() {
        Map<String, InventoryCategory> categories = seedCategories();
        Map<String, Supplier> suppliers = seedSuppliers();
        seedItems(categories, suppliers);
    }

    private Map<String, InventoryCategory> seedCategories() {
        Map<String, InventoryCategory> map = new LinkedHashMap<>();
        String[] names = {
                "Cà phê & Trà",
                "Sữa & Kem",
                "Nguyên liệu",
                "Dụng cụ",
                "Bao bì"
        };
        for (String name : names) {
            map.put(name, upsertCategory(name));
        }
        return map;
    }

    private InventoryCategory upsertCategory(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục kho không hợp lệ");
        }
        return inventoryCategoryRepository.findByName(trimmed).orElseGet(() -> {
            InventoryCategory c = new InventoryCategory();
            c.setName(trimmed);
            return inventoryCategoryRepository.save(c);
        });
    }

    private Map<String, Supplier> seedSuppliers() {
        Map<String, Supplier> map = new LinkedHashMap<>();
        String[] names = {
                "Trung Nguyên Legend",
                "Vinamilk",
                "Lavazza",
                "Phúc Long",
                "TH True Milk",
                "Fonterra (Anchor)",
                "Torani",
                "Callebaut",
                "Mật ong Hương Sen",
                "AnEco Packaging",
                "Greenware VN"
        };
        for (String name : names) {
            map.put(name, upsertSupplier(name));
        }
        return map;
    }

    private Supplier upsertSupplier(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Tên NCC không hợp lệ");
        }
        return supplierRepository.findByName(trimmed).orElseGet(() -> {
            Supplier s = new Supplier();
            s.setName(trimmed);
            return supplierRepository.save(s);
        });
    }

    private void seedItems(Map<String, InventoryCategory> cat, Map<String, Supplier> sup) {
        InventoryCategory caPheTra = cat.get("Cà phê & Trà");
        InventoryCategory suaKem = cat.get("Sữa & Kem");
        InventoryCategory nguyenLieu = cat.get("Nguyên liệu");
        InventoryCategory dungCu = cat.get("Dụng cụ");
        InventoryCategory baoBi = cat.get("Bao bì");

        List<Runnable> seeds = List.of(
                // Cà phê & Trà — đơn vị, tồn, min, max, giá vốn/đơn vị (VNĐ)
                () -> upsertItem("Hạt cà phê Robusta rang", caPheTra, sup.get("Trung Nguyên Legend"),
                        "kg", "45", "10", "100", "120000"),
                () -> upsertItem("Hạt Arabica blend", caPheTra, sup.get("Lavazza"),
                        "kg", "12", "15", "40", "185000"),
                () -> upsertItem("Trà đen Ceylon", caPheTra, sup.get("Phúc Long"),
                        "kg", "8", "5", "20", "95000"),
                () -> upsertItem("Trà xanh Thái Nguyên", caPheTra, sup.get("Phúc Long"),
                        "kg", "3", "5", "15", "78000"),

                () -> upsertItem("Sữa tươi tiệt trùng", suaKem, sup.get("TH True Milk"),
                        "lít", "80", "20", "120", "32000"),
                () -> upsertItem("Kem whipping", suaKem, sup.get("Fonterra (Anchor)"),
                        "lít", "15", "8", "40", "145000"),
                () -> upsertItem("Sữa đặc có đường", suaKem, sup.get("Vinamilk"),
                        "lon", "200", "50", "300", "12000"),

                () -> upsertItem("Sirô vanilla", nguyenLieu, sup.get("Torani"),
                        "chai", "6", "4", "15", "185000"),
                () -> upsertItem("Bột cacao", nguyenLieu, sup.get("Callebaut"),
                        "kg", "4", "3", "10", "220000"),
                () -> upsertItem("Đường mật", nguyenLieu, sup.get("Mật ong Hương Sen"),
                        "kg", "25", "10", "50", "55000"),
                () -> upsertItem("Đường cát trắng", nguyenLieu, sup.get("Vinamilk"),
                        "kg", "40", "15", "60", "18000"),

                () -> upsertItem("Ly giấy 12oz", dungCu, sup.get("Greenware VN"),
                        "thùng", "30", "10", "50", "280000"),
                () -> upsertItem("Nắp ly nóng", dungCu, sup.get("Greenware VN"),
                        "thùng", "28", "10", "50", "95000"),
                () -> upsertItem("Ống hút giấy", dungCu, sup.get("AnEco Packaging"),
                        "gói", "100", "40", "150", "45000"),

                () -> upsertItem("Túi giấy Kraft size M", baoBi, sup.get("AnEco Packaging"),
                        "xấp", "15", "20", "40", "120000"),
                () -> upsertItem("Hộp take-away 500ml", baoBi, sup.get("Greenware VN"),
                        "thùng", "8", "10", "25", "195000")
        );

        seeds.forEach(Runnable::run);
    }

    private void upsertItem(
            String name,
            InventoryCategory category,
            Supplier supplier,
            String unit,
            String currentStock,
            String minStock,
            String maxStock,
            String costPerUnit
    ) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("Tên nguyên liệu không hợp lệ");
        }

        InventoryItem item = inventoryItemRepository.findByName(trimmed).orElseGet(InventoryItem::new);
        item.setName(trimmed);
        item.setCategory(category);
        item.setSupplier(supplier);
        item.setUnit(unit != null ? unit.trim() : null);
        item.setCurrentStock(toDecimal(currentStock));
        item.setMinStock(toDecimal(minStock));
        item.setMaxStock(toDecimal(maxStock));
        item.setCostPerUnit(toDecimal(costPerUnit));
        item.setLastUpdated(LocalDate.now());
        applyStockStatus(item);

        inventoryItemRepository.save(item);
    }

    private static BigDecimal toDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    private static void applyStockStatus(InventoryItem item) {
        BigDecimal stock = item.getCurrentStock() != null ? item.getCurrentStock() : BigDecimal.ZERO;
        if (stock.compareTo(BigDecimal.ZERO) <= 0) {
            item.setStatus(InventoryStatus.OUT_OF_STOCK);
            return;
        }
        BigDecimal min = item.getMinStock();
        if (min != null && min.compareTo(BigDecimal.ZERO) > 0 && stock.compareTo(min) <= 0) {
            item.setStatus(InventoryStatus.LOW_STOCK);
            return;
        }
        item.setStatus(InventoryStatus.IN_STOCK);
    }
}