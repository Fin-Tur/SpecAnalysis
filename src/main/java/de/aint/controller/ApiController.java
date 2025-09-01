package de.aint.controller;

import de.aint.models.Isotop;
import de.aint.models.Spectrum;
import de.aint.models.Persistence.Roi.RoiDTO;
import de.aint.services.SpectrumService;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final SpectrumService service;

    public ApiController(SpectrumService service) {
        this.service = service;
    }


    @GetMapping
    public ResponseEntity<Spectrum> getSpectrum(@RequestHeader("X-Spectrum-Id") Long spectrumId) {
        Spectrum s = service.getSpectrumByID(spectrumId);
        return (s == null) ? ResponseEntity.noContent().build() : ResponseEntity.ok(s);
    }


    //Get "/smoothed"
    @GetMapping("smoothed")
    public ResponseEntity<Spectrum> getSmoothed(
            @RequestHeader("X-Spectrum-Id") Long spectrumId,
            @RequestParam(defaultValue = "SG") String algorithm,
            @RequestParam(name = "window_size", defaultValue = "0") @Min(0) int windowSize,
            @RequestParam(defaultValue = "0") @Min(0) int iterations,
            @RequestParam(defaultValue = "0") @Min(0) int sigma
    ) {
        Spectrum smoothed = service.getSmoothedById(spectrumId, algorithm, windowSize, iterations, sigma);
        return ResponseEntity.ok(smoothed);
    }


    @GetMapping("background")
    public ResponseEntity<Spectrum> getBackground(
            @RequestHeader("X-Spectrum-Id") Long spectrumId,
            @RequestParam(defaultValue = "original") String source
    ) {
        return ResponseEntity.ok(service.getBackgroundById(spectrumId, source));
    }


    @GetMapping("isotopes")
    public List<Isotop> isotopes(@RequestHeader(value = "X-Spectrum-Id", required = false) Long spectrumId) {
        return service.getIsotopes();
    }


    @GetMapping("custom")
    public ResponseEntity<Spectrum> getCustom(
            @RequestHeader("X-Spectrum-Id") Long spectrumId,
            @RequestParam(required = false) String isotopes,
            @RequestParam(defaultValue = "custom") String source
    ) {
        List<String> selected = List.of();
        if (StringUtils.hasText(isotopes)) {
            selected = Arrays.stream(isotopes.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        Spectrum result = service.getCustomById(spectrumId, source, selected);
        return (result == null) ? ResponseEntity.status(HttpStatus.BAD_REQUEST).build() : ResponseEntity.ok(result);
    }


    @GetMapping("peaks")
    public RoiDTO[] peaks(@RequestHeader("X-Spectrum-Id") Long spectrumId) {
        return service.getPeaksById(spectrumId);
    }

    //POST "/" -> File-Upload (Multipart "file") returns variants[0]
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }
        try {   //TODO : Streams
            File tmp = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tmp);
            Spectrum firstVariant = service.uploadAndParse(tmp);
            //Clean up temp
            if (!tmp.delete()) {
                log.warn("Could not delete temporary file: {}", tmp.getAbsolutePath());
            }
            return ResponseEntity.ok(firstVariant);
        } catch (Exception e) {
            log.error("Upload/Parse failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload/Parse failed: " + e.getMessage());
        }
    }

    @GetMapping("spectrum/delete")
    public ResponseEntity<Void> deleteSpectrum(@RequestHeader("X-Spectrum-Id") Long spectrumId) {
        service.delSpectrum(spectrumId);
        log.info("Deleted spectrum with ID : {}", spectrumId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("spectrum/rename")
    public ResponseEntity<Void> renameSpectrum(@RequestHeader("X-Spectrum-Id") Long spectrumId,
                                                @RequestParam("newName") String newName) {
        service.renameSpectrum(spectrumId, newName);
        log.info("Renamed spectrum with ID : {} to new name: {}", spectrumId, newName);
        return ResponseEntity.noContent().build();
    }

    //Clean error handling if spectrum is not loaded
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
