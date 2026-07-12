# Elendheim Harmonizer

A tiny Android toy for singing. Open it, tap once, and sing — it stacks a
perfect fifth on top of your voice and plays the blend back to you live.

This app does three things:

- **A. Listens** to your voice through the mic.
- **B. Stacks a perfect fifth** on top in real time.
- **C. Plays it back live** so you hear the harmony as you sing.

That's the whole thing. One button, one slider, dark by default.

## Using it

1. Tap the big button and allow the microphone the first time.
2. Sing. You'll see your note and the note a fifth above it.
3. Slide **Fifth level** to taste.

Headphones are recommended so the speaker doesn't feed back into the mic.

## How it works

The harmony is a real-time granular pitch shifter: your voice is written into a
short circular buffer and read back by two overlapping, windowed taps that sweep
through it at a 3:2 rate (a perfect fifth). It runs sample-by-sample with no FFT,
so latency stays low enough to sing through. The clean voice is always kept in
the mix, with the fifth blended underneath. On a real voice, natural vibrato and
breath give it a warm, slightly choral character.

## Building

Requires a recent Android Studio (or the Android SDK + JDK 17).

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## License

MIT. See [LICENSE](LICENSE).
