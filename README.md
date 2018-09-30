# THUCTC: 一个高效的中文文本分类工具

## 目录
* [项目介绍](#项目介绍)
* [使用方法](#使用方法)
* [样例程序](#样例程序)
* [中文文本分类数据集THUCNews](#中文文本分类数据集THUCNews)
* [测试结果](#测试结果)
* [注意事项](#注意事项)
* [开源协议](#开源协议)
* [相关论文](#相关论文)
* [作者](#作者)

## 项目介绍

THUCTC(THU Chinese Text Classification)是由清华大学自然语言处理实验室推出的中文文本分类工具包，能够自动高效地实现用户自定义的文本分类语料的训练、评测、分类功能。文本分类通常包括特征选取、特征降维、分类模型学习三个步骤。如何选取合适的文本特征并进行降维，是中文文本分类的挑战性问题。我组根据多年在中文文本分类的研究经验，在THUCTC中选取二字串bigram作为特征单元，特征降维方法为Chi-square，权重计算方法为tfidf，分类模型使用的是LibSVM或LibLinear。THUCTC对于开放领域的长文本具有良好的普适性，不依赖于任何中文分词工具的性能，具有准确率高、测试速度快的优点。

## 使用方法

我们提供了两种方式运行工具包：

1. 使用java开发工具，例如eclipse，将包括lib\THUCTC_java_v1.jar在内的lib文件夹下的包导入自己的工程中，仿照Demo.java程序调用函数即可。

2. 使用根目录下的THUCTC_java_v1_run.jar运行工具包。

	使用命令 `java -jar THUCTC_java_v1.jar + 程序参数`

#### 运行参数 

* [-c CATEGORY_LIST_FILE_PATH] 从文件中读入类别信息。该文件中每行包含且仅包含一个类别名称。 
* [-train TRAIN_PATH] 进行训练，并设置训练语料文件夹路径。该文件夹下每个子文件夹的名称都对应一个类别名称，内含属于该类别的训练语料。若不设置，则不进行训练。
* [-test EVAL_PATH] 进行评测，并设置评测语料文件夹路径。该文件夹下每个子文件夹的名称都对应一个类别名称，内含属于该类别的评测语料。若不设置，则不进行评测。也可以使用-eval。
* [-classify FILE_PATH] 对一个文件进行分类。
* [-n topN] 设置返回候选分类数，按得分大小排序。默认为1，即只返回最可能的分类。
* [-svm libsvm or liblinear] 选择使用libsvm还是liblinear进行训练和测试，默认使用liblinear。
* [-l LOAD_MODEL_PATH] 设置读取模型路径。
* [-s SAVE_MODEL_PATH] 设置保存模型路径。
* [-f FEATURE_SIZE] 设置保留特征数目，默认为5000。
* [-d1 RATIO] 设置训练集占总文件数比例，默认为0.8。
* [-d2 RATIO] 设置测试集占总文件数比例，默认为0.2。
* [-e ENCODING] 设置训练及测试文件编码，默认为UTF-8。
* [-filter SUFFIX] 设置文件后缀过滤。例如设置“-filter .txt”，则训练和测试时仅考虑文件名后缀为.txt的文件。

## 样例程序

我们随工具包提供了一个调用THUCTC的样例代码Demo.java，其中实现了三种功能：

1. 对文本进行训练并测试(runTrainAndTest)；
2. 读取已经训练好的模型，对文件进行分类(runLoadModelAndUse)；
3. 按照自己的想法添加训练文件，训练模型(AddFilesManuallyAndTrain)；

###BasicTextClassifier类接口说明

BasicTextClassifier 是系统的入口类,提供多种设置接口供使用者调用。利用此入口类可以从文件中读入别信息、设置训练语料路径、设置训练参数以及模型保存路径等。

其中常用的类成员函数包括：

* `public void Init(String[] args)`

	功能：输入运行参数，初始化系统。

* `public void runAsBigramChineseTextClassifier()`

	功能：根据参数，运行系统。
	
*  `public boolean loadCategoryListFromFile(String filePath)`

	功能：从文件中获取分类列表，等同于参数`-c filePath`
	
*  `public boolean loadCategoryListFromFolder(String folder)`

	功能：从文件夹中获取分类列表
	
*  `public void addTrainingText(String category, String filename)`

	功能：给定类别，添加训练文本
	
*  `public void addfiles(String filename)`
  	
  	功能：根据训练文件所在的文件夹名称,自动判别类别并加入训练，等同于参数`-train filename`
  
*  `public ClassifyResult[] classifyFile(String filepath, int topN)`

	功能：对一个文件进行分类，返回前 topN 个分类结果。如果输入的 filepath 是文件夹,则只会在 Console 中打印每个子文件的分类结果，返回值是空数组，等同于参数`-classify filepath -n topN`

*  `public ClassifyResult[] classifyText(String text, int topN)`

	功能：对一个文本进行分类,返回前 topN 个分类结果

*  `public void testfiles(String filename)`

	功能：对文件进行自动分类测试，等同于参数`-test filename`

*  `public double getPrecision()`

	功能：获得测试准确率


##中文文本分类数据集THUCNews

THUCNews是根据新浪新闻RSS订阅频道2005~2011年间的历史数据筛选过滤生成，包含74万篇新闻文档（2.19 GB），均为UTF-8纯文本格式。我们在原始新浪新闻分类体系的基础上，重新整合划分出14个候选分类类别：财经、彩票、房产、股票、家居、教育、科技、社会、时尚、时政、体育、星座、游戏、娱乐。使用THUCTC工具包在此数据集上进行评测，准确率可以达到88.6%。

数据集请登录[thuctc.thunlp.org](http://thuctc.thunlp.org)网站填写个人信息进行下载。

## 测试结果

文本分类的性能评价有多种指标，其中主流的文本分类评价指标包括准确率、召回率、F-measure、微平均与宏平均等。其中，微平均指所有样本的测试结果的算数平均值，宏平均指所有类别的测试结果的算数平均值。我们的测试也主要对这些指标进行测试。
我们选取上节介绍的数据集进行测试，测试时使用以下参数组合`(-d1 -d2),(-f)`:

* `-d1 0.7 -d2 0.3 -f 5000` 微平均为最优

	|类别|正确率|召回率|F-measure|
	|:----:|----:|----:|----:|
	|体育|0.979|0.990|0.985|
	|娱乐|0.946|0.958|0.952|
	|家具|0.864|0.832|0.848|
	|彩票|0.813|0.757|0.779|
	|房产|0.973|0.972|0.973|
	|教育|0.911|0.879|0.895|
	|时尚|0.746|0.874|0.805|
	|时政|0.780|0.901|0.836|
	|星座|0.816|0.516|0.632|
	|游戏|0.922|0.594|0.707|
	|社会|0.836|0.820|0.828|
	|科技|0.850|0.921|0.884|
	|股票|0.895|0.833|0.863|
	|财经|0.772|0.685|0.726|
	|宏平均|0.861|0.823|0.842|
	|微平均|0.884|||
	

* `-d1 0.8 -d2 0.2 -f 20000` 宏平均为最优
	
	|类别|正确率|召回率|F-measure|
	|:----:|----:|----:|----:|
	|体育|0.979|0.986|0.983|
	|娱乐|0.936|0.966|0.951|
	|家具|0.871|0.883|0.877|
	|彩票|0.967|0.862|0.911|
	|房产|0.957|0.953|0.955|
	|教育|0.887|0.850|0.868|
	|时尚|0.868|0.881|0.875|
	|时政|0.764|0.868|0.813|
	|星座|0.974|0.618|0.756|
	|游戏|0.922|0.536|0.678|
	|社会|0.796|0.802|0.799|
	|科技|0.845|0.882|0.863|
	|股票|0.858|0.854|0.856|
	|财经|0.779|0.656|0.713|
	|宏平均|0.886|0.829|0.856|
	|微平均|0.875|||


## 注意事项

1. 使用工具进行训练和测试时，训练语料和测试语料请严格按照如下格式放置：

	```
	Train(Test)\
		类别1\
			1.txt
			2.txt
			3.txt
			...
			n.txt
		类别2\
			...
		...
		类别n\
			...
	```
		
2. 该工具是通用的中文文本分类工具包，在针对中文文本进行分类时，选取二字串bigram作为特征单元是经过全面的实验分析和比较的。但在针对英文文本进行分类时，我们不保证选取二字串bigram作为特征单元的效果是最优的。
3. 在进行训练模型时，请注意根据自己的语料大小设置相应的使用内存上限。例如语料大小为2GB的时候，至少设置使用内存大小为4GB（-Xmx4096m）。如若程序执行缓慢，请调大使用内存上限。
4. 由于window系统上java使用内存的限制(大约在1GB)，请避免在window系统上使用较大的语料进行训练。


## 开源协议

1. THUCTC面向国内外大学、研究所、企业以及个人研究者免费开放源。
2. 如有机构或个人拟将THUCTC用于商业目的，请发邮件至thunlp@gmail.com洽谈技术许可协议。
3. 欢迎对该工具包的任何宝贵意见和建议，请发邮件至thunlp@gmail.com。
4. 如果您在THUCTC基础上发表论文或取得科研成果，请您在发表论文和申报成果时声明“使用了清华大学THUCTC”，并按如下格式引用：
	
	* **中文：郭志芃,赵宇,郑亚斌,司宪策,刘知远,孙茂松. THUCTC：一个高效的中文文本分类工具包. 2016.**
	
	* **英文: Zhipeng Guo, Yu Zhao, Yabin Zheng, Xiance Si, Zhiyuan Liu, Maosong Sun. THUCTC: An Efficient Chinese Text Classifier. 2016.**
5. 本工具包采用[LibSVM](http://www.csie.ntu.edu.tw/~cjlin/libsvm/index.html)和[Liblinear](https://www.csie.ntu.edu.tw/~cjlin/liblinear/)实现分类算法，特此致谢。该模块遵守[LibSVM](https://www.csie.ntu.edu.tw/~cjlin/libsvm/COPYRIGHT)/[Liblinear](https://www.csie.ntu.edu.tw/~cjlin/liblinear/COPYRIGHT)工具包指定的协议。
   
## 相关论文

* Jingyang Li, Maosong Sun. Scalable Term Selection for Text Categorization. Proc. of the 2007 Joint Conference on Empirical Methods in Natural Language Processing and Computational Natural Language Learning (EMNLP-CoNLL), Prague, Czech Republic, 2007, pp. 774-782.

* Jingyang Li, Maosong Sun, Xian Zhang. A Comparison and Semi-Quantitative Analysis of Words and Character-Bigrams as Features in Chinese Text Categorization. Proc. of the 2006 Joint Conference of the International Committee on Computational Linguistics and the Association for Computational Linguistics (COLING-ACL 2006), Sydney, Australia, 2006, pp. 545-552.

## 作者

指导教师：Maosong Sun（孙茂松教授）

贡献者：Zhipeng Guo（郭志芃），Yu Zhao（赵宇），Yabin Zheng（郑亚斌），Xiance Si（司宪策），Zhiyuan Liu（刘知远）.

使用者如有任何问题、建议和意见，欢迎发邮件至 thunlp@gmail.com 。


	
