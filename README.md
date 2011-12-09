# REPL-y

A fitter, happier, more productive REPL for Clojure.

## Improvements over the existing REPL
- allows a number of readline commands:
  - jumping to the start/end of lines
  - jumping forward/back by word (not available in JLine versions before 2)
  - history searching
  - and much much more
- CTRL-C doesn't bail out of the process - stops interruption-friendly operations
- code completion for vars, namespaces, Java classes

## Hopefully coming soon
- CTRL-C kills the currently running command, breaks out of infinite loops, etc.
- code completion for:
  - Java packages
  - namespace-qualified vars
  - Java static methods

## Maybe one day
- smart indentation
- colorization
- code completion for:
  - Java methods (maybe based on type-hinted clojure vars?)
- hold full source code in metadata? (for in-repl defns)

## Installation

I'm hoping this will eventually be distributed with Russ Olsen's excellent
[dejour](https://github.com/russolsen/dejour.git), and perhaps also with
Phil Hagelberg's [leiningen](https://github.com/technomancy/leiningen).

Meantime (assuming you have
[leiningen](https://github.com/technomancy/leiningen.git) installed):

    git clone https://github.com/trptcolin/reply.git
    cd reply
    lein deps, compile

## Usage

Run `bin/reply.sh`. This will get friendlier for non-Cygwin Windows users.

## Thanks

Thanks to the Clojure and JLine developers for their work on the
excellent projects that this project depends upon.

Special thanks to [8th Light](http://8thlight.com) for paying me to work on
this during our open-source Friday afternoons.


## License

Copyright (C) 2011 Colin Jones

Distributed under the Eclipse Public License, the same as Clojure.
