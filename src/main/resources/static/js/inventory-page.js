document.addEventListener('DOMContentLoaded', function () {
      if (!window.bootstrap) {
        console.error('Bootstrap JS chưa tải — trang kho không hoạt động.');
        return;
      }

      var allItems = [];
      var formMode = 'import'; // 'import' | 'edit'
      var importBtn = document.querySelector('.stock-import-btn');
      var importModalEl = document.getElementById('stockImportModal');
      var importModal = null;
      var importItemIdInput = document.getElementById('importItemId');
      var modalTitle = importModalEl ? importModalEl.querySelector('.modal-title') : null;
      var submitButton = importModalEl ? importModalEl.querySelector('button[type="submit"]') : null;

      if (importModalEl) {
        importModal = new bootstrap.Modal(importModalEl);
      }
      var searchInput = document.querySelector('.stock-search input');
      var tableBody = document.querySelector('.stock-table tbody');
      var filterContainer = document.getElementById('stockCategoryFilters');

      var totalItemsEl = document.getElementById('stockTotalItems');
      var inStockEl = document.getElementById('stockInStock');
      var lowOutEl = document.getElementById('stockLowOut');
      var inventoryValueEl = document.getElementById('stockInventoryValue');

      var sortKey = null;
      var sortDir = 'asc';
      var stockTableHead = document.querySelector('.stock-table thead');

      function getActiveCategoryFilter() {
        if (!filterContainer) return 'ALL';
        var active = filterContainer.querySelector('.stock-filter.active');
        return active ? (active.getAttribute('data-category') || 'ALL') : 'ALL';
      }

      function applyTableFilters() {
        if (!tableBody) return;
        var filterCat = getActiveCategoryFilter();
        var keyword = normalizeText(searchInput ? searchInput.value : '');
        var rows = tableBody.querySelectorAll('tr');
        rows.forEach(function (row) {
          if (!row.cells || row.cells.length < 2) {
            return;
          }
          var nameCell = row.cells[0] ? row.cells[0].textContent : '';
          var categoryCell = row.cells[1] ? row.cells[1].textContent.trim() : '';
          var matchCat = filterCat === 'ALL' || normalizeText(categoryCell) === normalizeText(filterCat);
          var matchSearch = !keyword ||
            normalizeText(nameCell).indexOf(keyword) !== -1 ||
            normalizeText(categoryCell).indexOf(keyword) !== -1;
          row.style.display = (matchCat && matchSearch) ? '' : 'none';
        });
      }

      function num(v) {
        var n = v != null ? Number(v) : 0;
        return isFinite(n) ? n : 0;
      }

      function itemLevelPercent(item) {
        var stock = num(item.currentStock);
        var maxStock = num(item.maxStock);
        if (maxStock <= 0) return 0;
        var p = Math.round((stock / maxStock) * 100);
        if (p < 0) p = 0;
        if (p > 100) p = 100;
        return p;
      }

      function itemCategoryStr(item) {
        return item && item.category && item.category.name ? item.category.name : '';
      }

      function itemSupplierStr(item) {
        return item && item.supplier && item.supplier.name ? item.supplier.name : '';
      }

      function statusRank(status) {
        if (status === 'OUT_OF_STOCK') return 0;
        if (status === 'LOW_STOCK') return 1;
        if (status === 'IN_STOCK') return 2;
        return 3;
      }

      function compareItemsForSort(a, b, key) {
        switch (key) {
          case 'name':
            return (a.name || '').localeCompare(b.name || '', 'vi', { sensitivity: 'base' });
          case 'category':
            return itemCategoryStr(a).localeCompare(itemCategoryStr(b), 'vi', { sensitivity: 'base' });
          case 'stock':
            return num(a.currentStock) - num(b.currentStock);
          case 'level':
            return itemLevelPercent(a) - itemLevelPercent(b);
          case 'cost':
            return num(a.costPerUnit) - num(b.costPerUnit);
          case 'supplier':
            return itemSupplierStr(a).localeCompare(itemSupplierStr(b), 'vi', { sensitivity: 'base' });
          case 'status':
            return statusRank(a.status) - statusRank(b.status);
          default:
            return 0;
        }
      }

      function sortItemsForDisplay(list) {
        if (!sortKey || !list || list.length === 0) {
          return list ? list.slice() : [];
        }
        var arr = list.slice();
        arr.sort(function (a, b) {
          var c = compareItemsForSort(a, b, sortKey);
          return sortDir === 'desc' ? -c : c;
        });
        return arr;
      }

      function updateSortHeaders() {
        if (!stockTableHead) return;
        stockTableHead.querySelectorAll('.stock-th-sort').forEach(function (btn) {
          var key = btn.getAttribute('data-sort');
          var icon = btn.querySelector('.stock-sort-icon');
          if (!icon) return;
          if (sortKey === key) {
            icon.className = 'bi stock-sort-icon ' + (sortDir === 'asc' ? 'bi-sort-up' : 'bi-sort-down');
          } else {
            icon.className = 'bi bi-arrow-down-up stock-sort-icon';
          }
        });
      }

      function redrawTable() {
        renderTable(sortItemsForDisplay(allItems));
        updateSortHeaders();
      }

      function renderTable(items) {
        if (!tableBody) return;
        tableBody.innerHTML = '';

        if (!items || items.length === 0) {
          var emptyRow = document.createElement('tr');
          emptyRow.innerHTML = '<td colspan="8" class="text-center text-muted py-4">Chưa có dữ liệu kho hàng</td>';
          tableBody.appendChild(emptyRow);
          return;
        }

        items.forEach(function (item) {
          var name = item.name || '(Chưa đặt tên)';
          var categoryName = item.category && item.category.name ? item.category.name : '-';
          var stock = item.currentStock != null ? Number(item.currentStock) : 0;
          var unit = item.unit || '';
          var maxStock = item.maxStock != null ? Number(item.maxStock) : 0;
          var percent = maxStock > 0 ? Math.round((stock / maxStock) * 100) : 0;
          if (percent < 0) percent = 0;
          if (percent > 100) percent = 100;

          var cost = item.costPerUnit != null ? Number(item.costPerUnit) : 0;
          var priceText = cost > 0 ? cost.toLocaleString('vi-VN') + ' đ' : '-';
          var supplierName = item.supplier && item.supplier.name ? item.supplier.name : '-';

          var statusLabel = '';
          var statusClass = '';
          var levelClass = '';
          if (item.status === 'IN_STOCK') {
            statusLabel = 'Còn hàng';
            statusClass = 'stock-status-in';
          } else if (item.status === 'LOW_STOCK') {
            statusLabel = 'Sắp hết';
            statusClass = 'stock-status-low';
            levelClass = 'stock-level-warning';
          } else if (item.status === 'OUT_OF_STOCK') {
            statusLabel = 'Hết hàng';
            statusClass = 'stock-status-out';
            levelClass = 'stock-level-empty';
          } else {
            statusLabel = item.status || '-';
            statusClass = 'stock-status-in';
          }

          var row = document.createElement('tr');
          row.innerHTML =
            '<td>' + name + '</td>' +
            '<td>' + categoryName + '</td>' +
            '<td><strong>' + stock + '</strong> ' + unit + '</td>' +
            '<td>' +
            '<div class="stock-level">' +
            '<div class="stock-level-bar ' + levelClass + '" style="width: ' + percent + '%;"></div>' +
            '</div>' +
            '<span class="stock-level-text">' + percent + '%</span>' +
            '</td>' +
            '<td>' + priceText + '</td>' +
            '<td>' + supplierName + '</td>' +
            '<td><span class="stock-status-badge ' + statusClass + '">' + statusLabel + '</span></td>' +
            '<td>' +
            '<button class="icon-circle-btn" type="button" aria-label="Chỉnh sửa nguyên liệu" title="Chỉnh sửa nguyên liệu" data-item-id="' + item.id + '">' +
            '<i class="bi bi-pencil"></i>' +
            '</button>' +
            '</td>';
          tableBody.appendChild(row);
        });

        if (items && items.length > 0) {
          applyTableFilters();
        }
      }

      function updateSummary(items) {
        if (!items) items = [];
        var total = items.length;
        var inStockCount = items.filter(function (i) { return i.status === 'IN_STOCK'; }).length;
        var lowCount = items.filter(function (i) { return i.status === 'LOW_STOCK'; }).length;
        var outCount = items.filter(function (i) { return i.status === 'OUT_OF_STOCK'; }).length;
        var totalValue = 0;
        items.forEach(function (i) {
          var qty = i.currentStock != null ? Number(i.currentStock) : 0;
          var cost = i.costPerUnit != null ? Number(i.costPerUnit) : 0;
          totalValue += qty * cost;
        });

        if (totalItemsEl) totalItemsEl.textContent = total;
        if (inStockEl) inStockEl.textContent = inStockCount;
        if (lowOutEl) lowOutEl.textContent = lowCount + ' / ' + outCount;
        if (inventoryValueEl) inventoryValueEl.textContent = totalValue.toLocaleString('vi-VN') + ' đ';

        var subtitleEl = document.getElementById('stockPageSubtitle');
        if (subtitleEl) {
          subtitleEl.textContent = outCount + ' mặt hàng hết hàng · ' + lowCount + ' mặt hàng sắp hết';
        }
      }

      function normalizeText(text) {
        return text ? text.toString().toLowerCase().trim() : '';
      }

      if (stockTableHead) {
        stockTableHead.addEventListener('click', function (e) {
          var btn = e.target.closest('.stock-th-sort');
          if (!btn) return;
          var key = btn.getAttribute('data-sort');
          if (!key) return;
          if (sortKey === key) {
            sortDir = sortDir === 'asc' ? 'desc' : 'asc';
          } else {
            sortKey = key;
            sortDir = 'asc';
          }
          redrawTable();
        });
      }

      function fillImportCategorySelect(categories) {
        var sel = document.getElementById('importCategory');
        if (!sel) return;
        sel.innerHTML = '';
        var opt0 = document.createElement('option');
        opt0.value = '';
        opt0.textContent = 'Chọn danh mục';
        opt0.selected = true;
        sel.appendChild(opt0);
        if (!categories || categories.length === 0) {
          var warn = document.createElement('option');
          warn.value = '';
          warn.disabled = true;
          warn.textContent = 'Chưa có danh mục trong hệ thống';
          sel.appendChild(warn);
          return;
        }
        categories.forEach(function (c) {
          if (!c || !c.name) return;
          var o = document.createElement('option');
          o.value = c.name;
          o.textContent = c.name;
          sel.appendChild(o);
        });
      }

      function renderCategoryFilterButtons(categories) {
        if (!filterContainer) return;
        filterContainer.querySelectorAll('.stock-filter').forEach(function (btn) {
          if (btn.getAttribute('data-category') !== 'ALL') {
            btn.remove();
          }
        });
        (categories || []).forEach(function (c) {
          if (!c || !c.name) return;
          var b = document.createElement('button');
          b.type = 'button';
          b.className = 'stock-filter';
          b.setAttribute('data-category', c.name);
          b.textContent = c.name;
          filterContainer.appendChild(b);
        });
      }

      if (filterContainer && tableBody) {
        filterContainer.addEventListener('click', function (e) {
          var btn = e.target.closest('.stock-filter');
          if (!btn) return;
          filterContainer.querySelectorAll('.stock-filter').forEach(function (b) { b.classList.remove('active'); });
          btn.classList.add('active');
          applyTableFilters();
        });
      }

      function loadInventoryPage() {
        Promise.all([
          fetch('/api/inventory/categories', { credentials: 'same-origin' }).then(function (r) {
            return r.ok ? r.json() : [];
          }),
          fetch('/api/inventory/items', { credentials: 'same-origin' }).then(function (r) {
            if (!r.ok) {
              throw new Error('Lỗi khi tải dữ liệu kho');
            }
            return r.json();
          })
        ])
          .then(function (pair) {
            var cats = Array.isArray(pair[0]) ? pair[0] : [];
            var items = Array.isArray(pair[1]) ? pair[1] : [];
            fillImportCategorySelect(cats);
            renderCategoryFilterButtons(cats);
            allItems = items;
            redrawTable();
            updateSummary(allItems);
          })
          .catch(function (err) {
            console.error(err);
            var subEl = document.getElementById('stockPageSubtitle');
            if (subEl) {
              subEl.textContent = 'Không tải được dữ liệu kho — hãy đăng nhập tài khoản ADMIN.';
            }
            fillImportCategorySelect([]);
            renderCategoryFilterButtons([]);
            sortKey = null;
            sortDir = 'asc';
            renderTable([]);
            updateSortHeaders();
            updateSummary([]);
          });
      }

      // Mở modal nhập hàng
      var importForm = document.getElementById('stockImportForm');

      if (importBtn && importModal) {
        importBtn.addEventListener('click', function () {
          if (importForm) {
            importForm.reset();
          }
          formMode = 'import';
          if (importItemIdInput) importItemIdInput.value = '';
          if (modalTitle) modalTitle.textContent = 'Phiếu nhập hàng';
          if (submitButton) submitButton.textContent = 'Lưu phiếu nhập';
          importModal.show();
        });
      }

      if (importForm && importModal) {
          importForm.addEventListener('submit', function (e) {
            e.preventDefault();

            var name = document.getElementById('importItemName').value.trim();
            var category = document.getElementById('importCategory').value.trim();
            var quantityVal = document.getElementById('importQuantity').value;
            var unit = document.getElementById('importUnit').value.trim();
            var costVal = document.getElementById('importCost').value;
            var supplier = document.getElementById('importSupplier').value.trim();
            var dateVal = document.getElementById('importDate').value;
            var note = document.getElementById('importNote').value.trim();
            var itemId = importItemIdInput ? importItemIdInput.value : '';
            var minStockVal = document.getElementById('importMinStock');
            var maxStockVal = document.getElementById('importMaxStock');
            var minStock = minStockVal && minStockVal.value !== '' ? parseFloat(minStockVal.value) : null;
            var maxStock = maxStockVal && maxStockVal.value !== '' ? parseFloat(maxStockVal.value) : null;
            if (minStock !== null && isNaN(minStock)) minStock = null;
            if (maxStock !== null && isNaN(maxStock)) maxStock = null;

            var quantity = quantityVal ? parseFloat(quantityVal) : 0;
            var unitCost = costVal ? parseFloat(costVal) : 0;

            if (!name) {
              alert('Vui lòng nhập tên nguyên liệu.');
              return;
            }

            if (formMode === 'import') {
              if (!quantityVal || quantity <= 0 || isNaN(quantity)) {
                alert('Vui lòng nhập số lượng nhập lớn hơn 0.');
                return;
              }
            } else if (quantityVal === '' || isNaN(quantity) || quantity < 0) {
              alert('Số lượng tồn kho không hợp lệ.');
              return;
            }

            if (!unit) {
              alert('Vui lòng nhập đơn vị (kg, lít, ...).');
              return;
            }

            if (formMode === 'import') {
              var payload = {
                name: name,
                categoryName: category || null,
                quantity: quantity,
                unit: unit || null,
                unitCost: unitCost,
                supplierName: supplier || null,
                importDate: dateVal || null,
                note: note || null,
                minStock: minStock,
                maxStock: maxStock
              };

              fetch('/api/inventory/import', {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(payload)
              })
                .then(function (res) {
                  if (!res.ok) {
                    return res.text().then(function (t) {
                      throw new Error(t || 'Lỗi khi lưu phiếu nhập');
                    });
                  }
                  return res.json();
                })
                .then(function () {
                  alert('Đã lưu phiếu nhập thành công.');
                  importModal.hide();
                  // Sau khi nhập hàng thành công, có thể reload danh sách kho
                  fetch('/api/inventory/items', { credentials: 'same-origin' })
                    .then(function (res) { return res.json(); })
                    .then(function (items) {
                      allItems = items;
                      redrawTable();
                      updateSummary(allItems);
                    })
                    .catch(function (err) { console.error(err); });
                  fetch('/api/inventory/categories', { credentials: 'same-origin' })
                    .then(function (res) { return res.ok ? res.json() : []; })
                    .then(function (cats) {
                      if (Array.isArray(cats)) {
                        fillImportCategorySelect(cats);
                        renderCategoryFilterButtons(cats);
                      }
                    })
                    .catch(function () { /* ignore */ });
                })
                .catch(function (err) {
                  console.error(err);
                  alert(err && err.message ? err.message : 'Không thể lưu phiếu nhập. Vui lòng thử lại.');
                });
            } else if (formMode === 'edit' && itemId) {
              var updatePayload = {
                name: name,
                categoryName: category || null,
                currentStock: quantity,
                unit: unit || null,
                unitCost: unitCost,
                supplierName: supplier || null,
                minStock: minStock,
                maxStock: maxStock
              };

              fetch('/api/inventory/items/' + encodeURIComponent(itemId), {
                method: 'PUT',
                headers: {
                  'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(updatePayload)
              })
                .then(function (res) {
                  if (!res.ok) {
                    return res.text().then(function (t) {
                      throw new Error(t || 'Lỗi khi lưu chỉnh sửa');
                    });
                  }
                  return res.json();
                })
                .then(function () {
                  alert('Đã lưu thay đổi nguyên liệu.');
                  importModal.hide();
                  fetch('/api/inventory/items', { credentials: 'same-origin' })
                    .then(function (res) { return res.json(); })
                    .then(function (items) {
                      allItems = items;
                      redrawTable();
                      updateSummary(allItems);
                    })
                    .catch(function (err) { console.error(err); });
                  fetch('/api/inventory/categories', { credentials: 'same-origin' })
                    .then(function (res) { return res.ok ? res.json() : []; })
                    .then(function (cats) {
                      if (Array.isArray(cats)) {
                        fillImportCategorySelect(cats);
                        renderCategoryFilterButtons(cats);
                      }
                    })
                    .catch(function () { /* ignore */ });
                })
                .catch(function (err) {
                  console.error(err);
                  alert(err && err.message ? err.message : 'Không thể lưu chỉnh sửa. Vui lòng thử lại.');
                });
            }
          });
      }

      // Bắt sự kiện click nút chỉnh sửa trong bảng (event delegation)
      if (tableBody && importModal) {
        tableBody.addEventListener('click', function (e) {
          var btn = e.target.closest('.icon-circle-btn');
          if (!btn) return;
          var itemId = btn.getAttribute('data-item-id');
          if (!itemId) return;

          var item = allItems.find(function (i) { return String(i.id) === String(itemId); });
          if (!item) return;

          // Prefill form với dữ liệu item
          var nameInput = document.getElementById('importItemName');
          var categorySelect = document.getElementById('importCategory');
          var quantityInput = document.getElementById('importQuantity');
          var unitInput = document.getElementById('importUnit');
          var costInput = document.getElementById('importCost');
          var supplierInput = document.getElementById('importSupplier');
          var dateInput = document.getElementById('importDate');
          var minStockInput = document.getElementById('importMinStock');
          var maxStockInput = document.getElementById('importMaxStock');

          if (nameInput) nameInput.value = item.name || '';
          if (categorySelect) {
            var catName = item.category && item.category.name ? item.category.name : '';
            var found = false;
            Array.from(categorySelect.options).forEach(function (opt) {
              if (opt.text.trim().toLowerCase() === catName.trim().toLowerCase()) {
                categorySelect.value = opt.value;
                found = true;
              }
            });
            if (!found) categorySelect.value = '';
          }
          if (quantityInput) quantityInput.value = item.currentStock != null ? Number(item.currentStock) : '';
          if (unitInput) unitInput.value = item.unit || '';
          if (costInput) costInput.value = item.costPerUnit != null ? Number(item.costPerUnit) : '';
          if (supplierInput) supplierInput.value = item.supplier && item.supplier.name ? item.supplier.name : '';
          if (minStockInput) minStockInput.value = item.minStock != null ? Number(item.minStock) : '';
          if (maxStockInput) maxStockInput.value = item.maxStock != null ? Number(item.maxStock) : '';
          if (dateInput) {
            var lu = item.lastUpdated;
            if (lu && typeof lu === 'string') {
              dateInput.value = lu.length >= 10 ? lu.slice(0, 10) : lu;
            } else if (lu && Array.isArray(lu) && lu.length >= 3) {
              var y = String(lu[0]);
              var m = String(lu[1]).padStart(2, '0');
              var d = String(lu[2]).padStart(2, '0');
              dateInput.value = y + '-' + m + '-' + d;
            } else {
              dateInput.value = '';
            }
          }

          formMode = 'edit';
          if (importItemIdInput) importItemIdInput.value = item.id;
          if (modalTitle) modalTitle.textContent = 'Chỉnh sửa nguyên liệu';
          if (submitButton) submitButton.textContent = 'Lưu thay đổi';

          importModal.show();
        });
      }

      if (tableBody) {
        loadInventoryPage();
      }

      if (searchInput && tableBody) {
        searchInput.addEventListener('input', function () {
          applyTableFilters();
        });
      }

});
