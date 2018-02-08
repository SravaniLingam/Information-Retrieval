import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.HashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.BitSet;
import java.util.List;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

public class MainClass {
	private final Map<String, Integer> stemsMap = new HashMap<>();
	private final Map<String, Integer> lemmaMap = new HashMap<>();
	private static Map<String, DocumentProperty> docProperties = new HashMap<>();
	public static Set<String> stopwords12;
	private final Map<String, Properties> stemsDictionary = new HashMap<>();
	private final Map<String, Properties> lemmaDictionary = new HashMap<>();

	private final Set<String> stopwords = new HashSet<>();

	public static StanfordLemmatizer lemmatizer = new StanfordLemmatizer();

	public static void main(final String args[]) throws IOException {

		final long start = System.currentTimeMillis();
		final CommandLine commandLine = validateArguments(args);

		final File folder = new File(commandLine.getOptionValue("path"));
		final File stopwords = new File(commandLine.getOptionValue("stop"));
		final MainClass mc = new MainClass(stopwords);
		mc.parse(folder);

		String fileName = "Index_Version1.uncompressed";
		Map<String, Properties> dictionary = mc.getLemmaDictionary();

		displayIndexResults(fileName, dictionary);

		File file1 = new File("Index_Version1.compressed");
		File file2 = new File("Index_Version1.compressedDictionary");
		long startCompressTime = System.currentTimeMillis();
		blockCompress(dictionary, file1, file2);
		long endcompressedTime = System.currentTimeMillis();
		long elapsedTime = endcompressedTime - startCompressTime;
		displayCompressionResults(elapsedTime, file1, file2);

		fileName = "Index_Version2.uncompressed";
		dictionary = mc.getStemsDictionary();
		displayIndexResults(fileName, dictionary);

		file1 = new File("Index_Version2.compressed");
		file2 = new File("Index_Version2.compressedDictionary");
		startCompressTime = System.currentTimeMillis();
		frontCodingCompress(dictionary, file1, file2);
		endcompressedTime = System.currentTimeMillis();
		elapsedTime = endcompressedTime - startCompressTime;
		displayCompressionResults(elapsedTime, file1, file2);
		System.out
				.println("==========================================================");

		displayTermCharacteristics(mc);

		displayResultforNasa(mc);

		dictionary = mc.getLemmaDictionary();
		System.out
				.println("==========================================================");
		System.out.println("INDEX 1");
		displayPeakTerms(dictionary);

		dictionary = mc.getStemsDictionary();
		System.out.println("INDEX 2");
		displayPeakTerms(dictionary);

		System.out.println("\nDocuments with largest max_term frequency: "
				+ StringUtils.join(getDocsWithLargestMaxTF(), " "));
		System.out.println("Documents with largest doclen: "
				+ StringUtils.join(getDocsWithLargestDoclen(), " "));
		System.out
				.println("==========================================================");
		System.out.println("\nTotal running time: "
				+ (System.currentTimeMillis() - start) + " milliseconds");
	}

