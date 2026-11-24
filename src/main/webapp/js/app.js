(function () {
  const page = document.body.dataset.page;

  renderSidebar();
  bindMenu();

  if (page === "dashboard") loadDashboard();
  if (page === "upload") bindUpload();
  if (page === "history") loadHistory();
  if (page === "accessibility") loadAccessibility();
  if (page === "analysis") loadAnalysis();

  function renderSidebar() {
    const host = document.getElementById("sidebar");
    const active = page;
    host.innerHTML = '<aside class="sidebar" id="sidebarPanel"><div class="brand"><span class="brand-mark">S</span><span>ScholarBridge</span></div><nav class="nav">' +
      navLink("dashboard", "dashboard.html", "Dashboard") +
      navLink("upload", "upload.html", "Upload Research Paper") +
      navLink("analysis", "history.html", "Quality Auditor") +
      navLink("accessibility", "accessibility.html", "Multilingual Accessibility") +
      navLink("history", "history.html", "Document History") + '</nav></aside>';
  }

  function navLink(name, href, label) {
    return '<a class="' + ((page === name || (name === "analysis" && page === "history")) ? "active" : "") +
      '" href="' + href + '">' + label + '</a>';
  }

  function bindMenu() {
    const button = document.getElementById("menuButton");
    if (!button) return;
    button.addEventListener("click", function () {
      document.getElementById("sidebarPanel").classList.toggle("open");
    });
  }

  async function api(url, options) {
    const response = await fetch(url, options);
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || data.message || "Request failed");
    return data;
  }

  async function loadDashboard() {
    try {
      const data = await api("api/dashboard");
      document.getElementById("stats").innerHTML =
        stat("Total documents", data.totalDocuments) +
        stat("Average quality", data.averageScore + "/100") +
        stat("Detected issues", data.totalIssues);
      document.getElementById("recentUploads").innerHTML = table(
        ["Document", "Upload date"], data.recentUploads.map(function (row) {
          return [escapeHtml(row.name), escapeHtml(row.uploadDate)];
        }));
      document.getElementById("recentAudits").innerHTML = table(
        ["Document", "Score", "Issues"], data.recentAudits.map(function (row) {
          return [escapeHtml(row.name), row.qualityScore + "/100", row.issueCount];
        }));
    } catch (error) {
      showNotice(error.message);
    }
  }

  function stat(label, value) {
    return '<article class="stat"><span>' + escapeHtml(label) + '</span><strong>' + escapeHtml(String(value)) + '</strong></article>';
  }

  function bindUpload() {
    document.getElementById("uploadForm").addEventListener("submit", async function (event) {
      event.preventDefault();
      const status = document.getElementById("uploadStatus");
      const file = document.getElementById("pdfFile").files[0];
      if (!file) return;
      status.innerHTML = '<p class="notice">Uploading and analyzing...</p>';
      const form = new FormData();
      form.append("file", file);
      try {
        const data = await api("api/upload", { method: "POST", body: form });
        status.innerHTML = '<p class="notice">Upload complete.</p>';
        document.getElementById("uploadResult").innerHTML =
          '<div class="metadata"><div><strong>Quality score</strong>' + data.qualityScore + '/100</div>' +
          '<div><strong>Issues detected</strong>' + data.issueCount + '</div></div>' +
          '<p><a class="button primary" href="analysis.html?id=' + data.documentId + '">View complete analysis</a></p>';
      } catch (error) {
        status.innerHTML = '<p class="notice">' + escapeHtml(error.message) + '</p>';
      }
    });
  }

  async function loadHistory() {
    try {
      const rows = await api("api/documents");
      document.getElementById("historyTable").innerHTML = table(
        ["Document", "Upload date", "Quality", "Issues", "Status", "Actions"],
        rows.map(function (row) {
          return [
            escapeHtml(row.name),
            escapeHtml(row.uploadDate),
            '<span class="badge">' + row.qualityScore + '/100</span>',
            row.issueCount,
            escapeHtml(row.status),
            '<div class="actions"><a class="button secondary" href="analysis.html?id=' + row.id + '">View</a>' +
            '<button class="button secondary delete-document" data-id="' + row.id + '">Delete</button></div>'
          ];
        }));
      document.querySelectorAll(".delete-document").forEach(function (button) {
        button.addEventListener("click", async function () {
          if (!confirm("Delete this document and all associated analysis records?")) return;
          try {
            await api("api/documents/" + button.dataset.id, { method: "DELETE" });
            loadHistory();
          } catch (error) {
            alert(error.message);
          }
        });
      });
    } catch (error) {
      document.getElementById("historyTable").innerHTML = '<p class="notice">' + escapeHtml(error.message) + '</p>';
    }
  }

  async function loadAccessibility() {
    try {
      const rows = await api("api/documents");
      const select = document.getElementById("documentSelect");
      select.innerHTML = rows.map(function (row) {
        return '<option value="' + row.id + '">' + escapeHtml(row.name) + '</option>';
      }).join("");
      if (rows.length) {
        await loadAbstract(rows[0].id);
      } else {
        document.getElementById("originalAbstract").textContent = "No documents available.";
      }
      select.addEventListener("change", function () { loadAbstract(select.value); });
      document.getElementById("generateButton").addEventListener("click", generateTranslation);
    } catch (error) {
      document.getElementById("originalAbstract").textContent = error.message;
    }
  }

  async function loadAbstract(id) {
    try {
      const document = await api("api/documents/" + id);
      document.getElementById("originalAbstract").textContent = document.abstractText || "Abstract could not be detected.";
    } catch (error) {
      document.getElementById("originalAbstract").textContent = error.message;
    }
  }

  async function generateTranslation() {
    const id = document.getElementById("documentSelect").value;
    const language = document.getElementById("languageSelect").value;
    const output = document.getElementById("translationOutput");
    output.textContent = "Generating...";
    try {
      const data = await api("api/translations/" + id, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ language: language })
      });
      output.textContent = "AI-generated: " + data.content;
    } catch (error) {
      output.textContent = error.message;
    }
  }

  async function loadAnalysis() {
    const id = new URLSearchParams(location.search).get("id");
    if (!id) {
      document.getElementById("analysisContent").innerHTML = '<article class="card"><p>Select a document from history.</p></article>';
      return;
    }
    try {
      const data = await api("api/documents/" + id);
      document.getElementById("analysisTitle").textContent = data.name;
      let html = '<article class="card"><h2>Extracted metadata</h2><div class="metadata">' +
        metadata("Title", data.title) + metadata("Authors", data.authors) +
        metadata("DOI", data.doi) + metadata("Keywords", data.keywords) +
        '</div><h3>Abstract</h3><div class="text-output">' + escapeHtml(data.abstractText || "Not detected") +
        '</div></article>';
      html += '<article class="card"><h2>Quality audit</h2>';
      if (data.audit) {
        html += '<div class="metadata"><div><strong>Overall score</strong>' + data.audit.qualityScore + '/100</div>' +
          '<div><strong>Issues</strong>' + data.audit.totalIssues + '</div>' +
          '<div><strong>Critical / high</strong>' + data.audit.criticalCount + '</div>' +
          '<div><strong>Medium</strong>' + data.audit.mediumCount + '</div></div>' +
          '<p class="muted">' + escapeHtml(data.audit.scoreBreakdown) + '</p>';
      }
      html += '<h3>Recommendations</h3>';
      html += data.issues.length ? '<ul>' + data.issues.map(function (issue) {
        return '<li><span class="badge ' + (issue.severity === "CRITICAL" ? "danger" : "warn") + '">' +
          escapeHtml(issue.severity) + '</span> <strong>' + escapeHtml(issue.category) + ':</strong> ' +
          escapeHtml(issue.recommendation) + '</li>';
      }).join("") + '</ul>' : '<p>No issues detected.</p>';
      html += '</article>';
      document.getElementById("analysisContent").innerHTML = html;
    } catch (error) {
      document.getElementById("analysisContent").innerHTML = '<article class="card"><p class="notice">' + escapeHtml(error.message) + '</p></article>';
    }
  }

  function metadata(label, value) {
    return '<div><strong>' + escapeHtml(label) + '</strong>' + escapeHtml(value || "Not detected") + '</div>';
  }

  function table(headers, rows) {
    return '<table><thead><tr>' + headers.map(function (header) {
      return '<th>' + escapeHtml(header) + '</th>';
    }).join("") + '</tr></thead><tbody>' + rows.map(function (row) {
      return '<tr>' + row.map(function (value) { return '<td>' + value + '</td>'; }).join("") + '</tr>';
    }).join("") + '</tbody></table>';
  }

  function showNotice(message) {
    document.querySelectorAll(".table-wrap").forEach(function (element) {
      element.innerHTML = '<p class="notice">' + escapeHtml(message) + '</p>';
    });
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (character) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[character];
    });
  }
})();
