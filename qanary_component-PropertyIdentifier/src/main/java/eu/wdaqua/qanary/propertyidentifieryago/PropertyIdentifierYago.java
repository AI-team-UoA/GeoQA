package eu.wdaqua.qanary.propertyidentifieryago;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.*;
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
				if (pos.contains("NN") || pos.contains("JJ") || pos.contains("NP") || pos.contains("NNP")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
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
					retVal = true;
//					System.out.println(" Source ================================================= Dest ");
//					System.out.println("edge : "+edge.toString());
//					System.out.println("source: "+edge.getSource());
//					System.out.println("relation: "+edge.getRelation());
//					System.out.println("dependent :"+edge.getDependent());
				}
				else if (edge.getSource().toString().contains("NN") && edge.getDependent().toString().contains("JJS")){
					retVal = true;
//					System.out.println("Dest ================================================= Source");
//					System.out.println("edge : "+edge.toString());
//					System.out.println("source: "+edge.getSource());
//					System.out.println("relation: "+edge.getRelation());
//					System.out.println("dependent :"+edge.getDependent());
				}
			}
		}
		return retVal;
	}

	public static String getJJS(String documentText) {
		String retVal = "";
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
					retVal = edge.getSource().toString();
					retVal = retVal.substring(0,retVal.indexOf('/')-1);
				}
				else if (edge.getSource().toString().contains("NN") && edge.getDependent().toString().contains("JJS")){
					retVal = edge.getDependent().toString();
					retVal = retVal.substring(0,retVal.indexOf('/')-1);
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
		List<Property> instProperties = new ArrayList<Property>();
		List<String> instanceProperties = new ArrayList<String>();
		List<String> valuePropertyList = new ArrayList<String>();
		List<String> allClassesList = new ArrayList<String>();
		boolean valueFlag = false;
		Set<String> coonceptsUri = new HashSet<String>();
		ResultSet r;
		Set<Entity> entities = new HashSet<Entity>();
		Set<String> entitiesUri = new HashSet<String>();
		HashMap<String,String> mapProperty = new HashMap<String,String>();
		Set<Concept> concepts = new HashSet<Concept>();

		mapProperty.put("mountain","http://dbpedia.org/ontology/elevation");
		mapProperty.put("river","http://dbpedia.org/property/length");
		mapProperty.put("stadium","http://dbpedia.org/property/capacity");
		mapProperty.put("city","http://dbpedia.org/ontology/populationTotal");
		mapProperty.put("lake","http://dbpedia.org/ontology/areaTotal");
		mapProperty.put("bridge","http://dbpedia.org/ontology/length");
		mapProperty.put("mountain","http://dbpedia.org/ontology/elevation");
		mapProperty.put("mountain","http://dbpedia.org/ontology/elevation");

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
				allClassesList.add(conceptTemp.link);
				logger.info("Concept start {}, end {} concept {} link {}", conceptTemp.begin, conceptTemp.end,
						myQuestion.substring(conceptTemp.begin, conceptTemp.end), conceptTemp.link);
			}

		}

		for (int i = 0; i < allVerbs.size(); i++) {
			for (String classlUri : allClassesList) {
				if (classlUri.toLowerCase().contains(allVerbs.get(i).toLowerCase())) {
					allVerbs.remove(i);
					i--;
					break;
				}
			}
		}

		sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?start ?end ?lcount ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
				+ "		     a               oa:SpecificResource; " //
				+ "		     oa:hasSource    ?q; " //
				+ "	         oa:hasSelector  [ " //
				+ "			         a        oa:TextPositionSelector ; " //
				+ "			         oa:start ?start ; " //
				+ "			         oa:end   ?end ;" //
				+ "			         oa:linkcount   ?lcount " + "		     ] " //
				+ "    ] . " //
				+ " ?a oa:hasBody ?uri ; " + "    oa:annotatedBy ?annotator " //
				+ "} " + "ORDER BY ?start ";

		r = myQanaryUtils.selectFromTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());

		while (r.hasNext()) {
			QuerySolution s = r.next();

			Entity entityTemp = new Entity();
			entityTemp.begin = s.getLiteral("start").getInt();

			entityTemp.end = s.getLiteral("end").getInt();

			entityTemp.uri = s.getResource("uri").getURI();

			System.out.println("uri: " + entityTemp.uri + "\t start: " + entityTemp.begin + "\tend: " + entityTemp.end);

			entityTemp.namedEntity = myQuestion.substring(entityTemp.begin, entityTemp.end);

			entityTemp.linkCount = s.getLiteral("lcount").getInt();
			if (entityTemp.uri.contains("yago-knowledge.org")) {
				entities.add(entityTemp);
				entitiesUri.add(entityTemp.uri);
			}
		}

		System.out.println("all verbs: " + allVerbs);
		for (String concept : coonceptsUri) {

			String classLabel = concept.substring(concept.lastIndexOf("/") + 1);
			System.out.println("classLabel : " + classLabel);
			File file = new File("src/main/resources/yagoproperties/" + classLabel + "_yago_label_of_properties.txt");
			File file1 = new File("src/main/resources/yagoproperties/" + classLabel + "_yago_value_of_properties.txt");
			Map<String, String> valuePropertyD = new HashMap<String, String>();
			Map<String, String> labelPropertyD = new HashMap<String, String>();
			if (file.exists()) {
				valuePropertyD.clear();
				labelPropertyD.clear();
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

				System.out.println("size is : " + labelPropertyD.size());

				if (labelPropertyD.size() > 0) {
					double score = 0.0;
					System.out.println("Inside label property:");
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
//					BufferedWriter bw1 = new BufferedWriter(
//							new FileWriter("/home/dharmen/justtestproperties.txt", true));
					for (Entry<String, String> entry : labelPropertyD.entrySet()) {
						for (String verb : allVerbs) {
							Pattern p = Pattern.compile("\\b" + verb  + "\\b", Pattern.CASE_INSENSITIVE);
//							if (verb.contains("height") && entry.getKey().contains("height")) {
//								bw1.write(entry.getKey());
//								bw1.newLine();
//							}
//						System.out.print("::Keyword: "+verb+"=================================="+entry.getKey()+"::");
							Matcher m = p.matcher(entry.getKey());
//							if (!concept.contains(verb)) {
//								System.out.println("before matching if condition =============== "+ verb);
							if (m.find() && !entry.getKey().equalsIgnoreCase("crosses")
									&& !entry.getKey().equalsIgnoreCase("runs")
									&& !entry.getKey().equalsIgnoreCase("south")
									&& !entry.getKey().equalsIgnoreCase("number") && entry.getKey().length() > 2) {
								valueFlag = true;
								Property property = new Property();
								if (relationList.size() == 0 || !relationList.contains(entry.getValue())) {
									relationList.add(entry.getValue());
									valuePropertyList.add(entry.getValue());
									property.begin = myQuestion.toLowerCase().indexOf(entry.getKey().toLowerCase());
									property.end = myQuestion.toLowerCase().indexOf(entry.getKey().toLowerCase())
											+ entry.getKey().length();
									property.label = entry.getKey();
									property.uri = entry.getValue();
//										if (property.begin > 0 && property.end > property.begin) {
									if(classLabel.equals("Mountain")  ){
										if(property.uri.equals("http://dbpedia.org/property/highest") || property.uri.equals("http://dbpedia.org/property/height"))
											property.uri = "http://dbpedia.org/ontology/elevation";
									}
									properties.add(property);
									System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
											+ " :" + entry.getValue() + " : Begin : " + property.begin + "  : end "
											+ property.end);
//										}
								}
								System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
										+ " :" + entry.getValue());
							}
//							}
						}
					}
//					bw1.close();
				}
				if (properties.size() == 0) {
					if (valuePropertyD.size() > 0) {
						double score = 0.0;
						System.out.println("Inside value property: ");
//						SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//						StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
						for (Entry<String, String> entry : valuePropertyD.entrySet()) {
							for (String verb : allVerbs) {
								if (verb.contains(classLabel.toLowerCase()) || verb.length() < 3
										|| entry.getKey().length() > 10 || entry.getKey().contains("(")
										|| entry.getKey().contains(")"))
									continue;

								Pattern p = Pattern.compile("\\b" + entry.getKey() + "\\b", Pattern.CASE_INSENSITIVE);
//						System.out.println("Keyword: "+verb+"=================================="+enrty.getKey());
								Matcher m = p.matcher(verb);
								if (!verb.equalsIgnoreCase(concept)) {
									if (m.find() && !entry.getKey().equalsIgnoreCase("crosses")) {
										valueFlag = true;
										Property property = new Property();
										if (relationList.size() == 0 || !relationList.contains(entry.getValue())) {
											relationList.add(entry.getValue());
											valuePropertyList.add(entry.getValue());
											property.begin = myQuestion.toLowerCase()
													.indexOf(entry.getKey().toLowerCase());
											property.end = property.begin + entry.getKey().length();
											property.label = verb;
											property.uri = entry.getValue();
											if (property.begin > 0 && property.end > property.begin) {
												properties.add(property);
												System.out.println("For class : " + classLabel + "   Found Value: "
														+ verb + " :" + entry.getValue() + " : Begin : "
														+ property.begin + "  : end " + property.end);
											}
										}
										System.out.println("For class : " + classLabel + "   Found Value: "
												+ entry.getKey() + " :" + entry.getValue());
									}
								}
							}
						}
					}
				}
			}
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
						+ "  ] ; " + "     oa:hasValue <" + DBpediaProperty.uri.trim() + ">;"
