document.addEventListener('DOMContentLoaded', function () {
      var BS = typeof bootstrap !== 'undefined' ? bootstrap : (typeof window !== 'undefined' ? window.bootstrap : null);
      if (!BS || typeof BS.Modal !== 'function') {
        console.error('[order-page] Bootstrap Modal không khả dụng — kiểm tra CDN / thứ tự script.');
      }

      var createBtn = document.querySelector('.order-create-btn');
      var createModalEl = document.getElementById('createOrderModal');
      var orderPaymentQrModalEl = document.getElementById('orderPaymentQrModal');

      if (createModalEl && BS && typeof BS.Modal === 'function') {
        var createModal = new BS.Modal(createModalEl);
        var orderPaymentQrModal = orderPaymentQrModalEl ? new BS.Modal(orderPaymentQrModalEl) : null;
        var createForm = document.getElementById('createOrderForm');

        // Elements for auto time, customer and items
        var orderTimeInput = document.getElementById('orderTime');
        var orderStatusSelect = document.getElementById('orderStatus');
        var orderTotalAmountInput = document.getElementById('orderTotalAmount');
        var menuSearchInput = document.getElementById('orderMenuSearchInput');
        var menuSearchResults = document.getElementById('orderMenuSearchResults');
        var quantityInput = document.getElementById('orderItemQuantity');
        var orderItemNoteInput = document.getElementById('orderItemNoteInput');
        var addItemBtn = document.getElementById('addOrderItemBtn');
        var itemsBody = document.getElementById('orderItemsBody');
        var itemsEmptyRow = document.getElementById('orderItemsEmptyRow');
        var customerNameInput = document.getElementById('orderCustomerName');
        var customerPhoneInput = document.getElementById('orderCustomerPhone');
        var orderTypeSelectEl = document.getElementById('orderType');
        var orderNoteInput = document.getElementById('orderNote');
        var selectedTableField = document.getElementById('selectedTableField');
        var selectedTableDisplay = document.getElementById('selectedTableDisplay');
        var tablePickerSection = document.getElementById('tablePickerSection');
        var tablePickerGrid = document.getElementById('tablePickerGrid');
        var reloadTablesBtn = document.getElementById('reloadTablesBtn');
        var tablePickerHint = document.getElementById('tablePickerHint');

        var selectedTableNumber = null;

        // QR thanh toán
        var orderPaymentMethodSelect = document.getElementById('orderPaymentMethod');
        var orderPaymentQrSection = document.getElementById('orderPaymentQrSection');
        var orderPaymentQrImage = document.getElementById('orderPaymentQrImage');
        var orderPaymentAmountText = document.getElementById('orderPaymentAmountText');
        var orderPaymentMethodText = document.getElementById('orderPaymentMethodText');
        var paymentSettings = null;

        // Elements for QR modal sau khi tạo đơn
        var qrOrderImage = document.getElementById('qrOrderImage');
        var qrOrderAmountText = document.getElementById('qrOrderAmountText');
        var qrOrderMethodText = document.getElementById('qrOrderMethodText');
        var qrOrderInfoText = document.getElementById('qrOrderInfoText');

        var modalTitle = document.querySelector('#createOrderModal .modal-title');
        var editingOrderCode = null;

        var currentOrderItems = []; // {id, name, price, quantity, note}

        function lookupCustomerByPhone() {
          if (!customerPhoneInput || !customerNameInput) return;
          var rawPhone = customerPhoneInput.value.trim();
          if (!rawPhone) return;

          fetch('/api/customers/phone/' + encodeURIComponent(rawPhone))
            .then(function (res) {
              if (!res.ok) {
                return null;
              }
              return res.json();
            })
            .then(function (customer) {
              if (customer && customer.name && !customerNameInput.value.trim()) {
                customerNameInput.value = customer.name;
              }
            })
            .catch(function () { /* bỏ qua lỗi tra cứu */ });
        }

        if (customerPhoneInput) {
          customerPhoneInput.addEventListener('blur', lookupCustomerByPhone);
          customerPhoneInput.addEventListener('change', lookupCustomerByPhone);
        }

        function formatCurrencyVND(amount) {
          var num = Number(amount || 0);
          if (!isFinite(num) || num <= 0) {
            return '0 đ';
          }
          return num.toLocaleString('vi-VN') + ' đ';
        }

        function setCurrentTime() {
          if (!orderTimeInput) return;
          var now = new Date();
          var hours = String(now.getHours()).padStart(2, '0');
          var minutes = String(now.getMinutes()).padStart(2, '0');
          orderTimeInput.value = hours + ':' + minutes;
        }

        function loadPaymentSettingsForOrder() {
          fetch('/api/settings/payment')
            .then(function (response) {
              if (!response.ok) {
                return null;
              }
              return response.json();
            })
            .then(function (data) {
              paymentSettings = data || null;
              updateOrderPaymentQr();
            })
            .catch(function () {
              paymentSettings = null;
              updateOrderPaymentQr();
            });
        }

        function updateOrderPaymentQr() {
          if (!orderPaymentQrSection || !orderPaymentAmountText || !orderPaymentMethodSelect || !orderTotalAmountInput) {
            return;
          }

          var method = orderPaymentMethodSelect.value;
          var rawAmount = Number(orderTotalAmountInput.value || 0);

          // Chỉ hiển thị khi có số tiền và phương thức là Chuyển khoản
          if (!rawAmount || rawAmount <= 0 || (method !== 'BANK_TRANSFER')) {
            orderPaymentQrSection.style.display = 'none';
            if (orderPaymentQrImage) {
              orderPaymentQrImage.style.display = 'none';
            }
            return;
          }

          if (!paymentSettings) {
            orderPaymentQrSection.style.display = 'none';
            return;
          }

          var amount = Math.round(rawAmount);
          orderPaymentAmountText.textContent = 'Số tiền: ' + formatCurrencyVND(amount);
          if (orderPaymentMethodText) {
            orderPaymentMethodText.textContent = 'Phương thức: Chuyển khoản ngân hàng';
          }

          var qrUrl = null;

          if (method === 'BANK_TRANSFER') {
            var bankCode = paymentSettings.bankCode || null;
            var bankAccount = paymentSettings.bankAccount || null;
            var bankOwnerName = paymentSettings.bankOwnerName || null;

            // Nếu backend chưa lưu bankCode, suy ra từ bankName giống trang Cài đặt
            if (!bankCode && paymentSettings.bankName) {
              var bankCodeMap = {
                'Vietcombank (VCB)': 'vietcombank',
                'ACB': 'acb',
                'Techcombank': 'techcombank',
                'VPBank': 'vpbank',
                'BIDV': 'bidv'
              };
              bankCode = bankCodeMap[paymentSettings.bankName] || null;
            }

            if (!bankCode || !bankAccount || !bankOwnerName || paymentSettings.bankEnabled === false) {
              orderPaymentQrSection.style.display = 'none';
              if (orderPaymentQrImage) {
                orderPaymentQrImage.style.display = 'none';
              }
              return;
            }

            var addInfo = 'Thanh toan don tai quan ca phe';
            qrUrl = 'https://img.vietqr.io/image/'
              + bankCode + '-' + encodeURIComponent(bankAccount)
              + '-compact2.png?amount=' + amount
              + '&addInfo=' + encodeURIComponent(addInfo)
              + '&accountName=' + encodeURIComponent(bankOwnerName);
          }

          if (!qrUrl || !orderPaymentQrImage) {
            orderPaymentQrSection.style.display = 'none';
            return;
          }

          orderPaymentQrImage.src = qrUrl;
          orderPaymentQrImage.style.display = 'block';
          orderPaymentQrSection.style.display = 'block';
        }

        function buildQrUrlForOrder(order) {
          if (!order || !paymentSettings) {
            return null;
          }

          var total = order.total != null ? Number(order.total) : 0;
          if (!total || !isFinite(total) || total <= 0) {
            return null;
          }

          var method = order.paymentMethod || 'CASH';

          if (method === 'BANK_TRANSFER') {
            var bankCode = paymentSettings.bankCode || null;
            var bankAccount = paymentSettings.bankAccount || null;
            var bankOwnerName = paymentSettings.bankOwnerName || null;

            if (!bankCode && paymentSettings.bankName) {
              var bankCodeMap = {
                'Vietcombank (VCB)': 'vietcombank',
                'ACB': 'acb',
                'Techcombank': 'techcombank',
                'VPBank': 'vpbank',
                'BIDV': 'bidv'
              };
              bankCode = bankCodeMap[paymentSettings.bankName] || null;
            }

            if (!bankCode || !bankAccount || !bankOwnerName || paymentSettings.bankEnabled === false) {
              return null;
            }

            var addInfo = 'Thanh toan don ' + (order.orderCode || 'tai quan ca phe');
            return 'https://img.vietqr.io/image/'
              + bankCode + '-' + encodeURIComponent(bankAccount)
              + '-compact2.png?amount=' + Math.round(total)
              + '&addInfo=' + encodeURIComponent(addInfo)
              + '&accountName=' + encodeURIComponent(bankOwnerName);
          }

          return null;
        }

        function showQrModalForOrder(order) {
          if (!orderPaymentQrModal || !qrOrderImage || !qrOrderAmountText) {
            return;
          }

          var method = order.paymentMethod || 'CASH';
          if (method !== 'BANK_TRANSFER') {
            // Đơn thanh toán tiền mặt/thẻ thì chỉ ẩn modal QR
            return;
          }

          var qrUrl = buildQrUrlForOrder(order);
          if (!qrUrl) {
            return;
          }

          var amount = order.total != null ? Number(order.total) : 0;
          qrOrderAmountText.textContent = 'Số tiền: ' + formatCurrencyVND(amount);
          if (qrOrderMethodText) {
            qrOrderMethodText.textContent = 'Phương thức: Chuyển khoản ngân hàng';
          }

          if (qrOrderInfoText) {
            qrOrderInfoText.textContent = 'Đơn ' + (order.orderCode || '') + ' - vui lòng quét QR để thanh toán.';
          }

          qrOrderImage.src = qrUrl;
          qrOrderImage.style.display = 'block';
          orderPaymentQrModal.show();
        }

        var menuSearchTimer = null;
        var lastMenuQuery = '';
        var latestMenuAbort = null;
        var menuResultsItems = [];
        var menuActiveIndex = -1;
        var selectedMenuItem = null; // item đã chọn, chờ bấm "Thêm"

        function hideMenuResults() {
          if (!menuSearchResults) return;
          menuSearchResults.style.display = 'none';
          menuSearchResults.innerHTML = '';
          menuResultsItems = [];
          menuActiveIndex = -1;
        }

        function setMenuActiveIndex(nextIndex) {
          if (!menuSearchResults) return;
          var buttons = menuSearchResults.querySelectorAll('button.list-group-item');
          if (!buttons || buttons.length === 0) {
            menuActiveIndex = -1;
            return;
          }
          var max = buttons.length - 1;
          var idx = nextIndex;
          if (idx < 0) idx = 0;
          if (idx > max) idx = max;
          menuActiveIndex = idx;

          buttons.forEach(function (btn, i) {
            btn.classList.toggle('active', i === menuActiveIndex);
          });

          var activeBtn = buttons[menuActiveIndex];
          if (activeBtn && typeof activeBtn.scrollIntoView === 'function') {
            activeBtn.scrollIntoView({ block: 'nearest' });
          }
        }

        function selectMenuItem(item) {
          if (!item || item.id == null) return;
          selectedMenuItem = item;
          if (menuSearchInput) {
            menuSearchInput.value = item.name || '';
          }
          hideMenuResults();
          if (quantityInput) {
            quantityInput.focus();
            quantityInput.select();
          }
        }

        function commitSelectedMenuItem() {
          var item = selectedMenuItem;
          if (!item || item.id == null) {
            // nếu chưa chọn item nhưng đang mở dropdown, lấy item đang highlight
            if (menuSearchResults && menuSearchResults.style.display === 'block' && menuResultsItems.length > 0) {
              var idx = menuActiveIndex >= 0 ? menuActiveIndex : 0;
              item = menuResultsItems[idx];
            }
          }
          if (!item || item.id == null) return;

          var qty = parseInt(quantityInput ? quantityInput.value : '1', 10);
          if (!qty || qty < 1) qty = 1;
          var lineNote = orderItemNoteInput ? (orderItemNoteInput.value || '') : '';
          lineNote = String(lineNote).trim();

          var price = Number(item.price || 0);
          if (!isFinite(price) || price < 0) price = 0;

          var existing = currentOrderItems.find(function (row) { return String(row.id) === String(item.id); });
          if (existing) {
            existing.quantity += qty;
            if (lineNote && (!existing.note || !String(existing.note).trim())) {
              existing.note = lineNote;
            }
          } else {
            currentOrderItems.push({
              id: String(item.id),
              name: item.name,
              price: price,
              quantity: qty,
              note: lineNote || ''
            });
          }

          renderOrderItems();
          if (menuSearchInput) {
            // clear để chọn món tiếp theo
            menuSearchInput.value = '';
            menuSearchInput.focus();
          }
          if (orderItemNoteInput) {
            orderItemNoteInput.value = '';
          }
          if (quantityInput) {
            quantityInput.value = '1';
          }
          selectedMenuItem = null;
          hideMenuResults();
        }

        function showMenuResults(items) {
          if (!menuSearchResults) return;
          menuSearchResults.innerHTML = '';
          menuResultsItems = Array.isArray(items) ? items.filter(function (x) { return x && x.id != null; }) : [];
          menuActiveIndex = -1;

          if (!menuResultsItems.length) {
            var empty = document.createElement('div');
            empty.className = 'list-group-item text-muted small';
            empty.textContent = 'Không tìm thấy món phù hợp.';
            menuSearchResults.appendChild(empty);
            menuSearchResults.style.display = 'block';
            return;
          }

          menuResultsItems.forEach(function (item, index) {
            var a = document.createElement('button');
            a.type = 'button';
            a.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
            a.dataset.index = String(index);

            var left = document.createElement('div');
            left.innerHTML = '<div class="fw-semibold">' + (item.name || '') + '</div>';
            var cat = item.category && item.category.name ? item.category.name : '';
            if (cat) {
              var small = document.createElement('div');
              small.className = 'small text-muted';
              small.textContent = cat;
              left.appendChild(small);
            }

            var price = Number(item.price || 0);
            var right = document.createElement('div');
            right.className = 'small text-muted';
            right.textContent = price > 0 ? price.toLocaleString('vi-VN') + ' đ' : '';

            a.appendChild(left);
            a.appendChild(right);

            a.addEventListener('click', function () {
              // chỉ chọn món, không auto thêm
              selectMenuItem(item);
            });

            a.addEventListener('mousemove', function () {
              setMenuActiveIndex(index);
            });

            menuSearchResults.appendChild(a);
          });

          menuSearchResults.style.display = 'block';
          // default highlight first for Enter
          setMenuActiveIndex(0);
        }

        function searchMenu(query) {
          if (!query || query.trim().length < 2) {
            hideMenuResults();
            return;
          }
          var q = query.trim();
          lastMenuQuery = q;

          try {
            if (latestMenuAbort) latestMenuAbort.abort();
          } catch (e) { /* ignore */ }

          latestMenuAbort = new AbortController();

          fetch('/api/menu/search?q=' + encodeURIComponent(q), { signal: latestMenuAbort.signal })
            .then(function (res) { return res.ok ? res.json() : []; })
            .then(function (items) {
              // tránh race condition: chỉ render nếu query vẫn là query mới nhất
              if (lastMenuQuery !== q) return;
              showMenuResults(items);
            })
            .catch(function (err) {
              if (err && err.name === 'AbortError') return;
              hideMenuResults();
            });
        }

        function toggleTablePickerByType() {
          if (!tablePickerSection || !orderTypeSelectEl) return;
          var type = orderTypeSelectEl.value || 'DINE_IN';
          tablePickerSection.style.display = (type === 'DINE_IN') ? 'block' : 'none';
          if (selectedTableField) {
            selectedTableField.style.display = (type === 'DINE_IN') ? 'block' : 'none';
          }
          if (type !== 'DINE_IN') {
            selectedTableNumber = null;
            if (selectedTableDisplay) selectedTableDisplay.value = '';
          }
        }

        function renderTablesStatus(list) {
          if (!tablePickerGrid) return;
          tablePickerGrid.innerHTML = '';

          if (!Array.isArray(list) || list.length === 0) {
            var empty = document.createElement('div');
            empty.className = 'text-muted small';
            empty.textContent = 'Không có dữ liệu bàn.';
            tablePickerGrid.appendChild(empty);
            return;
          }

          list.forEach(function (t) {
            if (!t || t.number == null) return;
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'table-btn';
            btn.textContent = String(t.number);

            var status = t.status || 'AVAILABLE';
            if (status === 'OCCUPIED') btn.classList.add('table-occupied');
            else if (status === 'RESERVED') btn.classList.add('table-reserved');
            else btn.classList.add('table-available');

            if (selectedTableNumber != null && Number(selectedTableNumber) === Number(t.number)) {
              btn.classList.add('table-selected');
            }

            btn.addEventListener('click', function () {
              selectedTableNumber = Number(t.number);
              if (selectedTableDisplay) {
                selectedTableDisplay.value = 'Bàn ' + selectedTableNumber;
              }

              // highlight selected
              Array.prototype.forEach.call(tablePickerGrid.querySelectorAll('.table-btn'), function (b) {
                b.classList.remove('table-selected');
              });
              btn.classList.add('table-selected');

              // chọn bàn chỉ highlight; ghi chú đơn nhập ở ô riêng

              if (tablePickerHint) {
                if (status === 'OCCUPIED') {
                  tablePickerHint.textContent = 'Bàn ' + selectedTableNumber + ' đang có khách. Nếu vẫn tạo đơn mới, có thể bị trùng đơn theo bàn.';
                } else if (status === 'RESERVED') {
                  tablePickerHint.textContent = 'Bàn ' + selectedTableNumber + ' đang được giữ theo đặt bàn (cửa sổ 15 phút).';
                } else {
                  tablePickerHint.textContent = 'Đã chọn bàn ' + selectedTableNumber + '.';
                }
              }
            });

            tablePickerGrid.appendChild(btn);
          });
        }

        function loadTableStatus() {
          if (!tablePickerGrid) return;
          tablePickerGrid.innerHTML = '<div class="text-muted small">Đang tải danh sách bàn...</div>';
          fetch('/api/tables/status?total=20')
            .then(function (res) { return res.ok ? res.json() : []; })
            .then(function (data) { renderTablesStatus(data); })
            .catch(function () {
              tablePickerGrid.innerHTML = '<div class="text-muted small">Không tải được trạng thái bàn.</div>';
            });
        }

        function renderOrderItems() {
          if (!itemsBody) return;

          // Clear body
          itemsBody.innerHTML = '';

          if (!currentOrderItems.length) {
            if (itemsEmptyRow) {
              itemsBody.appendChild(itemsEmptyRow);
            }
            if (orderTotalAmountInput) {
              orderTotalAmountInput.value = '';
            }
            return;
          }

          var total = 0;

          currentOrderItems.forEach(function (row, index) {
            var tr = document.createElement('tr');

            var tdName = document.createElement('td');
            tdName.textContent = row.name;

            var tdQty = document.createElement('td');
            tdQty.className = 'text-center';
            var qtyInput = document.createElement('input');
            qtyInput.type = 'number';
            qtyInput.min = '1';
            qtyInput.className = 'form-control form-control-sm text-center';
            qtyInput.value = String(row.quantity || 1);
            qtyInput.addEventListener('input', function () {
              var v = parseInt(qtyInput.value, 10);
              if (!v || v < 1) v = 1;
              currentOrderItems[index].quantity = v;
              renderOrderItems();
            });
            tdQty.appendChild(qtyInput);

            var tdNote = document.createElement('td');
            var noteInput = document.createElement('input');
            noteInput.type = 'text';
            noteInput.className = 'form-control form-control-sm';
            noteInput.placeholder = 'Ghi chú món...';
            noteInput.value = row.note || '';
            noteInput.addEventListener('input', function () {
              currentOrderItems[index].note = noteInput.value;
            });
            tdNote.appendChild(noteInput);

            var tdPrice = document.createElement('td');
            tdPrice.className = 'text-end';
            tdPrice.textContent = row.price.toLocaleString('vi-VN') + ' đ';

            var lineTotal = row.price * row.quantity;
            total += lineTotal;

            var tdLineTotal = document.createElement('td');
            tdLineTotal.className = 'text-end';
            tdLineTotal.textContent = lineTotal.toLocaleString('vi-VN') + ' đ';

            var tdRemove = document.createElement('td');
            var removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.className = 'btn btn-sm btn-link text-danger p-0';
            removeBtn.innerHTML = '<i class="bi bi-x-circle"></i>';
            removeBtn.addEventListener('click', function () {
              currentOrderItems.splice(index, 1);
              renderOrderItems();
            });
            tdRemove.appendChild(removeBtn);

            tr.appendChild(tdName);
            tr.appendChild(tdQty);
            tr.appendChild(tdNote);
            tr.appendChild(tdPrice);
            tr.appendChild(tdLineTotal);
            tr.appendChild(tdRemove);

            itemsBody.appendChild(tr);
          });

          if (orderTotalAmountInput) {
            orderTotalAmountInput.value = total;
          }

          // Cập nhật QR nếu đang chọn phương thức phù hợp
          updateOrderPaymentQr();
        }

        function addSelectedItem() {
          // Đã chuyển sang search autocomplete -> không dùng nữa
        }

        if (createBtn) {
          createBtn.addEventListener('click', function () {
            editingOrderCode = null;
            if (modalTitle) modalTitle.textContent = 'Tạo đơn hàng mới';

            if (createForm) {
              createForm.reset();
            }
            selectedTableNumber = null;
            if (tablePickerHint) tablePickerHint.textContent = '';
            // Reset items state
            currentOrderItems = [];
            renderOrderItems();

            // Auto-set current time and default status
            setCurrentTime();
            if (orderStatusSelect) {
              orderStatusSelect.value = 'PENDING';
            }

            hideMenuResults();
            if (menuSearchInput) menuSearchInput.value = '';

            // Tải cấu hình VietQR (ngân hàng) cho đơn chuyển khoản
            loadPaymentSettingsForOrder();

            toggleTablePickerByType();
            loadTableStatus();

            // Ẩn QR lúc mới mở
            updateOrderPaymentQr();
            createModal.show();
          });
        }

        // Xử lý nút Sửa đơn
        var editBtns = document.querySelectorAll('.edit-order-btn');
        editBtns.forEach(function (btn) {
          btn.addEventListener('click', function () {
            var code = this.getAttribute('data-code');
            if (!code) return;

            editingOrderCode = code;
            if (modalTitle) modalTitle.textContent = 'Cập nhật đơn hàng ' + code;

            if (createForm) createForm.reset();
            currentOrderItems = [];
            renderOrderItems();

            hideMenuResults();
            loadPaymentSettingsForOrder();

            // Lấy thông tin đơn hàng
            fetch('/api/orders/' + code)
              .then(function (res) { return res.json(); })
              .then(function (order) {
                if (document.getElementById('orderCode')) document.getElementById('orderCode').value = order.orderCode || '';
                if (document.getElementById('orderTime') && order.createdAt) {
                  var t = new Date(order.createdAt);
                  document.getElementById('orderTime').value = String(t.getHours()).padStart(2, '0') + ':' + String(t.getMinutes()).padStart(2, '0');
                }
                if (document.getElementById('orderStatus')) document.getElementById('orderStatus').value = order.status || 'PENDING';
                if (document.getElementById('orderCustomerName')) document.getElementById('orderCustomerName').value = order.customerName || '';
                if (document.getElementById('orderCustomerPhone')) document.getElementById('orderCustomerPhone').value = order.customerPhone || '';
                if (document.getElementById('orderType')) document.getElementById('orderType').value = order.type || 'DINE_IN';
                if (document.getElementById('orderNote')) document.getElementById('orderNote').value = order.tableName || '';
                if (document.getElementById('orderPaymentMethod')) document.getElementById('orderPaymentMethod').value = order.paymentMethod || 'CASH';

                // cố gắng parse số bàn từ tableName để highlight
                try {
                  var tn = order.tableName || '';
                  var m = tn.match(/(\d+)/);
                  selectedTableNumber = m ? Number(m[1]) : null;
                } catch (e) {
                  selectedTableNumber = null;
                }

                toggleTablePickerByType();
                loadTableStatus();

                // Lấy thông tin các món
                return fetch('/api/orders/' + code + '/items');
              })
              .then(function (res) { return res.json(); })
              .then(function (items) {
                if (Array.isArray(items)) {
                  currentOrderItems = items.map(function (item) {
                    return {
                      id: item.menuItemId ? String(item.menuItemId) : (item.menuItem ? String(item.menuItem.id) : null),
                      name: item.itemName,
                      price: item.itemPrice,
                      quantity: item.quantity,
                      note: item.note || ''
                    };
                  }).filter(function (x) { return x.id != null; });
                }
                renderOrderItems();
                createModal.show();
              })
              .catch(function (err) {
                alert('Có lỗi khi tải thông tin đơn hàng: ' + err);
              });
          });
        });

        // nút "Thêm" đã được bind để commitSelectedMenuItem()

        if (menuSearchInput) {
          menuSearchInput.addEventListener('input', function () {
            var val = menuSearchInput.value || '';
            if (menuSearchTimer) clearTimeout(menuSearchTimer);
            menuSearchTimer = setTimeout(function () {
              searchMenu(val);
            }, 300);
          });

          menuSearchInput.addEventListener('focus', function () {
            var val = menuSearchInput.value || '';
            if (val.trim().length >= 2) {
              searchMenu(val);
            }
          });

          menuSearchInput.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
              hideMenuResults();
              return;
            }

            if (e.key === 'ArrowDown') {
              if (menuSearchResults && menuSearchResults.style.display === 'block') {
                e.preventDefault();
                setMenuActiveIndex(menuActiveIndex + 1);
              }
              return;
            }

            if (e.key === 'ArrowUp') {
              if (menuSearchResults && menuSearchResults.style.display === 'block') {
                e.preventDefault();
                setMenuActiveIndex(menuActiveIndex - 1);
              }
              return;
            }

            if (e.key === 'Enter') {
              // Enter chỉ chọn món đầu tiên / món đang highlight (không auto thêm)
              if (menuSearchResults && menuSearchResults.style.display === 'block' && menuResultsItems.length > 0) {
                e.preventDefault();
                var idx = menuActiveIndex >= 0 ? menuActiveIndex : 0;
                var item = menuResultsItems[idx];
                selectMenuItem(item);
              }
            }
          });
        }

        if (addItemBtn) {
          addItemBtn.addEventListener('click', function () {
            commitSelectedMenuItem();
          });
        }

        if (quantityInput) {
          quantityInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
              e.preventDefault();
              commitSelectedMenuItem();
            }
          });
        }

        if (orderItemNoteInput) {
          orderItemNoteInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
              e.preventDefault();
              commitSelectedMenuItem();
            }
          });
        }

        document.addEventListener('click', function (e) {
          if (!menuSearchResults || !menuSearchInput) return;
          if (menuSearchResults.contains(e.target)) return;
          if (menuSearchInput.contains(e.target)) return;
          hideMenuResults();
        });

        if (createForm) {
          createForm.addEventListener('submit', function (e) {
            e.preventDefault();

            if (!currentOrderItems.length) {
              alert('Vui lòng chọn ít nhất 1 món cho đơn hàng.');
              return;
            }

            // customerNameInput & customerPhoneInput đã được khai báo phía trên

            var payload = {
              customerName: customerNameInput ? customerNameInput.value.trim() : null,
              customerPhone: customerPhoneInput ? customerPhoneInput.value.trim() : null,
              type: orderTypeSelectEl ? orderTypeSelectEl.value : 'DINE_IN',
              tableNote: (function () {
                var type = orderTypeSelectEl ? orderTypeSelectEl.value : 'DINE_IN';
                var note = orderNoteInput ? orderNoteInput.value.trim() : '';
                if (type === 'DINE_IN') {
                  // vẫn lưu tableName chung field tableName: "Bàn X · ghi chú"
                  var tablePart = selectedTableNumber ? ('Bàn ' + selectedTableNumber) : '';
                  if (tablePart && note) return tablePart + ' · ' + note;
                  if (tablePart) return tablePart;
                  return note || null;
                }
                return note || null;
              })(),
              status: orderStatusSelect ? orderStatusSelect.value : 'PENDING',
              paymentMethod: orderPaymentMethodSelect ? orderPaymentMethodSelect.value : 'CASH',
              items: currentOrderItems.map(function (row) {
                return {
                  menuItemId: row.id,
                  quantity: row.quantity,
                  note: row.note ? row.note.trim() : null
                };
              })
            };

            var fetchUrl = editingOrderCode ? '/api/orders/' + editingOrderCode : '/api/orders';
            var fetchMethod = editingOrderCode ? 'PUT' : 'POST';

            fetch(fetchUrl, {
              method: fetchMethod,
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(payload)
            })
              .then(function (res) {
                if (!res.ok) {
                  throw new Error('Request failed');
                }
                return res.json();
              })
              .then(function (data) {
                // Ẩn form tạo đơn
                createModal.hide();

                // Hiển thị modal QR nếu đơn dùng chuyển khoản ngân hàng
                try {
                  // Nếu chưa có paymentSettings (lỡ lỗi mạng trước đó) thì load lại rồi hiển thị
                  if (!paymentSettings) {
                    loadPaymentSettingsForOrder();
                    setTimeout(function () {
                      showQrModalForOrder(data);
                    }, 400);
                  } else {
                    showQrModalForOrder(data);
                  }
                } catch (err) {
                  // Nếu có lỗi hiển thị QR thì vẫn báo thành công bằng alert
                  alert('Đã lưu đơn hàng thành công với mã: ' + (data.orderCode || '#ORD-MỚI'));
                }

                // Tải lại trang sau 1 giây báo thành công (hoặc sau khi họ đóng QR/alert)
                // Cải thiện UI: Reload sau khi đóng modal để thấy dòng mới cập nhật
                document.getElementById('createOrderModal').addEventListener('hidden.bs.modal', function () {
                  if (!orderPaymentQrModalEl || !orderPaymentQrModalEl.classList.contains('show')) {
                    window.location.reload();
                  }
                });
                if (orderPaymentQrModalEl) {
                  orderPaymentQrModalEl.addEventListener('hidden.bs.modal', function () {
                    window.location.reload();
                  });
                }

                // Tránh trường hợp không có QR modal nào hiện ra, ta reload trực tiếp nếu là CASH
                if (payload.paymentMethod !== 'BANK_TRANSFER') {
                  setTimeout(function () { window.location.reload(); }, 300);
                }

              })
              .catch(function () {
                alert('Có lỗi xảy ra khi lưu đơn hàng. Vui lòng thử lại.');
              });
          });
        }

        if (orderPaymentMethodSelect) {
          orderPaymentMethodSelect.addEventListener('change', function () {
            updateOrderPaymentQr();
          });
        }

        if (orderTypeSelectEl) {
          orderTypeSelectEl.addEventListener('change', function () {
            toggleTablePickerByType();
            if (orderTypeSelectEl.value === 'DINE_IN') {
              loadTableStatus();
            } else {
              selectedTableNumber = null;
              if (tablePickerHint) tablePickerHint.textContent = '';
            }
          });
        }

        if (reloadTablesBtn) {
          reloadTablesBtn.addEventListener('click', function () {
            loadTableStatus();
          });
        }
      } else if (createBtn) {
        createBtn.addEventListener('click', function () {
          alert('Không mở được form tạo đơn. Kiểm tra quyền ADMIN/Thu ngân hoặc tải lại trang.');
        });
      }

      // Lọc trạng thái + tìm kiếm + sắp xếp cột (giống trang Kho)
      var filterBtns = document.querySelectorAll('.order-filter');
      var tbodyEl = document.querySelector('.orders-table tbody');
      var orderSearchInput = document.getElementById('orderSearchInput');
      var orderStatusFilters = document.getElementById('orderStatusFilters');
      var orderDateInput = document.getElementById('orderDateFilterInput');
      var orderDateLabel = document.getElementById('orderDateFilterLabel');
      var orderDateClear = document.getElementById('orderDateFilterClear');
      var ordersTableHead = document.querySelector('.orders-table thead');
      var orderSortKey = null;
      var orderSortDir = 'asc';

      function normalizeOrderText(text) {
        return text ? text.toString().toLowerCase().trim() : '';
      }

      function getRowStatusText(row) {
        var statusEl = row ? row.querySelector('.status-badge') : null;
        return statusEl ? statusEl.textContent.trim() : '';
      }

      function isEmptyRow(row) {
        return !!(row && row.querySelector('.text-muted.py-4'));
      }

      function getOrderSortValue(row, key) {
        var cells = row.cells;
        if (!cells || cells.length < 8) return '';
        switch (key) {
          case 'code':
            return normalizeOrderText((cells[0] && cells[0].textContent || '').replace(/^#/, '').trim());
          case 'customer':
            return normalizeOrderText(cells[1] ? cells[1].textContent : '');
          case 'type':
            return normalizeOrderText(cells[2] ? cells[2].textContent : '');
          case 'table':
            return normalizeOrderText(cells[3] ? cells[3].textContent : '');
          case 'total': {
            var t = cells[4] ? cells[4].textContent : '';
            var digits = String(t).replace(/\D/g, '');
            var n = parseInt(digits, 10);
            return isFinite(n) ? n : 0;
          }
          case 'time':
            return row.getAttribute('data-sort-time') || (cells[5] ? cells[5].textContent.trim() : '');
          case 'status':
            return normalizeOrderText(cells[6] ? cells[6].textContent : '');
          case 'payment':
            return normalizeOrderText(cells[7] ? cells[7].textContent : '');
          default:
            return '';
        }
      }

      function compareOrderSortValues(va, vb, key) {
        if (key === 'total') {
          return va - vb;
        }
        return String(va).localeCompare(String(vb), 'vi', { sensitivity: 'base', numeric: true });
      }

      function sortOrderDataRows() {
        if (!tbodyEl || !orderSortKey) return;
        var all = Array.prototype.slice.call(tbodyEl.querySelectorAll('tr'));
        var emptyRows = all.filter(isEmptyRow);
        var dataRows = all.filter(function (r) { return r.classList.contains('order-data-row'); });
        dataRows.sort(function (ra, rb) {
          var va = getOrderSortValue(ra, orderSortKey);
          var vb = getOrderSortValue(rb, orderSortKey);
          var c = compareOrderSortValues(va, vb, orderSortKey);
          return orderSortDir === 'asc' ? c : -c;
        });
        dataRows.forEach(function (r) { tbodyEl.appendChild(r); });
        emptyRows.forEach(function (r) { tbodyEl.appendChild(r); });
      }

      function updateOrderSortHeaders() {
        if (!ordersTableHead) return;
        ordersTableHead.querySelectorAll('.stock-th-sort').forEach(function (btn) {
          var key = btn.getAttribute('data-sort');
          var icon = btn.querySelector('.stock-sort-icon');
          if (!icon) return;
          if (orderSortKey === key) {
            icon.className = 'bi stock-sort-icon ' + (orderSortDir === 'asc' ? 'bi-sort-up' : 'bi-sort-down');
          } else {
            icon.className = 'bi bi-arrow-down-up stock-sort-icon';
          }
        });
      }

      function getActiveOrderFilterKey() {
        if (!orderStatusFilters) return 'ALL';
        var active = orderStatusFilters.querySelector('.stock-filter.active');
        return active ? (active.getAttribute('data-order-filter') || 'ALL') : 'ALL';
      }

      function rowMatchesOrderFilter(row, filterKey) {
        if (filterKey === 'ALL') return true;
        var st = row.getAttribute('data-order-status') || '';
        return st === filterKey;
      }

      function rowMatchesOrderSearch(row, term) {
        if (!term) return true;
        return normalizeOrderText(row.textContent || '').indexOf(term) !== -1;
      }

      function rowMatchesOrderDate(row) {
        var sel = orderDateInput && orderDateInput.value ? orderDateInput.value : '';
        if (!sel) return true;
        var d = row.getAttribute('data-order-date') || '';
        return d === sel;
      }

      function refreshOrderDateLabel() {
        if (!orderDateLabel) return;
        var v = orderDateInput && orderDateInput.value ? orderDateInput.value : '';
        if (!v) {
          orderDateLabel.textContent = 'Mọi ngày';
          return;
        }
        var p = v.split('-');
        orderDateLabel.textContent = p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : v;
      }

      function updateOrderSubtitle() {
        var el = document.getElementById('orderPageSubtitle');
        if (!el) return;
        var all = document.querySelectorAll('.orders-table tbody tr.order-data-row');
        if (all.length === 0) {
          el.textContent = 'Chưa có đơn hàng';
          return;
        }
        var n = 0;
        all.forEach(function (r) {
          if (r.style.display !== 'none') n++;
        });
        el.textContent = n === 0 ? 'Không có đơn phù hợp bộ lọc' : n + ' đơn đang hiển thị';
      }

      function applyOrderFilters() {
        var filterKey = getActiveOrderFilterKey();
        var term = normalizeOrderText(orderSearchInput ? orderSearchInput.value : '');
        var rows = document.querySelectorAll('.orders-table tbody tr.order-data-row');
        rows.forEach(function (row) {
          var show = rowMatchesOrderFilter(row, filterKey) && rowMatchesOrderSearch(row, term) && rowMatchesOrderDate(row);
          row.style.display = show ? '' : 'none';
        });
        updateOrderSubtitle();
      }

      if (ordersTableHead) {
        ordersTableHead.addEventListener('click', function (e) {
          var btn = e.target.closest('.stock-th-sort');
          if (!btn) return;
          var key = btn.getAttribute('data-sort');
          if (!key) return;
          if (orderSortKey === key) {
            orderSortDir = orderSortDir === 'asc' ? 'desc' : 'asc';
          } else {
            orderSortKey = key;
            orderSortDir = 'asc';
          }
          sortOrderDataRows();
          updateOrderSortHeaders();
          applyOrderFilters();
        });
      }

      function sortPendingFirst() {
        if (!tbodyEl) return;
        var rows = Array.prototype.slice.call(tbodyEl.querySelectorAll('tr'));
        var empty = rows.filter(isEmptyRow);
        var normal = rows.filter(function (r) { return r.classList.contains('order-data-row'); });

        normal.sort(function (a, b) {
          var aIsPending = getRowStatusText(a) === 'Chờ xử lý' ? 1 : 0;
          var bIsPending = getRowStatusText(b) === 'Chờ xử lý' ? 1 : 0;
          if (aIsPending !== bIsPending) return bIsPending - aIsPending;
          return 0;
        });

        normal.forEach(function (r) { tbodyEl.appendChild(r); });
        empty.forEach(function (r) { tbodyEl.appendChild(r); });
      }

      if (filterBtns.length > 0 && orderStatusFilters) {
        orderStatusFilters.addEventListener('click', function (e) {
          var btn = e.target.closest('.order-filter');
          if (!btn) return;
          filterBtns.forEach(function (b) { b.classList.remove('active'); });
          btn.classList.add('active');
          applyOrderFilters();
        });
      }

      if (orderSearchInput) {
        orderSearchInput.addEventListener('input', applyOrderFilters);
      }

      if (orderDateInput) {
        orderDateInput.addEventListener('change', function () {
          refreshOrderDateLabel();
          applyOrderFilters();
        });
      }
      if (orderDateClear) {
        orderDateClear.addEventListener('click', function () {
          if (orderDateInput) orderDateInput.value = '';
          refreshOrderDateLabel();
          applyOrderFilters();
        });
      }
      refreshOrderDateLabel();

      var isBarista = !!document.getElementById('roleBaristaFlag');
      if (isBarista) {
        sortPendingFirst();
        var pendingBtn = Array.prototype.slice.call(filterBtns).find(function (b) {
          return b.getAttribute('data-order-filter') === 'PENDING';
        });
        if (pendingBtn) {
          filterBtns.forEach(function (b) { b.classList.remove('active'); });
          pendingBtn.classList.add('active');
        }
        applyOrderFilters();
      } else {
        updateOrderSubtitle();
      }

      // Barista: nút "Sẵn sàng phục vụ" (chỉ đổi trạng thái PENDING -> COMPLETED)
      var readyBtns = document.querySelectorAll('.barista-ready-btn');
      readyBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
          var code = this.getAttribute('data-code');
          if (!code) return;
          if (!confirm('Đánh dấu đơn ' + code + ' là "Hoàn thành"?')) return;

          fetch('/api/orders/' + code + '/status', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'COMPLETED' })
          })
            .then(function (res) {
              if (!res.ok) throw new Error('Update status failed');
              return res.json();
            })
            .then(function () { window.location.reload(); })
            .catch(function () { alert('Không thể cập nhật trạng thái đơn.'); });
        });
      });
});
