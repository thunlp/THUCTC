package org.thunlp.text.classifiers;

public class ClassifyResult {
	/**
	 * 分类标签编号
	 */
	public int label;
	/**
	 * 分类概率
	 */
	public double prob;

	public ClassifyResult(int i, double val){
		prob = val;
		label = i;
	}
	
	public String toString () {
		return label + "\t" + prob;
	}
	
}
