package org.thunlp.text.classifiers;

import org.thunlp.language.chinese.WordSegment;
import org.thunlp.language.english.BilingualBigramWordSegment;

public class BilingualBigramTextClassifier extends AbstractTextClassifier{
	private boolean withSpaceInBigram = true;
	
	public BilingualBigramTextClassifier(int nclasses) {
		super(nclasses);
	}
	
	public BilingualBigramTextClassifier(int nclasses, boolean b) {
		super(nclasses);
		withSpaceInBigram = b;
	}

	@Override
	protected WordSegment initWordSegment() {
	    return new BilingualBigramWordSegment(withSpaceInBigram);
	}
}
