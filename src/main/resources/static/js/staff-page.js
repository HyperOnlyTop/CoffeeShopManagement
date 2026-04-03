        document.addEventListener('DOMContentLoaded', function () {
            let attendanceStatus = 'NOT_CHECKED_IN';
            var checkinBtn = document.getElementById('checkinBtn');
            if (checkinBtn) {
                fetch('/api/attendance/today')
                    .then(function (res) { return res.json(); })
                    .then(function (data) {
                        attendanceStatus = data.status || 'NOT_CHECKED_IN';
                        var textSpan = document.getElementById('checkinText');
                        if (attendanceStatus === 'NOT_CHECKED_IN') {
                            textSpan.textContent = 'Chấm công (Giờ vào)';
                            checkinBtn.classList.add('btn-primary');
                            checkinBtn.style.display = 'inline-block';
                        } else if (attendanceStatus === 'CHECKED_IN') {
                            textSpan.textContent = 'Chấm công ra (Giờ ra)';
                            checkinBtn.classList.remove('btn-primary');
                            checkinBtn.classList.add('btn-warning');
                            checkinBtn.style.display = 'inline-block';
                        } else if (attendanceStatus === 'COMPLETED') {
                            textSpan.textContent = 'Đã hoàn thành (' + data.workHours + ' giờ)';
                            checkinBtn.classList.remove('btn-primary', 'btn-warning');
                            checkinBtn.classList.add('btn-success');
                            checkinBtn.disabled = true;
                            checkinBtn.style.display = 'inline-block';
                        }
                    })
                    .catch(function (err) { console.error(err); });
            }

            window.staffCheckInOut = function () {
                var url = attendanceStatus === 'NOT_CHECKED_IN' ? '/api/attendance/check-in' : '/api/attendance/check-out';
                fetch(url, { method: 'POST' })
                    .then(async res => {
                        const msg = await res.text();
                        if (!res.ok) {
                            alert('Lỗi: ' + msg);
                        } else {
                            alert('Thành công: ' + msg);
                            window.location.reload();
                        }
                    })
                    .catch(err => {
                        alert('Lỗi kết nối khi chấm công!');
                    });
            };

            if (!window.bootstrap) return;

            function parseVnMoneyToInt(value) {
                if (value == null) return null;
                var s = String(value).trim();
                if (!s) return null;
                // Accept: "22000", "22.000", "22,000", "22 000đ"...
                s = s.replace(/[^\d]/g, '');
                if (!s) return null;
                var n = Number(s);
                if (!Number.isFinite(n)) return null;
                return Math.max(0, Math.trunc(n));
            }

            function formatVnMoney(n) {
                var num = Number(n);
                if (!Number.isFinite(num)) return '';
                return Math.trunc(num).toLocaleString('vi-VN');
            }

            function bindMoneyInput(el) {
                if (!el) return;
                el.addEventListener('focus', function () {
                    var raw = parseVnMoneyToInt(el.value);
                    el.value = raw != null ? String(raw) : '';
                });
                el.addEventListener('blur', function () {
                    var raw = parseVnMoneyToInt(el.value);
                    el.value = raw != null ? formatVnMoney(raw) : '';
                });
            }

            // Admin - quản lý tài khoản nhân sự
            var userAdminModalInstance = null;
            var staffOptionsCache = [];
            var userTableBody = document.getElementById('userAdminTableBody');
            var btnOpenCreateUserModal = document.getElementById('btnOpenCreateUserModal');
            var createUserModalEl = document.getElementById('createUserModal');
            var createUserForm = document.getElementById('createUserForm');

            function roleIsStaff(role) {
                var r = role ? String(role).toUpperCase() : '';
                return r && r !== 'CUSTOMER';
            }

            function renderCreateUserStaffOptions(users) {
                var sel = document.getElementById('userStaffId');
                if (!sel) return;

                var used = new Set();
                if (Array.isArray(users)) {
                    users.forEach(function (u) {
                        if (!u) return;
                        if (!roleIsStaff(u.role)) return;
                        if (u.staffId != null) used.add(String(u.staffId));
                    });
                }

                var opts = ['<option value="" selected disabled>Chọn nhân viên…</option>'];
                staffOptionsCache.forEach(function (s) {
                    if (!s || s.id == null) return;
                    if (used.has(String(s.id))) return;
                    var label = (s.name || ('Staff #' + s.id)) + (s.phone ? (' · ' + s.phone) : '');
                    opts.push('<option value="' + s.id + '">' + label + '</option>');
                });
                sel.innerHTML = opts.join('');
            }

            function renderUsers(users) {
                if (!userTableBody) return;
                if (!users || users.length === 0) {
                    userTableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-3">Chưa có tài khoản nào.</td></tr>';
                    return;
                }

                var staffOptionsHtml = (function () {
                    var opts = ['<option value="">-- Chưa gán --</option>'];
                    staffOptionsCache.forEach(function (s) {
                        if (!s || s.id == null) return;
                        var label = (s.name || ('Staff #' + s.id)) + (s.phone ? (' · ' + s.phone) : '');
                        opts.push('<option value="' + s.id + '">' + label + '</option>');
                    });
                    return opts.join('');
                })();

                var rows = users
                    .filter(function (u) {
                        var role = (u && u.role) ? String(u.role).toUpperCase() : '';
                        return role && role !== 'CUSTOMER';
                    })
                    .map(function (u) {
                        var safeUsername = u.username || '';
                        var safeFullName = u.fullName || '';
                        var safeEmail = u.email || '';
                        var safePhone = u.phone || '';
                        var safeRole = u.role || '';

                        return '<tr data-id="' + u.id + '">' +
                            '<td>' + safeUsername + '</td>' +
                            '<td>' + safeFullName + '</td>' +
                            '<td>' + safeEmail + '</td>' +
                            '<td>' + safePhone + '</td>' +
                            '<td>' + safeRole + '</td>' +
                            '<td>' +
                            '<div class="d-flex gap-2 align-items-center">' +
                            '<select class="form-select form-select-sm user-staff-select" style="min-width: 220px;">' +
                            staffOptionsHtml +
                            '</select>' +
                            '<button type="button" class="btn btn-sm btn-outline-primary btn-link-staff">Lưu</button>' +
                            '</div>' +
                            '</td>' +
                            '<td>' +
                            '<button type="button" class="btn btn-sm btn-outline-secondary me-1 btn-edit-user">Sửa</button>' +
                            '<button type="button" class="btn btn-sm btn-outline-danger btn-delete-user">Xóa</button>' +
                            '</td>' +
                            '</tr>';
                    }).join('');

                userTableBody.innerHTML = rows || '<tr><td colspan="7" class="text-center text-muted py-3">Chưa có tài khoản nào.</td></tr>';

                userTableBody.querySelectorAll('tr[data-id]').forEach(function (tr) {
                    var id = tr.getAttribute('data-id');
                    var u = users.find(function (x) { return String(x.id) === String(id); });
                    if (!u) return;
                    var sel = tr.querySelector('.user-staff-select');
                    if (!sel) return;
                    sel.value = u.staffId != null ? String(u.staffId) : '';
                });
            }

            function loadStaffOptions() {
                return fetch('/api/staff')
                    .then(function (res) { return res.ok ? res.json() : []; })
                    .then(function (data) { staffOptionsCache = Array.isArray(data) ? data : []; })
                    .catch(function () { staffOptionsCache = []; });
            }

            function loadUsers() {
                if (!userTableBody) return;
                fetch('/api/admin/users')
                    .then(function (res) {
                        if (!res.ok) throw new Error('Không thể tải danh sách tài khoản');
                        return res.json();
                    })
                    .then(function (data) { renderUsers(Array.isArray(data) ? data : []); })
                    .catch(function () {
                        userTableBody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-3">Lỗi khi tải danh sách tài khoản.</td></tr>';
                    });
            }

            if (userTableBody) {
                loadStaffOptions().then(function () { loadUsers(); });
                userTableBody.addEventListener('click', function (e) {
                    var target = e.target;
                    var row = target.closest('tr');
                    if (!row) return;
                    var id = row.getAttribute('data-id');
                    if (!id) return;

                    if (target.classList.contains('btn-link-staff')) {
                        var sel = row.querySelector('.user-staff-select');
                        var staffId = sel && sel.value ? Number(sel.value) : null;
                        fetch('/api/admin/users/' + id + '/staff', {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ staffId: staffId })
                        })
                            .then(function (res) {
                                if (!res.ok) return res.text().then(function (t) { throw new Error(t || 'Lưu liên kết thất bại'); });
                                return res.json();
                            })
                            .then(function () { loadUsers(); })
                            .catch(function (err) { alert(err && err.message ? err.message : 'Không thể lưu liên kết nhân viên.'); });
                        return;
                    }

                    if (target.classList.contains('btn-delete-user')) {
                        if (!confirm('Bạn có chắc chắn muốn xóa tài khoản này?')) return;
                        fetch('/api/admin/users/' + id, { method: 'DELETE' })
                            .then(function (res) {
                                if (!res.ok && res.status !== 204) throw new Error('Xóa tài khoản thất bại');
                                loadUsers();
                            })
                            .catch(function () { alert('Không thể xóa tài khoản.'); });
                    }
                });
            }

            // Admin - tạo tài khoản nhân sự (chọn staff ngay lúc tạo)
            if (btnOpenCreateUserModal && createUserModalEl) {
                var createUserModal = new bootstrap.Modal(createUserModalEl);
                btnOpenCreateUserModal.addEventListener('click', function () {
                    Promise.all([
                        loadStaffOptions(),
                        fetch('/api/admin/users').then(function (r) { return r.ok ? r.json() : []; }).catch(function () { return []; })
                    ]).then(function (res) {
                        var users = Array.isArray(res[1]) ? res[1] : [];
                        if (createUserForm) createUserForm.reset();
                        renderCreateUserStaffOptions(users);
                        createUserModal.show();
                    }).catch(function () {
                        alert('Không thể tải dữ liệu nhân viên/tài khoản.');
                    });
                });

                if (createUserForm) {
                    createUserForm.addEventListener('submit', function (e) {
                        e.preventDefault();
                        var username = (document.getElementById('userUsername')?.value || '').trim();
                        var password = (document.getElementById('userPassword')?.value || '').trim();
                        var fullName = (document.getElementById('userFullName')?.value || '').trim();
                        var email = (document.getElementById('userEmail')?.value || '').trim();
                        var phone = (document.getElementById('userPhone')?.value || '').trim();
                        var role = document.getElementById('userRole')?.value;
                        var staffIdRaw = document.getElementById('userStaffId')?.value;

                        if (!username) return alert('Vui lòng nhập username.');
                        if (!password) return alert('Vui lòng nhập mật khẩu.');
                        if (!role) return alert('Vui lòng chọn vai trò.');
                        if (!staffIdRaw) return alert('Vui lòng chọn nhân viên để gán.');

                        var payload = {
                            username: username,
                            password: password,
                            fullName: fullName || null,
                            email: email || null,
                            phone: phone || null,
                            role: role,
                            staffId: Number(staffIdRaw)
                        };

                        fetch('/api/admin/users', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        }).then(function (res) {
                            if (!res.ok) return res.text().then(function (t) { throw new Error(t || 'Tạo tài khoản thất bại'); });
                            return res.json();
                        }).then(function () {
                            alert('Đã tạo tài khoản và gán nhân viên thành công.');
                            createUserModal.hide();
                            loadStaffOptions().then(function () { loadUsers(); });
                        }).catch(function (err) {
                            alert(err && err.message ? err.message : 'Không thể tạo tài khoản.');
                        });
                    });
                }
            }

            // Phân ca theo ngày (ai cũng xem, chỉ ADMIN sửa)
            var shiftDateInput = document.getElementById('shiftDateInput');
            var btnLoadShiftDate = document.getElementById('btnLoadShiftDate');
            var shiftTableBody = document.getElementById('shiftTableBody');
            var staffCache = [];
            var assignmentsSet = new Set(); // key = staffId|shiftCode
            var isAdminForShifts = !!document.getElementById('roleAdminFlag');

            function todayIso() {
                var d = new Date();
                var m = String(d.getMonth() + 1).padStart(2, '0');
                var day = String(d.getDate()).padStart(2, '0');
                return d.getFullYear() + '-' + m + '-' + day;
            }

            function loadShiftScreen(dateStr) {
                if (!shiftTableBody) return;
                if (!dateStr) {
                    shiftTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Chọn ngày để phân ca.</td></tr>';
                    return;
                }
                shiftTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Đang tải...</td></tr>';
                assignmentsSet = new Set();

                Promise.all([
                    fetch('/api/staff/basic').then(function (r) { return r.ok ? r.json() : []; }).catch(function () { return []; }),
                    fetch('/api/staff-shifts?date=' + encodeURIComponent(dateStr)).then(function (r) { return r.ok ? r.json() : []; }).catch(function () { return []; })
                ]).then(function (res) {
                    staffCache = Array.isArray(res[0]) ? res[0] : [];
                    var assigns = Array.isArray(res[1]) ? res[1] : [];
                    assigns.forEach(function (a) {
                        if (!a || !a.staff || a.staff.id == null || !a.shiftCode) return;
                        assignmentsSet.add(String(a.staff.id) + '|' + String(a.shiftCode));
                    });

                    var rows = staffCache.map(function (s) {
                        var id = s.id;
                        var name = s.name || '';
                        var phone = s.phone ? (' · ' + s.phone) : '';
                        function cell(code) {
                            var key = String(id) + '|' + code;
                            var checked = assignmentsSet.has(key) ? 'checked' : '';
                            var disabled = isAdminForShifts ? '' : 'disabled';
                            return '<input type="checkbox" class="form-check-input shift-toggle" data-staff="' + id + '" data-code="' + code + '" ' + checked + ' ' + disabled + ' />';
                        }
                        return '<tr>' +
                            '<td><div class="fw-semibold">' + name + '</div><div class="text-muted small">' + phone + '</div></td>' +
                            '<td class="text-center">' + cell('MORNING') + '</td>' +
                            '<td class="text-center">' + cell('AFTERNOON') + '</td>' +
                            '<td class="text-center">' + cell('EVENING') + '</td>' +
                            '<td class="text-center">' + cell('FULL_DAY') + '</td>' +
                            '</tr>';
                    }).join('');

                    shiftTableBody.innerHTML = rows || '<tr><td colspan="5" class="text-center text-muted py-3">Chưa có nhân viên.</td></tr>';
                });
            }

            function toggleAssign(dateStr, staffId, code, checked) {
                var url = checked ? '/api/staff-shifts/assign' : '/api/staff-shifts/unassign';
                return fetch(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ staffId: Number(staffId), workDate: dateStr, shiftCode: code })
                }).then(function (r) {
                    if (!r.ok) return r.json().catch(function () { return null; }).then(function (b) {
                        throw new Error((b && b.error) ? b.error : 'Không lưu được phân ca');
                    });
                    return r.json().catch(function () { return {}; });
                });
            }

            if (shiftDateInput && !shiftDateInput.value) {
                shiftDateInput.value = todayIso();
            }
            if (btnLoadShiftDate) {
                btnLoadShiftDate.addEventListener('click', function () {
                    loadShiftScreen(shiftDateInput ? shiftDateInput.value : '');
                });
            }
            if (shiftDateInput) {
                shiftDateInput.addEventListener('change', function () {
                    loadShiftScreen(shiftDateInput.value);
                });
            }
            if (shiftTableBody) {
                shiftTableBody.addEventListener('change', function (e) {
                    var t = e.target;
                    if (!t || !t.classList.contains('shift-toggle')) return;
                    if (!isAdminForShifts) {
                        t.checked = !t.checked;
                        alert('Bạn chỉ có quyền xem lịch phân ca.');
                        return;
                    }
                    var dateStr = shiftDateInput ? shiftDateInput.value : '';
                    var staffId = t.getAttribute('data-staff');
                    var code = t.getAttribute('data-code');
                    var checked = !!t.checked;

                    // Rule B (UI): FULL_DAY không chồng với MORNING/AFTERNOON/EVENING
                    var row = t.closest('tr');
                    function getToggle(code) {
                        return row ? row.querySelector('.shift-toggle[data-code="' + code + '"]') : null;
                    }
                    var full = getToggle('FULL_DAY');
                    var m = getToggle('MORNING');
                    var a = getToggle('AFTERNOON');
                    var ev = getToggle('EVENING');

                    if (checked && code === 'FULL_DAY') {
                        if ((m && m.checked) || (a && a.checked) || (ev && ev.checked)) {
                            alert('Không thể chọn "Cả ngày" khi đã có ca khác trong ngày.');
                            t.checked = false;
                            return;
                        }
                    }
                    if (checked && code !== 'FULL_DAY') {
                        if (full && full.checked) {
                            alert('Không thể chọn ca này khi đã chọn "Cả ngày" trong ngày.');
                            t.checked = false;
                            return;
                        }
                    }

                    toggleAssign(dateStr, staffId, code, checked).catch(function (err) {
                        alert(err && err.message ? err.message : 'Không lưu được phân ca');
                        t.checked = !checked;
                    });
                });
                // initial load
                loadShiftScreen(shiftDateInput ? shiftDateInput.value : '');
            }

            var addBtn = document.querySelector('.staff-add-btn');
            var modalEl = document.getElementById('createStaffModal');
            var detailModalEl = document.getElementById('staffDetailModal');
            var searchInput = document.querySelector('.staff-search input');

            if (addBtn && modalEl) {
                var modal = new bootstrap.Modal(modalEl);
                var form = document.getElementById('createStaffForm');

                addBtn.addEventListener('click', function () {
                    if (form) {
                        form.reset();
                    }
                    bindMoneyInput(document.getElementById('staffSalary'));
                    modal.show();
                });

                if (form) {
                    form.addEventListener('submit', function (e) {
                        e.preventDefault();

                        var name = document.getElementById('staffFullName')?.value.trim();
                        var phone = document.getElementById('staffPhone')?.value.trim();
                        var role = document.getElementById('staffRole')?.value;
                        var status = document.getElementById('staffStatus')?.value;
                        var shift = document.getElementById('staffShift')?.value.trim();
                        var salaryValue = document.getElementById('staffSalary')?.value;
                        var avatarUrl = document.getElementById('staffAvatarUrl')?.value.trim();

                        if (!name) {
                            alert('Vui lòng nhập họ và tên nhân viên.');
                            return;
                        }

                        if (!phone) {
                            alert('Vui lòng nhập số điện thoại.');
                            return;
                        }

                        if (!role) {
                            alert('Vui lòng chọn vị trí.');
                            return;
                        }

                        var payload = {
                            name: name,
                            phone: phone,
                            role: role,
                            status: status || 'ACTIVE',
                            shift: shift || null,
                            salary: parseVnMoneyToInt(salaryValue),
                            avatarUrl: avatarUrl || null,
                            startDate: null
                        };

                        fetch('/api/staff', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(payload)
                        }).then(function (res) {
                            if (!res.ok) {
                                throw new Error('Lỗi tạo nhân viên');
                            }
                            return res.json();
                        }).then(function () {
                            alert('Đã thêm nhân viên mới thành công.');
                            modal.hide();
                            window.location.reload();
                        }).catch(function (err) {
                            console.error(err);
                            alert('Có lỗi xảy ra khi thêm nhân viên mới.');
                        });
                    });
                }
            }

            // Tìm kiếm nhân viên theo tên/vị trí
            if (searchInput) {
                searchInput.addEventListener('input', function () {
                    var keyword = searchInput.value.trim().toLowerCase();
                    var cards = document.querySelectorAll('.staff-card-clickable');
                    cards.forEach(function (card) {
                        var nameEl = card.querySelector('.staff-name');
                        var roleEl = card.querySelector('.staff-role');
                        var text = '';
                        if (nameEl) text += nameEl.textContent.toLowerCase();
                        if (roleEl) text += ' ' + roleEl.textContent.toLowerCase();
                        if (!keyword || text.indexOf(keyword) !== -1) {
                            card.parentElement.style.display = '';
                        } else {
                            card.parentElement.style.display = 'none';
                        }
                    });
                });
            }

            // Mở modal chi tiết/chỉnh sửa khi bấm vào thẻ nhân viên
            if (detailModalEl) {
                var detailModal = new bootstrap.Modal(detailModalEl);
                var cards = document.querySelectorAll('.staff-card-clickable');
                var editForm = document.getElementById('editStaffForm');

                cards.forEach(function (card) {
                    card.addEventListener('click', function () {
                        var id = card.getAttribute('data-id');
                        var name = card.getAttribute('data-name') || '';
                        var role = card.getAttribute('data-role') || '';
                        var status = card.getAttribute('data-status') || 'ACTIVE';
                        var shift = card.getAttribute('data-shift') || '';
                        var salary = card.getAttribute('data-salary') || '';
                        var startDate = card.getAttribute('data-start-date') || '';
                        var avatarUrl = card.getAttribute('data-avatar-url') || '';
                        var phone = card.getAttribute('data-phone') || '';

                        document.getElementById('editStaffId').value = id;
                        document.getElementById('editStaffName').value = name;
                        document.getElementById('editStaffRole').value = role;
                        document.getElementById('editStaffStatus').value = status;
                        document.getElementById('editStaffShift').value = shift;
                        document.getElementById('editStaffSalary').value = salary != null ? formatVnMoney(salary) : '';
                        document.getElementById('editStaffStartDate').value = startDate || '';
                        document.getElementById('editStaffAvatarUrl').value = avatarUrl || '';

                        var phoneInput = document.getElementById('editStaffPhone');
                        if (phoneInput) {
                            phoneInput.value = phone || '';
                        }

                        // Avatar & badge trong modal
                        var avatarEl = document.getElementById('editStaffAvatar');
                        if (avatarEl) {
                            var url = avatarUrl || card.querySelector('.staff-avatar')?.getAttribute('src') || '';
                            if (url) {
                                avatarEl.src = url;
                            }
                        }

                        var badge = document.getElementById('editStaffStatusBadge');
                        if (badge) {
                            badge.classList.remove('staff-status-active', 'staff-status-leave', 'staff-status-quit');
                            if (status === 'ACTIVE') {
                                badge.classList.add('staff-status-active');
                                badge.textContent = 'Đang làm';
                            } else if (status === 'ON_LEAVE') {
                                badge.classList.add('staff-status-leave');
                                badge.textContent = 'Nghỉ phép';
                            } else if (status === 'INACTIVE') {
                                badge.classList.add('staff-status-quit');
                                badge.textContent = 'Đã nghỉ việc';
                            } else {
                                badge.textContent = 'Chưa rõ';
                            }
                        }

                        // Tính tổng giờ làm và lương tạm tính
                        var totalHoursInput = document.getElementById('editStaffTotalHours');
                        var calculatedSalaryInput = document.getElementById('editStaffCalculatedSalary');
                        var salaryInputEl = document.getElementById('editStaffSalary');
                        var currentTotalHours = 0;

                        function updateCalculatedSalary() {
                            if (!calculatedSalaryInput || !salaryInputEl) return;
                            var hourly = parseVnMoneyToInt(salaryInputEl.value) || 0;
                            var total = currentTotalHours * hourly;
                            calculatedSalaryInput.value = total > 0
                                ? total.toLocaleString('vi-VN') + ' đ'
                                : '0 đ';
                        }

                        if (totalHoursInput && calculatedSalaryInput && id) {
                            totalHoursInput.value = 'Đang tính...';
                            calculatedSalaryInput.value = '';

                            fetch('/api/staff/' + id + '/salary')
                                .then(function (res) { return res.ok ? res.json() : null; })
                                .then(function (data) {
                                    if (!data) {
                                        currentTotalHours = 0;
                                        totalHoursInput.value = '0 giờ';
                                        updateCalculatedSalary();
                                        return;
                                    }

                                    currentTotalHours = Number(data.totalHours || 0);
                                    totalHoursInput.value = currentTotalHours.toFixed(2) + ' giờ';

                                    if (salaryInputEl && !salaryInputEl.value && data.hourlyRate != null) {
                                        salaryInputEl.value = formatVnMoney(data.hourlyRate);
                                    }

                                    updateCalculatedSalary();
                                })
                                .catch(function () {
                                    currentTotalHours = 0;
                                    totalHoursInput.value = '0 giờ';
                                    updateCalculatedSalary();
                                });
                        }

                        if (salaryInputEl) {
                            bindMoneyInput(salaryInputEl);
                            salaryInputEl.addEventListener('input', updateCalculatedSalary);
                        }

                        detailModal.show();
                    });
                });

                if (editForm) {
                    editForm.addEventListener('submit', function (e) {
                        e.preventDefault();

                        var id = document.getElementById('editStaffId').value;
                        var name = document.getElementById('editStaffName').value.trim();
                        var role = document.getElementById('editStaffRole').value;
                        var status = document.getElementById('editStaffStatus').value;
                        var shift = document.getElementById('editStaffShift').value.trim();
                        var salaryValue = document.getElementById('editStaffSalary').value;
                        var startDate = document.getElementById('editStaffStartDate').value;
                        var avatarUrl = document.getElementById('editStaffAvatarUrl').value.trim();
                        var phone = document.getElementById('editStaffPhone') ? document.getElementById('editStaffPhone').value.trim() : '';

                        if (!id || !name) {
                            alert('Vui lòng nhập đầy đủ thông tin bắt buộc.');
                            return;
                        }

                        var payload = {
                            id: Number(id),
                            name: name,
                            phone: phone || null,
                            role: role,
                            status: status,
                            shift: shift || null,
                            salary: parseVnMoneyToInt(salaryValue),
                            avatarUrl: avatarUrl || null,
                            startDate: startDate || null
                        };

                        fetch('/api/staff/update', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(payload)
                        }).then(function (res) {
                            if (!res.ok) {
                                throw new Error('Lỗi cập nhật nhân viên');
                            }
                            return res.json();
                        }).then(function (updated) {
                            // Cập nhật lại thẻ nhân viên tương ứng trên giao diện
                            var card = document.querySelector('.staff-card-clickable[data-id="' + updated.id + '"]');
                            if (card) {
                                card.setAttribute('data-name', updated.name || '');
                                card.setAttribute('data-phone', updated.phone || '');
                                card.setAttribute('data-role', updated.role || '');
                                card.setAttribute('data-status', updated.status || '');
                                card.setAttribute('data-shift', updated.shift || '');
                                card.setAttribute('data-salary', updated.salary || '');
                                card.setAttribute('data-start-date', updated.startDate || '');
                                card.setAttribute('data-avatar-url', updated.avatarUrl || '');

                                var nameEl = card.querySelector('.staff-name');
                                if (nameEl) {
                                    nameEl.textContent = updated.name || '';
                                }

                                var phoneEl = card.querySelector('.staff-phone-text');
                                if (phoneEl) {
                                    phoneEl.textContent = updated.phone || 'Chưa có số điện thoại';
                                }

                                var roleEl = card.querySelector('.staff-role');
                                if (roleEl && updated.role) {
                                    roleEl.textContent = updated.role.replace(/_/g, ' ');
                                }

                                // Cập nhật lại các badge trạng thái (ẩn/hiện đúng span)
                                var activeBadge = card.querySelector('.staff-status-badge.staff-status-active');
                                var leaveBadge = card.querySelector('.staff-status-badge.staff-status-leave');
                                var quitBadge = card.querySelector('.staff-status-badge.staff-status-quit');
                                var unknownBadge = card.querySelector('.staff-status-badge:not(.staff-status-active):not(.staff-status-leave):not(.staff-status-quit)');

                                function setBadgeVisibility(el, visible) {
                                    if (!el) return;
                                    el.style.display = visible ? '' : 'none';
                                }

                                if (updated.status === 'ACTIVE') {
                                    setBadgeVisibility(activeBadge, true);
                                    setBadgeVisibility(leaveBadge, false);
                                    setBadgeVisibility(quitBadge, false);
                                    setBadgeVisibility(unknownBadge, false);
                                } else if (updated.status === 'ON_LEAVE') {
                                    setBadgeVisibility(activeBadge, false);
                                    setBadgeVisibility(leaveBadge, true);
                                    setBadgeVisibility(quitBadge, false);
                                    setBadgeVisibility(unknownBadge, false);
                                } else if (updated.status === 'INACTIVE') {
                                    setBadgeVisibility(activeBadge, false);
                                    setBadgeVisibility(leaveBadge, false);
                                    setBadgeVisibility(quitBadge, true);
                                    setBadgeVisibility(unknownBadge, false);
                                } else {
                                    setBadgeVisibility(activeBadge, false);
                                    setBadgeVisibility(leaveBadge, false);
                                    setBadgeVisibility(quitBadge, false);
                                    setBadgeVisibility(unknownBadge, true);
                                }

                                var shiftEl = card.querySelector('.staff-shift-text');
                                if (shiftEl) {
                                    shiftEl.textContent = updated.shift || 'Chưa thiết lập ca làm';
                                }

                                // Cập nhật ảnh đại diện nếu có link mới
                                var avatarImg = card.querySelector('.staff-avatar');
                                if (avatarImg && updated.avatarUrl) {
                                    avatarImg.src = updated.avatarUrl;
                                }
                            }

                            alert('Đã cập nhật thông tin nhân viên thành công.');
                            detailModal.hide();
                        }).catch(function (err) {
                            console.error(err);
                            alert('Có lỗi xảy ra khi cập nhật nhân viên.');
                        });
                    });
                }
            }

            // Hiển thị lương và tổng giờ làm cho nhân viên đang đăng nhập (chỉ xem)
            (function loadMySalarySummary() {
                var cards = document.querySelectorAll('.staff-card-clickable');
                if (!cards.length) return;

                // Với tài khoản nhân viên, trang Staff chỉ hiển thị đúng 1 thẻ
                var card = cards.length === 1 ? cards[0] : null;
                if (!card) return;

                var hoursSpan = card.querySelector('.staff-hours-month-text');
                var salarySpan = card.querySelector('.staff-salary-month-text');
                if (!hoursSpan || !salarySpan) return;

                fetch('/api/staff/me/salary')
                    .then(function (res) { return res.ok ? res.json() : null; })
                    .then(function (data) {
                        if (!data) return;

                        var totalHours = Number(data.totalHours || 0);
                        var totalSalary = Number(data.totalSalary || 0);

                        hoursSpan.textContent = 'Giờ làm tháng này: ' + totalHours.toFixed(2) + ' giờ';
                        salarySpan.textContent = 'Lương tạm tính: ' + totalSalary.toLocaleString('vi-VN') + ' đ';
                    })
                    .catch(function () { });
            })();

            // Hiển thị lương & giờ làm cho tất cả nhân viên khi admin xem danh sách
            (function loadAllSalarySummaryForAdmin() {
                var cards = document.querySelectorAll('.staff-card-clickable');
                if (!cards.length || cards.length === 1) return; // 1 thẻ thường là nhân viên tự xem

                cards.forEach(function (card) {
                    var id = card.getAttribute('data-id');
                    if (!id) return;

                    var hoursSpan = card.querySelector('.staff-hours-month-text');
                    var salarySpan = card.querySelector('.staff-salary-month-text');
                    if (!hoursSpan || !salarySpan) return;

                    fetch('/api/staff/' + id + '/salary')
                        .then(function (res) { return res.ok ? res.json() : null; })
                        .then(function (data) {
                            if (!data) return;

                            var totalHours = Number(data.totalHours || 0);
                            var totalSalary = Number(data.totalSalary || 0);

                            hoursSpan.textContent = 'Giờ làm tháng này: ' + totalHours.toFixed(2) + ' giờ';
                            salarySpan.textContent = 'Lương tạm tính: ' + totalSalary.toLocaleString('vi-VN') + ' đ';
                        })
                        .catch(function () { });
                });
            })();
        });

