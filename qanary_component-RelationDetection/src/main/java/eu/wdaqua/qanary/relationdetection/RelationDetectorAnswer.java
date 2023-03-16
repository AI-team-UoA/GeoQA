package eu.wdaqua.qanary.relationdetection;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * represents the answer of a {@link RelationDetector} processing
 *
 * @author AnBo
 *
 */
public class RelationDetectorAnswer {

	private final String relationStringInQuestion;
	private final int indexBegin;

	/**
	 * the identifier for the RDF entity representing the geospatial relation
	 */
	private final URI geospatialRelationIdentifier;

	public RelationDetectorAnswer(GeospatialRelation geospatialRelation, int index, String relationString) throws URISyntaxException {
		this.indexBegin = index;
		this.relationStringInQuestion = relationString;
		this.geospatialRelationIdentifier = new URI(geospatialRelation.getURI());
//		this.geospatialRelationIdentifier = new URI(geospatialRelation.getURI() + this.getGeospatialRelation().hashCode());
	}

	public URI getGeospatialRelationIdentifier(){
		return this.geospatialRelationIdentifier;
	}

	public String getRelationStringInQuestion() {
		return relationStringInQuestion;
	}

	public int getIndexBegin(){
		return indexBegin;
	}
}
