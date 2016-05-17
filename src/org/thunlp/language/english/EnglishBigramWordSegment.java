package org.thunlp.language.english;

import java.util.LinkedList;

import org.thunlp.language.chinese.BigramWordSegment;
import org.thunlp.language.chinese.WordSegment;

public class EnglishBigramWordSegment implements WordSegment {
	
    private LinkedList<String> results = null; 

    private boolean withSpaceInBigram;
    
    public EnglishBigramWordSegment() {
    	this(false);
    }
    
    public EnglishBigramWordSegment(boolean b) {
    	results = new LinkedList<String>();
    	withSpaceInBigram = b;
    }
    
    public boolean outputPosTag() {
		return false;
	}

	public String[] segment(String text) {
		EnglishWordSegment seg = new EnglishWordSegment();
		String[] tokens = seg.segment(text);
		int len = tokens.length;
		results.clear();
		if (len > 1) {
			for (int i = 0; i < len - 1; ++i)
				if (withSpaceInBigram)
					results.add(tokens[i] + " " + tokens[i+1]);
				else
					results.add(tokens[i] + tokens[i+1]);
			return results.toArray(new String[results.size()]);
		} else if (len < 1) {
			return new String[] {""};
		} else {
			return tokens;
		}
	}
	
	public static void main(String argc[]) {
		BigramWordSegment bws = new BigramWordSegment();
		EnglishBigramWordSegment seg = new EnglishBigramWordSegment(false);
		String res[] = seg.segment("每 一 个 大 类 包 括 一 个 或 多 个 小 类");
		for (String str : res)
			System.out.println(str);
		res = bws.segment("每一个大类包括一个或多个小类");
		for (String str : res)
			System.out.println(str);
	}
	
}
