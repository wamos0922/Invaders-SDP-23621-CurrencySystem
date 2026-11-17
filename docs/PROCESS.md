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
| Testing | Unit tests by feature owners, test plan and report from the tester |
| Presentation | Ticket GOG-67 |

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
- Each feature owner writes unit tests for their own logic (this follows the JUnit practice from the CSE2024 labs).
- The tester owns the bigger picture: the test plan and checklist, weekly playtests, bug tickets and the final test report.
- We cap the tester at about 4-5 tickets to avoid overload and last-week crunch. The tester's tickets are on hold until he has been asked what he prefers.

**Documentation: an epic.** Docs have several deliverables and owners, so they get their own epic (GOG-63) with child tickets, so progress and ownership are visible.

**Static analysis: one ticket, not an epic.** It is a small job (run it, fix the top findings, write a summary), so it is a single ticket (GOG-68) under the quality epic (GOG-20).

## 7. Scrumban working agreement

**Board columns:** Idea → To Do → In Progress → In Review → Testing → Done
(*In Review* already exists but was unused. *Testing* is to be added; see the checklist.)

**Rules**
- **WIP limit:** at most 2 tickets In Progress per person.
- **Weekly planning:** at the start of each week we choose what moves from Idea to To Do. Nothing is committed beyond that week.
- **Weekly review and retro:** at the end of each week we note what finished, what slipped and what to change.
- **Every ticket** has one owner, a due date, and a parent epic or feature.
- **Priority labels:** Must, Should or Stretch. Challenge modes, shard space and the skill tree are Stretch. The core game ships first.
- **Definition of Done:** PR merged, reviewed by someone other than the author, tests passing, and the ticket key (for example `GOG-12`) in the branch or PR name.
- **Quality:** feature owners write unit tests; static analysis is run before the final week.

## 8. Changes and status

Update this list as work is done.

- [x] Documentation epic **GOG-63** created, with tickets GOG-64 (README roles), GOG-65 (collect role lines), GOG-66 (process notes), GOG-67 (presentation).
- [x] Static analysis ticket **GOG-68** created under GOG-20 (unassigned for now).
- [x] Team informed of the reassignment plan; no objections raised (confirmed by Haikal, 17 Nov 2026).
- [ ] Apply the reassignments in section 9.
- [ ] Merge GOG-39 to 43 into one ticket "Design 5 shard rarities".
- [ ] Close GOG-36 ("cancel").
- [ ] Create the free-roam movement ticket for Imran.
- [ ] Set GOG-18 and GOG-19 to In Progress.
- [ ] Re-parent GOG-31 under GOG-6, and GOG-32 to 34 under a new "Progression" epic.
- [ ] Add the Testing column and the WIP limit (In Review already exists; start using it).
- [ ] Add due dates and Must / Should / Stretch labels.
- [ ] Create the tester's tickets (on hold).
- [ ] Decide the owner of GOG-68 (static analysis).
- [ ] Confirm the skill-tree split (GOG-50 to 52).

## 9. Reassignment plan

**Status: planned.** This is what we agreed to review with the team. Change the status to "applied" once done.

| Ticket | Summary | From | To |
|---|---|---|---|
| GOG-30 | Enemy variants 36-39 | Muizzuddin | Nikmar |
| GOG-31 | Tutorial level | Muizzuddin | Wafi |
| GOG-35 | Story visuals and audio | Muizzuddin | Wafi |
| GOG-38 | Level completion rewards | Ikhwan | Imran |
| GOG-39 | Design 5 shard rarities (merge of GOG-39 to 43) | unassigned | Wafi |
| GOG-44, 45, 46 | Challenge modes | unassigned | Imran |
| GOG-47 | Nerfed boss variants | unassigned | Nikmar |
| GOG-48, 49 | Shard space mode | unassigned | Haikal |
| GOG-50 | Mode-specific equipment slots | unassigned | Ikhwan (*proposed*) |
| GOG-51 | Skill tree page navigation | unassigned | Wafi (*proposed*) |
| GOG-52 | Skill tree progression and unlock | unassigned | Nikmar (*proposed*) |
| GOG-16 | Fight every boss at the end | unassigned | Backlog (Stretch) |
| new | Free-roam movement in endless mode | missing | Imran |

**Open tickets per person**

| Person | Before | After (planned) |
|---|---|---|
| Amani | 12 | 12 (2 features: level cards and stamina) |
| Muizzuddin | 7 | 3 |
| Ikhwan | 5 | 5 |
| Asyrafuddin | 5 | 5 |
| Wafi | 1 | 5 |
| Imran | 0 | 5 |
| Nikmar | 1 | 4 (5 with GOG-68) |
| Haikal | 0 | 2, plus the docs tickets GOG-64 to 67 |
| Tester (Akif) | 0 | on hold |
| Unassigned | 15 | 1 (GOG-16), plus GOG-68 |

## 10. Team roles

To be confirmed by each member. Each person adds their own line to the README too (GOG-65).

| Member | Role / features |
|---|---|
| Haikal | Team lead; mode selection menu, bigger game screen, shard space mode, documentation and presentation |
| Imran | Game modes (endless and story), free-roam movement, challenge modes |
| Amani | Level-up cards, stamina system |
| Ikhwan | Shards, upgrades, customize tab |
| Muizzuddin | Story mode (planet progression, story script, story UI) |
| Wafi | Story content (planet content, tutorial, visuals and audio), shard designs |
| Asyrafuddin | Enemy variants |
| Nikmar | Boss design and implementation |
| Akif | Tester |

## 11. Testing and quality plan

- **Unit tests:** each feature owner tests their own logic (AAA pattern, JUnit). Amani's stamina tickets (GOG-59, GOG-62) are the first example.
- **Test plan and checklist:** by the tester, covering every feature (*on hold*).
- **Weekly playtest:** one per week, under GOG-20, with bugs logged as tickets.
- **Code review:** every PR reviewed by someone else; reviews stay on GitHub.
- **Static analysis:** a tool such as SpotBugs, Checkstyle or SonarLint is run on the repo (GOG-68). We keep the before and after results and note the tool and version.
- **Test report:** written near the end by the tester.

## 12. Weekly check-in log

Fill in at the end of each week.

### Week 1 (17-23 Nov)
- **Planned (moved to To Do):** [list tickets]
- **Finished:** [list tickets]
- **Slipped and why:** [notes]
- **Change for next week:** [notes]

### Week 2 (24-30 Nov)
- **Planned:** [list tickets]
- **Finished:** [list tickets]
- **Slipped and why:** [notes]
- **Change for next week:** [notes]

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

## 15. Links

- Jira project: GOG (gog-spaceinvaders.atlassian.net)
- Before-state snapshot: [`process/jira-snapshot-2026-11-17.md`](process/jira-snapshot-2026-11-17.md)
- Documentation epic: GOG-63 · Static analysis: GOG-68 · Quality epic: GOG-20
