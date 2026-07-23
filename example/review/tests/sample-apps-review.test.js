const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');

const CppAgent = require('../agents/cpp-agent');
const CsharpAgent = require('../agents/csharp-agent');
const JavaAgent = require('../agents/java-agent');
const ReactAgent = require('../agents/react-agent');
const RustAgent = require('../agents/rust-agent');
const TypeScriptAgent = require('../agents/typescript-agent');

const exampleRoot = path.resolve(__dirname, '../..');
const requirementsDir = path.join(exampleRoot, 'requirements');
const samplesRoot = path.join(exampleRoot, 'sample-apps');

const cases = [
  {
    name: 'C++ console',
    directory: 'cpp-console',
    Agent: CppAgent,
    expectedFinding: 'Potentially unsafe C string or input function'
  },
  {
    name: 'C# console',
    directory: 'csharp-console',
    Agent: CsharpAgent,
    expectedFinding: 'async void methods detected'
  },
  {
    name: 'Angular TypeScript',
    directory: 'angular-typescript',
    Agent: TypeScriptAgent,
    expectedFinding: 'TypeScript strict mode not enabled'
  },
  {
    name: 'browser JavaScript',
    directory: 'javascript-browser',
    Agent: ReactAgent,
    expectedFinding: 'JWT tokens stored in localStorage/sessionStorage'
  },
  {
    name: 'Java console',
    directory: 'java-console',
    Agent: JavaAgent,
    expectedFinding: 'Manual JSON construction detected'
  },
  {
    name: 'Rust CLI',
    directory: 'rust-cli',
    Agent: RustAgent,
    expectedFinding: 'Excessive unwrap usage'
  },
  {
    name: 'React UI',
    directory: 'react-ui',
    Agent: ReactAgent,
    expectedFinding: 'Missing alt text on images'
  }
];

test('every sample application produces its expected deterministic review finding', async t => {
  const outputDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sample-review-reports-'));

  try {
    for (const reviewCase of cases) {
      await t.test(reviewCase.name, async () => {
        const agent = new reviewCase.Agent(false, false);
        const result = await agent.run(
          path.join(samplesRoot, reviewCase.directory),
          requirementsDir,
          outputDir,
          '202607231800'
        );

        const findingTitles = agent.findings.map(finding => finding.title);
        assert.ok(
          findingTitles.includes(reviewCase.expectedFinding),
          `Expected "${reviewCase.expectedFinding}", got ${findingTitles.join(', ')}`
        );
        assert.ok(fs.existsSync(result.reportPath));

        const report = fs.readFileSync(result.reportPath, 'utf-8');
        assert.match(report, /\*\*Deepseek Analysis:\*\* Disabled/);
        assert.match(report, new RegExp(reviewCase.expectedFinding.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
      });
    }
  } finally {
    fs.rmSync(outputDir, { recursive: true, force: true });
  }
});
