    document.addEventListener('DOMContentLoaded', function () {
      if (!window.bootstrap) return;

      var modalEl = document.getElementById('tableDetailModal');
      var modal = modalEl ? new bootstrap.Modal(modalEl) : null;
      var titleEl = document.getElementById('tableDetailTitle');
      var bodyEl = document.getElementById('tableDetailBody');
      var releaseBtn = document.getElementById('btnReleaseTable');
      var activeTableNo = null;

      function renderDetail(data) {
        if (!bodyEl) return;
        if (!data || !data.order) {
          bodyEl.innerHTML = '<div class="text-muted">Bàn đang trống hoặc không có đơn hoạt động.</div>';
          if (releaseBtn) releaseBtn.style.display = 'none';
          return;
        }

        var order = data.order;
        var items = Array.isArray(data.items) ? data.items : [];

        var html = '';
        html += '<div class="mb-2"><strong>Mã đơn:</strong> ' + (order.orderCode || '') + '</div>';
        html += '<div class="mb-2"><strong>Khách:</strong> ' + (order.customerName || 'Khách lẻ') + (order.customerPhone ? (' · ' + order.customerPhone) : '') + '</div>';
        html += '<div class="mb-2"><strong>Trạng thái đơn:</strong> ' + (order.status || '') + '</div>';
        html += '<div class="mb-3"><strong>Ghi chú:</strong> ' + (order.tableName || '') + '</div>';

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
        if (releaseBtn) releaseBtn.style.display = 'inline-block';
      }

      function loadTableDetail(tableNo) {
        if (!bodyEl) return;
        bodyEl.textContent = 'Đang tải...';
        if (releaseBtn) releaseBtn.style.display = 'none';
        fetch('/api/tables/' + encodeURIComponent(tableNo) + '/active-order')
          .then(function (res) {
            if (!res.ok) return null;
            return res.json();
          })
          .then(function (data) {
            renderDetail(data);
          })
          .catch(function () {
            bodyEl.innerHTML = '<div class="text-danger">Không tải được chi tiết bàn.</div>';
          });
      }

      document.querySelectorAll('.table-card[data-table]').forEach(function (card) {
        card.addEventListener('click', function () {
          var tableNo = card.getAttribute('data-table');
          activeTableNo = tableNo;
          if (titleEl) titleEl.textContent = 'Bàn ' + tableNo;
          if (modal) modal.show();
          loadTableDetail(tableNo);
        });
      });

      if (releaseBtn) {
        releaseBtn.addEventListener('click', function () {
          if (!activeTableNo) return;
          fetch('/api/tables/' + encodeURIComponent(activeTableNo) + '/release', { method: 'POST' })
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function () {
              // reload để cập nhật màu bàn
              window.location.reload();
            })
            .catch(function () {
              alert('Không trả bàn được. Vui lòng thử lại.');
            });
        });
      }
    });
