document.addEventListener('DOMContentLoaded', function () {
      var BS = typeof bootstrap !== 'undefined' ? bootstrap : (typeof window !== 'undefined' ? window.bootstrap : null);
      if (!BS || typeof BS.Modal !== 'function') {
        console.error('[order-page] Bootstrap Modal không khả dụng — kiểm tra CDN / thứ tự script.');
      }

      var createBtn = document.querySelector('.order-create-btn');
      var createModalEl = document.getElementById('createOrderModal');
      var orderInvoiceModalEl = document.getElementById('orderInvoiceModal');
      var orderPrepDetailModalEl = document.getElementById('orderPrepDetailModal');

      function markOrderStatusCompletedThenReload(code) {
        if (!code) return Promise.resolve();
        return fetch('/api/orders/' + encodeURIComponent(code) + '/status', {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: 'COMPLETED' })
        })
          .then(function (res) {
            if (!res.ok) throw new Error('failed');
            return res.json();
          })
          .then(function () {
            window.location.reload();
          })
          .catch(function () {
            alert('Không thể cập nhật trạng thái đơn.');
          });
      }

      if (BS && typeof BS.Modal === 'function') {
        var invoiceModal = orderInvoiceModalEl ? new BS.Modal(orderInvoiceModalEl) : null;
        var prepDetailModal = orderPrepDetailModalEl ? new BS.Modal(orderPrepDetailModalEl) : null;
        var serverPickupNotifyModalEl = document.getElementById('serverPickupNotifyModal');
        var serverPickupNotifyModal = serverPickupNotifyModalEl ? new BS.Modal(serverPickupNotifyModalEl) : null;
        var confirmCompleteModalEl = document.getElementById('confirmCompleteModal');
        var confirmCompleteModal = confirmCompleteModalEl ? new BS.Modal(confirmCompleteModalEl) : null;
        var confirmCompleteOrderCodeEl = document.getElementById('confirmCompleteOrderCode');
        var confirmCompleteBtn = document.getElementById('confirmCompleteBtn');
        var pendingCompleteCode = null;
        var paymentSettings = null;

        if (confirmCompleteBtn) {
          confirmCompleteBtn.addEventListener('click', function() {
            if (!pendingCompleteCode) return;
            confirmCompleteBtn.disabled = true;
            confirmCompleteBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...';
            markOrderStatusCompletedThenReload(pendingCompleteCode).finally(function() {
              confirmCompleteBtn.disabled = false;
              confirmCompleteBtn.innerHTML = '<i class="bi bi-check-lg me-1"></i>Xác nhận hoàn thành';
              if (confirmCompleteModal) confirmCompleteModal.hide();
            });
          });
        }

        window.showConfirmCompleteModal = function(code) {
          pendingCompleteCode = code;
          if (confirmCompleteOrderCodeEl) confirmCompleteOrderCodeEl.textContent = '#' + code;
          if (confirmCompleteModal) confirmCompleteModal.show();
        };

        var invOrderCodeLabel = document.getElementById('invOrderCodeLabel');
        var invMetaLine = document.getElementById('invMetaLine');
        var invItemsBody = document.getElementById('invItemsBody');
        var invTotalLine = document.getElementById('invTotalLine');
        var invQrBlock = document.getElementById('invQrBlock');
        var invQrPlaceholder = document.getElementById('invQrPlaceholder');
        var invQrImage = document.getElementById('invQrImage');
        var invQrCaption = document.getElementById('invQrCaption');
        var invPaidAlert = document.getElementById('invPaidAlert');
        var invMarkPaidBtn = document.getElementById('invMarkPaidBtn');
        var invMarkUnpaidBtn = document.getElementById('invMarkUnpaidBtn');
        var currentInvoiceOrderCode = null;

        var prepOrderCodeLabel = document.getElementById('prepOrderCodeLabel');
        var prepMetaLine = document.getElementById('prepMetaLine');
        var prepStatusLine = document.getElementById('prepStatusLine');
        var prepItemsBody = document.getElementById('prepItemsBody');
        var prepHintLine = document.getElementById('prepHintLine');
        var prepMarkCompletedBtn = document.getElementById('prepMarkCompletedBtn');
        var currentPrepOrderCode = null;

        function formatCurrencyVND(amount) {
          var num = Number(amount || 0);
          if (!isFinite(num) || num <= 0) {
            return '0 đ';
          }
          return num.toLocaleString('vi-VN') + ' đ';
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
            })
            .catch(function () {
              paymentSettings = null;
            });
        }

        function formatInvoiceDateTime(iso) {
          if (!iso) return '';
          var d = new Date(iso);
          if (!isFinite(d.getTime())) return String(iso);
          return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
        }

        function orderStatusLabelVi(st) {
          if (st === 'COMPLETED') return 'Hoàn thành (pha chế)';
          if (st === 'PENDING') return 'Chờ xử lý';
          if (st === 'CANCELLED') return 'Đã hủy';
          return st || '—';
        }

        function refreshInvoicePaymentButtons(order) {
          var canMark = !!document.getElementById('orderPageMarkPaidAllowed');
          if (invMarkPaidBtn) {
            invMarkPaidBtn.classList.add('d-none');
            invMarkPaidBtn.disabled = false;
          }
          if (invMarkUnpaidBtn) {
            invMarkUnpaidBtn.classList.add('d-none');
            invMarkUnpaidBtn.disabled = false;
          }
          if (!canMark || !order) {
            return;
          }
          if (order.status === 'CANCELLED') {
            return;
          }
          if (order.paidAt) {
            if (invMarkUnpaidBtn) invMarkUnpaidBtn.classList.remove('d-none');
          } else {
            if (invMarkPaidBtn) invMarkPaidBtn.classList.remove('d-none');
          }
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

        function openInvoiceModal(orderCode) {
          if (!invoiceModal || !orderCode) return;
          currentInvoiceOrderCode = orderCode;

          function ensurePaymentSettings() {
            if (paymentSettings) return Promise.resolve();
            return fetch('/api/settings/payment')
              .then(function (r) { return r.ok ? r.json() : null; })
              .then(function (d) { paymentSettings = d || null; });
          }

          Promise.all([
            ensurePaymentSettings(),
            fetch('/api/orders/' + encodeURIComponent(orderCode)).then(function (r) {
              if (!r.ok) throw new Error('Không tải được đơn.');
              return r.json();
            }),
            fetch('/api/orders/' + encodeURIComponent(orderCode) + '/items').then(function (r) {
              if (!r.ok) throw new Error('Không tải được chi tiết món.');
              return r.json();
            })
          ]).then(function (parts) {
            var order = parts[1];
            var items = Array.isArray(parts[2]) ? parts[2] : [];

            if (invOrderCodeLabel) invOrderCodeLabel.textContent = order.orderCode ? ('#' + order.orderCode) : '';

            var cust = (order.customerName || 'Khách lẻ') + (order.customerPhone ? (' · ' + order.customerPhone) : '');
            var tableNote = order.tableNumber != null ? ('Bàn ' + order.tableNumber) : (order.orderNote || '—');
            var pm = order.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt';
            if (invMetaLine) {
              invMetaLine.textContent = cust + ' · ' + tableNote + ' · ' + orderStatusLabelVi(order.status) + ' · ' + pm;
            }

            if (invItemsBody) {
              invItemsBody.innerHTML = '';
              items.forEach(function (it) {
                var tr = document.createElement('tr');
                var nameTd = document.createElement('td');
                nameTd.textContent = it.itemName || '—';
                var qtyTd = document.createElement('td');
                qtyTd.className = 'text-center';
                qtyTd.textContent = it.quantity != null ? String(it.quantity) : '0';
                var subTd = document.createElement('td');
                subTd.className = 'text-end';
                var price = Number(it.itemPrice != null ? it.itemPrice : 0);
                var q = Number(it.quantity != null ? it.quantity : 0);
                var line = (isFinite(price) && isFinite(q)) ? price * q : 0;
                subTd.textContent = line.toLocaleString('vi-VN') + ' đ';
                tr.appendChild(nameTd);
                tr.appendChild(qtyTd);
                tr.appendChild(subTd);
                invItemsBody.appendChild(tr);
              });
            }

            var tot = order.total != null ? Number(order.total) : 0;
            if (invTotalLine) invTotalLine.textContent = 'Tổng cộng: ' + formatCurrencyVND(tot);

            var qrUrl = buildQrUrlForOrder(order);
            if (invQrBlock && invQrPlaceholder && invQrImage && invQrCaption) {
              if (qrUrl) {
                invQrBlock.classList.remove('d-none');
                invQrPlaceholder.classList.add('d-none');
                invQrImage.src = qrUrl;
                invQrImage.style.display = 'block';
                invQrCaption.textContent = 'Số tiền: ' + formatCurrencyVND(tot) + ' — quét để chuyển khoản.';
              } else {
                invQrBlock.classList.add('d-none');
                invQrPlaceholder.classList.remove('d-none');
                invQrImage.style.display = 'none';
                invQrImage.src = '';
                if (order.paymentMethod === 'BANK_TRANSFER') {
                  invQrPlaceholder.textContent = 'Chưa cấu hình VietQR trong Cài đặt, hoặc thiếu thông tin ngân hàng.';
                } else {
                  invQrPlaceholder.textContent = 'Đơn tiền mặt: thu tiền trực tiếp, không hiển thị QR.';
                }
              }
            }

            if (invPaidAlert) {
              if (order.status === 'CANCELLED') {
                invPaidAlert.className = 'alert alert-secondary border mt-3 mb-0 small';
                invPaidAlert.textContent = 'Đơn đã hủy — không cập nhật thanh toán.';
              } else if (order.paidAt) {
                invPaidAlert.className = 'alert alert-success border mt-3 mb-0 small';
                invPaidAlert.textContent = 'Đã ghi nhận thu tiền lúc ' + formatInvoiceDateTime(order.paidAt) + '.';
              } else {
                invPaidAlert.className = 'alert alert-warning border mt-3 mb-0 small';
                invPaidAlert.textContent = 'Chưa ghi nhận thu tiền — sau khi nhận tiền từ khách, bấm “Xác nhận đã thu tiền”.';
              }
            }

            refreshInvoicePaymentButtons(order);
            invoiceModal.show();
          }).catch(function (err) {
            alert(err && err.message ? err.message : 'Không mở được hóa đơn.');
          });
        }

        function openPrepDetailModal(orderCode) {
          if (!prepDetailModal || !orderCode) return;
          currentPrepOrderCode = orderCode;

          Promise.all([
            fetch('/api/orders/' + encodeURIComponent(orderCode)).then(function (r) {
              if (!r.ok) throw new Error('Không tải được đơn.');
              return r.json();
            }),
            fetch('/api/orders/' + encodeURIComponent(orderCode) + '/items').then(function (r) {
              if (!r.ok) throw new Error('Không tải được chi tiết món.');
              return r.json();
            })
          ]).then(function (parts) {
            var order = parts[0];
            var items = Array.isArray(parts[1]) ? parts[1] : [];

            if (prepOrderCodeLabel) prepOrderCodeLabel.textContent = order.orderCode ? ('#' + order.orderCode) : '';

            var typeLabel = order.type === 'TAKEAWAY' ? 'Mang về' : 'Tại bàn';
            var tablePart = order.tableNumber != null ? (' · Bàn ' + order.tableNumber) : '';
            var notePart = (order.orderNote && String(order.orderNote).trim()) ? (' · Ghi chú đơn: ' + order.orderNote) : '';
            var cust = (order.customerName || 'Khách lẻ') + (order.customerPhone ? (' · ' + order.customerPhone) : '');
            if (prepMetaLine) {
              prepMetaLine.textContent = typeLabel + tablePart + notePart + ' · ' + cust;
            }

            if (prepStatusLine) {
              prepStatusLine.innerHTML = '<span class="fw-medium">Trạng thái đơn:</span> ' + orderStatusLabelVi(order.status);
            }

            if (prepItemsBody) {
              prepItemsBody.innerHTML = '';
              items.forEach(function (it) {
                var tr = document.createElement('tr');
                var nameTd = document.createElement('td');
                nameTd.textContent = it.itemName || '—';
                var qtyTd = document.createElement('td');
                qtyTd.className = 'text-center';
                qtyTd.textContent = it.quantity != null ? String(it.quantity) : '0';
                var noteTd = document.createElement('td');
                noteTd.className = 'small text-break';
                noteTd.textContent = (it.note && String(it.note).trim()) ? it.note : '—';
                tr.appendChild(nameTd);
                tr.appendChild(qtyTd);
                tr.appendChild(noteTd);
                prepItemsBody.appendChild(tr);
              });
            }

            var canMarkPrep = !!document.getElementById('orderPageCanMarkPrepComplete');
            if (prepMarkCompletedBtn) {
              if (canMarkPrep && order.status === 'PENDING') {
                prepMarkCompletedBtn.classList.remove('d-none');
              } else {
                prepMarkCompletedBtn.classList.add('d-none');
              }
            }

            if (prepHintLine) {
              prepHintLine.classList.remove('d-none');
              if (canMarkPrep && order.status === 'PENDING') {
                prepHintLine.textContent = 'Kiểm tra đủ món và ghi chú, sau đó bấm « Hoàn thành pha chế » — đồng bộ với bảng đơn.';
              } else if (canMarkPrep) {
                prepHintLine.textContent = 'Đơn không còn ở trạng thái chờ pha chế.';
              } else {
                prepHintLine.textContent = 'Chỉ xem nội dung pha chế (Thu ngân / Phục vụ không đánh dấu hoàn thành pha chế).';
              }
            }

            prepDetailModal.show();
          }).catch(function (err) {
            alert(err && err.message ? err.message : 'Không mở được chi tiết đơn.');
          });
        }

        function submitInvoicePayment(paid) {
          if (!currentInvoiceOrderCode) return;
          var code = currentInvoiceOrderCode;
          fetch('/api/orders/' + encodeURIComponent(code) + '/payment', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ paid: paid })
          })
            .then(function (res) {
              if (!res.ok) {
                return res.json().catch(function () { return {}; }).then(function (body) {
                  throw new Error((body && body.error) ? body.error : 'Cập nhật thanh toán thất bại');
                });
              }
              return res.json();
            })
            .then(function () {
              window.location.reload();
            })
            .catch(function (e) {
              alert(e && e.message ? e.message : 'Lỗi cập nhật thanh toán.');
            });
        }

        if (invMarkPaidBtn) {
          invMarkPaidBtn.addEventListener('click', function () {
            if (!confirm('Xác nhận đã thu tiền cho đơn ' + currentInvoiceOrderCode + '?')) return;
            submitInvoicePayment(true);
          });
        }
        if (invMarkUnpaidBtn) {
          invMarkUnpaidBtn.addEventListener('click', function () {
            if (!confirm('Hủy trạng thái đã thu tiền cho đơn này?')) return;
            submitInvoicePayment(false);
          });
        }

        if (prepMarkCompletedBtn) {
          prepMarkCompletedBtn.addEventListener('click', function () {
            var c = currentPrepOrderCode;
            if (!c) return;
            if (prepDetailModal) prepDetailModal.hide();
            window.showConfirmCompleteModal(c);
          });
        }

        var invoiceTableBody = document.querySelector('.orders-table tbody');
        if (invoiceTableBody) {
          invoiceTableBody.addEventListener('click', function (e) {
            var db = e.target.closest('.order-detail-btn');
            if (db) {
              var oc = db.getAttribute('data-code');
              if (oc) openPrepDetailModal(oc);
              return;
            }
            var ib = e.target.closest('.invoice-order-btn');
            if (ib) {
              var oc2 = ib.getAttribute('data-code');
              if (oc2) openInvoiceModal(oc2);
            }
          });
        }

        if (document.getElementById('orderPageServerPickupPoll')) {
          var serverPickupNotifyBody = document.getElementById('serverPickupNotifyBody');
          var STORAGE_PREP_AFTER = 'serverOrderPrepAfter';
          var STORAGE_READY_ORDERS = 'serverReadyOrders';

          var serverDropdown = document.getElementById('serverReadyOrdersDropdown');
          var serverBadge = document.getElementById('serverReadyOrdersBadge');
          var serverList = document.getElementById('serverReadyOrdersList');
          var serverClearBtn = document.getElementById('serverClearAllReady');

          if (serverDropdown) serverDropdown.style.display = 'block';

          function localIsoNoMs(d) {
            function p(n) { return String(n).padStart(2, '0'); }
            return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + 'T' +
              p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
          }

          function escapeHtmlText(s) {
            var d = document.createElement('div');
            d.textContent = s;
            return d.innerHTML;
          }

          function loadReadyOrders() {
            try {
              var raw = sessionStorage.getItem(STORAGE_READY_ORDERS);
              var arr = raw ? JSON.parse(raw) : [];
              return Array.isArray(arr) ? arr : [];
            } catch (e) {
              return [];
            }
          }

          function saveReadyOrders(arr) {
            try {
              sessionStorage.setItem(STORAGE_READY_ORDERS, JSON.stringify(arr.slice(-50)));
            } catch (e) { /* ignore */ }
          }

          var readyOrders = loadReadyOrders();
          if (!sessionStorage.getItem(STORAGE_PREP_AFTER)) {
            sessionStorage.setItem(STORAGE_PREP_AFTER, localIsoNoMs(new Date(Date.now() - 180000)));
          }

          function maxIso(a, b) {
            if (!a) return b;
            if (!b) return a;
            return a > b ? a : b;
          }

          function updateServerBadge() {
            if (!serverBadge) return;
            var count = readyOrders.length;
            if (count > 0) {
              serverBadge.textContent = count > 99 ? '99+' : String(count);
              serverBadge.classList.remove('d-none');
            } else {
              serverBadge.classList.add('d-none');
            }
          }

          function renderReadyOrdersList() {
            if (!serverList) return;
            if (readyOrders.length === 0) {
              serverList.innerHTML = '<div class="p-3 text-muted small text-center">Chưa có đơn nào chờ phục vụ.</div>';
              return;
            }
            var html = '<div class="list-group list-group-flush">';
            readyOrders.forEach(function (row, idx) {
              var typeLabel = row.type === 'TAKEAWAY' ? '<span class="badge bg-warning-subtle text-warning-emphasis me-1">Mang về</span>' : '';
              var tableLabel = (row.tableNumber != null && row.tableNumber !== '') ? '<span class="badge bg-info-subtle text-info-emphasis me-1">Bàn ' + escapeHtmlText(String(row.tableNumber)) + '</span>' : '';
              var name = row.customerName ? escapeHtmlText(String(row.customerName).trim()) : 'Khách';
              var note = (row.orderNote && String(row.orderNote).trim()) ? '<div class="text-muted small text-truncate" style="max-width: 250px;">' + escapeHtmlText(String(row.orderNote).trim()) + '</div>' : '';
              var time = row.preparedAt ? new Date(row.preparedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';

              html += '<div class="list-group-item d-flex justify-content-between align-items-start py-2 px-3 server-ready-item" data-idx="' + idx + '" data-code="' + escapeHtmlText(String(row.orderCode)) + '">';
              html += '<div class="flex-grow-1">';
              html += '<div class="d-flex align-items-center gap-1 flex-wrap">';
              html += '<strong class="text-primary">#' + escapeHtmlText(String(row.orderCode)) + '</strong>';
              html += typeLabel + tableLabel;
              html += '</div>';
              html += '<div class="small">' + name + '</div>';
              html += note;
              html += '</div>';
              html += '<div class="d-flex flex-column align-items-end gap-1">';
              html += '<span class="small text-muted">' + time + '</span>';
              html += '<button type="button" class="btn btn-sm btn-outline-success py-0 px-2 server-done-btn" data-idx="' + idx + '" title="Đã phục vụ"><i class="bi bi-check-lg"></i></button>';
              html += '</div>';
              html += '</div>';
            });
            html += '</div>';
            serverList.innerHTML = html;

            serverList.querySelectorAll('.server-done-btn').forEach(function (btn) {
              btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var idx = parseInt(btn.getAttribute('data-idx'), 10);
                if (!isNaN(idx) && idx >= 0 && idx < readyOrders.length) {
                  readyOrders.splice(idx, 1);
                  saveReadyOrders(readyOrders);
                  updateServerBadge();
                  renderReadyOrdersList();
                }
              });
            });

            serverList.querySelectorAll('.server-ready-item').forEach(function (item) {
              item.style.cursor = 'pointer';
              item.addEventListener('click', function () {
                var code = item.getAttribute('data-code');
                if (code) {
                  var row = document.querySelector('tr[data-order-code="' + code + '"]');
                  if (row) {
                    row.scrollIntoView({ block: 'center', behavior: 'smooth' });
                    row.style.backgroundColor = '#fff3cd';
                    setTimeout(function () { row.style.backgroundColor = ''; }, 2000);
                  }
                }
              });
            });
          }

          if (serverClearBtn) {
            serverClearBtn.addEventListener('click', function () {
              readyOrders = [];
              saveReadyOrders(readyOrders);
              updateServerBadge();
              renderReadyOrdersList();
            });
          }

          function pollPreparedSince() {
            var after = sessionStorage.getItem(STORAGE_PREP_AFTER);
            if (!after) return;
            fetch('/api/orders/prepared-since?after=' + encodeURIComponent(after))
              .then(function (r) {
                if (!r.ok) throw new Error('poll');
                return r.json();
              })
              .then(function (list) {
                if (!Array.isArray(list) || list.length === 0) return;
                var newestAfter = after;
                list.forEach(function (row) {
                  if (row && row.preparedAt) newestAfter = maxIso(newestAfter, String(row.preparedAt));
                });

                var existingCodes = readyOrders.map(function (r) { return r.orderCode; });
                var toShow = [];
                list.forEach(function (row) {
                  var code = row && row.orderCode;
                  if (!code) return;
                  if (existingCodes.indexOf(code) >= 0) return;
                  readyOrders.unshift(row);
                  toShow.push(row);
                });

                if (newestAfter && newestAfter !== after) {
                  sessionStorage.setItem(STORAGE_PREP_AFTER, newestAfter);
                }
                saveReadyOrders(readyOrders);
                updateServerBadge();
                renderReadyOrdersList();

                if (toShow.length) {
                  try {
                    var notifSound = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2teleVcuRYi9zMF5RjA8dLPKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc8c7XKvn1LNzxztcq+fUs3PHO1yr59Szc=');
                    notifSound.volume = 0.5;
                    notifSound.play().catch(function() {});
                  } catch(e) {}

                  if (serverPickupNotifyBody && serverPickupNotifyModal) {
                    var html = '<ul class="mb-0 ps-3">';
                    toShow.forEach(function (row) {
                      var line = '<strong>#' + escapeHtmlText(String(row.orderCode)) + '</strong>';
                      if (row.type === 'TAKEAWAY') line += ' · Mang về';
                      else if (row.tableNumber != null && row.tableNumber !== '') line += ' · Bàn ' + escapeHtmlText(String(row.tableNumber));
                      var name = row.customerName ? String(row.customerName).trim() : '';
                      if (name) line += ' · ' + escapeHtmlText(name);
                      if (row.orderNote && String(row.orderNote).trim()) {
                        line += ' <span class="text-muted">(' + escapeHtmlText(String(row.orderNote).trim()) + ')</span>';
                      }
                      html += '<li class="mb-1">' + line + '</li>';
                    });
                    html += '</ul>';
                    serverPickupNotifyBody.innerHTML = html;
                    serverPickupNotifyModal.show();
                  }
                }
              })
              .catch(function () { /* im lặng — poll nền */ });
          }

          updateServerBadge();
          renderReadyOrdersList();
          pollPreparedSince();
          setInterval(pollPreparedSince, 3000);
        }

        if (createModalEl) {
        var createModal = new BS.Modal(createModalEl);
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
        var walkInGuestCheckbox = document.getElementById('orderWalkInGuest');
        var WALK_IN_DISPLAY_NAME = 'Khách vãng lai';
        var orderTypeSelectEl = document.getElementById('orderType');
        var orderNoteInput = document.getElementById('orderNote');
        var selectedTableField = document.getElementById('selectedTableField');
        var selectedTableDisplay = document.getElementById('selectedTableDisplay');
        var tablePickerSection = document.getElementById('tablePickerSection');
        var tablePickerGrid = document.getElementById('tablePickerGrid');
        var reloadTablesBtn = document.getElementById('reloadTablesBtn');
        var tablePickerHint = document.getElementById('tablePickerHint');

        var selectedTableNumber = null;

        var orderPaymentMethodSelect = document.getElementById('orderPaymentMethod');

        var modalTitle = document.querySelector('#createOrderModal .modal-title');
        var editingOrderCode = null;

        var currentOrderItems = []; // {id, name, price, quantity, note, loyaltyRedemption?}
        var waitingLoyaltyRedeemSlot = false;
        var loyaltyPhoneTimer = null;
        var LOYALTY_REDEEM_POINTS = 10;
        var LOYALTY_MAX_FREE_VND = 50000;

        var orderLoyaltyRow = document.getElementById('orderLoyaltyRow');
        var orderLoyaltyPointsBadge = document.getElementById('orderLoyaltyPointsBadge');
        var orderLoyaltyHint = document.getElementById('orderLoyaltyHint');
        var orderLoyaltyRedeemModeBtn = document.getElementById('orderLoyaltyRedeemModeBtn');
        var orderLoyaltyRedeemActiveHint = document.getElementById('orderLoyaltyRedeemActiveHint');

        function isNonDrinkMenuCategory(item) {
          var cat = (item && item.category && item.category.name) ? String(item.category.name).trim().toLowerCase() : '';
          var foodCats = ['bánh ngọt', 'thức ăn nhẹ', 'đồ ăn nhẹ', 'do an nhe', 'banh ngot', 'food', 'snack'];
          return foodCats.indexOf(cat) !== -1;
        }

        function syncRedeemModeUi() {
          if (orderLoyaltyRedeemActiveHint) {
            orderLoyaltyRedeemActiveHint.classList.toggle('d-none', !waitingLoyaltyRedeemSlot);
          }
          if (orderLoyaltyRedeemModeBtn) {
            orderLoyaltyRedeemModeBtn.classList.toggle('active', waitingLoyaltyRedeemSlot);
          }
        }

        function applyWalkInGuestUi() {
          var on = walkInGuestCheckbox && walkInGuestCheckbox.checked;
          if (customerNameInput) {
            customerNameInput.disabled = !!on;
            if (on) {
              customerNameInput.value = WALK_IN_DISPLAY_NAME;
            }
          }
          if (customerPhoneInput) {
            customerPhoneInput.disabled = !!on;
            if (on) {
              customerPhoneInput.value = '';
            }
          }
          if (on) {
            waitingLoyaltyRedeemSlot = false;
            if (orderLoyaltyRow) orderLoyaltyRow.style.display = 'none';
            syncRedeemModeUi();
          } else {
            if (customerNameInput && customerNameInput.value.trim() === WALK_IN_DISPLAY_NAME) {
              customerNameInput.value = '';
            }
            refreshOrderLoyaltyFromPhone();
          }
        }

        if (walkInGuestCheckbox) {
          walkInGuestCheckbox.addEventListener('change', function () {
            if (walkInGuestCheckbox.checked) {
              var hasRedeem = currentOrderItems.some(function (r) { return r.loyaltyRedemption; });
              if (hasRedeem) {
                alert('Đơn đang có món đổi điểm. Xóa dòng đó trước khi chọn khách vãng lai.');
                walkInGuestCheckbox.checked = false;
                return;
              }
            }
            applyWalkInGuestUi();
          });
        }

        function refreshOrderLoyaltyFromPhone() {
          if (walkInGuestCheckbox && walkInGuestCheckbox.checked) {
            if (orderLoyaltyRow) orderLoyaltyRow.style.display = 'none';
            waitingLoyaltyRedeemSlot = false;
            syncRedeemModeUi();
            return;
          }
          if (!orderLoyaltyRow || !customerPhoneInput) return;
          var phone = customerPhoneInput.value.trim();
          if (!phone) {
            orderLoyaltyRow.style.display = 'none';
            waitingLoyaltyRedeemSlot = false;
            syncRedeemModeUi();
            return;
          }
          if (phone.replace(/\D/g, '').length < 9) {
            orderLoyaltyRow.style.display = 'none';
            return;
          }

          fetch('/api/customers/phone/' + encodeURIComponent(phone))
            .then(function (res) {
              orderLoyaltyRow.style.display = '';
              if (res.status === 404) {
                if (orderLoyaltyPointsBadge) {
                  orderLoyaltyPointsBadge.textContent = 'Khách mới';
                  orderLoyaltyPointsBadge.className = 'badge bg-secondary';
                }
                if (orderLoyaltyHint) {
                  orderLoyaltyHint.textContent = 'Chưa có hồ sơ — coi như đăng ký tại quầy. Hoàn thành đơn sẽ tạo hồ sơ theo SĐT và tích điểm (đồ uống).';
                }
                if (orderLoyaltyRedeemModeBtn) orderLoyaltyRedeemModeBtn.disabled = true;
                waitingLoyaltyRedeemSlot = false;
                syncRedeemModeUi();
                return null;
              }
              if (!res.ok) {
                orderLoyaltyRow.style.display = 'none';
                return null;
              }
              return res.json();
            })
            .then(function (customer) {
              if (!customer) return;
              var p = customer.loyaltyPoints != null ? Number(customer.loyaltyPoints) : 0;
              if (orderLoyaltyPointsBadge) {
                orderLoyaltyPointsBadge.textContent = p + ' điểm';
                orderLoyaltyPointsBadge.className = 'badge ' + (p >= LOYALTY_REDEEM_POINTS ? 'bg-success' : 'bg-secondary');
              }
              if (orderLoyaltyHint) {
                orderLoyaltyHint.textContent = p >= LOYALTY_REDEEM_POINTS
                  ? ('Đủ điểm đổi 1 ly (đồ uống dưới ' + (LOYALTY_MAX_FREE_VND / 1000) + 'k). Đồng bộ với Quản lý khách hàng.')
                  : ('Cần ' + LOYALTY_REDEEM_POINTS + ' điểm để đổi 1 ly.');
              }
              var hasRedeemLine = currentOrderItems.some(function (r) { return r.loyaltyRedemption; });
              if (orderLoyaltyRedeemModeBtn) {
                orderLoyaltyRedeemModeBtn.disabled = p < LOYALTY_REDEEM_POINTS || hasRedeemLine;
              }
              if (hasRedeemLine) waitingLoyaltyRedeemSlot = false;
              syncRedeemModeUi();
            })
            .catch(function () {
              if (orderLoyaltyRow) orderLoyaltyRow.style.display = 'none';
            });
        }

        function lookupCustomerByPhone() {
          if (walkInGuestCheckbox && walkInGuestCheckbox.checked) return;
          if (!customerPhoneInput || !customerNameInput) return;
          var rawPhone = customerPhoneInput.value.trim();
          if (!rawPhone) {
            refreshOrderLoyaltyFromPhone();
            return;
          }

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
              refreshOrderLoyaltyFromPhone();
            })
            .catch(function () {
              refreshOrderLoyaltyFromPhone();
            });
        }

        if (customerPhoneInput) {
          customerPhoneInput.addEventListener('blur', lookupCustomerByPhone);
          customerPhoneInput.addEventListener('change', lookupCustomerByPhone);
          customerPhoneInput.addEventListener('input', function () {
            if (loyaltyPhoneTimer) clearTimeout(loyaltyPhoneTimer);
            loyaltyPhoneTimer = setTimeout(function () {
              refreshOrderLoyaltyFromPhone();
            }, 400);
          });
        }

        if (orderLoyaltyRedeemModeBtn) {
          orderLoyaltyRedeemModeBtn.addEventListener('click', function () {
            if (walkInGuestCheckbox && walkInGuestCheckbox.checked) return;
            if (orderLoyaltyRedeemModeBtn.disabled) return;
            waitingLoyaltyRedeemSlot = !waitingLoyaltyRedeemSlot;
            syncRedeemModeUi();
          });
        }

        function setCurrentTime() {
          if (!orderTimeInput) return;
          var now = new Date();
          var hours = String(now.getHours()).padStart(2, '0');
          var minutes = String(now.getMinutes()).padStart(2, '0');
          orderTimeInput.value = hours + ':' + minutes;
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

          var catalogPrice = Number(item.price || 0);
          if (!isFinite(catalogPrice) || catalogPrice < 0) catalogPrice = 0;

          var redeemApply = Boolean(waitingLoyaltyRedeemSlot);
          if (redeemApply && walkInGuestCheckbox && walkInGuestCheckbox.checked) {
            return;
          }
          if (redeemApply) {
            if (currentOrderItems.some(function (r) { return r.loyaltyRedemption; })) {
              alert('Đơn chỉ có tối đa 1 ly đổi điểm.');
              return;
            }
            if (qty !== 1) {
              alert('Đổi điểm: chỉ thêm đúng 1 ly (số lượng 1).');
              return;
            }
            if (catalogPrice >= LOYALTY_MAX_FREE_VND) {
              alert('Chỉ đổi được món có giá dưới ' + LOYALTY_MAX_FREE_VND.toLocaleString('vi-VN') + 'đ.');
              return;
            }
            if (isNonDrinkMenuCategory(item)) {
              alert('Chỉ đồ uống mới được đổi bằng điểm.');
              return;
            }
          }

          var existing = currentOrderItems.find(function (row) {
            return String(row.id) === String(item.id) && Boolean(row.loyaltyRedemption) === redeemApply;
          });
          if (existing && !redeemApply) {
            existing.quantity += qty;
            if (lineNote && (!existing.note || !String(existing.note).trim())) {
              existing.note = lineNote;
            }
          } else {
            currentOrderItems.push({
              id: String(item.id),
              name: redeemApply ? ((item.name || '') + ' (đổi điểm)') : (item.name || ''),
              price: redeemApply ? 0 : catalogPrice,
              quantity: qty,
              note: lineNote || '',
              loyaltyRedemption: redeemApply
            });
          }

          if (redeemApply) {
            waitingLoyaltyRedeemSlot = false;
            syncRedeemModeUi();
          }

          renderOrderItems();
          refreshOrderLoyaltyFromPhone();

          if (menuSearchInput) {
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
            var isRedeem = Boolean(row.loyaltyRedemption);

            var tdName = document.createElement('td');
            tdName.textContent = row.name || '';
            if (isRedeem) {
              var b = document.createElement('span');
              b.className = 'badge bg-info text-dark ms-1';
              b.textContent = 'Đổi điểm';
              tdName.appendChild(document.createTextNode(' '));
              tdName.appendChild(b);
            }

            var tdQty = document.createElement('td');
            tdQty.className = 'text-center';
            var qtyInput = document.createElement('input');
            qtyInput.type = 'number';
            qtyInput.min = '1';
            qtyInput.className = 'form-control form-control-sm text-center';
            qtyInput.value = String(row.quantity || 1);
            qtyInput.disabled = isRedeem;
            qtyInput.addEventListener('input', function () {
              if (isRedeem) return;
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
            tdPrice.textContent = isRedeem ? '0 đ' : (Number(row.price || 0).toLocaleString('vi-VN') + ' đ');

            var lineTotal = isRedeem ? 0 : (Number(row.price || 0) * Number(row.quantity || 1));
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
              refreshOrderLoyaltyFromPhone();
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
            orderTotalAmountInput.value = Number(total).toLocaleString('vi-VN') + ' đ';
          }

          refreshOrderLoyaltyFromPhone();
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
            waitingLoyaltyRedeemSlot = false;
            if (orderLoyaltyRow) orderLoyaltyRow.style.display = 'none';
            syncRedeemModeUi();
            if (tablePickerHint) tablePickerHint.textContent = '';
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

            if (walkInGuestCheckbox) walkInGuestCheckbox.checked = false;
            applyWalkInGuestUi();
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
                if (document.getElementById('orderWalkInGuest')) {
                  document.getElementById('orderWalkInGuest').checked = order.walkInGuest === true;
                }
                applyWalkInGuestUi();
                if (document.getElementById('orderType')) document.getElementById('orderType').value = order.type || 'DINE_IN';
                if (document.getElementById('orderPaymentMethod')) document.getElementById('orderPaymentMethod').value = order.paymentMethod || 'CASH';

                var tn = order.tableNumber != null ? Number(order.tableNumber) : null;
                selectedTableNumber = tn != null && Number.isFinite(tn) ? tn : null;
                if (document.getElementById('orderNote')) {
                  document.getElementById('orderNote').value = order.orderNote != null ? String(order.orderNote) : '';
                }
                if (selectedTableDisplay) {
                  selectedTableDisplay.value = selectedTableNumber != null ? ('Bàn ' + selectedTableNumber) : '';
                }

                toggleTablePickerByType();
                loadTableStatus();

                refreshOrderLoyaltyFromPhone();

                return fetch('/api/orders/' + code + '/items');
              })
              .then(function (res) { return res.json(); })
              .then(function (items) {
                if (Array.isArray(items)) {
                  currentOrderItems = items.map(function (item) {
                    var lr = Boolean(item.loyaltyRedemption);
                    return {
                      id: item.menuItemId ? String(item.menuItemId) : (item.menuItem ? String(item.menuItem.id) : null),
                      name: item.itemName,
                      price: lr ? 0 : Number(item.itemPrice != null ? item.itemPrice : 0),
                      quantity: item.quantity,
                      note: item.note || '',
                      loyaltyRedemption: lr
                    };
                  }).filter(function (x) { return x.id != null; });
                }
                renderOrderItems();
                refreshOrderLoyaltyFromPhone();
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

            var walkIn = walkInGuestCheckbox && walkInGuestCheckbox.checked;
            if (walkIn) {
              var hasRedeemSubmit = currentOrderItems.some(function (r) { return r.loyaltyRedemption; });
              if (hasRedeemSubmit) {
                alert('Khách vãng lai không thể đổi điểm. Xóa dòng đổi điểm hoặc bỏ chọn khách vãng lai.');
                return;
              }
            }

            // customerNameInput & customerPhoneInput đã được khai báo phía trên

            var payload = {
              walkInGuest: walkIn ? true : null,
              customerName: walkIn ? WALK_IN_DISPLAY_NAME : (customerNameInput ? customerNameInput.value.trim() : null),
              customerPhone: walkIn ? null : (customerPhoneInput ? customerPhoneInput.value.trim() : null),
              type: orderTypeSelectEl ? orderTypeSelectEl.value : 'DINE_IN',
              tableNumber: (function () {
                var type = orderTypeSelectEl ? orderTypeSelectEl.value : 'DINE_IN';
                if (type !== 'DINE_IN') return null;
                return selectedTableNumber != null && Number.isFinite(Number(selectedTableNumber))
                  ? Number(selectedTableNumber)
                  : null;
              })(),
              orderNote: (function () {
                var raw = orderNoteInput ? orderNoteInput.value.trim() : '';
                return raw ? raw : null;
              })(),
              status: orderStatusSelect ? orderStatusSelect.value : 'PENDING',
              paymentMethod: orderPaymentMethodSelect ? orderPaymentMethodSelect.value : 'CASH',
              items: currentOrderItems.map(function (row) {
                return {
                  menuItemId: row.id,
                  quantity: row.quantity,
                  note: row.note ? row.note.trim() : null,
                  loyaltyRedemption: row.loyaltyRedemption === true ? true : null
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
                  return res.json().catch(function () { return {}; }).then(function (body) {
                    throw new Error((body && body.error) ? body.error : 'Lưu đơn thất bại');
                  });
                }
                return res.json();
              })
              .then(function () {
                createModal.hide();
                setTimeout(function () { window.location.reload(); }, 350);
              })
              .catch(function (err) {
                alert(err && err.message ? err.message : 'Có lỗi xảy ra khi lưu đơn hàng. Vui lòng thử lại.');
              });
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

      function getTodayLocalYyyyMmDd() {
        var d = new Date();
        var p = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
      }

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
            return row.getAttribute('data-sort-table') || normalizeOrderText(cells[3] ? cells[3].textContent : '');
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

      function countOrdersByDateScope() {
        var pendingD = 0;
        var completedD = 0;
        var cancelledD = 0;
        var unknownD = 0;
        var rows = document.querySelectorAll('.orders-table tbody tr.order-data-row');
        rows.forEach(function (row) {
          if (!rowMatchesOrderDate(row)) return;
          var st = row.getAttribute('data-order-status') || '';
          if (st === 'PENDING') pendingD++;
          else if (st === 'COMPLETED') completedD++;
          else if (st === 'CANCELLED') cancelledD++;
          else unknownD++;
        });
        return {
          pending: pendingD,
          completed: completedD,
          cancelled: cancelledD,
          unknown: unknownD,
          totalOnDay: pendingD + completedD + cancelledD + unknownD
        };
      }

      function updateOrderStatCards() {
        var totalEl = document.getElementById('orderStatTotal');
        var pendEl = document.getElementById('orderStatPending');
        var compEl = document.getElementById('orderStatCompleted');
        var cancEl = document.getElementById('orderStatCancelled');
        if (!totalEl || !pendEl || !compEl || !cancEl) return;

        var c = countOrdersByDateScope();
        var tab = getActiveOrderFilterKey();
        var totalForTab;
        if (tab === 'ALL') {
          totalForTab = c.totalOnDay;
        } else if (tab === 'PENDING') {
          totalForTab = c.pending;
        } else if (tab === 'COMPLETED') {
          totalForTab = c.completed;
        } else if (tab === 'CANCELLED') {
          totalForTab = c.cancelled;
        } else {
          totalForTab = c.totalOnDay;
        }

        totalEl.textContent = String(totalForTab);
        pendEl.textContent = String(c.pending);
        compEl.textContent = String(c.completed);
        cancEl.textContent = String(c.cancelled);
      }

      function applyOrderFilters() {
        var filterKey = getActiveOrderFilterKey();
        var term = normalizeOrderText(orderSearchInput ? orderSearchInput.value : '');
        var rows = document.querySelectorAll('.orders-table tbody tr.order-data-row');
        rows.forEach(function (row) {
          var show = rowMatchesOrderFilter(row, filterKey) && rowMatchesOrderSearch(row, term) && rowMatchesOrderDate(row);
          row.style.display = show ? '' : 'none';
        });
        updateOrderStatCards();
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
        orderDateInput.value = getTodayLocalYyyyMmDd();
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

      var preferPendingFirst = !!document.getElementById('orderPageCanMarkPrepComplete');
      if (preferPendingFirst) {
        sortPendingFirst();
        var pendingBtn = Array.prototype.slice.call(filterBtns).find(function (b) {
          return b.getAttribute('data-order-filter') === 'PENDING';
        });
        if (pendingBtn) {
          filterBtns.forEach(function (b) { b.classList.remove('active'); });
          pendingBtn.classList.add('active');
        }
      }
      applyOrderFilters();

      // Admin / Pha chế: nút "Sẵn sàng phục vụ" (PENDING -> COMPLETED)
      var readyBtns = document.querySelectorAll('.barista-ready-btn');
      readyBtns.forEach(function (btn) {
        btn.addEventListener('click', function () {
          var code = this.getAttribute('data-code');
          if (!code) return;
          if (typeof window.showConfirmCompleteModal === 'function') {
            window.showConfirmCompleteModal(code);
          } else {
            if (!confirm('Đánh dấu đơn ' + code + ' là "Hoàn thành"?')) return;
            markOrderStatusCompletedThenReload(code);
          }
        });
      });
});
