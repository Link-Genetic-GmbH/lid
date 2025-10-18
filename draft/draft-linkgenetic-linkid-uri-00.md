---
title: The LinkID (linkid) URI Scheme
abbrev: LinkID URI
docname: draft-linkgenetic-linkid-uri-00
category: std
ipr: trust200902
area: ART
workgroup: ARTAREA
keyword: [URI, Persistent Identifier, Web, IANA, LinkID]
date: 2025-09-18

author:
- name: Christian Nyffenegger
  org: Link Genetic GmbH
  email: info@linkgenetic.com
  country: CH
---

# Abstract

This document defines the `linkid` URI scheme, a persistent identifier
for Web resources that resolves through HTTPS-based resolvers.
It is intended as a general-purpose complement to existing identifier
systems such as DOI, Handle, and ARK, enabling stable linking across
the entire Web.

# Introduction

Hyperlinks are fundamental to the Web, but they are fragile. As
resources move or disappear, users frequently encounter the
"404 Not Found" error. This phenomenon, commonly called *link rot*,
undermines trust, accessibility, long-term preservation, and even
sustainability due to duplicated storage and repeated network traffic.

Several persistent identifier systems exist today, including DOI,
Handle, and ARK. However, their adoption is mostly limited to
specific communities (e.g., academic publishing, libraries). The Web
as a whole lacks a universal, Web-native persistent identifier scheme.

The `linkid` URI scheme aims to fill this gap by providing stable,
location-independent identifiers that always resolve to the current
best representation of a resource.

# Conventions and Terminology

The key words “MUST”, “MUST NOT”, “REQUIRED”, “SHALL”, “SHALL NOT”,
“SHOULD”, “SHOULD NOT”, “RECOMMENDED”, “NOT RECOMMENDED”, “MAY”, and
“OPTIONAL” in this document are to be interpreted as described in
[RFC2119] as updated by [RFC8174] when, and only when, they appear in
all capitals, as shown here.

This document uses the following terms:

- **LinkID (linkid)**: A persistent, location-independent identifier
  assigned to a resource.
- **Client**: Software that dereferences a `linkid:` URI.
- **Resolver**: An HTTPS service that maps a LinkID to one or more
  current resource representations or redirect targets.
- **Registry**: An authority that issues LinkIDs and curates resolution
  metadata.
- **Resolution Record**: A metadata entry describing a dereferenceable
  resource location and its attributes.
- **Tombstone**: A metadata record that explains why a LinkID no longer
  resolves (withdrawn, legal, superseded, etc.).
- **Issuer**: The registry or authority that minted the LinkID.

# Motivation and Use Cases

- **Academic and scientific references:** Long-lived identifiers for
  datasets, publications, and research outputs.
- **Government and legal documents:** Stable references to laws,
  policies, and contracts across decades.
- **Corporate and knowledge systems:** Durable identifiers for
  documents, manuals, or policies referenced internally and externally.
- **Sustainability:** Reduction of digital waste from repeated searches,
  duplicate storage, and broken cross-references.

# URI Scheme Definition

The `lid` URI has the following syntax:

```
lid:<id>[?<parameters>]
```

Where:

- `<id>`: an opaque, URL-safe string (32–64 characters), typically a
  UUID (without hyphens), cryptographic hash (SHA-256, SHA-3), or
  registry-issued identifier using characters [A-Za-z0-9._~-].
- `<parameters>`: optional query parameters for content negotiation
  (e.g., `?format=pdf&lang=en&version=3`).

## Syntax (ABNF)

The `lid` URI syntax is formally defined using ABNF as per [RFC5234]
and [RFC3986]:

```
lid-URI     = "lid:" lid-id [ "?" lid-params ]
lid-id      = 1*( lid-unreserved / pct-encoded )
lid-unreserved = ALPHA / DIGIT / "." / "_" / "~" / "-"

lid-params  = param *( "&" param )
param       = pname [ "=" pvalue ]
pname       = 1*( pchar )
pvalue      = *pchar

; pchar from RFC3986 (minus "&" to avoid param separator collisions)
pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
pct-encoded = "%" HEXDIG HEXDIG
sub-delims  = "!" / "$" / "&" / "'" / "(" / ")" / "*" /
              "+" / "," / ";" / "="
```

Notes:

- Parameter separator is `&`. Clients MAY accept `;` as an alternative
  separator for robustness but MUST NOT emit it.
