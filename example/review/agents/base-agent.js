#!/usr/bin/env node
/**
 * Base Agent Framework
 * Common utilities for all review agents
 * Hybrid approach: Regex + optional Deepseek LLM for semantic analysis
 */

const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');

class ReviewAgent {
  constructor(language, internetEnabled = false, deepseekEnabled = false) {
    this.language = language;
    this.internetEnabled = internetEnabled;
    this.deepseekEnabled = deepseekEnabled;
    this.deepseekAvailable = false;
    this.deepseekProvider = 'none';
    this.deepseekModel = null;
    this.deepseekStatus = deepseekEnabled ? 'Unavailable' : 'Disabled';
    this.requirementsByCategory = {};
    this.findings = [];
    this.verdict = 'APPROVED';
    this.changes = [];
    this.analysisMethod = 'regex';  // Track which method was used
  }

  /**
   * Select the Deepseek provider. Offline mode uses local Ollama only; online
   * mode uses the hosted Deepseek API only when an API key is configured.
   */
  async initializeDeepseek() {
    if (!this.deepseekEnabled) {
      this.deepseekAvailable = false;
      this.deepseekStatus = 'Disabled';
      return false;
    }

    if (this.internetEnabled) {
      if (!process.env.DEEPSEEK_API_KEY) {
        this.deepseekAvailable = false;
        this.deepseekStatus = 'Unavailable (missing DEEPSEEK_API_KEY)';
        console.warn('  Online Deepseek unavailable: DEEPSEEK_API_KEY is not set; using deterministic checks');
        return false;
      }

      this.deepseekProvider = 'online';
      this.deepseekModel = process.env.DEEPSEEK_ONLINE_MODEL || 'deepseek-v4-flash';
      this.deepseekAvailable = true;
      this.deepseekStatus = `Enabled (online API: ${this.deepseekModel})`;
      console.log(`  Deepseek provider: online API (${this.deepseekModel})`);
      return true;
    }

    this.deepseekProvider = 'offline';
    this.deepseekModel = process.env.DEEPSEEK_OLLAMA_MODEL || 'deepseek-coder:1.3b-base';

    try {
      const tags = await this._requestJson(http, {
        hostname: '127.0.0.1',
        port: 11434,
        path: '/api/tags',
        method: 'GET'
      }, null, 1500);
      const models = Array.isArray(tags.models) ? tags.models : [];
      const modelAvailable = models.some(model =>
        model.name === this.deepseekModel || model.model === this.deepseekModel
      );

      if (!modelAvailable) {
        this.deepseekAvailable = false;
        this.deepseekStatus = `Unavailable (Ollama model ${this.deepseekModel} not installed)`;
        console.warn(`  Offline Deepseek unavailable: run "ollama pull ${this.deepseekModel}"; using deterministic checks`);
        return false;
      }

      this.deepseekAvailable = true;
      this.deepseekStatus = `Enabled (offline Ollama: ${this.deepseekModel})`;
      console.log(`  Deepseek provider: offline Ollama (${this.deepseekModel})`);
      return true;
    } catch {
      this.deepseekAvailable = false;
      this.deepseekStatus = 'Unavailable (Ollama not reachable)';
      console.warn('  Offline Deepseek unavailable: Ollama is not reachable on 127.0.0.1:11434; using deterministic checks');
      return false;
    }
  }

  /**
   * Use Deepseek to filter language-appropriate requirements
   */
  async filterLanguageAppropriateRequirements(requirements) {
    if (requirements.length === 0) {
      return [];
    }
    if (!this.deepseekAvailable) {
      return requirements;  // Fallback: return all requirements
    }

    const prompt = `You are a code review expert. Given a requirement and a programming language, determine if the requirement is relevant to that language.

Language: ${this.language}

Requirements to filter:
${requirements.map((r, i) => `${i + 1}. ${r.id}: ${r.content.split('\n')[0]}`).join('\n')}

For each requirement, respond with ONLY the requirement ID if it IS relevant to ${this.language}.
Exclude requirements that are clearly only for other languages.

Example format:
REQ-001
REQ-003
REQ-005`;

    try {
      const filtered = await this._callDeepseek(prompt);
      const relevantIds = new Set(
        requirements
          .filter(requirement => filtered.includes(requirement.id))
          .map(requirement => requirement.id)
      );
      return requirements.filter(requirement => relevantIds.has(requirement.id));
    } catch (e) {
      console.warn('  Deepseek filtering failed, using all requirements');
      this._markDeepseekUnavailable('provider request failed');
      return requirements;
    }
  }

