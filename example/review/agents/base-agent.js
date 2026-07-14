#!/usr/bin/env node
/**
 * Base Agent Framework
 * Common utilities for all review agents
 * Hybrid approach: Regex + optional Deepseek LLM for semantic analysis
 */

const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

class ReviewAgent {
  constructor(language, internetEnabled = false, deepseekEnabled = false) {
    this.language = language;
    this.internetEnabled = internetEnabled;
    this.deepseekEnabled = deepseekEnabled;
    this.deepseekAvailable = false;
    this.requirementsByCategory = {};
    this.findings = [];
    this.verdict = 'APPROVED';
    this.changes = [];
    this.analysisMethod = 'regex';  // Track which method was used
    
    // Check if Deepseek is available
    if (deepseekEnabled) {
      this._checkDeepseekAvailability();
    }
  }

  /**
   * Check if Deepseek (via Ollama) is available
   */
  _checkDeepseekAvailability() {
    try {
      // Try to connect to Ollama on localhost:11434
      const http = require('http');
      const req = http.get('http://localhost:11434/api/tags', (res) => {
        this.deepseekAvailable = (res.statusCode === 200);
      });
      req.on('error', () => {
        this.deepseekAvailable = false;
      });
      req.setTimeout(1000);
    } catch (e) {
      this.deepseekAvailable = false;
    }
  }

  /**
   * Use Deepseek to filter language-appropriate requirements
   */
  async filterLanguageAppropriateRequirements(requirements) {
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
      const relevantIds = filtered.split('\n').filter(line => line.trim().match(/^REQ-/));
      return requirements.filter(r => relevantIds.includes(r.id));
    } catch (e) {
      console.warn('  Deepseek filtering failed, using all requirements');
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
      return { matched: [], missing: [], score: 0 };
    }
  }

  /**
   * Call Deepseek API via Ollama
   */
  async _callDeepseek(prompt) {
    return new Promise((resolve, reject) => {
      const postData = JSON.stringify({
        model: 'deepseek-coder:1.3b-base',
        prompt: prompt,
        stream: false
      });

      const options = {
        hostname: 'localhost',
        port: 11434,
        path: '/api/generate',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      };

      const http = require('http');
      const req = http.request(options, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => {
          try {
            const parsed = JSON.parse(data);
            resolve(parsed.response || '');
          } catch {
            reject(new Error('Invalid Deepseek response'));
          }
        });
      });

      req.on('error', reject);
      req.setTimeout(30000);  // 30 second timeout
      req.write(postData);
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
    md += `**Deepseek Analysis:** ${this.deepseekAvailable ? 'Enabled (Ollama)' : 'Disabled'}\n`;
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
