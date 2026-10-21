/*
 * 스타 인사이트 그래프 5종 (2-34차 2-1·2-2) — 글로벌·국가 어드민 스타 상세와 스타 본인 마이페이지 공용.
 * Chart.js 를 먼저 불러와야 한다. 데이터는 StarInsightService.getSeries 응답 그대로:
 *   { labels[], series{rank, visits, follows, likes, posts}, totals{...}, rankNow }
 * 사용: InsightCharts.render(document.getElementById('insightCharts'), data)
 *       InsightCharts.render(el, data, { metrics: ['visits', 'likes'] }) — 일부 지표만 (2-35차 대시보드 합산은 순위 제외)
 * 화면 글자는 스타에게도 보이므로 영어로 쓴다.
 */
(function (global) {
  'use strict';

  var METRICS = [
    { key: 'rank', title: 'Global Rank', color: '#7c4dff', type: 'line' },
    { key: 'visits', title: 'Visitors', color: '#ff4d6d', type: 'bar' },
    { key: 'follows', title: 'Favorites', color: '#ffb703', type: 'bar' },
    { key: 'likes', title: 'Likes', color: '#e5383b', type: 'bar' },
    { key: 'posts', title: 'Posts', color: '#219ebc', type: 'bar' }
  ];

  var charts = [];

  function summary(metric, data) {
    if (metric.key === 'rank') {
      return data.rankNow ? '#' + Number(data.rankNow).toLocaleString() + ' now' : 'Not ranked yet';
    }
    var total = data.totals ? data.totals[metric.key] : 0;
    return Number(total || 0).toLocaleString() + ' in this period';
  }

  function render(container, data, opts) {
    var only = opts && opts.metrics ? opts.metrics : null;
    charts.forEach(function (c) { c.destroy(); });
    charts = [];
    container.innerHTML = '';
    if (!data || !data.series) {
      container.innerHTML = '<div class="text-secondary py-4 text-center">No data.</div>';
      return;
    }

    METRICS.forEach(function (metric) {
      if (only && only.indexOf(metric.key) < 0) return;
      if (!data.series[metric.key]) return;
      var card = document.createElement('div');
      card.className = 'insight-chart-card';
      var head = document.createElement('div');
      head.className = 'insight-chart-head';
      var title = document.createElement('b');
      title.textContent = metric.title;
      var sub = document.createElement('span');
      sub.textContent = summary(metric, data);
      head.appendChild(title);
      head.appendChild(sub);
      var box = document.createElement('div');
      box.className = 'insight-chart-box';
      var canvas = document.createElement('canvas');
      box.appendChild(canvas);
      card.appendChild(head);
      card.appendChild(box);
      container.appendChild(card);

      var values = data.series[metric.key] || [];
      var isRank = metric.key === 'rank';
      charts.push(new Chart(canvas.getContext('2d'), {
        type: metric.type,
        data: {
          labels: data.labels,
          datasets: [{
            data: values,
            backgroundColor: metric.color,
            borderColor: metric.color,
            borderWidth: isRank ? 2 : 0,
            pointRadius: isRank ? 2 : 0,
            spanGaps: false,
            tension: 0.25
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: false,
          plugins: { legend: { display: false } },
          scales: {
            x: { ticks: { maxTicksLimit: 8, maxRotation: 0 }, grid: { display: false } },
            // 순위는 작을수록 위 — 축을 뒤집는다. 순위가 없는 칸(스냅샷 전)은 끊어 그린다
            y: isRank
              ? { reverse: true, ticks: { precision: 0 }, beginAtZero: false }
              : { beginAtZero: true, ticks: { precision: 0 } }
          }
        }
      }));
    });
  }

  global.InsightCharts = { render: render };
})(window);
