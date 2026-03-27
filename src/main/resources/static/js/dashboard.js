document.addEventListener('DOMContentLoaded', function() {
  const canvas = document.getElementById('revenueChart');
  const exportBtn = document.querySelector('.export-btn');

  if (exportBtn) {
    exportBtn.addEventListener('click', function () {
      window.location.href = '/api/reports/orders-7-days';
    });
  }

  if (!canvas) {
    return;
  }

  const ctx = canvas.getContext('2d');

  const gradient = ctx.createLinearGradient(0, 0, 0, 300);
  gradient.addColorStop(0, 'rgba(255, 193, 7, 0.3)');
  gradient.addColorStop(1, 'rgba(255, 193, 7, 0)');

  function buildChart(labels, data) {
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Doanh thu',
            data: data,
            backgroundColor: '#FFC107',
            borderRadius: 16,
            maxBarThickness: 32,
            borderSkipped: false
          }
        ]
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
  }

  fetch('/api/revenue/daily')
    .then(function(res) { return res.ok ? res.json() : []; })
    .then(function(data) {
      if (!Array.isArray(data) || !data.length) {
        return;
      }

      data.sort(function(a, b) {
        if (!a.revenueDate || !b.revenueDate) return 0;
        return a.revenueDate.localeCompare(b.revenueDate);
      });

      var last = data.slice(-7);
      var labels = last.map(function(item) {
        if (!item.revenueDate) return '';
        var d = new Date(item.revenueDate);
        var day = d.getDay();
        var map = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
        return map[day];
      });
      var values = last.map(function(item) {
        return Number(item.totalRevenue || 0);
      });

      buildChart(labels, values);
    })
    .catch(function() {
      // Nếu lỗi, giữ trống biểu đồ
    });
});
