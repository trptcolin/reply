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
of the 2.x preview series. It's definitely your best bet for installation and
Clojure development in general.

And here's how to get a standalone version up and running (assuming you have
[leiningen](https://github.com/technomancy/leiningen.git) installed):

    git clone https://github.com/trptcolin/reply.git
    cd reply
    lein deps, compile

## Usage

The easiest way: use `lein repl` (for 2.x). Skip the rest if that's all you
need.

Running `lein trampoline run` at the command line, in this project, will get
you the latest stuff on master.

There are also example bin scripts that are currently just set up to work with
Leiningen 1.x (2.x already contains REPLy, just not necessarily always the
latest from master here). If you want additional stuff on the classpath, you
can set `$USER_CP`. For instance:

    USER_CP=`lein classpath` reply

### Examples of fancy options:

Launch in [nREPL](https://github.com/clojure/tools.nrepl) mode:

    $ reply

Standalone execution (no nREPL):

    $ reply --standalone

Skip all the bells-and-whistles initialization and provide your own:

    $ reply --skip-default-init -e '(println "OHAI WORLD")'

For details on the latest and greatest:

    $ reply --help


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

Copyright (C) 2011-2012 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure. See the
LICENSE file for details.
