#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ${groupId}.QanaryComponent;
import ${groupId}.QanaryMessage;

@Component
public class ${classname} extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(${classname}.class);

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		try {
			logger.info("store data in graph {}", myQanaryMessage.getValues().get(new URL(QanaryMessage.endpointKey)));
			// TODO: insert data in QanaryMessage.outgraph
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		logger.info("apply vocabulary alignment on outgraph");
		// TODO: implement this (custom for every component)

		return myQanaryMessage;
	}

}
