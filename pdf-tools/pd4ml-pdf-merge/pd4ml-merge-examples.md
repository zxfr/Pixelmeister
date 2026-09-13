# Examples

_Available starting from PD4ML v4.1.1_

All examples live under `src/test/java/com/pd4ml/pdf/merge/example/usecases/` (test scope -- not shipped in the distributed jar) and are ordinary runnable classes with a `main(String[] args)`. Easiest from an IDE: right-click the class, Run. From the command line:

```bash
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/test-classes;target/classes;$(cat cp.txt)" \
     com.pd4ml.pdf.merge.example.usecases.<ClassName> <args...>
```

Every example builds its own small sample PDF(s) in memory (no external fixture files needed) and was actually run end-to-end -- these aren't untested snippets.

| #  | Class                               | Demonstrates                                                                                                                 |
| -- | ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 01 | `Ex01_BasicTwoSourceMerge`          | The minimal call: select a page range from two sources and merge them. Start here.                                           |
| 02 | `Ex02_PageRangeGrammar`             | The full page-range grammar: `N`, `N-M` (descending too), `N+`, and `odd`/`even` as suppressing filters, with deduplication. |
| 03 | `Ex03_SingleSourceFilterAndReorder` | With exactly one source, `selectPages(...)` doubles as a filter/reorder pass, not just a subset tool.                        |
| 04 | `Ex04_SharedResourceDeduplication`  | Two pages of the same source sharing one font object are cloned exactly once in the output.                                  |
| 05 | `Ex05_EncryptedSourceMerging`       | Merging an encrypted source alongside a plain one -- transparent decryption via `addSource`'s password.                      |
| 06 | `Ex06_EncryptOutput`                | Encrypting the merged output, via the AES-256 shortcut or full `PdfEncryptor.Options` control.                               |
| 07 | `Ex07_TaggingReconciliation`        | Merging a tagged source with an untagged one -- best-effort `/StructTreeRoot` reconciliation.                                |
| 08 | `Ex08_ErrorHandlingPatterns`        | Every `PdfMergeException` failure mode and how to recognize it.                                                              |

For a ready-made command-line tool covering the same ground without writing any code, see the [pd4mergecli Reference](pd4mergecli-reference.md) and run `com.pd4ml.pdf.merge.cli.Pd4MergeCli`.

## See also

* [PD4ML PDF Merge Manual](pd4ml-merge-manual.md) -- the programmer's manual behind these examples.
* [pd4mergecli Reference](pd4mergecli-reference.md) -- command-line reference for `com.pd4ml.pdf.merge.cli.Pd4MergeCli`.
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) / [PD4ML COS Examples](../pd4ml-pdf-cos/pd4ml-cos-examples.md) -- standalone `com.pd4ml.pdf.cos` foundation this package is built on.
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) -- command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`.
* [PD4ML PDF Optimizer Manual](../pd4ml-pdf-optimizer/pd4ml-optimize-manual.md) / [PD4ML Optimizer Examples](../pd4ml-pdf-optimizer/pd4ml-optimize-examples.md) -- a natural next step after merging: clean up the result.
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) / [PD4ML XFDF Examples](../pd4ml-pdf-xfdf/pd4ml-xfdf-examples.md) -- another natural next step: bring in reviewers' annotations and form field values from an XFDF file.
