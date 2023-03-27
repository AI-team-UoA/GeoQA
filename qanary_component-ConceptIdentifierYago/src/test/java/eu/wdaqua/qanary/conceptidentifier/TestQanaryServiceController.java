package eu.wdaqua.qanary.conceptidentifier;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.inGraphKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.outGraphKey;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.QanaryServiceController;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestQanaryServiceController {
	private static final Logger logger = LoggerFactory.getLogger(TestQanaryServiceController.class);

	@Inject
	QanaryServiceController controller;

	private MockMvc mockMvc;

	/**
	 * initialize local controller enabled for tests
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/jsp/view/");
		viewResolver.setSuffix(".jsp");

		mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
	}

	/**
	 * test description interface
	 *
	 * @throws Exception
	 */
	@Test
	public void testDescriptionAvailable() throws Exception {
		mockMvc.perform(get(QanaryConfiguration.description)) // fetch
				.andExpect(status().isOk()) // HTTP 200
				.andReturn(); //
	}

	/**
	 * test correct message format
	 */
	@Test
	public void testMessageFromJson() {
		// create message from json string
		QanaryMessage message;
		try {
			message = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));

			URI endpointKeyUrlFromMessage = message.getEndpoint();
			assertNotNull(endpointKeyUrlFromMessage);

			URI endpointKeyUrlFromHere = new URI(endpointKey);

			// TODO: more tests to ensure mechanism
			assertTrue(endpointKeyUrlFromHere.toString().compareTo(endpointKeyUrlFromMessage.toString()) == 0);

		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	// TODO: Move these tests to a separate file

	@Test
	public void testCreateNGrams() {
		var ngrams = ConceptIdentifier.createNGrams(2, "My name is Sergios");
		assertEquals("My name", ngrams.get(0));
		assertEquals("name is", ngrams.get(1));
		assertEquals("is Sergios", ngrams.get(2));

		ngrams = ConceptIdentifier.createNGrams(1, "My name is Sergios");
		var split = "My name is Sergios".split(" ");
		for (int i = 0; i < ngrams.size(); i++) {
			assertEquals(split[i], ngrams.get(i));
		}
	}
}