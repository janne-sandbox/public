#!/usr/bin/env node
/**
 * React Review Agent
 * Evaluates React components against project requirements
 */

const path = require('path');
const fs = require('fs');
const ReviewAgent = require('./base-agent');

class ReactAgent extends ReviewAgent {
  constructor(internetEnabled = false, deepseekEnabled = false) {
    super('react', internetEnabled, deepseekEnabled);
  }

  /**
   * Run the React review
   */
  async run(projectRoot, requirementsDir, outputDir, timestamp) {
    console.log('  Loading requirements...');
    this.loadRequirements(requirementsDir);
    await this._filterRequirementsForLanguage();

    console.log('  Scanning React files...');
    const reactFiles = this.scanProjectFiles(projectRoot, /\.(tsx?|jsx?)$/)
      .filter(file => !this._isExcludedReactPath(file));
    console.log(`  Found ${reactFiles.length} React/TypeScript files`);

    console.log('  Evaluating against requirements...');
    this._evaluateReactCoverage(reactFiles);

    console.log('  Writing report...');
    const reportPath = this.writeReport(outputDir, timestamp);
    console.log(`  ✓ Report: ${path.basename(reportPath)}`);

    return { verdict: this.verdict, reportPath };
  }

  _isExcludedReactPath(filePath) {
    const normalized = filePath.replaceAll('\\', '/');
    return (
      normalized.includes('/review/') ||
      normalized.includes('/documentation/') ||
      normalized.includes('/coverage/') ||
      normalized.includes('/dist/') ||
      normalized.includes('/build/')
    );
  }

