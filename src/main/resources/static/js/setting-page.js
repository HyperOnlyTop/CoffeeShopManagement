    document.addEventListener('DOMContentLoaded', function () {
      // Tabs logic
      var tabButtons = document.querySelectorAll('.settings-tab');
      var tabContents = document.querySelectorAll('.settings-tab-content');

      function activateTab(tabName) {
        tabButtons.forEach(function (btn) {
          if (btn.dataset.tab === tabName) {
            btn.classList.add('active');
          } else {
            btn.classList.remove('active');
          }
        });

        tabContents.forEach(function (section) {
          if (section.dataset.tabContent === tabName) {
            section.classList.add('active');
          } else {
            section.classList.remove('active');
          }
        });
      }

      tabButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
          var target = btn.dataset.tab;
          if (target) {
            activateTab(target);
          }
        });
      });



      // Thanh toán QR (VietQR) — cập nhật preview
      var bankNameSelect = document.getElementById('bankNameSelect');
      var bankAccountNumberInput = document.getElementById('bankAccountNumber');
      var bankAccountHolderInput = document.getElementById('bankAccountHolder');

      var qrPreviewImage = document.getElementById('qrPreviewImage');
      var qrBankNamePreview = document.getElementById('qrBankNamePreview');
      var qrAccountNumberPreview = document.getElementById('qrAccountNumberPreview');
      var qrAccountHolderPreview = document.getElementById('qrAccountHolderPreview');

      function updateQrPreview() {
        if (!qrBankNamePreview || !qrAccountNumberPreview || !qrAccountHolderPreview) {
          return;
        }

        qrBankNamePreview.textContent = bankNameSelect && bankNameSelect.value ? bankNameSelect.value : 'Ngân hàng';
        qrAccountNumberPreview.textContent = bankAccountNumberInput && bankAccountNumberInput.value ? bankAccountNumberInput.value : '—';
        qrAccountHolderPreview.textContent = bankAccountHolderInput && bankAccountHolderInput.value ? bankAccountHolderInput.value : '—';

        if (qrPreviewImage) {
          qrPreviewImage.alt = 'QR chuyển khoản ngân hàng (VietQR)';
        }
      }

      if (bankNameSelect) bankNameSelect.addEventListener('input', updateQrPreview);
      if (bankAccountNumberInput) bankAccountNumberInput.addEventListener('input', updateQrPreview);
      if (bankAccountHolderInput) bankAccountHolderInput.addEventListener('input', updateQrPreview);

      updateQrPreview();

      var paymentSaveButton = document.getElementById('paymentSaveButton');
      if (paymentSaveButton) {
        paymentSaveButton.addEventListener('click', function () {
          if (!qrPreviewImage) {
            alert('Không thể tạo QR, vui lòng tải lại trang và thử lại.');
            return;
          }

          var bankName = bankNameSelect ? bankNameSelect.value : '';
          var accountNumber = bankAccountNumberInput ? bankAccountNumberInput.value.trim() : '';
          var accountHolder = bankAccountHolderInput ? bankAccountHolderInput.value.trim() : '';

          var bankCodeMap = {
            'Vietcombank (VCB)': 'vietcombank',
            'ACB': 'acb',
            'Techcombank': 'techcombank',
            'VPBank': 'vpbank',
            'BIDV': 'bidv'
          };

          var bankCode = bankCodeMap[bankName] || null;

          var payload = {
            bankCode: bankCode,
            bankName: bankName || null,
            bankAccount: accountNumber || null,
            bankOwnerName: accountHolder || null,
            bankEnabled: !!accountNumber
          };

          fetch('/api/settings/payment', {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
          })
            .then(function (response) {
              if (!response.ok) {
                throw new Error('Lưu cấu hình thanh toán thất bại');
              }
              return response.json();
            })
            .then(function () {
              if (!bankCode || !accountNumber || !accountHolder) {
                alert('Đã lưu cấu hình nhưng thiếu thông tin ngân hàng để tạo VietQR.');
              } else {
                var amount = 75000;
                var addInfo = 'Thanh toan tai Brew & Co.';
                var qrUrl = 'https://img.vietqr.io/image/'
                  + bankCode + '-' + encodeURIComponent(accountNumber)
                  + '-compact2.png?amount=' + amount
                  + '&addInfo=' + encodeURIComponent(addInfo)
                  + '&accountName=' + encodeURIComponent(accountHolder);
                qrPreviewImage.src = qrUrl;
                qrPreviewImage.alt = 'VietQR chuyển khoản ngân hàng';
              }

              updateQrPreview();
              alert('Đã lưu cấu hình thanh toán và cập nhật QR thành công.');
            })
            .catch(function () {
              alert('Không thể lưu cấu hình thanh toán. Vui lòng thử lại.');
            });
        });
      }

      const saveButton = document.querySelector('.settings-save-btn');

      const shopNameInput = document.getElementById('shopName');
      const shopPhoneInput = document.getElementById('shopPhone');
      const shopAddressInput = document.getElementById('shopAddress');
      const shopEmailInput = document.getElementById('shopEmail');
      const shopWebsiteInput = document.getElementById('shopWebsite');
      const shopTaxCodeInput = document.getElementById('shopTaxCode');
      const weekdayOpenInput = document.getElementById('weekdayOpen');
      const weekdayCloseInput = document.getElementById('weekdayClose');
      const saturdayOpenInput = document.getElementById('saturdayOpen');
      const saturdayCloseInput = document.getElementById('saturdayClose');
      const sundayOpenInput = document.getElementById('sundayOpen');
      const sundayCloseInput = document.getElementById('sundayClose');
      const shopDescriptionInput = document.getElementById('shopDescription');

      function loadSettings() {
        fetch('/api/settings/shop')
          .then(function (response) {
            if (!response.ok) {
              return null;
            }
            return response.json();
          })
          .then(function (data) {
            if (!data) {
              return;
            }

            if (data.shopName) shopNameInput.value = data.shopName;
            if (data.phone) shopPhoneInput.value = data.phone;
            if (data.address) shopAddressInput.value = data.address;
            if (data.email) shopEmailInput.value = data.email;
            if (data.website) shopWebsiteInput.value = data.website;
            if (data.taxCode) shopTaxCodeInput.value = data.taxCode;
            if (data.weekdayOpen) weekdayOpenInput.value = data.weekdayOpen;
            if (data.weekdayClose) weekdayCloseInput.value = data.weekdayClose;
            if (data.saturdayOpen) saturdayOpenInput.value = data.saturdayOpen;
            if (data.saturdayClose) saturdayCloseInput.value = data.saturdayClose;
            if (data.sundayOpen) sundayOpenInput.value = data.sundayOpen;
            if (data.sundayClose) sundayCloseInput.value = data.sundayClose;
            if (data.description) shopDescriptionInput.value = data.description;
          })
          .catch(function () {
            // Nếu lỗi, giữ nguyên giá trị mặc định trên form
          });
      }

      function saveSettings() {
        const payload = {
          shopName: shopNameInput.value.trim(),
          phone: shopPhoneInput.value.trim(),
          address: shopAddressInput.value.trim(),
          email: shopEmailInput.value.trim(),
          website: shopWebsiteInput.value.trim(),
          taxCode: shopTaxCodeInput.value.trim(),
          weekdayOpen: weekdayOpenInput.value.trim(),
          weekdayClose: weekdayCloseInput.value.trim(),
          saturdayOpen: saturdayOpenInput.value.trim(),
          saturdayClose: saturdayCloseInput.value.trim(),
          sundayOpen: sundayOpenInput.value.trim(),
          sundayClose: sundayCloseInput.value.trim(),
          description: shopDescriptionInput.value.trim()
        };

        fetch('/api/settings/shop', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        })
          .then(function (response) {
            if (!response.ok) {
              throw new Error('Lưu cài đặt thất bại');
            }
            return response.json();
          })
          .then(function () {
            alert('Đã lưu cài đặt quán cà phê thành công.');
          })
          .catch(function () {
            alert('Không thể lưu cài đặt. Vui lòng thử lại.');
          });
      }

      if (saveButton) {
        saveButton.addEventListener('click', function () {
          saveSettings();
        });
      }

      // Tải cấu hình thanh toán (VietQR / ngân hàng)
      function loadPaymentSettings() {
        fetch('/api/settings/payment')
          .then(function (response) {
            if (!response.ok) {
              return null;
            }
            return response.json();
          })
          .then(function (data) {
            if (!data) {
              return;
            }

            if (bankNameSelect && data.bankName) {
              bankNameSelect.value = data.bankName;
            }
            if (bankAccountNumberInput && data.bankAccount) {
              bankAccountNumberInput.value = data.bankAccount;
            }
            if (bankAccountHolderInput && data.bankOwnerName) {
              bankAccountHolderInput.value = data.bankOwnerName;
            }

            updateQrPreview();
          })
          .catch(function () {
            // Nếu lỗi, giữ nguyên giá trị mặc định
          });
      }

      // Xử lý cập nhật thông tin tài khoản
      const updateProfileForm = document.getElementById('updateProfileForm');
      if (updateProfileForm) {
        updateProfileForm.addEventListener('submit', function (e) {
          e.preventDefault();
          const fullName = document.getElementById('profileFullName').value;
          const email = document.getElementById('profileEmail').value;
          const phone = document.getElementById('profilePhone').value;
          const avatarUrl = document.getElementById('profileAvatarUrl').value;

          fetch('/api/account/update-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, phone, avatarUrl })
          })
            .then(res => res.json())
            .then(data => {
              if (data.message) {
                alert(data.message);
                location.reload();
              } else {
                alert(data.error || 'Cập nhật thất bại');
              }
            })
            .catch(() => alert('Lỗi kết nối server'));
        });
      }

      // Xử lý đổi mật khẩu
      const changePasswordForm = document.getElementById('changePasswordForm');
      if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', function (e) {
          e.preventDefault();
          const currentPassword = document.getElementById('currentPassword').value;
          const newPassword = document.getElementById('newPassword').value;
          const confirmPassword = document.getElementById('confirmPassword').value;

          if (newPassword !== confirmPassword) {
            alert('Mật khẩu xác nhận không khớp');
            return;
          }

          fetch('/api/account/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ currentPassword, newPassword, confirmPassword })
          })
            .then(res => res.json())
            .then(data => {
              if (data.message) {
                alert(data.message);
                changePasswordForm.reset();
              } else {
                alert(data.error || 'Đổi mật khẩu thất bại');
              }
            })
            .catch(() => alert('Lỗi kết nối server'));
        });
      }

      // Xử lý tải ảnh đại diện từ máy tính
      const avatarFileInput = document.getElementById('avatarFileInput');
      if (avatarFileInput) {
        avatarFileInput.addEventListener('change', function () {
          const file = this.files[0];
          if (!file) return;

          const formData = new FormData();
          formData.append('file', file);

          fetch('/api/account/upload-avatar', {
            method: 'POST',
            body: formData
          })
            .then(res => res.json())
            .then(data => {
              if (data.avatarUrl) {
                document.getElementById('profileAvatarPreviewLarge').src = data.avatarUrl;
                document.getElementById('profileAvatarUrl').value = data.avatarUrl;
                alert('Đã tải ảnh lên thành công. Vui lòng bấm "Cập nhật thông tin" để lưu thay đổi.');
              } else {
                alert(data.error || 'Tải ảnh thất bại');
              }
            })
            .catch(() => alert('Lỗi kết nối khi tải ảnh'));
        });
      }

      loadSettings();
      loadPaymentSettings();
    });
