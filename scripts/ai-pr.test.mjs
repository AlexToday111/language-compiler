import assert from 'node:assert/strict';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { START, END, mergeBody, eligible, diffContext, parseResponse, run } from './ai-pr.mjs';

const response = (value, status = 'completed') => ({
  status, output: [{ content: [{ type: 'output_text', text: JSON.stringify(value) }] }],
});
const generated = { title: 'Add AST printing', summary: '## Summary\nAdds an AST printer.' };
const pr = {
  number: 3, state: 'open', draft: false, labels: [], changed_files: 1,
  title: 'Work in progress', body: '## Testing\nNot run\n\n## AI usage\nNo',
  head: { sha: 'a'.repeat(40), repo: { full_name: 'owner/project' } },
};

test('replaces only the managed block and preserves manual sections exactly', () => {
  const prefix = 'Author notes\n';
  const suffix = '\n## Testing\nPassed locally\n## AI usage\nEditor assistance';
  const body = `${prefix}${START}\nold\n${END}${suffix}`;
  const updated = mergeBody(body, 'new');
  assert.equal(updated, `${prefix}${START}\nnew\n${END}${suffix}`);
  assert.equal(mergeBody(updated, 'new'), updated);
});

test('appends a block for existing PRs and handles empty descriptions', () => {
  assert.equal(mergeBody(null, 'new'), `${START}\nnew\n${END}`);
  assert.equal(mergeBody('Author notes', 'new'), `Author notes\n\n${START}\nnew\n${END}`);
});

test('rejects broken or duplicate markers without removing author text', () => {
  for (const body of [START, END, `${END}${START}`, `${START}${START}${END}`,
    `${START}${END}${END}`]) assert.throws(() => mergeBody(body, 'new'), /markers/);
});

test('skips forks, drafts, closed PRs and the opt-out label', () => {
  assert.equal(eligible(pr, 'owner/project'), true);
  assert.equal(eligible(pr, 'someone/fork'), false);
  assert.equal(eligible({ ...pr, draft: true }, 'owner/project'), false);
  assert.equal(eligible({ ...pr, state: 'closed' }, 'owner/project'), false);
  assert.equal(eligible({ ...pr, head: { repo: null } }, 'owner/project'), false);
  assert.equal(eligible({ ...pr, labels: [{ name: 'ai-pr-skip' }] }, 'owner/project'), false);
});

test('bounds diff input and flags unavailable or omitted patches', () => {
  assert.equal(diffContext([{ filename: 'a', patch: '+hello' }], 1).partial, false);
  assert.equal(diffContext([{ filename: 'binary' }], 1).partial, true);
  assert.equal(diffContext([], 101).partial, true);
  const large = diffContext([{ patch: '+'.repeat(70000) }], 1);
  assert.equal(large.partial, true);
  assert.equal(large.files.length, 60000);
});

test('validates structured output and refuses incomplete or unsafe responses', () => {
  assert.deepEqual(parseResponse(response(generated)), generated);
  for (const value of [null, {}, { ...generated, title: 'bad\ntitle' },
    { ...generated, title: 'a'.repeat(121) }, { ...generated, summary: START }]) {
    assert.throws(() => parseResponse(response(value)));
  }
  assert.throws(() => parseResponse(response(generated, 'incomplete')));
  assert.throws(() => parseResponse({ status: 'completed', output: [
    { content: [{ type: 'refusal' }] },
  ] }));
});

async function simulate(t, { latest = pr, ai = response(generated), fail = false } = {}) {
  const directory = await mkdtemp(join(tmpdir(), 'project-d-ai-pr-'));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const eventPath = join(directory, 'event.json');
  await writeFile(eventPath, JSON.stringify({ pull_request: { number: 3 } }));
  const calls = [];
  const replies = [pr, [{ filename: 'Ast.java', patch: '+class Ast {}' }], ai, latest, {}];
  const request = async (url, options) => {
    calls.push({ url, ...options });
    const value = replies.shift();
    if (fail && url.includes('api.openai.com')) return { ok: false, status: 429 };
    return { ok: true, json: async () => value };
  };
  const env = {
    GH_TOKEN: 'fake-github-token', OPENAI_API_KEY: 'fake-openai-key',
    GITHUB_REPOSITORY: 'owner/project', GITHUB_EVENT_PATH: eventPath,
  };
  return { calls, execute: () => run(env, request) };
}

test('generates and publishes using fresh author text and separate API credentials', async t => {
  const latest = { ...pr, body: 'New author notes\n' + pr.body };
  const { calls, execute } = await simulate(t, { latest });
  await execute();
  assert.equal(calls.length, 5);
  const payload = JSON.parse(calls.at(-1).body);
  assert.equal(calls.at(-1).method, 'PATCH');
  assert.equal(payload.title, generated.title);
  assert.ok(payload.body.startsWith(latest.body));
  assert.ok(payload.body.includes(pr.head.sha));
  assert.equal(calls[2].headers.Authorization, 'Bearer fake-openai-key');
  assert.equal(calls[0].headers.Authorization, 'Bearer fake-github-token');
  assert.equal(JSON.parse(calls[2].body).store, false);
});

test('does not publish stale results or overwrite a newly opted-out PR', async t => {
  for (const latest of [
    { ...pr, head: { ...pr.head, sha: 'b'.repeat(40) } },
    { ...pr, labels: [{ name: 'ai-pr-skip' }] },
  ]) {
    const { calls, execute } = await simulate(t, { latest });
    await execute();
    assert.equal(calls.some(call => call.method === 'PATCH'), false);
  }
});

test('API errors and malformed model output leave the PR unchanged', async t => {
  for (const options of [{ fail: true }, { ai: response({ title: 'incomplete' }) }]) {
    const { calls, execute } = await simulate(t, options);
    await assert.rejects(execute);
    assert.equal(calls.some(call => call.method === 'PATCH'), false);
  }
});

test('missing API key skips all network access', async () => {
  await run({}, () => assert.fail('Network must not be called'));
});
