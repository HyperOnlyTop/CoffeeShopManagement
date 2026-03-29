document.addEventListener('DOMContentLoaded', function () {
  var logoutLinks = document.querySelectorAll('.logout-btn a');

  if (!logoutLinks || logoutLinks.length === 0) {
    return;
  }

  // Tạo modal xác nhận đăng xuất (dùng Bootstrap nếu có)
  var logoutModal = document.getElementById('logoutConfirmModal');
  if (!logoutModal) {
    var modalHtml =
      '<div class="modal fade" id="logoutConfirmModal" tabindex="-1" aria-hidden="true">' +
      '  <div class="modal-dialog modal-dialog-centered">' +
      '    <div class="modal-content">' +
      '      <div class="modal-header">' +
      '        <h5 class="modal-title">Xác nhận đăng xuất</h5>' +
      '        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
      '      </div>' +
      '      <div class="modal-body">' +
      '        <p>Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?</p>' +
      '      </div>' +
      '      <div class="modal-footer">' +
      '        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>' +
      '        <button type="button" class="btn btn-danger" id="logoutConfirmButton">Đăng xuất</button>' +
      '      </div>' +
      '    </div>' +
      '  </div>' +
      '</div>';

    if (document.body) {
      document.body.insertAdjacentHTML('beforeend', modalHtml);
      logoutModal = document.getElementById('logoutConfirmModal');
    }
  }

  var logoutConfirmButton = document.getElementById('logoutConfirmButton');
  var currentLogoutHref = null;

  function openLogoutModal(targetHref) {
    currentLogoutHref = targetHref;

    if (typeof bootstrap !== 'undefined' && logoutModal) {
      try {
        var modalInstance = bootstrap.Modal.getOrCreateInstance(logoutModal);
        modalInstance.show();
        return;
      } catch (e) {
        // fallback bên dưới
      }
    }

    // Fallback nếu không có Bootstrap: dùng confirm mặc định
    var confirmed = window.confirm('Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?');
    if (confirmed && currentLogoutHref) {
      window.location.href = currentLogoutHref;
    }
  }

  if (logoutConfirmButton) {
    logoutConfirmButton.addEventListener('click', function () {
      if (currentLogoutHref) {
        window.location.href = currentLogoutHref;
      }
    });
  }

  logoutLinks.forEach(function (link) {
    link.addEventListener('click', function (event) {
      var targetHref = link.getAttribute('href');
      if (!targetHref || targetHref === '#') {
        return;
      }

      event.preventDefault();
      openLogoutModal(targetHref);
    });
  });
});
