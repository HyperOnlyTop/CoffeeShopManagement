document.addEventListener('DOMContentLoaded', function () {
var toggleBtn = document.getElementById('chatWidgetToggle');
        var panel = document.getElementById('chatWidget');
        var closeBtn = document.getElementById('chatWidgetClose');
        var messagesEl = document.getElementById('chatMessages');
        var inputEl = document.getElementById('chatInput');
        var sendBtn = document.getElementById('chatSendBtn');

        // Featured menu navigation
        var featuredTrack = document.getElementById('featuredMenuTrack');
        var featuredPrev = document.getElementById('featuredMenuPrev');
        var featuredNext = document.getElementById('featuredMenuNext');
        function scrollFeatured(direction) {
            if (!featuredTrack) return;
            var step = Math.max(320, Math.floor(featuredTrack.clientWidth * 0.9));
            featuredTrack.scrollBy({ left: direction * step, behavior: 'smooth' });
        }
        if (featuredPrev) featuredPrev.addEventListener('click', function () { scrollFeatured(-1); });
        if (featuredNext) featuredNext.addEventListener('click', function () { scrollFeatured(1); });

        // Featured reviews navigation
        var reviewTrack = document.getElementById('featuredReviewTrack');
        var reviewPrev = document.getElementById('featuredReviewPrev');
        var reviewNext = document.getElementById('featuredReviewNext');
        function scrollReviews(direction) {
            if (!reviewTrack) return;
            var step = Math.max(380, Math.floor(reviewTrack.clientWidth * 0.9));
            reviewTrack.scrollBy({ left: direction * step, behavior: 'smooth' });
        }
        if (reviewPrev) reviewPrev.addEventListener('click', function () { scrollReviews(-1); });
        if (reviewNext) reviewNext.addEventListener('click', function () { scrollReviews(1); });

        if (!toggleBtn || !panel || !closeBtn || !messagesEl || !inputEl || !sendBtn) {
            return;
        }

        function togglePanel() {
            if (panel.style.display === 'flex') {
                panel.style.display = 'none';
            } else {
                panel.style.display = 'flex';
                inputEl.focus();
                if (!messagesEl.dataset.initialized) {
                    messagesEl.innerHTML = '';
                    addMessage('Xin chào! Hãy để lại câu hỏi, chúng tôi sẽ phản hồi trong thời gian sớm nhất.', 'bot');
                    messagesEl.dataset.initialized = '1';
                }
            }
        }

        function addMessage(text, author) {
            var row = document.createElement('div');
            row.className = 'chat-msg-row' + (author === 'me' ? ' me' : '');
            var bubble = document.createElement('div');
            bubble.className = 'chat-msg-bubble ' + (author === 'me' ? 'me' : 'bot');
            bubble.textContent = text;
            row.appendChild(bubble);
            messagesEl.appendChild(row);
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }

        function handleUserMessage() {
            var text = inputEl.value.trim();
            if (!text) return;
            addMessage(text, 'me');
            inputEl.value = '';

            addMessage('Đang gửi câu hỏi đến AI, vui lòng chờ...', 'bot');

            fetch('/api/chat/ai', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: text })
            })
                .then(function (res) {
                    return res.json()
                        .then(function (data) { return { ok: res.ok, data: data }; })
                        .catch(function () { return { ok: res.ok, data: null }; });
                })
                .then(function (result) {
                    var ok = result.ok;
                    var data = result.data;
                    if (!ok) {
                        var errorMsg = data && data.error
                            ? data.error
                            : 'AI hiện không trả lời được. Vui lòng thử lại sau.';
                        addMessage(errorMsg, 'bot');
                        return;
                    }

                    var reply = data && data.reply
                        ? data.reply
                        : 'AI hiện không trả lời được. Vui lòng thử lại sau.';
                    addMessage(reply, 'bot');
                })
                .catch(function () {
                    addMessage('Có lỗi khi gọi dịch vụ AI. Vui lòng thử lại sau.', 'bot');
                });
        }

        toggleBtn.addEventListener('click', togglePanel);
        closeBtn.addEventListener('click', function () {
            panel.style.display = 'none';
        });

        sendBtn.addEventListener('click', handleUserMessage);
        inputEl.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleUserMessage();
            }
        });
});

