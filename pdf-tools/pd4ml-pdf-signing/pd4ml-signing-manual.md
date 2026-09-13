# PD4ML PDF COS/Signing API - Programmer's Manual

_Available starting from PD4ML v4.1.1_

## 1. Architecture overview

This project is two layers in one Maven module:

* **`com.pd4ml.pdf.cos`** - a PDF COS (Carousel Object Structure) object model, parser, and writer, built independently rather than on top of an existing PDF library. This is the _only_ PDF-syntax code in the project; nothing else parses or serializes PDF bytes.
* **`com.pd4ml.pdf.sign`** - a digital-signing layer built entirely on top of `com.pd4ml.pdf.cos`. It never touches PDF bytes directly; it builds/mutates `COSDictionary`/`COSArray`/`COSStream` objects and hands them to `com.pd4ml.pdf.cos.writer` to serialize.

**Dependency direction is one-way**: `com.pd4ml.pdf.sign` depends on `com.pd4ml.pdf.cos`; `com.pd4ml.pdf.cos` never depends on `com.pd4ml.pdf.sign`. This means `com.pd4ml.pdf.cos` is independently usable as a general-purpose PDF reader/writer for anything, not just signing (see §2 and §7).

BouncyCastle (`bcpkix`/`bcprov`) is the only third-party dependency, used exclusively inside `com.pd4ml.pdf.sign` (CMS/PKCS#7, RFC 3161, OCSP) and `com.pd4ml.pdf.cos.security` (RC4/AES decryption of already-encrypted input). Nothing else in the project performs cryptography.

### 1.1 How a signature gets applied

A PDF signature is an **incremental update** (ISO 32000-1 §7.5.6): every byte of the original file is preserved unchanged; a `/Sig` dictionary, a signature field widget annotation, and (optionally) an appearance stream are _appended_ after them, followed by a small classic cross-reference table covering just the new/changed objects and a trailer whose `/Prev` points back to the original file's own cross-reference section.

```
[ original file bytes, untouched ] [ new objects ] [ new xref table ] [ new trailer, /Prev -> original xref ]
```

The generic mechanics of building that - allocating object numbers, tracking byte offsets as objects are written, emitting a correct xref + trailer - live in `com.pd4ml.pdf.cos.writer.IncrementalUpdateWriter` and know nothing about signatures specifically; `com.pd4ml.pdf.sign.pdf.IncrementalPdfSigner` builds on it, contributing only the signature-specific COS structures (the `/Sig` dict, widget, AcroForm wiring, appearance stream, DocMDP reference). `com.pd4ml.pdf.sign.ltv.LtvUpdater` is a second, independent consumer of the same `IncrementalUpdateWriter`, appending a _further_ incremental update (`/DSS`+`/VRI`) on top of an already-signed file.

The one deliberately special-cased part is `/Contents` (where the CMS signature bytes go) and `/ByteRange` (which bytes were hashed): both are written with a reserved-width placeholder first (fixed-width, so nothing downstream shifts when the real values are patched in), the file is otherwise finalized, `/ByteRange` is patched with the real offsets, the two covered byte spans are hashed and handed to a `ByteRangeSigner` (pure cryptography, no PDF-library involvement), and the resulting CMS bytes are hex-patched into `/Contents` at the same reserved offset.

### 1.2 Key abstraction: `ByteRangeSigner`

```java
public interface ByteRangeSigner {
    byte[] sign(byte[] dataToHash) throws PdfSigningException;
}
```

This is the entire boundary between "PDF/COS structure" and "cryptography" - `IncrementalPdfSigner` calls it exactly once, with exactly the bytes the signature is defined to cover, and gets back the raw CMS bytes to embed. The only implementation shipped is `CmsSigner` (package-private; reached only through `PdfSigner`), which itself branches three ways depending on where the private key actually lives (§3.3).

## 2. `com.pd4ml.pdf.cos` - object model, parsing, writing

### 2.1 The object model

| Type                      | Represents                                                             |
| ------------------------- | ---------------------------------------------------------------------- |
| `COSDictionary`           | a PDF dictionary (`<< /Key value ... >>`)                              |
| `COSArray`                | a PDF array (`[ ... ]`)                                                |
| `COSStream`               | a `COSDictionary` plus a byte stream (extends `COSDictionary`)         |
| `COSString`               | a PDF string, stored as raw bytes; `getBytes()`/`getString()`          |
| `COSName`                 | a PDF name (`/Foo`)                                                    |
| `COSInteger` / `COSFloat` | numbers (both extend `COSNumber`)                                      |
| `COSBoolean`              | `true`/`false`                                                         |
| `COSNull`                 | the `null` object (singleton `COSNull.NULL`)                           |
| `COSObject`               | an indirect reference (`N G R`) - `resolve()` follows it               |
| `COSObjectKey`            | an object number + generation pair, the identity of an indirect object |
| `COSDocument`             | the whole document: object table + trailer                             |

Every `COSBase` (the common superclass) can carry an assigned `COSObjectKey` (`getKey()`/`setKey()`) - that's what makes it an _indirect_ object when serialized; without one, it's written inline wherever it appears.

`COSDictionary` offers both indirection-transparent and indirection-aware accessors: `getItem(key)` returns the raw value (possibly a `COSObject` reference), `getDictionaryObject(key)` resolves it first. Typed convenience getters (`getString`, `getInt`, `getDictionary`, `getArray`, `getStream`, `getNameAsString`, ...) all resolve automatically and return a sensible default/`null` on a type mismatch rather than throwing.

### 2.2 Parsing

```java
COSDocument doc = COSParser.load(pdfBytes);                    // empty password
COSDocument doc = COSParser.load(pdfBytes, "ownerPassword");   // tried as user AND owner password
```

`COSDocument.isEncrypted()` reports whether the trailer named an `/Encrypt` dictionary; `getEncryptionError()` is non-null if a password was given but didn't authenticate (strings/streams are then still in their raw encrypted form). If encryption succeeded (or the document was never encrypted), every accessor throughout the API already returns plaintext.

`COSDocument.getCatalog()` resolves `trailer → /Root`. `getTrailer()` returns the trailer dictionary directly.

### 2.3 COS-path queries

```java
COSBase result = COSPathEvaluator.evaluate(doc, "/Root/Pages/Kids[0]/MediaBox"); // null on any miss
COSBase result = COSPathEvaluator.evaluateStrict(doc, "...");                    // throws COSPathException instead
```

A small path language: `/Root/...` starts from the catalog, `trailer/...` starts from the trailer dictionary directly, `[N]` indexes into an array, indirect references are followed transparently at every step. See `com.pd4ml.pdf.cos.path` for the full grammar (`COSPathParser`).

### 2.4 Writing

`com.pd4ml.pdf.cos.writer` is where new content gets serialized:

* **`COSWriter`** - a generic `ICOSVisitor<Void>` implementation: give it any `COSBase` and it writes correct PDF syntax for it, deciding reference-vs-inline per value (`instanceof COSObject` or `value.getKey() != null` → written as `"N G R"`; otherwise inlined). Public building-block methods: `writeIndirectObject(key, value)`, `writeValue(value)`, `writeName(...)`, `writeString(...)`, `writeHex(...)`, `formatFloat(float)` (static), plus raw `writeAscii`/`writeBytes` escape hatches and `position()` for exact byte-offset tracking.
*   **`IncrementalUpdateWriter`** - the generic incremental-update mechanics described in §1.1, with zero PDF-_semantic_ knowledge:

    ```java
    IncrementalUpdateWriter update = new IncrementalUpdateWriter(originalBytes, document);

    COSDictionary newThing = new COSDictionary();
    update.assignNewKey(newThing);              // gives it an indirect identity
    someExistingDict.setItem("Foo", newThing);  // wire it into the graph
    update.writeObject(someExistingDict);       // rewrite: it changed
    update.writeObject(newThing);               // write: it's new

    byte[] result = update.finish();            // writes xref + trailer, returns the whole file
    ```

    `writeObjectCustom(key, bodyWriter)` is the escape hatch for the rare case where a caller needs precise control over an object's own byte layout (this is exactly how `IncrementalPdfSigner` reserves the `/Contents`/`/ByteRange` placeholders - see its `writeSignatureBody`).

`com.pd4ml.pdf.cos.util.PageTree.findPage(catalog, index)` walks `/Root/Pages/Kids` (with an identity-based cycle guard) to resolve a zero-based page index to its `COSDictionary` - used by the signer to find where to attach a visible signature widget, but generally useful for anything page-indexed.

### 2.5 Filters and security

`com.pd4ml.pdf.cos.filter` implements `FlateDecode`, `ASCII85Decode`, `ASCIIHexDecode`, and PNG/TIFF predictors - `COSStream.getDecodedBytes()` applies the full `/Filter` chain automatically, stopping (and returning partially-decoded bytes) at the first _unsupported_ filter (typically an image codec like `DCTDecode`, which is expected to be handed to an image decoder directly rather than "decoded" as PDF filter data). `isFullyDecodable()` tells you in advance whether that'll happen.

`com.pd4ml.pdf.cos.security` implements the standard security handler (RC4 and AES, both legacy and AES-256 key derivation) for _reading_ an encrypted PDF. There is no encryption/write support - see §1 and §5 for why signing an encrypted PDF is refused rather than attempted.

## 3. `com.pd4ml.pdf.sign` - signing

### 3.1 Entry point: `PdfSigner`

```java
PdfSigner signer = new PdfSigner(); // stateless, thread-safe, reusable

// byte[] in, byte[] out
byte[] signedBytes = signer.sign(inputBytes, identity, options);

// File in, File out (input fully buffered first; output may equal input)
signer.sign(inputFile, outputFile, identity, options);

// Stream in, stream out (neither closed by this method)
signer.sign(inputStream, outputStream, identity, options);

// Full result (signed bytes + raw CMS bytes) -- needed for LTV, see §4
SignResult result = signer.signForResult(inputBytes, identity, options);
```

All four throw the single checked `PdfSigningException` for every failure mode (§5). A `PdfSigner` instance holds no mutable state; share one freely across threads.

### 3.2 `SigningOptions` and `VisibleSignatureOptions`

`SigningOptions` is a fluent builder-style configuration object for one `sign(...)` call:

| Method                                                | Default         | Notes                                                                                                           |
| ----------------------------------------------------- | --------------- | --------------------------------------------------------------------------------------------------------------- |
| `setReason(String)`                                   | none            | `/Reason`                                                                                                       |
| `setLocation(String)`                                 | none            | `/Location`                                                                                                     |
| `setContactInfo(String)`                              | none            | `/ContactInfo`                                                                                                  |
| `setSignerName(String)`                               | none            | `/Name`; also the default visible-appearance caption if no explicit text is set                                 |
| `setDigestAlgorithm(DigestAlgorithm)`                 | `SHA256`        | `SHA256`/`SHA384`/`SHA512`; JCA algorithm string (RSA vs. ECDSA suffix) derived from the certificate's key type |
| `setCertificationLevel(CertificationLevel)`           | `NOT_CERTIFIED` | see §3.5                                                                                                        |
| `setTsaUrl(String)`                                   | none            | enables RFC 3161 timestamping                                                                                   |
| `setTsaCredentials(user, pass)`                       | none            | optional Basic Auth for the TSA                                                                                 |
| `setVisibleSignatureOptions(VisibleSignatureOptions)` | none            | omit for an invisible signature                                                                                 |
| `setFieldName(String)`                                | `"Signature1"`  | AcroForm field name; give each co-signature a distinct name                                                     |
| `setSignaturePlaceholderSize(int)`                    | `16384` bytes   | see §3.6                                                                                                        |

`VisibleSignatureOptions`:

| Method                                  | Default                                                                                     |
| --------------------------------------- | ------------------------------------------------------------------------------------------- |
| `setPage(int)`                          | `0` (zero-based)                                                                            |
| `setRectangle(llx, lly, width, height)` | `36, 36, 200, 60`                                                                           |
| `setSignatureImage(File\|InputStream)`  | none                                                                                        |
| `setVisibleText(String)`                | none (falls back to a default "Digitally signed by ..." caption if there's no image either) |

### 3.3 Key sources: `CertificateUtils` and `SigningIdentity`

A `SigningIdentity` bundles a certificate chain with however its private key operations actually get performed:

```java
// PKCS#12 (.p12/.pfx) -- private key held in-process
SigningIdentity id = CertificateUtils.loadFromPkcs12(new File("signer.p12"), password);
SigningIdentity id = CertificateUtils.loadFromPkcs12(inputStream, password, "aliasName"); // explicit alias

// PKCS#11 (hardware token/HSM) -- key stays on the token
SigningIdentity id = CertificateUtils.loadFromPkcs11(new File("token.cfg"), pin);
SigningIdentity id = CertificateUtils.loadFromPkcs11(new File("token.cfg"), pin, "aliasName");
SigningIdentity id = CertificateUtils.loadFromPkcs11(preConfiguredProvider, pin, "aliasName");

// Cloud KMS -- no local private key at all
SigningIdentity id = CertificateUtils.forRemoteKey(certificateChain, remoteSigner);
```

Internally, `SigningIdentity` carries `getPrivateKey()` (null for a KMS identity), an optional `getProvider()` (the _specific_ JCA `Provider` instance that must perform any `Signature` operation with this key - set for PKCS#11, since the key is non-extractable and only usable through the exact provider that opened the token session), and an optional `getRemoteSigner()`. `CmsSigner` branches on these three states - see §3.4.

**PKCS#11 provider instantiation** (`CertificateUtils.loadFromPkcs11(File, ...)`) is JDK-version-portable by design: it tries the public Java 9+ `Provider.configure(String)` API first via reflection, and falls back to the legacy Java 8 `SunPKCS11(String)` constructor (also via reflection, to avoid a hard compile-time dependency on the internal `sun.security.pkcs11.SunPKCS11` class that newer JDKs' module system hides). This means the library compiles cleanly on any modern JDK despite targeting Java 1.8, and the resulting jar picks the right strategy at runtime based on whichever JDK actually runs it.

### 3.4 The cloud-KMS seam: `RemoteSigner`

```java
public interface RemoteSigner {
    byte[] sign(byte[] dataToSign, String jcaSignatureAlgorithm) throws Exception;
}
```

One method. `dataToSign` is the exact bytes to sign (never a pre-hashed digest - KMS vendors differ on digest/padding conventions, so the implementation controls that itself, typically by requesting the KMS's own "sign over this raw message" operation). `jcaSignatureAlgorithm` is e.g. `"SHA256withRSA"` or `"SHA256withECDSA"` - map it to your KMS's own signing-algorithm enum. The returned bytes must be exactly what a `java.security.Signature` configured with that algorithm would produce (DER `Ecdsa-Sig-Value` for EC, raw PKCS#1 v1.5 for RSA) - i.e. exactly what the equivalent KMS "sign" API call already returns.

No vendor SDK is bundled - this keeps the core dependency-free and avoids forcing a specific SDK version on every consumer regardless of which (if any) cloud they use. See `Ex09_CloudKmsSigning` for a runnable stand-in plus a real AWS KMS call shape in comments.

Internally, `CmsSigner` wraps `RemoteSigner` in a custom BouncyCastle `ContentSigner` (buffers the bytes BC wants signed; calls `RemoteSigner` only when BC asks for the final signature) fed into `JcaSignerInfoGeneratorBuilder` - the standard BC pattern for HSM/remote signing, letting BC assemble the CMS structure around a signature it never computed itself.

### 3.5 DocMDP certification

```java
options.setCertificationLevel(SigningOptions.CertificationLevel.NO_CHANGES_ALLOWED);
```

| Level                                  | DocMDP `/P` | Meaning                                                |
| -------------------------------------- | ----------- | ------------------------------------------------------ |
| `NOT_CERTIFIED`                        | -           | an ordinary signature (default)                        |
| `NO_CHANGES_ALLOWED`                   | 1           | no further changes of any kind                         |
| `FORM_FILLING_ALLOWED`                 | 2           | form filling (and further signing) still permitted     |
| `FORM_FILLING_AND_ANNOTATIONS_ALLOWED` | 3           | form filling, signing, and annotations still permitted |

Only valid on the first signature applied to a document - `IncrementalPdfSigner` throws `PdfSigningException` if `/Root/Perms/DocMDP` already resolves to an existing signature. Co-signing an already-signed but _uncertified_ document works normally (see `Ex07_CoSigningExistingDocument`); just give each signature its own `setFieldName(...)`.

### 3.6 Sizing the `/Contents` placeholder

The placeholder is reserved _before_ the real CMS bytes are known (see §1.1), so its size must be decided up front via `setSignaturePlaceholderSize(int)` (default 16384 bytes = a 32KB hex placeholder). Signing throws `PdfSigningException` with a precise message (actual vs. reserved size) if the real CMS blob doesn't fit - raise this for a long certificate chain and/or an RFC 3161 timestamp token (each adds several KB). Over-allocating is cheap (unused space is simply zero-padded, entirely inside the final, unsigned padding region).

### 3.7 Timestamps

```java
options.setTsaUrl("http://timestamp.digicert.com");
options.setTsaCredentials("user", "pass"); // if the TSA requires Basic Auth
```

`TimestampUtils` builds an RFC 3161 `TimeStampRequest`, POSTs it to the TSA, and `CmsSigner` attaches the resulting `TimeStampToken` as the unsigned CMS attribute `id-aa-signatureTimeStampToken` - this proves the signature existed at a specific time, independent of the signing certificate's own validity period, and is one building block of long-term validation (the other being LTV proper, §4).

## 4. `com.pd4ml.pdf.sign.ltv` - PAdES-LT (long-term validation)

LTV embeds the evidence a validator needs to check a signature's certificate chain for revocation _long after_ the signing certificate (or even a TSA's) may have expired: OCSP responses and/or CRLs, plus the certificate chain itself, in a `/DSS` (Document Security Store) dictionary at `/Root/DSS`, with a `/VRI` entry keyed by the hex-uppercase SHA-1 of the target signature's exact CMS bytes (ETSI TS 102 778-4 / ISO 32000-2 §12.8.4.3). This is applied as its **own** incremental update, layered on top of an already-signed PDF; it never touches the signature itself.

### 4.1 All-in-one: `LtvUpdater.addLtv`

```java
SignResult result = signer.signForResult(inputBytes, identity, options);
byte[] withLtv = LtvUpdater.addLtv(
        result.getSignedPdf(), result.getCmsSignature(),
        identity.getCertificateChain(), new LtvOptions());
```

Fetches OCSP/CRL live (via `RevocationFetcher`, §4.2) and embeds the result. `SignResult` (from `signForResult`, not the plain `sign` methods) is what makes this possible - it carries the exact raw CMS bytes needed for the `/VRI` key alongside the signed PDF.

### 4.2 `RevocationFetcher` and `LtvOptions`

```java
RevocationFetcher.Result revocation = RevocationFetcher.fetch(certificateChain, ltvOptions);
for (String warning : revocation.getWarnings()) { ... } // never throws; failures become warnings
```

For each certificate (skipping the self-signed trust anchor by default - see `LtvOptions.setIncludeRootCertificate`), extracts the OCSP responder URL (Authority Information Access extension) and CRL URL (CRL Distribution Points extension) via BouncyCastle ASN.1 parsing, and fetches both over plain `HttpURLConnection`. Never throws for a per-certificate failure (no extension present, unreachable host, non-2xx response, ...) - each becomes a string in `Result.getWarnings()`, and fetching continues with the rest.

`LtvOptions`: `setFetchOcsp`/`setFetchCrl` (both default `true`), `setCrlOnlyAsFallback` (default `false` - fetch both independently by default, for redundant evidence), `setIncludeRootCertificate` (default `false`), `setConnectTimeoutMillis`/`setReadTimeoutMillis` (default 10s each).

### 4.3 Lower-level: `LtvUpdater.embedValidationInformation`

For a caller with its own revocation infrastructure (a cache, a corporate OCSP proxy, pre-fetched responses, or revocation data recovered by some other means):

```java
RevocationFetcher.Result myOwnData = ...; // build it yourself
byte[] withLtv = LtvUpdater.embedValidationInformation(
        signedPdf, cmsSignatureBytes, certificateChain, myOwnData);
```

This is also how you add LTV to a PDF signed **earlier or elsewhere**, with no original `SigningIdentity`/`SignResult` in hand - recover the CMS bytes from the target signature's `/Contents` (`COSString.getBytes()` - already decoded, no hex-parsing needed) and the certificate chain from that CMS's own `SignedData` (match `SignerInformation.getSID()` against the embedded certificate set, then walk subject/issuer matches to build the ordered chain). See `Ex14_LtvOnExistingSignedPdf` for the complete, runnable version of this pattern (also what `pdfsigncli ltv` does internally).

### 4.4 Embedding details

* `/DSS/OCSPs`, `/DSS/CRLs`, `/DSS/Certs` are arrays of raw-byte streams, **deduplicated by content hash** - repeated LTV passes over the same document (e.g. adding LTV for a second signature later) don't re-embed byte-identical evidence.
* `/DSS/VRI` is keyed by `hex(SHA1(cmsBytes)).toUpperCase()`; each entry references the subset of `/OCSPs`/`/CRLs`/`/Certs` streams relevant to that specific signature.
* A `/DSS` that already exists (from a prior LTV pass, or written by another tool) is extended in place rather than replaced.

## 5. Error handling

Every checked failure from `com.pd4ml.pdf.sign`'s public API surfaces as a single type: `PdfSigningException` (`getCause()` carries the real underlying exception, if any - a `COSException`, `IOException`, BC exception, etc.). There's no need to catch anything else from a `sign(...)` or `LtvUpdater` call.

Failure modes worth knowing by name:

| Message (paraphrased)                                                                              | Cause                                                                                                                                                                                   |
| -------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| "Input PDF is encrypted; this signer ... does not support writing back into an encrypted document" | `com.pd4ml.pdf.cos` can decrypt while reading but has no re-encryption/write support (§1); thrown from `IncrementalPdfSigner`'s constructor, before any incremental update is attempted |
| "Document is already certified; only the first signature may certify it"                           | a second `setCertificationLevel(...)` call on an already-certified document                                                                                                             |
| "CMS signature (N bytes) exceeds the reserved placeholder (M bytes)"                               | §3.6 - raise `setSignaturePlaceholderSize(int)`                                                                                                                                         |
| "PDF has no indirect /Root (Catalog); cannot incrementally update it"                              | malformed/non-PDF input, or a `/Root` that was never made indirect                                                                                                                      |
| "No private key entry found in keystore" / "... on the PKCS#11 token"                              | wrong alias/password/PIN, or an empty keystore/token                                                                                                                                    |
| "Could not locate the base file's startxref"                                                       | the input's own cross-reference table is missing/unusable - not something an incremental update can safely build on                                                                     |

## 6. Threading and statelessness

`PdfSigner` and `CertificateUtils` hold no mutable state and are safe to share across threads; every call is independent. A `SigningIdentity` wraps a `PrivateKey`/`Provider`/`RemoteSigner` - safe to reuse across calls and threads as long as whatever it wraps is (a `java.security.PrivateKey` is; a PKCS#11 `Provider`'s thread-safety depends on the underlying PKCS#11 module - consult your token/HSM vendor if signing concurrently from multiple threads against the same token). `IncrementalUpdateWriter` and `IncrementalPdfSigner` instances are NOT thread-safe or reusable - each `sign(...)` call constructs fresh ones internally; you never construct them yourself.

## 7. Using `com.pd4ml.pdf.cos` standalone

Nothing about `com.pd4ml.pdf.cos` requires `com.pd4ml.pdf.sign` - it's usable on its own for any PDF-reading (or, via `com.pd4ml.pdf.cos.writer`, incremental-update-writing) task: form-field inspection/filling, metadata extraction, structural validation, adding annotations, or building your own incremental-update feature the same way `IncrementalPdfSigner` does. See `Ex17_CosInspectionAndPathQuery` for a signing-free usage example, §2 above for a quick object-model/parser/writer tour, and the [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) for a full standalone `com.pd4ml.pdf.cos` manual (object model, COS-path grammar, the writer, an incremental-update _editing_ recipe, filters, and reading encrypted PDFs) plus the [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) for the command-line tool covering all of it (`com.pd4ml.pdf.cos.cli.CosCli`) with no code required.

## 8. See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) - the core HTML/CSS-to-PDF engine `com.pd4ml.pdf.sign` and the rest of the post-processing toolkit sit alongside.
* [PD4ML Signing Examples](pd4ml-signing-examples.md) - the full set of 18 runnable use-case examples.
* [pd4signcli Reference](pd4signcli-reference.md) - command-line reference for `pdfsigncli`.
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) - standalone `com.pd4ml.pdf.cos` programmer's manual.
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) - command-line reference for `coscli` (`com.pd4ml.pdf.cos.cli.CosCli`).
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) - combining page ranges from multiple PDFs, sharing this same `com.pd4ml.pdf.cos` foundation.
* [PD4ML PDF Optimizer Manual](../pd4ml-pdf-optimizer/pd4ml-optimize-manual.md) - cleaning up unreferenced objects and incremental-update history.
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) - `com.pd4ml.pdf.xfdf`, importing/exporting annotations and form field values, sharing this same `com.pd4ml.pdf.cos` foundation.
