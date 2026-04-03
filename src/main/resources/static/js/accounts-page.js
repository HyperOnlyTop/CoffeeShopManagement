document.addEventListener('DOMContentLoaded', function () {
      const customersTableBody = document.getElementById('customersTableBody');
      const customerSearchInput = document.getElementById('customerSearchInput');
      const btnReloadCustomers = document.getElementById('btnReloadCustomers');
      const customerSegmentFilters = document.getElementById('customerSegmentFilters');
      const customerPageSubtitle = document.getElementById('customerPageSubtitle');
      const customerStatTotal = document.getElementById('customerStatTotal');
      const customerStatRedeemReady = document.getElementById('customerStatRedeemReady');
      const customerStatAccumulating = document.getElementById('customerStatAccumulating');
      const customerStatRedemptionsSum = document.getElementById('customerStatRedemptionsSum');

      let customersCache = [];

      const bookingHistoryModalEl = document.getElementById('bookingHistoryModal');
      const bookingHistoryPhoneText = document.getElementById('bookingHistoryPhoneText');
      const bookingHistoryBody = document.getElementById('bookingHistoryBody');
      let bookingHistoryModalInstance = null;

      if (bookingHistoryModalEl && typeof bootstrap !== 'undefined') {
        bookingHistoryModalInstance = bootstrap.Modal.getOrCreateInstance(bookingHistoryModalEl);
      }

      function num(v) {
        const n = v != null ? Number(v) : 0;
        return isFinite(n) ? n : 0;
      }

      function normalize(text) {
        return text ? text.toString().toLowerCase().trim() : '';
      }

      function getActiveSegment() {
        if (!customerSegmentFilters) return 'ALL';
        const active = customerSegmentFilters.querySelector('.stock-filter.active');
        return active ? (active.getAttribute('data-segment') || 'ALL') : 'ALL';
      }

      function matchesSegment(c, seg) {
        const p = num(c.loyaltyPoints);
        const r = num(c.loyaltyRedeemedCount);
        switch (seg) {
          case 'ALL':
            return true;
          case 'REDEEM_READY':
            return p >= 10;
          case 'ACCUMULATING':
            return p >= 1 && p <= 9;
          case 'NO_POINTS':
            return p === 0;
          case 'REDEEMED_BEFORE':
            return r > 0;
          default:
            return true;
        }
      }

      function updateCustomerStats() {
        const list = customersCache;
        const total = list.length;
        let ready = 0;
        let accumulating = 0;
        let redemptionsSum = 0;
        for (let i = 0; i < list.length; i++) {
          const c = list[i];
          const p = num(c.loyaltyPoints);
          if (p >= 10) ready++;
          if (p >= 1 && p <= 9) accumulating++;
          redemptionsSum += num(c.loyaltyRedeemedCount);
        }
        if (customerStatTotal) customerStatTotal.textContent = String(total);
        if (customerStatRedeemReady) customerStatRedeemReady.textContent = String(ready);
        if (customerStatAccumulating) customerStatAccumulating.textContent = String(accumulating);
        if (customerStatRedemptionsSum) customerStatRedemptionsSum.textContent = String(redemptionsSum);
        if (customerPageSubtitle) {
          customerPageSubtitle.textContent = total === 0
            ? 'Chưa có khách hàng trong hệ thống'
            : total + ' khách hàng trong hệ thống';
        }
      }

      function applyCustomerFilters() {
        const seg = getActiveSegment();
        const term = normalize(customerSearchInput ? customerSearchInput.value : '');
        const filtered = customersCache.filter(function (c) {
          if (!matchesSegment(c, seg)) return false;
          if (!term) return true;
          const hay = normalize((c.name || '') + ' ' + (c.phone || '') + ' ' + (c.email || ''));
          return hay.indexOf(term) !== -1;
        });
        renderCustomers(filtered);
      }

      function renderCustomers(customers) {
        if (!customersTableBody) return;
        if (!customers || customers.length === 0) {
          const emptyMsg = customersCache.length === 0
            ? 'Chưa có khách hàng nào.'
            : 'Không có khách hàng phù hợp bộ lọc hoặc từ khóa tìm kiếm.';
          customersTableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-3">' + emptyMsg + '</td></tr>';
          return;
        }

        const rows = customers.map(function (c) {
          const id = c.id != null ? c.id : '';
          const name = c.name || '';
          const phone = c.phone || '';
          const email = c.email || '';
          const points = num(c.loyaltyPoints);
          const redeemed = num(c.loyaltyRedeemedCount);
          const canRedeem = points >= 10 && phone;

          return '<tr data-phone="' + phone + '">' +
            '<td>' + id + '</td>' +
            '<td>' + name + '</td>' +
            '<td>' + phone + '</td>' +
            '<td>' + email + '</td>' +
            '<td class="text-end"><strong>' + points + '</strong></td>' +
            '<td class="text-end">' + redeemed + '</td>' +
            '<td class="customer-actions-cell">' +
            '<div class="customer-actions-inner">' +
            '<button type="button" class="btn btn-sm btn-outline-primary btn-booking-history" ' + (phone ? '' : 'disabled') + '>' +
            '<i class="bi bi-clock-history me-1"></i> Lịch sử' +
            '</button>' +
            '<button type="button" class="btn btn-sm btn-warning btn-redeem" ' + (canRedeem ? '' : 'disabled') + '>' +
            '<i class="bi bi-gift me-1"></i> Đổi 10 điểm' +
            '</button>' +
            '<button type="button" class="btn btn-sm btn-outline-secondary btn-adjust" data-delta="1" ' + (phone ? '' : 'disabled') + '>+1</button>' +
            '<button type="button" class="btn btn-sm btn-outline-secondary btn-adjust" data-delta="-1" ' + (phone ? '' : 'disabled') + '>-1</button>' +
            '</div></td>' +
            '</tr>';
        }).join('');

        customersTableBody.innerHTML = rows;
      }

      function loadCustomers() {
        if (!customersTableBody) return;
        customersTableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-3">Đang tải danh sách khách hàng...</td></tr>';
        if (customerPageSubtitle) customerPageSubtitle.textContent = 'Đang tải dữ liệu…';

        fetch('/api/loyalty/customers')
          .then(function (res) {
            if (!res.ok) throw new Error('Không thể tải khách hàng');
            return res.json();
          })
          .then(function (data) {
            customersCache = Array.isArray(data) ? data : [];
            updateCustomerStats();
            applyCustomerFilters();
          })
          .catch(function () {
            customersTableBody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-3">Lỗi khi tải khách hàng.</td></tr>';
            if (customerPageSubtitle) customerPageSubtitle.textContent = 'Không tải được dữ liệu';
          });
      }

      function redeemByPhone(phone) {
        if (!phone) return;
        fetch('/api/loyalty/redeem', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone: phone })
        })
          .then(function (res) {
            return res.text().then(function (txt) { return { ok: res.ok, txt: txt }; });
          })
          .then(function (result) {
            if (!result.ok) {
              alert(result.txt || 'Đổi điểm thất bại');
              return;
            }
            loadCustomers();
            alert('Đã đổi 10 điểm thành công. (Nhớ chọn món < 50.000 VNĐ)');
          })
          .catch(function () {
            alert('Lỗi khi đổi điểm');
          });
      }

      function adjustPoints(phone, delta) {
        if (!phone) return;
        fetch('/api/loyalty/adjust', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone: phone, delta: delta })
        })
          .then(function (res) { return res.ok ? res.json() : null; })
          .then(function () { loadCustomers(); })
          .catch(function () { alert('Lỗi khi cập nhật điểm'); });
      }

      function formatDateTime(isoString) {
        if (!isoString) return '';
        const d = new Date(isoString);
        if (isNaN(d.getTime())) return isoString;
        return d.toLocaleString('vi-VN');
      }

      function openBookingHistory(phone) {
        if (!bookingHistoryModalInstance || !bookingHistoryBody) {
          return;
        }
        if (bookingHistoryPhoneText) bookingHistoryPhoneText.textContent = phone || '—';
        bookingHistoryBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Đang tải...</td></tr>';

        fetch('/api/bookings/by-phone?phone=' + encodeURIComponent(phone))
          .then(function (res) { return res.ok ? res.json() : []; })
          .then(function (data) {
            if (!Array.isArray(data) || data.length === 0) {
              bookingHistoryBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Chưa có đặt bàn nào theo SĐT này.</td></tr>';
              return;
            }
            const rows = data.map(function (b) {
              const bookingTime = formatDateTime(b.bookingTime);
              const guests = b.guests != null ? b.guests : '';
              const note = b.note || '';
              const createdAt = formatDateTime(b.createdAt);
              return '<tr>' +
                '<td>' + bookingTime + '</td>' +
                '<td class="text-center">' + guests + '</td>' +
                '<td>' + note + '</td>' +
                '<td>' + createdAt + '</td>' +
                '</tr>';
            }).join('');
            bookingHistoryBody.innerHTML = rows;
          })
          .catch(function () {
            bookingHistoryBody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3">Lỗi khi tải lịch sử đặt bàn.</td></tr>';
          });

        bookingHistoryModalInstance.show();
      }

      loadCustomers();

      if (btnReloadCustomers) {
        btnReloadCustomers.addEventListener('click', loadCustomers);
      }
      if (customerSearchInput) {
        customerSearchInput.addEventListener('input', applyCustomerFilters);
      }
      if (customerSegmentFilters) {
        customerSegmentFilters.addEventListener('click', function (e) {
          const btn = e.target.closest('.stock-filter');
          if (!btn) return;
          customerSegmentFilters.querySelectorAll('.stock-filter').forEach(function (b) {
            b.classList.remove('active');
          });
          btn.classList.add('active');
          applyCustomerFilters();
        });
      }
      if (customersTableBody) {
        customersTableBody.addEventListener('click', function (e) {
          const redeemBtn = e.target.closest('.btn-redeem');
          const adjustBtn = e.target.closest('.btn-adjust');
          const bookingBtn = e.target.closest('.btn-booking-history');
          const row = e.target.closest('tr');
          const phone = row ? row.getAttribute('data-phone') : null;

          if (bookingBtn && phone) {
            openBookingHistory(phone);
            return;
          }

          if (redeemBtn && phone) {
            if (confirm('Đổi 10 điểm lấy 1 ly nước (< 50.000 VNĐ)?')) {
              redeemByPhone(phone);
            }
            return;
          }

          if (adjustBtn && phone) {
            const delta = Number(adjustBtn.getAttribute('data-delta') || 0);
            if (delta) adjustPoints(phone, delta);
          }
        });
      }
    });
