package com.example.QuanLyQuanCafe.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.QuanLyQuanCafe.model.InventoryCategory;
import com.example.QuanLyQuanCafe.model.InventoryImportRequest;
import com.example.QuanLyQuanCafe.model.InventoryItem;
import com.example.QuanLyQuanCafe.model.InventoryStatus;
import com.example.QuanLyQuanCafe.model.InventoryTransaction;
import com.example.QuanLyQuanCafe.model.InventoryTransactionType;
import com.example.QuanLyQuanCafe.model.InventoryUpdateRequest;
import com.example.QuanLyQuanCafe.model.Supplier;
import com.example.QuanLyQuanCafe.repository.InventoryCategoryRepository;
import com.example.QuanLyQuanCafe.repository.InventoryItemRepository;
import com.example.QuanLyQuanCafe.repository.InventoryTransactionRepository;
import com.example.QuanLyQuanCafe.repository.SupplierRepository;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryService(
            InventoryItemRepository inventoryItemRepository,
            InventoryCategoryRepository inventoryCategoryRepository,
            SupplierRepository supplierRepository,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryCategoryRepository = inventoryCategoryRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    public List<InventoryItem> findAll() {
        return inventoryItemRepository.findAll();
    }

    public List<InventoryItem> findByStatus(InventoryStatus status) {
        return inventoryItemRepository.findByStatus(status);
    }

    public List<InventoryCategory> findAllCategoriesOrdered() {
        return inventoryCategoryRepository.findAllByOrderByNameAsc();
    }

    /**
     * Đồng bộ trạng thái với tồn kho và ngưỡng min (nếu có).
     */
    private void applyStockStatus(InventoryItem item) {
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

    @Transactional
    public InventoryItem importStock(InventoryImportRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên nguyên liệu không hợp lệ");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }

        // Tìm hoặc tạo danh mục theo tên đơn giản (nếu có)
        InventoryCategory category = null;
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            category = inventoryCategoryRepository
                    .findByName(request.getCategoryName().trim())
                    .orElseGet(() -> {
                        InventoryCategory c = new InventoryCategory();
                        c.setName(request.getCategoryName().trim());
                        return inventoryCategoryRepository.save(c);
                    });
        }

        // Tìm hoặc tạo nhà cung cấp theo tên đơn giản (nếu có)
        Supplier supplier = null;
        if (request.getSupplierName() != null && !request.getSupplierName().isBlank()) {
            supplier = supplierRepository
                    .findByName(request.getSupplierName().trim())
                    .orElseGet(() -> {
                        Supplier s = new Supplier();
                        s.setName(request.getSupplierName().trim());
                        return supplierRepository.save(s);
                    });
        }

        BigDecimal quantity = request.getQuantity();
        BigDecimal unitCost = request.getUnitCost() != null ? request.getUnitCost() : BigDecimal.ZERO;

        // Tìm item theo tên, nếu chưa có thì tạo mới
        InventoryItem item = inventoryItemRepository
                .findByName(request.getName().trim())
                .orElseGet(() -> {
                    InventoryItem i = new InventoryItem();
                    i.setName(request.getName().trim());
                    i.setCurrentStock(BigDecimal.ZERO);
                    return i;
                });

        BigDecimal stockBefore = item.getCurrentStock() != null ? item.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal stockAfter = stockBefore.add(quantity);

        item.setCategory(category);
        item.setSupplier(supplier);
        item.setUnit(request.getUnit());
        item.setCurrentStock(stockAfter);
        item.setCostPerUnit(unitCost);
        if (request.getMinStock() != null) {
            item.setMinStock(request.getMinStock());
        }
        if (request.getMaxStock() != null) {
            item.setMaxStock(request.getMaxStock());
        }

        LocalDate date = request.getImportDate() != null ? request.getImportDate() : LocalDate.now();
        item.setLastUpdated(date);
        applyStockStatus(item);

        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Lưu giao dịch nhập kho
        InventoryTransaction tx = new InventoryTransaction();
        tx.setItem(savedItem);
        tx.setType(InventoryTransactionType.IMPORT);
        tx.setQuantity(quantity);
        tx.setUnitCost(unitCost);
        tx.setTotalCost(unitCost.multiply(quantity));
        tx.setStockBefore(stockBefore);
        tx.setStockAfter(stockAfter);
        if (request.getNote() != null && !request.getNote().isBlank()) {
            tx.setNote(request.getNote().trim());
        }
        inventoryTransactionRepository.save(tx);

        return savedItem;
    }

    @Transactional
    public InventoryItem updateItem(Long id, InventoryUpdateRequest request) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName().trim());
        }

        // Tìm hoặc tạo danh mục theo tên đơn giản (nếu có)
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            InventoryCategory category = inventoryCategoryRepository
                    .findByName(request.getCategoryName().trim())
                    .orElseGet(() -> {
                        InventoryCategory c = new InventoryCategory();
                        c.setName(request.getCategoryName().trim());
                        return inventoryCategoryRepository.save(c);
                    });
            item.setCategory(category);
        }

        // Tìm hoặc tạo nhà cung cấp theo tên đơn giản (nếu có)
        if (request.getSupplierName() != null && !request.getSupplierName().isBlank()) {
            Supplier supplier = supplierRepository
                    .findByName(request.getSupplierName().trim())
                    .orElseGet(() -> {
                        Supplier s = new Supplier();
                        s.setName(request.getSupplierName().trim());
                        return supplierRepository.save(s);
                    });
            item.setSupplier(supplier);
        }

        if (request.getCurrentStock() != null) {
            item.setCurrentStock(request.getCurrentStock());
        }
        if (request.getUnit() != null) {
            item.setUnit(request.getUnit());
        }
        if (request.getUnitCost() != null) {
            item.setCostPerUnit(request.getUnitCost());
        }
        if (request.getMinStock() != null) {
            item.setMinStock(request.getMinStock());
        }
        if (request.getMaxStock() != null) {
            item.setMaxStock(request.getMaxStock());
        }

        applyStockStatus(item);
        item.setLastUpdated(LocalDate.now());

        return inventoryItemRepository.save(item);
    }
}
