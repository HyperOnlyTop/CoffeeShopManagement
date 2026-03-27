document.addEventListener('DOMContentLoaded', function() {
  var revenueTotalEl = document.getElementById('revTotal7');
  var revenueOrdersEl = document.getElementById('revOrders7');
  var revenueAvgEl = document.getElementById('revAvgOrder');
  var bestDayLabelEl = document.getElementById('revBestDayLabel');
  var bestDayRevenueEl = document.getElementById('revBestDayRevenue');

  var revenueCanvas = document.getElementById('revenueChart');
  var ordersCanvas = document.getElementById('ordersChart');

  var categoryList = document.getElementById('categoryProgressList');
  var topProductsList = document.getElementById('topProductsList');

  // Load summary metrics
  fetch('/api/revenue/summary-7-days')
    .then(function (res) { return res.ok ? res.json() : null; })
    .then(function (data) {
      if (!data) return;

      if (revenueTotalEl) {
        var total = Number(data.totalRevenue || 0);
        revenueTotalEl.textContent = total.toLocaleString('vi-VN') + ' đ';
      }
      if (revenueOrdersEl) {
        revenueOrdersEl.textContent = Number(data.totalOrders || 0).toLocaleString('vi-VN');
      }
      if (revenueAvgEl) {
        var avg = Number(data.averageOrderValue || 0);
        revenueAvgEl.textContent = avg.toLocaleString('vi-VN') + ' đ';
      }
      if (bestDayLabelEl) {
        bestDayLabelEl.textContent = data.bestDayLabel || '--';
      }
      if (bestDayRevenueEl) {
        var bestRev = Number(data.bestDayRevenue || 0);
        bestDayRevenueEl.textContent = bestRev.toLocaleString('vi-VN') + ' đ';
      }
    })
    .catch(function () { });

  // Load daily revenue & orders for charts
  if (revenueCanvas && ordersCanvas && window.Chart) {
    fetch('/api/revenue/daily')
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
          var day = d.getDay();
          var map = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
          return map[day];
        });

        var revenueValues = data.map(function (item) {
          return Number(item.totalRevenue || 0);
        });

        var ordersValues = data.map(function (item) {
          return Number(item.totalOrders || 0);
        });

        var ctxRevenue = revenueCanvas.getContext('2d');
        var gradient = ctxRevenue.createLinearGradient(0, 0, 0, 400);
        gradient.addColorStop(0, 'rgba(201, 141, 77, 0.2)');
        gradient.addColorStop(1, 'rgba(201, 141, 77, 0)');

        new Chart(ctxRevenue, {
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
              pointRadius: 3,
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
                ticks: { color: '#8F9198', font: { family: 'Inter', size: 12 } }
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

        var ctxOrders = ordersCanvas.getContext('2d');
        new Chart(ctxOrders, {
          type: 'bar',
          data: {
            labels: labels,
            datasets: [{
              label: 'Số Đơn Hàng',
              data: ordersValues,
              backgroundColor: '#C98D4D',
              borderRadius: 4,
              borderSkipped: false,
              barThickness: 32
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
                displayColors: false
              }
            },
            scales: {
              x: {
                grid: { display: false, drawBorder: false },
                ticks: { color: '#8F9198', font: { family: 'Inter', size: 12 } }
              },
              y: {
                border: { display: false },
                grid: { color: '#F0F0F0', borderDash: [5, 5] },
                ticks: {
                  color: '#8F9198',
                  font: { family: 'Inter', size: 12 }
                },
                suggestedMin: 0
              }
            }
          }
        });
      })
      .catch(function () { });
  }

  // Load category revenue and top products could be added here later
});
