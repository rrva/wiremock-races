package hello;


import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class HelloControllerTest {


    private Logger logger = LoggerFactory.getLogger(HelloControllerTest.class);
    private RestTemplate restTemplate = new RestTemplateBuilder().setReadTimeout(2000).build();

    @Rule
    public WireMockRule wireMock = new WireMockRule(options().port(9094).notifier
            (new Slf4jNotifier(true)));

    @Test
    public void response_with_timeout_retries_timeout() throws InterruptedException {
        final String SCENARIO = "when_clipApi_times_out_should_retry";
        final String FIRST_WAS_TIMEOUT = "t";

        wireMock.stubFor(get(urlMatching("/hello")).inScenario(SCENARIO)
                .willReturn(aJsonResponseWithContent("hello.json").withFixedDelay(3000))
                .willSetStateTo(FIRST_WAS_TIMEOUT));

        wireMock.stubFor(get(urlMatching("/hello")).inScenario(SCENARIO)
                .whenScenarioStateIs(FIRST_WAS_TIMEOUT)
                .willReturn(
                        aJsonResponseWithContent("hello.json")));


        sendTwoRequestsInParallel();

        Thread.sleep(1000);

        wireMock.verify(2, getRequestedFor(urlMatching("/hello")));

    }

    private List<Greeting> sendTwoRequestsInParallel() {
        List<Greeting> x = Arrays.asList(1, 2).parallelStream().map((y) -> {
            try {
                logger.info("Requesting "+y);
                Greeting g = restTemplate.getForObject
                        ("http://localhost:9094/hello",
                                Greeting.class);
                logger.info("Got response for "+y);
                return g;
            } catch (Exception e) {
                logger.error("Got error for "+y+" "+e.getMessage(), e);
                return new Greeting("error");
            }
        }).collect(Collectors.toList());
        return x;
    }


    private ResponseDefinitionBuilder aJsonResponseWithContent(String fileName) {
        return aResponse()
                .withBodyFile(fileName)
                .withHeader(CONTENT_TYPE, APPLICATION_JSON.toString());
    }
}
