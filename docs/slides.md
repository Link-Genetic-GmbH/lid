---
marp: true
theme: default
paginate: true
---

# LinkID: Resilient, Persistent Links for the Web

Link Genetic — W3C discussion session

---

## The Problem: Broken Links

- Links rot; content moves; formats change
- Trust, accessibility, and preservation suffer
- Costly re-searching and duplication

---

## LinkID at a Glance

- Persistent identifier (`linkid:`) or `https://w3id.org/linkid/...`
- Resolver maps LinkID → current target
- Open, federated registries; semantic metadata
- Complements DOI/ARK/UUID; interoperable by design

---

## Proposal Highlights

- URI scheme draft (IETF) and HTTP namespace
- Resolution API (redirect or metadata)
- Governance and persistence models

---

## Interoperability

- Alignment with DOI / ARK / UUID
- Mapping strategies and compatibility

---

## Roadmap

- Open-source reference implementation (this demo)
- SDKs (JS/Java/Python)
- Conformance tests and metadata framework

---

## Discussion & Next Steps

- Candidate WGs or Community Groups
- Feedback from TAG, URI, WebArch
- Standardisation pathways

---

## Live Demo

- GET `/.well-known/linkid-resolver`
- Resolve → redirect / metadata
- Register, update, withdraw


