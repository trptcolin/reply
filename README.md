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

I'm hoping this will eventually be distributed with Russ Olsen's excellent
[dejour](https://github.com/russolsen/dejour), and perhaps also with
Phil Hagelberg's [leiningen](https://github.com/technomancy/leiningen).

Meantime (assuming you have
[leiningen](https://github.com/technomancy/leiningen.git) installed):

    git clone https://github.com/trptcolin/reply.git
    cd reply
    lein deps, compile

## Usage

Run `bin/reply.sh`. This will get friendlier for non-Cygwin Windows users.

And if you want to really live the high life:

    ln -s /path/to/reply/bin/reply.sh ~/bin/reply

If you want additional stuff on the classpath, you can set `$USER_CP`. For
instance:

    USER_CP=`lein classpath` reply

### Examples of fancy options:

No options; standalone execution:

    $ reply

Launch [nREPL](https://github.com/clojure/tools.nrepl) in interactive mode:

    $ reply --nrepl

Skip all the bells-and-whistles initialization and provide your own:

    $ reply --skip-default-init -i '(println "OHAI WORLD")'

For details on the latest and greatest:

    $ reply --help


## Thanks

Thanks to the developers of [Clojure](https://github.com/clojure/clojure),
[JLine](https://github.com/jline/jline2), [nREPL](https://github.com/clojure/tools.nrepl),
[clojure-complete](https://github.com/ninjudd/clojure-complete),
[ClojureDocs](http://clojuredocs.org), and [clojuredocs-client](https://github.com/dakrone/clojuredocs-client),
for their work on the excellent projects that this project depends upon.

Special thanks to [8th Light](http://8thlight.com) for allowing me to work on
this during our open-source Friday afternoons.


## License

Copyright (C) 2011 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure. See the
LICENSE file for details.
