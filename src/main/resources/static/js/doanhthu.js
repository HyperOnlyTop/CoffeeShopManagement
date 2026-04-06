document.addEventListener('DOMContentLoaded', function() {
  var revenueTotalEl = document.getElementById('revTotal7');
  var revenueCostEl = document.getElementById('revCost7');
  var revenueProfitEl = document.getElementById('revProfit7');
  var revenueMarginDescEl = document.getElementById('revMarginDesc');
  var revTitleRevenueEl = document.getElementById('revTitleRevenue');
  var revDescRevenueEl = document.getElementById('revDescRevenue');
  var revSummaryPeriodNoteEl = document.getElementById('revSummaryPeriodNote');
  var exportBtn = document.getElementById('revenueExportBtn');

  var revenueCanvas = document.getElementById('revenueChart');
  var ordersCanvas = document.getElementById('ordersChart');

  var categoryList = document.getElementById('categoryProgressList');

  var currentDays = 7;

  if (exportBtn) {
    exportBtn.addEventListener('click', function () {
      window.location.href = '/api/reports/revenue?days=' + currentDays;
    });
  }

  var categoryColors = ['#C98D4D', '#6366F1', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];

  function loadCategoryRevenue(days) {
    if (!categoryList) return;

    fetch('/api/revenue/by-category?days=' + days)
      .then(function (res) { return res.ok ? res.json() : []; })
      .then(function (data) {
        if (!Array.isArray(data) || data.length === 0) {
          categoryList.innerHTML = '<div class="col-12 text-muted">Chưa có dữ liệu</div>';
          return;
        }

        var html = '';
        data.forEach(function (item, idx) {
          var color = categoryColors[idx % categoryColors.length];
          var pct = Number(item.percent || 0);
          var rev = Number(item.revenue || 0);
          var cost = Number(item.cost || 0);
          var profit = Number(item.profit || 0);
          var marginPct = Number(item.marginPercent || 0);
          var profitClass = profit >= 0 ? 'text-success' : 'text-danger';

          html += '<div class="col-md-4 col-sm-6 mb-3">' +
            '<div class="p-3 border rounded h-100">' +
            '<div class="d-flex justify-content-between align-items-center mb-2">' +
            '<span class="fw-semibold">' + (item.category || 'Khác') + '</span>' +
            '<span class="badge" style="background-color: ' + color + ';">' + pct.toLocaleString('vi-VN') + '%</span>' +
            '</div>' +
            '<div class="small text-muted mb-1">Doanh thu: <span class="text-dark">' + rev.toLocaleString('vi-VN') + ' đ</span></div>' +
            '<div class="small text-muted mb-1">Giá vốn: <span class="text-dark">' + cost.toLocaleString('vi-VN') + ' đ</span></div>' +
            '<div class="small mb-1">Lãi gộp: <span class="fw-medium ' + profitClass + '">' + profit.toLocaleString('vi-VN') + ' đ</span></div>' +
            '<div class="small text-muted">Biên LN: <span class="' + profitClass + '">' + marginPct.toLocaleString('vi-VN') + '%</span></div>' +
            '</div>' +
            '</div>';
        });
        categoryList.innerHTML = html;
      })
      .catch(function () {
        categoryList.innerHTML = '<div class="col-12 text-muted">Lỗi tải dữ liệu</div>';
      });
  }

  function loadSummaryMetrics(days) {
    fetch('/api/revenue/summary?days=' + days)
      .then(function (res) { return res.ok ? res.json() : null; })
      .then(function (data) {
        if (!data) return;

        var periodLabel = days === 30 ? '30 ngày gần nhất' : '7 ngày gần nhất';
        if (revSummaryPeriodNoteEl) {
          revSummaryPeriodNoteEl.innerHTML =
            'Số liệu tổng hợp theo đơn đã hoàn thành (và đơn chưa gán trạng thái) trong <strong>' + periodLabel +
            '</strong>; giá vốn lấy từ <strong>giá vốn món</strong> khi tạo đơn.';
        }
        if (revTitleRevenueEl) {
          revTitleRevenueEl.textContent = days === 30 ? 'Doanh thu (30 ngày)' : 'Doanh thu (7 ngày)';
        }
        if (revDescRevenueEl) {
          revDescRevenueEl.textContent = 'Tổng tiền đơn đã tính doanh thu';
        }

        if (revenueTotalEl) {
          var total = Number(data.totalRevenue || 0);
          revenueTotalEl.textContent = total.toLocaleString('vi-VN') + ' đ';
        }
        if (revenueCostEl) {
          var cost = Number(data.totalCost || 0);
          revenueCostEl.textContent = cost.toLocaleString('vi-VN') + ' đ';
        }
        if (revenueProfitEl) {
          var profit = Number(data.grossProfit || 0);
          revenueProfitEl.textContent = profit.toLocaleString('vi-VN') + ' đ';
        }
        if (revenueMarginDescEl) {
          var pct = data.grossMarginPercent != null ? Number(data.grossMarginPercent) : null;
          revenueMarginDescEl.textContent =
            pct != null && !isNaN(pct)
              ? 'Biên lợi nhuận gộp: ' + pct.toLocaleString('vi-VN') + '%'
              : 'Biên lợi nhuận gộp: —';
        }
      })
      .catch(function () { });
  }

  var revenueChartInstance = null;
  var ordersChartInstance = null;

  function loadChartData(days) {
    currentDays = days;
    loadSummaryMetrics(days);
    loadCategoryRevenue(days);

    if (!revenueCanvas || !ordersCanvas || !window.Chart) {
      return;
    }

    var revenueCard = revenueCanvas.closest('.custom-card');
    var revSub = revenueCard ? revenueCard.querySelector('.chart-subtitle') : null;
    if (revSub) {
      revSub.textContent = days + ' ngày gần nhất';
    }
    var ordersSubEl = document.getElementById('ordersChartSubtitle');
    if (ordersSubEl) {
      ordersSubEl.textContent = days + ' ngày gần nhất';
    }

    fetch('/api/revenue/daily?days=' + days)
      .then(function (res) { return res.ok ? res.json() : []; })
      .then(function (data) {
        if (!Array.isArray(data) || !data.length) return;

        data.sort(function (a, b) {
          if (!a.revenueDate || !b.revenueDate) return 0;
          return a.revenueDate.localeCompare(b.revenueDate);
        });

        var labels = data.map(function (item) {
          if (!item.revenueDate) return '';
          var d = new Date(item.revenueDate);
          var dayNum = d.getDate();
          var monthNum = d.getMonth() + 1;
          if (days > 14) {
            return dayNum + '/' + monthNum;
          } else {
            var map = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
            return map[d.getDay()] + ' ' + dayNum + '/' + monthNum;
          }
        });

        var revenueValues = data.map(function (item) {
          return Number(item.totalRevenue || 0);
        });

        var ordersValues = data.map(function (item) {
          return Number(item.totalOrders || 0);
        });

        // Revenue Line Chart
        var ctxRevenue = revenueCanvas.getContext('2d');
        if (revenueChartInstance) revenueChartInstance.destroy();
        
        var gradient = ctxRevenue.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, 'rgba(201, 141, 77, 0.2)');
        gradient.addColorStop(1, 'rgba(201, 141, 77, 0)');

        revenueChartInstance = new Chart(ctxRevenue, {
          type: 'line',
          data: {
            labels: labels,
            datasets: [{
              label: 'Doanh Thu',
              data: revenueValues,
              borderColor: '#C98D4D',
              backgroundColor: gradient,
              borderWidth: 2,
              tension: 0.4,
              fill: true,
              pointBackgroundColor: '#FFF',
              pointBorderColor: '#C98D4D',
              pointBorderWidth: 2,
              pointRadius: days > 14 ? 1 : 3,
              pointHoverRadius: 6
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false },
              tooltip: {
                backgroundColor: '#1E1B18',
                titleFont: { family: 'Inter', size: 13 },
                bodyFont: { family: 'Inter', size: 13 },
                padding: 10,
                cornerRadius: 8,
                displayColors: false,
                callbacks: {
                  label: function (context) {
                    var value = Number(context.raw || 0);
                    return value.toLocaleString('vi-VN') + ' đ';
                  }
                }
              }
            },
            scales: {
              x: {
                grid: { display: false, drawBorder: false },
                ticks: { 
                    color: '#8F9198', 
                    font: { family: 'Inter', size: 11 },
                    maxRotation: 0,
                    autoSkip: true,
                    maxTicksLimit: days > 14 ? 10 : 7
                }
              },
              y: {
                border: { display: false },
                grid: { color: '#F0F0F0', borderDash: [5, 5] },
                ticks: {
                  color: '#8F9198',
                  font: { family: 'Inter', size: 12 },
                  callback: function (value) {
                    return Number(value).toLocaleString('vi-VN');
                  }
                },
                suggestedMin: 0
              }
            }
          }
        });

        // Orders Bar Chart
        var ctxOrders = ordersCanvas.getContext('2d');
        if (ordersChartInstance) ordersChartInstance.destroy();

        var maxOrders = Math.max.apply(null, ordersValues);
        var yMaxOrders = Math.max(10, Math.ceil(maxOrders / 5) * 5 + 5);

        ordersChartInstance = new Chart(ctxOrders, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: 'Số Đơn Hàng',
              data: ordersValues,
              backgroundColor: '#C98D4D',
              borderRadius: 4,
              borderSkipped: false,
              barThickness: days > 14 ? 'flex' : 32
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false },
              tooltip: {
                backgroundColor: '#1E1B18',
                titleFont: { family: 'Inter', size: 13 },
                bodyFont: { family: 'Inter', size: 13 },
                padding: 10,
                cornerRadius: 8,
                displayColors: false,
                callbacks: {
                  label: function (context) {
                    return context.raw + ' đơn';
                  }
                }
              }
            },
            scales: {
              x: {
                grid: { display: false, drawBorder: false },
                ticks: { 
                    color: '#8F9198', 
                    font: { family: 'Inter', size: 11 },
                    maxRotation: 0,
                    autoSkip: true,
                    maxTicksLimit: days > 14 ? 10 : 7
                }
              },
              y: {
                border: { display: false },
                grid: { color: '#F0F0F0', borderDash: [5, 5] },
                ticks: {
                  color: '#8F9198',
                  font: { family: 'Inter', size: 12 },
                  stepSize: 5,
                  callback: function (value) {
                    if (value % 5 === 0) {
                      return value;
                    }
                    return '';
                  }
                },
                min: 0,
                max: yMaxOrders
              }
            }
          }
        });
      })
      .catch(function () { });
  }

  // Handle filter buttons
  var filterGroup = document.getElementById('revenueFilterGroup');
  if (filterGroup) {
    var buttons = filterGroup.querySelectorAll('.btn');
    buttons.forEach(function(btn) {
      btn.addEventListener('click', function() {
        buttons.forEach(function(b) { b.classList.remove('active'); });
        this.classList.add('active');
        var days = parseInt(this.getAttribute('data-days'));
        loadChartData(days);
      });
    });
  }

  // Initial load (summary + charts + category)
  loadChartData(7);
});
