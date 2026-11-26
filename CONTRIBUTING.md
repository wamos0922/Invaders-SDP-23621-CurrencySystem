# Contributing

## Branches
- `main` — protected. No direct push. Changes arrive only through PRs.
- `GOG-<number>-<short-feature>` — one branch per feature, made from the latest `main`. The key `GOG-<number>` is UPPERCASE (Jira only links branches whose key is uppercase); the rest is lowercase with hyphens. Delete after merge.
  - `GOG-13-mode-selection-menu`, `GOG-52-converter-screen`
  - `<number>` is the Jira ticket number, so the branch shows up on the ticket. Wrong: `gog-13-...` (lowercase key, Jira will not link it).

## Commits
- Format: `<type>: <subject>` — see `.gitmessage.txt`. Enable it: `git config commit.template .gitmessage.txt`
- Types: `feat` `fix` `refactor` `docs` `test` `ci` `chore` `style`
- Subject ≤ 50 chars, imperative ("add", not "added"), English.
- Optional footer: `Refs GOG-<number>` (links the commit to the Jira ticket).
- Small commits. If you need "and" in the subject, split it.
- Example: `feat: add mode selection menu`

## Pull requests
- One feature per PR. Target ~500 changed lines or less.
- Base branch: `main` of our repo.
- Title: `<type>: <subject> (GOG-<number>)`, e.g. `feat: add mode selection menu (GOG-13)`. Type first, lowercase. The key goes at the end in brackets, not as a scope. The `pr-title` check enforces this.
- Description: fill the template. Put the ticket key and the reason under "Why". Screenshots for anything visible.
- Merge latest `main` into your branch before requesting review. PRs with conflicts are not reviewed.
- Request **one reviewer** by priority: **Akif** (main), then **Ikhwan** (wam, 2nd, if Akif is unavailable or is the author), then **Haikal** (3rd). Nobody reviews their own PR.
- Merge method: **Merge commit** ("Create a merge commit"). Each author's own commits stay on `main`. Delete the branch.

## Reviews
Reviewers, in order: Akif, Ikhwan (wam), Haikal (see `docs/PROCESS.md`).

Tag every comment with a priority so the author knows what is required:

| Tag | Meaning | Reviewer action |
|---|---|---|
| **P1** | Must fix — breaks behaviour or violates a rule | Request changes |
| **P2** | Should fix — strong recommendation | Request changes |
| **P3** | Consider — fix now or leave a follow-up note | Comment |
| **P4** | Optional | Approve |
| **P5** | Nit / opinion | Approve |

Review focus, in order: (1) does it work, (2) does it touch shared files more than needed, (3) consistency with existing code, (4) naming.

Approval must say **what you checked** in one line. "LGTM" alone is not a review.

## Shared files — extra care
`engine/DrawManager.java`, `engine/Core.java`, `screen/GameScreen.java` are used by every team.
Any change there must be mentioned in the PR description, and the reviewer checks it with extra care.

## Definition of Done
A ticket is Done when all of these are true:
- The `pr-title` check and CI are green (build, smoke test and `tests/`), and the game runs.
- You added or updated a test in `tests/` for your own logic and ran it. Our tests are plain Java programs with their own `main()` and `check()` helper (see `tests/ShardWalletTest.java`), not JUnit.
- The PR is reviewed and approved by a non-author, and the approval says what was checked.
- The ticket key is in the branch name and the PR title.
- It is merged with a merge commit.
- Screen work: reachable from the menu and exits with ESC.
- Docs updated if behaviour changed.

Run your own test before the PR (CI runs all of them):
- macOS / Linux: `javac -d bin $(find src -name "*.java") && javac -cp bin -d bin-tests $(find tests -name "*.java") && java -cp bin-tests:bin:res ShardWalletTest`
- Windows (PowerShell): `javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName; javac -cp bin -d bin-tests (Get-ChildItem -Recurse tests -Filter *.java).FullName; java -cp "bin-tests;bin;res" ShardWalletTest`
- A test inside a package needs the package name, for example `engine.StaminaManagerTest`.

## Jira workflow
Idea → To Do → In Progress → In Review → Done. At most 3 tickets In Progress per person. There is no Testing column: testing happens in In Review (the author runs the game, the reviewer playtests and says what was checked).
Static analysis runs once before the final week (GOG-68), and the top issues are fixed before the presentation.

## Schedule
Feature freeze: **4 Dec 2026** (agreed by the whole team). After that, PRs are bug fixes, tests and docs only. Final presentation: 8 Dec 2026.
