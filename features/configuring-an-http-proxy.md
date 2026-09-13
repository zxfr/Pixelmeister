# Configuring an HTTP Proxy

When the source HTML a conversion needs to fetch -- or any image, stylesheet, or font it references -- sits behind a proxy, PD4ML can be told about it in either of two ways: the standard, JVM-wide proxy system properties Java itself defines, or a PD4ML-specific dynamic parameter that scopes the proxy to PD4ML's own outbound requests alone. Which one is appropriate depends entirely on whether anything else running in the same JVM also needs to talk to the network -- and, if so, whether it should go through the same proxy.

## JVM-wide proxy (system properties)

The most direct approach sets the same `http.proxy*` system properties any Java networking code -- not just PD4ML -- already honors. Setting them in code rather than on the command line (`-Dhttp.proxyHost=...`) makes sense when the proxy host/port is only known at runtime:

```java
if (proxyHost != null && proxyHost.length() != 0 && proxyPort != 0) {
    System.getProperties().setProperty("http.proxySet", "true");
    System.getProperties().setProperty("http.proxyHost", proxyHost);
    System.getProperties().setProperty("http.proxyPort", "" + proxyPort);
}
```

### Older JVM property names (deprecated)

Very old JVM releases expected the same three properties without the `http.` prefix. These names are deprecated on any reasonably current JVM, but are worth knowing if a legacy deployment target requires them:

```java
if (proxyHost != null && proxyHost.length() != 0 && proxyPort != 0) {
    System.getProperties().setProperty("proxySet", "true");
    System.getProperties().setProperty("proxyHost", proxyHost);
    System.getProperties().setProperty("proxyPort", "" + proxyPort);
}
```

Either form has the same underlying drawback: `System.getProperties()` is process-wide JVM state, not something scoped to PD4ML. Setting it affects every other piece of Java code sharing that JVM -- an application server's other deployed applications, for instance -- which may be surprising if those neighbors have their own, different networking requirements.

## Scoping the proxy to PD4ML only

When a proxy should apply to PD4ML's own HTTP requests specifically, without reaching into JVM-wide state that other code might depend on, PD4ML exposes the proxy host and port as a named dynamic parameter instead. The exact call has changed between major versions:

{% tabs %}
{% tab title="v4" %}
```java
import com.pd4ml.Constants;

pd4ml.setParam(Constants.PD4ML_HTTP_PROXY, proxyHost + ":" + proxyPort);
```
{% endtab %}

{% tab title="v3" %}
```java
import org.zefer.pd4ml.PD4Constants;

Map m = new HashMap();
m.put(PD4Constants.PD4ML_HTTP_PROXY, proxyHost + ":" + proxyPort);
pd4ml.setDynamicParams(m);
```
{% endtab %}
{% endtabs %}

The v4 form follows the single-named-parameter replacement for `setDynamicParams(Map)` described in the [v3 to v4 Migration Guide](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/pd4ml-v3-to-v4-migration-guide)'s API correspondence table -- though that same page flags an unresolved discrepancy in pd4ml.com's own documentation over whether `setDynamicParams()` was actually dropped from v4 at all. If `pd4ml.setParam(...)` doesn't have the intended effect on a given PD4ML v4 build, falling back to the v3-style `setDynamicParams(Map)` call is worth trying, and is exactly the kind of thing that Migration Guide caveat is there to flag.

## A more direct current-v4 alternative: `setHttpOptions(...)`

Checking directly against the current `com.pd4ml.PD4ML` source turns up a cleaner option than either dynamic-parameter form above: `setHttpOptions(proxy, basicAuthentication)` sets the proxy (and, in the same call, HTTP basic-auth credentials -- see [HTTP Authentication and Session Propagation](http-authentication-and-session-propagation.md)) directly, with no `Constants` key or `Map` involved. Pass `null` for the authentication argument if only the proxy is needed:

```java
pd4ml.setHttpOptions(proxyHost + ":" + proxyPort, null);
```

This sidesteps the `setParam`/`setDynamicParams` ambiguity discussed above entirely, and is the option worth reaching for first on a current PD4ML build.

## See also

* [HTTP Authentication and Session Propagation](http-authentication-and-session-propagation.md) -- `setHttpOptions(...)`'s other argument, plus session-ID and servlet-request propagation.
* [PD4ML v3 to v4 Migration Guide](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/pd4ml-v3-to-v4-migration-guide) -- background on the `setDynamicParams()`/`setParam()` API change and its documented ambiguity.
* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) -- §6.2 covers `readHTML(...)`'s URL-based overloads that a configured proxy would apply to.
