# MathDefence

An Android castle-defence arcade math game built in **Kotlin** and **Jetpack Compose**.

## Gameplay

- **Shield Core Math Challenge:** Your castle's glowing Shield Core displays a randomly generated math equation surrounded by a live countdown ring.
- **Various Attackers:** Distinct enemy types (*Blaster Bot*, *Hover Drone*, *Siege Mech*, *Void Striker*, *Plasma Tank*, *Artillery Orb*) hover over the battlefield, each displaying a candidate answer and aiming their gun at your castle.
- **Modular Boss Dreadnoughts (Every 10–20 Waves):** Giant multi-component bosses (*Iron Dreadnought*, *Solar Leviathan*, *Void Titan*, *Plasma Colossus*, *Omega Citadel*) appear with 6 modular weapon pods carrying answers. Each correct shot destroys that component permanently (making it unusable by the boss), while boss weapons deal heavier shield damage when fired!
- **Skill Level Progression (Unlocked by Defeating Bosses):**
  - **Multiplication & Division (and Mixture `×`/`÷`):** Level 1 starts with `1–5×` tables, Level 2 adds `6` & `7`, Level 3 adds `8`, `9`, `10`, Level 4 adds `11` & `12`, Level 5 adds `13`, Level 6 adds `14`, and so on.
  - **Addition & Subtraction (and Mixture `+`/`−`):** Level 1 (`2d ± 1d`), Level 2 (`2d ± 2d`), Level 3 (`3d ± 2d`), Level 4 (`4d ± 2d`), Level 5 (`1 d.p.` decimals with reduced digits), Level 6–7 (larger `1 d.p.` decimals), Level 8+ (`2 d.p.` decimals & beyond).
- **Wrong Answer & Timeout Penalties:** Picking the wrong attacker/component makes **that specific unit** shoot your castle; running out of time causes a **random active attacker / intact boss component** to open fire. Once shields hit `0`, castle HP takes direct damage until it falls.
- **Escalating Pace:** The answer countdown shrinks as waves climb, making attacks more frequent.
- **Limited Pauses (3 per Game):** You get only **3 pauses per game**, and the pause screen conceals the current problem to prevent pausing to solve sums.
- **5 Math Categories:** Choose **Addition**, **Subtraction**, **Multiplication**, **Division**, or **Mixture** from the Main Menu, with per-category high scores saved automatically.

## Building APKs Locally

Make sure `build.sh` is executable and run:

```bash
./build.sh
```

Options:
- `./build.sh --all` — Build both Debug and Release APKs into `dist/` (default)
- `./build.sh --debug` — Build Debug APK only
- `./build.sh --release` — Build Release APK only
- `./build.sh --test` — Run JUnit unit tests before building APKs
- `./build.sh --clean` — Run Gradle `clean` before building
- `./build.sh --docker` — Force building inside the included Docker Android SDK container (`Dockerfile.build`)

If a local JDK 17 + `ANDROID_HOME` is detected, `build.sh` builds natively; otherwise it automatically builds inside Docker using `Dockerfile.build`.

## Automated GitHub Releases

Pushing a version tag (e.g. `git tag v1.1.0 && git push origin v1.1.0`) or manually triggering the **Build & Release Android APK** workflow in GitHub Actions runs unit tests, builds both Debug and Release APKs, and attaches them to a GitHub Release.
