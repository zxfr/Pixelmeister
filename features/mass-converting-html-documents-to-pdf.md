# Mass-Converting HTML Documents to PDF

HTML, as a source format for PDF generation, has a structural constraint that is easy to overlook: the entire source document has to be fully read and laid out in memory before PDF output can begin. For ordinary documents -- a handful to a few hundred pages -- this is a non-issue. But once a single source document is expected to produce well over 1,000 PDF pages, converting it in one pass starts to strain both memory and throughput.

When the document's structure allows it, a better strategy is to split it into a series of smaller, independent HTML fragments, convert each one to PDF individually, and merge the resulting PDFs into a single final document. Bank and telecom account statements are the textbook use case: they're often extremely long, but their structure is repetitive -- a predictable sequence of per-account or per-page content blocks -- which makes them easy to split cleanly.

{% hint style="info" %}
This page was checked against the current PD4ML source rather than carried over unchanged. Two findings worth knowing before you use either version below:

1. The original v3 sample still runs essentially unmodified today. `org.zefer.pd4ml.PD4ML` and `org.zefer.pd4ml.PD4Document` are maintained as compatibility wrappers over the current engine, and `render(URL[], OutputStream)` plus `mergePDFs(...)` are both still present on them.
2. That said, the original sample's `pd4ml.setHtmlWidth(800)` call has **no effect** when combined with the multi-URL `render(URL[], OutputStream)` entry point -- this is documented on the method itself ("The method takes no effect for multi-URL PD4ML call") and was true even when the article was first written. It's harmless dead code in the sample, not a bug you introduced.

The v4 tab below isn't just a mechanical API translation -- it also takes advantage of `com.pd4ml.pdf.merge.PdfMerger`, which accepts an arbitrary number of sources per merge call. That removes the need for the original sample's binary-tree merge loop, and, as a side effect, restores `setHtmlWidth()` to actually taking effect, since v4 converts one document at a time rather than batching a whole chunk into a single `render()` call.
{% endhint %}

## How the chunking works

The sample class below requests a new HTML document at a time via `getNextDocument()`, converts documents in batches (`DOCS_PER_CHUNK`, 20 by default), and keeps each batch's PDF bytes in a list. Once every document has been converted, the batches are merged into a single output PDF.

`getNextDocument()` is meant to be overridden with something more realistic -- for example, pulling the next XML data portion from a database, transforming it to HTML, and returning it as a `StringReader`/`InputStream` rather than a filesystem `URL`.

Because the documents are converted independently of one another, both the per-document conversion step and the merge step parallelize cleanly, which is worth exploiting on multi-core hardware if conversion throughput matters.

{% tabs %}
{% tab title="v4" %}
```java
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.pd4ml.PD4ML;
import com.pd4ml.pdf.merge.PdfMerger;

public class MassConvert {

	private final static int DOCS_PER_CHUNK = 20;

	public URL getNextDocument() throws MalformedURLException {
		// to be overridden
		//
		// if source HTML documents are generated on-the-fly, it's probably a good
		// idea to change this to return an InputStream/StringReader instead, and
		// adjust the readHTML() call below correspondingly
		return new URL("file:/O:/work/testarea/AccountStatement.htm");
	}

	public int getDocumentNumber() {
		// to be overridden
		return 1000;
	}

	public static void main(String[] args) throws Exception {
		new MassConvert().convert();
	}

	public void convert() throws Exception {

		long start = System.currentTimeMillis();
		int docNumber = getDocumentNumber();

		byte[] merged = null;

		for (int chunkStart = 0; chunkStart < docNumber; chunkStart += DOCS_PER_CHUNK) {
			int chunkEnd = Math.min(chunkStart + DOCS_PER_CHUNK, docNumber);
			PdfMerger chunkMerger = new PdfMerger();

			for (int i = chunkStart; i < chunkEnd; i++) {
				URL url = getNextDocument();

				PD4ML pd4ml = new PD4ML();
				pd4ml.setHtmlWidth(800);
				pd4ml.readHTML(url);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				pd4ml.writePDF(baos);

				chunkMerger.addSource(baos.toByteArray());
			}

			byte[] chunkPdf = chunkMerger.mergeToBytes();

			if (merged == null) {
				merged = chunkPdf;
			} else {
				PdfMerger overallMerger = new PdfMerger();
				overallMerger.addSource(merged);
				overallMerger.addSource(chunkPdf);
				merged = overallMerger.mergeToBytes();
			}

			int perc = chunkEnd * 100 / docNumber;
			System.out.println(perc + "% " + (System.currentTimeMillis() - start) / 1000 + "sec");
		}

		System.out.println("done in " + (System.currentTimeMillis() - start) / 1000 + "sec");
		System.out.println("Resulting PDF size: " + merged.length + " bytes");

		File pdfFile = File.createTempFile("merge", ".pdf");
		try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
			fos.write(merged);
		}
	}
}
```
{% endtab %}

