# AI Usage

I used Claude (Anthropic) while building this assignment.

## What I used it for

- **Concurrency and load testing concepts** — the largest use. Lost updates vs
  torn reads, why a `CountDownLatch` start gate makes threads genuinely
  simultaneous, why measuring peak concurrency beats timing the run, and why
  the shutdown flag needs `compareAndSet`.
- **Test code** — drafts of the concurrency, controller and logging tests.
- **Refactoring** — the `SpeechToTextClient` and `ApplicationTerminator`
  interfaces, which make the system testable without a network and without
  killing the test's own application.
- **Commit messages and documentation.**
- **The frontend** — `index.html`, `styles.css` and `app.js`.