  /**
   * Use Deepseek for semantic requirement matching
   */
  async semanticRequirementMatch(requirementId, requirementContent, codeFiles) {
    if (!this.deepseekAvailable) {
      return { matched: [], missing: [], score: 0 };  // Fallback
    }

    const codeSnippet = codeFiles.slice(0, 3)
      .map(f => `\n--- ${f} ---\n${fs.readFileSync(f, 'utf-8').slice(0, 500)}`)
      .join('\n');

    const prompt = `You are a code review expert. Does this code address the requirement?

Requirement ID: ${requirementId}
Requirement:
${requirementContent.split('\n').slice(0, 10).join('\n')}

Code Sample:
${codeSnippet}

Respond with YES or NO, followed by brief explanation.`;

    try {
      const response = await this._callDeepseek(prompt);
      const matched = response.includes('YES');
      return { 
        matched: matched ? [requirementId] : [],
        missing: !matched ? [requirementId] : [],
        score: matched ? 1 : 0 
      };
    } catch (e) {
      this._markDeepseekUnavailable('provider request failed');
      return { matched: [], missing: [], score: 0 };
    }
  }

  /**
   * Ask Deepseek for additional semantic findings. Deterministic findings
   * remain available when the provider cannot be used.
   */
  async reviewCodeWithDeepseek(codeFiles) {
    if (!this.deepseekAvailable || codeFiles.length === 0) {
      return;
    }

    const codeSample = codeFiles.slice(0, 6)
      .map(file => `--- ${path.relative(process.cwd(), file)} ---\n${fs.readFileSync(file, 'utf-8').slice(0, 1600)}`)
      .join('\n\n');

    const prompt = `Review this ${this.language} code for correctness, security, resource management, and maintainability.
Return ONLY a JSON array. Each item must have exactly these string fields:
"severity" ("critical", "major", or "minor"), "title", "file", and "description".
Return [] when there are no findings. Do not use Markdown.

Code:
${codeSample}`;

    try {
      const response = await this._callDeepseek(prompt);
      this.findings.push(...this._parseDeepseekFindings(response));
    } catch (error) {
      console.warn(`  Deepseek code review failed (${error.message}); deterministic checks remain in effect`);
      this._markDeepseekUnavailable('code review request failed');
    }
  }

  _markDeepseekUnavailable(reason) {
    this.deepseekAvailable = false;
    this.deepseekStatus = `Unavailable (${reason})`;
  }

  _parseDeepseekFindings(response) {
    const text = String(response || '').trim()
      .replace(/^```(?:json)?\s*/i, '')
      .replace(/\s*```$/, '');
    const start = text.indexOf('[');
    const end = text.lastIndexOf(']');
    if (start < 0 || end < start) {
      throw new Error('Deepseek did not return a JSON array');
    }

    const parsed = JSON.parse(text.slice(start, end + 1));
    if (!Array.isArray(parsed)) {
      throw new Error('Deepseek findings were not an array');
    }

    const allowedSeverities = new Set(['critical', 'major', 'minor']);
    return parsed.slice(0, 10).flatMap(item => {
      if (!item || !allowedSeverities.has(item.severity)) {
        return [];
      }

      const title = this._cleanModelText(item.title, 160);
      const file = this._cleanModelText(item.file, 260);
      const description = this._cleanModelText(item.description, 1000);
      if (!title || !file || !description) {
        return [];
      }

      return [{
        severity: item.severity,
        title,
        file,
        description,
        requirement: 'Deepseek semantic review'
      }];
    });
  }