- Parameter names are case-insensitive; parameter values are
  case-sensitive unless defined otherwise for a specific parameter.

## Parsing, Normalization, and Comparison

- The scheme `lid` is case-insensitive; it SHOULD be rendered in
  lowercase.
- The `<id>` component is treated as an opaque, case-sensitive string.
  Registries MAY impose additional constraints; clients MUST NOT.
- Percent-encoding MUST use uppercase hex digits. Normalization SHOULD
  remove unnecessary percent-encoding for unreserved characters.
- Parameters used for content negotiation (e.g., `format`, `lang`,
  `profile`, `version`) do not change the identity of the LinkID; they
  influence resolution outcomes. Equality of two `lid:` URIs is
  determined solely by the scheme and `<id>` after normalization.
- Parameter ordering is not significant; duplicate parameter names MUST
  be processed by taking the first occurrence and ignoring subsequent
  duplicates.

## Well-Known Parameters

The following parameters are reserved for interoperable content
negotiation and filtering:

- `format`: Desired media type or short token (e.g., `application/pdf`,
  `pdf`).
- `lang`: Preferred language as BCP 47 language tag (e.g., `en`, `fr-CH`).
- `version`: Integer or opaque version label (e.g., `3` or `v3`).
- `profile`: A profile URI per [RFC6906] (or short token negotiated with
  the resolver).

## Resolution Algorithm

Resolution occurs via HTTPS resolvers using the following algorithm:

1. **Resolver Discovery**: Clients discover resolvers through:
   - Well-known URI: `/.well-known/linkid-resolver`
   - DNS SRV records: `_linkid._https.<domain>`
   - Default resolver: `https://resolver.linkid.org/`

2. **Resolution Request**: Send HTTPS GET to resolver endpoint:
   ```
   GET /resolve/<id>?<parameters> HTTP/1.1
   Host: resolver.linkid.org
   Accept: application/linkid+json, text/html, */*
   ```

3. **Resolution Response**: Resolvers return either:
   - **HTTP 301/302 redirect** to current resource location
   - **HTTP 200** with `application/linkid+json` metadata record
   - **HTTP 404** if identifier not found
   - **HTTP 410** if identifier is permanently gone

## Resolver Discovery

Clients obtain one or more resolver base URLs using any of the
following mechanisms (higher items RECOMMENDED first):

1. **Configured/default resolvers**: Implementations SHOULD ship with
   a curated default resolver list (e.g., `https://resolver.linkid.org/`).
2. **Well-known URI**: Fetch `GET /.well-known/linkid-resolver` on a
   resolver origin as per [RFC8615]. The resource returns
   `application/json` describing endpoints. Example:

   ```json
   {
     "issuer": "https://registry.example.org",
     "endpoints": {
       "resolve": "https://resolver.example.org/resolve/{id}",
       "metadata": "https://resolver.example.org/records/{id}"
     },
     "policies": {
       "httpsOnly": true,
       "rateLimit": {"limit": 1000, "window": "1m"}
     }
   }
   ```
3. **DNS records**: Discover resolvers via `_linkid._https` SRV records
   [RFC2782] and/or `HTTPS` service records [RFC9460].

## Resolution Protocol

Resolution occurs via HTTPS using the selected resolver base URL.

Resolution Request:

```
GET /resolve/<id>?<parameters> HTTP/1.1
Host: resolver.example.org
Accept: application/linkid+json, text/html, */*
Accept-Language: en, *;q=0.1
Prefer: return=representation
```

Resolution Responses:

- 303 See Other with `Location` pointing to the selected resource
  representation when the client indicates a preference for immediate
  dereference.
- 200 OK with `Content-Type: application/linkid+json` when returning
  metadata. Resolvers SHOULD include `Link` headers (see [RFC8288])
  such as `rel="describedby"`, `rel="alternate"`, and `rel="self"`.
- 404 Not Found when no record exists.
- 410 Gone when withdrawn; include tombstone metadata.
- 406 Not Acceptable if the client’s constraints cannot be satisfied.

Headers:

- Resolvers SHOULD include `Vary` reflecting inputs affecting selection,
  e.g., `Vary: Accept, Accept-Language, Prefer`.
- Resolvers SHOULD include `ETag`/`Last-Modified` on metadata responses
  and honor conditional requests (304 Not Modified).

## Metadata Format

The `application/linkid+json` media type contains resolution metadata:

