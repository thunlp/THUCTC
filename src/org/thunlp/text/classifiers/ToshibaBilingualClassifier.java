package org.thunlp.text.classifiers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.thunlp.io.TextFileReader;
import org.thunlp.io.TextFileWriter;

public class ToshibaBilingualClassifier extends BasicTextClassifier{

	private TestResult tempResult = new TestResult();
	
	private int classifierType = 4;
	
	@Override
	public void Init(String[] args) {
		String usage = "org.thunlp.text.classifiers.ToshibaBilingualClassifier \n"
			+ "Optional Arguments:  \n"
			
			+ " \t-c [CATEGORY_LIST_FILE] \n" 
			+ " \t\tLoad the name list of all categories from a file, in which each line contains one category name. Notice that category list must be loaded before training, evaluating and classifying. If this argument is not used, the classifier will automatically load the category file in [LOAD_MODEL_PATH], or take all names of directories in [TRAIN_PATH] as category names. If they are still not used, the classifier will claim its need and force quit.\n"

			+ " \t-train [TRAIN_PATH] \n"
			+ " \t\tTrain with documents in [TRAIN_PATH] directory. Each subdirectory in [TRAIN_PATH] should correspond to a category name, and contains training documents that belongs to this category. If [TRAIN_PATH] is not set, training will not be executed. The ratio of documents in used can be set by –d1 [RATIO].\n"
			
			+ " \t-eval [TEST_PATH] \n"
			+ " \t\tPerform evaluation with documents in [EVAL_PATH]. Each subdirectory in [EVAL_PATH] should correspond to a category name, and contains testing documents that belongs to this category. If [EVAL_PATH] is not set, testing will not be executed. The ratio of documents in used can be set by –d2 [RATIO].\n"
			
			+ " \t-classify [FILE_PATH]\n"
			+ " \t\tClassify the document in [FILE_PATH]. The results will be printed to console window. [FILE_PATH] can be a file or a directory\n"
			
			+ " \t-n [topN] \n"
			+ " \t\tReturn [topN] most probable category when classifying; default is 1.\n"
			
			+ " \t-l [LOAD_MODEL_PATH] \n"
			+ " \t\tLoad trained model in the directory of [LOAD_MODEL_PATH]; default is null.\n"
			+ " \t-s [SAVE_MODEL_PATH] \n"
			+ " \t\tSave trained model in the directory of [SAVE_MODEL_PATH]; default is null.\n"
			
			+ " \t-chn \n" 
			+ " \t\tSet the classifier as a Chinese monolingual classifier. Only the documents that have an extension “.chn” are used for both training and testing; default is not set.\n"
			
			+ " \t-eng \n"
			+ " \t\tSet the classifier as an English monolingual classifier. Only the documents that have an extension “.eng” are used for both training and testing; default is not set.\n"

			+ " \t-chn+eng \n"
			+ " \t\tSet the classifier as a Chinese cross-lingual classifier. Only the documents that have an extension “.chn” are used for training, and documents that have an extension “.translated.chn” are used for testing; default is not set.\n"

			+ " \t-eng+chn \n"
			+ " \t\tSet the classifier as an English cross-lingual classifier. Only the documents that have an extension “.eng” are used for training, and documents that have an extension “.translated.eng” are used for testing; default is not set.\n"

			+ " \t-mix \n" 
			+ " \t\tDefault setting. All documents are used for both training and testing, except for “.translated” documents.\n"

			
			+ " \t-f [FEATURE_SIZE] \n"
			+ " \t\tSet the maximum number of selected features; default is 5000\n"
			+ " \t-d1 [RATIO] \n"
			+ " \t\tSet the ratio of used training documents in training folder; default is 0.8\n"
			+ " \t-d2 [RATIO] \n"
			+ " \t\tSet the ratio of used testing documents in testing folder; default is 0.2\n"
			+ " \t\tFor most cases, you can set training folder and testing folder as the same path for convenience, and use -d1 and -d2 to adjust the ratio\n"

			
			+ " \t-e [ENCODING] \n"
			+ " \t\tSet encoding of used documents; default is UTF-8\n"
			;
		if (args.length == 0)
			exit(usage);
		for (int i = 0; i < args.length; i++) {
			if ("-h".equals(args[i]) || "-help".equals(args[i])) {
				exit(usage);
			} else if ("-c".equals(args[i])) {
				if (i + 1 >= args.length || !loadCategoryListFromFile(args[i + 1]))
					exit("loading CATEGORY_LIST_FILE Error!");
				i ++;
			} else if ("-cdir".equals(args[i])) {
				if (i + 1 >= args.length || !loadCategoryListFromFolder(args[i + 1]))
					exit("loading CATEGORY_LIST_FOLDER Error!");
				i ++;
			} else if ("-train".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading TRAIN_PATH Error!");
				trainingFolder = args[i + 1];
				i ++;
			} else if ("-chn".equals(args[i])) {
				classifierType = 0;
			} else if ("-eng".equals(args[i])) {
				classifierType = 1;
			} else if ("-chn+eng".equals(args[i])) {
				classifierType = 2;
			} else if ("-eng+chn".equals(args[i])) {
				classifierType = 3;
			} else if ("-mix".equals(args[i])) {
				classifierType = 4;
			} else if ("-eval".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading EVAL_PATH Error!");
				testingFolder = args[i + 1];
				i ++;
			} else if ("-classify".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading -classify CLASSIFY_PATH Error!");
				classifyPath = args[i + 1];
				if (!new File(loadModelPath).exists())
					exit("Can't find classify FILE_PATH: " + classifyPath);
				i ++;
			} else if ("-n".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading -n topN Error!");
				try {
					resultNum = Integer.parseInt(args[i + 1]);
				} catch (Exception e) {
					exit("-n topN  needs an INTEGER input!");
				}
				i ++;
			} else if ("-l".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading LOAD_MODEL_PATH Error!");
				loadModelPath = args[i + 1];
				i ++;
			} else if ("-s".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading SAVE_MODEL_PATH Error!");
				saveModelPath = args[i + 1];
				i ++;
			} else if ("-f".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading FEATURE_SIZE Error!");
				maxFeatures = Integer.parseInt(args[i + 1]);
				i ++;
			} else if ("-d1".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading RATIO_TRAIN Error!");
				ratio1 = Double.parseDouble(args[i + 1]);
				i ++;
			} else if ("-d2".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading RATIO_TEST Error!");
				ratio2 = Double.parseDouble(args[i + 1]);
				i ++;
			} else if ("-e".equals(args[i])) {
				if (i + 1 >= args.length)
					exit("loading ENCODING Error!");
				encoding = args[i + 1];
				i ++;
			} else {
				exit("Can't find argument: " + args[i] + "\n\n" + usage);
			}
		}
		
		/****************************************************************************************/
		
		// check category size
		if (!loadCategoryListFromFolder(trainingFolder) && loadModelPath != null)
			loadCategoryListFromFile(loadModelPath + File.separator + "category");
		
		if (categoryList.size() == 0 && (testingFolder != null || loadModelPath != null)) {
			exit("Category list NOT LOADED !!! \nUse [-c CATEGORY_LIST_FOLDER] ");
		}

		for (int i = 0; i < categoryList.size(); ++i) {
			categoryToInt.put(categoryList.get(i), i);
		}
		
		if (classifierType == 0 || classifierType == 2)
			setTextClassifier(new BigramChineseTextClassifier(categoryList.size()));
		else if (classifierType == 1 || classifierType == 3)
			setTextClassifier(new BigramEnglishTextClassifier(categoryList.size()));
		else if (classifierType == 4)
			setTextClassifier(new BilingualBigramTextClassifier(categoryList.size()));
		else
			exit("ERROR setting classifierType!");

	}

