# GITHUB ISSUES
- hook up to dejour

# UPCOMING
- more tests
- add a license
- make things configurable:
  - hard-kills in standalone mode - that's dangerous & should be opt-out

# TO DISCUSS / SOLICIT INPUT ON
- stop all threads that were spawned (via threadgroup?)
  - nREPL has the same issue: can we get msgs back from nREPL threads that have been launched?
- what does using clj-stacktrace really buy us?
  - it must, right? but our current formatting isn't any better than clojure.stacktrace/pst in 1.3
    - worse in the standalone case (!)
- can we have a minimal version without needing all cd-client's dependencies?
  - 11 of the 15 dependencies are from cd-client (understandably of course)

# UPSTREAM
- submitted
  - Clojure, LineNumberingPushbackReader (CLJ-909)
  - clojure-complete, update for 1.3 (pull request #2)
- unsubmitted
  - have a workaround
    - Clojure, interruptible print-sequential
  - no workaround
    - jline completion paging:
      - the line with "--more--" shouldn't have completions on it
      - go back upwards, like with `less`?