document.addEventListener('DOMContentLoaded', function () {
var bookingSection = document.getElementById('booking');
        var dateInput = document.getElementById('bookingDate');
        var timeSelect = document.getElementById('bookingTime');
        var MIN_LEAD_MS = 2 * 60 * 60 * 1000;

        function pad(n) { return (n < 10 ? '0' : '') + n; }
        function ymd(d) {
            return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
        }
        function slotHours() {
            var a = [];
            for (var h = 7; h <= 21; h++) a.push(pad(h) + ':00');
            return a;
        }
        function nextYmdFromParts(y, mo, d) {
            var dt = new Date(y, mo, d);
            dt.setDate(dt.getDate() + 1);
            return ymd(dt);
        }
        /** Chỉ giữ các khung giờ ≥ hiện tại + 2 giờ; nếu ngày chọn không còn slot thì tự nhảy ngày tiếp theo (im lặng, không báo đỏ). */
        function refreshTimeOptions() {
            if (!dateInput || !timeSelect) return;
            var previous = timeSelect.value;
            var minDate = dateInput.min;
            var guard = 0;
            while (guard++ < 62) {
                var dateVal = dateInput.value;
                if (!dateVal) return;
                if (dateVal < minDate) {
                    dateInput.value = minDate;
                    continue;
                }
                var parts = dateVal.split('-');
                var y = parseInt(parts[0], 10);
                var mo = parseInt(parts[1], 10) - 1;
                var d = parseInt(parts[2], 10);
                var now = new Date();
                var minBook = new Date(now.getTime() + MIN_LEAD_MS);
                timeSelect.innerHTML = '';
                var ph = document.createElement('option');
                ph.value = '';
                ph.disabled = true;
                ph.selected = true;
                ph.textContent = 'Chọn giờ';
                timeSelect.appendChild(ph);
                var any = false;
                slotHours().forEach(function (t) {
                    var hm = t.split(':');
                    var dt = new Date(y, mo, d, parseInt(hm[0], 10), parseInt(hm[1], 10), 0, 0);
                    if (dt.getTime() >= minBook.getTime()) {
                        var opt = document.createElement('option');
                        opt.value = t;
                        opt.textContent = t;
                        timeSelect.appendChild(opt);
                        any = true;
                    }
                });
                if (any) {
                    if (previous) {
                        var match = timeSelect.querySelector('option[value="' + previous + '"]');
                        if (match) {
                            ph.selected = false;
                            match.selected = true;
                        }
                    }
                    timeSelect.disabled = false;
                    return;
                }
                var nextStr = nextYmdFromParts(y, mo, d);
                if (!nextStr || nextStr < minDate) {
                    timeSelect.disabled = true;
                    return;
                }
                dateInput.value = nextStr;
            }
            timeSelect.disabled = true;
        }
        if (dateInput && timeSelect) {
            var today = new Date();
            dateInput.min = ymd(today);
            if (!dateInput.value || dateInput.value < dateInput.min) {
                dateInput.value = dateInput.min;
            }
            dateInput.addEventListener('change', refreshTimeOptions);
            dateInput.addEventListener('input', refreshTimeOptions);
            refreshTimeOptions();
            var bookingForm = dateInput.closest('form');
            if (bookingForm) {
                bookingForm.addEventListener('submit', function (e) {
                    if (timeSelect.disabled || !timeSelect.value) {
                        e.preventDefault();
                        refreshTimeOptions();
                        if (!timeSelect.disabled && timeSelect.value) {
                            return;
                        }
                        timeSelect.focus();
                        if (typeof timeSelect.reportValidity === 'function') {
                            timeSelect.reportValidity();
                        }
                    }
                });
            }
            setInterval(refreshTimeOptions, 60 * 1000);
        }

        var lookupToggle = document.getElementById('bookingLookupToggle');
        var lookupPanel = document.getElementById('bookingLookupPanel');
        var lookupResults = document.getElementById('bookingLookupResults');
        var loggedIn = bookingSection && bookingSection.getAttribute('data-user-logged-in') === 'true';

        function statusVi(code) {
            var m = { CONFIRMED: 'Đã xác nhận', CANCELLED: 'Đã hủy', CHECKED_IN: 'Đã check-in', COMPLETED: 'Hoàn tất', NO_SHOW: 'Không đến' };
            return m[code] || code;
        }
        function escapeHtml(s) {
            if (s == null) return '';
            return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }
        function renderList(rows) {
            if (!lookupResults) return;
            if (!rows || rows.length === 0) {
                lookupResults.innerHTML = '<p class="mb-0 text-muted-coffee">Không tìm thấy lịch đặt nào.</p>';
                return;
            }
            var html = '<ul class="list-unstyled mb-0">';
            rows.forEach(function (r) {
                html += '<li class="mb-3 pb-3 border-bottom">';
                html += '<div class="fw-semibold">' + escapeHtml(r.customerName) + '</div>';
                html += '<div>Giờ đến: <span class="text-nowrap">' + escapeHtml(String(r.bookingTime)) + '</span></div>';
                html += '<div>Số khách: ' + r.guests + ' · Trạng thái: ' + escapeHtml(statusVi(r.status)) + '</div>';
                if (r.note) html += '<div class="text-muted">Ghi chú: ' + escapeHtml(r.note) + '</div>';
                html += '</li>';
            });
            html += '</ul>';
            lookupResults.innerHTML = html;
        }
        function fetchMyBookings() {
            lookupResults.innerHTML = '<p class="mb-0 text-muted-coffee">Đang tải…</p>';
            fetch('/api/public/bookings/me', { credentials: 'same-origin' })
                .then(function (res) {
                    return res.json().then(function (data) { return { ok: res.ok, data: data }; });
                })
                .then(function (x) {
                    if (!x.ok) {
                        lookupResults.innerHTML = '<p class="mb-0 text-danger">Không tải được (phiên đăng nhập có thể đã hết). Vui lòng đăng nhập lại.</p>';
                        return;
                    }
                    renderList(Array.isArray(x.data) ? x.data : []);
                })
                .catch(function () {
                    lookupResults.innerHTML = '<p class="mb-0 text-danger">Lỗi kết nối. Vui lòng thử lại.</p>';
                });
        }
        function clearLookupGuestError() {
            var el = document.getElementById('lookupGuestError');
            if (el) {
                el.textContent = '';
                el.classList.add('d-none');
            }
        }
        function showLookupGuestError(msg) {
            var el = document.getElementById('lookupGuestError');
            if (el) {
                el.textContent = msg;
                el.classList.remove('d-none');
            }
        }
        var lookupPhoneEl = document.getElementById('lookupPhone');
        var lookupEmailEl = document.getElementById('lookupEmail');
        if (lookupPhoneEl) lookupPhoneEl.addEventListener('input', clearLookupGuestError);
        if (lookupEmailEl) lookupEmailEl.addEventListener('input', clearLookupGuestError);

        function fetchGuestLookup() {
            var pEl = document.getElementById('lookupPhone');
            var eEl = document.getElementById('lookupEmail');
            var p = pEl ? pEl.value.trim() : '';
            var em = eEl ? eEl.value.trim() : '';
            clearLookupGuestError();
            if (p && em) {
                showLookupGuestError('Chỉ nhập một trong hai: số điện thoại hoặc email (xóa trường còn lại).');
                return;
            }
            if (!p && !em) {
                showLookupGuestError('Vui lòng nhập số điện thoại hoặc email đã dùng khi đặt bàn.');
                return;
            }
            var q = p ? ('phone=' + encodeURIComponent(p)) : ('email=' + encodeURIComponent(em));
            lookupResults.innerHTML = '<p class="mb-0 text-muted-coffee">Đang tìm…</p>';
            fetch('/api/public/bookings/lookup?' + q, { credentials: 'same-origin' })
                .then(function (res) {
                    return res.json().then(function (data) { return { ok: res.ok, data: data }; });
                })
                .then(function (x) {
                    if (!x.ok && x.data && x.data.error) {
                        lookupResults.innerHTML = '<p class="mb-0 text-danger">' + escapeHtml(x.data.error) + '</p>';
                        return;
                    }
                    if (!x.ok) {
                        lookupResults.innerHTML = '<p class="mb-0 text-danger">Không tra cứu được.</p>';
                        return;
                    }
                    renderList(Array.isArray(x.data) ? x.data : []);
                })
                .catch(function () {
                    lookupResults.innerHTML = '<p class="mb-0 text-danger">Lỗi kết nối. Vui lòng thử lại.</p>';
                });
        }
        if (lookupToggle && lookupPanel) {
            lookupToggle.addEventListener('click', function () {
                lookupPanel.classList.toggle('d-none');
                var open = !lookupPanel.classList.contains('d-none');
                lookupToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
                if (open && !loggedIn) {
                    clearLookupGuestError();
                }
                if (open && loggedIn) {
                    fetchMyBookings();
                }
            });
        }
        var guestBtn = document.getElementById('lookupGuestBtn');
        if (guestBtn) guestBtn.addEventListener('click', fetchGuestLookup);

        function setInlineAlert(el, message, isError) {
            if (!el) return;
            el.textContent = message;
            el.classList.remove('d-none', 'alert-success', 'alert-danger', 'alert', 'small', 'py-2', 'px-3', 'mb-0');
            el.classList.add('alert', 'small', 'py-2', 'px-3', 'mb-0', isError ? 'alert-danger' : 'alert-success');
            el.classList.remove('d-none');
            try {
                el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            } catch (e) { /* ignore */ }
        }

        var landingBookingForm = document.getElementById('landingBookingForm');
        var bookingAjaxAlert = document.getElementById('bookingAjaxAlert');
        if (landingBookingForm && bookingAjaxAlert) {
            landingBookingForm.addEventListener('submit', function (e) {
                e.preventDefault();
                fetch('/api/public/bookings', {
                    method: 'POST',
                    body: new FormData(landingBookingForm),
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json' }
                })
                    .then(function (res) {
                        return res.json().then(function (data) {
                            return { ok: res.ok, data: data };
                        }).catch(function () {
                            return { ok: false, data: { ok: false, message: 'Không đọc được phản hồi từ máy chủ.' } };
                        });
                    })
                    .then(function (x) {
                        var d = x.data || {};
                        if (x.ok && d.ok) {
                            setInlineAlert(bookingAjaxAlert, d.message || 'Đặt bàn thành công. Quán sẽ giữ bàn tối đa 15 phút.', false);
                            var noteEl = document.getElementById('bookingNote');
                            if (noteEl) noteEl.value = '';
                        } else {
                            setInlineAlert(bookingAjaxAlert, d.message || 'Có lỗi khi đặt bàn.', true);
                        }
                    })
                    .catch(function () {
                        setInlineAlert(bookingAjaxAlert, 'Lỗi kết nối. Vui lòng thử lại.', true);
                    });
            });
        }

        var landingReviewForm = document.getElementById('landingReviewForm');
        var reviewAjaxAlert = document.getElementById('reviewAjaxAlert');
        if (landingReviewForm && reviewAjaxAlert) {
            landingReviewForm.addEventListener('submit', function (e) {
                e.preventDefault();
                fetch('/api/public/reviews', {
                    method: 'POST',
                    body: new FormData(landingReviewForm),
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json' }
                })
                    .then(function (res) {
                        return res.json().then(function (data) {
                            return { ok: res.ok, data: data };
                        }).catch(function () {
                            return { ok: false, data: { ok: false, message: 'Không đọc được phản hồi từ máy chủ.' } };
                        });
                    })
                    .then(function (x) {
                        var d = x.data || {};
                        if (x.ok && d.ok) {
                            reviewAjaxAlert.classList.remove('d-none', 'alert-danger');
                            reviewAjaxAlert.classList.add('alert-success');
                            reviewAjaxAlert.textContent = d.message || 'Đã ghi nhận đánh giá.';
                            var c = document.getElementById('reviewComment');
                            if (c) c.value = '';
                            var r = document.getElementById('reviewRating');
                            if (r) r.value = '5';
                        } else {
                            reviewAjaxAlert.classList.remove('d-none', 'alert-success');
                            reviewAjaxAlert.classList.add('alert-danger');
                            reviewAjaxAlert.textContent = d.message || 'Không gửi được đánh giá.';
                        }
                        try {
                            reviewAjaxAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                        } catch (err) { /* ignore */ }
                    })
                    .catch(function () {
                        reviewAjaxAlert.classList.remove('d-none', 'alert-success');
                        reviewAjaxAlert.classList.add('alert-danger');
                        reviewAjaxAlert.textContent = 'Lỗi kết nối. Vui lòng thử lại.';
                    });
            });
        }
});
