[![Tests](https://github.com/trptcolin/reply/actions/workflows/test.yml/badge.svg)](https://github.com/trptcolin/reply/actions/workflows/test.yml)
[![Clojars Project](https://img.shields.io/clojars/v/reply/reply.svg)](https://clojars.org/reply/reply)
[![cljdoc badge](https://cljdoc.org/badge/reply/reply)](https://cljdoc.org/d/reply/reply/CURRENT)
[![downloads badge](https://versions.deps.co/reply/reply/downloads.svg)](https://clojars.org/reply/reply)

# REPL-y

A fitter, happier, more productive REPL for Clojure.

## Improvements over the existing REPL that comes in clojure.jar

- A number of readline commands, some not available in earlier JLine versions:
  - navigation to the start/end of lines, and forward/back by word
  - history navigation and search
  - and much much more
- CTRL-C:
  - kills the currently running command, breaks out of infinite loops, etc.
  - doesn't bail out of the process - stops interruption-friendly operations
- Code completion for:
  - Clojure vars and namespaces
  - Clojure namespace-qualified vars
  - Java classes, packages
  - Java package-qualified classes, static methods
- Optional nREPL integration

## Installation

REPLy is bundled with [Leiningen][]. If you're using it then you're covered.

Here's how to get a standalone version up and running (assuming you have
Leiningen installed):

    git clone https://github.com/trptcolin/reply.git
    cd reply
    lein compile
    lein run

## Usage

### Leiningen

The easiest way to use REPLy is simply to run `lein repl`. That's it!

If you want to check out the latest stuff on REPLy main, you can run `lein
trampoline run` in this project.

### Clojure CLI (tools.deps)

Starting REPLy using the `clojure` command is as easy as:

    clojure -Sdeps '{:deps {reply/reply {:mvn/version "0.6.0"}}}' -M -m reply.main

**Note:** REPLy doesn't pin a Clojure version - it's just a library, so the
Clojure version comes from your own deps. To see what your setup resolves to,
run `clojure -e '(clojure-version)'`.
[More on the distinction between the Clojure CLI and Clojure itself](https://clojureverse.org/t/how-to-declare-a-super-specific-version-of-clojure-in-the-deps-edn/6751/2?u=jgomo3).

### Other

If you want to use REPLy from another piece of software, your entry point
should be `reply.main/launch-nrepl`. There are lots of options, which you can
learn more about by running `(println (last (reply.main/parse-args ["-h"])))`.

## Debugging

If you're having problems, feel free to [open an
issue](https://github.com/trptcolin/reply/issues), but the following may help.

For keybinding issues, check out `~/.inputrc` - you can mostly use the same
specifications there as you can with normal readline applications like bash,
but from time to time we do come across missing features that we then add to
[jline](https://github.com/jline/jline3).

By default REPLy quiets JLine's own logging: during startup it sets the
`org.jline` `java.util.logging` logger to `SEVERE`. If you need to see what
JLine is doing under the hood (handy for debugging keybinding issues), lower
that logger's level to something more verbose like `FINE` after REPLy starts.

You can use the `--standalone` flag to rule out any nREPL-related questions,
but I'm not aware of anyone using `--standalone` for other purposes. Please let
me know if you are!

## Thanks

Thanks to the developers of [Clojure](https://github.com/clojure/clojure),
[JLine](https://github.com/jline/jline3), [nREPL](https://github.com/nrepl/nrepl),
and [incomplete](https://github.com/nrepl/incomplete)
for their work on the excellent projects that this project depends upon.

Special thanks to [8th Light](http://8thlight.com) for allowing me to work on
this during our open-source Friday afternoons.

## License

Copyright (C) 2011-2026 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure. See the
LICENSE file for details.

[Leiningen]: https://leiningen.org
