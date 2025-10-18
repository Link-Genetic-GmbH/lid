# LinkID (`linkid:`) — Persistent, Never-Break Identifiers for the Web

[![Spec Status](https://img.shields.io/badge/status-Community%20Draft-blue)](https://linkgenetic.github.io/linkid/spec/)
[![CI](https://github.com/Link-Genetic-GmbH/linkid/actions/workflows/build.yml/badge.svg)](https://github.com/Link-Genetic-GmbH/linkid/actions)

---

## 🌍 Overview

**LinkID** (`linkid:`) is a proposal for a **W3C/IETF standard** to provide *never-break* identifiers on the Web.  
It complements existing systems (DOI, ARK, Handle) but is designed for general use across documents, websites, and archives.

- Stable IDs even if resources move or change
- Resolver algorithm with semantic/AI support
- HTTP-based form (`https://w3id.org/linkid/...`) and optional `linkid:` scheme
- Open ecosystem: reference resolvers, client SDKs, tests

---

## 📖 Explainer

We maintain a short, human-readable **[Explainer document](docs/explainer.md)**  
that introduces the motivation, goals, and design of **LinkID**.  

If you are new to this project, start there 👉 it explains **why LinkID matters**  
and how it complements existing persistent identifier systems like DOI, ARK, or Handle.

---


## 📄 IANA / IETF Draft

The current Internet-Draft for the proposed `linkid:` URI scheme is https://www.iana.org/assignments/uri-schemes/prov/linkid.

Maintained in [`/draft/draft-linkgenetic-linkid-uri-00.md`](draft/draft-linkgenetic-linkid-uri-00.md).

This folder contains Internet-Draft source files for the proposed
`linkid:` URI scheme.

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
- [`sdk/java`](sdk/java) — Java client (sample implementation)
- [`sdk/python`](sdk/python) — Python client (sample implementation)

### Sample client usage

Python (`sdk/python`):

```python
# pip install -r sdk/python/requirements.txt
from sdk.python import LinkIDClient

client = LinkIDClient()
result = client.resolve("b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14", metadata=True)

if hasattr(result, "uri"):
    print("Redirect to", result.uri)
else:
    print("Metadata payload", result.data)
```

Java (`sdk/java`):

```java
// mvn -f sdk/java/pom.xml package
import org.linkgenetic.linkid.LinkIdClient;
import org.linkgenetic.linkid.LinkIdClient.ResolveOptions;

LinkIdClient client = LinkIdClient.builder().build();
LinkIdClient.ResolutionResult result = client.resolve(
    "b2f6f0d7c7d34e3e8a4f0a6b2a9c9f14",
    ResolveOptions.builder().metadata(true).build()
);

switch (result) {
    case LinkIdClient.RedirectResolution redirect -> System.out.println("Redirect to " + redirect.uri());
    case LinkIdClient.MetadataResolution metadata -> System.out.println("Metadata: " + metadata.metadata());
    default -> System.out.println("Unknown result type");
}
```


---

## 🧪 Tests

Tests live under [`/tests`](tests/).  
We aim to provide **two independent implementations** and **Web Platform Tests (WPT)** for conformance.

---
