# AWS CodeArtifact Setup for Maven

This guide walks you through the process of configuring your Maven project to authenticate with and deploy artifacts to an AWS CodeArtifact repository, specifically for the `dis-insights` package. 
AWS CodeArtifact is a fully managed artifact repository service that makes it easy to securely store and share packages used in your software development process.

---

## Prerequisites

Before you begin, make sure you have completed the following prerequisites:

- **AWS Account**: You need an active AWS account with access permissions to the relevant CodeArtifact domain and repository.
- **AWS CLI**: Ensure that the AWS Command Line Interface (CLI) is installed and properly configured with credentials that have sufficient permissions (e.g., access to CodeArtifact).
- **Maven**: Apache Maven must be installed and available via your system’s `PATH` so it can be used from the command line.

---

## Step 1: Authenticate with AWS CodeArtifact

To interact with a CodeArtifact repository, you must first retrieve an authentication token using the AWS CLI. This token is temporary and required for Maven to authenticate requests.

Run the following command to retrieve and export the token as an environment variable:

```bash
export CODEARTIFACT_AUTH_TOKEN=$(aws codeartifact get-authorization-token \
  --domain salescode \
  --domain-owner 008136251604 \
  --query authorizationToken \
  --output text)
```

This command fetches the token from the specified domain and makes it available to your terminal session via the `CODEARTIFACT_AUTH_TOKEN` environment variable.

---

## Step 2: Add Credentials to Maven settings.xml

To allow Maven to use the authentication token, you must add a `<server>` entry in your `settings.xml` file.

Add or update the following block:

```xml
<servers>
  <server>
    <id>salescode-dis-insights</id>
    <username>aws</username>
    <password>${env.CODEARTIFACT_AUTH_TOKEN}</password>
  </server>
</servers>
```

- The `<id>` must match the ID used in your repository and distribution configuration.
- The `username` is always `aws` for CodeArtifact.
- The `password` references the environment variable that holds your token.

---

## Step 3: Configure a Maven Profile for Repository Access

You need to add a Maven profile to define which repositories Maven should use to fetch dependencies. Add the following profile in the same `settings.xml` file:

```xml
<profiles>
  <profile>
    <id>salescode-dis-insights</id>
    <activation>
      <activeByDefault>true</activeByDefault>
    </activation>
    <repositories>
      <repository>
        <id>salescode-dis-insights</id>
        <url>https://salescode-008136251604.d.codeartifact.ap-south-1.amazonaws.com/maven/dis-insights/</url>
      </repository>
    </repositories>
  </profile>
</profiles>
```

Setting `activeByDefault` to `true` ensures that this profile is always used unless another is explicitly activated.

### Optional: Use a Mirror to Redirect All Repositories

If you want all Maven dependency requests to go through the CodeArtifact repository, you can configure a mirror. Add the following block in your `settings.xml`:

```xml
<mirrors>
  <mirror>
    <id>salescode-dis-insights</id>
    <name>Salescode Mirror</name>
    <url>https://salescode-008136251604.d.codeartifact.ap-south-1.amazonaws.com/maven/dis-insights/</url>
    <mirrorOf>*</mirrorOf>
  </mirror>
</mirrors>
```

> ⚠️ Note: Setting `mirrorOf` to `*` will override all other repository configurations. Use with caution if your project relies on multiple sources.

---


## Step 4: Configure Deployment Settings in `pom.xml`

To publish your project’s artifacts to AWS CodeArtifact, you must define a distribution repository in your `pom.xml` file:

```xml
<distributionManagement>
  <repository>
    <id>salescode-dis-insights</id>
    <name>salescode-dis-insights</name>
    <url>https://salescode-008136251604.d.codeartifact.ap-south-1.amazonaws.com/maven/dis-insights/</url>
  </repository>
</distributionManagement>
```

Make sure the `<id>` matches the one defined in your `settings.xml` servers and profiles.

---

## Publish Artifacts

To publish a Maven artifact with `mvn` to a CodeArtifact repository, you must also edit `~/.m2/settings.xml` and the project `pom.xml`.

If you haven't, create and store a CodeArtifact auth token in an environment variable as described in *Pass an auth token using an environment variable* to set up authentication to your CodeArtifact repository.

### Add a `<servers>` section to `settings.xml`:

```xml
<settings>
    ...
    <servers>
        <server>
            <id>codeartifact</id>
            <username>aws</username>
            <password>${env.CODEARTIFACT_AUTH_TOKEN}</password>
        </server>
    </servers>
    ...
</settings>
```

### Add a `<distributionManagement>` section to your project's `pom.xml`:

```xml
<project>
    ...
    <distributionManagement>
        <repository>
            <id>codeartifact</id>
            <name>codeartifact</name>
            <url>https://my_domain-111122223333.d.codeartifact.us-west-2.amazonaws.com/maven/my_repo/</url>
        </repository>
    </distributionManagement>
    ...
</project>
```

### Deploy the artifact:

```sh
mvn deploy
```

### Verify the package was published:

```sh
aws codeartifact list-package-versions --domain my_domain --domain-owner 111122223333 --repository my_repo --format maven \
  --namespace com.company.framework --package my-package-name
```

### Sample output:

```json
{
    "defaultDisplayVersion": null,
    "format": "maven",
    "namespace": "com.company.framework",
    "package": "my-package-name",
    "versions": [
        {
            "version": "1.0", 
            "revision": "REVISION-SAMPLE-1-C7F4S5E9B772FC",
            "status": "Published"
        }
    ]
}
```

---


## Final Notes

Once all the above steps are complete:

- You can build and install dependencies with Maven as usual (`mvn install`, `mvn package`, etc.).
- To deploy your project, run `mvn deploy`.

If you encounter authentication issues, ensure your AWS token is valid (tokens expire after 12 hours by default) and that your `settings.xml` configuration aligns with the expected repository values.

This setup ensures your project can securely fetch dependencies from and deploy artifacts to AWS CodeArtifact.