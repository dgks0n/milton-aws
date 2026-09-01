# Milton AWS

[![CI](https://github.com/dgks0n/milton-aws/actions/workflows/ci.yml/badge.svg)](https://github.com/dgks0n/milton-aws/actions/workflows/ci.yml)

Milton AWS is a Java storage adapter that exposes Amazon S3 objects and
DynamoDB metadata through a [Milton](https://milton.io/) WebDAV resource
controller.

The 2.x codebase uses Java 17, Milton 4, and AWS SDK for Java 2.x. It does not
bundle a logging implementation or load credentials from project resources.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- An AWS identity allowed to manage the configured S3 bucket and its matching
  DynamoDB table

The default controller reads configuration in this order:

| Setting | Java system property | Environment variable | Default |
| --- | --- | --- | --- |
| AWS region | `milton.aws.region` | `AWS_REGION` | `us-west-2` |
| Bucket/table name | `milton.aws.bucket` | `MILTON_AWS_BUCKET` | `milton-s3-demo` |

AWS credentials come from the SDK's default credentials provider chain. Use an
IAM role, web identity, AWS profile, or the standard AWS environment variables.
Never put credentials in this repository or on the application classpath.

## Use

Add the snapshot dependency while developing locally:

```xml
<dependency>
  <groupId>io.milton</groupId>
  <artifactId>milton-aws</artifactId>
  <version>2.0.0-SNAPSHOT</version>
</dependency>
```

Milton can instantiate `AmazonS3Controller` through its no-argument
constructor. Applications that manage their own AWS clients can instead inject
an `AmazonStorageService`:

```java
AmazonStorageService storage = new AmazonStorageServiceImpl(Region.US_WEST_2);
AmazonS3Controller controller = new AmazonS3Controller(storage, "my-webdav-bucket");
```

The S3 bucket and DynamoDB table intentionally share a name. File bytes live in
S3; hierarchy, names, media types, sizes, and timestamps live in DynamoDB.

## Build and test

```shell
./mvnw verify
```

`verify` compiles with Java lint checks, runs the offline unit suite, enforces a
minimum 80% line-coverage gate, builds the JaCoCo report and CycloneDX SBOM, and
checks Google Java Format. Tests use mocked AWS SDK clients and never require an
AWS account.

To format changes:

```shell
./mvnw spotless:apply
```

The coverage report is written to `target/site/jacoco/index.html`.

## Compatibility

Version 2.0 is a breaking modernization release:

- Maven coordinates move from `com.aws.s3` to `io.milton`.
- Public AWS types use AWS SDK for Java 2.x.
- Java 17 is the minimum runtime.
- AWS credentials are discovered through the standard SDK provider chain.
- Storage failures raise `MiltonAwsException` instead of being silently logged.

## Security

See [SECURITY.md](SECURITY.md) for reporting and credential-response guidance.

## License

See [LICENSE](LICENSE).
