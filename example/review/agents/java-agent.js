#!/usr/bin/env node
/**
 * Java Review Agent
 * Evaluates Java code against project requirements
 */

const path = require('path');
const fs = require('fs');
const ReviewAgent = require('./base-agent');

class JavaAgent extends ReviewAgent {
  constructor(internetEnabled = false, deepseekEnabled = false) {
    super('java', internetEnabled, deepseekEnabled);
  }

  /**
   * Run the Java review
   */
  async run(projectRoot, requirementsDir, outputDir, timestamp) {
    console.log('  Loading requirements...');
    this.loadRequirements(requirementsDir);
    await this.initializeDeepseek();
    await this._filterRequirementsForLanguage();

    console.log('  Scanning Java files...');
    const javaFiles = this.scanProjectFiles(projectRoot, /\.java$/);
    console.log(`  Found ${javaFiles.length} Java files`);

    console.log('  Evaluating against requirements...');
    this._evaluateJavaCoverage(javaFiles);

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

    // Deterministic fallback when Deepseek is unavailable.
    this.requirementsByCategory.functional = functional.filter(req => this._isRequirementRelevantToJava(req.content));
    this.requirementsByCategory.nonFunctional = nonFunctional.filter(req => this._isRequirementRelevantToJava(req.content));
    console.log(`  Filtered requirements (rules): functional ${functional.length} -> ${this.requirementsByCategory.functional.length}, non-functional ${nonFunctional.length} -> ${this.requirementsByCategory.nonFunctional.length}`);
  }

  _isRequirementRelevantToJava(content) {
    const text = (content || '').toLowerCase();

    const javaSignals = [
      'java', 'spring', 'maven', 'jackson', 'jpa', 'hibernate',
      '@restcontroller', 'preparedstatement', 'record class', 'java 21'
    ];

    const nonJavaSignals = [
      'react', 'jsx', 'tsx', 'vite', 'useeffect', 'usestate',
      'typescript', 'tsconfig', 'zod', 'prisma',
      'rust', 'cargo', 'tokio', 'rumqttc', 'borrow checker'
    ];

    const hasJavaSignal = javaSignals.some(signal => text.includes(signal));
    const hasNonJavaSignal = nonJavaSignals.some(signal => text.includes(signal));

    if (hasJavaSignal) {
      return true;
    }

    // Keep general requirements unless they are clearly language-specific to non-Java stacks.
    return !hasNonJavaSignal;
  }

  _evaluateJavaCoverage(javaFiles) {
    // Check for Spring Boot 3.2 / Java 21 patterns
    this._checkModernJavaPatterns(javaFiles);

    // Check for Jackson usage
    this._checkJacksonUsage(javaFiles);

    // Check for OWASP compliance
    this._checkOWASPCompliance(javaFiles);

    // Check for test coverage
    this._checkTestCoverage(javaFiles);

    // Check against functional requirements
    this._checkFunctionalRequirements(javaFiles);
  }

