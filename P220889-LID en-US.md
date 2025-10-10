```
P220889 – System and Method for Resilient Identification and
Management of Hyperlinks
```
# TECHNICAL FIELD OF THE INVENTION

# The invention relates to the field of data processing and information management, in

# particular systems and methods for the stable, unambiguous, and long-term reliable

# identification, management, and assignment of hyperlinks, including their source and

# target resources, in a computer network.

# STATE OF THE ART

# Hyperlinks are the basic structure of the World Wide Web. However, conventional links

# refer directly to a URL (Uniform Resource Locator). A URL specifies the storage location

# of a target resource, including the host (server computer), its port (access interface), and

# the file path on the host. If the target resource is deleted or its structure changes, for

# example, by changing the storage location on the host, a "dead" link is created, i.e., the

# computer attempting to connect to the target resource returns a 404 error.

# Well-known approaches for referencing target resources are:

# - URL shorteners. These only generate alternative, shortened URLs, but do not solve the

# problem of reference integrity. An example of a URL shortening service is offered at bit.ly.

# - DOI systems (DOI = Digital Object Identifier). These work for scientific publications,

# but not for general web content or company documents.

# - Blockchain or hash approaches. These only secure certain versions, but are not very

# flexible in dynamic contexts.

# The approaches mentioned only consider the target address of a link's target resource.

# However, there is no solution that also uniquely manages the source address of the

# source resource. As a result, it has not yet been possible to trace the origin and usage

# context of a link in a technically resilient manner.


## SUBJECT OF THE INVENTION

# The invention provides a system, method, and computer program product for the resilient

# identification and management of hyperlinks using unique LinkID technology with source

# resource and target resource management and persistent identifier namespace. A

# "persistent identifier namespace" is understood to be a namespace that assigns a unique

# identifier in the form of a LinkID to each reference ("hyperlink" or "link") from a source

# resource to a target resource. These LinkIDs form a namespace. Unless otherwise

# specified in the context of the following description, the term "source resource" refers to

# the storage location (address) of the digital object forming the source, and the term

# "target resource" refers to the storage location of the digital object forming the target of

# the reference.

# The system according to the invention has the features of claim 1. Further embodiments

# of the system according to the invention are the subject of the claims dependent on claim

# 1. A system within the meaning of claim 1 is understood to be an arrangement of at least

# two computers connected to each other, whereby this connection allows an exchange of

# data between the computers. The connection may be wired or wireless. In the most

# relevant applications, the at least two computers are connected in a network, which may

# in particular be the Internet.

# The method according to the invention has the features of claim 7. A further embodiment

# of the method according to the invention is the subject of the claim dependent on claim

# 7.

# The computer program product according to the invention has the features of claim 8.

# The invention provides a LinkID system that manages both the source resource (original

# document, position, context) and the target resource (document, position, context) of a

# hyperlink. Each hyperlink is uniquely identified by a LinkID. The LinkID can contain or

# reference metadata about the source resource and the target resource. With the system

# according to the invention, changes to source resource or target resource objects remain

# traceable.

# In addition, the invention introduces a "persistent identifier" that becomes a "link

# identifier" ("LinkID"). Each hyperlink is thus given a unique identifier in the format:

# Namespace designation:<prefixed identifier>


# In an exemplary embodiment, the LinkID has the following format:

# lid:7e96f229-21c3-4a3d-a6cf-ef7d8dd70f

# where "lid" denotes the namespace created according to the invention.

# The LinkID represents a prefixed identifier that never breaks. The prefix 'lid:' defines the

# namespace and distinguishes the technology from conventional identification systems

# (DOI = Digital Object Identifier, URN = Uniform Resource Name, URL = Uniform Resource

# Locator). The suffixed identifier can be implemented as a UUID (Universally Unique

# Identifier), hash, or AI-generated signature.

# "Non-breaking" means that the identifier stability is guaranteed, regardless of changes to

# the original target resource. Unlike classic URLs, which "break" when moved/changed

# (i.e., generate 404 errors), the LinkID remains permanently valid. "Non-breaking"

# therefore means persistent, stable, unchangeable, permanently resolvable. Accordingly,

