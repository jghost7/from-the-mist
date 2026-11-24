# CurseForge listing kit

Everything for the project page and the first file. `upload/` is not in git; rebuild it with the commands at the
bottom.

## Create the project

| Field | Value |
|---|---|
| Name | From the Mist |
| Summary | contents of `summary.txt` |
| Description | paste `description.md` (switch the editor to Markdown first) |
| Logo | `logo.png` (512 x 512; `logo-master.png` is the 1024 original) |
| Class | Mods |
| Main category | Mobs |
| Extra categories | Adventure and RPG |
| Licence | MIT License |
| Source | https://github.com/jghost7/from-the-mist |
| Issues | https://github.com/jghost7/from-the-mist/issues (only works once the repository is public) |

## Upload the first file

CurseForge takes the mod **jar** as it is. No zip.

| Field | Value |
|---|---|
| File | `upload/from-the-mist-1.0.0.jar` |
| Display name | From the Mist 1.0.0 (Fabric 26.3) |
| Release type | Beta |
| Game version | 26.3 |
| Mod loader | Fabric |
| Java version | Java 25 |
| Environment | Client and Server |
| Changelog | contents of `changelog.md` |
| Related projects | Fabric API, as a **Required Dependency** |

## Gallery

`screenshots/` has six frames from the automated test run. They do the job, but they show a flat test world and
the default hand. Screenshots from a real survival world will sell it better.

## Before going public

- The GitHub repository is private. The description links to it, so make it public first or remove the link.
- The description deliberately names no film, show or book. Keep it that way in the listing and in replies.

## Rebuilding the upload file

```
gradlew build
copy build\libs\from-the-mist-1.0.0.jar curseforge\upload\
```
