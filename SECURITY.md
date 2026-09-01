# Security policy

## Reporting a vulnerability

Do not open a public issue for an undisclosed vulnerability or exposed
credential. Use GitHub's private vulnerability reporting for this repository,
or contact the repository owner privately if that feature is unavailable.

Include the affected version, impact, reproduction steps, and any suggested
mitigation. Avoid accessing data that is not yours.

## Supported versions

Security fixes are applied to the current 2.x development line. The historical
1.x code uses end-of-support dependencies and should not be deployed.

## Credential exposure response

Removing a key from the current source tree is not sufficient: Git retains old
objects. If a credential has ever been committed, immediately deactivate and
delete it in AWS, review CloudTrail for misuse, replace it with a short-lived
role-based credential, and then coordinate a history rewrite if required.
