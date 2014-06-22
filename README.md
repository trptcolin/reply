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
- ClojureDocs support via a `clojuredocs` command
- Optional nREPL integration

## Installation

REPLy is now part of [leiningen](https://github.com/technomancy/leiningen), as
of the 2.x series. It's definitely your best bet for installation and Clojure
development in general.

And here's how to get a standalone version up and running (assuming you have
[leiningen](https://github.com/technomancy/leiningen.git) installed):

    git clone https://github.com/trptcolin/reply.git
    cd reply
    lein do deps, compile

## Usage

The easiest way to use REPLy is simply to run `lein repl` (for 2.x). That's it!

If you want to check out the latest stuff on REPLy master, you can run `lein
trampoline run` in this project.

If you're confined to Leiningen 1.x, you can use the example bin scripts that
are set up to work with Leiningen 1.x.

If for some reason your use case requires avoiding Leiningen, you can use the
bin scripts as a guide (you're probably used to shell scripting anyway, in that
case). If you want to add additional dependencies to the classpath, setting
`$USER_CP` will. For example: `USER_CP=$(lein classpath) reply`.

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
[JLine](https://github.com/jline/jline2), [nREPL](https://github.com/clojure/tools.nrepl),
[clojure-complete](https://github.com/ninjudd/clojure-complete),
[ClojureDocs](http://clojuredocs.org), and [clojuredocs-client](https://github.com/dakrone/clojuredocs-client),
for their work on the excellent projects that this project depends upon.

Special thanks to [8th Light](http://8thlight.com) for allowing me to work on
this during our open-source Friday afternoons.


## License

Copyright (C) 2011-2013 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure. See the
LICENSE file for details.
