(function () {
    const sidebar = document.querySelector('[data-generated-nav]');
    if (!sidebar) return;

    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    const sections = [
        ['Review tool', [
            ['index.html', 'Documentation home'],
            ['review-overview.html', 'Review overview'],
            ['review-instructions.html', 'Using the tool'],
            ['review-architecture.html', 'Review architecture'],
            ['review-agent-typescript.html', 'TypeScript agent'],
            ['review-agent-java.html', 'Java agent'],
            ['review-agent-rust.html', 'Rust agent'],
            ['review-agent-react.html', 'React agent']
        ]],
        ['Development process', [
            ['process.html', 'Process overview'],
            ['process-pdca.html', 'PDCA delivery cycle'],
            ['process-governance.html', 'Governance and gates'],
            ['process-milestones.html', 'Milestones and operations']
        ]]
    ];

    const navSections = sections.map(([title, links]) => {
        const navLinks = links.map(([href, label]) => {
            const active = href === currentPage ? ' active' : '';
            const current = href === currentPage ? ' aria-current="page"' : '';
            return `<a class="nav-item${active}" href="${href}"${current}>${label}</a>`;
        }).join('');
        return `<div class="nav-section">
            <div class="nav-section-title">${title}</div>
            ${navLinks}
        </div>`;
    }).join('');

    sidebar.innerHTML = `
        <div class="logo-container">
            <img src="nologo-white.svg" alt="NoLogo">
            <h1>Example project documentation</h1>
        </div>
        <nav class="nav-menu" aria-label="Project documentation">
            ${navSections}
        </nav>`;
}());
