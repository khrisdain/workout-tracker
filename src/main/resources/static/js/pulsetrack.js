/* ==========================================================================
   PulseTrack front-end behaviour.

   Charts are declared in the markup with data- attributes so the templates
   stay free of inline script and the JSON is produced server side by Thymeleaf.
   ========================================================================== */
(function () {
    "use strict";

    const ACCENT = "#ff5a3c";
    const INK = "#0e1420";
    const PALETTE = ["#ff5a3c", "#2563eb", "#16a34a", "#f59e0b", "#7c3aed", "#0891b2"];

    function parse(el, attr, fallback) {
        try {
            return JSON.parse(el.getAttribute(attr)) || fallback;
        } catch (e) {
            return fallback;
        }
    }

    /** Line chart: training minutes per day. */
    function renderTrend(canvas) {
        const labels = parse(canvas, "data-labels", []);
        const values = parse(canvas, "data-values", []);
        if (!labels.length) {
            return;
        }
        new Chart(canvas, {
            type: "line",
            data: {
                labels: labels,
                datasets: [{
                    label: "Minutes",
                    data: values,
                    borderColor: ACCENT,
                    backgroundColor: "rgba(255, 90, 60, .14)",
                    fill: true,
                    tension: .35,
                    pointRadius: 0,
                    pointHoverRadius: 4,
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, grid: { color: "rgba(15,23,42,.06)" }, ticks: { precision: 0 } },
                    x: { grid: { display: false }, ticks: { maxTicksLimit: 8 } }
                }
            }
        });
    }

    /** Doughnut chart: share of sessions by workout type. */
    function renderBreakdown(canvas) {
        const labels = parse(canvas, "data-labels", []);
        const values = parse(canvas, "data-values", []);
        if (!labels.length) {
            return;
        }
        new Chart(canvas, {
            type: "doughnut",
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: PALETTE.slice(0, labels.length),
                    borderWidth: 0,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "62%",
                plugins: {
                    legend: { position: "bottom", labels: { boxWidth: 10, usePointStyle: true, color: INK } }
                }
            }
        });
    }

    /** Horizontal bar: macro grams from the nutrition microservice. */
    function renderMacros(canvas) {
        const labels = parse(canvas, "data-labels", []);
        const values = parse(canvas, "data-values", []);
        if (!labels.length) {
            return;
        }
        new Chart(canvas, {
            type: "bar",
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: ["#2563eb", "#f59e0b", "#16a34a"],
                    borderRadius: 6,
                    barThickness: 26
                }]
            },
            options: {
                indexAxis: "y",
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { beginAtZero: true, grid: { color: "rgba(15,23,42,.06)" } },
                    y: { grid: { display: false } }
                }
            }
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (typeof Chart !== "undefined") {
            document.querySelectorAll("canvas[data-chart='trend']").forEach(renderTrend);
            document.querySelectorAll("canvas[data-chart='breakdown']").forEach(renderBreakdown);
            document.querySelectorAll("canvas[data-chart='macros']").forEach(renderMacros);
        }

        // Any form marked data-confirm asks before it destroys something.
        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.getAttribute("data-confirm"))) {
                    event.preventDefault();
                }
            });
        });

        // Submitting the filter bar should always land on page one.
        document.querySelectorAll("form[data-reset-page]").forEach(function (form) {
            form.addEventListener("submit", function () {
                const page = form.querySelector("input[name='page']");
                if (page) {
                    page.value = "0";
                }
            });
        });
    });
})();
