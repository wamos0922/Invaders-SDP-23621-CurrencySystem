# Process Review and Workflow Redesign

**Team:** GoG (9 members) · **Project:** Space Invaders extension (CSE2024) · **Jira:** project `GOG` (gog-spaceinvaders.atlassian.net)
**Date of review:** 17 Nov 2026 · **Written by:** Haikal (team lead) · **Status:** in progress, see the checklist in section 8

---

## 1. Purpose

This document records how we reviewed our own project workflow on 17 Nov 2026, what we found wrong, what we decided to change and why, and what evidence we keep. It is both a team record and the basis for the "best practice / what we did wrong and would change" part of our final presentation (8 Dec 2026).

## 2. Context

- Nine members, each owning their own features (no sub-squads in this second half).
- Game: Space Invaders extension with game modes (endless and story), bigger game screen, enemy and boss variants, level-up cards, stamina, shards and challenges, and a story where the planet changes at certain levels.
- Tracking: Jira project `GOG`. Code: our own GitHub repo, with PRs into `main`.
- Final presentation: **8 Dec 2026**. At the time of the review, 21 days remained.

## 3. What we are evaluated on

After a conversation with Prof. Lee on 17 Nov 2026 we wrote down the evaluation criteria as we understood them.

**Individual**
- All lab submissions, problem analysis and self-reflection.
- Team project contribution as shown in Jira and GitHub. A member who never appears in Jira is treated as having done no implementation, so testing, implementation, planning and documentation all have to be visible in Jira.

**Group**
- Project management as shown in Jira and Git: how work was broken down (WBS), how roles were divided, and who was assigned what.
- The process we claim must be visible. If we say Scrum but Jira shows no Scrum process, we are not showing an agile process. Another form (for example a whiteboard) is acceptable if we can show it.
- A README that states each member's role and what they did, in case Jira does not show it perfectly.
- Static analysis (if used): the tool, extensions and analysis must be presented and submitted.
- Code reviews visible on PRs or in CI, and tests visible on GitHub.
- A presentation of what we did, our best practices, or what we did wrong and would change.

**How we plan to show each item**

| Criterion | Where it will be visible |
|---|---|
| Individual work in Jira | Every member has their own tickets, with PRs linked |
| Planning, WBS, roles | Epics, features, subtasks, assignees and due dates in Jira |
| Process (Scrumban) | Board columns, WIP limit, weekly check-in notes (section 12) |
| Roles in README | README roles table |
| Static analysis | Ticket GOG-68, report and screenshots in `docs/process/` |
| Reviews and CI | PR reviews on GitHub, links in tickets |
| Testing | Plain Java tests in `tests/` by feature owners, run by the CI `tests` job; test plan and report from the tester |
| Presentation | Checklist in GOG-66 |

## 4. Starting state (17 Nov 2026)

We reviewed every ticket in the project. At the time of the first review there were 47 tickets (GOG-4 to GOG-52). During the review, 10 more stamina subtasks (GOG-53 to GOG-62) were added by Amani, so the full before-snapshot has 57 tickets. The full list is in [`process/jira-snapshot-2026-11-17.md`](process/jira-snapshot-2026-11-17.md).

Key facts at the time:
- Only 8 of the 47 original tickets were Done.
- The board had **no sprints**. Jira reported that the board does not support them, so it behaved like a loose Kanban board. It was not Scrum.
- The board had the columns Idea, To Do, In Progress, In Review and Done. In Review was empty, and there was no Testing column.
- Open (not Done) tickets per person at 14:37: Amani 12 (including the 10 new subtasks), Muizzuddin 7, Ikhwan 5, Asyrafuddin 5, Wafi 1, Nikmar 1, Imran 0, Haikal 0, **tester (Akif) 0**.
- Between 14:40 and 14:41, all 10 stamina subtasks (GOG-53 to 62) were moved to Done, and Muizzuddin moved GOG-35 and GOG-37 to In Progress. So the 14:37 snapshot and the later screenshots differ. See [`process/workload-analysis-2026-11-17.md`](process/workload-analysis-2026-11-17.md) for the current picture.
- 15 tickets had no assignee.
- No testing, documentation or static-analysis tickets existed.

## 5. Problems we found

