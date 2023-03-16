package eu.wdaqua.qanary.relationdetection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

/**
 * This is the main class of the RelationDetection Component that interacts with the pipeline and stores data in the triplestore.
 * 		GeospatialRelation: Enum representing all supported geospatial relations.
 * 		RelationDetector: Class containing the relation-detection logic, i.e. the functionality of this component.
 * 		RelationDetectorAnswer: Class used to represent the results of the RelationDetector.
 */

@Component
public class RelationDetection extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(RelationDetection.class);
	private final RelationDetector myRelationDetector = new RelationDetector();

	/**
	 * implement this method encapsulating the functionality of your Qanary component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		/*
		 * Informational logging
		 */
		logger.info("process: {}", myQanaryMessage);
		try {
			logger.info("store data in graph {}", myQanaryMessage.getValues().get(new URL(myQanaryMessage.getEndpoint().toString())));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return myQanaryMessage;
		}

		/*
		 * Relation Detection
		 */
		long startTime = System.currentTimeMillis();

		// compute and evaluate the GeoRelations
		QanaryQuestion<String> myQanaryQuestion = getQanaryQuestion(myQanaryMessage);
		List<RelationDetectorAnswer> myDetectedRelationList = myRelationDetector.process(myQanaryQuestion); // this is where the detection happens
		if (myDetectedRelationList == null || myDetectedRelationList.size() == 0) {
			logger.info("Time with no results {}", System.currentTimeMillis() - startTime);
			return myQanaryMessage;
		}

		// store the data in the provided Qanary triplestore using the defined outgraph
		QanaryUtils myQanaryUtils = getUtils(myQanaryMessage);
		for(RelationDetectorAnswer myDetectedRelation : myDetectedRelationList) {
			// Push the GeoRelation in graph
			logger.info("applying vocabulary alignment on outgraph {}", myQanaryQuestion.getOutGraph());
			String sparql = ""
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
					+ "INSERT { "
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
					+ "  ?a a qa:AnnotationOfRelation."
					+ "  ?a oa:hasTarget [ "
					+ "        a    oa:SpecificResource; "
					+ "        oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
					+ "        oa:hasRelation [ "
					+ "          a oa:GeoRelation ; "
					+ "          oa:geoRelation <" + myDetectedRelation.getGeospatialRelationIdentifier() + "> ; "
					+ "             oa:hasSelector  [ "
					+ "                    a oa:TextPositionSelector ; "
					+ "                    oa:start \"" + myDetectedRelation.getIndexBegin() + "\"^^xsd:nonNegativeInteger ; "
					+ "                    oa:relString \"" + myDetectedRelation.getRelationStringInQuestion() +"\"^^xsd:string ;"
					+ "             ] "
					+ "        ] "
					+ "  ] " + "}} "
					+ "WHERE { "
					+ "BIND (IRI(str(RAND())) AS ?a) ."
					+ "BIND (now() as ?time) "
					+ "}";
			myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
		}

		logger.info("Time with results {}", System.currentTimeMillis() - startTime);

		return myQanaryMessage;
	}
}
