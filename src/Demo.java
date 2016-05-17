import org.thunlp.text.classifiers.BasicTextClassifier;
import org.thunlp.text.classifiers.ClassifyResult;
import org.thunlp.text.classifiers.LinearBigramChineseTextClassifier;


public class Demo {

	/**
	 * 如果需要对一批文本进行训练，再进行测试，可以按照本函数的代码调用分类器
	 * 
	 */
	public static void runTrainAndTest() {
		
		// 新建分类器对象
		BasicTextClassifier classifier = new BasicTextClassifier();
		
		// 设置参数
		String defaultArguments = ""
			 + "-train 书库_cleared "  // 设置您的训练路径，这里的路径只是给出样例
			 + "-test 书库_cleared " 
				//	+ "-l C:\\Users\\do\\workspace\\TestJar\\my_novel_model "
				//	+ "-cdir E:\\Corpus\\书库_cleared "
				//	+ "-n 1 "
			// + "-classify E:\\Corpus\\书库_cleared\\言情小说 "  // 设置您的测试路径。一般可以设置为与训练路径相同，即把所有文档放在一起。
			 + "-d1 0.7 "  // 前70%用于训练
			 + "-d2 0.3 "  // 后30%用于测试
			 + "-f 35000 " // 设置保留特征数，可以自行调节以优化性能
			+  "-s ./my_novel_model"  // 将训练好的模型保存在硬盘上，便于以后测试或部署时直接读取模型，无需训练
			 ;
		
		// 初始化
		classifier.Init(defaultArguments.split(" "));
		
		// 运行
		classifier.runAsBigramChineseTextClassifier();
		
	}
	
	
	/**
	 * 如果需要读取已经训练好的模型，再用其进行分类，可以按照本函数的代码调用分类器
	 * 
	 */
	public static void runLoadModelAndUse() {
		// 新建分类器对象
		BasicTextClassifier classifier = new BasicTextClassifier();
		
		// 设置分类种类，并读取模型
		classifier.loadCategoryListFromFile("my_novel_model/category");
		classifier.setTextClassifier(new LinearBigramChineseTextClassifier(classifier.getCategorySize()));
		classifier.getTextClassifier().loadModel("my_novel_model");
		
		/*
		 * 上面三行代码等价于设置如下参数，然后初始化并运行：
		 *
		   String defaultArguments = ""
		 +  "-l  my_novel_model"  // 设置您的训练好的模型的路径，这里的路径只是给出样例
		 ; 
		 classifier.Init(defaultArguments.split(" "));
		 classifier.runAsLinearBigramChineseTextClassifier();
		 * 
		 */
		
		// 之后就可以使用分类器进行分类
		String text = "再次回到世锦赛的赛场上，林丹终于变回了以前的那个超级丹.";
		int topN = 3;  // 保留最有可能的3个结果
		ClassifyResult[] result = classifier.classifyText(text, topN); 
		for (int i = 0; i < topN; ++i) {
			// 输出分类编号，分类名称，以及概率值。
			System.out.println(result[i].label + "\t" + 
								classifier.getCategoryName(result[i].label) + "\t" + 
								result[i].prob); 
		}
	}

	/**
	 * 如果需要按照特殊需求自行添加训练文件，可以按照本函数的代码调用分类器
	 * 
	 */
	public static void AddFilesManuallyAndTrain() {

		// 新建分类器对象
		BasicTextClassifier classifier = new BasicTextClassifier();
		
		// 设置分类种类
		classifier.loadCategoryListFromFile("在此输入您的类型列表文件的路径，例如  /media/disk2/data/novel/category.lst");
		classifier.setTextClassifier(new LinearBigramChineseTextClassifier(classifier.getCategorySize()));

		/*
		 * 上面两行代码等价于设置如下参数，然后初始化：
		 * 
		   String defaultArguments = ""
		 + "-c /media/disk2/data/novel/category_list "  // 设置您的类型列表文件的路径
		 ; 
		 classifier.Init(defaultArguments.split(" "));
		 * 
		 */
		
		// 手动添加文件的方法。每个训练文件按照下面的接口调用一次。
		String filepath = "在此输入您的一个训练文件路径";
		String category = "在此输入训练文件对应的类型名称";
		if (!classifier.addTrainingText(category, filepath)) {
			System.out.println("添加训练文件失败。文件路径为：" + filepath);
			return;
		}
		
		// 训练并保存模型
		classifier.getTextClassifier().train();
		classifier.getTextClassifier().saveModel("在此输入您保存的模型的路径，例如  /media/disk2/data/novel/my_novel_model");
		
	}
	
	public static void main (String args[]) {
		runTrainAndTest();
//		  runLoadModelAndUse();
		// AddFilesManuallyAndTrain();
	}
	
}