1. **No visible process.** No sprints, no WIP limit, an unused In Review column and no Testing column, so Jira would not support a claim of Scrum.
2. **Uneven workload.** One member had 7 open tickets (Muizzuddin) while others had 0 or 1.
3. **Unassigned work.** 15 tickets had no owner.
4. **The tester had no tickets,** so he would appear to have done nothing in Jira.
5. **No testing or documentation work was planned in Jira.** Under the evaluation rules, work that is not in Jira can be treated as not done.
6. **Messy hierarchy.** Some tickets sat under the wrong parent, and several epics still showed "Idea" while containing finished work.
7. **Leftovers.** One ticket (GOG-36) was titled "cancel", and five shard-design tickets (GOG-39 to 43) were near-duplicates of one task.
8. **A missing ticket.** Free-roam movement in endless mode was Imran's responsibility but had no ticket.

## 6. Decisions

**Process: Scrumban.** We chose Scrumban over pure Kanban or Scrum.
- Pure Kanban would give us flow but little evidence of planning and review.
- Scrum would need sprints turned on and committed scope, which is hard with 21 days left and uneven feature sizes.
- Scrumban keeps the Kanban board with WIP limits, and adds a short weekly planning and review. That gives us three weekly cycles (17-23 Nov, 24-30 Nov, 1-7 Dec) and a written check-in each week as process evidence.

**Testing: shared, not a one-person job.**
- Each feature owner writes unit tests for their own logic (we planned JUnit like the CSE2024 labs, but in practice the tests are plain Java programs with their own `main()` and `check()` helper in `tests/`, and CI runs them).
- The tester owns the bigger picture: the test plan and checklist, weekly playtests, bug tickets and the final test report.
- We cap the tester at about 4-5 tickets to avoid overload and last-week crunch. The tester's tickets are on hold until he has been asked what he prefers.

**Documentation: one epic, one ticket.** Docs get their own epic (GOG-63) so the work is visible in Jira. We first created four child tickets, then merged them into a single task (GOG-66) with a checklist, the same style as GOG-6. One ticket is easier to keep up to date, and GOG-64, 65 and 67 were deleted as duplicates.

**Static analysis: one ticket, not an epic.** It is a small job (run it, fix the top findings, write a summary), so it is a single ticket (GOG-68). The quality epic GOG-20 was later merged into GOG-63 (23 Nov 2026).

## 7. Scrumban working agreement

**Board columns:** Idea → To Do → In Progress → In Review → Done
(*In Review* already exists but was unused. We cancelled the separate *Testing* column on 26 Nov 2026: testing happens in In Review, where the author runs the game and the reviewer playtests.)

