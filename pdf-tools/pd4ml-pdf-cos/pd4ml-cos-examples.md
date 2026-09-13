# Examples

_Available starting from PD4ML v4.1.1_

All examples live under `src/test/java/com/pd4ml/pdf/cos/example/usecases/` (test scope -- not shipped in the distributed jar) and are ordinary runnable classes with a `main(String[] args)`. Easiest from an IDE: right-click the class, Run. From the command line:

```bash
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/test-classes;target/classes;$(cat cp.txt)" \
     com.pd4ml.pdf.cos.example.usecases.<ClassName> <args...>
```

Every example builds its own small sample PDF in memory (no external fixture files needed) and was actually run end-to-end -- these aren't untested snippets.

| #  | Class                                         | Demonstrates                                                                                                                       |
| -- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| 01 | `Ex01_ParsingAndCatalog`                      | The minimal call: parse a PDF, walk trailer to Root (the Catalog). Start here.                                                     |
| 02 | `Ex02_CosPathQueries`                         | The COS-path query language: `/Root/...`, `[N]` array indexing, `evaluate` vs. `evaluateStrict`.                                   |
| 03 | `Ex03_ListingObjectsAndPages`                 | Enumerating every indirect object, and walking the page tree by index.                                                             |
| 04 | `Ex04_IncrementalUpdateEditing`               | Editing an existing PDF as a real incremental update via `IncrementalUpdateWriter` -- the same mechanism signing and LTV build on. |
| 05 | `Ex05_ReadingEncryptedPdf`                    | Reading an encrypted PDF: transparent decryption once given the right password.                                                    |
| 06 | `Ex06_FiltersAndDecodedStreams`               | `COSStream.getDecodedBytes()`/`isFullyDecodable()` and the `/Filter` chain.                                                        |
| 07 | `Ex07_BuildingAndWritingADocumentFromScratch` | Building a PDF entirely from the COS object model, then serializing it with `COSDocumentWriter` -- no template, no other library.  |
| 08 | `Ex08_ErrorHandlingAndRecovery`               | `COSParseException` for genuinely unparseable input, versus the brute-force recovery scan for a corrupt-but-recoverable one.       |

For a ready-made command-line tool covering the same ground without writing any code, see the [pd4coscli Reference](pd4coscli-reference.md) and run `com.pd4ml.pdf.cos.cli.CosCli`.

## See also

* [PD4ML COS API Manual](pd4ml-cos-api-manual.md) -- the programmer's manual behind these examples.
* [pd4coscli Reference](pd4coscli-reference.md) -- command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`.
* [PD4ML Signing Manual](../pd4ml-pdf-signing/pd4ml-signing-manual.md) / [PD4ML Signing Examples](../pd4ml-pdf-signing/pd4ml-signing-examples.md) -- `com.pd4ml.pdf.sign`, digital signing built on this same foundation.
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) / [PD4ML Merge Examples](../pd4ml-pdf-merge/pd4ml-merge-examples.md) -- combining page ranges from multiple PDFs.
* [PD4ML PDF Optimizer Manual](../pd4ml-pdf-optimizer/pd4ml-optimize-manual.md) / [PD4ML Optimizer Examples](../pd4ml-pdf-optimizer/pd4ml-optimize-examples.md) -- cleaning up unreferenced objects and incremental-update history.
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) / [PD4ML XFDF Examples](../pd4ml-pdf-xfdf/pd4ml-xfdf-examples.md) -- `com.pd4ml.pdf.xfdf`, importing/exporting annotations and form field values, also built on this same foundation.
