package org.linkgenetic.resolver.controller;

import org.linkgenetic.resolver.model.*;
import org.linkgenetic.resolver.service.RegistryService;
import org.linkgenetic.resolver.service.ResolverService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class ResolverController {

    private final ResolverService resolverService;
    private final RegistryService registryService;

    public ResolverController(ResolverService resolverService, RegistryService registryService) {
        this.resolverService = resolverService;
        this.registryService = registryService;
    }

    @GetMapping("/resolve/{id}")
    public ResponseEntity<?> resolve(
            @PathVariable String id,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String version) {

        Map<String, String> params = new HashMap<>();
        if (format != null) params.put("format", format);
        if (lang != null) params.put("lang", lang);
        if (version != null) params.put("version", version);

        ResolutionResult result = resolverService.resolve(id, params);

        if (result instanceof ResolutionResult.RedirectResult) {
            ResolutionResult.RedirectResult redirect = (ResolutionResult.RedirectResult) result;
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirect.getUri());
            headers.add("Link", String.format("<%s>; rel=\"canonical\"", "https://w3id.org/linkid/" + id));

            redirect.getHeaders().forEach(headers::add);

            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } else {
            ResolutionResult.MetadataResult metadata = (ResolutionResult.MetadataResult) result;
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/linkid+json");

            return new ResponseEntity<>(metadata.getRecord(), headers, HttpStatus.OK);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
        RegistrationResponse response = registryService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/resolve/{id}")
    public ResponseEntity<LinkIdRecord> update(
            @PathVariable String id,
            @RequestBody UpdateRequest request,
            @RequestHeader("Authorization") String authorization) {

        String issuer = extractIssuerFromToken(authorization);
        LinkIdRecord updated = registryService.update(id, request.getRecords(), issuer);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/resolve/{id}")
    public ResponseEntity<Map<String, Object>> withdraw(
            @PathVariable String id,
            @RequestBody WithdrawRequest request,
            @RequestHeader("Authorization") String authorization) {

        String issuer = extractIssuerFromToken(authorization);
        registryService.withdraw(id, request.getReason(), request.getContact(), issuer);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "withdrawn");
        response.put("id", id);
        response.put("message", "LinkID successfully withdrawn");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/linkid/{id}")
    public ResponseEntity<LinkIdRecord> getLinkId(@PathVariable String id) {
        LinkIdRecord record = registryService.get(id);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/issuer/{issuer}/linkids")
    public ResponseEntity<List<LinkIdRecord>> getByIssuer(@PathVariable String issuer) {
        List<LinkIdRecord> records = registryService.getByIssuer(issuer);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/stats")
    public ResponseEntity<RegistryService.RegistryStats> getStats() {
        RegistryService.RegistryStats stats = registryService.getStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/.well-known/linkid-resolver")
    public ResponseEntity<Map<String, Object>> wellKnown() {
        Map<String, Object> discovery = new HashMap<>();
        discovery.put("resolver", "LinkID Resolver Java");
        discovery.put("version", "1.0.0");
        discovery.put("endpoints", Map.of(
                "resolve", "/resolve/{id}",
                "register", "/register",
                "update", "/resolve/{id}",
                "withdraw", "/resolve/{id}"
        ));
        discovery.put("capabilities", List.of(
                "redirect",
                "metadata",
                "content-negotiation",
                "caching",
                "authentication"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Cache-Control", "max-age=86400");

        return new ResponseEntity<>(discovery, headers, HttpStatus.OK);
    }

    private String extractIssuerFromToken(String authorization) {
        return "example.org";
    }

    public static class UpdateRequest {
        private List<ResolutionRecord> records;

        public List<ResolutionRecord> getRecords() {
            return records;
        }

        public void setRecords(List<ResolutionRecord> records) {
            this.records = records;
        }
    }

    public static class WithdrawRequest {
        private String reason;
        private String contact;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getContact() {
            return contact;
        }

        public void setContact(String contact) {
            this.contact = contact;
        }
    }
}