//						+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; "
						+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
						+ "BIND (now() as ?time) " + "}";
				logger.info("Sparql query {}", sparql);
				myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
			}
			valuePropertyD.clear();
			labelPropertyD.clear();
		}
		//check for implicit properties in question

		if(isJJSNN(myQuestion)){
			String questionProperty = getJJS(myQuestion);
			for(Property prop: properties){
				if(!prop.label.toLowerCase().contains(questionProperty)||!prop.uri.toLowerCase().contains(questionProperty)){
					Property property = new Property();
					switch (questionProperty){
						case "highest":
								property.uri = "http://dbpedia.org/ontology/elevation";
								property.label = questionProperty;
								property.begin = myQuestion.indexOf(questionProperty);
								property.end = property.begin+questionProperty.length();
							break;
						case "longest":
							property.uri = "http://dbpedia.org/property/length";
							property.label = questionProperty;
							property.begin = myQuestion.indexOf(questionProperty);
							property.end = property.begin+questionProperty.length();
							break;
						case "population":
							property.uri = "http://dbpedia.org/ontology/populationTotal";
							property.label = questionProperty;
							property.begin = myQuestion.indexOf(questionProperty);
							property.end = property.begin+questionProperty.length();
							break;
						case "largest":
							// code need to be updated
							//this property depends on the class present in the question
							property.uri = "http://dbpedia.org/ontology/length";
							property.label = questionProperty;
							property.begin = myQuestion.indexOf(questionProperty);
							property.end = property.begin+questionProperty.length();
							break;
						default:
							break;
					}
					if(property.uri!=null){
						properties.add(property);
					}
				}
			}
		}

		// Instance property
		List<String> allNouns = getNouns(myQuestion);
		System.out.println("Nouns : " + allNouns);
		for (String entity : entitiesUri) {

			String sparqlQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
					+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "prefix dbo: <http://dbpedia.org/ontology/> " + " select DISTINCT ?p " + " where {" + "<" + entity
					+ "> " + " ?p ?o. }  ";

			System.out.println("Sparql Query : " + sparqlQuery + "\n Instance: " + entity);
			Query query = QueryFactory.create(sparqlQuery);

			QueryExecution exec = QueryExecutionFactory.sparqlService("http://pyravlos1.di.uoa.gr:8890/sparql", query);

			ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
			if (!results.hasNext() && properties.size() == 0) {
				break;
			} else {
				while (results.hasNext()) {
					QuerySolution qs = results.next();
					String dbpediaProperty = qs.get("p").toString();
					String dbpediaPropertyLabel = dbpediaProperty.substring(dbpediaProperty.lastIndexOf('/') + 1);
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
					for (String verb : allNouns) {
//									Pattern p = Pattern.compile("\\b" + dbpediaPropertyLabel + "\\b", Pattern.CASE_INSENSITIVE);
//									Matcher m = p.matcher(verb);
						if (verb.toLowerCase().contains("south") || verb.toLowerCase().contains("cross")
								|| verb.toLowerCase().contains("north") || verb.toLowerCase().contains("west")
								|| verb.toLowerCase().contains("east") || verb.toLowerCase().contains("of")
								|| verb.toLowerCase().contains("county"))
							continue;
						Property property = new Property();
//									if (m.find() && !dbpediaPropertyLabel.contains("cross")) {
						if (dbpediaPropertyLabel.toLowerCase().contains(verb.toLowerCase()) && verb.length() > 2) {
							if (relationList.size() == 0 || !relationList.contains(dbpediaPropertyLabel)
									&& !dbpediaProperty.toLowerCase().contains("rank")) {
								relationList.add(dbpediaPropertyLabel);
								instanceProperties.add(dbpediaProperty);
//											valuePropertyList.add(enrty.getValue());
								property.label = dbpediaPropertyLabel;
//											dbpediaPropertyLabel = dbpediaPropertyLabel.substring(dbpediaPropertyLabel.indexOf(' '));
								property.begin = myQuestion.toLowerCase().indexOf(verb.toLowerCase());
								property.end = myQuestion.toLowerCase().indexOf(verb.toLowerCase()) + verb.length();
								property.uri = dbpediaProperty;
								if (property.begin > 0 && property.end > property.begin
										&& !property.uri.toLowerCase().contains("mouth"))
									instProperties.add(property);
//											break;
							}
						}
//								}
					}

				}
			}
		}
		allVerbs.clear();
		for (Property DBpediaProperty : instProperties) {
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "prefix dbp: <http://dbpedia.org/property/> "
					+ "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
					+ "  ?a a qa:AnnotationOfRelationInstance . " + "  ?a oa:hasTarget [ "
					+ "           a    oa:SpecificResource; " + "           oa:hasSource    <"
					+ myQanaryQuestion.getUri() + ">; " + "			  oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start " + DBpediaProperty.begin + " ; " //
					+ "			         oa:end   " + DBpediaProperty.end + " " //
					+ "		     ] " //
					+ "  ] ; " + "     oa:hasValue <" + DBpediaProperty.uri + ">;"
//								+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; "
					+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
					+ "BIND (now() as ?time) " + "}";
			logger.info("Sparql query {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
		}
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");

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
	public class Entity {

		public int begin;
		public int end;
		public String namedEntity;
		public String uri;
		public int linkCount;

		public void print() {
			System.out.println("Start: " + begin + "\t End: " + end + "\t Entity: " + namedEntity);
		}
	}
}