**Rules**
- **WIP limit:** at most 3 tickets In Progress per person (raised from 2 at Team Meeting #2, 25 Nov 2026).
- **Weekly planning:** at the start of each week we choose what moves from Idea to To Do. Nothing is committed beyond that week.
- **Weekly review and retro:** at the end of each week we note what finished, what slipped and what to change.
- **Every ticket** has one owner, a due date, and a parent epic or feature.
- **Priority labels:** Must, Should or Stretch. Challenge modes, shard space and the skill tree are Stretch. The core game ships first.
- **Definition of Done:** the `pr-title` check and CI are green (build, smoke test and `tests/`) and the game runs; the owner added or updated a test in `tests/` for their own logic; the PR is reviewed by one of the three reviewers (never the author) and the approval says what was checked; the ticket key (for example `GOG-13`) is in the branch and PR title; merged with a merge commit; screen work is reachable from the menu and exits with ESC. Full list in `CONTRIBUTING.md`.
- **Git conventions** (details in `CONTRIBUTING.md`): branch `GOG-<number>-<short-feature>` (key in uppercase so Jira links it); commit `<type>: <subject>` with optional `Refs GOG-<n>` footer; PR title `<type>: <subject> (GOG-<n>)`, checked by the `pr-title` workflow; merge commit (decided 26 Nov 2026, not squash).
- **Reviewers (agreed by the team, 23 Nov 2026):** three people review PRs, in this order of priority: Akif (main reviewer and tester), Ikhwan (wam, 2nd), Haikal (3rd). The author requests Akif first; if Akif is unavailable or is the author, Ikhwan; then Haikal.
- **PR size:** one feature per PR, about 500 changed lines or fewer (raised from 400 at Team Meeting #2, 25 Nov 2026).
- **Feature freeze:** 4 Dec 2026, agreed by the whole team. After that, PRs are bug fixes, tests and docs only.
- **Quality:** feature owners add tests in `tests/` (run by CI); static analysis is run once before the final week (GOG-68).

## 8. Changes and status

Update this list as work is done. Last updated 26 Nov 2026 (Team Meeting #2 and the 26 Nov decisions added below).

**Done**
- [x] Documentation epic **GOG-63** created, with one task **GOG-66** (checklist for README, process notes and presentation). GOG-64, 65 and 67 were deleted as duplicates.
- [x] Static analysis ticket **GOG-68** created under GOG-20 (unassigned for now).
- [x] Free-roam movement ticket **GOG-69** created for Imran, under GOG-7.
- [x] "Progression" epic **GOG-70** created (Ikhwan); GOG-32 to 34 moved under it.
- [x] Team informed of the reassignment plan; no objections raised (confirmed by Haikal, 17 Nov 2026).
- [x] Reassignments applied (section 9). GOG-46, 48 and 49 were moved back to Ikhwan, and GOG-50 to 52 back to Amani, after review.
- [x] Every non-epic ticket now sits under an epic. The project has 6 epics.

**Team Meeting #2 (25 Nov 2026)**
- [x] Decided: WIP limit raised from 2 to 3 tickets In Progress per person.
- [x] Decided: PR size limit raised from 400 to 500 changed lines.
- [ ] Still open from the meeting: assign an integration owner, set due dates for boss tickets GOG-74 to 86.

**Team decisions (26 Nov 2026)**
- [x] Merge method is a merge commit, not squash.
- [x] The Testing column is cancelled; testing happens in In Review.
- [x] Feature freeze on 4 Dec 2026, agreed by everyone.
- [x] Reviewer order is the same in every document: Akif, then Ikhwan (wam), then Haikal.

**Still to do**
- [ ] Close or remove GOG-36 ("cancel").
- [ ] Set GOG-18, GOG-19 and GOG-70 to In Progress.
- [ ] Decide the owner of GOG-68 (static analysis) and the tool (SpotBugs, Checkstyle or SonarLint).
- [ ] Set the WIP limit to 3 in Jira (In Review already exists and is where testing happens; the Testing column was cancelled).
- [ ] Merge the CI `tests` job and fix or exclude any test that cannot run on the CI machine.
- [ ] Add due dates and Must / Should / Stretch labels.
- [ ] Create the tester's tickets (on hold until Akif says what he prefers).
- [ ] Decide whether to merge GOG-39 to 43 into one ticket "Design 5 shard rarities" (optional).
- [ ] Check GOG-35 with Muizzuddin (not moved yet).

## 9. Reassignments

**Status: applied on 17 Nov 2026.** Tickets moved out of the first plan are marked in the last column.

| Ticket | Summary | From | To | Note |
|---|---|---|---|---|
| GOG-30 | Enemy variants 36-39 | Muizzuddin | Nikmar | |
| GOG-31 | Tutorial level | Muizzuddin | Wafi | |
| GOG-38 | Level completion rewards | Ikhwan | Imran | |
| GOG-39 to 43 | Shard rarity designs | unassigned | Wafi | merge still optional |
| GOG-44, 45 | Weekly and daily challenge modes | unassigned | Imran | |
| GOG-46 | Challenge mode reset scheduling | unassigned | Ikhwan | first planned for Imran, moved back |
| GOG-47 | Nerfed boss variants | unassigned | Nikmar | |
| GOG-48, 49 | Shard space mode | unassigned | Ikhwan | first planned for Haikal, moved back |
| GOG-50 to 52 | Equipment slots and skill tree | unassigned | Amani | first planned to be split, kept with the feature owner |
| GOG-69 | Free-roam movement in endless mode | missing | Imran | new ticket |
| GOG-16 | Fight every boss at the end | unassigned | unassigned | Stretch |
| GOG-35 | Story visuals and audio | Muizzuddin | Muizzuddin | not moved yet |

**Open tickets per person** (epics not counted; checked in Jira at 17:00 KST)

| Person | Open now |
|---|---|
| Ikhwan | 7 (GOG-10, 32, 33, 34, 46, 48, 49) |
| Wafi | 7 (GOG-14, 31, 39 to 43) |
| Muizzuddin | 5 (GOG-6, 23, 35, 36, 37; GOG-36 is to be closed) |
| Amani | 5 (GOG-9, 11, 50, 51, 52; the 10 stamina subtasks are Done) |
| Asyrafuddin | 5 (GOG-8, 26 to 29) |
| Imran | 4 (GOG-38, 44, 45, 69) |
| Nikmar | 3 (GOG-15, 30, 47) |
| Haikal | 1 (GOG-66) |
| Tester (Akif) | 0, on hold |
| Unassigned | 2 (GOG-16, GOG-68) |

Before the changes, 15 tickets were unassigned and Wafi and Nikmar had one ticket each (see [`process/workload-analysis-2026-11-17.md`](process/workload-analysis-2026-11-17.md)). Ikhwan and Wafi are now the highest, so we watch them in the weekly check-in.

## 10. Team roles

| Member | Role in the second half | Main work |
| --- | --- | --- |
| Haikal Fiqri | Team lead, Jira admin | Jira setup and workflow, git rules, PR template, process docs, mode selection menu, animated main menu |
| Zarith Imran | Game modes | Endless and story modes, free-roam movement, level completion rewards |
| Aqil Amani | Level cards and stamina | Stamina system |
| Ikhwan Zarif | Progression | Shards, skill tree, customize screens, shard space mode |
| Muizzuddin | Story mode | Story script, story menu, planet progression, visuals and sound |
| Wafi | Story content | Planet backgrounds, tutorial level, shard designs |
| Asyrafuddin | Enemies and bosses | Enemy design and gameplay for levels 1-9 and 21-29, bosses at levels 10 and 30 |
| Nik Ammar | Enemies and bosses | Enemy design and gameplay for levels 11-19 and 31-39, bosses at levels 20 and 40 |
| Akif | Tester and main PR reviewer | Review and playtest, static analysis |


## 11. Testing and quality plan

- **Unit tests:** each feature owner tests their own logic with a plain Java test in `tests/` (own `main()` and `check()` helper, not JUnit; arrange-act-assert order, with the steps labelled in only some files). CI compiles and runs all of them. Amani's stamina tests (GOG-59, GOG-62) are the first example.
- **Tests CI skips:** `EffectIntegrationTest` and `TwoHitEnemyTest` are stale (they read level settings that no longer exist), and `test.StoryTest` is an interactive playtest. CI excludes these three on purpose; fixing the two stale tests is a follow-up under GOG-63.
- **Test plan and checklist:** by the tester, covering every feature (*on hold*).
- **Weekly playtest:** one per week, under GOG-63, with bugs logged as tickets.
- **Code review:** every PR is reviewed by Akif, Ikhwan or Haikal (in that priority), never the author; reviews stay on GitHub, with P1-P5 priority tags (see `CONTRIBUTING.md`).
- **Static analysis:** a tool such as SpotBugs, Checkstyle or SonarLint is run on the repo (GOG-68). We keep the before and after results and note the tool and version.
- **Test report:** written near the end by the tester.

## 12. Weekly check-in log

Fill in at the end of each week. Weeks 1 and 2 were filled from Jira (resolution dates, KST) on 26 Nov 2026. Week 2 is still running, so update it on 30 Nov.

### Week 1 (17-23 Nov)
- **Planned (moved to To Do):** the open tickets after the 17 Nov reassignments (section 9). Jira has no sprints, so the plan was not recorded as a separate step.
- **Finished (27 tickets):**
  - Imran: GOG-7 (game modes), GOG-12 (screen sizes), GOG-69 (free-roam movement)
  - Amani: GOG-11 (stamina system) and its subtasks GOG-53 to 62, GOG-71, GOG-72. GOG-54 to 62 were closed together on 17 Nov (see section 4).
  - Muizzuddin: GOG-24 (story script), GOG-37 (story menu), GOG-73 (file selection)
  - Wafi: GOG-39 to 43 (five shard designs)
  - Asyrafuddin: GOG-26 (enemy design, levels 1-9)
  - Nikmar: GOG-84 (enemy gameplay, levels 11-19)
  - Ikhwan: GOG-50 (items page)
  - Haikal: process review and Jira restructure (PR #14, GOG-63 and GOG-66 created, reassignments) and the git rules (PR #25); no Jira ticket of his own was resolved this week.
- **Slipped and why:** GOG-10, 14, 23, 27, 28, 30, 31, 33, 38, 51 and 52 were open on 17 Nov and finished in week 2. Wafi and Ikhwan started the week with 7 open tickets each, the highest in the team. New work was also added mid-week (boss tickets GOG-74 to 86 on 22 Nov, skill tree tickets GOG-88 to 91 on 23 Nov). Most reviews and merges landed on 24-25 Nov (PRs #35 to #38). The tester's tickets (GOG-68, GOG-87) were not started.
- **Change for next week:** reviewer order agreed (Akif, Ikhwan, Haikal) and git conventions written in `CONTRIBUTING.md` and the PR template (23 Nov); GOG-20 merged into GOG-63.

### Week 2 (24-30 Nov, so far)
- **Planned:** boss and enemy tickets (GOG-74 to 86), skill tree (GOG-88 to 91), polish tickets (GOG-96 to 101), daily challenge (GOG-102), and the docs tickets GOG-92 to 95 (created 23 Nov).
- **Finished so far (16 tickets, to 26 Nov):**
  - Ikhwan: GOG-10 (shard wallet), GOG-33, GOG-51, GOG-52 (customize screens), GOG-88 (skill tree data)
  - Wafi: GOG-14 (planet backgrounds), GOG-31 (tutorial level)
  - Muizzuddin: GOG-23 (story level config)
  - Imran: GOG-38 (level completion rewards)
  - Asyrafuddin: GOG-28 (enemy design, levels 21-29), GOG-74 (boss gameplay, level 10), GOG-78 (boss design, level 10)
  - Nikmar: GOG-27, GOG-30 (enemy design), GOG-75 (boss gameplay, level 20), GOG-86 (enemy gameplay, levels 31-39)
  - Haikal: docs PR #34 merged; GOG-92 to 95 are in progress or in review.
- **Slipped and why:** boss levels 30 and 40 (GOG-76, 77, 79, 81, 82) are still To Do because their due dates are not set yet (open item from Team Meeting #2). GOG-85, GOG-90 and GOG-102 are In Review. The tester's tickets GOG-68 (static analysis) and GOG-87 (review and playtest) are still To Do.
- **Change for next week:** Team Meeting #2 (25 Nov) raised the WIP limit to 3 and the PR size to about 500 lines. On 26 Nov the team chose merge commits, cancelled the Testing column, and set the feature freeze for 4 Dec.

### Week 3 (1-7 Dec)
- **Planned:** [list tickets]
- **Finished:** [list tickets]
- **Slipped and why:** [notes]
- **What we would do differently:** [notes]

## 13. Evidence and screenshots

Save images in `docs/process/images/` and date every file name. The "before" screenshots must be taken **before** the changes in section 8 are applied.

**Before (17 Nov 2026)**
- [x] `process/images/board-before-2026-11-17.png`: the full board (14:41)
- [x] `process/images/list-1-before-2026-11-17.png`, `list-2-2026-11-17.png`, `list-3-2026-11-17.png`: the list view with assignee and status (14:55, after GOG-63 to 68 were added, before any reassignment)
- [x] `process/images/workload-before-2026-11-17.png`: tickets by assignee and status (analysis in [`process/workload-analysis-2026-11-17.md`](process/workload-analysis-2026-11-17.md))
- [x] `process/images/epics-before-2026-11-17.png`: epics still showing "Idea" (14:55)

**After**
- [ ] `board-after-[date].png`: board with the new columns
- [ ] `epics-after-[date].png`: epics including Documentation and quality
- [ ] `workload-after-[date].png`: workload by assignee after reassignment

**Later**
- [ ] Static-analysis results (before and after)
- [ ] PR review examples
- [ ] Test results
- [ ] Weekly check-in notes (section 12)

## 14. What we would do differently

- Define the process and choose the board type on day one, and not claim Scrum without sprints.
- Create testing and documentation tickets together with the feature tickets.
- Give every member (including the tester) visible tickets from the start.
- Review workload per person every week, not only at the end.
- Keep Jira clean as we go: correct parents and statuses, and close cancelled tickets.
- Choose the test framework on day one. We planned JUnit but ended with plain Java test programs, and CI only started running them in the last week.

## 15. Links

- Jira project: GOG (gog-spaceinvaders.atlassian.net)
- Before-state snapshot: [`process/jira-snapshot-2026-11-17.md`](process/jira-snapshot-2026-11-17.md)
- Documentation epic: GOG-63 (task GOG-66) · Static analysis: GOG-68 · Quality epic: GOG-20 (merged into GOG-63) · Progression epic: GOG-70
