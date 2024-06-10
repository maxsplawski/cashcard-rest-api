package example.cashcard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {
    private final CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping
    private ResponseEntity<Iterable<CashCard>> findAll() {
        return ResponseEntity.ok(this.cashCardRepository.findAll());
    }

    @GetMapping("/{id}")
    private ResponseEntity<CashCard> findById(@PathVariable Long id) {
        Optional<CashCard> optionalCashCard = this.cashCardRepository.findById(id);

        return optionalCashCard.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(
            @RequestBody CashCard createCashCardRequest,
            UriComponentsBuilder ucb
    ) {
        CashCard cashCard = this.cashCardRepository.save(createCashCardRequest);

        URI cashCardLocation = ucb
                .path("/cashcards/{id}")
                .buildAndExpand(cashCard.id())
                .toUri();

        return ResponseEntity.created(cashCardLocation).build();
    }
}