```json
{
  "id": "b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14",
  "created": "2025-01-15T09:30:00Z",
  "updated": "2025-07-10T14:22:30Z",
  "issuer": "https://registry.example.org",
  "status": "active",
  "records": [
    {
      "uri": "https://content.example.org/v3/document.pdf",
      "status": "active",
      "mediaType": "application/pdf",
      "language": "en",
      "quality": 0.95,
      "validFrom": "2025-07-10T00:00:00Z",
      "validUntil": null,
      "checksum": {
        "algorithm": "sha256",
        "value": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
      },
      "size": 2047583,
      "lastModified": "2025-07-09T16:45:00Z"
    }
  ],
  "alternates": [
    {
      "scheme": "doi",
      "identifier": "10.1000/182"
    },
    {
      "scheme": "ark",
      "identifier": "ark:/12345/fk2test"
    }
  ]
}
```

### Field Semantics

- `id` (string, REQUIRED): The LinkID being resolved.
- `created`/`updated` (date-time strings, REQUIRED): Timestamps per
  RFC 3339.
- `issuer` (string, REQUIRED): URL of the issuing registry.
- `status` (string, REQUIRED): One of `active`, `withdrawn`,
  `superseded`.
- `records` (array, REQUIRED): Zero or more resolution records.
- `alternates` (array, OPTIONAL): Cross-system identifiers.

Record object fields:

- `uri` (string, REQUIRED): Dereferenceable HTTPS URL.
- `status` (string, REQUIRED): `active` or `deprecated`.
- `mediaType` (string, OPTIONAL): IANA media type.
- `language` (string, OPTIONAL): BCP 47 language tag.
- `quality` (number, OPTIONAL): Selection quality score [0.0, 1.0].
- `validFrom`/`validUntil` (date-time, OPTIONAL): Temporal validity.
- `checksum` (object, OPTIONAL): `algorithm` (e.g., `sha256`), `value`.
- `size` (integer, OPTIONAL): Octet size.
- `lastModified` (date-time, OPTIONAL): Server-known modification time.

