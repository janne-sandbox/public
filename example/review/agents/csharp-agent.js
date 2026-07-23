#!/usr/bin/env node
/**
 * C# Review Agent
 * Evaluates C# code with deterministic checks and optional Deepseek.
 */

const fs = require('fs');
const path = require('path');
const ReviewAgent = require('./base-agent');

class CsharpAgent extends ReviewAgent {
  constructor(internetEnabled = false, deepseekEnabled = false) {
    super('csharp', internetEnabled, deepseekEnabled);
  }

  async run(projectRoot, requirementsDir, outputDir, timestamp) {
    console.log('  Loading requirements...');
    this.loadRequirements(requirementsDir);
    await this.initializeDeepseek();
    await this._filterRequirementsForLanguage();

    console.log('  Scanning C# files...');
    const csharpFiles = this.scanProjectFiles(projectRoot, /\.cs$/i);
    const projectFiles = this.scanProjectFiles(projectRoot, /\.csproj$/i);
    console.log(`  Found ${csharpFiles.length} C# files`);

    console.log('  Evaluating against requirements...');
    await this._evaluateCsharpCoverage(csharpFiles, projectFiles);

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
      functional.filter(requirement => this._isRequirementRelevantToCsharp(requirement.content));
    this.requirementsByCategory.nonFunctional =
      nonFunctional.filter(requirement => this._isRequirementRelevantToCsharp(requirement.content));
    console.log(`  Filtered requirements (rules): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
  }

  _isRequirementRelevantToCsharp(content) {
    const text = (content || '').toLowerCase();
    const csharpSignals = [
      'c#', 'csharp', '.net', 'asp.net', 'nuget', 'entity framework',
      'async task', 'nullable', 'csproj'
    ];
    const otherLanguageSignals = [
      'java', 'spring', 'maven', 'typescript', 'tsconfig', 'react',
      'jsx', 'tsx', 'rust', 'cargo', 'tokio', 'c++', 'cmake', 'std::'
    ];

    if (csharpSignals.some(signal => text.includes(signal))) {
      return true;
    }
    return !otherLanguageSignals.some(signal => text.includes(signal));
  }

  async _evaluateCsharpCoverage(csharpFiles, projectFiles) {
    this._checkAsyncBlocking(csharpFiles);
    this._checkSqlConstruction(csharpFiles);
    this._checkNullableContext(csharpFiles, projectFiles);
    this._checkTestCoverage(csharpFiles);
    this._checkFunctionalRequirements(csharpFiles);
    await this.reviewCodeWithDeepseek(csharpFiles);
  }

  _checkAsyncBlocking(csharpFiles) {
    let asyncVoid = 0;
    let blockingWaits = 0;

    for (const file of csharpFiles) {
      const content = fs.readFileSync(file, 'utf-8');
      asyncVoid += (content.match(/\basync\s+void\s+\w+\s*\(/g) || []).length;
      blockingWaits += (content.match(/\.(?:Result|Wait\s*\(\s*\))/g) || []).length;
    }

    if (asyncVoid > 0) {
      this.findings.push({
        severity: 'major',
        title: 'async void methods detected',
        file: 'Multiple C# files',
        description: `Found ${asyncVoid} async void method(s). Except for event handlers, return Task so failures can be observed and tested.`,
        requirement: 'C# asynchronous correctness'
      });
    }

    if (blockingWaits > 0) {
      this.findings.push({
        severity: 'major',
        title: 'Synchronous blocking on asynchronous work',
        file: 'Multiple C# files',
        description: `Found ${blockingWaits} .Result or .Wait() operation(s). Use await to avoid deadlocks and thread-pool starvation.`,
        requirement: 'C# asynchronous correctness'
      });
    }
  }

  _checkSqlConstruction(csharpFiles) {
    let riskyQueries = 0;
    const riskySql = /(?:CommandText|FromSqlRaw|ExecuteSqlRaw)\s*(?:=|\()\s*[^;\n]*(?:\+|\$")/g;

    for (const file of csharpFiles) {
      riskyQueries += (fs.readFileSync(file, 'utf-8').match(riskySql) || []).length;
    }

    if (riskyQueries > 0) {
      this.findings.push({
        severity: 'critical',
        title: 'Potential SQL injection in constructed query',
        file: 'Multiple C# files',
        description: `Found ${riskyQueries} SQL operation(s) built through concatenation or interpolation. Use parameters or safe Entity Framework APIs.`,
        requirement: 'OWASP-REQ-001'
      });
    }
  }

  _checkNullableContext(csharpFiles, projectFiles) {
    if (csharpFiles.length === 0) return;
    const nullableEnabled = projectFiles.some(file =>
      /<Nullable>\s*enable\s*<\/Nullable>/i.test(fs.readFileSync(file, 'utf-8'))
    );

    if (!nullableEnabled) {
      this.findings.push({
        severity: 'major',
        title: 'Nullable reference types are not enabled',
        file: '*.csproj',
        description: 'Enable <Nullable>enable</Nullable> in the C# project to make nullability part of compile-time checking.',
        requirement: 'C# type safety'
      });
    }
  }

  _checkTestCoverage(csharpFiles) {
    if (csharpFiles.length === 0) return;
    const productionFiles = csharpFiles.filter(file => !/(?:Tests?|Specs?)[/\\]/i.test(file));
    const testFiles = csharpFiles.filter(file => /(?:Tests?|Specs?)[/\\]|(?:Tests?|Specs?)\.cs$/i.test(file));
    if (productionFiles.length > 0 && testFiles.length === 0) {
      this.findings.push({
        severity: 'major',
        title: 'No C# test files detected',
        file: 'Test suite',
        description: 'No C# test source or test/spec directory was found.',
        requirement: 'Testing'
      });
    }
  }

  _checkFunctionalRequirements(csharpFiles) {
    for (const requirement of (this.requirementsByCategory.functional || []).slice(0, 5)) {
      const result = this.evaluateAgainstRequirement(
        requirement.id,
        requirement.content,
        csharpFiles
      );
      if (result.score === 0) {
        this.findings.push({
          severity: 'minor',
          title: `Potential gap in requirement coverage: ${requirement.id}`,
          file: requirement.file,
          description: 'Requirement not clearly reflected in C# code',
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
  const agent = new CsharpAgent(
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

module.exports = CsharpAgent;
