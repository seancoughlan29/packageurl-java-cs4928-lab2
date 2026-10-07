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

## Refactoring A: extract a package name policy

| Step | Change |
| --- | --- |
| A1 | PackageNamePolicy.java (new): package-private interface, one method String normalize(String value) |
| A2 | PackageNamePolicies.java (new): four reusable policies PRESERVE, LOWERCASE, PUB, PYPI. Not one class per package type. |
| A3 | PackageNamePolicies.forType(type) maps each type onto a policy, keeping the original switch groups exactly |
| A4 | validateName keeps the empty-name check and delegates normalisation |
| A5 | namespace, version, parsing, constructors, canonical output and type constraints untouched |

validateName went from 29 lines to 5:

    private static String validateName(final String type, final String value) throws MalformedPackageURLException {
        if (value.isEmpty()) {
            throw new MalformedPackageURLException("The PackageURL name specified is invalid");
        }
        return PackageNamePolicies.forType(type).normalize(value);
    }

Behaviour preserved: github and the other lowercase types still lowercase;
pub still lowercases then underscores non [a-z0-9_] characters; pypi still
converts underscores to hyphens; rpm names are returned exactly as supplied;
unrelated types unchanged; an empty name still throws.

| Record | Evidence |
| --- | --- |
| mvn test | PASS - Tests run: 143, Failures: 0, Errors: 0, Skipped: 0 |
| CS4928 checker | PASS - Tests run: 5, Failures: 0, Errors: 0 |
| git diff --check | clean |
| First commit SHA | 964396bb8effbc3405c9a7122cbcf76d29f4d5cb |

Before to after: the decision about how a package type normalises its name
moved out of validateName into one resolver, so validateName now holds only
the rule that is true for every package type.

## Problem B: qualifier key canonicalisation

There are two private parseQualifiers methods, one taking a Map<String,String>
and one taking an encoded query string. Both begin by lowercasing qualifier
keys, so the rule "qualifier keys are lowercase" is written out twice. If that
rule ever changed you would have to find and fix both, and nothing in the code
tells you the second one exists.

The rest of the two methods is not shared and must not be merged. The
encoded-string path splits on & and =, and deliberately throws once two keys
collide after lowercasing. The Map path filters out entries with empty values
and lets a later key silently overwrite an earlier one instead of throwing.
Both behaviours were confirmed against the baseline.

Two different parsing algorithms that happen to share one small rule. Sharing
a rule is not the same as sharing a responsibility.