  async _filterRequirementsForLanguage() {
    const functional = this.requirementsByCategory.functional || [];
    const nonFunctional = this.requirementsByCategory.nonFunctional || [];

    if (this.deepseekAvailable) {
      this.requirementsByCategory.functional = await this.filterLanguageAppropriateRequirements(functional);
      this.requirementsByCategory.nonFunctional = await this.filterLanguageAppropriateRequirements(nonFunctional);
      console.log(`  Filtered requirements (Deepseek): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
      return;
    }

    this.requirementsByCategory.functional = functional.filter(req => this._isRequirementRelevantToReact(req.content));
    this.requirementsByCategory.nonFunctional = nonFunctional.filter(req => this._isRequirementRelevantToReact(req.content));
    console.log(`  Filtered requirements (rules): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
  }

  _isRequirementRelevantToReact(content) {
    const text = (content || '').toLowerCase();

    const reactSignals = [
      'react', 'jsx', 'tsx', 'component', 'hook', 'usestate', 'useeffect',
      'aria', 'wcag', 'alt text', 'dangerouslysetinnerhtml', 'vite'
    ];

    const nonReactSignals = [
      'java', 'spring', 'maven', 'jackson', 'jpa', 'preparedstatement',
      'rust', 'cargo', 'tokio', 'rumqttc', 'borrow checker',
      'mqtt payload', 'active mq artemis', 'hapi fhir backend'
    ];

    const hasReactSignal = reactSignals.some(signal => text.includes(signal));
    const hasNonReactSignal = nonReactSignals.some(signal => text.includes(signal));

    if (hasReactSignal) {
      return true;
    }

    return !hasNonReactSignal;
  }

  _evaluateReactCoverage(reactFiles) {
    // Check for accessibility
    this._checkAccessibility(reactFiles);

    // Check for security patterns
    this._checkSecurity(reactFiles);

    // Check for performance patterns
    this._checkPerformance(reactFiles);

    // Check for component organization
    this._checkComponentOrganization(reactFiles);

    // Check against functional requirements
    this._checkFunctionalRequirements(reactFiles);
  }

  _checkAccessibility(reactFiles) {
    let imagesWithoutAlt = 0;
    let ariaLabels = 0;

    for (const file of reactFiles) {
      const content = fs.readFileSync(file, 'utf-8');

      // Count concrete <img ...> tags that are missing an alt attribute.
      const imgTags = content.match(/<img\b[^>]*>/g) || [];
      for (const tag of imgTags) {
        if (!/\balt\s*=/.test(tag)) {
          imagesWithoutAlt++;
        }
      }

      ariaLabels += (content.match(/aria-label|aria-describedby|role=/g) || []).length;
    }

    if (imagesWithoutAlt > 0) {
      this.findings.push({
        severity: 'major',
        title: 'Missing alt text on images',
        file: 'Multiple React files',
        description: `Found ${imagesWithoutAlt} images without alt text. Required for WCAG 2.1 AA.`,
        requirement: 'WCAG-REQ'
      });
      this.changes.push({
        title: 'Add alt text to all images',
        file: 'React components',
        language: 'tsx',
        reason: 'Alt text is required for accessibility and SEO',
        codeSnippet: `// ✗ Bad
<img src="alarm.svg" />

// ✓ Good
<img src="alarm.svg" alt="Active alarm indicator" />`
      });
    }

    if (ariaLabels === 0 && reactFiles.length > 10) {
      this.findings.push({
        severity: 'major',
        title: 'No ARIA labels detected',
        file: 'React components',
        description: 'Interactive elements need aria-label or aria-describedby for screen readers',
        requirement: 'REQ-011'
      });
    }
  }

  _checkSecurity(reactFiles) {
    let dangerouslySetInnerHTML = 0;
    let tokenInStorage = 0;
    let passwordInLogs = 0;
    let htmlSanitization = 0;

    for (const file of reactFiles.slice(0, 15)) {
      const content = fs.readFileSync(file, 'utf-8');
      dangerouslySetInnerHTML += (content.match(/dangerouslySetInnerHTML/g) || []).length;
      tokenInStorage += (content.match(/localStorage\.setItem.*token|sessionStorage\.setItem.*token/g) || []).length;
      passwordInLogs += (content.match(/console\.(log|error).*password/g) || []).length;
      htmlSanitization += (content.match(/DOMPurify|sanitizeHtml|xss/g) || []).length;
    }

    if (dangerouslySetInnerHTML > 0 && htmlSanitization === 0) {
      this.findings.push({
        severity: 'critical',
        title: 'dangerouslySetInnerHTML without sanitization',
        file: 'Multiple React files',
        description: `Found ${dangerouslySetInnerHTML} uses without HTML sanitization. XSS vulnerability risk.`,
        requirement: 'OWASP-REQ'
      });
      this.changes.push({
        title: 'Sanitize HTML before rendering',
        file: 'React components',
        language: 'tsx',
        reason: 'Prevents XSS attacks by removing malicious scripts',
        codeSnippet: `import DOMPurify from 'dompurify';

// ✓ Safe
<div dangerouslySetInnerHTML={{
  __html: DOMPurify.sanitize(userContent)
}} />`
      });
    }

    if (tokenInStorage > 0) {
      this.findings.push({
        severity: 'major',
        title: 'JWT tokens stored in localStorage/sessionStorage',
        file: 'Multiple React files',
        description: 'Tokens must be stored in memory only, not in storage accessible to JavaScript',
        requirement: 'OWASP-REQ-002'
      });
      this.changes.push({
        title: 'Move tokens to memory-only storage',
        file: 'Auth context',
        language: 'tsx',
        reason: 'localStorage is vulnerable to XSS. Use a Ref or context.',
        codeSnippet: `// ✓ Good
const tokenRef = useRef<string | null>(null);
// or Context with in-memory state`
      });
    }
  }

  _checkPerformance(reactFiles) {
    let useMemoCount = 0;
    let useCallbackCount = 0;
    let useLazyCount = 0;
    let largeComponents = 0;

    for (const file of reactFiles.slice(0, 15)) {
      const content = fs.readFileSync(file, 'utf-8');
      useMemoCount += (content.match(/useMemo/g) || []).length;
      useCallbackCount += (content.match(/useCallback/g) || []).length;
      useLazyCount += (content.match(/lazy|Suspense/g) || []).length;
      if (content.length > 5000) largeComponents++;
    }

    if (largeComponents > 2) {
      this.findings.push({
        severity: 'minor',
        title: 'Large components detected',
        file: 'Multiple React files',
        description: `Found ${largeComponents} components > 5KB. Consider splitting into smaller components.`,
        requirement: 'REQ-010'
      });
    }

    if (useLazyCount === 0 && reactFiles.length > 10) {
      this.findings.push({
        severity: 'minor',
        title: 'No code splitting detected',
        file: 'React architecture',
        description: 'Consider using React.lazy() for route-based code splitting',
        requirement: 'REQ-005'
      });
    }
  }

  _checkComponentOrganization(reactFiles) {
    const componentFiles = reactFiles.filter(f => f.includes('component'));
    const hookFiles = reactFiles.filter(f => f.includes('hook'));

    if (componentFiles.length === 0 && reactFiles.length > 10) {
      this.findings.push({
        severity: 'minor',
        title: 'No clear component folder structure',
        file: 'Project structure',
        description: 'Consider organizing components into a dedicated folder',
        requirement: 'REQ-010'
      });
    }

    if (hookFiles.length === 0 && reactFiles.length > 15) {
      this.findings.push({
        severity: 'minor',
        title: 'No custom hooks detected',
        file: 'React architecture',
        description: 'Consider extracting custom hooks for reusable logic',
        requirement: 'REQ-004'
      });
    }
  }

  _checkFunctionalRequirements(reactFiles) {
    const functionalReqs = this.requirementsByCategory.functional || [];

    for (const req of functionalReqs.slice(0, 5)) {
      const result = this.evaluateAgainstRequirement(req.id, req.content, reactFiles);
      if (result.score === 0) {
        this.findings.push({
          severity: 'minor',
          title: `Potential gap in requirement coverage: ${req.id}`,
          file: req.file,
          description: `Requirement not clearly reflected in React code`,
          requirement: req.id
        });
      }
    }
  }
}

// ─── CLI ─────────────────────────────────────────────────────────────────────

async function main() {
  const args = process.argv.slice(2);
  const params = {};

  for (const arg of args) {
    const [key, value] = arg.split('=');
    params[key.replace('--', '')] = value;
  }

  const projectRoot = process.cwd();
  const requirementsDir = params['requirements-dir'] || './requirements';
  const outputDir = params['output-dir'] || './review/results';
  const timestamp = params['timestamp'] || new Date().toISOString().replace(/\D/g, '').slice(0, 12);
  const internet = params['internet'] === 'true';
  const deepseek = params['deepseek'] === 'true';

  const agent = new ReactAgent(internet, deepseek);
  const result = await agent.run(projectRoot, requirementsDir, outputDir, timestamp);
  console.log(`\n  Verdict: ${result.verdict}`);
}

if (require.main === module) {
  main().catch(err => {
    console.error('Agent error:', err);
    process.exit(1);
  });
}

module.exports = ReactAgent;
