package edu.stanford.nlp.mt.decoder.feat.sparse;

import java.util.List;

import edu.stanford.nlp.mt.base.ConcreteRule;
import edu.stanford.nlp.mt.base.FeatureValue;
import edu.stanford.nlp.mt.base.Featurizable;
import edu.stanford.nlp.mt.base.IString;
import edu.stanford.nlp.mt.base.SourceClassMap;
import edu.stanford.nlp.mt.base.TargetClassMap;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

/**
 * Indicator features for each rule in a derivation.
 * 
 * @author Daniel Cer
 * @author Spence Green
 * 
 */
public class DiscriminativePhraseTable implements RuleFeaturizer<IString, String> {

  private static final String FEATURE_NAME = "DPT";

  private static final int LEXICAL_FEATURE_CUTOFF = 50;
  
  private final boolean addLexicalizedRule;
  private final boolean addClassBasedRule;
  private final int countFeatureIndex;

  private SourceClassMap sourceMap;
  private TargetClassMap targetMap;
  
  /**
   * Constructor.
   */
  public DiscriminativePhraseTable() {
    this.addLexicalizedRule = true;
    this.addClassBasedRule = false;
    this.countFeatureIndex = -1;
  }

  /**
   * Constructor for reflection loading.
   * 
   * @param args
   */
  public DiscriminativePhraseTable(String... args) {
    this.addLexicalizedRule = args.length > 0 ? Boolean.parseBoolean(args[0]) : true;
    this.addClassBasedRule = args.length > 1 ? Boolean.parseBoolean(args[1]) : false;
    this.countFeatureIndex = args.length > 2 ? Integer.parseInt(args[2]) : -1;
    if (addClassBasedRule) {
      sourceMap = SourceClassMap.getInstance();
      targetMap = TargetClassMap.getInstance();
    }
  }

  @Override
  public void initialize() {}

  @Override
  public List<FeatureValue<String>> ruleFeaturize(Featurizable<IString, String> f) {
    List<FeatureValue<String>> features = Generics.newLinkedList();
    if (addLexicalizedRule && aboveThreshold(f.rule)) {
      String sourcePhrase = f.sourcePhrase.toString("-");
      String targetPhrase = f.targetPhrase.toString("-");
      String ruleString = String.format("%s>%s", sourcePhrase, targetPhrase);
      features.add(new FeatureValue<String>(FEATURE_NAME + ":" + ruleString, 1.0));        
    }
    if (addClassBasedRule) {
      StringBuilder sb = new StringBuilder();
      for (IString token : f.sourcePhrase) {
        if (sb.length() > 0) sb.append("-");
        String tokenClass = sourceMap.get(token).toString();
        sb.append(tokenClass);
      }
      sb.append(">");
      boolean seenFirst = false;
      for (IString token : f.targetPhrase) {
        if (seenFirst) sb.append("-");
        String tokenClass = targetMap.get(token).toString();
        sb.append(tokenClass);
        seenFirst = true;
      }
      features.add(new FeatureValue<String>(FEATURE_NAME + ":" + sb.toString(), 1.0));
    }
    return features;
  }

  private boolean aboveThreshold(ConcreteRule<IString, String> rule) {
    if (countFeatureIndex < 0) return true;
    if (countFeatureIndex >= rule.abstractRule.scores.length) {
      // Generated by unknown word model...don't know count.
      return false;
    }
    double count = Math.exp(rule.abstractRule.scores[countFeatureIndex]);
    return count > LEXICAL_FEATURE_CUTOFF;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
