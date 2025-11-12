# REPLy Changelog

## Unreleased

- Remove `ReplyMain` class as `clojure.main -m` makes it obsolete.
- Remove unnecessary `CustomizableBufferLineNumberingPushbackReader` class.

## 0.5.1 (2021-09-02)
- [#209](https://github.com/trptcolin/reply/pull/209): Remove clojuredocs integration, which had been broken for quite awhile
- [#208](https://github.com/trptcolin/reply/pull/208): Allow keyword completion
- [#207](https://github.com/trptcolin/reply/pull/207): Remove legacy initialization code, which was breaking completion in some scenarios

## 0.5.0 (2021-08-18)
- [#203](https://github.com/trptcolin/reply/pull/203): Replace `clojure-complete` with `incomplete`.

## 0.4.4
- Remove JDK 6 support
- Restore parsing of :port option in nREPL mode
- Url to connect to supports new transport schemes

## 0.4.3
- Fix completion-eval to not bind to namespaces that don't exist.

## 0.4.2
- Remove SIGINT interruption delay

## 0.4.1
- #166: Set history file max size from inputrc
- Use newer jline2 to support history max size config

## 0.4.0
- #181: Update to nREPL 0.4.

## 0.3.10
- Defer loading of reply.parsing namespace for faster startup
- Bump nREPL dep to 0.2.13

## 0.3.8
- Require JDK 1.6
- Bump a bunch of deps

## 0.3.7

## 0.3.6

## 0.3.5

## 0.3.4 (there was no 0.3.3), 2014-08-06
- Exclude nrepl from drawbridge dependency

## 0.3.2, 2014-08-05
- Move to official sjacket release (off trptcolin fork)
- Lots of --standalone bugfixes (@ivan)
- Display JVM/nREPL versions on startup (@bbatsov)

## 0.3.1, 2014-06-10
- Bump jline version (2.11 -> 2.12) to get lots of fixes
- nREPL rendering options to support custom printers

## 0.3.0, 2013-11-11
- Move all shutdown-agents calls into -main (technomancy/leiningen#1288)
- Add newline when exiting (#126)

## 0.2.1, 2013-07-26
- Fix completion where *print-length* is low (#120)

## 0.2.0, 2013-05-23
- Overhaul of jline support to decouple jline read from STDIN read (#91, 68)
- Patch up completion for separately-started nREPL (#105)
- Add apropos-better/find-name (Andy Fingerhut)
- Eliminate defn override / `sourcery` (#102)
- Fix negative rationals (cgrand/sjacket#14)
- Allow input of non-BMP characters (jline/jline2#80)
- Handle completion load failure more gracefully (#108)
- Speed up & avoid crash for input handling in e.g. (read) forms (NREPL-39)
- Add more default key bindings for arrows & Windows numpad (jline/jline2#86,#75)
- Patch jansi security issue (jline/jline2#85)
- Allow aborting current line via ctrl-g (jline)
- Implement forward search (jline)

## 0.1.10, 2013-02-15
- Handle interruption (via ctrl-c) during input with new jline capabilities
- Allow startup in any ns (technomancy/leiningen#955)
- Allow customizing in/out streams (technomancy/leiningen#957)

## 0.1.9, 2013-01-19
- Handle unexpected nodes in the input parse tree by failing faster
  (technomancy/leiningen#940)

## 0.1.8, 2013-01-17
- Parse characters *containing* whitespace (sjacket)

## 0.1.7, 2013-01-16
- Fix regexes with whitespace (sjacket)
- Fix printing Calendar objects (nREPL)

## 0.1.6, 2013-01-12
- Bump for nREPL 0.2.0 final

## 0.1.5, 2013-01-11
- Fix parsing characters followed by non-whitespace characters (sjacket)

## 0.1.4, 2012-12-29
- Update sjacket to handle more single characters, and long strings

## 0.1.3, 2012-12-28
- Update sjacket to handle ns-qualified keywords (#94)
- Stop busy-wait pegging CPU (#92)

## 0.1.2, 2012-11-06
- Fix exit/quit parsing

## 0.1.1, 2012-10-30
- Insulate against user-specified `*print-level*` settings on initialization
  (#89)
- Get rid of 1.4 dependency (ExceptionInfo) (#88)

## 0.1.0, 2012-10-28
- First non-beta release
