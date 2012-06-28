package reply.hacks;

import clojure.lang.StringSeq;

import java.io.Reader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;

public class RawInputTrackingLineNumberingPushbackReader extends CustomizableBufferLineNumberingPushbackReader {

  private LinkedList<Character> rawInput = new LinkedList<Character>();

  public RawInputTrackingLineNumberingPushbackReader(Reader r){
      super(new LineNumberReader(r));
  }

  public RawInputTrackingLineNumberingPushbackReader(Reader r, int sz){
      super(new LineNumberReader(r, sz), sz);
  }

  public List<Character> getRawInput() {
      return rawInput;
  }

  public void clearRawInput() {
      rawInput = new LinkedList<Character>();
  }

  public int read() throws IOException {
      int c = super.read();
      rawInput.addLast((char) c);
      return c;
  }

  public void unread(int c) throws IOException{
      super.unread(c);
      rawInput.removeLast();
  }

  @SuppressWarnings("unchecked")
  public String readLine() throws IOException {
      String line = super.readLine();
      if (line != null && line.length() > 0) {
          rawInput.addAll(StringSeq.create(line));
      }
      return line;
  }
}

