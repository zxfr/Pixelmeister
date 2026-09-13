# Using PD4ML with Apache Maven

PD4ML's Maven repositories host the artifacts needed to compile, test, package, run integration tests against, or deploy a PD4ML-based application via a standard Maven (or Maven-compatible: Gradle, Ivy, ...) build. Because PD4ML is commercially distributed rather than published to Maven Central, consuming it as a dependency requires two things beyond the ordinary `<dependency>` declaration: pointing the build at PD4ML's own repository, and, for one specific use case covered below, supplying license credentials to reach it. This page covers both the standard binary distribution and the source-code distribution used for stepping through PD4ML's own code in a debugger.

## The standard binary distribution

The dependency declaration itself is unremarkable -- groupId, artifactId, and a version range that resolves to "4.0.0 or newer":

```xml
<project>
...
<dependencies>
...
	<dependency>
		<groupId>com.pd4ml</groupId>
		<artifactId>pd4ml</artifactId>
		<version>[4.0.0,)</version>
	</dependency>
</dependencies>
</project>
```

What makes it resolvable at all is a repository entry in `settings.xml` (or, equivalently, a `<repositories>` block directly in the POM), naming PD4ML's own Maven repository as an additional artifact source. No credentials are required for this one -- it's openly reachable:

```xml
<settings>
  ...
  <profiles>
    <profile>
      <id>main</id>
      <repositories>
        <repository>
          <id>pd4ml</id>
          <name>PD4ML Repository</name>
          <url>https://pd4ml.tech/maven2/</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  ...
<activeprofiles>
    <activeprofile>main</activeprofile>
</activeprofiles>
```

{% hint style="info" %}
This page gives the repository URL as `https://pd4ml.tech/maven2/`, while the [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) (§2, Obtaining PD4ML) gives it as `https://pd4ml.com/maven2/`. Both domains appear elsewhere across pd4ml.com's own documentation (the same `.tech`/`.com` split shows up in a few Javadoc links referenced from other pages in this space), so it's worth confirming which one resolves for a given PD4ML release before wiring it into a build that needs to keep working unattended.
{% endhint %}

## The source-code distribution

A separate, credentialed repository additionally publishes source jars for PD4ML itself -- useful for stepping through PD4ML's own code in a debugger during integration work, rather than working blind against compiled class files. The dependency declaration is identical to the standard case above; it's the repository configuration that differs, both in URL and in requiring authentication:

```xml
<project>
	...
	<dependencies>
		...
		<dependency>
			<groupId>com.pd4ml</groupId>
			<artifactId>pd4ml</artifactId>
			<version>[4.0.0,)</version>
		</dependency>
	</dependencies>
</project>
```

```xml
<settings>
  ...
  <servers>
    ...
    <server>
      <id>pd4ml-src</id>
      <username>LOGIN</username>
      <password>PASSWORD</password>
    </server>
  </servers>
  ...
  <profiles>
    <profile>
      <id>main</id>
      <repositories>
        <repository>
          <id>pd4ml-src</id>
          <name>PD4ML Repository</name>
          <url>https://pd4ml.tech/maven2-src/</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
</settings>
  <activeprofiles>
    <activeprofile>main</activeprofile>
  </activeprofiles>
```

`LOGIN` and `PASSWORD` are the credentials tied to a PD4ML license, and the `<server>` entry's `id` (`pd4ml-src`) must match the `<repository>` entry's `id` for Maven to associate the two. Because `settings.xml` is plain text and often lives outside version control precisely to avoid leaking secrets, storing a real password in it unencrypted is poor practice regardless -- Maven's built-in [password encryption](http://maven.apache.org/guides/mini/guide-encryption.html) mechanism lets `<password>` hold an encrypted value instead, decrypted locally against a master password kept separately.

## See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) -- §2 (Obtaining PD4ML) covers the same repository from the manual's own setup walkthrough, including the Bouncy Castle dependencies a full build also needs.
* [Maven password encryption guide](http://maven.apache.org/guides/mini/guide-encryption.html) -- the official Apache Maven documentation referenced above.
