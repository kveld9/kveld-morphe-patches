# Semantic Release & CI/CD Lifecycle

## 1. Conventional Commits Standard

Automated semantic releases depend on commit messages adhering strictly to the conventional commits specification:

| Prefix | Semver Impact | Release Notes Section | Trigger Automated Release? |
| :--- | :--- | :--- | :--- |
| `feat:` | **Minor** (`x.Y.0`) | ✨ New Features | **Yes** |
| `fix:` | **Patch** (`x.x.Z`) | 🐛 Bug Fixes | **Yes** |
| `perf:` | **Patch** (`x.x.Z`) | 🔧 Improvements | **Yes** |
| `bump:` | **Patch** (`x.x.Z`) | 🚀 Updated App Support | **Yes** |
| `chore:` | None | Hidden | **No** (Skipped) |
| `docs:` | None | Hidden | **No** (Skipped) |

---

## 2. Release Pipeline Chain (`release.yml` & `.releaserc`)

When commits are pushed to `main` (or `dev` for pre-releases), the release pipeline executes:

1. **Commit Analysis**: Evaluates commit log since previous release tag.
2. **Catalog Generation**: Executes `./gradlew generatePatchesList`.
3. **Version Sync**: Patches `patches-list.json` with `${nextRelease.version}`.
4. **README Injection**: Runs `.github/scripts/generate_patches_readme.py`.
5. **Gradle Bump**: `gradle-semantic-release-plugin` syncs `gradle.properties`.
6. **Git Commit & Tag**: Commits `CHANGELOG.md`, `patches-list.json`, `README.md`, `gradle.properties`, `patches-bundle.json` with `[skip ci]`.
7. **Artifact Publishing**: Attaches `patches/build/libs/patches-*.mpp` to the GitHub Release.
8. **Backmerge**: Merges release commit from `main` back to `dev`.

---

## 3. Branching Strategy

- **`dev` branch**: Working branch. Commits generate pre-releases (`v1.x.x-dev.1`).
- **`main` branch**: Stable release branch. Merged from `dev` without squashing.
- **Never force-push** after a release tag has been created.
