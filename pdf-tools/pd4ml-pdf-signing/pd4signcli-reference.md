# pdfsigncli - Command-Line Reference

_Available starting from PD4ML v4.1.1_

`com.pd4ml.pdf.sign.cli.PdfSignCli` is a single command-line tool covering both layers of this project: `com.pd4ml.pdf.cos` (PDF inspection / COS-path queries) and `com.pd4ml.pdf.sign` (digital signing and PAdES-LT / LTV).

## Invocation

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli <command> [args...]
```

Throughout this document, `pd4ml.jar` stands for the pd4ml library jar and `bouncycastle-runtime-1.84.jar` for its companion Bouncy Castle runtime jar (see the project root's own `README.md`) - both assumed to be in the current directory in every example below. `com.pd4ml.pdf.sign.cli.PdfSignCli` isn't `pd4ml.jar`'s own `Main-Class` (that's `com.pd4ml.tools.Pd4Cmd`, a different tool), so it's run with `-cp`, not `-jar`; unlike the other CLI tools in this project, signing genuinely needs Bouncy Castle on the classpath - `-cp`, unlike `-jar`, does not follow a jar's own manifest `Class-Path` entry, so it has to be named explicitly here. (On Windows, use `;` instead of `:` to separate classpath entries.) Running with no arguments, or `help`/`--help`/`-h` as the first argument, prints the same usage summary this document expands on.

Four commands: **`inspect`**, **`fields`**, **`sign`**, **`ltv`**.

Flags use `--key=value` syntax (e.g. `--reason=Approved`); a flag with no `=value` (e.g. `--visible`, `--ltv`) is a boolean switch, true by its mere presence. There is no short-flag (`-r`) form.

### Exit codes

| Code | Meaning                                                                                                                                                                      |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0    | Success                                                                                                                                                                      |
| 1    | The operation failed for a business/runtime reason (bad password, malformed PDF, signing error, LTV chain-reconstruction failure, ...) -- the message on stderr explains why |
| 2    | Usage error (missing/invalid arguments or flags) -- usage text is printed to stderr alongside the message                                                                    |

***

## `inspect` - structural summary + COS-path queries

```
pdfsigncli inspect <input.pdf> [--password=...] [cos-path-expr ...]
```

Parses `input.pdf` with `com.pd4ml.pdf.cos.parser.COSParser` and prints:

* PDF version
* indirect object count
* whether the document is encrypted (and, if so, whether the given/empty password was accepted)
* page count (`/Root/Pages/Count`)
* whether an `/AcroForm` is present
* whether a `/DSS` (LTV validation store) is present

Each trailing positional argument (not starting with `--`) is evaluated as a **COS path expression** against the parsed document, and the result printed. COS path syntax mirrors a simplified filesystem-like path over the object graph:

| Expression                     | Meaning                                                                      |
| ------------------------------ | ---------------------------------------------------------------------------- |
| `/Root/Pages/Count`            | `trailer → Root → Pages → Count`, resolving indirect references at each step |
| `/Root/Pages/Kids[0]/MediaBox` | first kid's `/MediaBox` array                                                |
| `trailer/Size`                 | a value straight from the trailer dictionary (not via `/Root`)               |
| `trailer/ID[0]`                | the first element of the trailer's `/ID` array                               |

A stream value's decoded bytes (or raw bytes, if the filter chain isn't fully understood -- e.g. `DCTDecode` image data) are previewed, truncated to 200 characters.

**`--password=<pw>`** - tried as both the user and owner password if the PDF is encrypted; omit for an empty password (the common case for a document with no _open_ password but a restricted owner password).

### Examples

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli inspect report.pdf
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli inspect report.pdf "/Root/Pages/Count" "/Root/Pages/Kids[0]/MediaBox"
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli inspect protected.pdf --password=secret "trailer/Size"
```

***

## `fields` - list signature fields

```
pdfsigncli fields <input.pdf> [--password=...]
```

Walks `/Root/AcroForm/Fields`, printing every field of type `/Sig` (non-signature fields are skipped):

* field name (`/T`)
* `unsigned` (an empty field, ready to be filled by a future `sign` call using `--field=<that name>`) or `signed`
* for a signed field: `/SubFilter` (normally `adbe.pkcs7.detached`), signer name / reason / location / contact (whichever are present), the signing date (`/M`), the `/ByteRange` array, and -- if this is the document's certifying signature -- `CERTIFYING signature, DocMDP P=<level>`