	public MainClass(final File file) throws FileNotFoundException, IOException {

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line; (line = reader.readLine()) != null;) {
				this.stopwords.add(line.trim());
			}
		}
	}

	public void parse(final File rootFile) throws IOException {

		for (final File file : rootFile.listFiles()) {

			if (file.isDirectory()) {
				this.parse(file);

			} else {

				this.readFile(file);
			}
		}
	}

	public MainClass(final Set<String> stopwords) {

		MainClass.stopwords12 = stopwords;
	}

	private void readFile(final File file) throws IOException {

		if (file == null || !file.exists() || file.isDirectory()) {
			return;
		}

		final MainClass mc = new MainClass(this.stopwords);
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line; (line = reader.readLine()) != null;) {
				tokenize(file, line, mc);
			}
		}

		this.append(mc, file);
	}

	public void store(final String word, final List<String> lemma,
			final String stem, final File file) {

		final String doc = file.getName().replaceAll("[^\\d]", "");
		if (!MainClass.docProperties.containsKey(doc)) {
			docProperties.put(doc, new DocumentProperty());
		}

		if (!MainClass.stopwords12.contains(word)) {

			int count = this.stemsMap.containsKey(stem) ? this.stemsMap
					.get(stem) : 0;
			this.stemsMap.put(stem, count + 1);

			for (final String string : lemma) {
				count = this.lemmaMap.containsKey(string) ? this.lemmaMap
						.get(string) : 0;
				this.lemmaMap.put(string, count + 1);
			}

			if (MainClass.docProperties.get(doc).getMaxFreq() < count + 1) {
				MainClass.docProperties.get(doc).setMaxFreq(count + 1);
			}
		}

		final int len = MainClass.docProperties.get(doc).getDoclen();
		MainClass.docProperties.get(doc).setDoclen(len + 1);
	}

	public final Map<String, Integer> getStemsMap() {
		return this.stemsMap;
	}

	public final Map<String, Integer> getLemmaMap() {
		return this.lemmaMap;
	}

	public static final Map<String, DocumentProperty> getDocProperties() {
		return MainClass.docProperties;
	}

	public static void frontCodingCompress(
			final Map<String, Properties> dictionary, final File file,
			final File file2) throws FileNotFoundException, IOException {

		final List<String> sortedList = new ArrayList<>();
		sortedList.addAll(dictionary.keySet());
		Collections.sort(sortedList);

		int minLen = Integer.MAX_VALUE;
		for (final String string : sortedList) {
			minLen = Math.min(minLen, string.length());
		}

		int prefixLen = 0;
		boolean breakFlag = false;
		final StringBuilder frontCodeString = new StringBuilder();

		
		try (final RandomAccessFile accessFile = new RandomAccessFile(file,
				"rw"); PrintWriter writer = new PrintWriter(file2)) {

			while (prefixLen < minLen) {
				final char cur = sortedList.get(0).charAt(prefixLen);
				for (int i = 1; i < sortedList.size(); i++) {

					if (!(sortedList.get(i).charAt(prefixLen) == cur)) {
						breakFlag = true;
						break;
					}
				}

				if (breakFlag) {
					break;
				}

				prefixLen++;
			}

			if (prefixLen >= 1) {
				frontCodeString.append(Integer.toString(sortedList.get(0)
						.length())
						+ sortedList.get(0).substring(0, prefixLen)
						+ "*" + sortedList.get(0).substring(prefixLen));
				for (int i = 1; i < sortedList.size(); i++) {
					frontCodeString.append(Integer.toString(sortedList.get(i)
							.length() - prefixLen)
							+ "|" + sortedList.get(i).substring(prefixLen));
				}
			} else {

				for (int i = 0; i < sortedList.size(); i++) {
					frontCodeString.append(Integer.toString(sortedList.get(i)
							.length()) + sortedList.get(i));
				}
			}

			for (final String strng : sortedList) {
				int prv = 0;
				final Map<String, DocumentProperty> tempPostingFile = dictionary
						.get(strng).getPostingFile();
				for (final Entry<String, DocumentProperty> lst : tempPostingFile
						.entrySet()) {
					final byte[] gap = delta(Integer.parseInt(lst.getKey())
							- prv);
					accessFile.write(gap);
					prv = Integer.parseInt(lst.getKey());

					final byte[] term_freq = delta(dictionary.get(strng)
							.getTermFreq().get(lst.getKey()));
					accessFile.write(term_freq);

					final byte[] doclen = delta(lst.getValue().getDoclen());
					accessFile.write(doclen);

					final byte[] maximum = delta(lst.getValue().getMaxFreq());
					accessFile.write(maximum);
				}
			}

			writer.write(frontCodeString.toString());
		}
	}

	public static byte[] delta(final int number)
			throws UnsupportedEncodingException {
		final String unary = Integer.toBinaryString(number);
		final int len = unary.length();
		final String lenUnary = Integer.toBinaryString(len);
		String compressed = new String();
		int i = 1;
		while (i < lenUnary.length()) {
			compressed = compressed + "1";
			i++;
		}

		compressed = compressed + "0" + lenUnary.substring(1);
		compressed = compressed + unary.substring(1);
		final byte[] bytes = StringtoBytes(compressed);
		return bytes;
	}

	public static int min(final int a, final int b, final int c, final int d) {
		return Math.min(a, Math.min(b, Math.min(c, d)));
	}

	public static File write(final Map<String, Properties> dictionary,
			final String fileName) throws FileNotFoundException,
			UnsupportedEncodingException {

		final File file = new File(fileName);
		try (final PrintWriter writer = new PrintWriter(file, "UTF-8")) {
			for (final Entry<String, Properties> entry : dictionary.entrySet()) {
				writer.print(entry.getKey() + ":"
						+ entry.getValue().getDocFreq() + " [");

				final List<String> pairs = new ArrayList<>();
				for (final Entry<String, DocumentProperty> list : entry
						.getValue().getPostingFile().entrySet()) {
					pairs.add(list.getKey() + ":"
							+ entry.getValue().getTermFreq().get(list.getKey())
							+ " " + list.getValue().getMaxFreq() + " "
							+ list.getValue().getDoclen());
				}

				writer.print(StringUtils.join(pairs, " "));
				writer.println("]");
			}
		}

		return file;
	}

	public void append(final MainClass mc, final File file) {
		final String doc = file.getName().replaceAll("[^\\d]", "");
		this.appendToDictionary(this.stemsDictionary, mc.getStemsMap(),
				MainClass.getDocProperties(), doc);
		this.appendToDictionary(this.lemmaDictionary, mc.getLemmaMap(),
				MainClass.getDocProperties(), doc);
	}

	public static List<String> getDocsWithLargestDoclen() {
		int maximum = 0;
		final List<String> documewnts = new ArrayList<>();

		for (final Entry<String, DocumentProperty> entry : MainClass
				.getDocProperties().entrySet()) {

			if (maximum < entry.getValue().getDoclen()) {
				documewnts.clear();
				maximum = entry.getValue().getDoclen();
				documewnts.add("cranfield" + entry.getKey() + " with Doclen = "
						+ maximum);
			} else if (maximum == entry.getValue().getDoclen()) {

				documewnts.add("cranfield" + entry.getKey() + " with Doclen = "
						+ maximum);
			}
		}

		return documewnts;
	}

	private void appendToDictionary(final Map<String, Properties> appendTo,
			final Map<String, Integer> appendFrom,
			final Map<String, DocumentProperty> docProperties, final String file) {
		for (final Entry<String, Integer> entry : appendFrom.entrySet()) {
			Properties temp;
			if (appendTo.containsKey(entry.getKey())) {

				temp = appendTo.get(entry.getKey());
				temp.setDocFreq(temp.getDocFreq() + 1);

				final Map<String, Integer> freq = temp.getTermFreq();
				freq.put(file, entry.getValue());
				temp.setTermFreq(freq);
			} else {

				temp = new Properties();
				temp.setDocFreq(1);
				temp.getTermFreq().put(file, entry.getValue());
			}

			final DocumentProperty property = docProperties.get(file);
			temp.getPostingFile().put(file, property);
			appendTo.put(entry.getKey(), temp);
		}
	}

	public final Map<String, Properties> getStemsDictionary() {
		return this.stemsDictionary;
	}

	public final Map<String, Properties> getLemmaDictionary() {
		return this.lemmaDictionary;
	}

	public static List<String> getTermsWithSmallestDf(
			final Map<String, Properties> dictionary) {
		final List<String> terms = new ArrayList<>();

		int min = Integer.MAX_VALUE;
		for (final Entry<String, Properties> entry : dictionary.entrySet()) {

			if (min > entry.getValue().getDocFreq()) {
				terms.clear();
				min = entry.getValue().getDocFreq();
				terms.add(entry.getKey());
			} else if (min == entry.getValue().getDocFreq()) {

				terms.add(entry.getKey());
			}
		}

		return terms;
	}

	public static ResultFormatter getTermCharacteristics(
			final Set<String> terms, final Map<String, Properties> dictionary)
			throws NumberFormatException, UnsupportedEncodingException {

		final ResultFormatter formatter = new ResultFormatter();
		formatter.addRow("TERM", "TF", "DF", "Size of inverted list");

		for (final String term : terms) {
			if (dictionary.containsKey(term)) {
				final Map<String, DocumentProperty> postingFile = dictionary
						.get(term).getPostingFile();
				int term_freq = 0, lists = 0;
				for (final Entry<String, DocumentProperty> entry : postingFile
						.entrySet()) {

					term_freq += dictionary.get(term).getTermFreq()
							.get(entry.getKey());

					lists += Integer.SIZE / 8 * 4;
				}

				formatter.addRow(term, String.valueOf(term_freq),
						String.valueOf(dictionary.get(term).getDocFreq()),
						lists + " bytes");
			}
		}

		return formatter;
	}

	static void displayPeakTerms(final Map<String, Properties> dictionary) {
		
		List<String> terms = getTermsWithLargestDf(dictionary);

		ResultFormatter formatter = new ResultFormatter();
		formatter.addRow("Term with Largest DF", "DF");
		for (final String string : terms) {
			formatter.addRow(string,
					String.valueOf(dictionary.get(string).getDocFreq()));
		}

		System.out.println(formatter);

		terms = getTermsWithSmallestDf(dictionary);
		formatter = new ResultFormatter();
		formatter.addRow("Term with Smallest DF", "DF");
		for (final String string : terms) {
			formatter.addRow(string,
					String.valueOf(dictionary.get(string).getDocFreq()));
		}

		System.out.println(formatter);
	}

	public static ResultFormatter getFirstThree(final Properties properties) {
		
		List<String> docs = new ArrayList<>();
		docs.addAll(properties.getTermFreq().keySet());
		Collections.sort(docs);
		docs = docs.subList(0, 3);

		
		final ResultFormatter formatter = new ResultFormatter();
		formatter.addRow("DOC-ID", "TF", "MAX_TF", "DOCLEN");
		for (final String string : docs) {
			final int tf = properties.getTermFreq().get(string);
			final int max_tf = properties.getPostingFile().get(string)
					.getMaxFreq();
			final int doclen = properties.getPostingFile().get(string)
					.getDoclen();
			formatter.addRow("cranfield" + string, String.valueOf(tf),
					String.valueOf(max_tf), String.valueOf(doclen));
		}
		return formatter;
	}

	private String transformText(String text) {
	
		text = text.replaceAll("\\<.*?>", " ");

	
		text = text.replaceAll("[\\d+]", "");

		
		text = text.replaceAll("[+^:,?;=%#&~`$!@*_)/(}{\\.]", "");

	
		text = text.replaceAll("\\'s", "");

		text = text.replaceAll("\\'", " ");

		
		text = text.replaceAll("-", " ");

		
		text = text.replaceAll("\\s+", " ");

		text = text.trim().toLowerCase();
		return text;
	}

	public void tokenize(final File file, String line,
			final MainClass mc) {
		
		line = this.transformText(line);

		
		final String[] words = line.split(" ");
		for (final String word : words) {
			
			if (word == null || word.length() < 1) {
				continue;
			}

			
			final String stem = MainClass.stem(word);
			final List<String> lemma = lemmatizer.lemmatize(word);

			mc.store(word, lemma, stem, file);
		}
	}

	private static void displayResultforNasa(final MainClass mc) {
	
		final Properties properties = mc.getLemmaDictionary().get("nasa");
		final int df = properties.getDocFreq();

		
		final ResultFormatter formatter = getFirstThree(properties);

		
		System.out.println("NASA: DF = " + df + "\n");
		System.out.println(formatter);
	}

	private static void displayCompressionResults(final long time,
			final File file, final File file2) throws FileNotFoundException,
			IOException {
		
		final ResultFormatter formatter = new ResultFormatter();
		formatter.addRow(file.getName(),
				String.valueOf(file.length() + file2.length()));
		formatter.addRow("Creation time for " + file.getName(), time + " ms");
		System.out.println(formatter);
	}

	public static List<String> getTermsWithLargestDf(
			final Map<String, Properties> dictionary) {
		final List<String> terms = new ArrayList<>();

		
		int max = 0;
		for (final Entry<String, Properties> entry : dictionary.entrySet()) {
			
			if (max < entry.getValue().getDocFreq()) {
				terms.clear();
				max = entry.getValue().getDocFreq();
				terms.add(entry.getKey());
			} else if (max == entry.getValue().getDocFreq()) {
				
				terms.add(entry.getKey());
			}
		}

		return terms;
	}

	public static List<String> getDocsWithLargestMaxTF() {
		final List<String> docs = new ArrayList<>();

		int max = 0;
		for (final Entry<String, DocumentProperty> entry : MainClass
				.getDocProperties().entrySet()) {

			if (max < entry.getValue().getMaxFreq()) {
				docs.clear();
				max = entry.getValue().getMaxFreq();
				docs.add("cranfield" + entry.getKey() + " with TF = " + max);
			} else if (max == entry.getValue().getMaxFreq()) {

				docs.add("cranfield" + entry.getKey() + " with TF = " + max);
			}
		}

		return docs;
	}

	static void displayTermCharacteristics(final MainClass mc)
			throws NumberFormatException, UnsupportedEncodingException {
		
		final Set<String> terms = new HashSet<>();
		terms.add("reynolds");
		terms.add("nasa");
		terms.add("prandtl");
		terms.add("flow");
		terms.add("pressure");
		terms.add("boundary");
		terms.add("shock");

		final StanfordLemmatizer lemmatizer = MainClass.lemmatizer;
		final Set<String> lemmaSet = new HashSet<>();
		for (final String string : terms) {
			lemmaSet.addAll(lemmatizer.lemmatize(string));
		}

		final Set<String> stemSet = new HashSet<>();
		for (final String string : terms) {
			stemSet.add(stem(string));
		}

		
		
		System.out.println("LEMMATIZATION TOKENS\n");
		ResultFormatter formatter = getTermCharacteristics(lemmaSet,
				mc.getLemmaDictionary());
		System.out.println(formatter);

		
		System.out.println("STEMMING TOKENS\n");
		formatter = getTermCharacteristics(stemSet, mc.getStemsDictionary());
		System.out.println(formatter);
		System.out
				.println("==========================================================");
	}

	public static String stem(final String token) {
		final Stemmer stemmer = new Stemmer();

		final char[] charArray = token.toCharArray();
		for (final char element : charArray) {
			stemmer.add(element);
		}

		stemmer.stem();

		return stemmer.toString();
	}

	public static byte[] gamma(final int number)
			throws UnsupportedEncodingException {
		final String unary = Integer.toBinaryString(number);
		String compressed = new String();
		int i = 1;
		while (i < unary.length()) {
			compressed = compressed + "1";
			i++;
		}

		compressed = compressed + "0" + unary.substring(1);
		final byte[] bytes = StringtoBytes(compressed);
		return bytes;
	}

	public static byte[] StringtoBytes(final String string)
			throws UnsupportedEncodingException {
		final BitSet bitSet = new BitSet(string.length());
		int index = 0;
		while (index < string.length()) {
			if (string.charAt(index) == '1') {
				bitSet.set(index);
			}
			index++;
		}

		final byte[] btob = new byte[(bitSet.length() + 7) / 8];
		int i = 0;
		while (i < bitSet.length()) {
			if (bitSet.get(i)) {
				btob[btob.length - i / 8 - 1] |= 1 << i % 8;
			}

			i++;
		}

		return btob;
	}

	public static void blockCompress(final Map<String, Properties> dictionary,
			final File file, final File file2) throws FileNotFoundException,
			IOException {
		
		try (final RandomAccessFile accessFile = new RandomAccessFile(file,
				"rw"); PrintWriter writer = new PrintWriter(file2)) {
			List<String> words = new ArrayList<String>();
			int count = 0;
			for (final Entry<String, Properties> entry : dictionary.entrySet()) {
				
				if (count == 0) {
					final String compressed = StringUtils.join(words.toArray());
					words = new ArrayList<String>();

					writer.write(compressed + compressed.length());
				}
				if (count < 8) {
					
					words.add(entry.getKey());
					final byte[] df = gamma(entry.getValue().getDocFreq());
					accessFile.write(df);

					int prev = 0;
					final Map<String, DocumentProperty> tempPostingFile = entry
							.getValue().getPostingFile();
					for (final Entry<String, DocumentProperty> list : tempPostingFile
							.entrySet()) {
						final byte[] gap = gamma(Integer
								.parseInt(list.getKey()) - prev);
						accessFile.write(gap);
						prev = Integer.parseInt(list.getKey());

						final byte[] tf = gamma(entry.getValue().getTermFreq()
								.get(list.getKey()));
						accessFile.write(tf);

						final byte[] doclen = gamma(list.getValue().getDoclen());
						accessFile.write(doclen);

						final byte[] max = gamma(list.getValue().getMaxFreq());
						accessFile.write(max);
					}

					count++;
				}
				if (count == 8) {
					
					count = 0;
				}
			}
		}
	}

	private static void displayIndexResults(final String fileName,
			final Map<String, Properties> dictionary)
			throws FileNotFoundException, UnsupportedEncodingException {
		
		final long startTime = System.currentTimeMillis();
		final File index1Uncompressed = write(dictionary, fileName);
		final long endTime = System.currentTimeMillis();

		
		final ResultFormatter formatter = new ResultFormatter();
		formatter.addRow(fileName, index1Uncompressed.length() + " bytes");
		formatter.addRow("Creation time for " + fileName, endTime - startTime
				+ " ms");

		
		formatter.addRow("Number of inverted lists in " + fileName,
				String.valueOf(dictionary.size()));
		System.out.println(formatter);
	}

	private static CommandLine validateArguments(final String[] args) {
		
		final Options options = new Options();
		options.addOption("path", "dataPath", true,
				"Absolute or relative path to the Cranfield database");
		options.addOption("stop", "stopWords", true,
				"Absolute or relative path to the Stop Words file");

		
		final CommandLineParser commandLineParser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = commandLineParser.parse(options, args, false);
		} catch (final ParseException e1) {
			System.out.println("Invalid arguments provided");
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Tokenization", options);
			System.exit(1);
		}

		
		if (!cmd.hasOption("path") || !cmd.hasOption("stop")) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Tokenization", options);
			System.exit(2);
		}

		return cmd;
	}
}
