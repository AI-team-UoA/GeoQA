package eu.wdaqua.qanary.conceptidentifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import eu.wdaqua.qanary.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * This component identifies concepts (types of features) in the question and maps them to the corresponding classes of the
 * YAGO2geo ontology.
 */
@Component
public class ConceptIdentifier extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(ConceptIdentifier.class);

	private final WordNetAnalyzer wordNet = new WordNetAnalyzer("qanary_component-ConceptIdentifierYago/src/main/resources/WordNet-3.0/dict");
	private final JaroWrinklerSimilarity jwSimilarity = new JaroWrinklerSimilarity();
	private final BertEmbeddingSimilarity bertSimilarity = new BertEmbeddingSimilarity();

	static Map<String, String> yago2ClassesMap = new HashMap<>();
	static Map<String, String> yago2geoClassesMap = new HashMap<>();
	static {
		loadListOfClasses("qanary_component-ConceptIdentifierYago/src/main/resources/YAGOClasses.txt", yago2ClassesMap);
		loadListOfClasses("qanary_component-ConceptIdentifierYago/src/main/resources/YAGO2geoClasses.txt", yago2geoClassesMap);
	}

	/**
	 * Load the classes of an ontology and their labels from a file.
	 *
	 * @param fname The path of a file that contains a list of classes used by some graph.
	 * @param targetMap The data structure that will hold the classes contained in the file.
	 */
	static void loadListOfClasses(String fname, Map<String, String> targetMap){
		try (BufferedReader br = new BufferedReader(new FileReader(fname))) {
			String line;
			while((line = br.readLine()) != null){
				String[] splitLine = line.split(",");
				targetMap.put(splitLine[0].trim(),splitLine[1].trim());
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Create all word n-grams of size n for the given string.
	 *
	 * @param n The size, in words, of each subsequence (n-gram).
	 * @param str The String to separate into n-grams.
	 * @return All subsequences of length n.
	 */
	static ArrayList<String> createNGrams(int n, String str) {
		var ngrams = new ArrayList<String>();
		var words = str.split(" ");
		for (int i = 0; i < words.length - n + 1; i++)
			ngrams.add(concat(words, i, i+n));
		return ngrams;
	}

	static String concat(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append(i > start ? " " : "").append(words[i]);
		return sb.toString();
	}

	static int wordcount(String string)	{
		return new StringTokenizer(string).countTokens();
	}

	/**
	 * Method encapsulating the functionality of the ConceptIdentifier.
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = CoreNLPUtilities.lemmatize(myQanaryQuestion.getTextualRepresentation());

		// YAGO2geo Concepts
		var yago2geoMappedConcepts = mapConcepts(myQuestion, yago2geoClassesMap);

		// YAGO2 Concepts
		var yago2MappedConcepts = mapConcepts(myQuestion, yago2ClassesMap);

		// Join YAGO2 & YAGO2geo mapped concepts
//		yago2geoMappedConcepts.addAll(yago2MappedConcepts); // disable to ignore YAGO2 Concepts

		// Store data (mapped Concepts) to the triplestore
		for (Concept mappedConcept : yago2geoMappedConcepts) {
			// insert data in QanaryMessage.outgraph
			logger.info("apply vocabulary alignment on outgraph: {}", myQanaryQuestion.getOutGraph());
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfConcepts . " //
					+ "  ?a oa:hasTarget [ " //
					+ "           a oa:SpecificResource; " //
					+ "             oa:hasSource    ?source; " //
					+ "             oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + mappedConcept.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end   \"" + mappedConcept.getEnd() + "\"^^xsd:nonNegativeInteger  " //
					+ "             ] " //
					+ "  ] . " //
					+ "  ?a oa:hasBody ?mappedConceptURI;" //
					+ "     oa:annotatedBy qa:ConceptIdentifier; " //
					+ "}} " //
					+ "WHERE { " //
					+ "  BIND (IRI(str(RAND())) AS ?a) ."//
					+ "  BIND (now() AS ?time) ." //
					+ "  BIND (<" + mappedConcept.getURI() + "> AS ?mappedConceptURI) ." //
					+ "  BIND (<" + myQanaryQuestion.getUri() + "> AS ?source  ) ." //
					+ "}";
			logger.debug("Sparql query to add concepts to Qanary triplestore: {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
		}

		return myQanaryMessage;
	}

	/**
	 * @param candidateStr The matching candidate.
	 * @param targetStr The target concept or a synonym of the concept that we want to match.
	 * @param conceptLabel The label of the concept, as known by GeoQA.
	 * @param similarity The Similarity object to use for matching.
	 * @param threshold Similarity threshold.
	 * @param mappedConcepts A data structure used to hold matched Concepts.
	 * @param myQuestion The lemmatized version of the question asked.
	 * @return True if the match was successful, i.e., if the candidate was mapped to a Concept.
	 */
	boolean mapIfSimilar(String candidateStr, String targetStr, String conceptLabel, Similarity similarity, double threshold, List<Concept> mappedConcepts, String myQuestion) {
		double similarityScore = similarity.computeSimilarity(candidateStr.toLowerCase(Locale.ROOT), targetStr.toLowerCase(Locale.ROOT));
		System.out.println("got similarity for candidate : " + candidateStr + "\t and concept label : " +targetStr + "\t is = "+similarityScore);
		if(similarityScore > threshold) {
			Concept concept = new Concept();
			int begin = myQuestion.toLowerCase().indexOf(candidateStr.toLowerCase());
			concept.setBegin(begin);
			concept.setEnd(begin + candidateStr.length());
			concept.setURI(yago2geoClassesMap.get(conceptLabel));
			mappedConcepts.add(concept);
			System.out.println("Identified Concepts: yago2geo:" + conceptLabel + " ============================"
					+ "candidate inside question is: " + candidateStr + " ===================");
			logger.info("identified concept: concept={} : {} : {}", concept, myQuestion, concept.getURI());
			return true;
		}
		return false;
	}

	/**
	 * @param question The input question.
	 * @param classesMap A map of Labels-Classes that the ConceptIdentifier will look for.
	 * @return A list of identified Concepts.
	 */
	List<Concept> mapConcepts(String question, Map<String, String> classesMap) {
		var mappedConcepts = new ArrayList<Concept>();

		MAP_CONCEPTS_LOOP:
		for (String conceptLabel : classesMap.keySet()) {
			List<String> ngrams = createNGrams(wordcount(conceptLabel), question);

			// use string similarity (Concept label compared to word-ngrams)
			for (String ngram : ngrams) {
				if (mapIfSimilar(ngram, conceptLabel, conceptLabel, jwSimilarity, 0.99, mappedConcepts, question))
					continue MAP_CONCEPTS_LOOP;
			}

			// use bert embedding cosine similarity
			for (String ngram : ngrams) {
				if (mapIfSimilar(ngram, conceptLabel, conceptLabel, bertSimilarity, 0.95, mappedConcepts, question))
					continue MAP_CONCEPTS_LOOP;
			}

			/*
			 * Use synonyms to identify Concepts.
			 */
			ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(conceptLabel);

			// use pattern matching
			for (String synonym : wordNetSynonyms) {
				List<String> ngramsSynonym = createNGrams(wordcount(conceptLabel), question);

				// use string similarity (Concept label compared to word-ngrams)
				for (String ngram : ngramsSynonym) {
					if (mapIfSimilar(ngram, synonym, conceptLabel, jwSimilarity, 0.99, mappedConcepts, question))
						continue MAP_CONCEPTS_LOOP;
				}
			}

			// use bert embedding cosine similarity
			for (String synonym : wordNetSynonyms) {
				List<String> ngramsSynonym = createNGrams(wordcount(conceptLabel), question);

				// use string similarity (Concept label compared to word-ngrams)
				for (String ngram : ngramsSynonym) {
					if (mapIfSimilar(ngram, synonym, conceptLabel, bertSimilarity, 0.95, mappedConcepts, question))
						continue MAP_CONCEPTS_LOOP;
				}
			}
		}

		return mappedConcepts;
	}
}
