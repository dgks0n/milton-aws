# Contributing

## Development workflow

1. Use Java 17 or newer and Maven 3.9 or newer.
2. Add or update unit tests with every behavior change.
3. Run `./mvnw spotless:apply` before committing.
4. Run `./mvnw verify`; do not bypass coverage, formatting, or dependency checks.
5. Keep commits focused and use an imperative Conventional Commit subject.

Tests must be deterministic, offline, and independent of personal AWS
credentials. Inject `S3Client`, `DynamoDbClient`, or the manager interfaces and
mock the boundary instead of contacting AWS.

## API changes

Preserve source compatibility within a major version. New public APIs need
Javadoc, input validation, and tests for success and failure behavior. Do not
return `null` collections or swallow AWS SDK failures.

## Security

Never commit access keys, session tokens, `.env` files, credential property
files, production object data, or test fixtures copied from production. Follow
[SECURITY.md](SECURITY.md) for private vulnerability reports.
