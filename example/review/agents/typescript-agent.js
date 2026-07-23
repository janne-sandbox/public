#!/usr/bin/env node
/**
 * TypeScript Review Agent
 * Evaluates TypeScript code against project requirements
 */

const path = require('path');
const fs = require('fs');
const ReviewAgent = require('./base-agent');

class TypeScriptAgent extends ReviewAgent {
  constructor(internetEnabled = false, deepseekEnabled = false) {
    super('typescript', internetEnabled, deepseekEnabled);
  }

  /**
   * Run the TypeScript review
   */
  async run(projectRoot, requirementsDir, outputDir, timestamp) {
    console.log('  Loading requirements...');
    this.loadRequirements(requirementsDir);
    await this.initializeDeepseek();
    await this._filterRequirementsForLanguage();

    console.log('  Scanning TypeScript files...');
    const tsFiles = this.scanProjectFiles(projectRoot, /\.(ts|tsx)$/);
    console.log(`  Found ${tsFiles.length} TypeScript files`);

    console.log('  Evaluating against requirements...');
    this._evaluateTypeScriptCoverage(tsFiles, projectRoot);

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

    this.requirementsByCategory.functional = functional.filter(req => this._isRequirementRelevantToTypeScript(req.content));
    this.requirementsByCategory.nonFunctional = nonFunctional.filter(req => this._isRequirementRelevantToTypeScript(req.content));
    console.log(`  Filtered requirements (rules): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
  }

  _isRequirementRelevantToTypeScript(content) {
    const text = (content || '').toLowerCase();

    const tsSignals = [
      'typescript', 'tsconfig', 'zod', 'prisma', 'express', 'node.js',
      'npm', 'vitest', 'strict mode', 'noimplicitany'
    ];

    const nonTsSignals = [
      'java', 'spring', 'maven', 'jackson', 'jpa',
      'rust', 'cargo', 'tokio', 'rumqttc', 'borrow checker',
      'react accessibility', 'aria-label', 'dangerouslysetinnerhtml'
    ];

    const hasTsSignal = tsSignals.some(signal => text.includes(signal));
    const hasNonTsSignal = nonTsSignals.some(signal => text.includes(signal));

    if (hasTsSignal) {
      return true;
    }

    return !hasNonTsSignal;
  }

  _evaluateTypeScriptCoverage(tsFiles, projectRoot) {
    // Check for strict TypeScript mode
    this._checkStrictMode(projectRoot);

    // Check for type safety patterns
    this._checkTypeSafety(tsFiles);

    // Check for test coverage
    this._checkTestCoverage(tsFiles);

    // Check against functional requirements
    this._checkFunctionalRequirements(tsFiles);
  }

  _checkStrictMode(projectRoot) {
    const tsconfigFiles = this.scanProjectFiles(projectRoot, /tsconfig\.json$/);

    for (const file of tsconfigFiles) {
      try {
        const content = JSON.parse(fs.readFileSync(file, 'utf-8'));
        if (!content.compilerOptions?.strict) {
          this.findings.push({
            severity: 'major',
            title: 'TypeScript strict mode not enabled',
            file,
            description: 'Strict mode should be enabled for compile-time safety',
            requirement: 'REQ-006 TypeScript Compile-Time Safety'
          });
          this.changes.push({
            title: 'Enable TypeScript strict mode',
            file,
            language: 'json',
            reason: 'Strict mode prevents many classes of runtime errors at compile time',
            codeSnippet: `"compilerOptions": {\n  "strict": true,\n  "noUncheckedIndexedAccess": true\n}`
          });
        }
      } catch (e) {
        // Ignore parse errors
      }
    }
  }

  _checkTypeSafety(tsFiles) {
    let anyTypeCounts = 0;
    let discriminatedUnions = 0;

    for (const file of tsFiles.slice(0, 10)) { // Check first 10 for performance
      const content = fs.readFileSync(file, 'utf-8');
      anyTypeCounts += (content.match(/:\s*any\b/g) || []).length;
      discriminatedUnions += (content.match(/type\s+\w+\s*=\s*\|/g) || []).length;
    }

    if (anyTypeCounts > 5) {
      this.findings.push({
        severity: 'minor',
        title: 'Multiple uses of `any` type',
        file: 'Multiple files',
        description: `Found ${anyTypeCounts} instances of untyped 'any'. Use explicit types or provide justifying comments.`,
        requirement: 'REQ-006'
      });
    }

    if (discriminatedUnions === 0 && tsFiles.length > 5) {
      this.findings.push({
        severity: 'minor',
        title: 'No discriminated unions detected',
        file: 'Architecture',
        description: 'Consider using discriminated unions for type-safe state modeling',
        requirement: 'REQ-006'
      });
    }
  }

  _checkTestCoverage(tsFiles) {
    const testFiles = tsFiles.filter(f => f.includes('.test.') || f.includes('.spec.'));
    const coverage = testFiles.length / Math.max(1, tsFiles.length);

    if (coverage < 0.3) {
      this.findings.push({
        severity: 'minor',
        title: 'Insufficient test file coverage',
        file: 'Test suite',
        description: `Only ${(coverage * 100).toFixed(1)}% of files have test counterparts. Target ≥30%.`,
        requirement: 'REQ-012'
      });
    }
  }

  _checkFunctionalRequirements(tsFiles) {
    const functionalReqs = this.requirementsByCategory.functional || [];
    let matchedCount = 0;

    for (const req of functionalReqs.slice(0, 5)) { // Sample first 5
      const result = this.evaluateAgainstRequirement(req.id, req.content, tsFiles);
      if (result.score > 0.5) {
        matchedCount++;
      } else if (result.score === 0) {
        this.findings.push({
          severity: 'minor',
          title: `Potential gap in requirement coverage: ${req.id}`,
          file: req.file,
          description: `Requirement not clearly reflected in TypeScript code`,
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

  const agent = new TypeScriptAgent(internet, deepseek);
  const result = await agent.run(projectRoot, requirementsDir, outputDir, timestamp);
  console.log(`\n  Verdict: ${result.verdict}`);
}

if (require.main === module) {
  main().catch(err => {
    console.error('Agent error:', err);
    process.exit(1);
  });
}

module.exports = TypeScriptAgent;
