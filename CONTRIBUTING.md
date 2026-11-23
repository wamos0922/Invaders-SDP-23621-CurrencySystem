# Contributing

## Branches
- `main` — protected. No direct push. Changes arrive only through PRs.
- `gog-<number>-<short-feature>` — one branch per feature, made from the latest `main`. Lowercase, hyphens only. Delete after merge.
  - `gog-12-mode-select-menu`, `gog-30-enemy-variants`
  - `<number>` is the Jira ticket number, so the branch shows up on the ticket.

## Commits
- Format: `<type>: <subject>` — see `.gitmessage.txt`. Enable it: `git config commit.template .gitmessage.txt`
- Types: `feat` `fix` `refactor` `docs` `test` `ci` `chore` `style`
- Subject ≤ 50 chars, imperative ("add", not "added"), English.
- Optional footer: `Refs GOG-<number>` (links the commit to the Jira ticket).
- Small commits. If you need "and" in the subject, split it.
- Example: `feat: add mode select menu`

## Pull requests
- One feature per PR. Target ~400 changed lines or less.
- Base branch: `main` of our repo.
- Title: `<type>: <subject> (GOG-<number>)`, e.g. `feat: add mode select menu (GOG-12)`. Type first, lowercase. The key goes at the end in brackets, not as a scope. The title becomes the squash commit on `main`.
- Description: fill the template. Put the ticket key and the reason under "Why". Screenshots for anything visible.
- Merge latest `main` into your branch before requesting review. PRs with conflicts are not reviewed.
- Request **1–2 reviewers**, not everyone.
- Merge method: **Squash and merge**. Delete the branch.

## Reviews
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
Any change there must be mentioned in the PR description and reviewed by the team that owns the file (menu framework: Hello World).

## Definition of Done
- Code compiles (CI green) and the game runs
- Reviewed and approved
- Screen reachable from the menu and exits with ESC (for screen work)
- Docs/wiki updated if behaviour changed
