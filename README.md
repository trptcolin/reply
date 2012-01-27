# REPL-y

A fitter, happier, more productive REPL for Clojure.

## Improvements over the existing REPL
- allows a number of readline commands:
  - jumping to the start/end of lines
  - jumping forward/back by word (not available in JLine versions before 2)
  - history searching
  - and much much more
- CTRL-C:
  - doesn't bail out of the process - stops interruption-friendly operations
  - kills the currently running command, breaks out of infinite loops, etc.
- code completion for:
  - vars, namespaces, Java classes
  - Java packages
  - namespace-qualified vars
  - Java static methods
- ClojureDocs support
- nREPL integration

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

Thanks to the Clojure and JLine developers for their work on the
excellent projects that this project depends upon.

Special thanks to [8th Light](http://8thlight.com) for paying me to work on
this during our open-source Friday afternoons.


## License

Copyright (C) 2011 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure.
