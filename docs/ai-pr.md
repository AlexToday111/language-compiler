# AI pull request descriptions

The PR template includes a managed summary, a testing checklist, and an AI usage
disclosure. Authors fill in **Testing** and **AI usage** themselves. The workflow
does not claim that AI wrote the code or that any tests passed.

## Setup

1. Merge the workflow, script, and template into the target branch (normally `main`).
   The automation executes the script from the PR's base commit.
2. In repository **Settings → Secrets and variables → Actions**, add the repository
   secret `OPENAI_API_KEY` with access to the OpenAI API.
3. Optionally add the repository variable `OPENAI_MODEL`. The default is
   `gpt-4.1-mini`; a replacement must support Responses API structured outputs.
4. Ensure repository/organization Actions policies allow this workflow,
   `pull_request_target`, and `pull-requests: write` for `GITHUB_TOKEN`.

No personal GitHub token or Node package installation is needed. Requests to the
OpenAI API use the configured API account and incur its usage charges. The title,
up to 100 changed file records, and up to 60,000 characters of filenames/patches
are sent to OpenAI with `store: false`. Missing or truncated patches are explicitly
flagged in the generated description.

## Behavior

- Runs on opening, reopening, new commits, and switching a draft to ready for review.
- Processes only non-draft PRs from branches in this repository. Fork PRs and
  Dependabot-triggered runs are skipped.
- Replaces the title and the content between `<!-- ai-pr:start -->` and
  `<!-- ai-pr:end -->`. Manual title edits are replaced on the next run.
- Preserves text outside the block, including testing results and AI disclosures.
  For older PRs without markers, appends a managed block to the existing body.
- Add the `ai-pr-skip` label to keep your title and summary under manual control.
  Removing the label takes effect on the next supported event, such as a new commit.
- Editing the title or body does not trigger generation, avoiding update loops.
- Without the API key, warns and leaves the PR unchanged. API errors, invalid output,
  or broken block markers fail the job without updating the PR.
- Rechecks the current PR before writing, preserving recent author edits outside the
  block and skipping stale results when the head commit changes or the PR is closed,
  converted to draft, or opted out. Avoid editing the body during the final update:
  GitHub's PR update endpoint does not provide an atomic compare-and-swap operation.

The privileged workflow checks out only the base commit, without persisted Git
credentials. It never executes PR head code. Patches are read through the GitHub
API and treated as untrusted input. Review generated text before merging.

## Local verification

With Node.js 20+:

```sh
node --test scripts/ai-pr.test.mjs
```

Tests simulate both APIs and make no external requests. They also run in Java CI.
Actual generation needs the repository secret and a new supported PR event after
the workflow reaches the target branch.

API references: [OpenAI structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses),
[GitHub PR events](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request_target),
and [GitHub PR API](https://docs.github.com/en/rest/pulls/pulls).