If the AcroForm has no fields at all, or none of type `/Sig`, this says so plainly rather than printing nothing.

### Example

```bash
$ java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli fields signed.pdf
Field: Signature1
  status: signed
  subfilter: adbe.pkcs7.detached
  signer name: Jane Doe
  reason: Approved
  signed at: D:20260825102208+00'00'
  byte range: [0 1483 34251 516]
  CERTIFYING signature, DocMDP P=1
```

Useful before a co-signing (`sign --field=<new name>`) or LTV (`ltv --field=<name>`) call, to see what field names already exist.

***

## `sign` - apply a digital signature

```
pdfsigncli sign <input.pdf> <output.pdf> [options...]
```

### Key source (exactly one required)

| Flags                                                                | Source                                                                                                                                                |
| -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `--p12=<file>` `--password=<pw>` `[--alias=<a>]`                     | PKCS#12 (`.p12`/`.pfx`) keystore. `--alias` selects a specific entry; omitted, the first private-key alias found is used.                             |
| `--pkcs11-config=<file>` `--pkcs11-pin=<pin>` `[--pkcs11-alias=<a>]` | PKCS#11 hardware token/HSM, via a standard `SunPKCS11` config file (`name=...` / `library=...` / `slot=...`). The private key never leaves the token. |

Cloud KMS signing (`RemoteSigner`/`CertificateUtils.forRemoteKey`) is **not** a CLI feature -- a KMS client is inherently vendor-specific code that can't be expressed as a flag. Use the library API directly (see the [PD4ML Signing Manual](pd4ml-signing-manual.md) and [PD4ML Signing Examples](pd4ml-signing-examples.md)'s `Ex09_CloudKmsSigning`).

### Signature metadata

| Flag                              | Meaning                                                                                                                                                |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `--reason=<text>`                 | `/Reason`                                                                                                                                              |
| `--location=<text>`               | `/Location`                                                                                                                                            |
| `--contact=<text>`                | `/ContactInfo`                                                                                                                                         |
| `--signer=<text>`                 | `/Name`                                                                                                                                                |
| `--field=<name>`                  | AcroForm field name (default `Signature1`)                                                                                                             |
| `--digest=SHA256\|SHA384\|SHA512` | message digest (default `SHA256`); the JCA signature algorithm (RSA vs. ECDSA) is derived automatically from the certificate's key type                |
| `--placeholder=<bytes>`           | bytes reserved for the CMS blob (default `16384`); raise this for a long certificate chain and/or a timestamp -- see the Troubleshooting section below |

### Certification (DocMDP)

| Flag value                   | Effect                                                               |
| ---------------------------- | -------------------------------------------------------------------- |
| `--certify=NONE`             | (default) an ordinary signature, no certification                    |
| `--certify=NO_CHANGES`       | DocMDP P=1 -- no further changes of any kind permitted               |
| `--certify=FORM_FILL`        | DocMDP P=2 -- form filling (and further signing) still permitted     |
| `--certify=FORM_FILL_ANNOTS` | DocMDP P=3 -- form filling, signing, and annotations still permitted |

Only valid on the **first** signature applied to a document; certifying an already-certified document fails with a clear error.

### Timestamp (RFC 3161)

| Flag                                      | Meaning                                                |
| ----------------------------------------- | ------------------------------------------------------ |
| `--tsa=<url>`                             | TSA endpoint, e.g. `http://timestamp.digicert.com`     |
| `--tsa-user=<user>` `--tsa-password=<pw>` | optional HTTP Basic Auth credentials some TSAs require |

### Visible appearance

| Flag                          | Meaning                                                                       |
| ----------------------------- | ----------------------------------------------------------------------------- |
| `--visible`                   | enables a visible signature widget (omit entirely for an invisible signature) |
| `--page=<n>`                  | zero-based page index (default `0`)                                           |
| `--rect=llx,lly,width,height` | position/size in PDF points                                                   |
| `--image=<file>`              | stamp image (PNG/JPEG)                                                        |
| `--text=<text>`               | caption text (used alone, or below the image if both given)                   |

### LTV right after signing

| Flag                            | Meaning                                                                                                                 |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `--ltv`                         | fetch OCSP/CRL and embed PAdES-LT validation info immediately after signing, using the identity's own certificate chain |
| _(plus any `ltv` option below)_ | passed straight through                                                                                                 |

### Examples

