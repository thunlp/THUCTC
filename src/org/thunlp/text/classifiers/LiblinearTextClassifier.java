package org.thunlp.text.classifiers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import de.bwaldvogel.liblinear.*;
import de.bwaldvogel.*;

import org.apache.commons.codec.binary.Base64;
import org.thunlp.io.TextFileWriter;
import org.thunlp.language.chinese.LangUtils;
import org.thunlp.language.chinese.WordSegment;
import org.thunlp.text.DocumentVector;
import org.thunlp.text.Lexicon;
import org.thunlp.text.Term;
import org.thunlp.text.TfIdfTermWeighter;
import org.thunlp.text.TfOnlyTermWeighter;
import org.thunlp.text.Lexicon.Word;

public abstract class LiblinearTextClassifier implements TextClassifier{
	public  Lexicon lexicon; // 词典
	private DocumentVector trainingVectorBuilder; // 用来构造训练特征向量
	private DocumentVector testVectorBuilder; // 用来构造待分类文本的特征向量
	private WordSegment seg;
	//private svm_model model; // 训练好的模型
	private de.bwaldvogel.liblinear.Model lmodel;
	private int maxFeatures = 5000; // 默认的最大特征数
	private int nclasses; // 类别数
	private int longestDoc; // 最长的文档向量长度，决定读取临时文件时缓冲大小
	private int ndocs; //训练集的大小

	public ArrayList<Integer> labelIndex = new ArrayList<Integer>(); // 类别标签
	public File tsCacheFile; // 训练集的cache文件，存放在磁盘上
	public DataOutputStream tsCache = null; // 训练集的cache输出流
	
	public int getLongestDoc() {
		return longestDoc;
	}
	
	public void init ( int nclasses, WordSegment seg) {
		lexicon = new Lexicon();
		trainingVectorBuilder =
		  new DocumentVector(lexicon, new TfOnlyTermWeighter());
		testVectorBuilder = null;
		//model = null;
		lmodel = null;
		this.nclasses = nclasses;
		ndocs = 0;
		this.seg = seg;
	}
	
	public Lexicon getLexicon() {
		return lexicon;
	}

	
	public void clear () {
		lexicon = null;
		trainingVectorBuilder = null;
		testVectorBuilder = null;
		lmodel = null;
		seg = null;
		labelIndex = null;
	}
	
	abstract protected WordSegment initWordSegment();

	public LiblinearTextClassifier( int nclasses ) {
		init( nclasses, initWordSegment());
	}
	
	/**
	 * 初始化一个基于bigram和svm的中文文本分类器
	 * @param nclasses 类别数
	 */
	public LiblinearTextClassifier( int nclasses, WordSegment seg ) {
		init( nclasses, seg);
	}
	
