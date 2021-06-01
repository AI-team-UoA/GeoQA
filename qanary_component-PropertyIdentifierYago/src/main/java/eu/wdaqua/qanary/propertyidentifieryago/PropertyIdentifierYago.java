package eu.wdaqua.qanary.propertyidentifieryago;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
//import net.ricecode.similarity.JaroWinklerStrategy;
//import net.ricecode.similarity.SimilarityStrategy;
//import net.ricecode.similarity.StringSimilarityService;
//import net.ricecode.similarity.StringSimilarityServiceImpl;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class PropertyIdentifierYago extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(PropertyIdentifierYago.class);

	public static List<String> getVerbsNouns(String documentText) {
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
				if (pos.contains("VB") || pos.contains("IN") || pos.contains("NN") || pos.contains("JJ")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
	}

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
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
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

	public static boolean isAlphaNumeric(String s) {
		return s != null && s.matches("^[a-zA-Z0-9]*$");
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		logger.info("Question: {}", myQuestion);
		// TODO: implement processing of question

		List<String> allVerbs = getVerbsNouns(myQuestion);
		List<String> relationList = new ArrayList<String>();
		List<Property> properties = new ArrayList<Property>();
		List<String> valuePropertyList = new ArrayList<String>();
		boolean valueFlag = false;
		Set<String> coonceptsUri = new HashSet<String>();
		ResultSet r;

		Set<Concept> concepts = new HashSet<Concept>();
		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
				+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?start ?end ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "    ?a a qa:AnnotationOfConcepts . " + "?a oa:hasTarget [ "
				+ "		     a               oa:SpecificResource; " //
				+ "		     oa:hasSource    ?q; " //
				+ "	         oa:hasSelector  [ " //
				+ "			         a        oa:TextPositionSelector ; " //
				+ "			         oa:start ?start ; " //
				+ "			         oa:end   ?end " //
				+ "		     ] " //
				+ "    ] . " //
				+ " ?a oa:hasBody ?uri ; " + "    oa:annotatedBy ?annotator " //
				+ "} " + "ORDER BY ?start ";

		r = myQanaryUtils.selectFromTripleStore(sparql);
		while (r.hasNext()) {
			QuerySolution s = r.next();

			Concept conceptTemp = new Concept();
			conceptTemp.begin = s.getLiteral("start").getInt();

			conceptTemp.end = s.getLiteral("end").getInt();

			conceptTemp.link = s.getResource("uri").getURI();

			// geoSparqlQuery += "" + conceptTemplate.replace("poiURI",
			// conceptTemp.link).replaceAll("poi",
			// "poi" + conceptTemp.begin);
			// newGeoSparqlQuery += "" + conceptTemplate.replace("poiURI",
			// conceptTemp.link).replaceAll("poi",
			// "poi" + conceptTemp.begin);
			if (conceptTemp.link.contains("yago-knowledge.org")) {
				concepts.add(conceptTemp);
				coonceptsUri.add(conceptTemp.link);
				logger.info("Concept start {}, end {} concept {} link {}", conceptTemp.begin, conceptTemp.end,
						myQuestion.substring(conceptTemp.begin, conceptTemp.end), conceptTemp.link);
			}

		}
		// for (int i = 0; i < concepts.size(); i++) {
		// myQuestion = myQuestion
		// .replace(coonceptsUri.get(i).substring(coonceptsUri.get(i).lastIndexOf("#")
		// + 1).toLowerCase(), "");
		// System.out.println("myQuestion: " + myQuestion);
		// System.out.println("The class labels: "
		// + coonceptsUri.get(i).substring(coonceptsUri.get(i).lastIndexOf("#")
		// + 1).toLowerCase());
		// }
		System.out.println("all verbs: " + allVerbs);
		for (String concept : coonceptsUri) {

			String classLabel = concept.substring(concept.lastIndexOf("/") + 1);
			File file = new File(
					"src/main/resources/yagoproperties/" + classLabel + "_yago_label_of_properties.txt");
			File file1 = new File(
					"src/main/resources/yagoproperties/" + classLabel + "_yago_value_of_properties.txt");
			Map<String, String> valuePropertyD = new HashMap<String, String>();
			Map<String, String> labelPropertyD = new HashMap<String, String>();
			if (file.exists()) {
				System.out.println("opening file : " + file.getName());
				BufferedReader br = new BufferedReader(new FileReader(file));

				String line = "";
				while ((line = br.readLine()) != null) {
					String splitedLine[] = line.split(",");
					if (splitedLine.length > 1) {
						labelPropertyD.put(splitedLine[0], splitedLine[1]);
					}
				}
				br.close();

				BufferedReader br1 = new BufferedReader(new FileReader(file1));

				line = "";
				while ((line = br1.readLine()) != null) {
					String splitedLine[] = line.split(",");
					if (splitedLine.length > 1) {
						valuePropertyD.put(splitedLine[1], splitedLine[0]);
					}
				}
				br1.close();

				System.out.println("size is : " + valuePropertyD.size());
				if (valuePropertyD.size() > 0) {
					double score = 0.0;
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
					for (Entry<String, String> enrty : valuePropertyD.entrySet()) {
						for (String verb : allVerbs) {
							if (verb.contains(classLabel) || enrty.getKey().length() > 10
									|| enrty.getKey().contains("(") || enrty.getKey().contains(")"))
								continue;

							Pattern p = Pattern.compile("\\b" + enrty.getKey() + "\\b", Pattern.CASE_INSENSITIVE);
//						System.out.println("Keyword: "+verb+"=================================="+enrty.getKey());
							Matcher m = p.matcher(verb);
							if (!verb.equalsIgnoreCase(concept)) {
								if (m.find() && !enrty.getKey().equalsIgnoreCase("crosses")) {
									valueFlag = true;
									Property property = new Property();
									if (relationList.size() == 0 || !relationList.contains(enrty.getValue())) {
										relationList.add(enrty.getValue());
										valuePropertyList.add(enrty.getValue());
										property.begin = myQuestion.toLowerCase().indexOf(enrty.getKey().toLowerCase());
										property.end = property.begin + enrty.getKey().length();
										property.label = enrty.getKey();
										property.uri = enrty.getValue();
										properties.add(property);
									}
									System.out.println("For class : " + classLabel + "   Found Value: " + enrty.getKey()
											+ " :" + enrty.getValue());
								}
							}
						}
					}
				}

				System.out.println("size is : " + labelPropertyD.size());
				if (labelPropertyD.size() > 0) {
					double score = 0.0;
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
					for (Entry<String, String> enrty : labelPropertyD.entrySet()) {
						for (String verb : allVerbs) {
							if (verb.contains(classLabel))
								continue;
							Pattern p = Pattern.compile("\\b" + enrty.getKey() + "\\b", Pattern.CASE_INSENSITIVE);
//						System.out.println("Keyword: "+verb+"=================================="+enrty.getKey());
							Matcher m = p.matcher(verb);
							if (!verb.equalsIgnoreCase(concept)) {
								if (m.find() && !enrty.getKey().equalsIgnoreCase("crosses")
										&& !enrty.getKey().equalsIgnoreCase("runs")
										&& !enrty.getKey().equalsIgnoreCase("south") && !enrty.getKey().equalsIgnoreCase("number") && enrty.getKey().length() > 2) {
									valueFlag = true;
									Property property = new Property();
									if (relationList.size() == 0 || !relationList.contains(enrty.getValue())) {
										relationList.add(enrty.getValue());
										valuePropertyList.add(enrty.getValue());
										property.begin = myQuestion.toLowerCase().indexOf(enrty.getKey().toLowerCase());
										property.end = myQuestion.toLowerCase().indexOf(enrty.getKey().toLowerCase())
												+ enrty.getKey().length();
										property.label = enrty.getKey();
										property.uri = enrty.getValue();
										properties.add(property);
									}
									System.out.println("For class : " + classLabel + "   Found Value: " + enrty.getKey()
											+ " :" + enrty.getValue());
								}
							}
						}
					}
				}

			}
//			classLabel = "http://dbpedia.org/ontology/" + classLabel.substring(0, 1).toUpperCase()
//					+ classLabel.substring(1);
//			System.out.println("class label : " + classLabel);
//			String classLabelValue = classLabel.substring(classLabel.lastIndexOf("/") + 1).toLowerCase();
//			String sparqlQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//					+ "prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
//					+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//					+ "prefix dbo: <http://dbpedia.org/ontology/> " + " select DISTINCT ?p ?label ?o" + " where {"
//					+ " ?uri a <" + concept + ">."
//					+ " ?uri ?p ?o. ?p rdfs:label ?label.  FILTER langMatches(lang(?label),'en')" + "}  ";
//
//			System.out.println("Sparql Query : " + sparqlQuery + "\n class label: " + classLabel);
//			Query query = QueryFactory.create(sparqlQuery);
//
//			QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
//
//			ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
//			if (!results.hasNext()) {
//				break;
//			} else {
//				while (results.hasNext()) {
//					QuerySolution qs = results.next();
//					String dbpediaProperty = qs.get("p").toString();
//					// String geom = qs.getLiteral("geom").getString();
//					if (!dbpediaProperty.contains(classLabelValue)
//							&& (dbpediaProperty.contains("http://dbpedia.org/ontology/")
//									|| dbpediaProperty.contains("http://dbpedia.org/property/"))) {
//						// System.out.println("Property : " + dbpediaProperty);
//						String labelProperty = qs.get("label").toString().toLowerCase();
//						String valueProperty = qs.get("o").toString();
//						labelProperty = labelProperty.substring(0, labelProperty.indexOf("@"));
//						double score = 0.0;
//						SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//						StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
//						// for (String word : myQuestion.split(" ")) {
//						// score = service.score(labelProperty, word);
//						// if (score > 0.95) {
//						// if (relationList.size() == 0) {
//						// relationList.add(dbpediaProperty);
//						// } else if (!relationList.contains(dbpediaProperty)) {
//						// relationList.add(dbpediaProperty);
//						// }
//						// System.out.println("Found : " + dbpediaProperty + "
//						// :" + labelProperty);
//						// }
//						// score =
//						// service.score(valueProperty.toLowerCase().replace(labelProperty,
//						// " ").trim(), word);
//						if (valueProperty.length() < 20) {
//							for (String verb : allVerbs) {
//								Pattern p = Pattern.compile("\\b" + valueProperty + "\\b", Pattern.CASE_INSENSITIVE);
////								System.out.println("Keyword: "+verb+"=================================="+valueProperty);
//								Matcher m = p.matcher(verb);
//								if (!verb.equalsIgnoreCase(concept)) {
//									if (m.find() && !valueProperty.equalsIgnoreCase("crosses")) {
//										valueFlag = true;
//										Property property = new Property();
//										if (relationList.size() == 0 || !relationList.contains(dbpediaProperty)) {
//											relationList.add(dbpediaProperty);
//											valuePropertyList.add(valueProperty);
//											property.begin = myQuestion.toLowerCase()
//													.indexOf(valueProperty.toLowerCase());
//											property.end = property.begin + valueProperty.length();
//											property.label = dbpediaProperty;
//											property.uri = valueProperty;
//											properties.add(property);
//										}
//										System.out.println("Found Value: " + dbpediaProperty + " :" + valueProperty);
//									}
//								}
//							}
//						}
//						// }
//					}
//				}
//			}

			for (Property DBpediaProperty : properties) {
				sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
						+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
						+ "prefix dbp: <http://dbpedia.org/property/> " + "INSERT { " + "GRAPH <"
						+ myQanaryQuestion.getOutGraph() + "> { " + "  ?a a qa:AnnotationOfRelation . "
						+ "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
						+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
						+ "			  oa:hasSelector  [ " //
						+ "			         a        oa:TextPositionSelector ; " //
						+ "			         oa:start " + DBpediaProperty.begin + " ; " //
						+ "			         oa:end   " + DBpediaProperty.end + " " //
						+ "		     ] " //
						+ "  ] ; " + "     oa:hasValue <" + DBpediaProperty.uri + ">;"
//						+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; "
						+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
						+ "BIND (now() as ?time) " + "}";
				logger.info("Sparql query {}", sparql);
				myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
			}
//
//			sparqlQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//					+ "prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
//					+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//					+ "prefix dbo: <http://dbpedia.org/ontology/> " + " select DISTINCT ?p ?label " + " where {"
//					+ " ?uri a <" + concept + ">."
//					+ " ?uri ?p ?o. ?p rdfs:label ?label.  FILTER langMatches(lang(?label),'en')" + "}  ";
//
//			System.out.println("Sparql Query : " + sparqlQuery + "\n class label: " + concept);
//			query = QueryFactory.create(sparqlQuery);
//			exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
//			results = ResultSetFactory.copyResults(exec.execSelect());
//			if (!results.hasNext()) {
//				break;
//			} else {
//				while (results.hasNext()) {
//					QuerySolution qs = results.next();
//					String dbpediaProperty = qs.get("p").toString();
//					// String geom = qs.getLiteral("geom").getString();
//					if (!dbpediaProperty.contains(classLabelValue)
//							&& (dbpediaProperty.contains("http://dbpedia.org/ontology/")
//									|| dbpediaProperty.contains("http://dbpedia.org/property/"))) {
//						// System.out.println("Property : " + dbpediaProperty);
//						String labelProperty = qs.get("label").toString().toLowerCase();
//
//						labelProperty = labelProperty.substring(0, labelProperty.indexOf("@"));
//						// double score = 0.0;
//						// SimilarityStrategy strategy = new
//						// JaroWinklerStrategy();
//						//
//						// StringSimilarityService service = new
//						// StringSimilarityServiceImpl(strategy);
//						// for (String word : myQuestion.split(" ")) {
//						// score = service.score(labelProperty, word);
//
//						Pattern p = Pattern.compile("\\b" + labelProperty + "\\b", Pattern.CASE_INSENSITIVE);
//						for (String verb : allVerbs) {
////							System.out.println("Keyword: "+verb+"======================"+labelProperty);
//							Matcher m = p.matcher(verb);
//							if (!verb.equalsIgnoreCase(concept)) {
//								Property property = new Property();
//								if (m.find() && !labelProperty.equalsIgnoreCase("crosses")
//										&& !labelProperty.equalsIgnoreCase("runs")
//										&& !labelProperty.equalsIgnoreCase("south") && labelProperty.length() > 2) {
//									if (relationList.size() == 0 || !relationList.contains(dbpediaProperty)) {
//										relationList.add(dbpediaProperty);
//										property.begin = myQuestion.toLowerCase().indexOf(labelProperty.toLowerCase());
//										property.end = property.begin + labelProperty.length();
//										property.label = labelProperty;
//										property.uri = dbpediaProperty;
//										properties.add(property);
//									}
////									else if (!relationList.contains(dbpediaProperty)) {
////										relationList.add(dbpediaProperty);
////									}
//									System.out.println("Found : " + dbpediaProperty + " :" + labelProperty);
//								}
//							}
//						}
//						// }
//					}
//				}
//			}

		}


		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

	class Concept {
		public int begin;
		public int end;
		public String link;
	}

	public class Property {
		public int begin;
		public int end;
		public String label;
		public String uri;
	}

}
