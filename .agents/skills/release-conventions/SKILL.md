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

## 3. Branching Strategy

- **Single-Branch Workflow**: Development and releases are anchored on `main`.
- Automated releases trigger exclusively on pushes to `main`.
- **Never force-push** after a release tag has been created.