	/**
	 * 利用Scalable Term Selection方法进行特征选择
	 * @author sames
	 * @param cacheFile 数据集，其中term的weight应该是tf
	 * @param featureSize 数据集中特征的总数，特征应该是从0到featureSize编号
	 * @param kept 需要保留的特征数
	 * @param ndocs 训练集文档数
	 * @param nclasses 类别数
	 * @param longestDoc 最长文档含有的特征数
	 * @return
	 */
	public Map<Integer, Integer> selectFeatureBySTS(File cacheFile,
			int featureSize, int kept, int ndocs, int nclasses, int longestDoc) {
		
		// lamda初始值
		double lamda = 0.5;
		int[][] featureStats = new int[featureSize][nclasses];
		int[] featureFreq = new int[featureSize];
		double[] prValues = new double[featureSize];
		PriorityQueue<Term> selectedFeatures;
		int[] classSize = new int[nclasses];

		// 统计chi-square需要的计数
		int label = 0;
		int nterms = 0;
		double sum = 0;
		Term[] terms = new Term[longestDoc + 1];
		for (int i = 0; i < terms.length; i++) {
			terms[i] = new Term();
		}
		int ndocsread = 0;
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(cacheFile)));
			while (true) {
				int ncut = 0;
				try {
					label = dis.readInt();
					nterms = dis.readInt();
					sum += nterms;
					for (int i = 0; i < nterms; i++) {
						terms[i].id = dis.readInt();
						terms[i].weight = dis.readDouble();
						if ( lexicon.getWord(terms[i].id).getDocumentFrequency()
								== 1 ) 
							ncut++;
					}
				} catch (EOFException e) {
					break;
				}
				sum -= ncut;

				classSize[label]++;
				for (int i = 0; i < nterms; i++) {
					Term t = terms[i];
					featureStats[t.id][label]++;
					featureFreq[t.id]++;
				}				
			}
			dis.close();
		} catch (IOException e) {
			return null;
		}

		System.err.println("start STS calculation");

		// 计算chi^2_avg(t)，这里利用一个优先级队列来选择chi^2最高的特征
		selectedFeatures = new PriorityQueue<Term>(kept + 1,
				new Comparator<Term>() {
					public int compare(Term t0, Term t1) {
						return Double.compare(t0.weight, t1.weight);
					}
				});

		long A, B, C, D;

		for (int i = 0; i < featureSize; i++) {
			double pr = -1;
			double prmax = -1;
			for (int j = 0; j < nclasses; j++) {
				A = featureStats[i][j];
				B = featureFreq[i] - A;
				C = classSize[j];
				D = ndocs - C;

				double fractorBase = (double) (B * C);
				if (Double.compare(fractorBase, 0.0) == 0) {
					pr = Double.MAX_VALUE;
				} else {
					pr = (double) (A * D) / fractorBase;
					if (pr > prmax) {
						prmax = pr;
						prValues[i] = prmax;
					}
				}
			}
		}

		double targetAVL = Math.pow(sum / ndocs, 0.085 * Math.log(kept));

		Term[] featuresToSort = new Term[kept];

		double first = 0;
		double second = 1;
		int iteration = 1;
		while (true) {
			selectedFeatures.clear();
			for (int i = 0; i < featureSize; i++) {
				if ( lexicon.getWord(i).getDocumentFrequency() == 1 )
					continue;
				Term t = new Term();
				t.id = i;
				t.weight = 1.0 / (lamda / Math.log(prValues[i]) + (1 - lamda)
						/ Math.log(featureFreq[i]));
				selectedFeatures.add(t);
				if (selectedFeatures.size() > kept) {
					selectedFeatures.poll();
				}
			}
			double AVL = 0;
			int n = 0;
			while (selectedFeatures.size() > 0) {
				Term t = selectedFeatures.poll();
				featuresToSort[n] = t;
				n++;
				AVL += featureFreq[t.id];
			}
			Arrays.sort(featuresToSort, new Term.TermIdComparator());
			AVL /= ndocs;

			System.out.println("Iteration:" + iteration + " lamda = " + lamda
					+ " AVL = " + AVL + " Target AVL = " + targetAVL);
			if (Math.abs(AVL - targetAVL) < 0.1)
				break;
			else {
				if (AVL < targetAVL) {
					second = lamda;
					lamda = (first + lamda) / 2;					
				} else {
					first = lamda;
					lamda = (lamda + second) / 2;				
				}
				if( Math.abs(second - first) < 1.0E-13 )
					break;				
			}
			iteration ++;
		}

		System.err.println("generating feature map");

		// 生成旧id和新选择的id的对应表
		Map<Integer, Integer> fidmap = new Hashtable<Integer, Integer>(kept);

		for (int i = 0; i < featuresToSort.length; i++) {
			fidmap.put(featuresToSort[i].id, i);

		}
		return fidmap;

	}

	
	/**
	 * 利用chi-square统计量来进行特征选择
	 * @param dataSet 数据集，其中term的weight应该是tf
	 * @param featureSize 数据集中特征的总数，特征应该是从0到featureSize编号的
	 * @param kept 要保留的特征数
	 * @return 选择前特征到选择后特征的id对应表，保证选择后特征的排序和选择前一样
	 */
	public Map<Integer, Integer> selectFeaturesByChiSquare( 
			File cacheFile, 
			int featureSize,
			int kept ) {
		return selectFeaturesByChiSquare(cacheFile, featureSize, kept, 
				ndocs, nclasses, longestDoc, null);
	}
	
	/**
	 * 真正的特征选择函数，允许输出所有特征chimax的值，用于调试
	 * @param cacheFile
	 * @param featureSize
	 * @param kept
	 * @param chimaxValues 每个特征的chimax值，如果为null则不记录
	 * @return
	 */
	public Map<Integer, Integer> selectFeaturesByChiSquare( 
			File cacheFile, 
			int featureSize,
			int kept, 
			int ndocs,
			int nclasses,
			int longestDoc,
			double [] chimaxValues ) {
		
		System.out.println("selectFeatureBySTS : " +
				"featureSize = " + featureSize + 
				"; kept = " + kept + 
				"; ndocs = " + ndocs + 
				"; nclasses = " + nclasses + 
				"; longestDoc = " + longestDoc);
		
		int [][] featureStats = new int[featureSize][nclasses]; //某词在某类出现次数
		int [] featureFreq = new int[featureSize];//某词词频
		PriorityQueue<Term> selectedFeatures;
		int [] classSize = new int[nclasses];//每类多少篇文章
		
		// 统计chi-square需要的计数
		int label = 0;
		int nterms = 0;
		Term [] terms = new Term[longestDoc + 1];
		for ( int i = 0 ; i < terms.length ; i++ ) {
			terms[i] = new Term();
		}
		int ndocsread = 0;
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(cacheFile)));
			while ( true ) {
				try {
					label = dis.readInt();
					nterms = dis.readInt();
					//System.out.println("Reading doc "+ ndocsread + " :  label = " + label + ";  nterms = " + nterms);
					for ( int i = 0 ; i < nterms ; i++ ) { 
						terms[i].id = dis.readInt();
						terms[i].weight = dis.readDouble();
					}
				} catch ( EOFException e ) {
					break;
				}
				
				classSize[label] ++;
				for ( int i = 0 ; i < nterms ; i++ ) { 
					Term t = terms[i];
					featureStats[t.id][label] ++;
					featureFreq[t.id] ++;
				}
				if ( ndocsread++ % 10000 == 0) {
					System.err.println("scanned " + ndocsread);
				}
			}
			dis.close();
		} catch ( IOException e ) {
			return null;
		}
		
		
		System.err.println("start chi-square calculation");
		
		// 计算chi^2_avg(t)，这里利用一个优先级队列来选择chi^2最高的特征
		selectedFeatures = new PriorityQueue<Term>( kept + 1, 
				new Comparator<Term>() {
					public int compare(Term t0, Term t1) {
						return Double.compare(t0.weight, t1.weight);
					}			
		});
		
		long A, B, C, D;
		
		for ( int i = 0 ; i < featureSize ; i++ ) {
			Word w = lexicon.getWord(i);
			if (w != null) {
			  if ( w.getDocumentFrequency() == 1 || w.getName().length() > 50 )
			    continue;
			}
			double chisqr = -1;
			double chimax = -1;
			for ( int j = 0 ; j < nclasses ; j++ ) {
				A = featureStats[i][j];
				B = featureFreq[i] - A;
				C = classSize[j] - A;
				D = ndocs - A - B - C;
				
				//System.out.println("A:"+A+" B:"+B+" C:"+C+" D:"+D);
				double fractorBase = (double)( (A+C) * (B+D) * (A+B) * (C+D) );
				if ( Double.compare(fractorBase, 0.0 ) == 0 ) {
					chisqr = 0;
				} else {
					// 我们不用ndocs，因为所有特征的ndocs都一样
					//chisqr = ndocs * ( A*D -B*C) * (A*D - B*C) / fractorBase  ;
					chisqr = ( A*D -B*C) / fractorBase * (A*D - B*C)   ;
				}
				if ( chisqr > chimax ) {
					chimax = chisqr;
				}

//				被注释的方法是计算chi^2_avg即概率加权平均的卡方值。我们实际用的是chimax
//				chisqr += (classSize[j] / (double) ndocs) * 
//						ndocs * ( A*D -B*C) * (A*D - B*C) 
//						/ (double)( (A+C) * (B+D) * (A+B) * (C+D) ) ;
			}
			if ( chimaxValues != null ) {
				chimaxValues[i] = chimax;
			}
			Term t = new Term();
			t.id = i;
			t.weight = chimax;
			selectedFeatures.add(t);
			if ( selectedFeatures.size() > kept ) {
				selectedFeatures.poll();
			}
		}
		outputSecletedFeatures(selectedFeatures);
		System.err.println("generating feature map");
		
		// 生成旧id和新选择的id的对应表
		Map<Integer, Integer> fidmap = new Hashtable<Integer, Integer>(kept);
		Term [] featuresToSort = new Term[selectedFeatures.size()];
		int n = 0;
		while ( selectedFeatures.size() > 0 ) {
			Term t = selectedFeatures.poll();
			featuresToSort[n] = t;
			n++;
		}
		Arrays.sort(featuresToSort, new Term.TermIdComparator());
		for ( int i = 0 ; i < featuresToSort.length ; i++ ) {
			fidmap.put(featuresToSort[i].id, i);
		}
		return fidmap;
	}
	
	public void outputSecletedFeatures(PriorityQueue<Term> features){
		System.out.println("store features...=======================================");
		try{
			TextFileWriter tw = new TextFileWriter("selectedFeatures","UTF-8");
			Term[] f;
			f = features.toArray(new Term[features.size()]);
			System.out.println(f.length);
			for(int i=f.length-1; i>=0; i--){
				tw.writeLine(lexicon.getWord(f[i].id).getName() + " " + f[i].weight);
			}
			tw.flush();
			tw.close();
			
		}catch(IOException e){
			e.printStackTrace();
		}
		System.out.println("end store features...=======================================");
	}
	
	public void setMaxFeatures( int max ) {
		maxFeatures = max;
	}
	
	public int getMaxFeatures() {
		return maxFeatures;
	}
	

	/**
	 * 加入一篇训练文档。要求label是小于总类别数的整数，从0开始。
	 * @param text 训练文本
	 * @param label 类别编号
	 * @return 加入是否成功。不成功可能是由于不能在磁盘上创建临时文件
	 */
	public boolean addTrainingText(String text, int label) {
		if ( label >= nclasses || label < 0 ) {
			return false;
		}
		if ( tsCache == null ) {
			try {
				//tsCacheFile = File.createTempFile("tctscache", "data");
				tsCacheFile = new File(".", "tctscache" + Long.toString(System.currentTimeMillis()) + "data");
				tsCache = new DataOutputStream(
								new BufferedOutputStream(
								new FileOutputStream(tsCacheFile)));
				longestDoc = 0;
			} catch (IOException e) {
				return false;
			}
		}
		text = LangUtils.removeEmptyLines(text);
		text = LangUtils.removeExtraSpaces(text);
		String [] bigrams = seg.segment(text);
		lexicon.addDocument(bigrams);
		Word [] words = lexicon.convertDocument(bigrams);
		bigrams = null;
		Term [] terms = trainingVectorBuilder.build( words, false );
		try {
			tsCache.writeInt(label);
			tsCache.writeInt(terms.length);
			if ( terms.length > longestDoc ) {
				longestDoc = terms.length;
			}
			for ( int i = 0 ; i < terms.length ; i++ ) {
				tsCache.writeInt(terms[i].id);
				tsCache.writeDouble(terms[i].weight);
			}
		} catch (IOException e) {
			return false;
		}
		if ( ! labelIndex.contains(label) ) {
			labelIndex.add(label);
		}
		ndocs++;
		return true;
	}

	/**
	 * 分类一篇文档
	 * @param text 待分类文档
	 * @return 分类结果，其中包含分类标签和概率，对于svm分类器，概率无意义
	 */

	public ClassifyResult classify(String text) {
		String [] bigrams = seg.segment(text);
		Word [] words = lexicon.convertDocument(bigrams);
		bigrams = null;
		Term [] terms = testVectorBuilder.build( words, true);
		
		int m = terms.length;
		//svm_node[] x = new svm_node[m];
		FeatureNode[] lx = new FeatureNode[m];
		for(int j = 0; j < m; j++)
		{
			lx[j] = new FeatureNode(terms[j].id + 1, terms[j].weight);
		}
		
		ClassifyResult cr = new ClassifyResult(-1, -Double.MAX_VALUE);
		//double [] probs = new double[svm.svm_get_nr_class(model)];
		double[] probs = new double[this.lmodel.getNrClass()];
		//svm.svm_predict_probability(model, x, probs);
		//de.bwaldvogel.liblinear.Linear.predictValues(lmodel, lx, probs);
		de.bwaldvogel.liblinear.Linear.predictProbability(lmodel, lx, probs);
		for (int i = 0; i < probs.length; i++) {
		  if (probs[i] > cr.prob) {
		    cr.prob = probs[i];
		    cr.label = i;
		  }
		}
		return cr;
	}
	
	public ClassifyResult[] classify(String text, int topN){
		
		String [] bigrams = seg.segment(text);
		Word [] words = lexicon.convertDocument(bigrams);
		bigrams = null;
		Term [] terms = testVectorBuilder.build(words, true);
		
		int m = terms.length;
		//svm_node[] x = new svm_node[m];
		FeatureNode[] lx = new FeatureNode[m];
		for(int j = 0; j < m; j++)
		{
			lx[j] = new FeatureNode(terms[j].id + 1, terms[j].weight);
		}
		
		//double [] probs = new double[svm.svm_get_nr_class(model)];
		double[] probs = new double[this.lmodel.getNrClass()];
		ArrayList<ClassifyResult> cr = new ArrayList<ClassifyResult>();
		//svm.svm_predict_probability(model, x, probs);
		//de.bwaldvogel.liblinear.Linear.predictValues(lmodel, lx, probs);
		de.bwaldvogel.liblinear.Linear.predictProbability(lmodel, lx, probs);
		for(int i=0; i<probs.length; i++){
			cr.add(new ClassifyResult(i, probs[i]));
		}
		Comparator com = new Comparator() {
			public int compare(Object obj1, Object obj2){
				ClassifyResult o1 = (ClassifyResult)obj1;
				ClassifyResult o2 = (ClassifyResult)obj2;
				if(o1.prob > o2.prob + 1e-20) return -1;
				else if(o1.prob < o2.prob - 1e-20) return 1;
				else return 0;
			}
		};
		
		Collections.sort(cr,com);
		
		/*
		double totalexp = 0.0;
		for(int i=0; i<probs.length;i++){
			totalexp += Math.exp(probs[i]);
		}
		for(int i=0; i<topN; i++){
			results[i] = ""+ al.get(i).index +" "+ Math.exp(al.get(i).value)/totalexp;
		}*/
		java.text.DecimalFormat dcmFmt = new DecimalFormat("0.0000");
		ClassifyResult result[] = new ClassifyResult[topN];
		for(int i=0; i<topN; i++){
			result[i] = new ClassifyResult(cr.get(i).label, cr.get(i).prob);
		}
		cr.clear();
		//System.out.println(""+totalexp+results[0]);
		return result;
	}
	
	
	public ClassifyResult[] classify(String text, String mode){
		
		/*
		double mean1 = 0.9587235538481998;
		double mean2 = 0.027685258283684743;
		double mean3 = 0.0047742850704747064;
		double sd1 = 0.11380367342131785;
		double sd2 = 0.07759971712653867;
		double sd3 = 0.02049807196764366;
		*/
		
		double mean1 = 0.8976;
		double mean2 = 0.0663;
		double mean3 = 0.0106;
		double sd1 = 0.1547;
		double sd2 = 0.1165;
		double sd3 = 0.0275;
		
		String [] bigrams = seg.segment(text);
		Word [] words = lexicon.convertDocument(bigrams);
		bigrams = null;
		Term [] terms = testVectorBuilder.build( words, true);
		
		
		int m = terms.length;
		FeatureNode[] lx = new FeatureNode[m];
		for(int j = 0; j < m; j++){
			lx[j] = new FeatureNode(terms[j].id + 1, terms[j].weight);
		}
		
		double[] probs = new double[this.lmodel.getNrClass()];
		//de.bwaldvogel.liblinear.Linear.predictValues(lmodel, lx, probs);
		de.bwaldvogel.liblinear.Linear.predictProbability(lmodel, lx, probs);
		
		ArrayList<ClassifyResult> al = new ArrayList<ClassifyResult>();
		for(int i=0; i<probs.length; i++){
			al.add(new ClassifyResult(i, probs[i]));
		}
		if(al.size() ==0){
			System.err.println("error!result size is 0!");
			return null;
		}
		Comparator com = new Comparator() {
			public int compare(Object obj1, Object obj2){
				ClassifyResult o1 = (ClassifyResult)obj1;
				ClassifyResult o2 = (ClassifyResult)obj2;
				if(o1.prob > o2.prob + 0.0000000001) return -1;
				else if(o1.prob <o2.prob - 0.0000000001) return 1;
				else return 0;
			}
		};
		
		Collections.sort(al,com);
		
		int num=0;
		ArrayList<ClassifyResult> res = new ArrayList<ClassifyResult>();
		
		java.text.DecimalFormat dcmFmt = new DecimalFormat("0.0000");
		
		res.add(new ClassifyResult(al.get(0).label, al.get(0).prob));
		if(al.size() >=3){
			if(al.get(1).prob-mean2>2*sd2/3){
				res.add(new ClassifyResult(al.get(1).label, al.get(1).prob));
				if(al.get(0).prob+al.get(1).prob<0.99){
					if(al.get(2).prob-mean3>sd3){
						res.add(new ClassifyResult(al.get(2).label, al.get(2).prob));
					}
				}
			}
		}
		return res.toArray(new ClassifyResult[res.size()]);
		//System.out.println(""+totalexp+results[0]);
		//return results;
	}
	/**
	 * 从磁盘上加载训练好的模型
	 * @param filename 模型文件名(是一个目录)
	 * @return 加载是否成功
	 */
	
	public boolean loadModel(String filename) {
		File modelPath = new File(filename);
		if ( ! modelPath.isDirectory() )
			return false;
		
		File lexiconFile = new File( modelPath, "lexicon");
		File modelFile = new File( modelPath, "model");
		
		System.out.println(lexiconFile.getAbsolutePath());
		
		try { 
			if ( lexiconFile.exists() ) {
				lexicon.loadFromFile(lexiconFile);
				System.out.println("lexicon exists!");
			} else {
				return false;
			}
	
			if ( modelFile.exists() ) {
				//this.model = svm.svm_load_model(modelFile.getAbsolutePath());
				//this.lmodel = de.bwaldvogel.liblinear.Linear.loadModel(new File(modelFile.getAbsolutePath()));
				System.out.println("model exists!");
				this.lmodel = de.bwaldvogel.liblinear.Linear.loadModel(modelFile);
			} else {
				return false;
			}
		} catch ( Exception e ) {
			return false;
		}
		lexicon.setLock( true );
		trainingVectorBuilder = null;
		testVectorBuilder =
		  new DocumentVector(lexicon, new TfIdfTermWeighter(lexicon));
		return true;
	}
	/**
	 * 将训练好的模型保存到磁盘
	 * @param filename 保存的文件名(实际是一个目录)
	 * @return 保存是否成功
	 */

	public boolean  saveModel(String filename) {
		File modelPath = new File(filename);
		if (!modelPath.exists() && !modelPath.mkdir() ) {
			return false;
		}
		
		File lexiconFile = new File( modelPath, "lexicon");
		File modelFile = new File( modelPath, "model");
		
		try {
			lexicon.saveToFile(lexiconFile);
			//svm.svm_save_model(modelFile.getAbsolutePath(), model);
			de.bwaldvogel.liblinear.Linear.saveModel(new File(modelFile.getAbsolutePath()), lmodel);
		} catch (IOException e ) {
			return false;
		}
		return true;
	}
	/**
	 * 训练模型
	 * @return 训练是否成功。不成功可能是由于不能正确地读写临时文件造成的
	 */
	public boolean train() {
		try {
			tsCache.close();
		} catch (IOException e) {
			return false;
		}
		
		Map<Integer, Integer> selectedFeatures = selectFeaturesByChiSquare(
				tsCacheFile, lexicon.getSize(), maxFeatures);
		
		//以下注释的代码为用李景阳论文Scalable Term Selection方法选择特征，目前未经完全测试通过！！
//		Map<Integer, Integer> selectedFeatures = selectFeatureBySTS(
//				tsCacheFile, lexicon.getSize(), maxFeatures, ndocs, nclasses,
//				longestDoc);
		if ( selectedFeatures == null ) {
			return false;
		}
		System.err.println("feature selection complete");
		//svm_problem problem = createLibSVMProblem(tsCacheFile, selectedFeatures);
		///////////////////add
		de.bwaldvogel.liblinear.Problem lproblem = createLiblinearProblem(tsCacheFile, selectedFeatures);
		System.err.println("liblinear problem created");
		
		lexicon = lexicon.map( selectedFeatures );
		lexicon.setLock( true );
		tsCacheFile.delete();
		trainingVectorBuilder = null;
		testVectorBuilder = new DocumentVector(lexicon, new TfIdfTermWeighter(lexicon));

		de.bwaldvogel.liblinear.Parameter lparam = new Parameter(SolverType.L1R_LR, 500, 0.01);
		//de.bwaldvogel.liblinear.Parameter lparam = new Parameter(solverType, C, eps)

		de.bwaldvogel.liblinear.Model tempModel = de.bwaldvogel.liblinear.Linear.train(lproblem, lparam);
		System.err.println("TRAINING COMPLETE=========================================================================================");
		this.lmodel = tempModel;
		//this.model = (svm_model)tempModel;
		return true;
	}
	
	private static class DataNode implements Comparable{
		int label;
		svm_node [] nodes;
		
		public int compareTo( Object o ) {
			DataNode other = (DataNode) o;
			return label - other.label;
		}
	}
	
	private static class LdataNode implements Comparable{
		int llabel;
		FeatureNode [] lnodes;
		
		public int compareTo( Object o ) {
			LdataNode other = (LdataNode) o;
			return llabel - other.llabel;
		}
	}
	
	private de.bwaldvogel.liblinear.Problem createLiblinearProblem( File cacheFile, 
			Map<Integer, Integer> selectedFeatures){
		Vector<Double> vy = new Vector<Double>();
		Vector<svm_node[]> vx = new Vector<svm_node[]>();
		
		//DataNode [] datanodes = new DataNode[this.ndocs];
		LdataNode [] ldatanodes = new LdataNode[this.ndocs];
		//FeatureNode[][] lfeatureNodes;
		
		int label, nterms;
		Term [] terms = new Term[longestDoc + 1];
		for ( int i = 0 ; i < terms.length ; i++ ) {
			terms[i] = new Term();
		}
		int ndocsread = 0;
		//add------------------------
		int maxIndex=0;
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(cacheFile)));
			while ( true ) {
				int n = 0;
				try {
					label = dis.readInt();
					nterms = dis.readInt();
					for ( int i = 0 ; i < nterms ; i++ ) { 
						int tid = dis.readInt();
						double tweight = dis.readDouble();
						Integer id = selectedFeatures.get(tid);
						if ( id != null ) {
							terms[n].id = id;
							//add
							maxIndex=Math.max(maxIndex, id+1);
							
							Word w = lexicon.getWord(tid);
							int df = w.getDocumentFrequency();
							terms[n].weight = Math.log( tweight + 1 ) 
								* ( Math.log( (double) ( ndocs + 1 ) / df ) );
							n++;
							//System.err.println("doc " + id + " " + w);
						}
					}
				} catch ( EOFException e ) {
					break;
				}
				
				
				//----------------------------
				//lfeatureNodes = new FeatureNode[this.ndocs][n];
				//-----------------------
				
				//System.out.println("===================================================n: "+n);
				// 归一化向量
				double normalizer = 0;
				for ( int i = 0 ; i < n ; i++ ) { 
					normalizer += terms[i].weight * terms[i].weight;
				}
				normalizer = Math.sqrt(normalizer);
				for ( int i = 0 ; i < n ; i++ ) { 
					terms[i].weight /= normalizer;
				}
				
				//datanodes[ndocsread] = new DataNode();
				
				// 放入svm problem中
				ldatanodes[ndocsread] = new LdataNode();
				ldatanodes[ndocsread].llabel= label;

				FeatureNode[] lx = new FeatureNode[n];
				for ( int i = 0; i < n ; i++ ) {
					lx[i] = new FeatureNode(terms[i].id + 1,terms[i].weight);
				}
				ldatanodes[ndocsread].lnodes = lx;
				
				if ( ndocsread++ % 10000 == 0) {
					System.err.println("scanned " + ndocsread);
				}
			}
			dis.close();
		} catch ( IOException e ) {
			return null;
		}
		
		assert( this.ndocs == ndocsread );
		
		Arrays.sort( ldatanodes );
		
		//svm_problem prob = new svm_problem();
		de.bwaldvogel.liblinear.Problem lprob = new de.bwaldvogel.liblinear.Problem();
		/*
		prob.l = datanodes.length;
		prob.x = new svm_node[prob.l][];
		for( int i = 0 ; i < prob.l ; i++ )
			prob.x[i] = datanodes[i].nodes;
		prob.y = new double[prob.l];
		for(int i = 0 ; i < prob.l ; i++ )
			prob.y[i] = (double) datanodes[i].label;
		
		return prob;
		*/
		//add
		System.out.println("max index: -------------------------------------: " + maxIndex);
		lprob.n = maxIndex;
		lprob.l = ldatanodes.length;
		lprob.x = new de.bwaldvogel.liblinear.FeatureNode[lprob.l][];
		for( int i = 0 ; i < lprob.l ; i++ )
			//lprob.x[i] = datanodes[i].nodes;
			lprob.x[i]=ldatanodes[i].lnodes;
		lprob.y = new int[lprob.l];
		for(int i = 0 ; i < lprob.l ; i++ )
			lprob.y[i] = ldatanodes[i].llabel;
		
		return lprob;
	}
	
	/**
	 * 根据特征选择的结果来生成一个用于训练的SVM problem
	 * @param cacheFile 存放训练集的缓存文件
	 * @param selectedFeatures 特征选择的结果
	 * @return 构造好的svm_problem数据结构
	 */

	public String saveToString() {
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  try {
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    oos.writeObject(this.lexicon);
	    //oos.writeObject(this.model);
	    oos.writeObject(this.lmodel);
	    oos.close();
	  } catch (IOException e) {
	    e.printStackTrace();
	    return "";  // Failed to serialize the model.
	  }
	  String base64 = new String(Base64.encodeBase64(baos.toByteArray()));
	  return base64;
	}
	
	public void loadFromString(String model) {
	  ByteArrayInputStream bais = 
	    new ByteArrayInputStream(Base64.decodeBase64(model.getBytes()));
	  ObjectInputStream ois;
    try {
      ois = new ObjectInputStream(bais);
      this.lexicon = (Lexicon) ois.readObject();
      //this.model = (svm_model) ois.readObject();
      this.lmodel = (de.bwaldvogel.liblinear.Model) ois.readObject();
      ois.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    testVectorBuilder = 
      new DocumentVector(lexicon, new TfIdfTermWeighter(lexicon));
	}
}
