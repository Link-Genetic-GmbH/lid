---
title: The LinkID (lid) URI Scheme
abbrev: LinkID URI
docname: draft-linkgenetic-lid-uri-00
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

This document defines the `lid` URI scheme, a persistent identifier
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

The `lid` URI scheme aims to fill this gap by providing stable,
location-independent identifiers that always resolve to the current
best representation of a resource.

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


- `<id>`: an opaque, URL-safe string (32–64 characters), typically a
  UUID, cryptographic hash, or registry-issued identifier.
- `<parameters>`: optional query parameters for content negotiation
  (e.g., `?format=pdf&lang=en`).

Resolution occurs via HTTPS resolvers that return either:

1. An HTTP 3xx redirect to the current resource, or
2. A JSON metadata record (`application/linkid+json`) describing
   candidate resources.

# Interoperability

The `lid` scheme is designed to interoperate with existing Web
infrastructure (HTTP, DNS, CDNs, archives). It can coexist with DOI,
Handle, and ARK identifiers by cross-referencing or embedding them as
alternate resolution records.

# Security Considerations

- All resolution MUST use HTTPS.  
- Resolution records SHOULD be signed to prevent tampering.  
- Resolvers MUST protect against malicious redirects, phishing, or
  malware.  
- Identifiers MUST NOT embed personal or sensitive data.  

# Privacy Considerations

- Identifiers are opaque and contain no personal information.  
- Telemetry data MUST be opt-in, aggregated, and privacy-preserving.  

# IANA Considerations

IANA is requested to register the `lid` URI scheme in the
“Uniform Resource Identifier (URI) Schemes” registry in accordance
with RFC 7595.

## URI Scheme Registration Template

- **Scheme name:** `lid`  
- **Status:** Permanent  
- **Applications/protocols that use this scheme name:** LinkID for
  persistent identifiers, resolved via HTTPS.  
- **Contact:** Link Genetic GmbH <info@linkgenetic.com>  
- **Change controller:** IETF  
- **References:** This document, RFC3986, RFC7595  
- **Syntax:** `lid:<id>[?<parameters>]`  
- **Semantics:** Persistent, location-independent identifiers resolved
  via HTTPS.  
- **Encoding considerations:** URL-safe characters, percent-encoding
  per RFC3986.  
- **Interoperability considerations:** Works alongside DOI, Handle,
  ARK, interoperable with HTTP.  
- **Security considerations:** HTTPS, signatures, anti-phishing.  
- **Privacy considerations:** No personal data, opaque IDs.  
- **Examples:**  
  - `lid:b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14`  
  - `lid:b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14?format=pdf&lang=en`

# References

* [RFC3986] T. Berners-Lee, R. Fielding, L. Masinter,
  *Uniform Resource Identifier (URI): Generic Syntax*,
  STD 66, RFC 3986, January 2005.

* [RFC7595] D. Thaler, T. Hansen, T. Hardie,
  *Guidelines and Registration Procedures for URI Schemes*,
  RFC 7595, June 2015.
