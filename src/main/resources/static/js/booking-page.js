document.addEventListener('DOMContentLoaded', function () {
      var searchInput = document.getElementById('bookingSearchInput');
      var filterWrap = document.getElementById('bookingSegmentFilters');
      var bookingDateInput = document.getElementById('bookingDateFilterInput');
      var bookingDateLabel = document.getElementById('bookingDateFilterLabel');
      var bookingDateClear = document.getElementById('bookingDateFilterClear');
      var tbodyEl = document.querySelector('.booking-table tbody');
      var bookingTableHead = document.querySelector('.booking-table thead');
      var bookingSortKey = null;
      var bookingSortDir = 'asc';

      function normalize(text) {
        return text ? text.toString().toLowerCase().trim() : '';
      }

      function isBookingEmptyRow(row) {
        return !!(row && row.querySelector('.text-muted.py-4'));
      }

      function getBookingSortValue(row, key) {
        var cells = row.cells;
        if (!cells || cells.length < 6) return '';
        var nameEl = cells[0] ? cells[0].querySelector('.booking-customer-name') : null;
        switch (key) {
          case 'name':
            return normalize(nameEl ? nameEl.textContent : (cells[0] ? cells[0].textContent : ''));
          case 'phone':
            return normalize(row.getAttribute('data-sort-phone') || '');
          case 'email':
            return normalize(row.getAttribute('data-sort-email') || '');
          case 'bookingTime':
            return row.getAttribute('data-sort-booking') || (cells[1] ? cells[1].textContent.trim() : '');
          case 'status':
            return normalize(row.getAttribute('data-booking-status') || '');
          case 'guests': {
            var gs = row.getAttribute('data-sort-guests');
            var g = parseInt(gs != null && gs !== '' ? gs : '0', 10);
            return isFinite(g) ? g : 0;
          }
          case 'note':
            return normalize(row.getAttribute('data-sort-note') || '');
          case 'table':
            return normalize(cells[4] ? cells[4].textContent : '');
          case 'created':
            return row.getAttribute('data-sort-created') || (cells[5] ? cells[5].textContent.trim() : '');
          default:
            return '';
        }
      }

      function compareBookingSort(va, vb, key) {
        if (key === 'guests') {
          return va - vb;
        }
        return String(va).localeCompare(String(vb), 'vi', { sensitivity: 'base', numeric: true });
      }

      function sortBookingDataRows() {
        if (!tbodyEl || !bookingSortKey) return;
        var all = Array.prototype.slice.call(tbodyEl.querySelectorAll('tr'));
        var emptyRows = all.filter(isBookingEmptyRow);
        var dataRows = all.filter(function (r) { return r.classList.contains('booking-data-row'); });
        dataRows.sort(function (ra, rb) {
          var va = getBookingSortValue(ra, bookingSortKey);
          var vb = getBookingSortValue(rb, bookingSortKey);
          var c = compareBookingSort(va, vb, bookingSortKey);
          return bookingSortDir === 'asc' ? c : -c;
        });
        dataRows.forEach(function (r) { tbodyEl.appendChild(r); });
        emptyRows.forEach(function (r) { tbodyEl.appendChild(r); });
      }

      function updateBookingSortHeaders() {
        if (!bookingTableHead) return;
        bookingTableHead.querySelectorAll('.stock-th-sort').forEach(function (btn) {
          var key = btn.getAttribute('data-sort');
          var icon = btn.querySelector('.stock-sort-icon');
          if (!icon) return;
          if (bookingSortKey === key) {
            icon.className = 'bi stock-sort-icon ' + (bookingSortDir === 'asc' ? 'bi-sort-up' : 'bi-sort-down');
          } else {
            icon.className = 'bi bi-arrow-down-up stock-sort-icon';
          }
        });
      }

      function getActiveSegment() {
        if (!filterWrap) return 'ALL';
        var active = filterWrap.querySelector('.stock-filter.active');
        return active ? (active.getAttribute('data-booking-segment') || 'ALL') : 'ALL';
      }

      function matchesSegment(row, seg) {
        var status = row.getAttribute('data-booking-status') || '';
        var hasTable = row.getAttribute('data-has-table') === 'true';
        var upcoming = row.getAttribute('data-upcoming') === 'true';
        switch (seg) {
          case 'ALL':
            return true;
          case 'CONFIRMED':
            return status === 'CONFIRMED';
          case 'CANCELLED':
            return status === 'CANCELLED';
          case 'WITH_TABLE':
            return hasTable;
          case 'UPCOMING':
            return upcoming;
          default:
            return true;
        }
      }

      function rowMatchesBookingDate(row) {
        var sel = bookingDateInput && bookingDateInput.value ? bookingDateInput.value : '';
        if (!sel) return true;
        var d = row.getAttribute('data-booking-day') || '';
        return d === sel;
      }

      function refreshBookingDateLabel() {
        if (!bookingDateLabel) return;
        var v = bookingDateInput && bookingDateInput.value ? bookingDateInput.value : '';
        if (!v) {
          bookingDateLabel.textContent = 'Mọi ngày';
          return;
        }
        var p = v.split('-');
        bookingDateLabel.textContent = p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : v;
      }

      function updateBookingSubtitle() {
        var el = document.getElementById('bookingPageSubtitle');
        if (!el) return;
        var all = document.querySelectorAll('.booking-table tbody tr.booking-data-row');
        if (all.length === 0) {
          el.textContent = 'Chưa có yêu cầu đặt bàn';
          return;
        }
        var n = 0;
        all.forEach(function (r) {
          if (r.style.display !== 'none') n++;
        });
        el.textContent = n === 0 ? 'Không có đặt bàn phù hợp bộ lọc' : n + ' yêu cầu đang hiển thị';
      }

      function applyBookingFilters() {
        var seg = getActiveSegment();
        var term = normalize(searchInput ? searchInput.value : '');
        var rows = document.querySelectorAll('.booking-table tbody tr.booking-data-row');
        rows.forEach(function (row) {
          var matchSeg = matchesSegment(row, seg);
          var matchSearch = !term || normalize(row.textContent || '').indexOf(term) !== -1;
          var matchDate = rowMatchesBookingDate(row);
          row.style.display = matchSeg && matchSearch && matchDate ? '' : 'none';
        });
        updateBookingSubtitle();
      }

      if (bookingTableHead) {
        bookingTableHead.addEventListener('click', function (e) {
          var btn = e.target.closest('.stock-th-sort');
          if (!btn) return;
          var key = btn.getAttribute('data-sort');
          if (!key) return;
          if (bookingSortKey === key) {
            bookingSortDir = bookingSortDir === 'asc' ? 'desc' : 'asc';
          } else {
            bookingSortKey = key;
            bookingSortDir = 'asc';
          }
          sortBookingDataRows();
          updateBookingSortHeaders();
          applyBookingFilters();
        });
      }

      if (filterWrap) {
        filterWrap.addEventListener('click', function (e) {
          var btn = e.target.closest('.stock-filter');
          if (!btn) return;
          filterWrap.querySelectorAll('.stock-filter').forEach(function (b) {
            b.classList.remove('active');
          });
          btn.classList.add('active');
          applyBookingFilters();
        });
      }

      if (searchInput) {
        searchInput.addEventListener('input', applyBookingFilters);
      }

      if (bookingDateInput) {
        bookingDateInput.addEventListener('change', function () {
          refreshBookingDateLabel();
          applyBookingFilters();
        });
      }
      if (bookingDateClear) {
        bookingDateClear.addEventListener('click', function () {
          if (bookingDateInput) bookingDateInput.value = '';
          refreshBookingDateLabel();
          applyBookingFilters();
        });
      }
      refreshBookingDateLabel();
      applyBookingFilters();

      function scrollToBookingHash() {
        var h = window.location.hash || '';
        if (!h || h.indexOf('booking-row-') !== 1) return;
        var el = document.querySelector(h);
        if (!el) return;
        el.scrollIntoView({ block: 'center', behavior: 'smooth' });
        el.classList.add('booking-reminder-highlight');
        setTimeout(function () { el.classList.remove('booking-reminder-highlight'); }, 3000);
      }
      scrollToBookingHash();
      window.addEventListener('hashchange', scrollToBookingHash);

      var reminderWrap = document.getElementById('bookingReminderDropdownWrap');
      var reminderListEl = document.getElementById('bookingReminderList');
      var reminderEmptyEl = document.getElementById('bookingReminderEmpty');
      var reminderBadge = document.querySelector('.booking-reminder-badge');
      var reminderMarkAllBtn = document.getElementById('bookingReminderMarkAllRead');
      var bookingPageIsAdmin = document.getElementById('bookingPageIsAdmin');

      function setReminderBadge(n) {
        if (!reminderBadge) return;
        var c = Number(n) || 0;
        if (c > 0) {
          reminderBadge.textContent = c > 99 ? '99+' : String(c);
          reminderBadge.classList.remove('d-none');
        } else {
          reminderBadge.classList.add('d-none');
        }
      }

      function formatReminderTime(iso) {
        if (!iso) return '';
        var d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
      }

      function renderReminders(payload) {
        if (!reminderListEl) return;
        var list = (payload && payload.reminders) ? payload.reminders : [];
        var unread = payload && payload.unreadCount != null ? Number(payload.unreadCount) : 0;
        setReminderBadge(unread);
        reminderListEl.innerHTML = '';
        if (!list.length) {
          if (reminderEmptyEl) {
            reminderEmptyEl.classList.remove('d-none');
          }
          return;
        }
        if (reminderEmptyEl) reminderEmptyEl.classList.add('d-none');
        var isAdmin = bookingPageIsAdmin && bookingPageIsAdmin.textContent.trim() === '1';
        list.forEach(function (r) {
          var a = document.createElement('div');
          a.className = 'list-group-item booking-reminder-item' + (r.acknowledged ? '' : ' unread');
          var title = document.createElement('div');
          title.className = 'fw-medium';
          title.textContent = r.summary || '';
          var meta = document.createElement('div');
          meta.className = 'booking-reminder-time mt-1';
          meta.textContent = 'Gửi lúc ' + formatReminderTime(r.createdAt);
          var actions = document.createElement('div');
          actions.className = 'd-flex flex-wrap gap-2 mt-2 align-items-center';
          var link = document.createElement('a');
          link.className = 'small';
          link.href = '/Booking#booking-row-' + r.bookingId;
          link.textContent = 'Xem dòng đặt bàn';
          if (isAdmin) {
            link.href = '/Booking/edit/' + r.bookingId;
            link.textContent = 'Mở sửa / gán bàn';
          }
          actions.appendChild(link);
          if (!r.acknowledged) {
            var markBtn = document.createElement('button');
            markBtn.type = 'button';
            markBtn.className = 'btn btn-link btn-sm p-0 small text-decoration-none';
            markBtn.textContent = 'Đã đọc';
            markBtn.addEventListener('click', function () {
              fetch('/api/bookings/reminders/' + r.id + '/read', { method: 'POST' })
                .then(function (res) { return res.ok ? res.json() : null; })
                .then(function (body) {
                  if (body && body.unreadCount != null) setReminderBadge(body.unreadCount);
                  fetchReminders();
                });
            });
            actions.appendChild(markBtn);
          }
          a.appendChild(title);
          a.appendChild(meta);
          a.appendChild(actions);
          reminderListEl.appendChild(a);
        });
      }

      function fetchReminders() {
        if (!reminderListEl) return;
        fetch('/api/bookings/reminders')
          .then(function (res) { return res.ok ? res.json() : { reminders: [], unreadCount: 0 }; })
          .then(renderReminders)
          .catch(function () { renderReminders({ reminders: [], unreadCount: 0 }); });
      }

      if (reminderWrap && reminderListEl) {
        var initial = reminderWrap.getAttribute('data-initial-unread');
        if (initial != null && initial !== '') {
          setReminderBadge(parseInt(initial, 10));
        }
        fetchReminders();
        setInterval(fetchReminders, 60000);
        if (reminderMarkAllBtn) {
          reminderMarkAllBtn.addEventListener('click', function () {
            fetch('/api/bookings/reminders/read-all', { method: 'POST' })
              .then(function (res) { return res.ok ? res.json() : null; })
              .then(function () { fetchReminders(); });
          });
        }
      }

      var bookingCancelModalEl = document.getElementById('bookingCancelModal');
      var bookingCancelNoteInput = document.getElementById('bookingCancelNoteInput');
      var bookingCancelConfirmBtn = document.getElementById('bookingCancelConfirmBtn');
      var pendingCancelBookingId = null;
      var bookingCancelModal = null;
      if (bookingCancelModalEl && typeof window.bootstrap !== 'undefined' && window.bootstrap.Modal) {
        bookingCancelModal = new window.bootstrap.Modal(bookingCancelModalEl);
      }

      function postStaffAction(bookingId, action, note) {
        var url = '/api/bookings/' + encodeURIComponent(bookingId) + '/staff-action';
        var opts = { method: 'POST' };
        if (action === 'CANCEL') {
          opts.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
          var p = new URLSearchParams();
          p.set('action', 'CANCEL');
          if (note && String(note).trim()) p.set('note', String(note).trim());
          opts.body = p.toString();
        } else {
          url += '?action=' + encodeURIComponent(action);
        }
        return fetch(url, opts).then(function (res) {
          return res.json().then(function (body) {
            return { ok: res.ok, body: body };
          });
        });
      }

      document.addEventListener('click', function (e) {
        var checkin = e.target.closest('.booking-checkin-btn');
        if (checkin) {
          var cid = checkin.getAttribute('data-booking-id');
          if (!cid) return;
          postStaffAction(cid, 'CHECK_IN', null)
            .then(function (x) {
              if (!x.ok) {
                alert((x.body && x.body.error) ? x.body.error : 'Không cập nhật được.');
                return;
              }
              window.location.reload();
            })
            .catch(function () { alert('Lỗi mạng hoặc máy chủ.'); });
          return;
        }
        var openCancel = e.target.closest('.booking-cancel-open-btn');
        if (openCancel) {
          pendingCancelBookingId = openCancel.getAttribute('data-booking-id');
          if (bookingCancelNoteInput) bookingCancelNoteInput.value = '';
          if (bookingCancelModal) bookingCancelModal.show();
          return;
        }
      });

      if (bookingCancelConfirmBtn) {
        bookingCancelConfirmBtn.addEventListener('click', function () {
          if (!pendingCancelBookingId) return;
          var note = bookingCancelNoteInput ? bookingCancelNoteInput.value : '';
          postStaffAction(pendingCancelBookingId, 'CANCEL', note)
            .then(function (x) {
              if (!x.ok) {
                alert((x.body && x.body.error) ? x.body.error : 'Không hủy được.');
                return;
              }
              pendingCancelBookingId = null;
              if (bookingCancelModal) bookingCancelModal.hide();
              window.location.reload();
            })
            .catch(function () { alert('Lỗi mạng hoặc máy chủ.'); });
        });
      }
    });