	public void run() {
		
		AbstractTextClassifier atc = (AbstractTextClassifier) this.getTextClassifier();
		
		String content = "";
		if (loadModelPath != null)
			try {
				content = TextFileReader.readAll(loadModelPath + File.separator + "maxFeatures", encoding);
				maxFeatures = Integer.parseInt(content.trim());
			} catch (FileNotFoundException e1) {
			} catch (IOException e) {
				e.printStackTrace();
			} catch ( NumberFormatException e ) {
				System.err.println("Can't parse content string to integer : " + content);
			}
		if (maxFeatures > 0)
			atc.setMaxFeatures(maxFeatures);
		
		if (trainingFolder != null) {
			if (classifierType == 0 || classifierType == 2) {
				this.suffix = ".chn";
			} else if (classifierType == 1 || classifierType == 3) {
				this.suffix = ".eng";
			} else {
				this.suffix = "NOT.translated";
			}
			addfiles(trainingFolder);
			System.out.println("Training set size: " + index);
			index = 0;
			atc.train();
				
		} else if (loadModelPath != null) {
			if (!atc.loadModel(loadModelPath))
				exit("Error when loading model from " + loadModelPath);
			try {
				TextFileWriter categoryWriter = new TextFileWriter(loadModelPath + File.separator + "category");
				for (int i = 0; i < categoryList.size(); ++i)
					categoryWriter.writeLine(categoryList.get(i));
				categoryWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				exit("Error when loading model " + loadModelPath + " (category file)");
			}
		}
		
		if (saveModelPath != null) {
			if (!new File(saveModelPath).exists() && !new File(saveModelPath).mkdirs())
				exit("SAVE_MODEL_PATH Can't be found or created:  " + saveModelPath);
			if (!classifier.saveModel(saveModelPath))
				exit("Error when saving model in " + saveModelPath);
			TextFileWriter argsWriter;
			try {
				argsWriter = new TextFileWriter(saveModelPath + File.separator + "maxFeatures", encoding);
				argsWriter.write("");
				argsWriter.write(new Integer(maxFeatures).toString());
				argsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				TextFileWriter categoryWriter = new TextFileWriter(saveModelPath + File.separator + "category", encoding);
				for (int i = 0; i < categoryList.size(); ++i)
					categoryWriter.writeLine(categoryList.get(i));
				categoryWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				exit("Error when saving model " + saveModelPath + " (category file)");
			}
		}
		
		if (classifyPath != null) {
			if (categoryList.size() == 0) 
				System.out.println("Category list NOT LOADED. Only output the label id of categories.");
			ClassifyResult[] result = classifyFile(classifyPath, resultNum);
			this.printClassifyResult(result);
		}
		
		if (testingFolder != null) {
			tester = new ToshibaBilingualClassifier.Tester(categoryList.size());
			if (classifierType == 0) {
				this.suffix = ".chn";
			} else if (classifierType == 1) {
				this.suffix = ".eng";
			} else if (classifierType == 2) {
				this.suffix = ".chn.translated";
			} else if (classifierType == 3) {
				this.suffix = ".eng.translated";
			} else if (classifierType == 4) {
				this.suffix = "NOT.translated";
			}
			testfiles(testingFolder);
			tempResult.assign(this.getTester().calculate(categoryList.toArray(new String[categoryList.size()])));
		}
	}
	
