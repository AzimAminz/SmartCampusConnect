import { SessionManager } from '../utils/session.js';
import { AuthService } from '../services/auth.js';
import { LibraryService } from '../services/libraryService.js';

export const DashboardView = {
  render() {
    const user = SessionManager.getCurrentUser() || { fullName: 'Guest User', userId: 'N/A', role: 'STUDENT' };
    const isAdmin = user.role === 'ADMIN';

    return `
      <style>
        /* Modern UI Glassmorphism & Animations */
        .dash-container {
          max-width: 1280px;
          margin: 0 auto;
          padding: 2rem;
          animation: fadeIn 0.4s ease-out;
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .tab-btn {
          background: rgba(255, 255, 255, 0.03);
          border: 1px solid rgba(255, 255, 255, 0.05);
          color: var(--text-secondary);
          padding: 0.75rem 1.5rem;
          font-family: 'Outfit', sans-serif;
          font-weight: 600;
          font-size: 0.95rem;
          border-radius: var(--radius-md);
          cursor: pointer;
          transition: var(--transition);
        }
        .tab-btn:hover {
          background: rgba(255, 255, 255, 0.08);
          color: var(--text-primary);
        }
        .tab-btn.active {
          background: linear-gradient(135deg, var(--primary), var(--secondary));
          color: white;
          border: none;
          box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
        }
        .section-panel {
          display: none;
          animation: slideUp 0.35s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .section-panel.active {
          display: block;
        }
        @keyframes slideUp {
          from { opacity: 0; transform: translateY(15px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .badge {
          padding: 0.35rem 0.75rem;
          border-radius: 20px;
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.5px;
          text-transform: uppercase;
        }
        .badge-available {
          background: rgba(16, 185, 129, 0.15);
          color: var(--success);
          border: 1px solid rgba(16, 185, 129, 0.25);
        }
        .badge-borrowed {
          background: rgba(239, 68, 68, 0.15);
          color: var(--danger);
          border: 1px solid rgba(239, 68, 68, 0.25);
        }
        .badge-student {
          background: rgba(6, 182, 212, 0.15);
          color: var(--secondary);
          border: 1px solid rgba(6, 182, 212, 0.25);
        }
        .badge-admin {
          background: rgba(79, 70, 229, 0.15);
          color: var(--primary);
          border: 1px solid rgba(79, 70, 229, 0.25);
        }
        .book-card {
          border-radius: var(--radius-md);
          background: rgba(255, 255, 255, 0.02);
          border: 1px solid rgba(255, 255, 255, 0.05);
          padding: 1.5rem;
          transition: var(--transition);
          display: flex;
          flex-direction: column;
          justify-content: space-between;
        }
        .book-card:hover {
          transform: translateY(-5px) scale(1.01);
          background: rgba(255, 255, 255, 0.05);
          border-color: rgba(6, 182, 212, 0.3);
          box-shadow: 0 8px 25px rgba(0, 0, 0, 0.4);
        }
        .sub-nav {
          display: flex;
          gap: 0.5rem;
          margin-bottom: 1.5rem;
          border-bottom: 1px solid rgba(255, 255, 255, 0.05);
          padding-bottom: 0.75rem;
        }
        .sub-tab-btn {
          background: none;
          border: none;
          color: var(--text-secondary);
          padding: 0.5rem 1rem;
          cursor: pointer;
          font-weight: 500;
          font-size: 0.9rem;
          transition: var(--transition);
          border-radius: var(--radius-sm);
        }
        .sub-tab-btn:hover {
          color: var(--text-primary);
          background: rgba(255, 255, 255, 0.03);
        }
        .sub-tab-btn.active {
          color: var(--secondary);
          background: rgba(6, 182, 212, 0.08);
          font-weight: 600;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          margin-top: 1rem;
        }
        th, td {
          padding: 1rem;
          text-align: left;
          border-bottom: 1px solid rgba(255, 255, 255, 0.05);
          font-size: 0.9rem;
        }
        th {
          font-family: 'Outfit', sans-serif;
          color: var(--text-secondary);
          font-weight: 600;
        }
        tr:hover td {
          background: rgba(255, 255, 255, 0.01);
        }
        .input-glow:focus {
          border-color: var(--secondary);
          box-shadow: 0 0 10px rgba(6, 182, 212, 0.2);
        }
      </style>

      <div>
        <!-- Modern Header -->
        <header class="header-nav glass-panel" style="border-radius: 0 0 var(--radius-lg) var(--radius-lg); margin-bottom: 2rem;">
          <div class="logo" style="display: flex; align-items: center; gap: 0.75rem;">
            <div style="font-size: 1.75rem;">🏫</div>
            <div style="display: flex; flex-direction: column;">
              <span class="logo-text" style="font-size: 1.25rem; letter-spacing: -0.5px; line-height: 1.1; background: linear-gradient(135deg, #fff, #9ca3af); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">SmartCampus</span>
              <span style="color: var(--secondary); font-size: 0.7rem; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">Connect Portal</span>
            </div>
          </div>
          <div style="display: flex; align-items: center; gap: 1.5rem;">
            <div style="text-align: right;">
              <div style="font-weight: 600; font-size: 0.95rem; color: var(--text-primary);">${user.fullName}</div>
              <div style="font-size: 0.75rem; color: var(--text-secondary); display: flex; align-items: center; gap: 0.35rem; justify-content: flex-end;">
                <span class="badge ${isAdmin ? 'badge-admin' : 'badge-student'}" style="padding: 1px 6px; font-size: 0.65rem;">${user.role}</span>
                <span>• ${user.userId}</span>
              </div>
            </div>
            <button id="logout-btn" class="btn-primary" style="background: rgba(239, 68, 68, 0.1); color: var(--danger); border: 1px solid rgba(239, 68, 68, 0.2); padding: 0.5rem 1rem; font-size: 0.85rem; border-radius: var(--radius-md);">
              Logout
            </button>
          </div>
        </header>

        <div class="dash-container">
          <!-- Main Tab Navigation -->
          <div style="display: flex; gap: 0.75rem; margin-bottom: 2rem;">
            <button id="nav-overview-btn" class="tab-btn active">📊 General Overview</button>
            <button id="nav-library-btn" class="tab-btn">📚 Campus Library (SOAP)</button>
          </div>

          <!-- Section 1: General Overview -->
          <section id="panel-overview" class="section-panel active">
            <div style="margin-bottom: 2rem;">
              <h1 style="font-size: 2.2rem; margin-bottom: 0.5rem; font-family: 'Outfit';">Matric Dashboard</h1>
              <p style="color: var(--text-secondary);">Distributed Campus Operations Console for SmartCampusConnect platform.</p>
            </div>

            <div class="dashboard-grid" style="padding: 0; margin-bottom: 2rem;">
              <div class="dashboard-card glass-panel">
                <h3 style="margin-bottom: 0.5rem; color: var(--text-secondary); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px;">Identity Profile</h3>
                <div style="font-size: 1.5rem; font-weight: 700; margin: 0.5rem 0; word-break: break-all;">${user.fullName}</div>
                <p style="color: var(--secondary); font-size: 0.85rem;">Access Role: ${user.role}</p>
              </div>
              
              <div class="dashboard-card glass-panel">
                <h3 style="margin-bottom: 0.5rem; color: var(--text-secondary); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px;">Distributed Endpoint</h3>
                <div style="font-size: 1.5rem; font-weight: 700; margin: 0.5rem 0; color: var(--secondary);">JAX-WS SOAP</div>
                <p style="color: var(--text-secondary); font-size: 0.85rem;">Port: 8085 /ws/booking</p>
              </div>

              <div class="dashboard-card glass-panel">
                <h3 style="margin-bottom: 0.5rem; color: var(--text-secondary); font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.5px;">Session Status</h3>
                <div style="font-size: 1.5rem; font-weight: 600; margin: 0.5rem 0; color: var(--success); display: flex; align-items: center; gap: 0.5rem;">
                  <span style="width: 10px; height: 10px; border-radius: 50%; background-color: var(--success); display: inline-block; animation: pulse 2s infinite;"></span>
                  Online & Active
                </div>
                <p style="color: var(--text-secondary); font-size: 0.85rem;">Token validated successfully</p>
              </div>
            </div>

            <!-- Profile Info Panel -->
            <div class="glass-panel" style="padding: 2rem; margin-top: 2rem;">
              <h3 style="margin-bottom: 1.5rem; font-family: 'Outfit';">System Account Summary</h3>
              <div style="display: flex; flex-direction: column; gap: 1rem;">
                <div style="display: flex; justify-content: space-between; padding-bottom: 0.75rem; border-bottom: 1px solid rgba(255,255,255,0.05);">
                  <span style="color: var(--text-secondary);">Matric / Access ID</span>
                  <span style="font-weight: 600;">${user.userId}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding-bottom: 0.75rem; border-bottom: 1px solid rgba(255,255,255,0.05);">
                  <span style="color: var(--text-secondary);">Full Account Name</span>
                  <span style="font-weight: 600;">${user.fullName}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding-bottom: 0.75rem; border-bottom: 1px solid rgba(255,255,255,0.05);">
                  <span style="color: var(--text-secondary);">Authorization Role Privilege</span>
                  <span class="badge ${isAdmin ? 'badge-admin' : 'badge-student'}">${user.role}</span>
                </div>
                <div style="display: flex; justify-content: space-between;">
                  <span style="color: var(--text-secondary);">Server Interface Linkage</span>
                  <span style="color: var(--success); font-family: monospace; font-size: 0.85rem;">REST (Port 8080) & SOAP (Port 8085)</span>
                </div>
              </div>
            </div>
          </section>

          <!-- Section 2: Library (SOAP) -->
          <section id="panel-library" class="section-panel">
            <div style="margin-bottom: 2rem; display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem;">
              <div>
                <h1 style="font-size: 2.2rem; margin-bottom: 0.5rem; font-family: 'Outfit';">Campus Library Catalog</h1>
                <p style="color: var(--text-secondary);">Lookup books and check availability instantly via secure JAX-WS SOAP operations.</p>
              </div>
              <div id="lib-alert-box" style="display: none; padding: 0.75rem 1.5rem; border-radius: var(--radius-md); font-size: 0.9rem; font-weight: 500; min-width: 250px; animation: slideUp 0.3s ease;"></div>
            </div>

            <!-- Library Sub Navigation -->
            <div class="sub-nav">
              <button id="sub-search-btn" class="sub-tab-btn active">🔍 Search Books</button>
              ${isAdmin ? `
                <button id="sub-issue-btn" class="sub-tab-btn">📝 Issue Loan</button>
                <button id="sub-return-btn" class="sub-tab-btn">↩️ Process Return</button>
                <button id="sub-addbook-btn" class="sub-tab-btn">➕ Add New Book</button>
                <button id="sub-audit-btn" class="sub-tab-btn">📋 Audit Loan History</button>
              ` : `
                <button id="sub-history-btn" class="sub-tab-btn">📜 My Loan History</button>
              `}
            </div>

            <!-- Library View 1: Search Books (All) -->
            <div id="lib-search-view" class="lib-sub-panel active" style="display: block;">
              <div class="glass-panel" style="padding: 1.5rem; margin-bottom: 2rem; display: flex; gap: 1rem; align-items: center;">
                <input type="text" id="catalog-search-input" class="form-control input-glow" placeholder="Search by ISBN, title, author, or category..." style="flex: 1; background: rgba(0,0,0,0.2);">
                <button id="catalog-search-btn" class="btn-primary" style="padding: 0.8rem 2rem;">Search</button>
              </div>

              <div id="catalog-results-container">
                <div style="text-align: center; padding: 3rem; color: var(--text-secondary);">
                  <div style="font-size: 2.5rem; margin-bottom: 1rem;">🔍</div>
                  <p>Type a search term above and click search to pull live books from SOAP registry.</p>
                </div>
              </div>
            </div>

            ${isAdmin ? `
              <!-- Library View 2: Issue Loan (Admin Only) -->
              <div id="lib-issue-view" class="lib-sub-panel" style="display: none;">
                <div class="glass-panel" style="padding: 2rem; max-width: 600px; margin: 0 auto;">
                  <h3 style="margin-bottom: 1.5rem; font-family: 'Outfit';">Issue Book Loan Transaction</h3>
                  <form id="issue-loan-form">
                    <div class="form-group">
                      <label for="issue-student-id">Student Matric ID</label>
                      <input type="text" id="issue-student-id" class="form-control" placeholder="e.g. B032310001" required>
                    </div>
                    <div class="form-group">
                      <label for="issue-student-name">Student Full Name</label>
                      <input type="text" id="issue-student-name" class="form-control" placeholder="e.g. Azim Amin" required>
                    </div>
                    <div class="form-group">
                      <label for="issue-book-isbn">Book ISBN Code</label>
                      <input type="text" id="issue-book-isbn" class="form-control" placeholder="e.g. 978-0134685991" required>
                    </div>
                    <div class="form-group" style="margin-bottom: 2rem;">
                      <label for="issue-due-date">Loan Due Date</label>
                      <input type="date" id="issue-due-date" class="form-control" required>
                    </div>
                    <button type="submit" class="btn-primary" style="width: 100%;">Confirm Loan Issue</button>
                  </form>
                </div>
              </div>

              <!-- Library View 3: Return Book (Admin Only) -->
              <div id="lib-return-view" class="lib-sub-panel" style="display: none;">
                <div class="glass-panel" style="padding: 2rem; max-width: 600px; margin: 0 auto;">
                  <h3 style="margin-bottom: 1.5rem; font-family: 'Outfit';">Register Book Return</h3>
                  <p style="color: var(--text-secondary); font-size: 0.85rem; margin-bottom: 1.5rem;">Enter the Loan Reference Code (e.g. LN-XXXXXXXX) to check in a borrowed book and resolve fines.</p>
                  <form id="return-book-form">
                    <div class="form-group" style="margin-bottom: 2rem;">
                      <label for="return-loan-ref">Loan Reference Code</label>
                      <input type="text" id="return-loan-ref" class="form-control" placeholder="e.g. LN-A1B2C3D4" required style="font-family: monospace; text-transform: uppercase;">
                    </div>
                    <button type="submit" class="btn-primary" style="width: 100%;">Process Check-In</button>
                  </form>
                </div>
              </div>

              <!-- Library View 4: Add New Book (Admin Only) -->
              <div id="lib-addbook-view" class="lib-sub-panel" style="display: none;">
                <div class="glass-panel" style="padding: 2rem; max-width: 600px; margin: 0 auto;">
                  <h3 style="margin-bottom: 1.5rem; font-family: 'Outfit';">Register New Master Book</h3>
                  <form id="add-book-form">
                    <div class="form-group">
                      <label for="add-book-isbn">Book ISBN (Unique identifier)</label>
                      <input type="text" id="add-book-isbn" class="form-control" placeholder="e.g. 978-0132350884" required>
                    </div>
                    <div class="form-group">
                      <label for="add-book-title">Book Title</label>
                      <input type="text" id="add-book-title" class="form-control" placeholder="e.g. Clean Code" required>
                    </div>
                    <div class="form-group">
                      <label for="add-book-author">Author Name</label>
                      <input type="text" id="add-book-author" class="form-control" placeholder="e.g. Robert C. Martin" required>
                    </div>
                    <div class="form-group" style="margin-bottom: 2rem;">
                      <label for="add-book-category">Library Category</label>
                      <input type="text" id="add-book-category" class="form-control" placeholder="e.g. Software Engineering" required>
                    </div>
                    <button type="submit" class="btn-primary" style="width: 100%;">Add to Master Catalog</button>
                  </form>
                </div>
              </div>

              <!-- Library View 5: Audit (Admin Only) -->
              <div id="lib-audit-view" class="lib-sub-panel" style="display: none;">
                <div class="glass-panel" style="padding: 2rem; margin-bottom: 2rem;">
                  <h3 style="margin-bottom: 1.5rem; font-family: 'Outfit';">Auditing Services Panel</h3>
                  <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; flex-wrap: wrap;">
                    <!-- Book History Search -->
                    <div style="border-right: 1px solid rgba(255,255,255,0.05); padding-right: 2rem;">
                      <h4 style="margin-bottom: 1rem; color: var(--secondary);">Query Book Loan History</h4>
                      <div class="form-group">
                        <label for="audit-book-isbn">Lookup Book ISBN</label>
                        <div style="display: flex; gap: 0.5rem;">
                          <input type="text" id="audit-book-isbn" class="form-control" placeholder="ISBN Code" style="flex: 1;">
                          <button id="audit-book-btn" class="btn-primary" style="padding: 0.5rem 1.25rem;">Audit</button>
                        </div>
                      </div>
                    </div>
                    
                    <!-- Student History Search -->
                    <div>
                      <h4 style="margin-bottom: 1rem; color: var(--secondary);">Query Student Loan History</h4>
                      <div class="form-group">
                        <label for="audit-student-id">Lookup Student Matric ID</label>
                        <div style="display: flex; gap: 0.5rem;">
                          <input type="text" id="audit-student-id" class="form-control" placeholder="Matric ID" style="flex: 1;">
                          <button id="audit-student-btn" class="btn-primary" style="padding: 0.5rem 1.25rem;">Audit</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="glass-panel" style="padding: 1.5rem; overflow-x: auto;">
                  <h4 style="margin-bottom: 1rem; font-family: 'Outfit';" id="audit-results-title">Audit Log Entries</h4>
                  <div id="audit-results-container">
                    <p style="text-align: center; color: var(--text-secondary); padding: 2rem 0;">No audit queries executed yet. Submit an ISBN or Matric ID above.</p>
                  </div>
                </div>
              </div>
            ` : `
              <!-- Library View 6: Personal History (Student Only) -->
              <div id="lib-history-view" class="lib-sub-panel" style="display: none;">
                <div class="glass-panel" style="padding: 1.5rem; overflow-x: auto;">
                  <h3 style="margin-bottom: 1rem; font-family: 'Outfit';">My Book Loans Profile</h3>
                  <div id="personal-history-container">
                    <p style="text-align: center; color: var(--text-secondary); padding: 2rem 0;">Retrieving personal loan history from SOAP registry...</p>
                  </div>
                </div>
              </div>
            `}
          </section>
        </div>
      </div>
    `;
  },

  afterRender(navigateTo) {
    const user = SessionManager.getCurrentUser() || { fullName: 'Guest User', userId: 'N/A', role: 'STUDENT' };
    const isAdmin = user.role === 'ADMIN';

    // 1. Logout Handler
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        if (confirm('Are you sure you want to exit the application?')) {
          await AuthService.logout();
          navigateTo('login');
        }
      });
    }

    // 2. Alert Notification Helper
    const alertBox = document.getElementById('lib-alert-box');
    function showAlert(msg, isError = false) {
      alertBox.textContent = msg;
      alertBox.style.display = 'block';
      if (isError) {
        alertBox.style.background = 'rgba(239, 68, 68, 0.15)';
        alertBox.style.color = 'var(--danger)';
        alertBox.style.border = '1px solid rgba(239, 68, 68, 0.25)';
      } else {
        alertBox.style.background = 'rgba(16, 185, 129, 0.15)';
        alertBox.style.color = 'var(--success)';
        alertBox.style.border = '1px solid rgba(16, 185, 129, 0.25)';
      }
      setTimeout(() => {
        alertBox.style.display = 'none';
      }, 5000);
    }

    // 3. Main Navigation Tab Handler
    const navOverviewBtn = document.getElementById('nav-overview-btn');
    const navLibraryBtn = document.getElementById('nav-library-btn');
    const panelOverview = document.getElementById('panel-overview');
    const panelLibrary = document.getElementById('panel-library');

    navOverviewBtn.addEventListener('click', () => {
      navOverviewBtn.classList.add('active');
      navLibraryBtn.classList.remove('active');
      panelOverview.classList.add('active');
      panelLibrary.classList.remove('active');
    });

    navLibraryBtn.addEventListener('click', () => {
      navLibraryBtn.classList.add('active');
      navOverviewBtn.classList.remove('active');
      panelLibrary.classList.add('active');
      panelOverview.classList.remove('active');

      // Auto trigger first catalog search on view load
      performBookSearch('');

      // Auto load student history if student
      if (!isAdmin) {
        loadPersonalHistory();
      }
    });

    // 4. Sub Navigation Tab Handler
    const subTabButtons = document.querySelectorAll('.sub-tab-btn');
    const subPanels = document.querySelectorAll('.lib-sub-panel');

    subTabButtons.forEach((btn, idx) => {
      btn.addEventListener('click', () => {
        subTabButtons.forEach(b => b.classList.remove('active'));
        subPanels.forEach(p => p.style.display = 'none');

        btn.classList.add('active');
        subPanels[idx].style.display = 'block';
      });
    });

    // 5. Operation: Search Books Functionality
    const searchInput = document.getElementById('catalog-search-input');
    const searchBtn = document.getElementById('catalog-search-btn');
    const resultsContainer = document.getElementById('catalog-results-container');

    async function performBookSearch(query) {
      resultsContainer.innerHTML = `
        <div style="text-align: center; padding: 2rem; color: var(--text-secondary);">
          <div class="spinner" style="width: 30px; height: 30px; margin: 0 auto 1rem auto;"></div>
          <p>Connecting to JAX-WS server...</p>
        </div>
      `;
      try {
        const books = await LibraryService.searchBooks(query);
        if (books.length === 0) {
          resultsContainer.innerHTML = `
            <div style="text-align: center; padding: 3rem; color: var(--text-secondary);">
              <div style="font-size: 2.5rem; margin-bottom: 1rem;">📭</div>
              <p>No books matching "${query}" were found in the library database.</p>
            </div>
          `;
          return;
        }

        let gridHtml = `<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem; margin-top: 1rem;">`;
        books.forEach(book => {
          const isAvail = book.status === 'AVAILABLE';
          gridHtml += `
            <div class="book-card">
              <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem; gap: 0.5rem;">
                  <span class="badge ${isAvail ? 'badge-available' : 'badge-borrowed'}">${book.status}</span>
                  <span style="font-size: 0.75rem; color: var(--text-secondary); font-family: monospace;">${book.isbn}</span>
                </div>
                <h4 style="font-family: 'Outfit', sans-serif; font-size: 1.15rem; margin-bottom: 0.5rem; color: white; line-height: 1.35;">${book.title}</h4>
                <p style="font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 0.25rem;">✍️ ${book.author || 'Unknown'}</p>
                <p style="font-size: 0.8rem; color: var(--text-secondary);">📁 ${book.category || 'General'}</p>
              </div>
            </div>
          `;
        });
        gridHtml += '</div>';
        resultsContainer.innerHTML = gridHtml;
      } catch (err) {
        resultsContainer.innerHTML = `
          <div style="text-align: center; padding: 2rem; color: var(--danger); background: rgba(239,68,68,0.05); border: 1px dashed rgba(239,68,68,0.15); border-radius: var(--radius-md);">
            <div style="font-size: 2rem; margin-bottom: 0.5rem;">⚠️</div>
            <p>SOAP Connection failed: ${err.message}</p>
          </div>
        `;
      }
    }

    searchBtn.addEventListener('click', () => performBookSearch(searchInput.value.trim()));
    searchInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter') performBookSearch(searchInput.value.trim());
    });

    // ---- Student Only Features ----
    async function loadPersonalHistory() {
      const personalContainer = document.getElementById('personal-history-container');
      try {
        const loans = await LibraryService.getStudentLoanHistory(user.userId);
        renderLoansTable(loans, personalContainer);
      } catch (err) {
        personalContainer.innerHTML = `
          <div style="text-align: center; padding: 2rem; color: var(--danger);">
            <p>Failed to pull borrowing records: ${err.message}</p>
          </div>
        `;
      }
    }

    function renderLoansTable(loans, container) {
      if (loans.length === 0) {
        container.innerHTML = `
          <div style="text-align: center; padding: 3rem; color: var(--text-secondary);">
            <div style="font-size: 2.5rem; margin-bottom: 1rem;">📖</div>
            <p>No book loans registered under this Matric account.</p>
          </div>
        `;
        return;
      }

      let tableHtml = `
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Book Details</th>
              <th>Issued On</th>
              <th>Due Deadline</th>
              <th>Returned On</th>
              <th>Status</th>
              <th>Fine</th>
            </tr>
          </thead>
          <tbody>
      `;

      loans.forEach(loan => {
        const isOverdue = loan.status === 'OVERDUE';
        const isReturned = loan.status === 'RETURNED';
        const fineColor = loan.fineAmount > 0 ? 'var(--danger)' : 'var(--text-secondary)';
        
        tableHtml += `
          <tr>
            <td style="font-family: monospace; font-weight: 700; color: var(--secondary);">${loan.loanReference}</td>
            <td>
              <div style="font-weight: 600; color: white;">${loan.bookTitle || 'Unknown Title'}</div>
              <div style="font-size: 0.75rem; color: var(--text-secondary); font-family: monospace;">ISBN: ${loan.bookIsbn}</div>
            </td>
            <td>${loan.loanDate}</td>
            <td>${loan.dueDate}</td>
            <td>${loan.returnDate || '<span style="color: var(--text-secondary); font-style: italic;">Active Loan</span>'}</td>
            <td>
              <span class="badge ${isReturned ? 'badge-available' : (isOverdue ? 'badge-borrowed' : 'badge-student')}">
                ${loan.status}
              </span>
            </td>
            <td style="font-weight: 600; color: ${fineColor};">RM ${loan.fineAmount.toFixed(2)}</td>
          </tr>
        `;
      });

      tableHtml += `
          </tbody>
        </table>
      `;
      container.innerHTML = tableHtml;
    }

    // ---- Admin Only Features ----
    if (isAdmin) {
      // Operation: Add Book Form
      const addBookForm = document.getElementById('add-book-form');
      addBookForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const isbn = document.getElementById('add-book-isbn').value.trim();
        const title = document.getElementById('add-book-title').value.trim();
        const author = document.getElementById('add-book-author').value.trim();
        const category = document.getElementById('add-book-category').value.trim();

        try {
          const success = await LibraryService.addBook({ isbn, title, author, category });
          if (success) {
            showAlert(`Book "${title}" added successfully!`);
            addBookForm.reset();
            // Automatically switch back to Search Catalog and trigger fresh search
            document.getElementById('sub-search-btn').click();
            performBookSearch('');
          } else {
            showAlert('Failed to add book. Verify credentials or database records.', true);
          }
        } catch (err) {
          showAlert(err.message, true);
        }
      });

      // Operation: Issue Book Loan Form
      const issueForm = document.getElementById('issue-loan-form');
      // Set default due date to 7 days from today
      const today = new Date();
      today.setDate(today.getDate() + 7);
      const day = String(today.getDate()).padStart(2, '0');
      const month = String(today.getMonth() + 1).padStart(2, '0');
      const year = today.getFullYear();
      document.getElementById('issue-due-date').value = `${year}-${month}-${day}`;

      issueForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const studentId = document.getElementById('issue-student-id').value.trim();
        const studentName = document.getElementById('issue-student-name').value.trim();
        const isbn = document.getElementById('issue-book-isbn').value.trim();
        const dueDate = document.getElementById('issue-due-date').value;

        try {
          const loanRef = await LibraryService.borrowBook({ studentId, studentName, isbn, dueDate });
          showAlert(`Loan created successfully! Reference Code: ${loanRef}`);
          issueForm.reset();
          // Reset default date picker value
          document.getElementById('issue-due-date').value = `${year}-${month}-${day}`;
          
          // Switch to search and refresh
          document.getElementById('sub-search-btn').click();
          performBookSearch('');
        } catch (err) {
          showAlert(err.message, true);
        }
      });

      // Operation: Process Return Form
      const returnForm = document.getElementById('return-book-form');
      returnForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const loanRef = document.getElementById('return-loan-ref').value.trim().toUpperCase();

        try {
          const success = await LibraryService.returnBook(loanRef);
          if (success) {
            showAlert(`Book loan reference ${loanRef} successfully checked-in!`);
            returnForm.reset();
            // Switch to search and refresh
            document.getElementById('sub-search-btn').click();
            performBookSearch('');
          } else {
            showAlert('Could not return book. Please verify the Reference Code.', true);
          }
        } catch (err) {
          showAlert(err.message, true);
        }
      });

      // Operation: Auditing lookup logs
      const auditBookIsbnInput = document.getElementById('audit-book-isbn');
      const auditBookBtn = document.getElementById('audit-book-btn');
      
      const auditStudentIdInput = document.getElementById('audit-student-id');
      const auditStudentBtn = document.getElementById('audit-student-btn');

      const auditResultsTitle = document.getElementById('audit-results-title');
      const auditResultsContainer = document.getElementById('audit-results-container');

      async function runBookAudit() {
        const isbn = auditBookIsbnInput.value.trim();
        if (!isbn) {
          showAlert('Please provide an ISBN code.', true);
          return;
        }

        auditResultsContainer.innerHTML = '<div style="text-align:center; padding: 2rem;"><div class="spinner" style="width:25px; height:25px; margin:auto;"></div></div>';
        auditResultsTitle.textContent = `Audit Log Entries for ISBN: ${isbn}`;

        try {
          const loans = await LibraryService.getBookLoanHistory(isbn);
          renderAuditTable(loans, auditResultsContainer, `No borrow logs registered for ISBN "${isbn}".`);
        } catch (err) {
          auditResultsContainer.innerHTML = `<p style="color:var(--danger); padding:1rem; text-align:center;">Audit failed: ${err.message}</p>`;
        }
      }

      async function runStudentAudit() {
        const studentId = auditStudentIdInput.value.trim();
        if (!studentId) {
          showAlert('Please provide a Matric ID.', true);
          return;
        }

        auditResultsContainer.innerHTML = '<div style="text-align:center; padding: 2rem;"><div class="spinner" style="width:25px; height:25px; margin:auto;"></div></div>';
        auditResultsTitle.textContent = `Audit Log Entries for Student ID: ${studentId}`;

        try {
          const loans = await LibraryService.getStudentLoanHistory(studentId);
          renderAuditTable(loans, auditResultsContainer, `No borrow logs registered for Student Matric ID "${studentId}".`);
        } catch (err) {
          auditResultsContainer.innerHTML = `<p style="color:var(--danger); padding:1rem; text-align:center;">Audit failed: ${err.message}</p>`;
        }
      }

      function renderAuditTable(loans, container, emptyMessage) {
        if (loans.length === 0) {
          container.innerHTML = `<p style="text-align: center; color: var(--text-secondary); padding: 2rem 0;">${emptyMessage}</p>`;
          return;
        }

        let tableHtml = `
          <table>
            <thead>
              <tr>
                <th>Reference</th>
                <th>Student</th>
                <th>ISBN & Title</th>
                <th>Dates</th>
                <th>Status</th>
                <th>Fine</th>
              </tr>
            </thead>
            <tbody>
        `;

        loans.forEach(loan => {
          const isOverdue = loan.status === 'OVERDUE';
          const isReturned = loan.status === 'RETURNED';
          
          tableHtml += `
            <tr>
              <td style="font-family: monospace; font-weight: 700; color: var(--secondary);">${loan.loanReference}</td>
              <td>
                <div style="font-weight: 600; color: white;">${loan.studentName || 'Student Name'}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary); font-family: monospace;">Matric: ${loan.studentId}</div>
              </td>
              <td>
                <div style="font-weight: 600; color: white;">${loan.bookTitle || 'Unknown Book'}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary); font-family: monospace;">ISBN: ${loan.bookIsbn}</div>
              </td>
              <td style="font-size: 0.85rem; line-height: 1.4;">
                <div>📅 Issued: ${loan.loanDate}</div>
                <div>⏳ Due: ${loan.dueDate}</div>
                ${loan.returnDate ? `<div>✅ Returned: ${loan.returnDate}</div>` : `<div style="color: var(--secondary); font-style: italic;">Active</div>`}
              </td>
              <td>
                <span class="badge ${isReturned ? 'badge-available' : (isOverdue ? 'badge-borrowed' : 'badge-student')}">
                  ${loan.status}
                </span>
              </td>
              <td style="font-weight: 700; color: ${loan.fineAmount > 0 ? 'var(--danger)' : 'var(--text-secondary)'};">
                RM ${loan.fineAmount.toFixed(2)}
              </td>
            </tr>
          `;
        });

        tableHtml += `
            </tbody>
          </table>
        `;
        container.innerHTML = tableHtml;
      }

      auditBookBtn.addEventListener('click', runBookAudit);
      auditBookIsbnInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') runBookAudit();
      });

      auditStudentBtn.addEventListener('click', runStudentAudit);
      auditStudentIdInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') runStudentAudit();
      });
    }
  }
};
