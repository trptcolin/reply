package reply;

import clojure.lang.RT;
import clojure.lang.Symbol;

public class ReplyMain {
  public static void main(String... args) {
    Symbol ns = Symbol.create("reply.main");
    RT.var("clojure.core", "require").invoke(ns);

    RT.var("reply.main", "launch").invoke(args);
  }
}
