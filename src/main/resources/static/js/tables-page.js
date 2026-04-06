document.addEventListener('DOMContentLoaded', function () {
  if (!window.bootstrap) return;

  var modalEl = document.getElementById('tableDetailModal');
  var modal = modalEl ? new bootstrap.Modal(modalEl) : null;
  var titleEl = document.getElementById('tableDetailTitle');
  var bodyEl = document.getElementById('tableDetailBody');
  var pendingSection = document.getElementById('pendingBookingsSection');
  var pendingList = document.getElementById('pendingBookingsList');

  var btnRelease = document.getElementById('btnReleaseTable');
  var btnCheckin = document.getElementById('btnCheckinBooking');
  var btnUnassign = document.getElementById('btnUnassignBooking');
  var btnCancel = document.getElementById('btnCancelBooking');

  var cancelModalEl = document.getElementById('cancelBookingModal');
  var cancelModal = cancelModalEl ? new bootstrap.Modal(cancelModalEl) : null;
  var cancelNoteInput = document.getElementById('cancelBookingNoteInput');
  var confirmCancelBtn = document.getElementById('confirmCancelBookingBtn');

  var activeTableNo = null;
  var activeTableStatus = null;

  function hideAllButtons() {
    if (btnRelease) btnRelease.style.display = 'none';
    if (btnCheckin) btnCheckin.style.display = 'none';
    if (btnUnassign) btnUnassign.style.display = 'none';
    if (btnCancel) btnCancel.style.display = 'none';
    if (pendingSection) pendingSection.style.display = 'none';
  }

  function formatDateTime(isoStr) {
    if (!isoStr) return '';
    try {
      var d = new Date(isoStr);
      var day = String(d.getDate()).padStart(2, '0');
      var month = String(d.getMonth() + 1).padStart(2, '0');
      var year = d.getFullYear();
      var hour = String(d.getHours()).padStart(2, '0');
      var min = String(d.getMinutes()).padStart(2, '0');
      return day + '/' + month + '/' + year + ' ' + hour + ':' + min;
    } catch (e) {
      return isoStr;
    }
  }

  function renderOccupiedDetail(data) {
    if (!bodyEl) return;
    if (!data || !data.order) {
      bodyEl.innerHTML = '<div class="text-muted">Không có đơn hoạt động.</div>';
      return;
    }

    var order = data.order;
    var items = Array.isArray(data.items) ? data.items : [];

    var html = '';
    html += '<div class="mb-2"><strong>Mã đơn:</strong> ' + (order.orderCode || '') + '</div>';
    html += '<div class="mb-2"><strong>Khách:</strong> ' + (order.customerName || 'Khách lẻ') + (order.customerPhone ? (' · ' + order.customerPhone) : '') + '</div>';
    html += '<div class="mb-2"><strong>Trạng thái đơn:</strong> ' + (order.status || '') + '</div>';
    html += '<div class="mb-3"><strong>Ghi chú đơn:</strong> ' + (order.orderNote || '') + '</div>';

    html += '<div class="table-responsive">';
    html += '<table class="table table-sm align-middle mb-0">';
    html += '<thead><tr><th>Món</th><th class="text-center" style="width:90px;">SL</th><th>Ghi chú</th></tr></thead><tbody>';
    if (!items.length) {
      html += '<tr><td colspan="3" class="text-muted">Chưa có món.</td></tr>';
    } else {
      items.forEach(function (it) {
        if (!it) return;
        html += '<tr>';
        html += '<td>' + (it.itemName || '') + '</td>';
        html += '<td class="text-center">' + (it.quantity != null ? it.quantity : '') + '</td>';
        html += '<td>' + (it.note || '') + '</td>';
        html += '</tr>';
      });
    }
    html += '</tbody></table></div>';

    bodyEl.innerHTML = html;
    if (btnRelease) btnRelease.style.display = 'inline-block';
  }

  function renderReservedDetail(tableData) {
    if (!bodyEl) return;
    var html = '<div class="alert alert-info py-2 px-3 mb-3">';
    html += '<i class="bi bi-calendar-event me-1"></i> Bàn đang được giữ cho booking';
    html += '</div>';
    html += '<div class="mb-2"><strong>Khách:</strong> ' + (tableData.bookingName || 'Không rõ') + '</div>';
    html += '<div class="mb-2"><strong>SĐT:</strong> ' + (tableData.bookingPhone || '') + '</div>';
    html += '<div class="mb-2"><strong>Giờ đặt:</strong> ' + formatDateTime(tableData.bookingTime) + '</div>';
    bodyEl.innerHTML = html;

    if (btnCheckin) btnCheckin.style.display = 'inline-block';
    if (btnUnassign) btnUnassign.style.display = 'inline-block';
    if (btnCancel) btnCancel.style.display = 'inline-block';
  }

  function renderAvailableDetail() {
    if (!bodyEl) return;
    bodyEl.innerHTML = '<div class="text-muted"><i class="bi bi-check-circle me-1"></i> Bàn đang trống, sẵn sàng phục vụ.</div>';
    loadPendingBookings();
  }

  function loadPendingBookings() {
    if (!pendingSection || !pendingList) return;
    pendingList.innerHTML = '<div class="text-muted">Đang tải...</div>';
    pendingSection.style.display = 'block';

    fetch('/api/tables/pending-bookings')
      .then(function (res) { return res.ok ? res.json() : []; })
      .then(function (bookings) {
        if (!bookings || !bookings.length) {
          pendingList.innerHTML = '<div class="text-muted">Không có booking nào chờ gán bàn.</div>';
          return;
        }
        var html = '<div class="list-group list-group-flush">';
        bookings.forEach(function (b) {
          html += '<div class="list-group-item d-flex justify-content-between align-items-center px-0 py-2">';
          html += '<div>';
          html += '<div class="fw-medium">' + (b.name || 'Khách') + '</div>';
          html += '<div class="small text-muted">' + formatDateTime(b.bookingTime) + ' · ' + (b.guests || 0) + ' khách</div>';
          html += '</div>';
          html += '<button type="button" class="btn btn-sm btn-outline-primary assign-booking-btn" data-booking-id="' + b.id + '">';
          html += '<i class="bi bi-pin-angle me-1"></i>Gán bàn</button>';
          html += '</div>';
        });
        html += '</div>';
        pendingList.innerHTML = html;

        pendingList.querySelectorAll('.assign-booking-btn').forEach(function (btn) {
          btn.addEventListener('click', function () {
            var bookingId = btn.getAttribute('data-booking-id');
            assignBookingToTable(bookingId);
          });
        });
      })
      .catch(function () {
        pendingList.innerHTML = '<div class="text-danger">Không tải được danh sách booking.</div>';
      });
  }

  function assignBookingToTable(bookingId) {
    if (!activeTableNo || !bookingId) return;
    fetch('/api/tables/' + encodeURIComponent(activeTableNo) + '/assign-booking?bookingId=' + encodeURIComponent(bookingId), { method: 'POST' })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        if (data.ok) {
          window.location.reload();
        } else {
          alert(data.error || 'Không gán được bàn.');
        }
      })
      .catch(function () {
        alert('Lỗi khi gán bàn. Vui lòng thử lại.');
      });
  }

  function loadTableDetail(tableNo, tableStatus, tableData) {
    if (!bodyEl) return;
    hideAllButtons();
    bodyEl.textContent = 'Đang tải...';

    activeTableStatus = tableStatus;

    if (tableStatus === 'OCCUPIED') {
      fetch('/api/tables/' + encodeURIComponent(tableNo) + '/active-order')
        .then(function (res) {
          if (res.status === 404) return null;
          if (!res.ok) throw new Error('HTTP ' + res.status);
          return res.json();
        })
        .then(function (data) {
          renderOccupiedDetail(data);
        })
        .catch(function () {
          bodyEl.innerHTML = '<div class="text-danger">Không tải được chi tiết đơn.</div>';
        });
    } else if (tableStatus === 'RESERVED') {
      renderReservedDetail(tableData);
    } else {
      renderAvailableDetail();
    }
  }

  function getTableDataFromCard(card) {
    var badge = card.querySelector('.table-badge');
    var status = 'AVAILABLE';
    if (badge) {
      if (badge.classList.contains('table-badge-occupied')) status = 'OCCUPIED';
      else if (badge.classList.contains('table-badge-reserved')) status = 'RESERVED';
    }

    var data = { status: status };

    if (status === 'RESERVED') {
      var metaEls = card.querySelectorAll('.table-meta');
      metaEls.forEach(function (el) {
        var text = el.textContent || '';
        if (text.includes('Khách:')) {
          var nameMatch = text.match(/Khách:\s*([^·]+)/);
          if (nameMatch) data.bookingName = nameMatch[1].trim();
          var phoneMatch = text.match(/SĐT:\s*(\S+)/);
          if (phoneMatch) data.bookingPhone = phoneMatch[1].trim();
        }
        if (text.includes('Giờ đặt:')) {
          var timeMatch = text.match(/Giờ đặt:\s*(.+)/);
          if (timeMatch) data.bookingTime = timeMatch[1].trim();
        }
      });
    }

    return data;
  }

  document.querySelectorAll('.table-card[data-table]').forEach(function (card) {
    card.style.cursor = 'pointer';
    card.addEventListener('click', function () {
      var tableNo = card.getAttribute('data-table');
      activeTableNo = tableNo;
      if (titleEl) titleEl.textContent = 'Bàn ' + tableNo;
      if (modal) modal.show();

      var tableData = getTableDataFromCard(card);
      loadTableDetail(tableNo, tableData.status, tableData);
    });
  });

  if (btnRelease) {
    btnRelease.addEventListener('click', function () {
      if (!activeTableNo) return;
      if (!confirm('Xác nhận trả bàn ' + activeTableNo + '?')) return;
      fetch('/api/tables/' + encodeURIComponent(activeTableNo) + '/release', { method: 'POST' })
        .then(function (res) { return res.ok ? res.json() : null; })
        .then(function () { window.location.reload(); })
        .catch(function () { alert('Không trả bàn được. Vui lòng thử lại.'); });
    });
  }

  if (btnCheckin) {
    btnCheckin.addEventListener('click', function () {
      if (!activeTableNo) return;
      if (!confirm('Xác nhận khách đã đến bàn ' + activeTableNo + '?')) return;
      fetch('/api/tables/' + encodeURIComponent(activeTableNo) + '/checkin-booking', { method: 'POST' })
        .then(function (res) { return res.json(); })
        .then(function (data) {
          if (data.ok) {
            window.location.reload();
          } else {
            alert(data.error || 'Không thể xác nhận.');
          }
        })
        .catch(function () { alert('Lỗi khi xác nhận. Vui lòng thử lại.'); });
    });
  }

  if (btnUnassign) {
    btnUnassign.addEventListener('click', function () {
      if (!activeTableNo) return;
      if (!confirm('Bỏ gán bàn ' + activeTableNo + ' khỏi booking? Booking vẫn còn nhưng không giữ bàn này nữa.')) return;
      fetch('/api/tables/' + encodeURIComponent(activeTableNo) + '/unassign-booking', { method: 'POST' })
        .then(function (res) { return res.json(); })
        .then(function (data) {
          if (data.ok) {
            window.location.reload();
          } else {
            alert(data.error || 'Không thể bỏ gán bàn.');
          }
        })
        .catch(function () { alert('Lỗi khi bỏ gán bàn. Vui lòng thử lại.'); });
    });
  }

  if (btnCancel) {
    btnCancel.addEventListener('click', function () {
      if (!activeTableNo) return;
      if (cancelNoteInput) cancelNoteInput.value = '';
      if (cancelModal) cancelModal.show();
    });
  }

  if (confirmCancelBtn) {
    confirmCancelBtn.addEventListener('click', function () {
      if (!activeTableNo) return;
      var note = cancelNoteInput ? cancelNoteInput.value.trim() : '';
      var url = '/api/tables/' + encodeURIComponent(activeTableNo) + '/cancel-booking';
      if (note) url += '?note=' + encodeURIComponent(note);

      fetch(url, { method: 'POST' })
        .then(function (res) { return res.json(); })
        .then(function (data) {
          if (data.ok) {
            window.location.reload();
          } else {
            alert(data.error || 'Không thể hủy booking.');
          }
        })
        .catch(function () { alert('Lỗi khi hủy booking. Vui lòng thử lại.'); });
    });
  }
});
