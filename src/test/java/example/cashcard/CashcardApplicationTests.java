package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CashcardApplicationTests {
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void doesNotReturnACashCardWhenUsingInvalidCredentials() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("BAD-USER", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("sarah1", "BAD-PASSWORD")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rejectsUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("hank-owns-no-cards", "qrs456")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void doesNotReturnACashCardWithUnknownId() {
		ResponseEntity<String> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/1000", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	public void returnsACashCard() {
		ResponseEntity<String> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Number amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);

		String owner = documentContext.read("$.owner");
		assertThat(owner).isEqualTo("sarah1");
	}

	@Test
	@DirtiesContext
	public void createsACashCard() {
		CashCard cashCard = new CashCard(null, 250.00, "sarah1");
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.postForEntity("/cashcards", cashCard, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI cashCardLocation = response.getHeaders().getLocation();
		ResponseEntity<String> getResponse = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity(cashCardLocation, String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isNotNull();

		Number amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(250.00);

		String owner = documentContext.read("$.owner");
		assertThat(owner).isEqualTo("sarah1");
	}

	@Test
	public void returnsAListOfAllCashCards() {
		ResponseEntity<String> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardsListLength = documentContext.read("$.length()");
		assertThat(cashCardsListLength).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	public void returnsAListOfAllCashCardsWithNoParametersAndUsesDefaultValues() {
		ResponseEntity<String> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardsListLength = documentContext.read("$.length()");
		assertThat(cashCardsListLength).isEqualTo(3);

		List<Double> amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);

		List<String> owners = documentContext.read("$..owner");
		assertThat(owners).containsExactly("sarah1", "sarah1", "sarah1");
	}

	@Test
	public void updatesAnExistingCashCard() {
		CashCard cashCard = new CashCard(null, 19.00, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCard);
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Number amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(19.00);
	}

	@Test
	public void doesNotUpdateACashCardThatDoesNotExist() {
		CashCard unknownCashCard = new CashCard(null, 19.00, null);
		HttpEntity<CashCard> request = new HttpEntity<>(unknownCashCard);
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/999", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void deletesACashCard() {
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void doesNotDeleteACashCardThatDoesNotExist() {
		ResponseEntity<Void> response = this.restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/999", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void doesNotDeleteACashCardThatAUserDoesNotOwn() {
		ResponseEntity<Void> deleteResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void contextLoads() {
	}

}
