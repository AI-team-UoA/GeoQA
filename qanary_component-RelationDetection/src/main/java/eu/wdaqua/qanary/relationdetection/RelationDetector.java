package eu.wdaqua.qanary.relationdetection;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.utils.CoreNLPUtilities;

/**
 * processor of question containing geospatial relation information
 *
 * @author AnBo
 *
 */

@Component
public class RelationDetector {
	private static final Logger logger = LoggerFactory.getLogger(RelationDetector.class);

	public List<Integer> getIndexListOfSpatialRelation(String word, String myQuestion) {
		var indexesOfSpatialRelation = new ArrayList<Integer>();
		Pattern p = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
		Matcher m;
		if(myQuestion.contains("crosses") || myQuestion.contains("includes") || myQuestion.contains("located"))
			m = p.matcher(myQuestion);
		else
			m = p.matcher(CoreNLPUtilities.lemmatize(myQuestion));

		while (m.find()) {
			indexesOfSpatialRelation.add(m.start());
		}
		return indexesOfSpatialRelation;
	}

	public List<Integer> getIndexListOfSpatialRelationLem(String word, String myQuestion) {
		var indexesOfSpatialRelation = new ArrayList<Integer>();
		Pattern p = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
		Matcher m;
		m = p.matcher(myQuestion);
		while (m.find()) {
			indexesOfSpatialRelation.add(m.start());
		}
		return indexesOfSpatialRelation;
	}

	/**
	 * Tries to match @textualRepresentation (label/word that refers to @myGeospatialRelation) in @myQuestion.
	 * Every match is added in @relationDetectorAnswerList.
	 * <p>
	 * Returns: true if a match was made, false otherwise.
	 */
	public boolean matchRelationInQuestion(ArrayList<RelationDetectorAnswer> relationDetectorAnswerList, String myQuestion,
										   GeospatialRelation myGeospatialRelation, String textualRepresentation, Pattern p) throws URISyntaxException {
		Matcher m = p.matcher(myQuestion);
		if (!m.find())
			return false;

		List<Integer> indexesOfSpatialRelation = getIndexListOfSpatialRelation(textualRepresentation, myQuestion);
		if (indexesOfSpatialRelation != null) {
			for (Integer indexOfSpatialRelation : indexesOfSpatialRelation) {
				logger.info("processed question: {}, found {} at{}", myQuestion, myGeospatialRelation, indexOfSpatialRelation);
				relationDetectorAnswerList.add(new RelationDetectorAnswer(myGeospatialRelation, indexOfSpatialRelation, textualRepresentation));
			}
		}
		return true;
	}

	/**
	 * check for the existence of a geospatial relation in the question
	 */
	public List<RelationDetectorAnswer> process(QanaryQuestion<String> myQanaryQuestion) throws Exception {
		return this.process(myQanaryQuestion.getTextualRepresentation());
	}

	/**
	 * check for the existence of a geospatial relation in the question
	 */
	public List<RelationDetectorAnswer> process(String myQuestion) throws URISyntaxException {
		logger.debug("The relationDetection Component is running with this question: {}", myQuestion);

		// TODO: Filter the POS tag that are verb, adverb, adjective to restrict input for relation detector
//		var filteredPosTags = CoreNLPUtilities.tagBasedGet(myQuestion, new String[] {"VB", "IN", "VP", "VBP", "VBZ", "RB", "RBS", "RBR"});

		// relation detection logic
		String lemmatizedQuestion = CoreNLPUtilities.lemmatize(myQuestion);
		var relationDetectorAnswerList = new ArrayList<RelationDetectorAnswer>();
		for (GeospatialRelation myGeospatialRelation : GeospatialRelation.values()) { 	// for every relation that GeoQA supports
			for (String textualRepresentation : myGeospatialRelation.getLabels()) { 	// for every label/word that refers to that relation
				Pattern p = Pattern.compile("\\b" + textualRepresentation + "\\b", Pattern.CASE_INSENSITIVE);
				// TODO: Shouldn't we check the lemmatized version anyway? What if we miss a relation in the lemmatized version? Naturally we would need to remove duplicates.
				if (!matchRelationInQuestion(relationDetectorAnswerList, myQuestion, myGeospatialRelation, textualRepresentation, p)) { 		// check if the label exists in the question
					matchRelationInQuestion(relationDetectorAnswerList, lemmatizedQuestion, myGeospatialRelation, textualRepresentation, p);	// check if the label exists in the lemmatized question
					// FIXME: This normally used `getIndexListOfSpatialRelationLem` but there shouldn't be a problem (just a bit of wasted CPU time). DO CHECK IF IT WORKS!!!
				}
			}
		}

		return relationDetectorAnswerList;
	}
}