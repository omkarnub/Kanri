# M Kanri – Agent Instructions

These rules apply to every task in this project. Read this file at the start of each session.

## 1. Skills
- Before starting any task, check all available skills and use every one that is relevant.
- Prefer a skill over ad-hoc work when one exists for the job (UI, testing, debugging, docs, etc.).
- If no skill fits, say so briefly and proceed with best practice.

## 2. Documentation (always keep it current)
- Documentation lives in `/docs` (and `README.md` for the overview). If it doesn't exist, create it.
- **Every time a feature, screen, API, dependency, permission, or config is added, changed, or removed, update the docs in the same task.** A task is not done until docs match the code.
- Keep a `CHANGELOG.md` with a dated entry for every change (Added / Changed / Fixed / Removed).
- Document for each feature: what it does, files/modules involved, data flow, and known limitations.
- Keep the architecture notes, setup steps, and build/run instructions accurate.

## 3. Testing on Emulator – ASK, DON'T TEST
- After every successful APK build and install on the emulator, **stop and ask me to test it.**
- Do **not** open, tap through, or test the app yourself unless I explicitly tell you to.
- When asking, give a short checklist of what changed and what I should try.
- Wait for my feedback before continuing to the next step.

## 4. Android Development Rules
**Code & architecture**
- Follow the existing project language, structure, and patterns; don't introduce a new framework without asking.
- Use a clean architecture (UI → ViewModel → Repository → Data source). Keep business logic out of Activities/Fragments/Composables.
- Handle lifecycle and configuration changes (rotation, process death) properly.
- Use coroutines/background threads for I/O; never block the main thread.

**UI/UX**
- Follow Material Design guidelines; support dark mode and different screen sizes.
- Use string, color, and dimension resources. No hardcoded text or values.
- Add content descriptions and keep touch targets accessible.

**Data & safety**
- Never hardcode secrets, API keys, or passwords. Use `local.properties` or secure storage.
- Request only the permissions truly needed, and handle denial gracefully.
- Validate input and handle errors/empty/loading states in every screen.
- Use Room/DataStore migrations carefully; never wipe user data silently.

**Build & quality**
- Pin dependency versions; don't add unnecessary libraries.
- Keep the build clean: fix warnings and lint errors before reporting done.
- Make sure ProGuard/R8 rules are correct for release builds.
- Write unit tests for logic where practical, and keep existing tests passing.
- Check Logcat for crashes/errors after install and mention any you see.

## 5. Git & Versioning
**Commits**
- Commit to git after **every completed feature or fix**, one feature per commit. Don't batch unrelated work.
- Include the updated docs and `CHANGELOG.md` in the same commit as the code.
- Use clear, conventional messages: `feat: ...`, `fix: ...`, `docs: ...`, `refactor: ...`, `chore: ...`.
- Only commit code that builds successfully.
- Never commit build outputs, APKs, keystores, secrets, or `local.properties`. Keep `.gitignore` up to date.
- Work on a feature branch for larger features and merge to `main` when done.

**Pushing to GitHub**
- **After every commit, push it to GitHub immediately** (current branch). Every new feature or fix must be backed up on the remote so it can never be lost.
- Push only code that builds successfully. Never push secrets, keystores, APKs, or `local.properties`.
- **NEVER force push** (`--force` / `--force-with-lease`) and never rewrite remote history without my explicit approval.
- If the remote isn't configured or the push fails (auth, conflicts, etc.), stop and tell me. Don't work around it.
- If a push would overwrite or conflict with newer work on the remote, stop and ask me first.
- **NEVER release the APK** (no release builds or public releases).

**Versioning (Semantic Versioning: MAJOR.MINOR.PATCH)**
- New feature → bump MINOR (e.g. 1.2.0 → 1.3.0).
- Bug fix or small tweak → bump PATCH (e.g. 1.3.0 → 1.3.1).
- Breaking change or major redesign → bump MAJOR.
- Update `versionName` and increment `versionCode` in `build.gradle` with every version bump.
- Do not create release tags unless explicitly requested by the user.
- Show the current version in the docs.

## 6. Protect Existing Work – No Silent Reverts
Newer work must never be replaced by older work. These rules are strict.

**Never do any of the following without my explicit approval:**
- Revert, undo, or roll back any feature, fix, or change (in code, docs, config, or resources).
- Replace newer code with an older version, or restore files/functions/screens from an earlier commit or backup.
- Run destructive or history-changing git commands: `git revert`, `git reset --hard`, `git checkout <old commit> -- <file>`, `git restore` over my changes, `git clean`, `git rebase` that drops commits, `git stash drop`, or branch deletion.
- Overwrite or regenerate a whole file from memory or from an old copy.

**How to avoid accidentally losing features**
- Always read the **current** version of a file before editing it. Never edit based on an older copy or an earlier part of the conversation.
- Make small, targeted edits. Don't rewrite entire files unless I ask.
- When resolving merge conflicts, keep the newest features from both sides. Never resolve a conflict by discarding my newer work.
- Before every commit, review `git diff` and confirm that no previously added feature, screen, string, or logic was removed or replaced unintentionally. If something was, fix it before committing.
- If a build or test fails, fix forward. Don't "fix" it by going back to old code.

**If a revert seems necessary:**
1. **Stop.** Don't make the change.
2. Explain to me clearly: what you want to revert, why, which files/commits/features are affected, and what would be lost or changed.
3. Offer alternatives if there are any (e.g. fix forward, patch only the broken part).
4. **Wait for my explicit approval.** Approval applies only to that specific revert, not to future ones.
5. If I don't approve, or don't answer, leave everything as it is.

## 7. Workflow for Every Task
1. Understand the request; ask if anything is unclear.
2. Check and use relevant skills.
3. Read the current state of the files you'll touch, then plan briefly and implement.
4. Build and fix errors (fix forward, no reverts).
5. Update docs and `CHANGELOG.md`.
6. Review `git diff` to confirm no existing feature was removed or replaced by older code.
7. Bump the version, commit, and **push to GitHub** (no force push; never release the APK).
8. Install the APK on the emulator.
9. **Ask me to test** and wait for feedback.