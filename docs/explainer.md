# Explainer: LinkID – Persistent, Never-Break Identifiers for the Web

**Authors**: Link Genetic GmbH
**Status**: Draft for Community Group discussion

---

## Problem

URLs are fragile. Content moves, domains expire, formats change. As a result, hyperlinks across the Web frequently break, leading to the widespread “404 Not Found” problem.
This undermines trust, accessibility, long-term preservation, and even sustainability: broken links force re-searching, duplicate storage, and wasted bandwidth.

Existing persistent identifier systems (e.g., DOI, Handle, ARK) address some of these challenges but are mostly scoped to specific domains (academic publishing, libraries). They are not widely adopted as a **general Web standard** for all resources.

---

## Goals

* Provide a **universal, Web-native identifier** that guarantees resolution even if the original resource moves or changes.
* Offer a **simple syntax** (e.g., `linkid:` scheme or HTTP-based `https://w3id.org/linkid/...`).
* Enable **resolvers** that map stable identifiers to current destinations using open, semantic, and AI-supported methods.
* Ensure compatibility with existing Web infrastructure (HTTP, DNS, browsers, archives).
* Support **sustainability claims** by quantifying avoided re-searching and duplication.

---

## Non-Goals

* Hosting or storing the actual content.
* Replacing existing persistent ID systems (DOI, ARK, Handle), but rather complementing and interlinking with them.
* Enforcing one central registry; federated/multi-stakeholder approaches are encouraged.

---

## Proposed Solution

* **Identifier Syntax**:

  * Option A: new URI scheme `linkid:<UUID or hash>`
  * Option B: HTTP namespace `https://w3id.org/linkid/<UUID>`
* **Resolver Module**:

  * Takes a `LinkID` and redirects (301/302) to the current target resource.
  * Uses semantic/AI-assisted rules to update mappings when sources move.
  * Maintains audit trails and optional CO₂-impact data.
* **Ecosystem Components**:

  * Open registry with versioned mappings.
  * Client libraries (JavaScript, Java, Python) for easy integration.
  * APIs for CMS, archives, and corporate systems.

---

## Security & Privacy Considerations

* **Data minimization**: identifiers carry no personal information.
* **Integrity**: mappings cryptographically signed to prevent tampering.
* **Abuse prevention**: resolvers must handle malicious redirects, phishing attempts, and provide safe-browsing checks.
* **Long-term trust**: governance under open standards, royalty-free patent policy.

---

## Alternatives Considered

* **Use only DOIs** – not feasible outside publishing, limited governance.
* **ARK/Handle** – strong in archival contexts but less mainstream on the Web.
* **HTTP 301 redirect chains** – brittle, often break without central stewardship.

---

## Stakeholders & Adoption

* **Browsers**: optional native resolution for `linkid:` scheme.
* **Content creators**: embed `LinkIDs` in documents, PDFs, websites.
* **Libraries & archives**: align with DOI, ARK, Handle communities.
* **Industry & governments**: ensure long-term referenceability of policies, contracts, knowledge bases.
* **Sustainability actors**: measure avoided CO₂ impact of broken links.

---

## References

* [W3C TAG Design Principles](https://www.w3.org/TR/design-principles/)
* [RFC 3986 – Uniform Resource Identifier (URI): Generic Syntax](https://www.rfc-editor.org/rfc/rfc3986)
* [RFC 7595 – Guidelines and Registration Procedures for URI Schemes](https://www.rfc-editor.org/rfc/rfc7595)
* [w3id.org Permanent Identifier Community Group](https://w3id.org/)

---

## Conference Session Overview

Broken links threaten the long-term integrity and trustworthiness of the Web.

The Link Genetic initiative introduces “LinkID”, a persistent, bidirectional identifier scheme designed to ensure link resilience, metadata preservation, and semantic interoperability across domains and over time.

This session invites W3C members to discuss the feasibility of a new `linkid:` URI scheme, alignment with existing identifier systems (DOI, ARK, UUID), and next steps toward standardisation.

We plan to:

- Identify potential Working Groups or Community Groups interested in hosting a LinkID proposal.
- Discuss governance, persistence, and interoperability models for resilient linking.
- Outline a roadmap for open-source reference implementation and metadata framework.

Goal(s): Gather feedback from W3C TAG, URI, and Web Architecture communities on the LinkID concept.

Agenda:

1. Introduction to Link Genetic & LinkID (5 min)
2. Challenges of link persistence and metadata loss (10 min)
3. Overview of the LinkID proposal and URI scheme draft (15 min)
4. Discussion: interoperability with DOI/ARK/UUID (20 min)
5. Next steps and standardisation pathways (10 min)

