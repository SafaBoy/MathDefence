# MathDefence

An Android castle-defence arcade math game built in **Kotlin** and **Jetpack Compose**.

## Gameplay

- **Shield Core Math Challenge:** Your castle's glowing Shield Core displays a randomly generated math equation surrounded by a live countdown ring.
- **Various Attackers:** Distinct enemy types (*Blaster Bot*, *Hover Drone*, *Siege Mech*, *Void Striker*, *Plasma Tank*, *Artillery Orb*) hover over the battlefield, each displaying a candidate answer and aiming their gun at your castle.
- **Tap the Right Answer:** Fire a Shield Core energy beam to vaporize the attacker holding the correct answer, earn combo points, and advance to the next wave.
- **Wrong Answer Penalty:** Picking the wrong attacker causes **that specific attacker** to fire its gun at your castle, damaging your shields.
- **Timeout Penalty:** Taking too long causes a **random attacker** on screen to shoot your castle, damaging your shields.
- **Castle Hull Exposure:** Once shields reach `0`, incoming shots strike your castle directly until its hitpoints are depleted and the castle falls.
- **Escalating Pace:** The answer countdown period shrinks progressively as waves increase, making enemy attacks more frequent.
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

Pushing a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) or manually triggering the **Build & Release Android APK** workflow in GitHub Actions runs unit tests, builds both Debug and Release APKs, and attaches them to a GitHub Release.
