[![CircleCI](https://circleci.com/gh/trptcolin/reply/tree/main.svg?style=svg)](https://circleci.com/gh/trptcolin/reply/tree/main)
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

REPLy is bundled with [Leiningen][] and [Boot][]. If you're using one of the two then you're covered.

Here's how to get a standalone version up and running (assuming you have
Leiningen installed):

    git clone https://github.com/trptcolin/reply.git
    cd reply
    lein do deps, compile

## Usage

### Leiningen

The easiest way to use REPLy is simply to run `lein repl`. That's it!

If you want to check out the latest stuff on REPLy master, you can run `lein
trampoline run` in this project.

### Boot

[Boot][] bundles REPLy, so all you have to do is:

    boot repl

### Clojure CLI (tools.deps)

Starting REPLy using the `clojure` command is as easy as:

    # Assuming Clojure 1.9, and Clojure CLI 1.10.1.727 or later
    clojure -Sdeps '{:deps {reply/reply {:mvn/version "0.5.0"}}}' -M -m reply.main

    # Assuming Clojure 1.9 and later, and Clojure CLI before 1.10.1.727
    clojure -Sdeps '{:deps {reply {:mvn/version "0.5.0"}}}' -m reply.main

**Note:** Use `clojure -Sdescribe` to see your Clojure CLI version. On the other
hand, the Clojure version, being Clojure just a library, depends on the deps.
To see what you could consider the default Clojure, you could use
`clojure -e '(clojure-version)'`.
[More information about the distinction between Clojure CLI and Clojure](https://clojureverse.org/t/how-to-declare-a-super-specific-version-of-clojure-in-the-deps-edn/6751/2?u=jgomo3).

### Other

If for some reason your use case requires avoiding the tools listed so
far, you can use the bin scripts as a guide (you're probably used to
shell scripting anyway, in that case). If you want to add additional
dependencies to the classpath, setting `$USER_CP` will. For example:
`USER_CP=$(lein classpath) reply`.

If you want to use REPLy from another piece of software, your entry point
should be `reply.main/launch-nrepl`. There are lots of options, which you can
learn more about by running `(println (last (reply.main/parse-args ["-h"])))`.

## Debugging

If you're having problems, feel free to [open an
issue](https://github.com/trptcolin/reply/issues), but the following may help.

For keybinding issues, check out `~/.inputrc` - you can mostly use the same
specifications there as you can with normal readline applications like bash,
but from time to time we do come across missing features that we then add to
[jline](https://github.com/jline/jline2).

To get a very detailed look at what jline is doing under the hood, you can
`export JLINE_LOGGING=trace` (or `debug`) before starting REPLy. There may be
more output than you'd like, but this kind of output is especially helpful when
debugging keybinding issues.

You can use the `--standalone` flag to rule out any nREPL-related questions,
but I'm not aware of anyone using `--standalone` for other purposes. Please let
me know if you are!

## Thanks

Thanks to the developers of [Clojure](https://github.com/clojure/clojure),
[JLine](https://github.com/jline/jline2), [nREPL](https://github.com/nrepl/nrepl),
[incomplete](https://github.com/nrepl/incomplete),
for their work on the excellent projects that this project depends upon.

Special thanks to [8th Light](http://8thlight.com) for allowing me to work on
this during our open-source Friday afternoons.

## License

Copyright (C) 2011-2021 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure. See the
LICENSE file for details.

[Leiningen]: https://leiningen.org
[Boot]: https://boot-clj.com/