  _checkModernJavaPatterns(javaFiles) {
    let records = 0;
    let textBlocks = 0;
    let switchExpressions = 0;

    for (const file of javaFiles.slice(0, 15)) {
      const content = fs.readFileSync(file, 'utf-8');
      records += (content.match(/\brecord\s+\w+/g) || []).length;
      textBlocks += (content.match(/"""/g) || []).length / 2;
      switchExpressions += (content.match(/switch\s*\([^)]+\)\s*{/g) || []).length;
    }

    if (records === 0 && javaFiles.length > 5) {
      this.findings.push({
        severity: 'minor',
        title: 'Java 21 records not used',
        file: 'Architecture',
        description: 'Java 21 records are immutable value types. Consider using them for data classes instead of @Data.',
        requirement: 'REQ-033'
      });
    }

    if (textBlocks === 0 && javaFiles.length > 10) {
      this.findings.push({
        severity: 'minor',
        title: 'Java text blocks not used for multi-line strings',
        file: 'Code Style',
        description: 'Text blocks (""") improve readability of multi-line strings',
        requirement: 'Code Style'
      });
    }
  }

  _checkJacksonUsage(javaFiles) {
    let jacksonAnnotations = 0;
    let manualJsonBuilding = 0;

    for (const file of javaFiles.slice(0, 10)) {
      const content = fs.readFileSync(file, 'utf-8');
      jacksonAnnotations += (content.match(/@(JsonProperty|JsonSerialize|JsonDeserialize)/g) || []).length;
      manualJsonBuilding += (content.match(/JSONObject|JSONArray|org\.json/g) || []).length;
    }

    if (manualJsonBuilding > jacksonAnnotations) {
      this.findings.push({
        severity: 'major',
        title: 'Manual JSON construction detected',
        file: 'Multiple files',
        description: 'Use Jackson for JSON serialization instead of manual JSONObject construction',
        requirement: 'CLAUDE.md — No string interpolation in JSON'
      });
      this.changes.push({
        title: 'Use Jackson for JSON serialization',
        file: 'Java files',
        language: 'java',
        reason: 'Jackson provides type-safe JSON handling via annotations',
        codeSnippet: `@RestController
@RequestMapping("/api")
public class AuditEventController {
  @PostMapping("/events")
  public ResponseEntity<?> createEvent(@RequestBody @Valid AuditEventDto event) {
    // Jackson automatically serializes/deserializes via annotations
    return ResponseEntity.ok(event);
  }
}`
      });
    }
  }

  _checkOWASPCompliance(javaFiles) {
    let sqlInjectionRisks = 0;
    let parameterizedQueries = 0;
    let queryConstruction = 0;

    const riskyQueryCall = /(executeQuery|executeUpdate|createNativeQuery|createQuery)\s*\([^)]*\+/;
    const riskyAnnotationQuery = /@Query\s*\([^)]*\+/;
    const queryCreation = /(executeQuery|executeUpdate|createNativeQuery|createQuery|@Query\s*\()/;

    for (const file of javaFiles.slice(0, 10)) {
      const content = fs.readFileSync(file, 'utf-8');
      const lines = content.split('\n');

      for (const line of lines) {
        if (queryCreation.test(line)) {
          queryConstruction++;
        }
        if (riskyQueryCall.test(line) || riskyAnnotationQuery.test(line)) {
          sqlInjectionRisks++;
        }
      }

      parameterizedQueries += (content.match(/PreparedStatement|setParameter\(|@Query\([^)]*:|@Param|\?\d*/g) || []).length;
    }

    if (sqlInjectionRisks > 0) {
      this.findings.push({
        severity: 'critical',
        title: 'Potential SQL injection vulnerability',
        file: 'Multiple files',
        description: 'String concatenation in SQL queries detected. Use parameterized queries.',
        requirement: 'OWASP-REQ-001 (Injection Prevention)'
      });
    }

    if (queryConstruction > 0 && parameterizedQueries === 0) {
      this.findings.push({
        severity: 'major',
        title: 'No parameterized queries detected',
        file: 'Database layer',
        description: 'All database queries must use parameterized statements or JPA with named parameters',
        requirement: 'OWASP-REQ-001'
      });
    }
  }

  _checkTestCoverage(javaFiles) {
    const testFiles = javaFiles.filter(f => f.includes('Test.java'));
    const coverage = testFiles.length / Math.max(1, javaFiles.filter(f => !f.includes('Test.java')).length);

    if (coverage < 0.3) {
      this.findings.push({
        severity: 'minor',
        title: 'Low test coverage',
        file: 'Test suite',
        description: `Only ${(coverage * 100).toFixed(1)}% of non-test files have test counterparts. Target ≥30%.`,
        requirement: 'REQ-058'
      });
    }
  }

  _checkFunctionalRequirements(javaFiles) {
    const functionalReqs = this.requirementsByCategory.functional || [];
    let matchedCount = 0;

    for (const req of functionalReqs.slice(0, 5)) {
      const result = this.evaluateAgainstRequirement(req.id, req.content, javaFiles);
      if (result.score > 0.5) {
        matchedCount++;
      } else if (result.score === 0) {
        this.findings.push({
          severity: 'minor',
          title: `Potential gap in requirement coverage: ${req.id}`,
          file: req.file,
          description: `Requirement not clearly reflected in Java code`,
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

  const agent = new JavaAgent(internet, deepseek);
  const result = await agent.run(projectRoot, requirementsDir, outputDir, timestamp);
  console.log(`\n  Verdict: ${result.verdict}`);
}

if (require.main === module) {
  main().catch(err => {
    console.error('Agent error:', err);
    process.exit(1);
  });
}

module.exports = JavaAgent;
