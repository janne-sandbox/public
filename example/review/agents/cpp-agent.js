#!/usr/bin/env node
/**
 * C++ Review Agent
 * Evaluates C and C++ code with deterministic checks and optional Deepseek.
 */

const fs = require('fs');
const path = require('path');
const ReviewAgent = require('./base-agent');

class CppAgent extends ReviewAgent {
  constructor(internetEnabled = false, deepseekEnabled = false) {
    super('cpp', internetEnabled, deepseekEnabled);
  }

  async run(projectRoot, requirementsDir, outputDir, timestamp) {
    console.log('  Loading requirements...');
    this.loadRequirements(requirementsDir);
    await this.initializeDeepseek();
    await this._filterRequirementsForLanguage();

    console.log('  Scanning C/C++ files...');
    const cppFiles = this.scanProjectFiles(projectRoot, /\.(c|cc|cpp|cxx|h|hh|hpp|hxx)$/i);
    console.log(`  Found ${cppFiles.length} C/C++ files`);

    console.log('  Evaluating against requirements...');
    await this._evaluateCppCoverage(cppFiles);

    console.log('  Writing report...');
    const reportPath = this.writeReport(outputDir, timestamp);
    console.log(`  ✓ Report: ${path.basename(reportPath)}`);
    return { verdict: this.verdict, reportPath };
  }