### JSON Schema (abridged)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://linkid.org/schemas/linkid-metadata.json",
  "type": "object",
  "required": ["id", "created", "updated", "issuer", "status", "records"],
  "properties": {
    "id": {"type": "string"},
    "created": {"type": "string", "format": "date-time"},
    "updated": {"type": "string", "format": "date-time"},
    "issuer": {"type": "string", "format": "uri"},
    "status": {"enum": ["active", "withdrawn", "superseded"]},
    "records": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["uri", "status"],
        "properties": {
          "uri": {"type": "string", "format": "uri"},
          "status": {"enum": ["active", "deprecated"]},
          "mediaType": {"type": "string"},
          "language": {"type": "string"},
          "quality": {"type": "number", "minimum": 0, "maximum": 1},
          "validFrom": {"type": "string", "format": "date-time"},
          "validUntil": {"type": "string", "format": "date-time"},
          "checksum": {
            "type": "object",
            "required": ["algorithm", "value"],
            "properties": {
              "algorithm": {"type": "string"},
              "value": {"type": "string"}
            }
          },
          "size": {"type": "integer", "minimum": 0},
          "lastModified": {"type": "string", "format": "date-time"}
        }
      }
    },
    "alternates": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["scheme", "identifier"],
        "properties": {
          "scheme": {"type": "string"},
          "identifier": {"type": "string"}
        }
      }
    }
  }
}
```

## Error Handling

Resolvers MUST handle the following error conditions:

- **Invalid Identifier Format**: HTTP 400 with
  `application/problem+json` [RFC7807]; problem type
  `urn:linkid:error:invalid-id`.
- **Identifier Not Found**: HTTP 404 with problem details; responses
  MAY be cached for a short negative TTL (see Caching).
- **Identifier Withdrawn**: HTTP 410 with tombstone metadata and
  problem details.
- **Resolver Unavailable**: HTTP 503 with `Retry-After` header.
- **Rate Limiting**: HTTP 429 with `Retry-After`; problem type
  `urn:linkid:error:rate-limited`.

Error responses SHOULD include `Link` headers to documentation and
support pages where applicable.

## Caching and Performance

To optimize performance, resolvers and clients SHOULD implement:

- **HTTP Caching**: Use standard Cache-Control headers
- **CDN Integration**: Distribute resolution records globally
- **Resolver Redundancy**: Multiple resolver endpoints for failover
- **Client-side Caching**: Cache resolution results with TTL

Cache invalidation occurs through:
- Explicit cache purge when records update
- Time-based expiration using Cache-Control max-age
- ETag-based conditional requests

Additional caching requirements:

- Metadata responses SHOULD include strong validators (`ETag`) and
  `Cache-Control: public, max-age=60, stale-while-revalidate=30` as a
  starting point; operators MAY tune values.
- Redirect responses (303) MAY be cached per [RFC9111]; resolvers
  SHOULD supply an explicit `Cache-Control`.
- 404/410 responses are cacheable; negative caching durations SHOULD be
  bounded (e.g., `max-age=30`).
- Resolvers SHOULD emit `Vary: Accept, Accept-Language, Prefer` on
  metadata to prevent cache poisoning across variants.
- When records change, resolvers MUST change the `ETag` and SHOULD send
  `Cache-Control: no-cache` for a short period to ensure revalidation.

# Conformance

## Conforming Resolver

A conforming resolver:

- MUST support HTTPS per best practices.
- MUST implement GET `/resolve/{id}` with behavior described above.
- MUST validate identifier syntax and return appropriate errors.
- MUST support `Accept` negotiation between redirect and metadata.
- MUST include cache validators on metadata.
- MUST restrict redirects to `https` by default (see Security).
- SHOULD implement rate limiting and return 429.
- SHOULD expose a well-known resource.

## Conforming Client

A conforming client:

- MUST treat `<id>` as opaque and case-sensitive.
- MUST follow 303 redirects up to a reasonable limit and honor HTTPS
  requirements (see Security).
- SHOULD send `Accept` and `Accept-Language`.
- SHOULD cache metadata and redirects per caching guidance.
- MAY use multiple resolvers and apply failover.

# Extensibility and Versioning

- New metadata fields MAY be added with unknown fields ignored by
  clients.
- New parameters SHOULD avoid collisions by using clear names; for
  complex extensions, use `profile` [RFC6906] to signal semantics.
- Versioning of resources SHOULD use the `version` parameter and/or
  record-level validity windows; protocol-level breaking changes SHOULD
  be signaled by the well-known document.

# Internationalization Considerations

- Language preferences MUST use BCP 47 language tags.
- JSON values MAY contain Unicode; URIs in `records[].uri` MUST be
  valid ASCII URIs (IRIs MUST be converted per [RFC3987]).
- Parameter `lang` is matched using Lookup per BCP 47; resolvers SHOULD
  apply reasonable fallbacks.

# Interoperability

The `lid` scheme is designed to interoperate with existing Web
infrastructure (HTTP, DNS, CDNs, archives). It can coexist with DOI,
Handle, and ARK identifiers by cross-referencing or embedding them as
alternate resolution records.

## Integration with Existing Systems

- **DOI Integration**: LinkIDs can reference DOI records and vice versa
- **Handle System**: Compatible resolver architecture and metadata
- **ARK Integration**: Similar persistence guarantees and naming
- **Web Archives**: Integration with Wayback Machine and archive.org
- **Content Management**: APIs for WordPress, Drupal, enterprise CMS

# Security Considerations

- All resolution MUST use HTTPS.  
- Resolution records SHOULD be signed to prevent tampering.  
- Resolvers MUST protect against malicious redirects, phishing, or
  malware.  
- Identifiers MUST NOT embed personal or sensitive data.  

Additional considerations:

- Resolvers MUST NOT redirect to `http` (plaintext) destinations unless
  explicitly configured by the operator and requested by the client; the
  default policy is HTTPS-only.
- Implement input validation for `<id>` and parameters to prevent
  injection and SSRF via resolver internal fetches (if any).
- If signatures are used, JWS [RFC7515] or COSE [RFC8152] are
  RECOMMENDED; signed metadata SHOULD include issuer and issuance time.
- Apply anti-abuse controls (rate limiting, bot protection). Avoid
  leaking internal details in error bodies.

# Privacy Considerations

- Identifiers are opaque and contain no personal information.  
- Telemetry data MUST be opt-in, aggregated, and privacy-preserving.  

Additionally, resolvers SHOULD:

- Minimize collection of IP addresses and user agents; aggregate or
  truncate where feasible.
- Provide clear privacy notices for any analytics.

# IANA Considerations

IANA is requested to register the `linkid` URI scheme in the
“Uniform Resource Identifier (URI) Schemes” registry in accordance
with RFC 7595.

## URI Scheme Registration Template

- **Scheme name:** `linkid`  
- **Status:** Permanent  
- **Applications/protocols that use this scheme name:** LinkID for
  persistent identifiers, resolved via HTTPS.  
- **Contact:** Link Genetic GmbH <info@linkgenetic.com>  
- **Change controller:** IETF  
- **References:** This document, RFC3986, RFC7595  
- **Syntax:** `linkid:<id>[?<parameters>]`  
- **Semantics:** Persistent, location-independent identifiers resolved
  via HTTPS.  
- **Encoding considerations:** URL-safe characters, percent-encoding
  per RFC3986.  
- **Interoperability considerations:** Works alongside DOI, Handle,
  ARK, interoperable with HTTP.  
- **Security considerations:** HTTPS, signatures, anti-phishing.  
- **Privacy considerations:** No personal data, opaque IDs.  
- **Examples:**  
  - `linkid:b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14`  
  - `linkid:b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14?format=pdf&lang=en`

## Well-Known URI Registration

IANA is requested to register the following well-known URI per [RFC8615]:

- **URI suffix:** `linkid-resolver`  
- **Change controller:** IETF  
- **Specification document:** This document  
- **Status:** Permanent  

## Media Type Registration

IANA is requested to register the `application/linkid+json` media type:

- **Type name:** application  
- **Subtype name:** linkid+json  
- **Required parameters:** none  
- **Optional parameters:** profile  
- **Encoding considerations:** 8bit; JSON text per RFC 8259  
- **Security considerations:** See Security Considerations  
- **Interoperability considerations:** JSON; follows conventions similar
  to other metadata formats  
- **Published specification:** This document  
- **Applications that use this media type:** LinkID resolvers and
  clients  
- **Fragment identifier considerations:** As per [RFC6901] if used  
- **Additional information:** File extension: `.linkid.json` (optional)  
- **Person & email address to contact for further information:** Link
  Genetic GmbH <info@linkgenetic.com>  
- **Intended usage:** Common  
- **Restrictions on usage:** None  
- **Author:** Link Genetic GmbH  
- **Change controller:** IETF  

# References

* [RFC3986] T. Berners-Lee, R. Fielding, L. Masinter,
  *Uniform Resource Identifier (URI): Generic Syntax*,
  STD 66, RFC 3986, January 2005.

* [RFC3987] M. Duerst, M. Suignard,
  *Internationalized Resource Identifiers (IRIs)*, RFC 3987, January 2005.

* [RFC5234] D. Crocker, P. Overell, *Augmented BNF for Syntax
  Specifications: ABNF*, STD 68, RFC 5234, January 2008.

* [RFC6901] P. Bryner, *JavaScript Object Notation (JSON) Pointer*,
  RFC 6901, April 2013.

* [RFC6906] E. Wilde, *The 'profile' Link Relation Type*, RFC 6906,
  March 2013.

* [RFC7595] D. Thaler, T. Hansen, T. Hardie,
  *Guidelines and Registration Procedures for URI Schemes*,
  RFC 7595, June 2015.

* [RFC7807] M. Nottingham, E. Wilde, *Problem Details for HTTP APIs*,
  RFC 7807, March 2016.

* [RFC8174] B. Leiba, *Ambiguity of Uppercase vs Lowercase in RFC 2119
  Key Words*, RFC 8174, May 2017.

* [RFC8288] M. Nottingham, *Web Linking*, RFC 8288, October 2017.

* [RFC8615] M. Nottingham, *Well-Known Uniform Resource Identifiers
  (URIs)*, RFC 8615, May 2019.

* [RFC9110] R. Fielding, M. Nottingham, J. Reschke, *HTTP Semantics*,
  STD 97, RFC 9110, June 2022.

* [RFC9111] M. Nottingham, J. Reschke, *HTTP Caching*, RFC 9111,
  June 2022.

* [RFC2782] A. Gulbrandsen, P. Vixie, L. Esibov, *A DNS RR for
  specifying the location of services (DNS SRV)*, RFC 2782, February 2000.

* [RFC9460] B. Schwartz, M. Bishop, E. Nygren, *Service Binding and
  Parameter Specification via the DNS (SVCB and HTTPS RRs)*, RFC 9460,
  November 2023.

* [RFC7515] M. Jones, J. Bradley, N. Sakimura, *JSON Web Signature
  (JWS)*, RFC 7515, May 2015.
