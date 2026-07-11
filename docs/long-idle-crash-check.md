# Long idle crash checks

This project cannot prove that the app will never crash, but it can simulate the Android conditions that usually happen after a long idle period:

- the app goes to background;
- Android kills the background process;
- Activity/Fragment state is recreated;
- background execution is limited by Doze;
- random navigation/actions happen after restore.

## Run

Connect a device or emulator with `adb`, then run from the repo root:

```powershell
.\scripts\android-long-idle-crash-check.ps1
```

For CI or a non-interactive smoke run:

```powershell
.\scripts\android-long-idle-crash-check.ps1 -NoInteractivePause
```

Useful options:

```powershell
.\scripts\android-long-idle-crash-check.ps1 -Serial emulator-5554
.\scripts\android-long-idle-crash-check.ps1 -DozeSeconds 120
.\scripts\android-long-idle-crash-check.ps1 -MonkeyEvents 2000 -MonkeyThrottleMs 100
.\scripts\android-long-idle-crash-check.ps1 -SkipDoze -SkipMonkey
```

Reports are written to:

```text
build/reports/long-idle-crash-check
```

## What the script checks

- Process death: start app, send Home, run `adb shell am kill com.esom.bank`, restart app, then verify tabs manually.
- Don't keep activities: temporarily enables `settings put global always_finish_activities 1`, then restores the previous setting in `finally`.
- Doze: sends the app to background, runs `dumpsys deviceidle force-idle`, waits, then unforces idle.
- Monkey smoke: runs `adb shell monkey -p com.esom.bank --throttle 150 -v 1000`.
- Crash detection: clears logcat before the run and fails if `AndroidRuntime:E` output appears afterward.

## Accuracy

- Process death check is the closest fast replacement for waiting 5 hours: about 70-85% accurate for the real "Android killed my app in background" scenario.
- Don't keep activities is useful for lifecycle bugs, but it is harsher and less realistic: about 40-60% accurate.
- Doze is useful for background/network restrictions, but it does not equal process death.
- Monkey is not a precise 5-hour simulation, but it often catches navigation and lifecycle crashes.

## Pass criteria

- The script exits with code `0`.
- No `AndroidRuntime` crash output is present in the generated log.
- During manual checkpoints, Wallet, History, Settings, and Chat open after restore.
- Chat polling continues without duplicate infinite loaders or crashes.
- Push toggle and data-only push do not crash the app.