{% tab title="v3" %}
```java
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.zefer.pd4ml.PD4Document;
import org.zefer.pd4ml.PD4ML;

public class MassConvert {
	
	private final static int DOCS_PER_CHUNK = 20;
	
	private static ArrayList chunks = new ArrayList();
	
	public URL getNextDocument() throws MalformedURLException {
		// to be overridden
		//
		// if source HTML documents are generated on-a-fly, probably it is a good idea 
		// to change the method to
		// public StringReader getNextDocument()
		// and to adjust the rest of the class code correspondingly
		return new URL("file:/O:/work/testarea/AccountStatement.htm");
	}

	public int getDocumentNumber() {
		// to be overridden
		return 1000;
	}

	public static void main(String[] args) {
		MassConvert mc = new MassConvert();
		mc.convert();
	}
	
	public void convert() {
		
		long start = System.currentTimeMillis();
		int perc = 0;
		int oldperc = 0;
		
		byte[] pdf = null;

		int docNumber = getDocumentNumber();
		URL[] urls = new URL[DOCS_PER_CHUNK];
		
        try {

        	System.out.println("Converting chunks");
        	
        	for ( int i = 0; docNumber > i; i++ ) {
        		urls[i % DOCS_PER_CHUNK] = getNextDocument();
        		
                perc = i * 100 / docNumber;
                		    
            	if ( (i+1) % DOCS_PER_CHUNK == 0 || i == docNumber - 1 ) {
                	PD4ML pd4ml = new PD4ML();
                	pd4ml.setHtmlWidth(800);
                	ByteArrayOutputStream baos = new ByteArrayOutputStream();
                	pd4ml.render(urls, baos);
                	
                	pdf = baos.toByteArray();

                    if (perc / 10 > oldperc / 10) {
                        oldperc = perc;
                    	System.out.print((perc/10)*10 + "%");
                    	System.out.println(" " + 
                    			(System.currentTimeMillis() - start)/1000 + "sec");
                    }
                	
                	chunks.add(pdf);
                	pdf = null;
                	urls = new URL[DOCS_PER_CHUNK > docNumber - i ? 
                			docNumber - 1 - i : DOCS_PER_CHUNK];
            	}
        	}

        	System.out.println("Merging " + chunks.size() + " chunks");
        	
        	int i = 0;
    		ArrayList buf = new ArrayList();
        	while ( chunks.size() > 1 ) {
            	Iterator ii = chunks.iterator();
            	while ( ii.hasNext() ) {
            		pdf = (byte[])ii.next();
            		ii.remove();
                	if ( ii.hasNext() ) {
                		byte[] pdf2 = (byte[])ii.next();
                		ii.remove();
            			InputStream is1 = new ByteArrayInputStream(pdf);
            			InputStream is2 = new ByteArrayInputStream(pdf2);
            			ByteArrayOutputStream osMerge = new ByteArrayOutputStream();
            			PD4Document.mergePDFs(is1, is2, osMerge);
            			buf.add(osMerge.toByteArray());
                	} else {
                		buf.add(pdf);
                		break;
                	}

                	System.out.print('.');
                	if ( (i+1) % DOCS_PER_CHUNK == 0 ) {
                    	System.out.println(" " + 
                    			(System.currentTimeMillis() - start)/1000 + "sec");
                	}
            		i++;
            	}
            	ii = null;
            	chunks.clear();
            	chunks = buf;
            	buf = new ArrayList();
        	}

        	if ( chunks.size() != 1 ) {
        		// not likely
        		System.out.println("\nERROR?");
        	} else {
            	pdf = (byte[])chunks.get(0);
            	
                System.out.println("\ndone in " + 
                		(System.currentTimeMillis() - start)/1000 + "sec");
            	System.out.println("Resulting PDF size: " + pdf.length + "bytes");
            	
            	File pdfFile = File.createTempFile("merge", ".pdf");
            	
            	FileOutputStream fos = new FileOutputStream(pdfFile);
            	fos.write(pdf);
            	fos.close();
            	
                String params = "C:\\Program Files (x86)\\Adobe\\Reader 11.0\\Reader\\AcroRD32.exe " + 
                		pdfFile.getAbsolutePath(); 
                Runtime.getRuntime().exec(params);
        	}
        	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
```
{% endtab %}
{% endtabs %}

## Notes on the v4 rewrite

* **Per-document conversion instead of per-chunk.** v4's `PD4ML` no longer exposes a `render(URL[], OutputStream)`-style entry point -- conversion is always one `readHTML()` + `writePDF()` pair per document. `DOCS_PER_CHUNK` still controls how many individually-converted PDFs accumulate before being folded into the running `merged` result, which keeps a bound on how many PDF byte arrays are held in memory at once, but the memory savings now come from converting one document at a time rather than from anything at the merge stage.
* **`PdfMerger` replaces the binary-tree merge loop.** Because `PdfMerger.addSource(...)` accepts as many sources as you like before a single `mergeToBytes()`/`merge(OutputStream)` call, there's no need to pair documents off two at a time the way the original `mergePDFs()`-based loop did. Each chunk is merged in one call, and the accumulated result is folded into each new chunk with one more call.
* **`readHTML()`/`writePDF()` failures surface per document.** Unlike the original single `render(URL[], OutputStream)` call, which failed or succeeded for an entire chunk at once, each document's `readHTML()`/`writePDF()` pair can be wrapped individually if you want one bad source document to be skipped or logged rather than aborting the whole run.

## Example: launching the result

The original sample ends by writing the merged PDF to a temporary file and shelling out to a hard-coded Acrobat Reader path to open it -- convenient for a one-off demo run, but brittle across machines and Reader versions. If you want to open the result in whatever PDF viewer is actually registered on the current machine, see [Launching a PDF Viewer After Conversion](launching-a-pdf-viewer-after-conversion.md), which covers the portable, cross-platform ways of doing that.
