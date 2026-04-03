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
        if (!cells || cells.length < 8) return '';
        switch (key) {
          case 'name':
            return normalize(cells[0] ? cells[0].textContent : '');
          case 'phone':
            return normalize(cells[1] ? cells[1].textContent : '');
          case 'email':
            return normalize(cells[2] ? cells[2].textContent : '');
          case 'bookingTime':
            return row.getAttribute('data-sort-booking') || (cells[3] ? cells[3].textContent.trim() : '');
          case 'guests': {
            var g = parseInt((cells[4] && cells[4].textContent) || '0', 10);
            return isFinite(g) ? g : 0;
          }
          case 'note':
            return normalize(cells[5] ? cells[5].textContent : '');
          case 'table':
            return normalize(cells[6] ? cells[6].textContent : '');
          case 'created':
            return row.getAttribute('data-sort-created') || (cells[7] ? cells[7].textContent.trim() : '');
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
    });
