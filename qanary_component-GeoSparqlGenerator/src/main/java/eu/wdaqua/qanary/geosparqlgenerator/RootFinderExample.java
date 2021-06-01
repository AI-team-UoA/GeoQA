package eu.wdaqua.qanary.geosparqlgenerator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class RootFinderExample {
	private StringBuilder out;
	private Set<IndexedWord> used;

//	private void formatSGNode(SemanticGraph sg, IndexedWord node, int spaces) {
//		used.add(node);
//		String oneline = formatSGNodeOneline(sg, node);
//		boolean toolong = (spaces + oneline.length() > width);
//		boolean breakable = sg.hasChildren(node);
//		if (toolong && breakable) {
//			formatSGNodeMultiline(sg, node, spaces);
//		} else {
//			out.append(oneline);
//		}
//	}

//	public String formatSemanticGraph(SemanticGraph sg) {
//		if (sg.vertexSet().isEmpty()) {
//			return "[]";
//		}
//		out = new StringBuilder(); // not thread-safe!!!
//		used = Generics.newHashSet();
//		if (sg.getRoots().size() == 1) {
////			formatSGNode(sg, sg.getFirstRoot(), 1);
//		} else {
//			int index = 0;
//			for (IndexedWord root : sg.getRoots()) {
//				index += 1;
//				out.append("root_").append(index).append("> ");
//				formatSGNode(sg, root, 9);
//				out.append("\n");
//			}
//		}
//		String result = out.toString();
//		if (!result.startsWith("[")) {
//			result = "[" + result + "]";
//		}
//		return result;
//	}

//	public static void traversalPrimitive(IndexedWord node) {
//		for(IndexedWord childNode: node.)
//	}
public static boolean isJJSClosestOrNearest(String documentText) {
	boolean retVal = false;
	Properties props = new Properties();
	props.put("annotators", "tokenize, ssplit, pos,lemma");
	StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	// Create an empty Annotation just with the given text
	Annotation document = new Annotation(documentText);
	// run all Annotators on this text
	pipeline.annotate(document);
	// Iterate over all of the sentences found
	List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	for (CoreMap sentence : sentences) {
		// Iterate over all tokens in a sentence
		for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
			String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//			System.out.println("token : "+token.originalText());
			if (pos.contains("JJS")) {
				if(token.originalText().equalsIgnoreCase("nearest")||token.originalText().equalsIgnoreCase("closest")) {
					retVal = true;

				}
			}
		}
	}
	return retVal;
}
	public static boolean isJJSNN(String documentText) {
		boolean retVal = false;
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			dependencies.prettyPrint();
			List<SemanticGraphEdge>	edges = dependencies.edgeListSorted();

			for(SemanticGraphEdge edge: edges){


				if(edge.getSource().toString().contains("JJS") && edge.getDependent().toString().contains("NN")){
					System.out.println(" Source ================================================= Dest ");
					System.out.println("edge : "+edge.toString());
					System.out.println("source: "+edge.getSource());
					System.out.println("relation: "+edge.getRelation());
					System.out.println("dependent :"+edge.getDependent());
				}
				else if (edge.getSource().toString().contains("NN") && edge.getDependent().toString().contains("JJS")){
					System.out.println("Dest ================================================= Source");
					System.out.println("edge : "+edge.toString());
					System.out.println("source: "+edge.getSource());
					System.out.println("relation: "+edge.getRelation());
					System.out.println("dependent :"+edge.getDependent());
				}
			}
		}
		return retVal;
	}
	public static void main(String[] args) throws IOException {


		String question = "What is the longest bridge in Scotland?";
		System.out.println("Does question ask nearest/closest ? : "+isJJSNN(question));

		/*MyGraph myGraph = new MyGraph();

		// build pipeline
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		BufferedReader br = new BufferedReader(new FileReader("/home/dharmenp/somequestions.txt"));
		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmenp/somequestionsWithDependencies.csv"));
		String line = "";
		ArrayList<String> questionList = new ArrayList<String>();
		while((line = br.readLine())!=null){
			questionList.add(line);
		}
		br.close();
		String text = "Which pubs in Dublin are near Guinness Brewery?";
		for(String question:questionList){
			Annotation annotation = new Annotation(question);
			pipeline.annotate(annotation);
			List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
			bw.write(question+",");
			for (CoreMap sentence : sentences) {
				SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
				dependencies.prettyPrint();
				List<SemanticGraphEdge>	edges = dependencies.edgeListSorted();

				for(SemanticGraphEdge edge: edges){
//					System.out.println("edge : "+edge.toString());
//					System.out.println("source: "+edge.getSource());
//					System.out.println("relation: "+edge.getRelation());
//					System.out.println("dependent :"+edge.getDependent());

					bw.write(edge.toString()+",");
				}
			}
			bw.newLine();
		}
		bw.close();
*/


		System.out.println("finished");
	}

}