# Contributing Guidelines

## How we work

Routine work happens on `dev`. The `main` branch is the stable release line touched only by explicit promotion merge, never casual pushes.

The repository operates under a strict No-PR policy: pull requests are not accepted, and work is committed directly by maintainers. In alignment with this policy, pull request templates (`.github/PULL_REQUEST_TEMPLATE.md`) and `.github/CODEOWNERS` are intentionally omitted.

All other branches are short-lived local task branches deleted after integration.

## Commit rules

- Use Conventional Commits formatting.
- Write commit titles in English in the imperative mood.
- Commit titles must be 72 characters or fewer (keep TikTok hook names short; move detailed explanations to the commit body).
- Each commit must encapsulate exactly one logical unit of work.
- Atomic commits: patch implementation and its documentation must be committed in the same atomic commit.

## Mandatory gate before declaring any patch done

Before declaring any patch done, run:

```bash
./gradlew runPatchTest -Papp=<target>
```

Criteria:
- 100% pass
- Zero fingerprint mismatches
- Zero smali compile errors

Quiet invocation:

```bash
./gradlew runPatchTest -Papp=<target> --console=plain > build/patchtest-<target>.log 2>&1
```

## Style

- Zero emojis in code, scripts, configs, logs, or commits.
- Named constants instead of magic literals.
- Consume `app.morphe.patches.shared.Constants`; never inline Compatibility blocks.

## Local hooks

Enable repository githooks locally:

```bash
git config core.hooksPath .githooks
```
