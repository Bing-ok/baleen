// Dstl (c) Crown Copyright 2017
package uk.gov.dstl.baleen.annotators.language;

import java.util.Optional;

import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import com.google.common.collect.ImmutableSet;

import uk.gov.dstl.baleen.core.pipelines.orderers.AnalysisEngineAction;
import uk.gov.dstl.baleen.resources.SharedWordNetResource;
import uk.gov.dstl.baleen.resources.utils.WordNetUtils;
import uk.gov.dstl.baleen.types.language.WordLemma;
import uk.gov.dstl.baleen.types.language.WordToken;
import uk.gov.dstl.baleen.uima.BaleenAnnotator;

/**
 * Add lemma form of word to the WordToken.
 *
 * <p>Uses WordNet, hence coverage will be as good as their dictionary.
 *
 * @baleen.javadoc
 */
public class WordNetLemmatizer extends BaleenAnnotator {

  /**
   * Connection to Wordnet
   *
   * @baleen.resource uk.gov.dstl.baleen.resources.SharedWordNetResource
   */
  public static final String KEY_WORDNET = "wordnet";

  @ExternalResource(key = KEY_WORDNET)
  private SharedWordNetResource wordnet;

  @Override
  protected void doProcess(JCas jCas) throws AnalysisEngineProcessException {
    for (final WordToken t : JCasUtil.select(jCas, WordToken.class)) {
      int lemmas = t.getLemmas() == null ? 0 : t.getLemmas().size();
      final String text = t.getCoveredText();
      final POS pos = WordNetUtils.toPos(t.getPartOfSpeech());
      if (pos != null) {
        final Optional<IndexWord> lookupWord = wordnet.lookupWord(pos, text);
        if (lookupWord.isPresent()) {
          FSArray fsArray = new FSArray(jCas, lemmas + 1);
          if (lemmas > 0) {
            copyExistingLemmas(t, fsArray);
          }
          t.setLemmas(fsArray);
          final WordLemma wordLemma = new WordLemma(jCas);
          wordLemma.setLemmaForm(lookupWord.get().getLemma());
          t.setLemmas(lemmas, wordLemma);
        }
      }
    }
  }

  private void copyExistingLemmas(final WordToken t, FSArray fsArray) {
    int i = 0;
    for (FeatureStructure fs : t.getLemmas().toArray()) {
      fsArray.set(i, fs);
      i++;
    }
  }

  @Override
  public AnalysisEngineAction getAction() {
    return new AnalysisEngineAction(
        ImmutableSet.of(WordToken.class), ImmutableSet.of(WordLemma.class));
  }
}
