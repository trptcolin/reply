package reply;

import clojure.lang.RT;
import clojure.lang.Symbol;

public class ReplyMain {
  public static void main(String... args) {
    String jlineLog = System.getenv("JLINE_LOGGING");
    if (jlineLog != null) {
      System.setProperty("jline.internal.Log." + jlineLog, "true");
    }
    Symbol ns = Symbol.create("reply.main");
    RT.var("clojure.core", "require").invoke(ns);
    RT.var("reply.main", "-main").applyTo(RT.seq(args));
  }
}
