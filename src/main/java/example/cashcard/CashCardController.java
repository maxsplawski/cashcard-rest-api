package example.cashcard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {
    private final CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable) {
        Page<CashCard> page = cashCardRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                ));

        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/{id}")
    private ResponseEntity<CashCard> findById(
            @PathVariable Long id,
            Principal principal
    ) {
        Optional<CashCard> optionalCashCard = this.cashCardRepository.findByIdAndOwner(id, principal.getName());

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

    @PutMapping("/{id}")
    private ResponseEntity<Void> updateCashCard(
            @PathVariable Long id,
            @RequestBody CashCard updateCashCard,
            Principal principal
    ) {
        Optional<CashCard> cashCard = this.cashCardRepository.findByIdAndOwner(id, principal.getName());
        if (cashCard.isPresent()) {
            CashCard updatedCashCard = new CashCard(cashCard.get().id(), updateCashCard.amount(), principal.getName());
            this.cashCardRepository.save(updatedCashCard);

            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(
            @PathVariable Long id,
            Principal principal
    ) {
        Optional<CashCard> cashCard = this.cashCardRepository.findByIdAndOwner(id, principal.getName());
        if (cashCard.isPresent()) {
            this.cashCardRepository.delete(cashCard.get());

            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
