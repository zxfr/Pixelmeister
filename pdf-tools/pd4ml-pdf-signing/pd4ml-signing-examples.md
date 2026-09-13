# Examples

_Available starting from PD4ML v4.1.1_

All examples live under `src/test/java/com/pd4ml/pdf/sign/example/usecases/` (test scope -- not shipped in the distributed jar) and are ordinary runnable classes with a `main(String[] args)`. Easiest from an IDE: right-click the class, Run. From the command line:

```bash
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/test-classes;target/classes;$(cat cp.txt)" \
     com.pd4ml.pdf.sign.example.usecases.<ClassName> <args...>
```

Every example was compiled against real BouncyCastle classes and, except where noted (live network calls, or a real PKCS#11 token this environment doesn't have), run end-to-end to produce a real signed PDF -- these aren't untested snippets.

| #  | Class                             | Demonstrates                                                                                                          |
| -- | --------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| 01 | `Ex01_InvisibleSigningBasic`      | The minimal call: PKCS#12 key, invisible signature. Start here.                                                       |
| 02 | `Ex02_VisibleSignatureWithText`   | A visible widget with wrapped text only, built-in Helvetica metrics.                                                  |
| 03 | `Ex03_VisibleSignatureWithImage`  | A visible widget with a stamp image (PNG/JPEG) plus a text caption.                                                   |
| 04 | `Ex04_DigestAlgorithmChoice`      | Choosing SHA-256 / SHA-384 / SHA-512; algorithm auto-derived from key type.                                           |
| 05 | `Ex05_RfcTimestamping`            | RFC 3161 trusted timestamp from a TSA, with optional Basic Auth.                                                      |
| 06 | `Ex06_DocMdpCertificationLevels`  | PDF "Certify" at all three DocMDP levels (P=1/2/3).                                                                   |
| 07 | `Ex07_CoSigningExistingDocument`  | Two independent signers on the same (uncertified) document.                                                           |
| 08 | `Ex08_Pkcs11HsmSigning`           | Signing with a PKCS#11 hardware token/HSM -- key never leaves the token.                                              |
| 09 | `Ex09_CloudKmsSigning`            | The `RemoteSigner` seam for cloud KMS (AWS/Azure/GCP), with a real AWS KMS sketch in comments.                        |
| 10 | `Ex10_CustomPlaceholderSize`      | Sizing the `/Contents` placeholder for a long chain + timestamp; the resulting error if it's too small.               |
| 11 | `Ex11_CustomFieldName`            | Naming the AcroForm signature field explicitly.                                                                       |
| 12 | `Ex12_StreamBasedSigning`         | The three equivalent call shapes: `File`, `InputStream`/`OutputStream`, `byte[]`.                                     |
| 13 | `Ex13_LtvImmediatelyAfterSigning` | Sign, then add PAdES-LT validation info in the same process.                                                          |
| 14 | `Ex14_LtvOnExistingSignedPdf`     | Add LTV to a PDF signed earlier/elsewhere -- chain recovered from the CMS itself, no original signing context needed. |
| 15 | `Ex15_ErrorHandlingPatterns`      | Every `PdfSigningException` failure mode and how to recognize it.                                                     |
| 16 | `Ex16_FullyLoadedCombinedExample` | Every major option combined: visible + certify + timestamp + LTV.                                                     |
| 17 | `Ex17_CosInspectionAndPathQuery`  | Using `com.pd4ml.pdf.cos` alone (no signing) to parse and COS-path-query a PDF.                                       |
| 18 | `Ex18_ListSignatureFields`        | Programmatically enumerating signature fields and their metadata.                                                     |

See also `com.pd4ml.pdf.sign.example.SignPdfExample` (`src/test/java/com/pd4ml/pdf/sign/example/SignPdfExample.java`) -- the original minimal single-file CLI sample (PKCS#12 + optional TSA only, real files expected), kept for the simplest possible reference.

For a ready-made command-line tool covering the same ground without writing any code, see the [pd4signcli Reference](pd4signcli-reference.md) and run `com.pd4ml.pdf.sign.cli.PdfSignCli`. For `com.pd4ml.pdf.cos` alone -- inspection, COS-path queries, object listing, and incremental-update editing, with no signing involved -- see the [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) (and its own examples) and run `com.pd4ml.pdf.cos.cli.CosCli`.

## See also

* [PD4ML Signing Manual](pd4ml-signing-manual.md) -- the programmer's manual behind these examples.
* [pd4signcli Reference](pd4signcli-reference.md) -- command-line reference for `com.pd4ml.pdf.sign.cli.PdfSignCli`.
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) -- standalone `com.pd4ml.pdf.cos` programmer's manual.
* [PD4ML COS Examples](../pd4ml-pdf-cos/pd4ml-cos-examples.md) -- runnable `com.pd4ml.pdf.cos` use-case examples.
* [PD4ML Merge Examples](../pd4ml-pdf-merge/pd4ml-merge-examples.md) / [PD4ML Optimizer Examples](../pd4ml-pdf-optimizer/pd4ml-optimize-examples.md) -- the same style of runnable examples for `com.pd4ml.pdf.merge` / `com.pd4ml.pdf.optimizer`.
* [PD4ML XFDF Examples](../pd4ml-pdf-xfdf/pd4ml-xfdf-examples.md) -- the same style of runnable examples for `com.pd4ml.pdf.xfdf` (annotation/form field import and export).
