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

## 📖 Explainer

We maintain a short, human-readable **[Explainer document](docs/explainer.md)**  
that introduces the motivation, goals, and design of **LinkID**.  

If you are new to this project, start there 👉 it explains **why LinkID matters**  
and how it complements existing persistent identifier systems like DOI, ARK, or Handle.

---


## 📄 IETF Draft

The current Internet-Draft for the proposed `lid:` URI scheme is
maintained in [`/draft/draft-linkgenetic-lid-uri-00.md`](draft/draft-linkgenetic-lid-uri-00.md).

You can also convert it using [IETF Author Tools](https://author-tools.ietf.org/).

This folder contains Internet-Draft source files for the proposed
`lid:` URI scheme.

## Why does the header look strange?

The draft files (e.g. `draft-linkgenetic-lid-uri-00.md`) use a
**YAML front-matter header** (`--- ... ---`) that is required by the
IETF [kramdown-rfc](https://github.com/cabo/kramdown-rfc) toolchain.

- On **GitHub**, this header is not recognized and is displayed like a
  Markdown table.
- This is expected and does **not** mean the draft is broken.
- The draft is valid for the [IETF Author Tools](https://author-tools.ietf.org/).

## How to render the draft

You can generate `.txt` (official format), `.xml`, and `.html` versions
from the Markdown source:

### Option A: Online
- Upload the `.md` file to [IETF Author Tools](https://author-tools.ietf.org/).

### Option B: Local build

pip install xml2rfc
xml2rfc draft-linkgenetic-lid-uri-00.md --text --html

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