Invisible, minimal:

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli sign in.pdf out.pdf --p12=signer.p12 --password=changeit
```

Visible, certified, timestamped:

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli sign in.pdf out.pdf \
     --p12=signer.p12 --password=changeit \
     --reason=Approved --location="Berlin, DE" --signer="Jane Doe" \
     --certify=NO_CHANGES --tsa=http://timestamp.digicert.com \
     --visible --page=0 --rect=36,36,220,70 --image=stamp.png --text="Jane Doe"
```

PKCS#11 token:

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli sign in.pdf out.pdf --pkcs11-config=token.cfg --pkcs11-pin=123456
```

Sign and add LTV in one call:

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli sign in.pdf out.pdf --p12=signer.p12 --password=changeit --ltv
```

***

## `ltv` - add long-term validation to an already-signed PDF

```
pdfsigncli ltv <input.pdf> <output.pdf> [--password=...] [--field=<name>] [ltv-options...]
```

Adds a `/DSS` (Document Security Store) + `/VRI` update to a PDF that was **already signed** -- by this tool, an earlier run, or any other PAdES-compliant signer -- as a second, independent incremental update. No original signing context (`SigningIdentity`, `SignResult`) is required: the certificate chain is reconstructed directly from the target signature's own embedded CMS `SignedData`.

**`--field=<name>`** selects which signed field to build LTV for, when the document has more than one signed field. If omitted and exactly one signed field exists, that one is used automatically; if omitted and more than one exists, the command fails and lists the available names.

### LTV options (shared with `sign --ltv`)

| Flag                     | Meaning                                                                                                                                                                                                      |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `--no-ocsp`              | skip OCSP fetching entirely                                                                                                                                                                                  |
| `--no-crl`               | skip CRL fetching entirely                                                                                                                                                                                   |
| `--crl-only-as-fallback` | only download a certificate's CRL when its OCSP responder was unreachable/absent (default: fetch both independently, for redundant evidence)                                                                 |
| `--include-root`         | also fetch/embed revocation info for the self-signed root/trust-anchor certificate (default: skipped -- a trust anchor is trusted by its presence in the verifier's trust store, not by a signature over it) |

Fetch failures for an individual certificate (no AIA/CRLDP extension, an unreachable responder, a non-2xx HTTP response, ...) are printed as warnings and do not abort the run -- LTV proceeds with whatever evidence was obtainable. If nothing could be fetched at all, a summary warning says so and only the certificate chain is embedded.

### Example

```bash
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli ltv signed.pdf signed-ltv.pdf
java -cp "pd4ml.jar:bouncycastle-runtime-1.84.jar" com.pd4ml.pdf.sign.cli.PdfSignCli ltv signed.pdf signed-ltv.pdf --field=Signature1 --include-root
```

***

## Troubleshooting

**"CMS signature (N bytes) exceeds the reserved placeholder"** - raise `--placeholder=<bytes>` on the `sign` call. A long certificate chain and/or an RFC 3161 timestamp can easily exceed the default 16 KB; 65536 is a safe generous value.

**"Document is already certified; only the first signature may certify it"** - `--certify=...` was used on a document that already has a DocMDP-certifying signature. Only the very first signature on a document may certify it; drop `--certify` for subsequent (co-)signatures.

**`ltv` says "Multiple signed fields found ... specify --field="** - run `fields` first to see the available names, then pass the right one.

**`ltv warning: no OCSP responses or CRLs were obtained`** - the certificate chain has no usable Authority Information Access / CRL Distribution Points extensions reachable from this network (common for internal/private CAs, or for a signing certificate deliberately issued without revocation info, or simply no network route to the responder from where this command runs). The chain is still embedded; only revocation _evidence_ is missing.

**Cloud KMS signing** - not available from `pdfsigncli`; see the library's `RemoteSigner` interface and `CertificateUtils.forRemoteKey` ([PD4ML Signing Examples](pd4ml-signing-examples.md)'s `Ex09_CloudKmsSigning`).

**Structural editing (not signing)** - `pdfsigncli inspect` reads and COS-path-queries a PDF but has no write-side commands for arbitrary COS edits (only `sign`/`ltv`, which write signature-specific structure). For generic dictionary/array edits (rotate a page, add a custom key, adjust metadata, ...), see the [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) (`com.pd4ml.pdf.cos.cli.CosCli`'s `set`/`delete` commands). For importing or exporting annotations and form field values via XFDF, see the [pd4xfdfcli Reference](../pd4ml-pdf-xfdf/pd4xfdfcli-reference.md) (`com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`).
