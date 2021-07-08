package eu.wdaqua.qanary.AGDISTIS;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.json.JSONArray;
import org.openrdf.query.resultio.stSPARQLQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.earthobservatory.org.StrabonEndpoint.client.EndpointResult;
import eu.earthobservatory.org.StrabonEndpoint.client.SPARQLEndpoint;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

@Component
public class AGDISTIS extends QanaryComponent {
	private final String agdistisService = "http://akswnc9.informatik.uni-leipzig.de:8113/AGDISTIS"; // http://139.18.2.164:8080/AGDISTIS
	private static final Logger logger = LoggerFactory.getLogger(AGDISTIS.class);

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

	public static int getNoOfLinks(String sparqlQuery, String endpointURI) {
		int count = 0;

		Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);
		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {

		} else {
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				count = qs.getLiteral("total").getInt();
				System.out.println("total: " + count);
			}
		}

		return count;
	}

	public static void runSparqlOnEndpoint(String sparqlQuery, String endpointURI) {

		Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);
		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {

		} else {
			while (results.hasNext()) {

				QuerySolution qs = results.next();

				String selectVariable = "";
				// if(sparqlQuery.contains("select ?x")) {
				selectVariable = "x";
				String uria = qs.get("x").toString();
				System.out.println("uria: " + uria);
				// }

			}
		}
	}

	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		try {
			long startTime = System.currentTimeMillis();
			logger.info("process: {}", myQanaryMessage);

			String countQuery1 = "SELECT (count(?p) as ?total) where { ";
			String countQuery2 = " ?p ?o. }";

			List<String> entitiesList = new ArrayList<String>();
			// STEP 1: Retrive the information needed for the question

			// the class QanaryUtils provides some helpers for standard tasks
			QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
			QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

			// Retrives the question string
			String myQuestion = myQanaryQuestion.getTextualRepresentation();

			// Retrieves the spots from the knowledge graph
			String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + "SELECT ?start ?end " + "FROM <"
					+ myQanaryMessage.getInGraph() + "> " + "WHERE { " + "?a a qa:AnnotationOfSpotInstance . "
					+ "?a oa:hasTarget [ " + "		a    oa:SpecificResource; " + "		oa:hasSource    ?q; "
					+ "		oa:hasSelector  [ " + "			a oa:TextPositionSelector ; "
					+ "			oa:start ?start ; " + "			oa:end  ?end " + "		] " + "] ; "
					+ "oa:annotatedBy ?annotator " + "} " + "ORDER BY ?start ";
			ResultSet r = myQanaryUtils.selectFromTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
			ArrayList<Spot> spots = new ArrayList<Spot>();
			while (r.hasNext()) {
				QuerySolution s = r.next();
				Spot spot = new Spot();
				spot.begin = s.getLiteral("start").getInt();
				spot.end = s.getLiteral("end").getInt();
				logger.info("Spot: {}-{}", spot.begin, spot.end);
				spots.add(spot);
			}

			// Step 2: Call the AGDISTIS service
			// Informations about the AGDISTIS API can be found here:
			// https://github.com/AKSW/AGDISTIS/wiki/2-Asking-the-webservice
			// curl --data-urlencode "text='The <entity>University of
			// Leipzig</entity> in <entity>Barack Obama</entity>.'" -d
			// type='agdistis' http://139.18.2.164:8080/AGDISTIS
			// Match the format "The <entity>University of Leipzig</entity> in
			// <entity>Barack Obama</entity>."
			String input = myQuestion;// lemmatize(myQuestion);
			Integer offset = 0;
			for (Spot spot : spots) {

				entitiesList.add(input.substring(spot.begin + offset, spot.end + offset));
				input = input.substring(0, spot.begin + offset) + "<entity>"
						+ input.substring(spot.begin + offset, spot.end + offset) + "</entity>"
						+ input.substring(spot.end + offset, input.length());
				offset += "<entity>".length() + "</entity>".length();
			}
			// String input="The <entity>University of Leipzig</entity> in
			// <entity>Barack Obama</entity>.";
			logger.info("Input to Agdistis: " + input);
			UriComponentsBuilder service = UriComponentsBuilder.fromHttpUrl(agdistisService);
			logger.info("Service request " + service);
			String body = "type=agdistis&" + "text='" + URLEncoder.encode(input, "UTF-8") + "'";
			RestTemplate restTemplate = new RestTemplate();
			String response = restTemplate.postForObject(service.build().encode().toUri(), body, String.class);
			logger.info("JSON document from Agdistis api {}", response);
//			BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmen/ned_adgistis.csv"));
			// Extract entities
			ArrayList<Link> links = new ArrayList<Link>();
			JSONArray arr = new JSONArray(response);
			for (int i = 0; i < arr.length(); i++) {
//				if(i==0) {
//					bw.write(myQuestion+",");
//				}
				if (!arr.getJSONObject(i).isNull("disambiguatedURL")) {
					Link l = new Link();
					l.link = arr.getJSONObject(i).getString("disambiguatedURL");
//					bw.write(l.link+",");
					l.linkCount = getNoOfLinks(countQuery1 + " <" + l.link + "> " + countQuery2,
							"https://dbpedia.org/sparql");

//					String yagoSparql = "select  ?x " +
//							"where { " +
//							"        {?x <http://www.w3.org/2002/07/owl#sameAs> <"+l.link+"> .} " +
//							"        UNION\n" +
//							"        { ?x <http://yago-knowledge.org/resource/hasWikipediaUrl> <https://dbpedia.org/resource/"+l.link.substring(l.link.lastIndexOf('/')+1)+"> . } " +
//							"      } ";
//
//					runSparqlOnEndpoint(yagoSparql, "https://linkeddata1.calcul.u-psud.fr/sparql");
					l.begin = arr.getJSONObject(i).getInt("start") - 1;
					l.end = arr.getJSONObject(i).getInt("start") - 1 + arr.getJSONObject(i).getInt("offset");
					l.printLink();
					links.add(l);
				}
			}
//			bw.newLine();
			int cnt = 0;
			for (String instance : entitiesList) {
				if (StringUtils.isNumeric(instance))
					continue;
				String sparqlQuery = " select ?instance where { ?instance <http://www.app-lab.eu/osm/ontology#has_name> \""
						+ instance + "\"^^<http://www.w3.org/2001/XMLSchema#string> . }";// ?instance ?p ?o . }";

				String host = "pyravlos1.di.uoa.gr";
				Integer port = 8080;
				String appName = "geoqa/Query";
				String query = sparqlQuery;
				String format = "TSV";

//				Query query = QueryFactory.create(sparqlQuery);
//				System.out.println("sparql query :"+query.toString());
//				QueryExecution exec = QueryExecutionFactory.sparqlService("http://pyravlos2.di.uoa.gr:8080/geoqa/Query", query);
//				ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
//
//				if (!results.hasNext()) {
//
//				} else {
//					while (results.hasNext()) {
//
//						QuerySolution qs = results.next();
//						String uria = qs.get("instance").toString();
//						System.out.println("URIA: "+uria);
//						Link l = new Link();
//						l.link = uria;
//						l.begin = myQuestion.indexOf(instance);
//						l.end = myQuestion.indexOf(instance)+instance.length();
//						links.add(l);
//					}
//				}

				SPARQLEndpoint endpoint = new SPARQLEndpoint(host, port, appName);
				if (query.length() > 2) {
					try {

						EndpointResult result = endpoint.query(query,
								(stSPARQLQueryResultFormat) stSPARQLQueryResultFormat.valueOf(format));

						System.out.println("<----- Result ----->");
						String resultString[] = result.getResponse().replaceAll("\n", "\n\t").split("\n");
						for (int i = 1; i < resultString.length; i++) {
							Link l = new Link();
							l.link = resultString[i].trim();
							l.linkCount = getNoOfLinks(countQuery1 + " <" + l.link + "> " + countQuery2,
									"http://pyravlos1.di.uoa.gr:8080/geoqa/Query");
							l.begin = myQuestion.indexOf(instance);// lemmatize(myQuestion).indexOf(instance);
							l.end = myQuestion.indexOf(instance) + instance.length();// lemmatize(myQuestion).indexOf(instance)+instance.length();
							links.add(l);
							System.out.println("Question: " + myQuestion + " ::: " + resultString[i].trim()
									+ "== index : " + l.begin + " : " + l.end);
						}

						System.out.println("<----- Result ----->");

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				sparqlQuery = "select ?instance where { ?instance <http://www.app-lab.eu/gadm/ontology/hasName> \""
						+ instance + "\"@en . }";// ?instance ?p ?o . }";
				query = sparqlQuery;
				if (query.length() > 2) {
					try {

						EndpointResult result = endpoint.query(query,
								(stSPARQLQueryResultFormat) stSPARQLQueryResultFormat.valueOf(format));

						System.out.println("<----- Result ----->");
						// System.out.println(result.getResponse().replaceAll("\n", "\n\t"));
						String resultString[] = result.getResponse().replaceAll("\n", "\n\t").split("\n");
						for (int i = 1; i < resultString.length; i++) {
							Link l = new Link();
							l.link = resultString[i].trim();
							l.linkCount = getNoOfLinks(countQuery1 + " <" + l.link + "> " + countQuery2,
									"http://pyravlos1.di.uoa.gr:8080/geoqa/Query");
							l.begin = myQuestion.indexOf(instance);// lemmatize(myQuestion).indexOf(instance);
							l.end = myQuestion.indexOf(instance) + instance.length();// lemmatize(myQuestion).indexOf(instance)+instance.length();
							links.add(l);
							System.out.println("Question: " + myQuestion + " ::: " + resultString[i].trim()
									+ "== index : " + l.begin + " : " + l.end);
						}
						System.out.println("<----- Result ----->");

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				cnt++;
			}
			// STEP4: Push the result of the component to the triplestore

			logger.info("Apply vocabulary alignment on outgraph");
			for (Link l : links) {
				sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
						+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "INSERT { " + "GRAPH <"
						+ myQanaryQuestion.getOutGraph() + "> { " + "  ?a a qa:AnnotationOfInstance . "
						+ "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
						+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
						+ "           oa:hasSelector  [ " + "                    a oa:TextPositionSelector ; "
						+ "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; "
						+ "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger  ;"
						+ "					   oa:linkcount \"" + l.linkCount + "\"^^xsd:nonNegativeInteger "
						+ "           ] " + "  ] . " + "  ?a oa:hasBody <" + l.link + "> ;"
						+ "     oa:annotatedBy <http://agdistis.aksw.org> ; " + "	    oa:AnnotatedAt ?time  " + "}} "
						+ "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ." + "BIND (now() as ?time) " + "}";
				logger.info("Sparql query {}", sparql);
				myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			logger.info("Time {}", estimatedTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return myQanaryMessage;
	}

	class Spot {
		public int begin;
		public int end;
	}

	class Link {
		public int begin;
		public int end;
		public String link;
		public int linkCount;

		public void printLink() {
			System.out.println("Start: " + begin + "\tEnd: " + end + "\tLink: " + link);
		}
	}
}
