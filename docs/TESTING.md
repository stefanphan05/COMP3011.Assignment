# Testing

```bash
./mvnw test
```

19 tests. Most run against the stub speech-to-text client, so no API key and no
network are needed.

## Does it match the spec

| Test | Why it exists                                                                                           |
|---|---------------------------------------------------------------------------------------------------------|
| `uptimeReturnsSpecShape` | The field names and date format are fixed by the spec, so this compares against the spec's own example. |
| `statsReturnSpecShape` | Same reason, for the stats endpoint.                                                                    |
| `statsHandleValuesBeyondIntRange` | The spec says 64-bit, so a big number must not come back negative.                                      |
| `shutdownReturnsAccepted` | The first shutdown request must be 202, not 200, it has been accepted, not finished.                    |
| `shutdownReturnsConflictWhenAlreadyShuttingDown` | A second request must be refused with 409.                                                              |
| `shutdownRejectsWrongMethod` | A GET on a POST-only path should say 405, not "not found".                                              |
| `validUploadReturnsTranscript` | Pins down the normal case before testing the broken ones.                                               |

## Does it fail properly

| Test | Why it exists |
|---|---|
| `missingFilePartReturnsBadRequest` | Forgetting the file is the caller's mistake, so it must be 400 and not 500. |
| `nonMultipartBodyReturnsUnsupportedMediaType` | Sending JSON instead of a file is also the caller's mistake, so 415. |
| `upstreamFailureReturnsBadGateway` | If OpenAI is down, this server is fine, so 502 tells an operator where to look. |
| `unexpectedFailureLeaksNothing` | Error messages can contain secrets, so the client must only ever see a generic one. |

## Does it survive many requests at once

| Test | Why it exists |
|---|---|
| `noTokenUpdatesAreLost` | Many threads add to the counters at the same time, and none of those additions may go missing. |
| `statsNeverShowAHalfFinishedUpdate` | A stats request can arrive mid-update and must never see two numbers that don't belong together. |
| `handles250SimultaneousRequests` | 250 people recording at once must all get an answer, quickly, with the counters still exact. |
| `exactlyOneShutdownRequestIsAccepted` | 200 requests race for something that can only happen once, so exactly one must win. |

## Does it log enough to debug, without leaking

| Test | Why it exists                                                                                    |
|---|--------------------------------------------------------------------------------------------------|
| `unexpectedFailureIsLoggedButNotReturned` | The log needs the detail so a failure can be diagnosed, the client must not get it.              |
| `shutdownDecisionsAreLogged` | Both accepting and refusing a shutdown should be recorded, so the decision can be checked later. |
| `apiKeyNeverAppearsInLogsOrResponse` | The API key must not escape anywhere, including the log.                                         |

## Results

```
[load]     250 requests in 683 ms, peak concurrency 250
[shutdown] 1 accepted, 199 conflicts, 0 unexpected
```

One at a time, those 250 requests would take 125 seconds. "Peak concurrency" is
counted inside the stub, so the overlap is measured rather than guessed from the
clock.

## Two tests we checked could actually fail

A test that has only ever passed proves very little.

`statsNeverShowAHalfFinishedUpdate` was written first, against the old code that
used two separate counters, and it failed straight away:

```
GlobalStatsResponse[inputTokens=1999710, outputTokens=199970]
```

That is one transcription's worth out of step. Commit `c398edc` is the failing
test, `83972c6` is the fix.

`apiKeyNeverAppearsInLogsOrResponse` would pass against code that logs nothing
at all, so a line that logged the API key was added on purpose. The test failed,
the line was removed, and it passed again.