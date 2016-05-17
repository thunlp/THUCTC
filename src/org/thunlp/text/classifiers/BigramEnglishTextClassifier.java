package org.thunlp.text.classifiers;

import org.thunlp.language.english.EnglishBigramWordSegment;
import org.thunlp.language.chinese.WordSegment;

public class BigramEnglishTextClassifier extends AbstractTextClassifier {

	private boolean withSpaceInBigram = true;
	
	public BigramEnglishTextClassifier(int nclasses) {
		super(nclasses);
	}
	
	public BigramEnglishTextClassifier(int nclasses, boolean b) {
		super(nclasses);
		withSpaceInBigram = b;
	}

	@Override
	protected WordSegment initWordSegment() {
	    return new EnglishBigramWordSegment(withSpaceInBigram);
	}

}
