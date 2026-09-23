---
name: release-conventions
description: Semantic release and CI/CD lifecycle, conventional commit standards, release pipeline chain (release.yml and .releaserc), and git branching strategy.
---

# Semantic Release & CI/CD Lifecycle

## 1. Conventional Commits Standard

Automated semantic releases depend on commit messages adhering strictly to the conventional commits specification configured in `.releaserc`:

| Prefix | Semver Impact | Release Notes Section | Trigger Automated Release? |
| :--- | :--- | :--- | :--- |
| `feat:` | **Minor** (`x.Y.0`) | New Features | **Yes** |
| `fix:` | **Patch** (`x.x.Z`) | Bug Fixes | **Yes** |
| `perf:` | **Patch** (`x.x.Z`) | Improvements | **Yes** |
| `bump:` | **Patch** (`x.x.Z`) | Updated App Support | **Yes** |
| `build(Needs bump):` | **Patch** (`x.x.Z`) | Hidden | **Yes** |
| `refactor:` | None | Code Refactoring | **No** (Documented in notes) |
| `chore:` | None | Hidden | **No** (Skipped) |
| `docs:` | None | Hidden | **No** (Skipped) |

### Mandatory Atomic Commits Policy

Never squash or bundle unrelated changes across test harnesses, patch suites, cross-compatibility bridges, and documentation into a single monolithic commit. Every patch and task must produce discrete, atomic commits:

1. **Single Commit per Patch (New or Updated)** (`feat(<target>): ...` or `fix(<target>): ...`):
   Every new patch or patch update MUST be committed in a single, autonomous commit containing both the patch implementation (Kotlin/Smali/resources/ELF) AND its accompanying documentation entries (README.md, app-specific docs).
2. **Strict Prohibition of Documentation Batching & Multi-Patch Grouping**:
   - Never split a patch's code and documentation into separate commits during patch creation or updates.
   - Never bundle multiple distinct patches together into a single commit.
   - Under NO circumstances should documentation from multiple distinct patches be grouped or batched into a collective `docs:` commit.
3. **Tooling & Test Harness** (`test(harness): ...` or `refactor(harness): ...`):
   Isolate test runner, automation, and harness improvements from product patch logic.
4. **Cross-Compatibility & Shared Contracts** (`feat(patches): ...` or `feat(shared): ...`):
   Isolate shared compatibility bindings across apps when not part of an individual patch unit.
5. **Standalone Documentation** (`docs(<app>): ...` or `docs: ...`):
   Strictly reserved for documentation-only updates unrelated to patch creation or modification (e.g. typo fixes, architectural guides, standalone FAQ edits).

---

## 2. Release Pipeline Chain (`release.yml` & `.releaserc`)

When commits are pushed to `main`, the automated release pipeline executes:

1. **Commit Analysis**: Evaluates commit log since previous release tag (`@semantic-release/commit-analyzer`).
2. **Release Notes Generation**: Formats notes into sections based on conventional commit types (`@semantic-release/release-notes-generator`).
3. **Changelog & Bundle Generation**: `@MorpheApp/changelog` updates `CHANGELOG.md` and generates `patches-bundle.json`.
4. **Gradle Version Bump**: `gradle-semantic-release-plugin` syncs `gradle.properties`.
5. **Catalog & Readme Generation**: Executes prepare commands (`@semantic-release/exec`):
   - `./gradlew generatePatchesList`
   - Patches `patches-list.json` with next release version (`jq '.version="${nextRelease.version}"' patches-list.json`)
   - Generates updated README (`python3 .github/scripts/generate_patches_readme.py $GITHUB_REPOSITORY $GITHUB_REF_NAME patches-list.json README.md`)
6. **Git Commit**: `@semantic-release/git` commits updated release assets (`CHANGELOG.md`, `gradle.properties`, `patches-bundle.json`, `patches-list.json`, `README.md`) with message `chore: Release v${nextRelease.version} [skip ci]`.
7. **GitHub Release & Asset Publishing**: Attaches compiled standalone Morphe Patch Package (`patches/build/libs/patches-*.mpp`) to the GitHub Release (`@semantic-release/github`).
8. **Build Provenance Attestation**: Attests build provenance for `patches/build/libs/patches-*.mpp` via `actions/attest-build-provenance`.
9. **Telegram Broadcast**: Dispatches release tag, version, and notes via `python3 .github/scripts/notify_telegram.py`.
10. **Compilation Verification Fallback**: When semantic-release does not publish a new release (e.g. for `refactor`, `chore`, `docs`), CI executes `./gradlew :patches:buildAndroid clean --no-daemon` to ensure compilation integrity.

> **CRITICAL REMINDER**: Never manually edit or commit `patches-list.json`, `patches-bundle.json`, or `CHANGELOG.md`. These are generated, formatted, and committed strictly by CI during the automated release pipeline.

---

## 3. Branching Strategy & Direct Commit Workflow
 
 - **Single-Branch Workflow**: Development and releases are anchored on `main`.
 - **Strict Direct Workflow (No Pull Requests)**: This repository operates without PRs. Work is committed directly or managed locally without PRs. Under no circumstances should agents propose, generate, or ask about PR titles, descriptions, or PR workflows.
 - Automated releases trigger exclusively on pushes to `main`.
 - **Never force-push** after a release tag has been created.