# the LinkID is a persistent, uniquely generated identifier that remains permanently valid

# regardless of changes to the original target resource and can always be resolved to a

# current target address. "Non-breaking" therefore means technical resilience against

# decay.

The system according to the invention may have the following features:

**Role and rights management** : Specific roles (e.g., administrators, authors, publishers) can be
defined to determine who is allowed to create or change source resource and target resource
entries.

**Offline and hybrid use:** LinkIDs can also be used in offline media (e.g., PDFs, QR codes, print)
and resolved again when accessed online.

**There are also significant advantages:** sustainability/green IT by avoiding redundant queries
and dead links, the creation of an open or proprietary ecosystem (analogous to DOI/ISBN), and
an architecture that is scalable to billions of links (cloud-native, distributed, with caching and
sharding).

**The technical solution also includes mechanisms for multiple destinations:**

A source resource can reference different target resources (e.g., language versions, mirror
servers, backups), and a target resource can be linked from multiple source resources.

An example of the multi-destination approach is when a document with the same LinkID refers
to different language versions. An offline example is when a LinkID embedded in a printed QR
code reliably refers to the target resource after scanning.


The system according to the invention comprises in particular:

**1. A LinkID generator** that generates a unique LinkID based on cryptographic hashing algorithms
and/or AI-supported context analysis.
**2. A source registry** that stores information about the source resource (source object) of a
hyperlink. This information may include, for example, one or more of the following: document ID,
position in a text, context description, version. The source registry enables traceability of where
a hyperlink was originally set.
**3. A destination registry** that stores target resources such as web pages, files, or database
entries and supports dynamic updating and, preferably, semantic search.
**4. A LinkID mapping** module that maintains the relationship between the source resource and
the target resource via the unique LinkID and preferably allows bidirectional queries, such as
"Which source resources refer to this target resource?" and "Which target resources were linked
from this source resource?"
**5. A resolver module** that translates a LinkID into one or more current target resources and uses
AI-supported semantic methods, in particular AI models and semantic search, to automatically
redirect broken links to valid targets. The resolver module, which acts as a namespace resolver,
recognizes and interprets the namespace prefix (e.g., "lid") and ensures that each request for a
LinkID is translated into the correct source resource-target resource mapping.

The resolver module is the component of the system that translates a requested LinkID into a
current target resource. The basic function of the resolver module can be summarized as follows:

- Input: LinkID (e.g., lid:7e96f229-21c3-4a3d-a6cf-ef7d8dd70f24).
- Processing: Query the registry for all known target resources associated with this LinkID.
- Output: Current, valid target resource (e.g., URL, file, archived copy).

The use of AI-supported semantic processes by the resolver module can be implemented as
follows (AI = artificial intelligence):

- If the original target address of the target resource is no longer accessible (HTTP error,
    timeout), an AI module is used.
- This analyzes the metadata of the original target resource (title, keywords, context of the
    document, domain structure).
- Semantic matching algorithms (e.g., natural language processing, vector space models)
    are used to identify alternative target resources that are equivalent or similar in content
    to the original target resource.


- Example 1: A scientific article has been published under a new URL. Since the target
    resource is no longer located at the original target address under these circumstances,
    the AI finds the same article via DOI or metadata.
- Example 2: A company website has been restructured. In such a case, the AI can identify
    the new page by text similarity and navigation structure.

The resolver module serves to ensure that users receive a valid, semantically equivalent target
resource despite the original target resource being unavailable. It thus prevents information loss
and enables consistent navigation in terms of content, even with dynamic or migrated content.

The invention has the following advantages:

- **Holistic management** : Both the origin (source resource) and the destination (target
    resource) are technically secured.
- **Traceability:** Increased traceability of where links originate and how they are used.
- **Data integrity** : Changes to source or target resources remain traceable.
- **Resilience:** Prevents loss of information due to dead links.
    **Scalability:** Can be used in global web and document infrastructures.
- **Security:** Auditability through cryptographic methods and versioning.
- **Sustainability** : Longer shelf life for digital resources.

In a preferred embodiment of the system according to the invention, the source registry
additionally stores context information and metadata such as document ID, position in the
document, and/or author.

