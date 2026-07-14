#!/usr/bin/env node
/**
 * Rust Review Agent
 * Evaluates Rust code against project requirements
 */

const path = require('path');
const fs = require('fs');
const ReviewAgent = require('./base-agent');

class RustAgent extends ReviewAgent {
  constructor(internetEnabled = false, deepseekEnabled = false) {
    super('rust', internetEnabled, deepseekEnabled);
  }

  /**
   * Run the Rust review
   */
  async run(projectRoot, requirementsDir, outputDir, timestamp) {
    console.log('  Loading requirements...');
    this.loadRequirements(requirementsDir);
    await this._filterRequirementsForLanguage();

    console.log('  Scanning Rust files...');
    const rsFiles = this.scanProjectFiles(projectRoot, /\.rs$/);
    console.log(`  Found ${rsFiles.length} Rust files`);

    console.log('  Evaluating against requirements...');
    this._evaluateRustCoverage(rsFiles);

    console.log('  Writing report...');
    const reportPath = this.writeReport(outputDir, timestamp);
    console.log(`  ✓ Report: ${path.basename(reportPath)}`);

    return { verdict: this.verdict, reportPath };
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

    this.requirementsByCategory.functional = functional.filter(req => this._isRequirementRelevantToRust(req.content));
    this.requirementsByCategory.nonFunctional = nonFunctional.filter(req => this._isRequirementRelevantToRust(req.content));
    console.log(`  Filtered requirements (rules): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
  }

  _isRequirementRelevantToRust(content) {
    const text = (content || '').toLowerCase();

    const rustSignals = [
      'rust', 'cargo', 'tokio', 'rumqttc', 'serde', 'ownership',
      'borrow checker', 'result<', 'option<', 'match', 'mqtt client'
    ];

    const nonRustSignals = [
      'java', 'spring', 'maven', 'jackson', 'jpa',
      'typescript', 'tsconfig', 'zod', 'prisma',
      'react', 'jsx', 'tsx', 'aria-label', 'dangerouslysetinnerhtml'
    ];

    const hasRustSignal = rustSignals.some(signal => text.includes(signal));
    const hasNonRustSignal = nonRustSignals.some(signal => text.includes(signal));

    if (hasRustSignal) {
      return true;
    }

    return !hasNonRustSignal;
  }

  _evaluateRustCoverage(rsFiles) {
    // Check for memory safety patterns
    this._checkMemorySafety(rsFiles);

    // Check for error handling
    this._checkErrorHandling(rsFiles);

    // Check for async/await patterns
    this._checkAsyncPatterns(rsFiles);

    // Check for test coverage
    this._checkTestCoverage(rsFiles);

    // Check against functional requirements
    this._checkFunctionalRequirements(rsFiles);
  }

  _checkMemorySafety(rsFiles) {
    let unsafeBlocks = 0;
    let deriveCopy = 0;
    let ownershipPatterns = 0;

    for (const file of rsFiles.slice(0, 10)) {
      const content = fs.readFileSync(file, 'utf-8');
      unsafeBlocks += (content.match(/unsafe\s*{/g) || []).length;
      deriveCopy += (content.match(/#\[derive\([^)]*Copy/g) || []).length;
      ownershipPatterns += (content.match(/&\w+|mut\s+\w+|move\s*\|/g) || []).length;
    }

    if (unsafeBlocks > 2) {
      this.findings.push({
        severity: 'major',
        title: 'Multiple unsafe blocks detected',
        file: 'Multiple files',
        description: `Found ${unsafeBlocks} unsafe blocks. Minimize unsafe code and document necessity.`,
        requirement: 'Memory Safety'
      });
      this.changes.push({
        title: 'Reduce unsafe code',
        file: 'Rust files',
        language: 'rust',
        reason: 'Unsafe blocks bypass Rust\'s memory safety guarantees. Use only when necessary.',
        codeSnippet: `// Prefer safe abstractions
pub fn safe_function() -> Result<(), Error> {
  // Use Result for error handling
  let data = std::fs::read_to_string("file.txt")?;
  Ok(())
}

// Only when absolutely necessary and well-documented:
// unsafe { /* ... */ }`
      });
    }
  }

  _checkErrorHandling(rsFiles) {
    let resultUsage = 0;
    let optionUsage = 0;
    let panicUsage = 0;
    let unwrapUsage = 0;

    for (const file of rsFiles.slice(0, 10)) {
      const content = fs.readFileSync(file, 'utf-8');
      resultUsage += (content.match(/Result<|Ok\(|Err\(/g) || []).length;
      optionUsage += (content.match(/Option<|Some\(|None\b/g) || []).length;
      panicUsage += (content.match(/panic!|unreachable!/g) || []).length;
      unwrapUsage += (content.match(/\.unwrap\(|\.expect\(/g) || []).length;
    }

    if (unwrapUsage > resultUsage / 2) {
      this.findings.push({
        severity: 'major',
        title: 'Excessive unwrap usage',
        file: 'Multiple files',
        description: `Found ${unwrapUsage} unwrap() calls. Use ? operator or match for graceful error handling.`,
        requirement: 'REQ-038'
      });
    }

    if (panicUsage > 1) {
      this.findings.push({
        severity: 'major',
        title: 'Panic macros in production code',
        file: 'Multiple files',
        description: 'Panic should be avoided in production code. Use Result/Option for recoverable errors.',
        requirement: 'Error Handling'
      });
    }
  }

  _checkAsyncPatterns(rsFiles) {
    let asyncAwait = 0;
    let tokioUsage = 0;

    for (const file of rsFiles.slice(0, 10)) {
      const content = fs.readFileSync(file, 'utf-8');
      asyncAwait += (content.match(/async\s+(fn|{)|\s+await/g) || []).length;
      tokioUsage += (content.match(/tokio::task|tokio::spawn|#\[tokio::main\]/g) || []).length;
    }

    if (asyncAwait === 0 && rsFiles.some(f => f.includes('main.rs'))) {
      this.findings.push({
        severity: 'minor',
        title: 'No async/await patterns detected',
        file: 'Architecture',
        description: 'Consider using async/await for I/O-bound operations (MQTT, network)',
        requirement: 'REQ-051'
      });
    }
  }

  _checkTestCoverage(rsFiles) {
    const testModules = rsFiles.filter(f => f.includes('tests') || f.includes('_test.rs'));
    const coverage = testModules.length / Math.max(1, rsFiles.length);

    if (coverage < 0.2) {
      this.findings.push({
        severity: 'major',
        title: 'Low test coverage',
        file: 'Test suite',
        description: `Only ${(coverage * 100).toFixed(1)}% test coverage. Target ≥20%.`,
        requirement: 'Testing'
      });
    }
  }

  _checkFunctionalRequirements(rsFiles) {
    const functionalReqs = this.requirementsByCategory.functional || [];

    for (const req of functionalReqs.slice(0, 5)) {
      const result = this.evaluateAgainstRequirement(req.id, req.content, rsFiles);
      if (result.score === 0) {
        this.findings.push({
          severity: 'minor',
          title: `Potential gap in requirement coverage: ${req.id}`,
          file: req.file,
          description: `Requirement not clearly reflected in Rust code`,
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

  const agent = new RustAgent(internet, deepseek);
  const result = await agent.run(projectRoot, requirementsDir, outputDir, timestamp);
  console.log(`\n  Verdict: ${result.verdict}`);
}

if (require.main === module) {
  main().catch(err => {
    console.error('Agent error:', err);
    process.exit(1);
  });
}

module.exports = RustAgent;
