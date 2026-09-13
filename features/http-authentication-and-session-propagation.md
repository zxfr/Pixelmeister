# HTTP Authentication and Session Propagation

When the HTML PD4ML is converting references resources that require authentication or live session state -- a protected image, a session-scoped fragment rendered only for a logged-in user -- PD4ML needs to be told how to reach them the same way the original browser request would have. This page covers the three distinct mechanisms PD4ML provides for that: reusing the current web application's own request/response, explicit HTTP basic authentication, and session-ID propagation, in both its URL-rewriting and cookie-based forms.

{% hint style="info" %}
This page was checked directly against the current `com.pd4ml.PD4ML` source rather than transcribed from older documentation as-is. Two corrections came out of that: `setSessionID(...)` -- covered below -- turns out to be a dedicated, purpose-built method that has existed since v3.0.0.b1, but was never mentioned on the original version of this page at all; and basic HTTP authentication has a current, direct API (`setHttpOptions(...)`) that supersedes the dynamic-parameter technique shown previously.
{% endhint %}

## Reusing the current web application's request/response

When PD4ML is converting a page that's part of the same running web application -- rather than an arbitrary external URL -- handing it the live servlet request and response lets it resolve dynamic, session-dependent resources exactly as that request would, without needing to reconstruct or propagate any authentication state explicitly:

```java
public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    PD4ML pd4ml = new PD4ML();
    pd4ml.useHttpRequest(request, response);
    // ... configure the conversion as needed ...
    pd4ml.readHTML(new URL(sourceUrl));
    pd4ml.writePDF(response.getOutputStream());
}
```

`useHttpRequest(...)` takes its two arguments as plain `Object`, accepting either a classic `javax.servlet.http.HttpServletRequest`/`HttpServletResponse` pair or a Jakarta EE 9+ `jakarta.servlet.http.*` pair -- the same javax/jakarta duality already covered for the JSP taglib itself in the Programmer's Manual's [§13](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/), so the same conversion code works unmodified against either servlet API generation.

## Basic HTTP authentication

For a source that sits behind HTTP basic authentication rather than being part of the same application, `setHttpOptions(proxy, basicAuthentication)` supplies the credentials as a plain `"login:password"` string -- and, in the same call, an optional proxy in `"host:port"` form, passing `null` for whichever of the two isn't needed:

{% tabs %}
{% tab title="v4" %}
```java
pd4ml.setHttpOptions(null, "mylogin:mypassword");
```
{% endtab %}

{% tab title="v3" %}
```java
Map m = new HashMap();
m.put(PD4Constants.PD4ML_BASIC_AUTHENTICATION, "mylogin:mypassword");
pd4ml.setDynamicParams(m);
```
{% endtab %}
{% endtabs %}

Because `setHttpOptions(...)` covers proxy configuration too, it's a more direct alternative to the dynamic-parameter approach shown on the [Configuring an HTTP Proxy](configuring-an-http-proxy.md) page for that use case as well -- worth revisiting if a codebase is still using `PD4ML_HTTP_PROXY` via `setParam(...)`/`setDynamicParams(...)`.

## Session ID propagation

PD4ML supports two different ways of carrying a session identifier through to the resources it fetches, and which one applies depends on how the target application tracks sessions in the first place.

### URL-rewriting style: `setSessionID(...)`

For an application that propagates its session via URL rewriting rather than cookies, `setSessionID(...)` tells PD4ML the current session ID once, and PD4ML appends it as a matrix parameter to every resource reference (image, stylesheet, ...) it requests from that point on -- in the form `resource.jpg;jsessionid=<sessionID>?other_args`. It must be called before `readHTML(...)`; calling it afterward throws `InvokeException`.

```java
pd4ml.setSessionID(sessionID);
```

The reserved variable name `jsessionid` can be overridden -- for a session-tracking scheme that expects a different parameter name -- via the `PD4ML_SESSIONID_VARNAME` dynamic parameter:

```java
pd4ml.setParam(Constants.PD4ML_SESSIONID_VARNAME, "customsessionidname");
pd4ml.setSessionID(sessionID);
```

### Cookie-based session propagation (any platform)

When the target application instead tracks its session via an ordinary cookie -- as most non-Java stacks do, and as Java web applications configured for cookie-based sessions also do -- `setCookie(name, value)` attaches that cookie to every outbound resource request PD4ML makes. The method's `value` parameter is the cookie's value alone; there's no need to append a `path=` attribute or otherwise construct a raw `Set-Cookie`-style string, as older examples of this technique sometimes showed:

```java
pd4ml.setCookie("JSESSIONID", sessionID);
```

The same call, with the platform's own session cookie name, is exactly how the same technique carries over outside Java -- PHP's default session cookie, for instance:

```java
pd4ml.setCookie("PHPSESSID", sessionID);
```

## See also

* [Configuring an HTTP Proxy](configuring-an-http-proxy.md) -- worth a look alongside `setHttpOptions(...)`'s proxy argument above.
* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) -- §13 covers the same javax/jakarta servlet-API duality for the JSP taglib that `useHttpRequest(...)` supports directly.
