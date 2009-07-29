package mt.train;

import java.util.*;

import it.unimi.dsi.fastutil.ints.IntArrayList;


/**
 * Same as LinearTimePhraseExtractor, but restricts phrases according to dependencies read from an info file.
 *
 * @author Michel Galley
 */
public class DependencyPhraseExtractor extends LinearTimePhraseExtractor {

  private static final boolean EXTRACT_MODIFIER_PHRASES = System.getProperty("extractModifierPhrases") != null;

  private static final int NO_ID = -2;
  private static final int ROOT_ID = -1;

  public DependencyPhraseExtractor(Properties prop, AlignmentTemplates alTemps, List<AbstractFeatureExtractor> extractors) {
    super(prop, alTemps, extractors);
    System.err.println("Using Moses phrase extractor.");
  }

  final List<Integer> deps = new IntArrayList(500);

  @Override
  public void setSentenceInfo(String infoStr) {

    deps.clear();
    StringTokenizer tok = new StringTokenizer(infoStr);

    // Read tokens:
    while(tok.hasMoreTokens()) {
      String t = tok.nextToken();
      int idx = t.indexOf(":");
      if(idx < 0)
        throw new RuntimeException("Bad token: "+t);
      int src = Integer.parseInt(t.substring(0,idx));
      int tgt = Integer.parseInt(t.substring(idx+1));
      System.err.printf("%s : %d -> %d\n", t, src, tgt);
      
      if(src <= ROOT_ID || tgt < ROOT_ID)
        throw new RuntimeException(String.format("Ill-formed dependency: %d -> %d\n", src, tgt));

      while(deps.size() <= src)
        deps.add(NO_ID);
      deps.set(src, tgt);
    }

    // Sanity check:
    for (int src = 0; src < deps.size(); src++) {
      if (deps.get(src) <= NO_ID)
        throw new RuntimeException(String.format("Word at %d dependent of %d\n", src, deps.get(src)));
    }
  }

  @Override
  public boolean ignore(WordAlignment sent, int f1, int f2, int e1, int e2) {

    if(deps.size() == 0) {
      System.err.println("warning: dependencies missing!");
      return false;
    }

    int headIdx = NO_ID;
    int headAttachCount = 0;

    for(int si=f1; si<=f2; ++si) {
      int ti = deps.get(si);
      if(f1 <= ti && ti <= f2)
        continue;
      if(headIdx == NO_ID) {
        headIdx = ti;
        ++headAttachCount;
      } else {
        if(headIdx == ti) {
          ++headAttachCount;
        } else {
          return false;
        }
      }
    }

    if(headAttachCount <= 0)
      throw new RuntimeException(String.format("Head word %d without dependents\n", headIdx));

    return !(headAttachCount > 1 && !EXTRACT_MODIFIER_PHRASES);
  }

}
