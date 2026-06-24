# BarGrounds

BarGrounds is a Nukkit plugin for Minecraft Bedrock Edition that shows a clean sidebar HUD with server and player information.

## Features

- Sidebar header with live ping: `INFOSERVER <ping> ms`
- Current date and time line
- Player info block: Nick, Rank, Clan
- Economy block: Coins, Rubies, Donate Coins
- PvP stats: Kills, Deaths
- Missing integrations are shown in red (plugin keeps working)
- Safe integration checks on startup with clear log output (`[OK]`, `[WARN]`, `[MISSING]`)
- Language support: English, Russian, Ukrainian, French, German
- Auto language detection from server language

## Compatibility

- Java: 8
- Nukkit API: 1.1.0
- Gradle: 6.9.4

## Installation

1. Build the plugin jar.
2. Put `BarGrounds-1.0.0.jar` into your server `plugins/` folder.
3. Start the server once to generate plugin data files.
4. Configure plugin settings and restart or run `/bargrounds reload`.

## Build From Source

BarGrounds uses a local Nukkit jar dependency.

1. Put Nukkit jar here:
`libs/nukkit.jar`
2. Run build:

```bash
./gradlew clean build
```

Output jar:

`build/libs/BarGrounds-1.0.0.jar`

## Configuration

Config file:

`src/main/resources/config.yml`

Main options:

- `update-ticks`: sidebar refresh period (20 ticks = 1 second)
- `language-mode`: `auto`, `en`, `ru`, `uk`, `fr`, `de`
- `date-format`: Java date pattern for HUD time line
- `max-line-length`: max visible line width (16..32)
- `center-date`: center date/time line in sidebar
- `integrations.economy`: plugin name for coins integration (default `EconomyAPI`)
- `integrations.rank`: plugin name for rank integration
- `integrations.clan`: plugin name for clan integration
- `integrations.rubies`: plugin name for rubies integration
- `integrations.donate-coins`: plugin name for donate currency integration
- `footer-link`: footer text shown at bottom of panel

## Commands

- `/bargrounds reload` - reload plugin config

## Permission

- `bargrounds.command` - allows `/bargrounds reload` (default: `op`)

## Integration Behavior

- If integration plugin exists and safe hook succeeds: value is shown normally.
- If plugin exists but hook is limited: value is shown as available.
- If plugin is missing: feature line is shown in red as missing.
- Core plugin remains enabled in all cases.

## Repository

`https://github.com/RUSPlugins-Team/BarGrounds`

## Author

RUSPlugins-Team
