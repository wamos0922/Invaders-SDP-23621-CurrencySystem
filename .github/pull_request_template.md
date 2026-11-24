## What
<!-- One sentence. What does this PR do? -->

## Why
<!-- Which requirement / issue? Why now? -->
Ticket: GOG-<number>   <!-- also put the key in the PR title, e.g. feat: add mode select menu (GOG-12) -->

## Changes
- 

## How to test
1. Build & run
   - macOS / Linux: `javac -d bin $(find src -name "*.java") && java -cp bin:res engine.Core`
   - Windows (PowerShell): `javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName; java -cp "bin;res" engine.Core`
2. 

## Screenshots
<!-- Required for anything visible on screen -->

## Checklist
- [ ] Branch is `GOG-<number>-<short-feature>` (key in UPPERCASE) and based on latest `main`
- [ ] PR title is `<type>: <subject> (GOG-<number>)`
- [ ] One feature per PR, under ~400 changed lines
- [ ] CI is green
- [ ] Touches `DrawManager` / `Core` / `GameScreen`? → explained above
- [ ] I ran the game after my change
- [ ] Depends on another PR? → `Depends on: #N` is the first line of this description
- [ ] Reviewer requested: Haikal or Akif