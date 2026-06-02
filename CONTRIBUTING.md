# Contributing to ToastStack

Thanks for your interest in contributing. This document describes how to set up a development environment, the conventions the project follows, and how to get a change merged.

If you only have a few minutes, the short version is: open an issue first for anything non-trivial, then submit a PR that passes `./gradlew :toaststack:lintDebug :toaststack:testDebugUnitTest`.

## Table of contents

- [Code of conduct](#code-of-conduct)
- [Getting started](#getting-started)
- [How to contribute](#how-to-contribute)
- [Project layout](#project-layout)
- [Coding conventions](#coding-conventions)
- [Tests](#tests)
- [Commit messages](#commit-messages)
- [Opening a pull request](#opening-a-pull-request)

## Code of conduct

We follow the [Contributor Covenant](CODE_OF_CONDUCT.md). Be kind, be welcoming, and assume good faith.

## Getting started

1. **Fork** the repository and clone your fork.
2. **Install prerequisites**:
   - JDK 17 or newer (Temurin or any other distribution)
   - Android Studio Ladybug or newer
   - Android SDK with platform 36 installed
3. **Verify the build**:
   ```bash
   ./gradlew :toaststack:assembleDebug :toaststack:testDebugUnitTest
   ```
4. **Run the demo app** to exercise changes by hand:
   ```bash
   ./gradlew :app:installDebug
   ```

If something does not work out of the box, that is a bug worth filing - please open an issue.

## How to contribute

- **Bug reports**: open an issue with steps to reproduce, expected vs actual behaviour, the ToastStack version, and your Compose/Kotlin/Android versions.
- **Feature requests**: open an issue describing the use case before writing code. We will discuss scope and the public API shape there to save you a wasted PR.
- **Documentation**: typos, missing context, broken links - PRs welcome with no prior issue needed.
- **Small fixes** (one-line typos, obvious bugs): jump straight to a PR.

For anything that touches the public API surface or adds a dependency, please discuss it in an issue first. This is a library - every new dependency lands in every consumer app, so we keep them minimal.

## Project layout

- **`:toaststack`** - the library module (`com.siliconcircuits.toaststack`). All public API and implementation lives here. Source goes in `toaststack/src/main/java/...`, tests in `toaststack/src/test/java/...`.
- **`:app`** - the demo app, used to showcase and manually test features. Not published.

See [CLAUDE.md](CLAUDE.md) for an architecture overview (the singleton registry + state holder + host renderer pattern, style resolution order, and auto initialization).

## Coding conventions

This project ships under the Apache License 2.0 and is meant to be read by contributors who may be new to Compose, Kotlin, or both. **Readability matters more than terseness.**

### KDoc and comments

- **Every public class, object, interface, and top-level function gets a KDoc.** Lead with a one-sentence summary; expand only when the contract is non-obvious.
- **Inline comments** explain the WHY, never the WHAT. If the code reads clearly, do not narrate it.
- **No change-log style comments** ("added for issue #42") - those belong in PR descriptions and git history.

### Style

- Kotlin official code style.
- Prefer expression bodies for trivial functions, block bodies for everything else.
- No `runBlocking` in production code. Tests may use `runTest`.
- Keep the public API small and deliberate. Internal-only types use Kotlin `internal` visibility and are excluded from `consumer-rules.pro`.

### Punctuation in source files

- Use ASCII hyphens (`-`), never em dashes (`-`) or en dashes (`-`). This applies to KDoc, code comments, commit messages, and any markdown file.

### Public API changes

- If you change the public API, update `README.md` and the relevant section of `CLAUDE.md`.
- Bump the library version in `toaststack/build.gradle.kts` for any release-worthy change.
- Add or update `consumer-rules.pro` if you add public API that R8 must preserve.

## Tests

Tests use JUnit 4 + Robolectric, so no device is needed - they run on the JVM.

```bash
# Run all library unit tests
./gradlew :toaststack:test

# Run a single test class
./gradlew :toaststack:testDebugUnitTest --tests "com.siliconcircuits.toaststack.SomeTestClass"

# Lint both modules (also runs in CI)
./gradlew :toaststack:lintDebug :app:lintDebug
```

- Every bug fix must include a regression test.
- Every new feature must include unit tests for its behaviour, and a Robolectric Compose UI test where it affects rendering.

## Commit messages

We follow **Conventional Commits**:

```
type(scope): short summary

Longer body if needed, wrapped at ~72 chars. Explain WHY,
not WHAT (the diff already shows what).
```

Common types:
- `feat` - new public API or user-facing capability
- `fix` - bug fix
- `refactor` - code restructuring with no behavioural change
- `test` - tests only
- `docs` - documentation only
- `chore` - build, CI, dependency bumps
- `perf` - performance improvement

Examples:
- `feat(api): add ToastStack.configure() for global defaults`
- `fix(state): stop auto-dismiss timer when TalkBack is active`
- `docs(readme): document the swipe-to-dismiss direction option`

## Opening a pull request

1. **Branch** from `main` with a descriptive name: `feat/inline-action-icons`, `fix/duplicate-window`.
2. **Keep PRs focused** - one logical change per PR.
3. **Run the local verification** before pushing:
   ```bash
   ./gradlew :toaststack:lintDebug :toaststack:testDebugUnitTest
   ```
4. **Open the PR** against `main` with a summary of what changed and **why**, a test plan, and a linked issue (`Closes #123`) where relevant.
5. **Address review feedback** by pushing follow-up commits, not force-pushes. We squash on merge so history stays clean.

Thanks again for contributing!