  async _filterRequirementsForLanguage() {
    const functional = this.requirementsByCategory.functional || [];
    const nonFunctional = this.requirementsByCategory.nonFunctional || [];

    if (this.deepseekAvailable) {
      this.requirementsByCategory.functional =
        await this.filterLanguageAppropriateRequirements(functional);
      this.requirementsByCategory.nonFunctional =
        await this.filterLanguageAppropriateRequirements(nonFunctional);
      console.log(`  Filtered requirements (Deepseek): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
      return;
    }

    this.requirementsByCategory.functional =
      functional.filter(requirement => this._isRequirementRelevantToCpp(requirement.content));
    this.requirementsByCategory.nonFunctional =
      nonFunctional.filter(requirement => this._isRequirementRelevantToCpp(requirement.content));
    console.log(`  Filtered requirements (rules): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
  }

  _isRequirementRelevantToCpp(content) {
    const text = (content || '').toLowerCase();
    const cppSignals = [
      'c++', 'cpp', 'cmake', 'clang', 'gcc', 'std::', 'raii',
      'unique_ptr', 'shared_ptr', 'header file'
    ];
    const otherLanguageSignals = [
      'java', 'spring', 'maven', 'jackson', 'typescript', 'tsconfig',
      'react', 'jsx', 'tsx', 'rust', 'cargo', 'tokio',
      'c#', 'csharp', '.net', 'nuget'
    ];

    if (cppSignals.some(signal => text.includes(signal))) {
      return true;
    }
    return !otherLanguageSignals.some(signal => text.includes(signal));
  }

  async _evaluateCppCoverage(cppFiles) {
    this._checkUnsafeFunctions(cppFiles);
    this._checkResourceManagement(cppFiles);
    this._checkCStyleCasts(cppFiles);
    this._checkTestCoverage(cppFiles);
    this._checkFunctionalRequirements(cppFiles);
    await this.reviewCodeWithDeepseek(cppFiles);
  }

  _checkUnsafeFunctions(cppFiles) {
    const unsafeCalls = [];
    const unsafePattern = /\b(gets|strcpy|strcat|sprintf|scanf)\s*\(/g;

    for (const file of cppFiles) {
      const content = fs.readFileSync(file, 'utf-8');
      for (const match of content.matchAll(unsafePattern)) {
        unsafeCalls.push(match[1]);
      }
    }

    if (unsafeCalls.length > 0) {
      this.findings.push({
        severity: unsafeCalls.includes('gets') ? 'critical' : 'major',
        title: 'Potentially unsafe C string or input function',
        file: 'Multiple C/C++ files',
        description: `Found ${unsafeCalls.length} call(s) to ${[...new Set(unsafeCalls)].join(', ')}. Prefer bounded input, std::string, std::format, or size-aware alternatives.`,
        requirement: 'OWASP-REQ-001'
      });
    }
  }

  _checkResourceManagement(cppFiles) {
    let rawAllocations = 0;
    let smartPointers = 0;

    for (const file of cppFiles) {
      const content = fs.readFileSync(file, 'utf-8');
      rawAllocations += (content.match(/\bnew\s+(?!\()/g) || []).length;
      smartPointers += (content.match(/\b(?:unique_ptr|shared_ptr|make_unique|make_shared)\b/g) || []).length;
    }

    if (rawAllocations > 0 && smartPointers === 0) {
      this.findings.push({
        severity: 'major',
        title: 'Raw owning allocations without smart pointers',
        file: 'Multiple C++ files',
        description: `Found ${rawAllocations} raw allocation(s) and no smart-pointer ownership. Prefer RAII containers or std::unique_ptr/std::shared_ptr.`,
        requirement: 'C++ resource safety'
      });
    }
  }

  _checkCStyleCasts(cppFiles) {
    let casts = 0;
    const castPattern = /\((?:char|short|int|long|float|double|void)\s*\*?\)\s*[A-Za-z_(]/g;

    for (const file of cppFiles.slice(0, 20)) {
      casts += (fs.readFileSync(file, 'utf-8').match(castPattern) || []).length;
    }

    if (casts > 0) {
      this.findings.push({
        severity: 'minor',
        title: 'C-style casts detected',
        file: 'Multiple C++ files',
        description: `Found ${casts} C-style cast(s). Prefer explicit static_cast, const_cast, dynamic_cast, or reinterpret_cast operations.`,
        requirement: 'C++ type safety'
      });
    }
  }

  _checkTestCoverage(cppFiles) {
    if (cppFiles.length === 0) return;
    const sourceFiles = cppFiles.filter(file => /\.(c|cc|cpp|cxx)$/i.test(file));
    const testFiles = sourceFiles.filter(file => /(?:test|spec)[^/]*\.(?:c|cc|cpp|cxx)$/i.test(file));
    if (sourceFiles.length > 0 && testFiles.length === 0) {
      this.findings.push({
        severity: 'major',
        title: 'No C++ test files detected',
        file: 'Test suite',
        description: 'No C/C++ source file with test or spec in its name was found.',
        requirement: 'Testing'
      });
    }
  }

  _checkFunctionalRequirements(cppFiles) {
    for (const requirement of (this.requirementsByCategory.functional || []).slice(0, 5)) {
      const result = this.evaluateAgainstRequirement(
        requirement.id,
        requirement.content,
        cppFiles
      );
      if (result.score === 0) {
        this.findings.push({
          severity: 'minor',
          title: `Potential gap in requirement coverage: ${requirement.id}`,
          file: requirement.file,
          description: 'Requirement not clearly reflected in C/C++ code',
          requirement: requirement.id
        });
      }
    }
  }
}

async function main() {
  const params = Object.fromEntries(process.argv.slice(2).map(argument => {
    const [key, ...value] = argument.split('=');
    return [key.replace(/^--/, ''), value.join('=')];
  }));
  const agent = new CppAgent(
    params.internet === 'true',
    params.deepseek === 'true'
  );
  const result = await agent.run(
    process.cwd(),
    params['requirements-dir'] || './requirements',
    params['output-dir'] || './review/results',
    params.timestamp || new Date().toISOString().replace(/\D/g, '').slice(0, 12)
  );
  console.log(`\n  Verdict: ${result.verdict}`);
}

if (require.main === module) {
  main().catch(error => {
    console.error('Agent error:', error);
    process.exit(1);
  });
}

module.exports = CppAgent;
