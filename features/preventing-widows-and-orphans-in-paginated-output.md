# Preventing Widows and Orphans in Paginated Output

A chart and its caption, or any other tightly related pair of elements, shouldn't be split across a page break -- with the image stranded at the bottom of one page and its caption orphaned alone at the top of the next. PD4ML offers two ways to keep content like this together, each with a different tradeoff between precision and portability.

## Conditional breaks: `ifSpaceBelowLessThan`

`<pd4ml:page.break ifSpaceBelowLessThan="N">` measures the vertical space remaining on the current page, in pixels, and only forces a break there if what's left is smaller than `N` -- in other words, if the content that follows wouldn't actually fit in the space that's left. Left in place unconditionally, a page break would waste whatever room remained even when the content would have fit; the conditional form only spends that break when it's actually needed:

```html
<pd4ml:page.break ifSpaceBelowLessThan="330">
Chart #4.2<br>
<img src="images/chart42.png" width="400" height="300">
```

Here, 330px covers a 300px-tall image plus roughly 30px for its caption -- the break only fires if less than that much space remains, keeping the chart and caption together on whichever page actually has room for both.

{% hint style="warning" %}
`ifSpaceBelowLessThan` is one of the extended, pre-v4 attributes of `<pd4ml:page.break>` -- the same family as the `pageFormat`/`htmlWidth` attributes covered in [Dynamic Page Format Changes Within a Document](dynamic-page-format-changes-within-a-document.md) and flagged in the [Usage Examples](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/usage-examples) page's Apply Page Breaks entry as not confirmed ported to v4 as of that writing. Confirm against the Javadoc/changelog for the PD4ML version actually in use before depending on it; the CSS-based approach below has no such version dependency.
{% endhint %}

## The version-independent alternative: `page-break-inside: avoid`

Wrapping the same content in a single-cell table and applying the standard CSS `page-break-inside: avoid` property to that table achieves the same practical outcome -- PD4ML won't break the page in the middle of the table's content -- using an ordinary CSS property rather than a PD4ML tag extension:

```html
<table style="page-break-inside: avoid">
<tr><td>
Chart #4.2<br>
<img src="images/chart42.png" width="400" height="300">
</td></tr>
</table>
```

Because this relies on nothing beyond standard CSS pagination support, it isn't subject to the same pre-v4/v4 uncertainty as `ifSpaceBelowLessThan` -- it's the safer default for new code, with the tag-attribute form remaining useful where its explicit pixel threshold gives finer control than `page-break-inside: avoid`'s all-or-nothing behavior, on a PD4ML build confirmed to still support it.

## See also

* [Dynamic Page Format Changes Within a Document](dynamic-page-format-changes-within-a-document.md) -- other pre-v4 `<pd4ml:page.break>` attributes and the same version caveat.
* [Usage Examples](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/usage-examples) -- the Apply Page Breaks entry, covering standard CSS-based page breaks alongside the proprietary tag.
