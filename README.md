# LinkID (`lid:`) — Persistent, Never-Break Identifiers for the Web

[![Spec Status](https://img.shields.io/badge/status-Community%20Draft-blue)](https://linkgenetic.github.io/lid/spec/)
[![CI](https://github.com/Link-Genetic-GmbH/lid/actions/workflows/build.yml/badge.svg)](https://github.com/Link-Genetic-GmbH/lid/actions)

---

## 🌍 Overview

**LinkID** (`lid:`) is a proposal for a **W3C/IETF standard** to provide *never-break* identifiers on the Web.  
It complements existing systems (DOI, ARK, Handle) but is designed for general use across documents, websites, and archives.

- Stable IDs even if resources move or change
- Resolver algorithm with semantic/AI support
- HTTP-based form (`https://w3id.org/linkid/...`) and optional `lid:` scheme
- Open ecosystem: reference resolvers, client SDKs, tests

---

## 📖 Specification

The Editor’s Draft is maintained in [`/spec/index.html`](spec/index.html).

➡️ For a human-friendly introduction, please see the [Explainer](docs/explainer.md).


---

## 🚀 Implementations

Reference resolvers:
- [`resolver/java`](resolver/java) — Java-based reference implementation
- [`resolver/node`](resolver/node) — Node.js prototype
- [`resolver/python`](resolver/python) — FastAPI prototype

Client SDKs:
- [`sdk/js`](sdk/js) — JavaScript client
- [`sdk/java`](sdk/java) — Java client
- [`sdk/python`](sdk/python) — Python client

---

## 🧪 Tests

Tests live under [`/tests`](tests/).  
We aim to provide **two independent implementations** and **Web Platform Tests (WPT)** for conformance.

---

## 📖 Explainer

We maintain a short, human-readable **[Explainer document](docs/explainer.md)**  
that introduces the motivation, goals, and design of **LinkID**.  

If you are new to this project, start there 👉 it explains **why LinkID matters**  
and how it complements existing persistent identifier systems like DOI, ARK, or Handle.