  _cleanModelText(value, maxLength) {
    return String(value || '')
      .replace(/[\r\n]+/g, ' ')
      .replace(/`/g, "'")
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .trim()
      .slice(0, maxLength);
  }

  /**
   * Call the hosted Deepseek API or local Ollama according to the selected
   * internet mode.
   */
  async _callDeepseek(prompt) {
    if (!this.deepseekAvailable) {
      throw new Error('Deepseek is unavailable');
    }

    if (this.deepseekProvider === 'online') {
      const postData = JSON.stringify({
        model: this.deepseekModel,
        messages: [{ role: 'user', content: prompt }],
        stream: false,
        max_tokens: 2048
      });
      const response = await this._requestJson(https, {
        hostname: 'api.deepseek.com',
        port: 443,
        path: '/chat/completions',
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${process.env.DEEPSEEK_API_KEY}`,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      }, postData, 60000);
      return response.choices?.[0]?.message?.content || '';
    }

    const postData = JSON.stringify({
      model: this.deepseekModel,
      prompt,
      stream: false
    });
    const response = await this._requestJson(http, {
      hostname: '127.0.0.1',
      port: 11434,
      path: '/api/generate',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    }, postData, 60000);
    return response.response || '';
  }

  _requestJson(transport, options, postData, timeoutMs) {
    return new Promise((resolve, reject) => {
      const req = transport.request(options, (res) => {
        let data = '';
        res.setEncoding('utf-8');
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          if (res.statusCode < 200 || res.statusCode >= 300) {
            reject(new Error(`Deepseek provider returned HTTP ${res.statusCode}`));
            return;
          }

          try {
            resolve(JSON.parse(data.trim()));
          } catch {
            reject(new Error('Deepseek provider returned invalid JSON'));
          }
        });
      });

      req.on('error', reject);
      req.setTimeout(timeoutMs, () => req.destroy(new Error('Deepseek provider timed out')));
      if (postData) {
        req.write(postData);
      }
      req.end();
    });
  }

  /**
   * Load all requirements from ./requirements/ folders
   */
  loadRequirements(requirementsDir) {
    const functionalDir = path.join(requirementsDir, 'functional');
    const nonFunctionalDir = path.join(requirementsDir, 'non-functional');

    this.requirementsByCategory.functional = this._readRequirementsFromDir(functionalDir);
    this.requirementsByCategory.nonFunctional = this._readRequirementsFromDir(nonFunctionalDir);

    return this.requirementsByCategory;
  }

  _readRequirementsFromDir(dirPath) {
    const requirements = [];
    if (!fs.existsSync(dirPath)) {
      console.warn(`Requirements directory not found: ${dirPath}`);
      return requirements;
    }

    const files = fs.readdirSync(dirPath).filter(f => f.endsWith('.md'));
    for (const file of files) {
      const content = fs.readFileSync(path.join(dirPath, file), 'utf-8');
      requirements.push({
        id: file.replace('.md', ''),
        file,
        content,
        rawLines: content.split('\n')
      });
    }
    return requirements;
  }

  /**
   * Extract requirement key phrases/checkpoints
   */
  extractRequirementCheckpoints(requirementContent) {
    const checkpoints = [];
    const lines = requirementContent.split('\n');
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      // Look for code blocks, lists, bold text as checkpoints
      if (line.startsWith('```') || line.startsWith('###') || line.startsWith('- ')) {
        checkpoints.push({
          line: i,
          text: line.trim(),
          context: lines.slice(Math.max(0, i - 2), Math.min(lines.length, i + 3))
        });
      }
    }
    return checkpoints;
  }

  /**
   * Scan project for language-specific files
   */
  scanProjectFiles(projectRoot, filePattern) {
    const matched = [];
    this._walkDir(projectRoot, file => {
      if (filePattern.test(file)) {
        matched.push(file);
      }
    });
    return matched;
  }

  _walkDir(dir, callback) {
    if (!fs.existsSync(dir)) return;
    const items = fs.readdirSync(dir);
    
    for (const item of items) {
      // Skip node_modules, .git, build outputs, etc.
      if (['node_modules', '.git', 'target', 'dist', 'build', '__pycache__'].includes(item)) {
        continue;
      }

      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      
      if (stat.isDirectory()) {
        this._walkDir(fullPath, callback);
      } else if (stat.isFile()) {
        callback(fullPath);
      }
    }
  }

  /**
   * Compare code against requirement checkpoints
   */
  evaluateAgainstRequirement(requirementId, requirementContent, codeFiles) {
    const checkpoints = this.extractRequirementCheckpoints(requirementContent);
    const matched = [];
    const missing = [];

    for (const checkpoint of checkpoints) {
      const found = codeFiles.some(file => {
        const content = fs.readFileSync(file, 'utf-8');
        return content.includes(checkpoint.text) || content.includes(checkpoint.text.replace('- ', ''));
      });

      if (found) {
        matched.push(checkpoint.text);
      } else {
        missing.push(checkpoint.text);
      }
    }

    return { requirementId, matched, missing, score: matched.length / Math.max(1, checkpoints.length) };
  }

  /**
   * Generate verdict based on findings
   */
  generateVerdict() {
    const totalFindings = this.findings.length;
    if (totalFindings === 0) {
      return 'APPROVED';
    }

    const criticalCount = this.findings.filter(f => f.severity === 'critical').length;
    const majorCount = this.findings.filter(f => f.severity === 'major').length;

    if (criticalCount > 0) {
      return 'DENIED';
    } else if (majorCount > 0 || this.changes.length > 0) {
      return 'APPROVED WITH CHANGES';
    } else {
      return 'APPROVED';
    }
  }

  /**
   * Write review result to markdown file
   */
  writeReport(outputDir, timestamp) {
    this.verdict = this.generateVerdict();
    const verdictSlug = this.verdict
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9\-]/g, '');

    const filename = `review-${this.language}-${verdictSlug}-${timestamp}.md`;
    const filepath = path.join(outputDir, filename);

    const report = this._buildReportMarkdown();
    fs.writeFileSync(filepath, report, 'utf-8');
    
    return filepath;
  }

  _buildReportMarkdown() {
    let md = `# Code Review Report — ${this.language.toUpperCase()}\n\n`;
    md += `**Timestamp:** ${new Date().toISOString()}\n`;
    md += `**Internet Access:** ${this.internetEnabled ? 'Enabled' : 'Disabled'}\n`;
    md += `**Deepseek Analysis:** ${this.deepseekStatus}\n`;
    md += `**Analysis Method:** ${this.deepseekAvailable ? 'Hybrid (Regex + Deepseek)' : 'Regex'}\n`;
    md += `**Verdict:** \`${this.verdict}\`\n\n`;

    md += `## Summary\n`;
    md += `- Total findings: ${this.findings.length}\n`;
    md += `- Critical issues: ${this.findings.filter(f => f.severity === 'critical').length}\n`;
    md += `- Major issues: ${this.findings.filter(f => f.severity === 'major').length}\n`;
    md += `- Minor issues: ${this.findings.filter(f => f.severity === 'minor').length}\n\n`;

    if (this.changes.length > 0) {
      md += `## Suggested Changes\n`;
      for (const change of this.changes) {
        md += `### ${change.title}\n`;
        md += `**File:** \`${change.file}\`\n`;
        md += `**Reason:** ${change.reason}\n\n`;
        if (change.codeSnippet) {
          md += `\`\`\`${change.language}\n${change.codeSnippet}\n\`\`\`\n\n`;
        }
      }
    }

    if (this.findings.length > 0) {
      md += `## Detailed Findings\n`;
      for (const finding of this.findings) {
        md += `### ${finding.title} (${finding.severity.toUpperCase()})\n`;
        md += `- **File:** \`${finding.file}\`\n`;
        md += `- **Line:** ${finding.line || 'N/A'}\n`;
        md += `- **Description:** ${finding.description}\n`;
        if (finding.requirement) {
          md += `- **Requirement:** ${finding.requirement}\n`;
        }
        md += '\n';
      }
    }

    md += `## Requirements Coverage\n`;
    for (const category in this.requirementsByCategory) {
      const reqs = this.requirementsByCategory[category];
      md += `### ${category}\n`;
      md += `- Total requirements: ${reqs.length}\n`;
      md += `- Reviewed: ${reqs.length > 0 ? 'Yes' : 'Not found'}\n\n`;
    }

    return md;
  }
}

module.exports = ReviewAgent;
