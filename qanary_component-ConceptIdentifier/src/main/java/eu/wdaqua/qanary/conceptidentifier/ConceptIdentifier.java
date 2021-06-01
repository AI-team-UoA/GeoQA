package eu.wdaqua.qanary.conceptidentifier;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class ConceptIdentifier extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(ConceptIdentifier.class);

	static List<String> allConceptWordUri = new ArrayList<String>();
	static List<String> osmClass = new ArrayList<String>();
	static List<String> commonClasses = new ArrayList<String>();
	static Map<String, String> osmUriMap = new HashMap<String, String>();
	static Map<String, String> DBpediaUrimap = new HashMap<String, String>();

	public static void getCommonClass(Set<String> dbpediaConcepts) {

		for (String lab : osmClass) {

			if (dbpediaConcepts.contains(lab)) {
				if (!commonClasses.contains(lab))
					commonClasses.add(lab);
			}
		}

	}

	public static void getXML(String fname) {
		try {
			File fXmlFile = new File(fname);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("owl:Class");
			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				String uri, cEntity;

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					uri = eElement.getAttribute("rdf:about");
					osmUriMap.put(uri.substring(uri.indexOf('#') + 1), uri);
					uri = uri.substring(uri.indexOf('#') + 1);
					osmClass.add(uri);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static List<String> getNouns(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> postags = new ArrayList<>();
		String lemmetizedQuestion = "";
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("NN")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
	}

	public static String lemmatize(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> lemmas = new ArrayList<>();
		String lemmetizedQuestion = "";
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				lemmas.add(token.get(LemmaAnnotation.class));
				lemmetizedQuestion += token.get(LemmaAnnotation.class) + " ";
			}
		}
		return lemmetizedQuestion;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 *
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// Map<String, String> allMapConceptWord =
		// DBpediaConceptsAndURIs.getDBpediaConceptsAndURIs();
		Map<String, ArrayList<String>> allMapConceptWord = YagoConceptsAndURIs.getYagoConceptsAndURIs();
		getXML("/src/main/resources/osm.owl");
		// getCommonClass(allMapConceptWord.keySet());
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		List<Concept> mappedConcepts = new ArrayList<Concept>();
		List<Concept> DBpediaConcepts = new ArrayList<Concept>();
		List<Concept> osmConcepts = new ArrayList<Concept>();
		List<String> allNouns = getNouns(myQanaryQuestion.getTextualRepresentation());
		// question string is required as input for the service call
//		osmUriMap.put("District", "http://www.app-lab.eu/gadm/District");
//		osmUriMap.put("County", "http://www.app-lab.eu/gadm/County");
//		osmUriMap.put("Administrative County", "http://www.app-lab.eu/gadm/AdministrativeCounty");
//		osmUriMap.put("London Borough", "http://www.app-lab.eu/gadm/LondonBorough");
//		osmUriMap.put("MetropolitanCounty", "http://www.app-lab.eu/gadm/MetropolitanCounty");
//		osmUriMap.put("Country", "http://www.app-lab.eu/gadm/HomeNation|ConstituentCountry");
//		osmUriMap.put("Province", "http://www.app-lab.eu/gadm/Province");
//		osmUriMap.put("Unitary District", "http://www.app-lab.eu/gadm/UnitaryDistrict");
//		osmUriMap.put("Administrative Unit", "http://www.app-lab.eu/gadm/AdministrativeUnit");
//		osmUriMap.put("Metropolitan Borough", "http://www.app-lab.eu/gadm/MetropolitanBorough");
		//-----------------------------------------------------------------------------------------
		osmUriMap.put("Civil Parishor Community", "http://kr.di.uoa.gr/yago2geo/ontology/OS_CivilParishorCommunity");
		osmUriMap.put("Unitary Authority Ward", "http://kr.di.uoa.gr/yago2geo/ontology/OS_UnitaryAuthorityWard");
		osmUriMap.put("District Ward", "http://kr.di.uoa.gr/yago2geo/ontology/OS_DistrictWard");
		osmUriMap.put("District", "http://kr.di.uoa.gr/yago2geo/ontology/OS_District");
		osmUriMap.put("County", "http://kr.di.uoa.gr/yago2geo/ontology/OS_County");
		osmUriMap.put("Metropolitan District Ward", "http://kr.di.uoa.gr/yago2geo/ontology/OS_MetropolitanDistrictWard");
		osmUriMap.put("Unitary Authority", "http://kr.di.uoa.gr/yago2geo/ontology/OS_UnitaryAuthority");
		osmUriMap.put("London Borough", "http://kr.di.uoa.gr/yago2geo/ontology/OS_LondonBorough");
		osmUriMap.put("London Borough Ward", "http://kr.di.uoa.gr/yago2geo/ontology/OS_LondonBoroughWard");
		osmUriMap.put("Metropolitan District", "http://kr.di.uoa.gr/yago2geo/ontology/OS_MetropolitanDistrict");
		osmUriMap.put("GreaterLondon Authority", "http://kr.di.uoa.gr/yago2geo/ontology/OS_GreaterLondonAuthority");
		osmUriMap.put("European Region", "http://kr.di.uoa.gr/yago2geo/ontology/OS_EuropeanRegion");
		osmUriMap.put("Community Ward", "http://kr.di.uoa.gr/yago2geo/ontology/OS_COMMUNITYWARD");
		osmUriMap.put("City Community Ward", "http://kr.di.uoa.gr/yago2geo/ontology/OS_CCOMMUNITYWARD");


		/*
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_CivilParishorCommunity
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_UnitaryAuthorityWard
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_DistrictWard
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_District
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_County
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_MetropolitanDistrictWard
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_UnitaryAuthority
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_LondonBorough
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_LondonBoroughWard
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_MetropolitanDistrict
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_GreaterLondonAuthority
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_EuropeanRegion
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_COMMUNITYWARD
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_COMMUNITY
		 * http://kr.di.uoa.gr/yago2geo/ontology/OS_CCOMMUNITY
		 * http://kr.di.uoa.gr/yago2geo/ontology/GAG_DecentralizedAdministration
		 * http://kr.di.uoa.gr/yago2geo/ontology/GAG_Municipality
		 * http://kr.di.uoa.gr/yago2geo/ontology/GAG_MunicipalUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GAG_Region
		 * http://kr.di.uoa.gr/yago2geo/ontology/GAG_RegionalUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GAG_MunicipalCommunity
		 * http://kr.di.uoa.gr/yago2geo/ontology/GADM_1stOrder_AdministrativeUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GADM_2ndOrder_AdministrativeUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GADM_3rdOrder_AdministrativeUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GADM_4thOrder_AdministrativeUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GADM_5thOrder_AdministrativeUnit
		 * http://kr.di.uoa.gr/yago2geo/ontology/GADM_6thOrder_AdministrativeUnit
		 */
		String myQuestion = lemmatize(myQanaryQuestion.getTextualRepresentation());

		String myQuestionNl = myQanaryQuestion.getTextualRepresentation();

		logger.info("Lemmatize Question: {}", myQuestion);
		logger.info("store data in graph {}",
				myQanaryMessage.getValues().get(new URL(myQanaryMessage.getEndpoint().toString())));
		WordNetAnalyzer wordNet = new WordNetAnalyzer("src/main/resources/WordNet-3.0/dict");
		osmUriMap.remove("county");

		for (String conceptLabel : allMapConceptWord.keySet()) {
			// logger.info("The word: {} question : {}", conceptLabel,
			// myQuestion);

			ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(conceptLabel);
			for (String synonym : wordNetSynonyms) {
				for (String nounWord : allNouns) {
					Pattern p = Pattern.compile("\\b" + synonym + "\\b", Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(nounWord);
					if (m.find()) {
						for (String s : allMapConceptWord.get(conceptLabel.replaceAll(" ", "_"))) {
							Concept concept = new Concept();
							concept.setBegin(myQuestion.toLowerCase().indexOf(synonym.toLowerCase()));
							concept.setEnd(myQuestion.toLowerCase().indexOf(synonym.toLowerCase()) + synonym.length());
							concept.setURI(s);
							mappedConcepts.add(concept);
							System.out.println(
									"Identified Concepts: dbo:" + conceptLabel + " ============================"
											+ "Synonym inside question is: " + synonym + " ===================");
							logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
									conceptLabel);
						}
						// TODO: remove break and collect all appearances of
						// concepts
						// TODO: implement test case "City nearby Forest
						// nearby
						// River"
						break;
					}
				}
			}
		}

		for (String conceptLabel : osmUriMap.keySet()) {
			// logger.info("The word: {} question : {}", conceptLabel,
			// myQuestion);

			ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(conceptLabel);
			for (String synonym : wordNetSynonyms) {
				for (String nounWord : allNouns) {
					Pattern p = Pattern.compile("\\b" + synonym + "\\b", Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(nounWord);
					if (m.find()) {
						Concept concept = new Concept();
						concept.setBegin(myQuestion.toLowerCase().indexOf(synonym.toLowerCase()));
						concept.setEnd(myQuestion.toLowerCase().indexOf(synonym.toLowerCase()) + synonym.length());
						concept.setURI(osmUriMap.get(conceptLabel.replaceAll(" ", "_")));
						mappedConcepts.add(concept);
						System.out.println("Identified Concepts: osm:" + conceptLabel + " ============================"
								+ "Synonym inside question is: " + synonym + " ===================");
						logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
								conceptLabel);
						// TODO: remove break and collect all appearances of
						// concepts
						// TODO: implement test case "City nearby Forest
						// nearby
						// River"
						break;
					}
				}
			}
		}

		// Find class from DBpedia
//		for (String conceptLabel : allMapConceptWord.keySet()) {
//			// logger.info("The word: {} question : {}", conceptLabel,
//			// myQuestion);
//
//			ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(conceptLabel);
//			for (String synonym : wordNetSynonyms) {
//				for (String nounWord : allNouns) {
//					Pattern p = Pattern.compile("\\b" + synonym + "\\b", Pattern.CASE_INSENSITIVE);
//					Matcher m = p.matcher(nounWord);
//					if (m.find()) {
//						Concept concept = new Concept();
//						concept.setBegin(myQuestion.toLowerCase().indexOf(synonym.toLowerCase()));
//						concept.setEnd(myQuestion.toLowerCase().indexOf(synonym.toLowerCase()) + synonym.length());
//						concept.setURI(allMapConceptWord.get(conceptLabel.replaceAll(" ", "_")));
//						mappedConcepts.add(concept);
//						System.out.println(
//								"Identified Concepts: dbo:" + conceptLabel + " ============================"
//										+ "Synonym inside question is: " + synonym + " ===================");
//						logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
//								conceptLabel);
//						// TODO: remove break and collect all appearances of
//						// concepts
//						// TODO: implement test case "City nearby Forest
//						// nearby
//						// River"
//						break;
//					}
//				}
//			}
//		}
		// for (String conceptLabel : allMapConceptWord.keySet()) {
		// // logger.info("The word: {} question : {}", conceptLabel,
		// // myQuestion);
		// Pattern p = Pattern.compile("\\b" + conceptLabel + "\\b",
		// Pattern.CASE_INSENSITIVE);
		// Matcher m = p.matcher(myQuestion);
		// if (m.find()) {
		// Concept concept = new Concept();
		// concept.setBegin(myQuestion.toLowerCase().indexOf(conceptLabel.toLowerCase()));
		// concept.setEnd(
		// myQuestion.toLowerCase().indexOf(conceptLabel.toLowerCase()) +
		// conceptLabel.length());
		// concept.setURI(allMapConceptWord.get(conceptLabel.replaceAll(" ",
		// "_")));
		// mappedConcepts.add(concept);
		//
		// logger.info("identified concept: concept={} : {} : {}",
		// concept.toString(), myQuestion,
		// conceptLabel);
		// // TODO: remove break and collect all appearances of
		// // concepts
		// // TODO: implement test case "City nearby Forest nearby
		// // River"
		// // break;
		// }
		// }

		// System.out.println("The Identified Concepts: ");
		// for (Concept concept : mappedConcepts) {
		// System.out.println("=========");
		// System.out.println(concept.getURI());
		// System.out.println("=========");
		// }
		/////////////////////////////////////////////////////////////////
		// Step 4: now push the results, i.e., identified concept and its
		// associated URI back to triplestore
		/////////////////////////////////////////////////////////////////


		ArrayList<Concept> removalList = new ArrayList<Concept>();
//		for (int i = 0; i < mappedConcepts.size(); i++) {
//			Concept tempConcept = mappedConcepts.get(i);
//			String conUri = tempConcept.getURI();
//			if (conUri.contains("Park")) {
//				if (myQuestionNl.contains(" car ")) {
//					removalList.add(mappedConcepts.get(i));
//				}
//			}
//			System.out.println("Concept: " + conUri);
//		}
		for (Concept tempConcept : mappedConcepts) {
			String conUri = tempConcept.getURI();
			if (conUri != null) {
				if (conUri.contains("Parking")) {
					System.out.println("Getting in parking with question : " + myQuestionNl);
					if (!myQuestionNl.contains(" car ")) {
						System.out.println("getting in car parking :" + myQuestion);
						removalList.add(tempConcept);
					}
				}
				if (conUri.contains("Gondola") || conUri.contains("http://dbpedia.org/ontology/List")
						|| conUri.contains("http://dbpedia.org/ontology/Automobile")
						|| conUri.contains("http://dbpedia.org/ontology/Altitude")
						|| conUri.contains("http://dbpedia.org/ontology/Name")
						|| conUri.contains("http://dbpedia.org/ontology/Population")
						|| (conUri.contains("http://www.app-lab.eu/osm/ontology#Peak")
						&& myQuestion.toLowerCase().contains("height")) ) {
					removalList.add(tempConcept);
				}
			}
//			System.out.println("Concept: " + conUri);
		}
//
		for (Concept removalC : removalList) {
			mappedConcepts.remove(removalC);
		}

		for (Concept mappedConcept : mappedConcepts) {
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

}
