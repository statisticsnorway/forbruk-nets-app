package no.ssb.forbruk.nets.health;


import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class LivenessReadinessController {

    @NonNull
    final ReadinessService readinessService;

    @GetMapping(value = "/alive")
    public ResponseEntity<String> alive() {
        return new ResponseEntity<>("I'm alive!", HttpStatus.OK);
    }

    @GetMapping(value = "/ready")
    public ResponseEntity<String> ready() {
        if (!readinessService.isReady()) {
            return new ResponseEntity<>("I'm not ready!", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ResponseEntity<>("I'm ready!", HttpStatus.OK);
    }

}

