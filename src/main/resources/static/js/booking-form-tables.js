/**
 * Lưới chọn bàn giữ chỗ trên form đặt bàn (admin), đồng bộ GET /api/tables/status với Quản lý bàn / tạo đơn.
 * Bàn đang có khách (OCCUPIED) không cho chọn.
 */
(function () {
  var grid = document.getElementById('bookingTablePickerGrid');
  var hidden = document.getElementById('bookingReservedTableNumber');
  var hint = document.getElementById('bookingTablePickerHint');
  var reloadBtn = document.getElementById('bookingReloadTablesBtn');
  var clearBtn = document.getElementById('bookingClearTableBtn');
  var bookingTimeInput = document.getElementById('bookingTimeAdmin');

  if (!grid || !hidden) return;

  function parseHidden() {
    var v = hidden.value;
    if (v === '' || v == null) return null;
    var n = Number(v);
    return Number.isFinite(n) ? n : null;
  }

  var selectedTableNumber = parseHidden();

  function setHidden(n) {
    selectedTableNumber = n;
    hidden.value = n == null ? '' : String(n);
  }

  function renderTablesStatus(list) {
    grid.innerHTML = '';

    if (!Array.isArray(list) || list.length === 0) {
      var empty = document.createElement('div');
      empty.className = 'text-muted small';
      empty.textContent = 'Không có dữ liệu bàn.';
      grid.appendChild(empty);
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

      var occupied = status === 'OCCUPIED';
      if (occupied) {
        btn.disabled = true;
      }

      if (selectedTableNumber != null && Number(selectedTableNumber) === Number(t.number)) {
        btn.classList.add('table-selected');
        if (occupied && hint) {
          hint.textContent = 'Bàn ' + t.number + ' đang có khách — không thể giữ bàn này. Chọn bàn khác hoặc bỏ gán bàn.';
        }
      }

      btn.addEventListener('click', function () {
        if (btn.disabled) return;
        selectedTableNumber = Number(t.number);
        hidden.value = String(selectedTableNumber);
        Array.prototype.forEach.call(grid.querySelectorAll('.table-btn'), function (b) {
          b.classList.remove('table-selected');
        });
        btn.classList.add('table-selected');
        if (hint) {
          if (status === 'RESERVED') {
            hint.textContent = 'Đã chọn bàn ' + selectedTableNumber + ' (đang hiển thị xanh nếu có đặt trong cửa sổ 15 phút quanh thời điểm hiện tại). Trùng giờ đặt sẽ được kiểm tra khi Lưu.';
          } else {
            hint.textContent = 'Đã chọn giữ bàn ' + selectedTableNumber + '.';
          }
        }
      });

      grid.appendChild(btn);
    });
  }

  function loadTableStatus() {
    grid.innerHTML = '<div class="text-muted small">Đang tải danh sách bàn...</div>';
    fetch('/api/tables/status?total=20')
      .then(function (res) { return res.ok ? res.json() : []; })
      .then(function (data) {
        selectedTableNumber = parseHidden();
        renderTablesStatus(data);
      })
      .catch(function () {
        grid.innerHTML = '<div class="text-muted small">Không tải được trạng thái bàn.</div>';
      });
  }

  if (reloadBtn) {
    reloadBtn.addEventListener('click', function () { loadTableStatus(); });
  }

  if (clearBtn) {
    clearBtn.addEventListener('click', function () {
      setHidden(null);
      loadTableStatus();
      if (hint) hint.textContent = 'Đã bỏ gán bàn (tuỳ chọn).';
    });
  }

  if (bookingTimeInput) {
    bookingTimeInput.addEventListener('change', function () { loadTableStatus(); });
  }

  loadTableStatus();
})();
