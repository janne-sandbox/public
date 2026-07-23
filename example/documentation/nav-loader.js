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
            ['review-samples.html', 'Sample applications'],
            ['review-agent-typescript.html', 'TypeScript agent'],
            ['review-agent-java.html', 'Java agent'],
            ['review-agent-rust.html', 'Rust agent'],
            ['review-agent-react.html', 'React agent'],
            ['review-agent-cpp.html', 'C++ agent'],
            ['review-agent-csharp.html', 'C# agent']
        ]],
        ['AI DEVELOPMENT', [
            ['ai-development.html', 'Chapter overview'],
            ['ai-development-vscode.html', 'VS Code setup'],
            ['ai-development-vscode-settings.html', 'VS Code JSON settings'],
            ['ai-development-codex.html', 'VS Code with Codex'],
            ['ai-development-copilot.html', 'VS Code with Copilot'],
            ['ai-development-claude.html', 'VS Code with Claude'],
            ['ai-development-ollama.html', 'Local AI with Ollama'],
            ['ai-development-continue-vscode.html', 'Continue for VS Code'],
            ['ai-development-models-modes.html', 'Models and operating modes'],
            ['ai-development-continue.html', 'Continue across chats'],
            ['ai-development-mcp.html', 'What MCP means'],
            ['ai-development-temperature.html', 'What temperature means'],
            ['ai-development-workflow.html', 'Responsible workflow']
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
