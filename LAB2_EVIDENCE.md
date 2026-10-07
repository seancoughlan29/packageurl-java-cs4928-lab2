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

## Refactoring B: extract the shared canonicalisation rule

B1: added one private helper to PackageURL that performs the existing
transformation and nothing else.

    private static String canonicalizeQualifierKey(final String key) {
        return StringUtil.toLowerCase(key);
    }

B2: parseQualifiers(Map) now calls the helper. Empty-value filtering unchanged.
B3: parseQualifiers(String) calls the same helper. Duplicate-key detection
stays exactly where it was.
B4: the two methods were not unified.

| Record | Evidence |
| --- | --- |
| mvn test | PASS - Tests run: 143, Failures: 0, Errors: 0, Skipped: 0 |
| CS4928 checker | PASS - Tests run: 5, Failures: 0, Errors: 0 |
| git diff --check | clean |
| Second commit SHA | c5eadfd542dfd01bc0721685d09ea086aee77078 |

Before to after: the lowercase-the-key rule now exists in one place and both
parsing paths call it, while duplicate detection and empty-value filtering
stayed in the methods they belong to.

## Comparing against the baseline

git diff --stat CS4928_BASELINE..HEAD, source files only:

    PackageNamePolicies.java |  81 +++++++++++
    PackageNamePolicy.java   |  41 ++++++
    PackageURL.java          |  46 +++-------

Refactoring A: what became more local? The decision about which normalisation
rule a package type gets. It used to run down a switch inside a validation
method; it now lives in PackageNamePolicies.forType, which is the only place
to look or to edit. The extra structure is one interface and one resolver
class, which is real added weight: two files that did not exist before, to
hold logic that was previously inline.

Refactoring B: what has a single source of truth? The rule that qualifier
keys are lowercase. What stayed separate on purpose: duplicate-key detection
in the encoded-string path, and empty-value filtering plus overwrite
behaviour in the Map path. Merging those to make the diff tidier would have
changed behaviour.

Why passing tests are necessary but not sufficient. Green tests only say I
did not break what the suite already checks. They say nothing about whether
the design improved, because a correct refactoring produces identical output
by definition, so no test can tell the switch from the policy resolver.
Tests also only cover the cases somebody thought to write, so the untested
corners of validateName are exactly as untested as before. The tests are what
let me claim behaviour was preserved; the design argument has to be made
separately, by pointing at what now changes in one place instead of several.

## Problem C: deliberately not implemented

Package-type behaviour also appears in validateNamespace, validateVersion and
verifyTypeConstraints, and a wider policy could eventually pull all of it onto
one axis. I left it because a refactoring experiment you can actually verify
has to stay small: each of these two changes is provable against the baseline
in a single test run, whereas touching four methods at once would make it much
harder to say which change caused a failure.

## Final judgement

Would I keep both in a real pull request? Yes, but as two separate ones.

Benefit: adding a package type with an unusual name rule is now a one-line
change in forType plus at most one new policy constant, instead of editing a
switch buried in a validation method. The qualifier helper means the lowercase
rule cannot drift between the two parsing paths.

Trade-off: Refactoring A adds two files and one level of indirection to
replace a switch that was perfectly readable. A reviewer could fairly argue I
traded an obvious thing for a slightly less obvious one, and that the payoff
only arrives if more type-specific name rules show up. The package-url spec
suggests they will, but that is a bet, not a certainty. Refactoring B is far
easier to defend: small, removes a duplicated rule, adds no structure.

## Repository

| Record | Evidence |
| --- | --- |
| Repository URL | https://github.com/seancoughlan29/packageurl-java-cs4928-lab2 |
| Refactoring B commit SHA | c5eadfd542dfd01bc0721685d09ea086aee77078 |

The final HEAD SHA after this evidence commit is shown to the coordinator with
git rev-parse HEAD, not stored in this file.
