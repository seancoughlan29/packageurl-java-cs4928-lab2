# CS4928 Lab 2 - Repository Surgery Evidence

Sean Coughlan | Repository: package-url/packageurl-java

## Baseline

| Record | Evidence |
| --- | --- |
| Pinned SHA | cf437186b2ae2d9100a187e0df3cce3a163e9b4b |
| Branch | cs4928-lab2 |
| Tag | CS4928_BASELINE |
| Upstream `mvn test` | PASS - Tests run: 138, Failures: 0, Errors: 0, Skipped: 0 |
| CS4928 checker | PASS - Tests run: 5, Failures: 0, Errors: 0 |

No source code was changed until the baseline was green.

## Problem A: type-specific name rules

Problem A: validateName mixes general validation with package-type-specific
normalisation. Goal: move the varying name-normalisation algorithm behind a
stable policy while preserving every existing result.

The method does two different jobs. The empty-name check is an invariant that
holds for every package type. The switch underneath it is a growing family of
small algorithms: ten types share a plain lowercase rule, PUB lowercases then
replaces every character outside [a-z0-9_] with an underscore, PYPI lowercases
then turns underscores into hyphens, and everything else is preserved as
supplied. That second job changes every time the spec adds a type.
