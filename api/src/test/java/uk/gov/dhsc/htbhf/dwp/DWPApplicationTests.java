package uk.gov.dhsc.htbhf.dwp;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static uk.gov.dhsc.htbhf.swagger.SwaggerGenerationUtil.assertSwaggerDocumentationRetrieved;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = DWPApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase
public class DWPApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void contextLoads() {
    }

    @Test
    public void swaggerDocumentationRetrieved() throws IOException {
        assertSwaggerDocumentationRetrieved(testRestTemplate, port);
    }

}