Furthermore, it may be provided that the LinkID mapping module enables bidirectional queries
between the source resource and the target resource.

In a further embodiment of the invention, it may be provided that changes to the source resource
or target resource are stored in a versioned and auditable manner. The system according to the
invention may comprise an audit and recovery mechanism for this purpose. This enables changes
to be traced and ensures that users can track the status and validity of a link over time.

In a particularly preferred embodiment, the unique LinkID is formed as a persistent identifier in
the format 'lid:<prefixed identifier>’ where the prefix "lid:" defines the namespace and the
suffixed identifier is a non-breaking, unique identifier. The invention features a namespace
mechanism in every configuration, which can be implemented by storing all hyperlinks as LinkIDs
with the prefix 'lid:'. The LinkID created in this way enables unique, universal, and interoperable
referencing.


In a preferred embodiment, the identifier following the prefix is generated as a UUID,
cryptographic hash, or AI-based signature. A UUID (Universally Unique Identifier) has a
standardized 128-bit format that is generated according to known RFC standards (e.g., RFC 4122)
and ensures unique identification worldwide, regardless of the location and time of generation. A
cryptographic hash is a checksum generated using known hash functions (e.g., SHA-256, SHA-3)
that provides a deterministic, collisionresistant value calculated from the metadata of the source
resource and target resource. An AI-based signature is an identifier generated by machine
learning or semantic analysis. To obtain this, characteristics of the content (e.g., keywords,
context, semantic vectors) are extracted and combined into a stable identifier. An AI-based
signature has the advantage that even if the format changes or migration occurs, the signature
can still recognize the resource based on content similarity.

## BRIEF DESCRIPTION OF THE SIGNATURES

Fig. 1 shows a schematic view of the system according to the invention for the resilient
identification and management of hyperlinks.

## EXAMPLES OF IMPLEMENTATION

The following describes possible applications of the system, method, and computer program
product according to the invention.

1. Application in company documents

If Word or PDF files are created in an organization, they can contain LinkIDs instead of direct
URLs. Each LinkID refers bidirectionally to the original source resource (document, context) and
the target resource (website, file). When documents are migrated to a new archive system or
when the target resource is changed, the reference remains functional because the LinkID is
resolved to the current or archived target address via the registry. This means that the references
(hyperlinks) in the Word or PDF files created by the organization remain valid even after migration.

2. Web integration

Inventive LinkIDs are integrated into a company website or content management system (CMS)
instead of direct URLs. If the website structure is changed, e.g., when a subpage is moved or the
URL structure is changed, all references (hyperlinks) remain valid. The registry stores the new
target address of the target resource and ensures that existing hyperlinks do not become
obsolete.

3. Data archiving

Digital long-term archives (e.g., in libraries, government agencies, or scientific institutions) can
use LinkIDs according to the invention to ensure access to stored content. Even after decades,
archived versions of documents, websites, or files can be reconstructed using the LinkID. This
ensures the integrity and traceability of sources in the long term.


4. API (application programming interface) integration

External systems can access the Source and/or Destination Registry according to the invention
via standardized interfaces, such as REST or GraphQL APIs. This allows source resource-target
resource mappings to be queried in real time and integrated into third-party applications. For
example, Testing tools, search engines, or compliance systems automatically check whether
references (hyperlinks) are still valid and, if necessary, retrieve alternative resources.

6. Enterprise content management system (ECM)

