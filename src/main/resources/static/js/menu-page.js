document.addEventListener('DOMContentLoaded', function () {

  var BS = typeof bootstrap !== 'undefined' ? bootstrap : null;



  var addItemBtn = document.querySelector('.stock-import-btn, .add-item-btn');

  var addItemModalEl = document.getElementById('addItemModal');

  var editItemModalEl = document.getElementById('editItemModal');

  var searchInput = document.getElementById('menuSearchInput');

  var menuTableBody = document.getElementById('menuTableBody');

  var menuTableHead = document.querySelector('.menu-admin-table thead');



  var currentCategory = 'ALL';

  var selectedCard = null;

  var categoriesCache = [];

  var menuSortKey = null;

  var menuSortDir = 'asc';



  function normalize(text) {

    return text ? text.toString().toLowerCase().trim() : '';

  }



  function clearSelect(selectEl, placeholderText) {

    if (!selectEl) return;

    selectEl.innerHTML = '';

    var opt = document.createElement('option');

    opt.value = '';

    opt.disabled = true;

    opt.selected = true;

    opt.textContent = placeholderText || 'Chọn danh mục';

    selectEl.appendChild(opt);

  }



  function setSelectOptions(selectEl, values, placeholderText) {

    if (!selectEl) return;

    clearSelect(selectEl, placeholderText);

    (values || []).forEach(function (val) {

      if (!val) return;

      var opt = document.createElement('option');

      opt.value = val;

      opt.textContent = val;

      selectEl.appendChild(opt);

    });

  }



  function updateMenuSubtitle() {

    var el = document.getElementById('menuPageSubtitle');

    if (!el) return;

    var allData = document.querySelectorAll('#menuTableBody tr.menu-admin-row');

    if (allData.length === 0) {

      el.textContent = 'Chưa có món trong thực đơn';

      return;

    }

    var visible = document.querySelectorAll('#menuTableBody tr.menu-admin-row:not(.d-none)').length;

    el.textContent = visible === 0 ? 'Không có món phù hợp bộ lọc' : visible + ' món đang hiển thị';

  }



  function applyMenuFilters() {

    var term = normalize(searchInput ? searchInput.value : '');

    var rows = document.querySelectorAll('#menuTableBody tr.menu-admin-row');

    rows.forEach(function (row) {

      var cardCategory = normalize(row.getAttribute('data-category'));

      var nameEl = row.querySelector('.menu-item-title');

      var descEl = row.querySelector('.menu-desc-cell');

      var name = normalize(nameEl ? nameEl.textContent : '');

      var desc = normalize(descEl ? descEl.textContent : '');



      var matchesCategory = currentCategory === 'ALL' || cardCategory === normalize(currentCategory);

      var matchesSearch = !term || name.indexOf(term) !== -1 || desc.indexOf(term) !== -1;



      if (matchesCategory && matchesSearch) {

        row.classList.remove('d-none');

      } else {

        row.classList.add('d-none');

      }

    });

    updateMenuSubtitle();

  }



  function isMenuEmptyPlaceholder(row) {

    return row && row.classList.contains('menu-empty-placeholder');

  }



  function getMenuSortValue(row, key) {

    var cells = row.cells;

    if (!cells || cells.length < 8) return '';

    switch (key) {

      case 'name':

        return normalize(cells[1] ? cells[1].textContent : '');

      case 'category':

        return normalize(cells[2] ? cells[2].textContent : '');

      case 'description':

        return normalize(cells[3] ? cells[3].textContent : '');

      case 'price': {

        var t = cells[4] ? cells[4].textContent : '';

        var digits = String(t).replace(/\D/g, '');

        var n = parseInt(digits, 10);

        return isFinite(n) ? n : 0;

      }

      case 'cost': {

        var c = cells[5] ? cells[5].textContent : '';

        if (String(c).indexOf('—') !== -1 && String(c).replace(/\D/g, '') === '') return 0;

        var d = String(c).replace(/\D/g, '');

        var m = parseInt(d, 10);

        return isFinite(m) ? m : 0;

      }

      case 'status':

        return normalize(cells[6] ? cells[6].textContent : '');

      default:

        return '';

    }

  }



  function compareMenuSort(va, vb, key) {

    if (key === 'price' || key === 'cost') {

      return va - vb;

    }

    return String(va).localeCompare(String(vb), 'vi', { sensitivity: 'base', numeric: true });

  }



  function sortMenuDataRows() {

    if (!menuTableBody || !menuSortKey) return;

    var all = Array.prototype.slice.call(menuTableBody.querySelectorAll('tr'));

    var emptyRows = all.filter(isMenuEmptyPlaceholder);

    var dataRows = all.filter(function (r) { return r.classList.contains('menu-admin-row'); });

    dataRows.sort(function (ra, rb) {

      var va = getMenuSortValue(ra, menuSortKey);

      var vb = getMenuSortValue(rb, menuSortKey);

      var c = compareMenuSort(va, vb, menuSortKey);

      return menuSortDir === 'asc' ? c : -c;

    });

    dataRows.forEach(function (r) { menuTableBody.appendChild(r); });

    emptyRows.forEach(function (r) { menuTableBody.appendChild(r); });

  }



  function updateMenuSortHeaders() {

    if (!menuTableHead) return;

    menuTableHead.querySelectorAll('.stock-th-sort').forEach(function (btn) {

      var key = btn.getAttribute('data-sort');

      var icon = btn.querySelector('.stock-sort-icon');

      if (!icon) return;

      if (menuSortKey === key) {

        icon.className = 'bi stock-sort-icon ' + (menuSortDir === 'asc' ? 'bi-sort-up' : 'bi-sort-down');

      } else {

        icon.className = 'bi bi-arrow-down-up stock-sort-icon';

      }

    });

  }



  function renderCategoryFilters(categories) {

    var filtersContainer = document.getElementById('menuCategoryFilters');

    if (!filtersContainer) return;



    filtersContainer.querySelectorAll('button.stock-filter[data-category]').forEach(function (btn) {

      if (btn.getAttribute('data-category') !== 'ALL') {

        btn.remove();

      }

    });



    (categories || []).forEach(function (cat) {

      if (!cat) return;

      var btn = document.createElement('button');

      btn.type = 'button';

      btn.className = 'stock-filter';

      btn.setAttribute('data-category', cat);

      btn.textContent = cat;

      filtersContainer.appendChild(btn);

    });



    filtersContainer.querySelectorAll('.stock-filter').forEach(function (btn) {

      btn.addEventListener('click', function () {

        filtersContainer.querySelectorAll('.stock-filter').forEach(function (b) { b.classList.remove('active'); });

        btn.classList.add('active');

        currentCategory = btn.getAttribute('data-category') || 'ALL';

        applyMenuFilters();

      });

    });

  }



  function loadCategoriesFromDb() {

    return fetch('/api/menu/categories')

      .then(function (res) { return res.ok ? res.json() : []; })

      .then(function (data) {

        if (!Array.isArray(data)) {

          return [];

        }

        var names = data

          .map(function (c) { return c && c.name ? c.name : null; })

          .filter(function (s) { return s && s.toString().trim().length > 0; });

        var seen = {};

        var distinct = [];

        names.forEach(function (n) {

          var key = normalize(n);

          if (!key || seen[key]) return;

          seen[key] = true;

          distinct.push(n.toString().trim());

        });

        distinct.sort(function (a, b) { return a.localeCompare(b, 'vi', { sensitivity: 'base' }); });

        categoriesCache = distinct;

        renderCategoryFilters(categoriesCache);

        setSelectOptions(document.getElementById('addItemCategory'), categoriesCache, 'Chọn danh mục');

        setSelectOptions(document.getElementById('editItemCategory'), categoriesCache, 'Chọn danh mục');

        return categoriesCache;

      })

      .catch(function () {

        setSelectOptions(document.getElementById('addItemCategory'), [], 'Không tải được danh mục');

        setSelectOptions(document.getElementById('editItemCategory'), [], 'Không tải được danh mục');

        return [];

      });

  }



  function parseCostCellText(raw) {

    if (!raw) return '';

    var t = raw.replace(/^Giá vốn:\s*/i, '').trim();

    if (t === '—' || t === '-') return '';

    return t;

  }



  function syncToggleAvailabilityButton() {

    var statusEl = document.getElementById('editItemStatus');

    var btn = document.getElementById('toggleAvailabilityBtn');

    if (!btn) return;

    var statusText = statusEl ? statusEl.textContent.trim().toLowerCase() : '';

    var isOut = statusText.indexOf('tạm hết') !== -1;

    btn.textContent = isOut ? 'Hiển thị lại món' : 'Tạm ẩn món';

  }



  if (menuTableHead) {

    menuTableHead.addEventListener('click', function (e) {

      var btn = e.target.closest('.stock-th-sort');

      if (!btn) return;

      var key = btn.getAttribute('data-sort');

      if (!key) return;

      if (menuSortKey === key) {

        menuSortDir = menuSortDir === 'asc' ? 'desc' : 'asc';

      } else {

        menuSortKey = key;

        menuSortDir = 'asc';

      }

      sortMenuDataRows();

      updateMenuSortHeaders();

      applyMenuFilters();

    });

  }



  if (searchInput) {

    searchInput.addEventListener('input', applyMenuFilters);

  }



  if (addItemBtn && addItemModalEl && BS && BS.Modal) {

    var addItemModal = new BS.Modal(addItemModalEl);

    var addItemForm = document.getElementById('addItemForm');



    addItemBtn.addEventListener('click', function () {

      if (addItemForm) addItemForm.reset();

      addItemModal.show();

    });



    if (addItemForm) {

      addItemForm.addEventListener('submit', function (e) {

        e.preventDefault();

        var nameInput = document.getElementById('addItemName');

        var priceInput = document.getElementById('addItemPrice');

        var categorySelect = document.getElementById('addItemCategory');

        var descInput = document.getElementById('addItemDescription');

        var imageInput = document.getElementById('addItemImageUrl');

        var payload = {

          name: nameInput ? nameInput.value.trim() : '',

          price: priceInput && priceInput.value ? Number(priceInput.value) : null,

          categoryName: categorySelect ? categorySelect.value : null,

          description: descInput ? descInput.value.trim() : null,

          imageUrl: imageInput && imageInput.value ? imageInput.value.trim() : null,

          status: 'AVAILABLE'

        };

        fetch('/api/menu/items', {

          method: 'POST',

          headers: { 'Content-Type': 'application/json' },

          body: JSON.stringify(payload)

        }).then(function (res) {

          if (!res.ok) throw new Error('Lưu món thất bại');

          return res.json();

        }).then(function () {

          addItemModal.hide();

          alert('Đã lưu món mới vào hệ thống');

          window.location.reload();

        }).catch(function (err) {

          console.error(err);

          alert('Không thể lưu món mới. Vui lòng thử lại.');

        });

      });

    }

  }



  if (editItemModalEl && BS && BS.Modal) {

    var editItemModal = new BS.Modal(editItemModalEl);



    document.querySelectorAll('.menu-edit-btn').forEach(function (btn) {

      btn.addEventListener('click', function () {

        var card = this.closest('tr.menu-admin-row');

        if (!card) return;

        selectedCard = card;



        var name = card.querySelector('.menu-item-title') ? card.querySelector('.menu-item-title').textContent.trim() : '';

        var desc = card.querySelector('.menu-desc-cell') ? card.querySelector('.menu-desc-cell').textContent.trim() : '';

        var imgEl = card.querySelector('img.menu-admin-thumb') || card.querySelector('img');

        var imgSrc = imgEl ? imgEl.getAttribute('src') : '';

        var category = card.getAttribute('data-category') || '';

        var priceText = card.querySelector('.menu-price') ? card.querySelector('.menu-price').textContent.trim() : '';

        var costText = '';

        var costEl = card.querySelector('.menu-cost');

        if (costEl) costText = parseCostCellText(costEl.textContent);

        var badgeEl = card.querySelector('.menu-badge');

        var statusText = badgeEl ? badgeEl.textContent.trim() : '';



        var titleEl = document.getElementById('editItemModalLabel');

        if (titleEl) titleEl.textContent = name;

        var imageEl = document.getElementById('editItemImage');

        if (imageEl && imgSrc) {

          imageEl.src = imgSrc;

          imageEl.alt = name || 'Món';

        }

        var nameInput = document.getElementById('editItemName');

        if (nameInput) nameInput.value = name;

        var descInput = document.getElementById('editItemDescription');

        if (descInput) descInput.value = desc === '—' ? '' : desc;

        var priceInput = document.getElementById('editItemPriceInput');

        if (priceInput) priceInput.value = priceText ? priceText.replace(/[^0-9]/g, '') : '';

        var costInput = document.getElementById('editItemCostInput');

        if (costInput) costInput.value = costText ? costText.replace(/[^0-9]/g, '') : '';

        var categorySelect = document.getElementById('editItemCategory');

        if (categorySelect) {

          var target = category ? category.toString().trim() : '';

          var matched = false;

          Array.from(categorySelect.options).forEach(function (opt) {

            var same = normalize(opt.value || opt.textContent) === normalize(target);

            if (same) {

              opt.selected = true;

              matched = true;

            }

          });

          if (!matched && target) categorySelect.value = target;

        }

        var imgUrlInput = document.getElementById('editItemImageUrl');

        if (imgUrlInput) imgUrlInput.value = imgSrc || '';

        var statusEl = document.getElementById('editItemStatus');

        if (statusEl) {

          var isOut = statusText && statusText.toLowerCase().indexOf('tạm hết') !== -1;

          statusEl.textContent = isOut ? 'Tạm hết' : 'Có sẵn';

          statusEl.className = 'badge rounded-pill px-3 py-2 ' + (isOut ? 'bg-secondary-subtle text-secondary' : 'bg-success-subtle text-success');

        }

        syncToggleAvailabilityButton();

        editItemModal.show();

      });

    });



    var toggleBtn = document.getElementById('toggleAvailabilityBtn');

    if (toggleBtn) {

      toggleBtn.addEventListener('click', function () {

        if (!selectedCard) return;

        var idStr = selectedCard.getAttribute('data-item-id');

        if (!idStr) {

          alert('Không xác định được món (thiếu ID). Vui lòng tải lại trang.');

          return;

        }

        var id = parseInt(idStr, 10);

        if (!isFinite(id)) {

          alert('ID món không hợp lệ.');

          return;

        }

        var statusEl = document.getElementById('editItemStatus');

        var isOut = statusEl && statusEl.textContent.trim().toLowerCase().indexOf('tạm hết') !== -1;

        var newStatus = isOut ? 'AVAILABLE' : 'UNAVAILABLE';

        fetch('/api/menu/items/status', {

          method: 'POST',

          headers: { 'Content-Type': 'application/json' },

          body: JSON.stringify({ id: id, status: newStatus })

        }).then(function (res) {

          if (!res.ok) throw new Error('Cập nhật trạng thái thất bại');

          return res.json();

        }).then(function () {

          alert(newStatus === 'AVAILABLE' ? 'Đã hiển thị lại món trong hệ thống' : 'Đã tạm ẩn món trong hệ thống');

          editItemModal.hide();

          window.location.reload();

        }).catch(function (err) {

          console.error(err);

          alert('Không thể cập nhật trạng thái món. Vui lòng thử lại.');

        });

      });

    }



    var editSaveBtn = document.getElementById('editItemSaveBtn');

    if (editSaveBtn) {

      editSaveBtn.addEventListener('click', function () {

        if (!selectedCard) return;

        var idStr = selectedCard.getAttribute('data-item-id');

        if (!idStr) {

          alert('Không xác định được món (thiếu ID). Vui lòng tải lại trang.');

          return;

        }

        var itemId = parseInt(idStr, 10);

        if (!isFinite(itemId)) {

          alert('ID món không hợp lệ.');

          return;

        }



        var nameInput = document.getElementById('editItemName');

        var descInput = document.getElementById('editItemDescription');

        var priceInput = document.getElementById('editItemPriceInput');

        var costInput = document.getElementById('editItemCostInput');

        var categorySelect = document.getElementById('editItemCategory');

        var imgUrlInput = document.getElementById('editItemImageUrl');

        var statusBadge = document.getElementById('editItemStatus');



        function toNumber(value) {

          if (!value) return null;

          var v = value.toString().trim();

          return v ? Number(v) : null;

        }



        var name = nameInput ? nameInput.value.trim() : '';

        var desc = descInput ? descInput.value.trim() : '';

        var category = categorySelect ? categorySelect.value : null;

        var price = toNumber(priceInput ? priceInput.value : null);

        var cost = toNumber(costInput ? costInput.value : null);

        var imgUrl = imgUrlInput && imgUrlInput.value ? imgUrlInput.value.trim() : null;

        var statusText = statusBadge ? statusBadge.textContent.trim().toLowerCase() : '';



        var payload = {

          id: itemId,

          name: name,

          description: desc,

          categoryName: category,

          price: price,

          cost: cost,

          imageUrl: imgUrl,

          status: statusText.indexOf('tạm hết') !== -1 ? 'UNAVAILABLE' : 'AVAILABLE'

        };



        fetch('/api/menu/items', {

          method: 'POST',

          headers: { 'Content-Type': 'application/json' },

          body: JSON.stringify(payload)

        }).then(function (res) {

          if (!res.ok) throw new Error('Lưu món thất bại');

          return res.json();

        }).then(function () {

          alert('Đã lưu thông tin món vào hệ thống');

          editItemModal.hide();

          window.location.reload();

        }).catch(function (err) {

          console.error(err);

          alert('Không thể lưu món. Vui lòng thử lại.');

        });

      });

    }

  }



  loadCategoriesFromDb().then(function () { applyMenuFilters(); });

});

