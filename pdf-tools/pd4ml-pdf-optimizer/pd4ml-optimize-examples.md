# Examples

_Available starting from PD4ML v4.1.1_

All examples live under `src/test/java/com/pd4ml/pdf/optimizer/example/usecases/` (test scope -- not shipped in the distributed jar) and are ordinary runnable classes with a `main(String[] args)`. Easiest from an IDE: right-click the class, Run. From the command line:

```bash
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/test-classes;target/classes;$(cat cp.txt)" \
     com.pd4ml.pdf.optimizer.example.usecases.<ClassName> <args...>
```

Every example builds its own small sample PDF in memory (no external fixture files needed) and was actually run end-to-end -- these aren't untested snippets.

| #  | Class                                 | Demonstrates                                                                                                                               |
| -- | ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| 01 | `Ex01_BasicCleanup`                   | The minimal call: clean up a PDF with real orphaned objects from an incremental-update edit. Start here.                                   |
| 02 | `Ex02_AlreadyCleanIsANoop`            | Optimizing an already-clean document reports zero removed objects -- a report, not an assumption.                                          |
| 03 | `Ex03_SharedResourceIsNotDuplicated`  | Two pages sharing one font object still share it after optimizing.                                                                         |
| 04 | `Ex04_EncryptedInputRequiresPassword` | Optimizing an encrypted input needs its password; the output is plain by default.                                                          |
| 05 | `Ex05_EncryptOutput`                  | Cleaning a PDF and re-encrypting the output in the same call.                                                                              |
| 06 | `Ex06_ErrorHandlingPatterns`          | Every `PdfOptimizeException` failure mode and how to recognize it.                                                                         |
| 07 | `Ex07_DuplicateContentDeduplication`  | Two originally-distinct but byte-identical font objects are merged into one by default; turning that off with `deduplicateContent(false)`. |

For a ready-made command-line tool covering the same ground without writing any code, see the [pd4optimizecli Reference](pd4optimizecli-reference.md) and run `com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli`.

## See also

* [PD4ML PDF Optimizer Manual](pd4ml-optimize-manual.md) -- the programmer's manual behind these examples.
* [pd4optimizecli Reference](pd4optimizecli-reference.md) -- command-line reference for `com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli`.
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) / [PD4ML COS Examples](../pd4ml-pdf-cos/pd4ml-cos-examples.md) -- standalone `com.pd4ml.pdf.cos` foundation this package is built on.
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) / [PD4ML Merge Examples](../pd4ml-pdf-merge/pd4ml-merge-examples.md) -- combining page ranges from multiple PDFs, a natural companion pass.
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) / [PD4ML XFDF Examples](../pd4ml-pdf-xfdf/pd4ml-xfdf-examples.md) -- importing annotations/form field values, which offers its own `--optimize` post-import cleanup delegating to this same package.
