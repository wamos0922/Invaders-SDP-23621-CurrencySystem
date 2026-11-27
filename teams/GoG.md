# GoG

## Team Introduction

Team members contain of student from Malaysia. We mainly focus on developing something that can give a big impact on currency system. It will not be too fancy since our focus is code reliability, stability and reusable rather than a fancy but once-used code.

## Members

The nine members worked as three squads of three. The team leader led all three squads.

| Squad | Members | Individual roles |
| --- | --- | --- |
| A | Haikal, Imran, Muizzudin | Team Leader, Code Builder, Code Checker  |
| B | Ikhwan, Wafi, Nik Ammar | Collaborator, Code Builder, Code Checker |
| C | Akif, Asyarafuddin, Aqil | UI Designer, Code Builder, Code Checker |

### GitHub profiles

| Name | GitHub |
| --- | --- |
| Haikal Fiqri | [haikalfiqri](https://github.com/haikalfiqri) |
| Zarith Imran | [zeth-ux](https://github.com/zeth-ux) |
| Muizzudin | [mfuizz](https://github.com/mfuizz) |
| Ikhwan Zarif | [wamos0922](https://github.com/wamos0922) |
| Wafi | [wafisapuan](https://github.com/wafisapuan) |
| Nik Ammar | [nikmar9705](https://github.com/nikmar9705) |
| Akif | [abangBoy13](https://github.com/abangBoy13) |
| Asyarafuddin | [asyrafuddin](https://github.com/asyrafuddin) |
| Aqil Amani | [Soy05](https://github.com/Soy05) |

## Responsibilities

# Team Requirements
Our team is responsible for designing and managing the game's currency system. This includes handling currency earning, spending, balance management, reward scaling, and integration with gameplay systems.

## Detailed Requirements
- Currency Types
  - Coins: Main currency earned through gameplay and used to purchase items.
  - Diamonds: Secondary/premium currency for special items or rewards.

- Currency Earning
  - Enemy Rewards: Earn coins by defeating minions.
  - Challenge Rewards: Receive additional or doubled rewards for completing challenges.
  - Level Rewards: Earn bonus currency after completing certain levels.
  - Difficulty Rewards: Higher difficulty levels provide higher currency rewards.

- Currency Management
  - Balance Display: Track and display the player's current coins and diamonds.
  - Item Purchases: Deduct the required currency when purchasing items.
  - Insufficient Funds: Prevent purchases when the player does not have enough currency.

## Dependencies on Other Systems

- Item System: Requires item prices and purchase requests to validate and deduct currency.
- Level Design System: Requires level completion and difficulty information to calculate level bonuses and reward scaling.
- Gameplay HUD & UI: Requires currency balance updates to display coins and diamonds during gameplay.