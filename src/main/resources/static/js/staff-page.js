        document.addEventListener('DOMContentLoaded', function () {
            function formatIsoDateVi(iso) {
                if (!iso) return '—';
                var p = String(iso).split('-');
                if (p.length !== 3) return iso;
                return p[2] + '/' + p[1] + '/' + p[0];
            }

            function formatTimeShort(s) {
                if (s == null || s === '') return '—';
                return String(s).substring(0, 5);
            }

            function escHtml(s) {
                if (s == null || s === '') return '';
                return String(s)
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;');
            }

            /** Chuẩn hoá ngày từ JSON (chuỗi ISO hoặc mảng LocalDate của Jackson) → yyyy-MM-dd. */
            function jsonDateToIsoInput(d) {
                if (d == null || d === '') return '';
                if (typeof d === 'string') return d.length >= 10 ? d.substring(0, 10) : '';
                if (Array.isArray(d) && d.length >= 3) {
                    var y = d[0];
                    var m = String(d[1]).padStart(2, '0');
                    var day = String(d[2]).padStart(2, '0');
                    return y + '-' + m + '-' + day;
                }
                return '';
            }

            function formatIsoDateViShort(iso) {
                if (!iso || String(iso).length < 10) return '';
                var p = String(iso).substring(0, 10).split('-');
                if (p.length !== 3) return iso;
                return p[2] + '/' + p[1] + '/' + p[0];
            }

            function staffExtraMetaLine(status, leaveFrom, leaveTo, leftOn) {
                if (status === 'ON_LEAVE' && leaveFrom) {
                    var to = leaveTo || leaveFrom;
                    return 'Nghỉ phép: ' + formatIsoDateViShort(leaveFrom) + ' – ' + formatIsoDateViShort(to);
                }
                if (status === 'INACTIVE' && leftOn) {
                    return 'Nghỉ việc: ' + formatIsoDateViShort(leftOn);
                }
                return '';
            }

            function setCardExtraMeta(card, status, leaveFrom, leaveTo, leftOn) {
                if (!card) return;
                var meta = card.querySelector('.staff-extra-meta');
                if (!meta) return;
                var line = staffExtraMetaLine(status, leaveFrom, leaveTo, leftOn);
                meta.textContent = line;
                meta.style.display = line ? '' : 'none';
            }

            function todayIsoForInput() {
                var d = new Date();
                var m = String(d.getMonth() + 1).padStart(2, '0');
                var day = String(d.getDate()).padStart(2, '0');
                return d.getFullYear() + '-' + m + '-' + day;
            }

            function staffOnLeaveOnWorkDate(s, dateStr) {
                if (!s || s.status !== 'ON_LEAVE' || !dateStr) return false;
                if (!s.leaveFrom || !s.leaveTo) return false;
                return dateStr >= s.leaveFrom && dateStr <= s.leaveTo;
            }

            /** Trạng thái hiển thị / lọc thẻ: khớp StaffService.effectiveCardStatus (Java). */
            function effectiveStaffCardStatus(dbStatus, leaveFrom, leaveTo) {
                if (dbStatus === 'INACTIVE') {
                    return 'INACTIVE';
                }
                var d = new Date();
                var iso = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
                var lf = (leaveFrom && String(leaveFrom).length >= 10) ? String(leaveFrom).substring(0, 10) : '';
                var lt = (leaveTo && String(leaveTo).length >= 10) ? String(leaveTo).substring(0, 10) : '';
                if (dbStatus === 'ON_LEAVE' && lf && lt && iso >= lf && iso <= lt) {
                    return 'ON_LEAVE';
                }
                return 'ACTIVE';
            }

            function setStaffCardBadgesForEffective(card, eff) {
                var activeBadge = card.querySelector('.staff-status-badge.staff-status-active');
                var leaveBadge = card.querySelector('.staff-status-badge.staff-status-leave');
                var quitBadge = card.querySelector('.staff-status-badge.staff-status-quit');
                var unknownBadge = card.querySelector('.staff-status-badge:not(.staff-status-active):not(.staff-status-leave):not(.staff-status-quit)');
                function vis(el, on) {
                    if (!el) return;
                    el.style.display = on ? '' : 'none';
                }
                vis(activeBadge, eff === 'ACTIVE');
                vis(leaveBadge, eff === 'ON_LEAVE');
                vis(quitBadge, eff === 'INACTIVE');
                vis(unknownBadge, false);
            }

            /** Số ngày nghỉ phép (theo khoảng đã lưu) giao với tháng hiện tại; sang tháng mới tự tính lại theo ngày hệ thống. */
            function leaveDaysInMonthOverlapping(fromStr, toStr, refDate) {
                refDate = refDate || new Date();
                var y = refDate.getFullYear();
                var m = refDate.getMonth();
                var pad = function (n) { return String(n).padStart(2, '0'); };
                var lastD = new Date(y, m + 1, 0).getDate();
                var monthStart = y + '-' + pad(m + 1) + '-01';
                var monthEnd = y + '-' + pad(m + 1) + '-' + pad(lastD);
                if (!fromStr || !toStr || String(fromStr).length < 10 || String(toStr).length < 10) return 0;
                var a = String(fromStr).substring(0, 10);
                var b = String(toStr).substring(0, 10);
                var s = a > monthStart ? a : monthStart;
                var t = b < monthEnd ? b : monthEnd;
                if (s > t) return 0;
                var d1 = new Date(s + 'T12:00:00');
                var d2 = new Date(t + 'T12:00:00');
                return Math.round((d2 - d1) / 86400000) + 1;
            }

            function updateStaffCardLeaveDaysMonth(card) {
                var el = card.querySelector('.staff-leave-days-month-text');
                if (!el) return;
                var st = card.getAttribute('data-status') || '';
                var lf = card.getAttribute('data-leave-from') || '';
                var lt = card.getAttribute('data-leave-to') || '';
                if (lf === 'null') lf = '';
                if (lt === 'null') lt = '';
                if (st !== 'ON_LEAVE' || !lf || !lt) {
                    el.textContent = 'Nghỉ phép trong tháng: 0 ngày';
                    return;
                }
                var n = leaveDaysInMonthOverlapping(lf, lt);
                el.textContent = 'Nghỉ phép trong tháng: ' + n + ' ngày';
            }

            function refreshAllStaffLeaveDaysMonthLabels() {
                document.querySelectorAll('.staff-card-clickable').forEach(updateStaffCardLeaveDaysMonth);
            }

            /** Mã StaffRole từ API /api/staff/basic → nhãn tiếng Việt (tab phân ca). */
            function staffRoleLabelVi(roleCode) {
                if (!roleCode) return 'Chưa thiết lập';
                var map = {
                    PHA_CHE: 'Pha chế',
                    PHUC_VU: 'Phục vụ',
                    THU_NGAN: 'Thu ngân',
                    QUAN_LY: 'Quản lý',
                    BAO_VE: 'Bảo vệ'
                };
                if (map[roleCode]) return map[roleCode];
                return String(roleCode).replace(/_/g, ' ');
            }

            let attendanceStatus = 'NOT_CHECKED_IN';
            let attendanceTodayData = null;
            var checkinBtn = document.getElementById('checkinBtn');
            var selfRoot = document.getElementById('staffPageSelfService');
            var hasStaffProfile = !selfRoot || selfRoot.getAttribute('data-has-profile') === 'true';

            function shiftLabelVi(code) {
                if (!code) return '';
                var m = { MORNING: 'sáng', AFTERNOON: 'chiều', EVENING: 'tối', FULL_DAY: 'cả ngày' };
                return m[code] || code;
            }

            /** Chuyển danh sách shift codes thành label tiếng Việt (vd: "sáng + chiều"). */
            function blockShiftsLabelVi(shiftsArr) {
                if (!shiftsArr || !shiftsArr.length) return '';
                return shiftsArr.map(shiftLabelVi).join(' + ');
            }

            function updateSelfTodayCards(data) {
                var titleEl = document.getElementById('staffSelfTodayTitle');
                var detailEl = document.getElementById('staffSelfTodayDetail');
                if (!titleEl) return;
                var st = (data && data.status) ? data.status : 'NOT_CHECKED_IN';

                if (st === 'NOT_STAFF') {
                    titleEl.textContent = 'Chưa có hồ sơ nhân viên';
                    if (detailEl) detailEl.textContent = 'Liên hệ quản lý để gán tài khoản.';
                    return;
                }
                if (st === 'NO_SHIFT') {
                    titleEl.textContent = 'Chưa được phân ca hôm nay';
                    if (detailEl) detailEl.textContent = data.message || 'Liên hệ quản lý.';
                    return;
                }
                if (st === 'CHECKED_IN') {
                    // Hiển thị thông tin block (có thể là ca gộp)
                    var blockLabel = '';
                    if (data.currentBlock) {
                        // currentBlock có thể là "MORNING,AFTERNOON" hoặc "MORNING"
                        var parts = String(data.currentBlock).split(',');
                        blockLabel = ' ' + blockShiftsLabelVi(parts);
                    }
                    titleEl.textContent = 'Đang trong ca' + blockLabel;
                    var t = data.checkInTime ? String(data.checkInTime).substring(0, 5) : '';
                    var blockTime = '';
                    if (data.currentBlockStart && data.currentBlockEnd) {
                        blockTime = ' (' + String(data.currentBlockStart).substring(0, 5) + '–' + String(data.currentBlockEnd).substring(0, 5) + ')';
                    }
                    if (detailEl) detailEl.textContent = t ? ('Giờ vào: ' + t + blockTime) : '';
                    return;
                }
                if (st === 'ALL_COMPLETED') {
                    titleEl.textContent = 'Đã chấm đủ hôm nay';
                    var h = data.totalHoursToday != null ? data.totalHoursToday : 0;
                    if (detailEl) detailEl.textContent = 'Tổng ' + h + ' giờ làm hôm nay.';
                    return;
                }
                if (st === 'READY_TO_CHECK_IN') {
                    // Hiển thị thông tin block tiếp theo
                    var nextLabel = '';
                    if (data.nextBlockShifts && data.nextBlockShifts.length) {
                        nextLabel = blockShiftsLabelVi(data.nextBlockShifts);
                    } else if (data.nextShift) {
                        nextLabel = shiftLabelVi(data.nextShift);
                    }
                    titleEl.textContent = 'Sẵn sàng chấm công' + (nextLabel ? ' ca ' + nextLabel : '');
                    if (detailEl) detailEl.textContent = data.message || '';
                    return;
                }
                if (st === 'WAITING_FOR_SHIFT') {
                    titleEl.textContent = 'Chờ đến giờ ca tiếp theo';
                    if (detailEl) detailEl.textContent = data.message || '';
                    return;
                }
                if (st === 'SHIFT_WINDOW_PASSED') {
                    titleEl.textContent = 'Đã quá giờ chấm công';
                    if (detailEl) detailEl.textContent = data.message || 'Liên hệ quản lý.';
                    return;
                }
                titleEl.textContent = '—';
                if (detailEl) detailEl.textContent = '';
            }

            function applyCheckinButtonState(data) {
                if (!checkinBtn) return;
                var textSpan = document.getElementById('checkinText');
                var st = (data && data.status) ? data.status : '';

                checkinBtn.classList.remove('btn-primary', 'btn-warning', 'btn-success', 'btn-secondary');
                checkinBtn.disabled = false;
                checkinBtn.removeAttribute('title');

                if (st === 'NOT_STAFF' || st === 'NO_SHIFT') {
                    checkinBtn.disabled = true;
                    checkinBtn.classList.add('btn-secondary');
                    if (textSpan) textSpan.textContent = 'Không thể chấm công';
                    checkinBtn.style.display = 'inline-block';
                    return;
                }
                if (st === 'CHECKED_IN') {
                    checkinBtn.classList.add('btn-warning');
                    if (textSpan) textSpan.textContent = 'Chấm công ra (Giờ ra)';
                    checkinBtn.style.display = 'inline-block';
                    return;
                }
                if (st === 'ALL_COMPLETED') {
                    checkinBtn.disabled = true;
                    checkinBtn.classList.add('btn-success');
                    var h = data.totalHoursToday != null ? data.totalHoursToday : 0;
                    if (textSpan) textSpan.textContent = 'Đã hoàn thành (' + h + ' giờ)';
                    checkinBtn.style.display = 'inline-block';
                    return;
                }
                if (st === 'READY_TO_CHECK_IN') {
                    checkinBtn.classList.add('btn-primary');
                    var nextLabel = '';
                    if (data.nextBlockShifts && data.nextBlockShifts.length) {
                        nextLabel = ' ca ' + blockShiftsLabelVi(data.nextBlockShifts);
                    } else if (data.nextShift) {
                        nextLabel = ' ca ' + shiftLabelVi(data.nextShift);
                    }
                    if (textSpan) textSpan.textContent = 'Chấm công vào' + nextLabel;
                    checkinBtn.style.display = 'inline-block';
                    return;
                }
                if (st === 'WAITING_FOR_SHIFT' || st === 'SHIFT_WINDOW_PASSED') {
                    checkinBtn.disabled = true;
                    checkinBtn.classList.add('btn-secondary');
                    if (textSpan) textSpan.textContent = st === 'WAITING_FOR_SHIFT' ? 'Chờ đến giờ ca' : 'Quá giờ chấm công';
                    checkinBtn.setAttribute('title', data.message || '');
                    checkinBtn.style.display = 'inline-block';
                    return;
                }
                checkinBtn.classList.add('btn-primary');
                if (textSpan) textSpan.textContent = 'Chấm công';
                checkinBtn.style.display = 'inline-block';
            }

            function loadSelfMonthSummary() {
                var hEl = document.getElementById('staffSelfMonthHours');
                var sEl = document.getElementById('staffSelfMonthSalary');
                if (!hEl && !sEl) return;
                fetch('/api/staff/me/salary')
                    .then(function (res) { return res.ok ? res.json() : null; })
                    .then(function (data) {
                        if (!data) return;
                        var totalHours = Number(data.totalHours || 0);
                        var totalSalary = Number(data.totalSalary || 0);
                        if (hEl) hEl.textContent = totalHours.toFixed(2) + ' giờ';
                        if (sEl) sEl.textContent = totalSalary.toLocaleString('vi-VN') + ' đ';
                    })
                    .catch(function () { });
            }

            if (checkinBtn) {
                if (selfRoot && !hasStaffProfile) {
                    checkinBtn.style.display = 'none';
                } else {
                    fetch('/api/attendance/today')
                        .then(function (res) { return res.json(); })
                        .then(function (data) {
                            attendanceStatus = data.status || 'NOT_CHECKED_IN';
                            attendanceTodayData = data;
                            updateSelfTodayCards(data);
                            applyCheckinButtonState(data);
                            if (selfRoot && hasStaffProfile) {
                                loadSelfMonthSummary();
                            }
                        })
                        .catch(function (err) { console.error(err); });
                }
            }

            window.staffCheckInOut = function () {
                if (attendanceStatus === 'NOT_STAFF' || attendanceStatus === 'NO_SHIFT') return;
                var url = (attendanceStatus === 'CHECKED_IN') ? '/api/attendance/check-out' : '/api/attendance/check-in';
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

            function syncCreateStaffStatusRows() {
                var stEl = document.getElementById('staffStatus');
                var leaveRow = document.getElementById('createStaffLeaveRow');
                var leftRow = document.getElementById('createStaffLeftOnRow');
                if (!stEl || !leaveRow || !leftRow) return;
                var v = stEl.value;
                leaveRow.classList.toggle('d-none', v !== 'ON_LEAVE');
                leftRow.classList.toggle('d-none', v !== 'INACTIVE');
                if (v === 'INACTIVE') {
                    var lo = document.getElementById('staffLeftOn');
                    if (lo && !lo.value) lo.value = todayIsoForInput();
                }
            }

            function syncEditStaffStatusRows() {
                var stEl = document.getElementById('editStaffStatus');
                var leaveRow = document.getElementById('editStaffLeaveRow');
                var leftRow = document.getElementById('editStaffLeftOnRow');
                if (!stEl || !leaveRow || !leftRow) return;
                var v = stEl.value;
                leaveRow.classList.toggle('d-none', v !== 'ON_LEAVE');
                leftRow.classList.toggle('d-none', v !== 'INACTIVE');
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
                        var roleVi = staffRoleLabelVi(s.role);
                        var nameLine = '<div class="fw-semibold">' + escHtml(name || '—') +
                            ' <span class="text-muted fw-normal">- ' + escHtml(roleVi) + '</span></div>';
                        var phone = s.phone ? (' · ' + escHtml(s.phone)) : '';
                        function cell(code) {
                            var key = String(id) + '|' + code;
                            var checked = assignmentsSet.has(key) ? 'checked' : '';
                            var blocked = s.status === 'INACTIVE' || staffOnLeaveOnWorkDate(s, dateStr);
                            var disabled = (!isAdminForShifts || blocked) ? 'disabled' : '';
                            var title = blocked ? ' title="' + escHtml(s.status === 'INACTIVE' ? 'Đã nghỉ việc' : 'Đang nghỉ phép ngày này') + '"' : '';
                            return '<input type="checkbox" class="form-check-input shift-toggle" data-staff="' + id + '" data-code="' + code + '" ' + checked + ' ' + disabled + title + ' />';
                        }
                        return '<tr>' +
                            '<td>' + nameLine + '<div class="text-muted small">' + phone + '</div></td>' +
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

            // Báo cáo chấm công (chỉ ADMIN — tab chỉ render khi có quyền)
            var attendanceReportFrom = document.getElementById('attendanceReportFrom');
            var attendanceReportTo = document.getElementById('attendanceReportTo');
            var btnLoadAttendanceReport = document.getElementById('btnLoadAttendanceReport');
            var attendanceReportTableBody = document.getElementById('attendanceReportTableBody');
            var tabAttendanceReport = document.getElementById('tab-attendance-report');

            function loadAttendanceReport() {
                if (!attendanceReportTableBody) return;
                var fromVal = attendanceReportFrom ? attendanceReportFrom.value : '';
                var toVal = attendanceReportTo ? attendanceReportTo.value : '';
                if (!fromVal || !toVal) {
                    attendanceReportTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Vui lòng chọn đủ từ ngày và đến ngày.</td></tr>';
                    return;
                }
                if (fromVal > toVal) {
                    attendanceReportTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-3">Từ ngày không được sau đến ngày.</td></tr>';
                    return;
                }
                attendanceReportTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Đang tải...</td></tr>';
                var q = '?from=' + encodeURIComponent(fromVal) + '&to=' + encodeURIComponent(toVal);
                fetch('/api/attendance/report' + q)
                    .then(function (r) {
                        if (r.status === 403) throw new Error('Bạn không có quyền xem báo cáo này.');
                        if (!r.ok) return r.json().catch(function () { return null; }).then(function (b) {
                            throw new Error((b && b.error) ? b.error : 'Không tải được dữ liệu');
                        });
                        return r.json();
                    })
                    .then(function (rows) {
                        if (!Array.isArray(rows) || rows.length === 0) {
                            attendanceReportTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Không có bản ghi chấm công trong khoảng này.</td></tr>';
                            return;
                        }
                        attendanceReportTableBody.innerHTML = rows.map(function (row) {
                            var wh = row.workHours;
                            var whText = '—';
                            if (wh != null && wh !== '') {
                                var n = Number(wh);
                                if (!isNaN(n)) {
                                    whText = (Math.round(n * 100) / 100).toString();
                                }
                            }
                            return '<tr>' +
                                '<td>' + formatIsoDateVi(row.workDate) + '</td>' +
                                '<td><span class="fw-medium">' + (row.staffName || '—') + '</span></td>' +
                                '<td>' + formatTimeShort(row.checkIn) + '</td>' +
                                '<td>' + formatTimeShort(row.checkOut) + '</td>' +
                                '<td class="text-end">' + whText + '</td>' +
                                '</tr>';
                        }).join('');
                    })
                    .catch(function (err) {
                        attendanceReportTableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-3">' +
                            (err && err.message ? err.message : 'Lỗi tải dữ liệu') + '</td></tr>';
                    });
            }

            if (attendanceReportFrom && attendanceReportTo) {
                if (!attendanceReportFrom.value || !attendanceReportTo.value) {
                    var now = new Date();
                    var y = now.getFullYear();
                    var m = String(now.getMonth() + 1).padStart(2, '0');
                    var d = String(now.getDate()).padStart(2, '0');
                    var todayIso = y + '-' + m + '-' + d;
                    attendanceReportFrom.value = y + '-' + m + '-01';
                    attendanceReportTo.value = todayIso;
                }
            }
            if (btnLoadAttendanceReport) {
                btnLoadAttendanceReport.addEventListener('click', function () {
                    loadAttendanceReport();
                });
            }
            if (tabAttendanceReport) {
                tabAttendanceReport.addEventListener('shown.bs.tab', function () {
                    loadAttendanceReport();
                });
            }

            var myAttendanceFrom = document.getElementById('myAttendanceFrom');
            var myAttendanceTo = document.getElementById('myAttendanceTo');
            var btnLoadMyAttendance = document.getElementById('btnLoadMyAttendance');
            var myAttendanceTableBody = document.getElementById('myAttendanceTableBody');

            function loadMyPersonalAttendance() {
                if (!myAttendanceTableBody) return;
                var fromVal = myAttendanceFrom ? myAttendanceFrom.value : '';
                var toVal = myAttendanceTo ? myAttendanceTo.value : '';
                if (!fromVal || !toVal) {
                    myAttendanceTableBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Vui lòng chọn đủ từ ngày và đến ngày.</td></tr>';
                    return;
                }
                if (fromVal > toVal) {
                    myAttendanceTableBody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3">Từ ngày không được sau đến ngày.</td></tr>';
                    return;
                }
                myAttendanceTableBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Đang tải...</td></tr>';
                var q = '?from=' + encodeURIComponent(fromVal) + '&to=' + encodeURIComponent(toVal);
                fetch('/api/attendance/my-records' + q)
                    .then(function (r) {
                        if (!r.ok) {
                            return r.json().catch(function () { return {}; }).then(function (b) {
                                throw new Error((b && b.error) ? b.error : 'Không tải được dữ liệu');
                            });
                        }
                        return r.json();
                    })
                    .then(function (rows) {
                        if (!Array.isArray(rows) || rows.length === 0) {
                            myAttendanceTableBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Không có bản ghi trong khoảng này.</td></tr>';
                            return;
                        }
                        myAttendanceTableBody.innerHTML = rows.map(function (row) {
                            var wh = row.workHours;
                            var whText = '—';
                            if (wh != null && wh !== '') {
                                var n = Number(wh);
                                if (!isNaN(n)) whText = (Math.round(n * 100) / 100).toString();
                            }
                            return '<tr>' +
                                '<td>' + formatIsoDateVi(row.workDate) + '</td>' +
                                '<td>' + formatTimeShort(row.checkIn) + '</td>' +
                                '<td>' + formatTimeShort(row.checkOut) + '</td>' +
                                '<td class="text-end">' + whText + '</td>' +
                                '</tr>';
                        }).join('');
                    })
                    .catch(function (err) {
                        myAttendanceTableBody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3">' +
                            (err && err.message ? err.message : 'Lỗi tải dữ liệu') + '</td></tr>';
                    });
            }

            if (myAttendanceFrom && myAttendanceTo && btnLoadMyAttendance) {
                if (!myAttendanceFrom.value || !myAttendanceTo.value) {
                    var now2 = new Date();
                    var y2 = now2.getFullYear();
                    var m2 = String(now2.getMonth() + 1).padStart(2, '0');
                    var d2 = String(now2.getDate()).padStart(2, '0');
                    myAttendanceFrom.value = y2 + '-' + m2 + '-01';
                    myAttendanceTo.value = y2 + '-' + m2 + '-' + d2;
                }
                btnLoadMyAttendance.addEventListener('click', function () {
                    loadMyPersonalAttendance();
                });
                if (document.getElementById('staffPageSelfService')) {
                    loadMyPersonalAttendance();
                }
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
                    var lf = document.getElementById('staffLeaveFrom');
                    var lt = document.getElementById('staffLeaveTo');
                    var lo = document.getElementById('staffLeftOn');
                    if (lf) lf.value = '';
                    if (lt) lt.value = '';
                    if (lo) lo.value = '';
                    bindMoneyInput(document.getElementById('staffSalary'));
                    syncCreateStaffStatusRows();
                    modal.show();
                });

                var staffStatusEl = document.getElementById('staffStatus');
                if (staffStatusEl) {
                    staffStatusEl.addEventListener('change', syncCreateStaffStatusRows);
                }

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

                        var accUser = (document.getElementById('staffAccountUsername')?.value || '').trim();
                        var accPass = document.getElementById('staffAccountPassword')?.value || '';
                        var accEmail = (document.getElementById('staffAccountEmail')?.value || '').trim();
                        if (!accUser) {
                            alert('Vui lòng nhập username cho tài khoản đăng nhập.');
                            return;
                        }
                        if (!accPass.trim()) {
                            alert('Vui lòng nhập mật khẩu cho tài khoản đăng nhập.');
                            return;
                        }

                        var st = status || 'ACTIVE';
                        var leaveFromVal = document.getElementById('staffLeaveFrom') ? document.getElementById('staffLeaveFrom').value : '';
                        var leaveToVal = document.getElementById('staffLeaveTo') ? document.getElementById('staffLeaveTo').value : '';
                        var leftOnVal = document.getElementById('staffLeftOn') ? document.getElementById('staffLeftOn').value : '';

                        if (st === 'ON_LEAVE' && !leaveFromVal && !leaveToVal) {
                            alert('Nghỉ phép: vui lòng chọn ít nhất một ngày (từ hoặc đến).');
                            return;
                        }
                        if (st === 'INACTIVE' && !leftOnVal) {
                            leftOnVal = todayIsoForInput();
                        }

                        var payload = {
                            name: name,
                            phone: phone,
                            role: role,
                            status: st,
                            shift: shift || null,
                            salary: parseVnMoneyToInt(salaryValue),
                            avatarUrl: avatarUrl || null,
                            startDate: null,
                            leaveFrom: st === 'ON_LEAVE' ? (leaveFromVal || null) : null,
                            leaveTo: st === 'ON_LEAVE' ? (leaveToVal || null) : null,
                            leftOn: st === 'INACTIVE' ? (leftOnVal || null) : null,
                            accountUsername: accUser,
                            accountPassword: accPass,
                            accountEmail: accEmail || null
                        };

                        fetch('/api/staff', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(payload)
                        }).then(function (res) {
                            if (!res.ok) {
                                return res.json().catch(function () { return null; }).then(function (b) {
                                    throw new Error((b && b.error) ? b.error : 'Lỗi tạo nhân viên');
                                });
                            }
                            return res.json();
                        }).then(function () {
                            alert('Đã thêm nhân viên và tạo tài khoản đăng nhập (đã gán staff).');
                            modal.hide();
                            window.location.reload();
                        }).catch(function (err) {
                            console.error(err);
                            alert(err && err.message ? err.message : 'Có lỗi xảy ra khi thêm nhân viên mới.');
                        });
                    });
                }
            }

            var selectedStatusFilter = 'ALL';

            function applyStaffCardVisibility() {
                var keyword = searchInput ? searchInput.value.trim().toLowerCase() : '';
                document.querySelectorAll('.staff-card-clickable').forEach(function (card) {
                    var nameEl = card.querySelector('.staff-name');
                    var roleEl = card.querySelector('.staff-role');
                    var text = '';
                    if (nameEl) text += nameEl.textContent.toLowerCase();
                    if (roleEl) text += ' ' + roleEl.textContent.toLowerCase();
                    if (keyword && text.indexOf(keyword) === -1) {
                        card.parentElement.style.display = 'none';
                        return;
                    }
                    var stData = card.getAttribute('data-effective-status') || card.getAttribute('data-status') || '';
                    if (selectedStatusFilter !== 'ALL' && stData !== selectedStatusFilter) {
                        card.parentElement.style.display = 'none';
                        return;
                    }
                    card.parentElement.style.display = '';
                });
            }

            document.querySelectorAll('.staff-status-filters .staff-filter').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    document.querySelectorAll('.staff-status-filters .staff-filter').forEach(function (b) { b.classList.remove('active'); });
                    btn.classList.add('active');
                    selectedStatusFilter = btn.getAttribute('data-status-filter') || 'ALL';
                    applyStaffCardVisibility();
                });
            });

            if (searchInput) {
                searchInput.addEventListener('input', applyStaffCardVisibility);
            }

            // Mở modal chi tiết/chỉnh sửa khi bấm vào thẻ nhân viên
            if (detailModalEl) {
                var detailModal = new bootstrap.Modal(detailModalEl);
                var cards = document.querySelectorAll('.staff-card-clickable');
                var editForm = document.getElementById('editStaffForm');
                var editStaffStatusEl = document.getElementById('editStaffStatus');
                if (editStaffStatusEl) {
                    editStaffStatusEl.addEventListener('change', syncEditStaffStatusRows);
                }

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
                        var leaveFrom = card.getAttribute('data-leave-from') || '';
                        var leaveTo = card.getAttribute('data-leave-to') || '';
                        var leftOn = card.getAttribute('data-left-on') || '';
                        var loginUser = card.getAttribute('data-login-user') || '';
                        var loginEmail = card.getAttribute('data-login-email') || '';

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

                        var linkedUserEl = document.getElementById('editStaffLinkedUsername');
                        var linkedEmailEl = document.getElementById('editStaffLinkedEmail');
                        var newPwEl = document.getElementById('editStaffNewPassword');
                        if (linkedUserEl) {
                            linkedUserEl.value = (loginUser && loginUser !== 'null') ? loginUser : '';
                            linkedUserEl.placeholder = (loginUser && loginUser !== 'null') ? '' : 'Chưa có tài khoản';
                        }
                        if (linkedEmailEl) {
                            linkedEmailEl.value = (loginEmail && loginEmail !== 'null') ? loginEmail : '';
                        }
                        if (newPwEl) newPwEl.value = '';

                        var elf = document.getElementById('editStaffLeaveFrom');
                        var elt = document.getElementById('editStaffLeaveTo');
                        var elo = document.getElementById('editStaffLeftOn');
                        if (elf) elf.value = leaveFrom && leaveFrom !== 'null' ? String(leaveFrom).substring(0, 10) : '';
                        if (elt) elt.value = leaveTo && leaveTo !== 'null' ? String(leaveTo).substring(0, 10) : '';
                        if (elo) elo.value = leftOn && leftOn !== 'null' ? String(leftOn).substring(0, 10) : '';
                        syncEditStaffStatusRows();

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
                        var leaveFromVal = document.getElementById('editStaffLeaveFrom') ? document.getElementById('editStaffLeaveFrom').value : '';
                        var leaveToVal = document.getElementById('editStaffLeaveTo') ? document.getElementById('editStaffLeaveTo').value : '';
                        var leftOnVal = document.getElementById('editStaffLeftOn') ? document.getElementById('editStaffLeftOn').value : '';

                        if (!id || !name) {
                            alert('Vui lòng nhập đầy đủ thông tin bắt buộc.');
                            return;
                        }

                        if (status === 'ON_LEAVE' && !leaveFromVal && !leaveToVal) {
                            alert('Nghỉ phép: vui lòng chọn ít nhất một ngày (từ hoặc đến).');
                            return;
                        }
                        if (status === 'INACTIVE' && !leftOnVal) {
                            leftOnVal = todayIsoForInput();
                        }

                        var linkedEmailField = document.getElementById('editStaffLinkedEmail');
                        var linkedEmailStr = linkedEmailField ? linkedEmailField.value.trim() : '';
                        var newPwField = document.getElementById('editStaffNewPassword');
                        var newPwStr = newPwField ? newPwField.value : '';

                        var payload = {
                            id: Number(id),
                            name: name,
                            phone: phone || null,
                            role: role,
                            status: status,
                            shift: shift || null,
                            salary: parseVnMoneyToInt(salaryValue),
                            avatarUrl: avatarUrl || null,
                            startDate: startDate || null,
                            leaveFrom: status === 'ON_LEAVE' ? (leaveFromVal || null) : null,
                            leaveTo: status === 'ON_LEAVE' ? (leaveToVal || null) : null,
                            leftOn: status === 'INACTIVE' ? (leftOnVal || null) : null,
                            linkedAccountEmail: linkedEmailStr
                        };
                        if (newPwStr.trim()) {
                            payload.linkedAccountNewPassword = newPwStr;
                        }

                        fetch('/api/staff/update', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(payload)
                        }).then(function (res) {
                            if (!res.ok) {
                                return res.json().catch(function () { return null; }).then(function (b) {
                                    throw new Error((b && b.error) ? b.error : 'Lỗi cập nhật nhân viên');
                                });
                            }
                            return res.json();
                        }).then(function (updated) {
                            // Cập nhật lại thẻ nhân viên tương ứng trên giao diện
                            var card = document.querySelector('.staff-card-clickable[data-id="' + updated.id + '"]');
                            if (card) {
                                var lfU = jsonDateToIsoInput(updated.leaveFrom);
                                var ltU = jsonDateToIsoInput(updated.leaveTo);
                                var loU = jsonDateToIsoInput(updated.leftOn);
                                card.setAttribute('data-name', updated.name || '');
                                card.setAttribute('data-phone', updated.phone || '');
                                card.setAttribute('data-role', updated.role || '');
                                card.setAttribute('data-status', updated.status || '');
                                var effU = effectiveStaffCardStatus(updated.status || '', lfU, ltU);
                                card.setAttribute('data-effective-status', effU);
                                card.setAttribute('data-shift', updated.shift || '');
                                card.setAttribute('data-salary', updated.salary || '');
                                card.setAttribute('data-start-date', jsonDateToIsoInput(updated.startDate) || '');
                                card.setAttribute('data-avatar-url', updated.avatarUrl || '');
                                card.setAttribute('data-leave-from', lfU || '');
                                card.setAttribute('data-leave-to', ltU || '');
                                card.setAttribute('data-left-on', loU || '');
                                card.setAttribute('data-login-email', linkedEmailStr);
                                setCardExtraMeta(card, updated.status, lfU, ltU, loU);
                                updateStaffCardLeaveDaysMonth(card);

                                var sdU = jsonDateToIsoInput(updated.startDate);
                                var startRow = card.querySelector('.staff-start-date-row');
                                var startText = card.querySelector('.staff-start-date-text');
                                if (startText && startRow) {
                                    if (sdU) {
                                        startText.textContent = formatIsoDateVi(sdU);
                                        startRow.style.display = '';
                                    } else {
                                        startRow.style.display = 'none';
                                    }
                                }

                                var npw = document.getElementById('editStaffNewPassword');
                                if (npw) npw.value = '';

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

                                setStaffCardBadgesForEffective(card, effU);
                                applyStaffCardVisibility();

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
                            alert(err && err.message ? err.message : 'Có lỗi xảy ra khi cập nhật nhân viên.');
                        });
                    });
                }
            }

            // Hiển thị lương & giờ làm trên thẻ nhân viên (chỉ trang admin)
            (function loadAllSalarySummaryForAdmin() {
                if (!document.getElementById('roleAdminFlag')) return;
                var cards = document.querySelectorAll('.staff-card-clickable');
                if (!cards.length) return;

                refreshAllStaffLeaveDaysMonthLabels();

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