	public static void run (int f) {
		
		ToshibaBilingualClassifier classifier = new ToshibaBilingualClassifier();
		
		String defaultArguments = ""
			 + "-mix "
			 + "-train E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-synonyms-combined "
			 + "-d1 0.6 "
			 //+ "-d2 1.0 "
			//"-l E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-models\\chi.monolingual.combined.30000 -c E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify " +
			 + "-test E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-synonyms-combined "
			 + "-f " + f
			 //+  "-s E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-models\\chi.bigram.crosslingual.combined.80000"
		;
		
		classifier.Init(defaultArguments.split(" "));
		
		classifier.run();
	}
	
	public static void main(String[] args) {
		/*int[] features = {10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000};
		TestResult[] results = new TestResult[features.length];
		for (int i = 0; i < features.length; ++i) {
			run(features[i]);
			results[i] = new TestResult(tempResult);
		}
		for (int i = 0; i < features.length; ++i) {
			System.out.println("Feature size: " + features[i] + "\n"
					+ results[i].toString());
		}*/
		ToshibaBilingualClassifier classifier = new ToshibaBilingualClassifier();
		
		//String defaultArguments = ""
			// + "-mix "
			// + "-train E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-synonyms-combined "
			// + "-d1 0.6 "
			 //+ "-d2 1.0 "
			//"-l E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-models\\chi.monolingual.combined.30000 -c E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify " +
			// + "-test E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-synonyms-combined "
			 //+ "-f " + f
			 //+  "-s E:\\Corpus\\toshiba-2ed-year\\dataset\\text-classify-models\\chi.bigram.crosslingual.combined.80000"
		//;
		
		classifier.Init(args);
		
		classifier.run();
		
	}
	
}