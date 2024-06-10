package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashcardApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void doesNotReturnACashCardWithUnknownId() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/cashcards/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	public void returnsACashCard() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");

		assertThat(id).isEqualTo(99);

		Number amount = documentContext.read("$.amount");

		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	public void createsACashCard() {
		CashCard cashCard = new CashCard(null, 250.00);
		ResponseEntity<Void> response = this.restTemplate.postForEntity("/cashcards", cashCard, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI cashCardLocation = response.getHeaders().getLocation();
		ResponseEntity<String> getResponse = this.restTemplate.getForEntity(cashCardLocation, String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");

		assertThat(id).isNotNull();

		Number amount = documentContext.read("$.amount");

		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	public void returnsAListOfAllCashCards() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/cashcards", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		int cashCardsListLength = documentContext.read("$.length()");
		assertThat(cashCardsListLength).isEqualTo(3);

		List<Number> ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		List<Number> amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
	}

	@Test
	void contextLoads() {
	}

}
