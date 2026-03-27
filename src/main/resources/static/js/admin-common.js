document.addEventListener('DOMContentLoaded', function () {
  var logoutLinks = document.querySelectorAll('.logout-btn a');

  if (!logoutLinks || logoutLinks.length === 0) {
    return;
  }

  logoutLinks.forEach(function (link) {
    link.addEventListener('click', function (event) {
      // Nếu href chưa được thiết lập (vd: '#') thì bỏ qua
      var targetHref = link.getAttribute('href');
      if (!targetHref || targetHref === '#') {
        return;
      }

      event.preventDefault();
      var confirmed = window.confirm('Bạn có muốn đăng xuất?');
      if (confirmed) {
        window.location.href = targetHref;
      }
    });
  });
});
