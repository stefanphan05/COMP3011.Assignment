# Design decisions

**Two interfaces exist purely to make the system testable.**
`SpeechToTextClient` lets tests run without a network or an API key, and `ApplicationTerminator` lets a test exercise the shutdown endpoint without killing the application running the test. Both have exactly one real implementation.

**The token counters live in one immutable record behind one `AtomicReference`.** Two separate `AtomicLong` fields prevent lost updates but do not keep the two numbers in step, so `/global/stats` could return input tokens from one more transcription than output tokens. See `docs/TESTING.md`.

**Uptime is measured with `System.nanoTime()`, not the wall clock.** `Instant.now()` can move backwards when the OS corrects the time, which would report a negative uptime against a spec that sets a minimum of 0.

**Virtual threads are on.** Each request blocks for the duration of the speech-to-text call, so a platform-thread-per-request model would need hundreds of OS threads to stay responsive. 250 simultaneous requests are handled in under a second.

**The browser client has no external dependencies.** No CDN, so the page renders correctly with no network. Audio is captured mono at 16 kHz and encoded as Opus at 16 kbit/s.