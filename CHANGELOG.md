# REPLy Changelog

## 0.2.0-beta1, ???
- Overhaul of jline support to decouple jline read from STDIN read (#91, 68)
- Patch up completion for separately-started nREPL (#105)
- Add apropos-better/find-name (Andy Fingerhut)
- Eliminate defn override / `sourcery` (#102)
- Fix negative rationals (cgrand/sjacket#14)
- Allow input of non-BMP characters (jline/jline2#80)
- Handle completion load failure more gracefully (#108)

## 0.1.10, 2012-02-15
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
