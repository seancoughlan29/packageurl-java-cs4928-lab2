/*
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.packageurl;

import com.github.packageurl.PackageURL.StandardTypes;
import com.github.packageurl.internal.StringUtil;

/**
 * Resolves the {@link PackageNamePolicy} that applies to a given package type.
 *
 * <p>The policies are reusable constants rather than one class per package
 * type: several types share the same rule, so the types are grouped onto a
 * small set of shared policies. This class is the single place that decides
 * which rule applies to which type.
 */
final class PackageNamePolicies {

    /** Keeps the name exactly as supplied. */
    static final PackageNamePolicy PRESERVE = value -> value;

    /** Lowercases the name. Shared by most types that normalise at all. */
    static final PackageNamePolicy LOWERCASE = StringUtil::toLowerCase;

    /** Lowercases, then replaces every character outside [a-z0-9_] with an underscore. */
    static final PackageNamePolicy PUB = value -> StringUtil.toLowerCase(value).replaceAll("[^a-z0-9_]", "_");

    /** Lowercases, then converts underscores to hyphens. */
    static final PackageNamePolicy PYPI = value -> StringUtil.toLowerCase(value).replace('_', '-');

    private PackageNamePolicies() {}

    /**
     * Returns the name policy for the given package type.
     *
     * <p>The groups below are exactly the groups of the switch this resolver
     * replaced, so every type normalises the same way it did before.
     *
     * @param type the lowercased package type
     * @return the policy for that type, never {@code null}
     */
    static PackageNamePolicy forType(final String type) {
        switch (type) {
            case StandardTypes.APK:
            case StandardTypes.BITBUCKET:
            case StandardTypes.BITNAMI:
            case StandardTypes.COMPOSER:
            case StandardTypes.DEB:
            case StandardTypes.GITHUB:
            case StandardTypes.GOLANG:
            case StandardTypes.HEX:
            case StandardTypes.LUAROCKS:
            case StandardTypes.OCI:
                return LOWERCASE;
            case StandardTypes.PUB:
                return PUB;
            case StandardTypes.PYPI:
                return PYPI;
            default:
                return PRESERVE;
        }
    }
}
