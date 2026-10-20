# TapAnchor

One physical button press, one real tap at a fixed point on an Android screen.

TapAnchor lets any device Android sees as a keyboard or HID controller (gamepad, selfie button, presentation clicker, e-reader page turner, USB foot pedal, keyboard, accessibility switch) trigger one real touch at an anchored (x, y) coordinate. No root. Android only.

## The problem

Most mobile games and apps only respond to a touch on the screen. Physical controllers and buttons send key events, not touches. So a gamepad button does nothing useful in a touch-only game, and an untreated controller button can turn into "back" or move the cursor.

On a PC, a USB transcription pedal works in any app because the OS turns its click into a pointer click anywhere. Android has no such bridge. TapAnchor is that bridge.

## What it does

- Reads the button directly from the hardware, before Android consumes it.
- Maps each button to a fixed point (x, y) on the screen.
- Injects a real touch at that point on every press: ACTION_DOWN and ACTION_UP with SOURCE_TOUCHSCREEN.
- While active, the button stops doing its original job. It does not become "back" and it does not move a pointer.

One button, one point, one tap.

## Requirements

- Android 7.0 (API 24) or newer
- A 64-bit device
- Shizuku installed and running (TapAnchor is built against 13.1.5)
- A device Android sees as a keyboard or HID controller

TapAnchor targets Android 15 (API 35) and is built with Java 17.

## Install

1. Download `TapAnchor-2.6.apk` from the Releases page.
2. Allow installs from unknown sources for your browser or file manager.
3. Install the APK.
4. Install Shizuku and start it. Shizuku explains the wireless debugging or ADB steps in its own app.
5. Open TapAnchor and grant it Shizuku access.

## How to use

1. Turn on and pair your button device.
2. Open TapAnchor. It lists the devices Android can see. Tap yours.
3. Tap `+ ANCORAR BOTÃO` and press the physical button once. A circle appears in the middle of the screen.
4. Drag the circle to the exact spot you want to tap. The center of the circle is the tap point.
5. Tap `TRAVAR POSIÇÃO`. The circle becomes small and translucent, and touches pass through it.
6. Open your game and press the button. Each press is one tap at the anchor.

The app UI is currently in Portuguese, so the button labels above are quoted as they appear.

To reposition, tap the small `NN` label next to the marker, drag it, and lock it again. While you are positioning, the reader is paused, so dragging never fires a tap.

## How it works

```
physical button
      |
      v
kernel /dev/input/eventN
      |
      v
Reader (runs inside Shizuku, uid shell)
      |  EVIOCGRAB: Android stops seeing the controller
      |  reads EV_KEY, ignores auto-repeat
      v
TapGuard: one press -> one tap, 10 taps/s cap
      |
      v
injectInputEvent(ACTION_DOWN/UP, SOURCE_TOUCHSCREEN, x, y)
      |
      v
game receives one tap at the anchored point
```

The Reader must run with the `shell` uid, the same credential ADB uses. A normal app cannot read an input device before Android does, nor inject a touch at that level. Shizuku provides that process with your approval. There is no root.

The touch is injected through `InputManager.injectInputEvent`, not through accessibility `dispatchGesture` and not through `performClick`. That is why games that only accept raw touch input still receive it.

The full technical description is in `SPEC-v2.md`.

## Safety invariants

These are fixed in code (`TapGuard`) and covered by `test/TapGuardTest.java`:

| Rule | Meaning |
|---|---|
| I1 | One press, one tap. Only the first down counts. Holding does not repeat. |
| I2 | No automation. No timer, queue, macro, recording, or sequence. Every tap comes from a KeyPress. |
| I3 | No randomness. The tap lands exactly on the anchor. |
| I4 | Brake at 10 taps per second across all buttons. Excess taps are dropped. |
| I5 | One button, one point. A trigger has at most one binding, and one press yields at most one tap. |

## Privacy

The manifest requests only:

- `SYSTEM_ALERT_WINDOW`, for the markers over the game
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`, to keep the service alive while you play
- `POST_NOTIFICATIONS`, for the "TapAnchor active" notification

It does not request `INTERNET`. The app does not read the screen, does not read messages, and collects no data.

The overlay permission is granted by the app itself through Shizuku (`appops set <pkg> SYSTEM_ALERT_WINDOW allow`), so you do not have to find it in system settings. This is disclosed here on purpose.

## Limits

- 64-bit only. The reader parses a 24-byte `input_event`, the 64-bit layout. On a 32-bit kernel the struct is 16 bytes and reads would not line up.
- Anchors are fixed in screen pixels. They do not migrate between portrait and landscape. Set them in the orientation you play in.
- It does not follow a mouse cursor. Mouse buttons can be anchored, but pointer movement is not followed.
- While active, the whole input device is grabbed. Buttons without an anchor also do not reach the system.
- It depends on Shizuku. Without it, the app stops at device selection.
- No macros, sequences, or timers, by product and safety decision.
- The 10 taps per second brake is not configurable.

## FAQ

**Is this cheating?**
The app does not automate anything. One tap per physical press, no timer, no sequence. Whether a specific game treats a remapped physical button as cheating is up to that game's rules. Input injected through the shell credential can be treated as suspicious by anti-cheat systems. There is no guarantee against a ban.

**Why Shizuku?**
Reading an input device before the system and injecting touch at that level both require the `shell` uid, the same credential as ADB. Shizuku runs a helper process with that credential after you approve it. No root is involved.

**Why not accessibility?**
Accessibility `dispatchGesture` draws a gesture. It does not inject a raw touch event, and games that ignore accessibility input never receive it. TapAnchor injects ACTION_DOWN and ACTION_UP as SOURCE_TOUCHSCREEN.

**Why 64-bit only?**
The reader parses a 24-byte `input_event`, the 64-bit layout. On a 32-bit kernel the struct is 16 bytes, so reads would not line up. See Limits.

**Do anchors survive rotation?**
No. They are pixel coordinates. Position them in the orientation you use, and reposition if you rotate.

**How is battery life?**
While active, a foreground service keeps the reader thread and a notification alive. The app does not poll the screen. Drain depends on the device and on how long you leave it running. No battery benchmark is published.

**Does holding the button repeat the tap?**
No. Auto-repeat events are filtered out. One press, one tap.

**Can I use more than one button?**
Yes. Each button gets its own anchor. The 10 taps per second cap is shared by all buttons.

**Does it work on PC?**
No. This is an Android app. A PC version is not part of this code.

**Does it need internet?**
No. The app has no INTERNET permission.

## Build from source

Requirements: Android Studio, Java 17, Android Gradle Plugin 8.7.3, Gradle wrapper 8.12.1.

```bash
git clone https://github.com/gufarina/tapanchor
cd tapanchor
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug`.

Dependencies include `dev.rikka.shizuku:api` and `dev.rikka.shizuku:provider` 13.1.5.

The domain layer is plain Java and runs on the JDK. The safety invariants are checked with:

```bash
test/run.sh
```

Release signing uses a local `keystore.properties` that is not in the repository. Create your own keystore before building a signed release. Never commit `keystore.properties`, `*.jks`, or `*.apk`.

## Risk of ban and terms of service

TapAnchor injects touch input through the shell credential. Some games and anti-cheat systems may consider that cheating, and game terms of service may prohibit it. Nothing here guarantees that you will not be banned. Check the rules of the game you play. You use the app at your own risk.

## License

MIT. See `LICENSE`.

## Credits

TapAnchor depends on Shizuku by RikkaApps. The privileged helper model, and the ability to run code with the shell credential without root, come from that project.
