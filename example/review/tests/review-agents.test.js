const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const test = require('node:test');

const ReviewAgent = require('../agents/base-agent');
const CppAgent = require('../agents/cpp-agent');
const CsharpAgent = require('../agents/csharp-agent');

test('online mode selects the hosted Deepseek chat API', async () => {
  const previousKey = process.env.DEEPSEEK_API_KEY;
  process.env.DEEPSEEK_API_KEY = 'test-key';

  try {
    const agent = new ReviewAgent('cpp', true, true);
    assert.equal(await agent.initializeDeepseek(), true);
    assert.equal(agent.deepseekProvider, 'online');
    assert.equal(agent.deepseekModel, 'deepseek-v4-flash');

    let captured;
    agent._requestJson = async (transport, options, postData) => {
      captured = { transport, options, body: JSON.parse(postData) };
      return { choices: [{ message: { content: '[]' } }] };
    };

    assert.equal(await agent._callDeepseek('review me'), '[]');
    assert.equal(captured.options.hostname, 'api.deepseek.com');
    assert.equal(captured.options.path, '/chat/completions');
    assert.equal(captured.body.model, 'deepseek-v4-flash');
    assert.deepEqual(captured.body.messages, [{ role: 'user', content: 'review me' }]);
  } finally {
    if (previousKey === undefined) {
      delete process.env.DEEPSEEK_API_KEY;
    } else {
      process.env.DEEPSEEK_API_KEY = previousKey;
    }
  }
});

test('online mode falls back when no API key is configured', async () => {
  const previousKey = process.env.DEEPSEEK_API_KEY;
  delete process.env.DEEPSEEK_API_KEY;

  try {
    const agent = new ReviewAgent('csharp', true, true);
    assert.equal(await agent.initializeDeepseek(), false);
    assert.equal(agent.deepseekAvailable, false);
    assert.match(agent.deepseekStatus, /missing DEEPSEEK_API_KEY/);
  } finally {
    if (previousKey !== undefined) {
      process.env.DEEPSEEK_API_KEY = previousKey;
    }
  }
});

test('offline mode selects an installed Ollama model', async () => {
  const agent = new ReviewAgent('cpp', false, true);
  agent._requestJson = async () => ({
    models: [{ name: 'deepseek-coder:1.3b-base' }]
  });

  assert.equal(await agent.initializeDeepseek(), true);
  assert.equal(agent.deepseekProvider, 'offline');
  assert.equal(agent.deepseekModel, 'deepseek-coder:1.3b-base');
  assert.match(agent.deepseekStatus, /offline Ollama/);
});

test('Deepseek findings are validated and normalized before reporting', () => {
  const agent = new ReviewAgent('cpp', false, false);
  const findings = agent._parseDeepseekFindings(`\`\`\`json
[
  {
    "severity": "major",
    "title": "Unchecked\\nlength",
    "file": "src/parser.cpp",
    "description": "Validate the input length."
  },
  {
    "severity": "unknown",
    "title": "Ignored",
    "file": "ignored.cpp",
    "description": "Invalid severity"
  }
]
\`\`\``);

  assert.equal(findings.length, 1);
  assert.equal(findings[0].title, 'Unchecked length');
  assert.equal(findings[0].requirement, 'Deepseek semantic review');
});

test('C++ agent detects unsafe strings, raw ownership, casts, and missing tests', async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'review-cpp-'));
  const source = path.join(directory, 'unsafe.cpp');
  fs.writeFileSync(source, `
char *copy(char *input) {
  char *result = new char[32];
  strcpy(result, input);
  return (char *) result;
}
`);

  try {
    const agent = new CppAgent(false, false);
    agent.requirementsByCategory = { functional: [], nonFunctional: [] };
    await agent._evaluateCppCoverage([source]);
    const titles = agent.findings.map(finding => finding.title);
    assert.ok(titles.includes('Potentially unsafe C string or input function'));
    assert.ok(titles.includes('Raw owning allocations without smart pointers'));
    assert.ok(titles.includes('C-style casts detected'));
    assert.ok(titles.includes('No C++ test files detected'));
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
  }
});

test('C# agent detects blocking async use, unsafe SQL, nullable gaps, and missing tests', async () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'review-csharp-'));
  const source = path.join(directory, 'Repository.cs');
  const project = path.join(directory, 'Sample.csproj');
  fs.writeFileSync(source, `
public class Repository {
  public async void Load() {
    var value = GetValueAsync().Result;
    command.CommandText = $"SELECT * FROM users WHERE name = '{value}'";
  }
}
`);
  fs.writeFileSync(project, '<Project Sdk="Microsoft.NET.Sdk"></Project>');

  try {
    const agent = new CsharpAgent(false, false);
    agent.requirementsByCategory = { functional: [], nonFunctional: [] };
    await agent._evaluateCsharpCoverage([source], [project]);
    const titles = agent.findings.map(finding => finding.title);
    assert.ok(titles.includes('async void methods detected'));
    assert.ok(titles.includes('Synchronous blocking on asynchronous work'));
    assert.ok(titles.includes('Potential SQL injection in constructed query'));
    assert.ok(titles.includes('Nullable reference types are not enabled'));
    assert.ok(titles.includes('No C# test files detected'));
  } finally {
    fs.rmSync(directory, { recursive: true, force: true });
  }
});