If a document is stored in an enterprise content management system, such as Microsoft
SharePoint or Adobe Experience Manager, there may be a reference (hyperlink) within that
document that points to an external website. A LinkID is now generated for this reference by
automatically creating a LinkID in the "lid:" namespace when the document is saved (e.g.,
lid:7e96f229-21c3-4a3d-a6cfef7d8dd70f24). This LinkID is stored in the registry together with
metadata, namely metadata of the source resource, such as the document ID, position in the
document, context (paragraph/reference), and metadata of the target resource, such as the
original URL of a target website. If the reference is later called up in the document, the LinkID is
resolved by the system checking whether the original URL is still accessible. If the URL is not
accessible (HTTP 404 or timeout), the resolver module falls back on alternative stored target
resources, e.g., an archived version, a mirror server, or a semantic replacement resource
determined by AI. Versioning and an audit trail can be provided by storing each change to the
target resource, e.g., due to relocation of the website or change of file paths, in the destination
registry with a timestamp. This makes it possible to trace at any time which version of a target
resource was linked to the LinkID at what point in time. The advantage for users is that they can
continue to activate the same link in the document, usually by clicking on it, and always reach a
valid target resource, so that even years later, the original link between the source resource and
the target resource can be reconstructed via the reference (hyperlink). This improves compliance
and archiving. Finally, error messages are also reduced, if not eliminated, which is important
because every error message is associated with a failed attempt to retrieve data and
consequently with corresponding energy consumption, so that avoiding error messages also
reduces energy consumption (green IT).


## PATENT CLAIMS

1. System for resilient identification and management of hyperlinks, comprising:
- at least one client computer, which is configured is send an LinkID creation request to a server
computer,
- at least one server computer, comprising

a) A LinkID generator that generates a unique identification number (LinkID) for a hyperlink.

b) a source registry that stores information about a source resource of the hyperlink,

c) a destination registry that stores information about a destination resource of the hyperlink,

d) a LinkID mapping module that maintains the relationship between the source resource and the
destination resource using LinkID,

e) a resolver module that resolves the LinkID into a current destination resource, wherein the
resolver module uses AI-supported semantic methods;

wherein the server computer is configured to send the LinkID to the at least one client computer
or to resolve an already provided LinkID into a current target resource.

2. System according to claim 1, wherein the source registry additionally stores context
information and metadata such as document ID, position in the document, or author.
3. System according to claim 1 or 2, wherein the LinkID mapping module enables bidirectional
queries between the source resource and the target resource.
4. System according to any one of claims 1 to 3, wherein changes to the source resource or target
resource are stored in a versioned and auditable manner.
5. System according to any one of claims 1 to 4, wherein the unique LinkID is formed as a
persistent identifier in the format 'lid:<prefixed identifier>', wherein the prefix 'lid:' defines the
namespace and the suffixed identifier is a non-breaking, unique identifier.
6. System according to claim 5, wherein the suffixed identifier is generated as a UUID (Universally
Unique Identifier), cryptographic hash, or AI-based signature.
7. A method for managing hyperlinks, comprising the steps of:
- generating a unique LinkID, in particular by a server computer in response to a LinkID creation
request by a client computer connected to the server computer,
- storing the linkID with source and destination information in a registry,
- resolving the linkID into a current destination resource,
- providing a trace of the source resource,
- automatically redirecting to an alternative destination resource in the event of failure of an
original URL (Uniform Resource Locator).


8. Method according to claim 7, wherein each hyperlink is represented by a persistent identifier
in the format 'lid:<prefixed identifier>', which uniquely references both the source and destination
information.
9. Computer program product comprising instructions that, when executed by a computer, cause
the computer to perform the following steps:
- Generating a unique LinkID,
- storing the LinkID with source and destination information in a registry,
- resolving the LinkID into a current target resource,
- Providing traceability of the source resource,
- automatically redirecting to an alternative destination resource if the original URL (Uniform
Resource Locator) fails.

## SUMMARY

The invention relates to a system for the resilient identification and management of hyperlinks.
The system according to the invention comprises at least one client computer and at least one
server computer. The server computer comprises a LinkID generator that generates a unique
identification number (LinkID) for a hyperlink; a source registry that stores information about the
source resource of a hyperlink; a destination registry that stores information about the
destination resource of a hyperlink; a LinkID mapping module that maintains the relationship
between source and destination by means of LinkID; and a resolver module that resolves the
LinkID into a current destination resource, wherein the resolver module uses AI-supported
semantic methods. The invention also relates to a method for managing hyperlinks, comprising
the steps of: generating a unique LinkID; storing the LinkID with source and destination
information in a registry; resolving the LinkID into a current destination resource; providing
traceability of the source resource; automatically redirecting to an alternative resource in the
event of failure of the original URL.


- Fig.


