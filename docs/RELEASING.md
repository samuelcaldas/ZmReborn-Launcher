# Signed ZM Reborn GitHub Releases

ZM Reborn publishes signed APKs only through `.github/workflows/release.yml`. Normal CI continues to build debug APKs and upload them as workflow artifacts; debug artifacts are not release assets.

## Current alpha fallback

The current private repository plan does not provide protected environments, repository rulesets, or GitHub provenance attestation for user-owned private repositories. The first alpha therefore uses repository-level `gh` secrets and the `RELEASE_CERT_SHA256` repository variable, and skips the unavailable provenance-attestation step. Signed APK, certificate, metadata, checksum, tag, and release checks remain fail-closed, but required reviewers, deployment approvals, and immutable `v*` tags are unavailable. Production publication requires an eligible public repository or GitHub plan setup.

## Repository configuration

Configure GitHub Releases before using release tags:

1. Open **Settings → Environments** and create `github-release`.
2. Under deployment protection rules, add the required release-maintainer reviewers and prevent self-review where repository policy supports it.
3. Restrict deployment branches and tags to tags matching `v*`.
4. Open **Settings → Rules → Rulesets** and create a `v*` tag ruleset that limits tag creation, update, and deletion to release maintainers. Published release tags must be immutable.
5. Add these `github-release` environment secrets:
   - `RELEASE_KEYSTORE_BASE64`
   - `RELEASE_KEYSTORE_PASSWORD`
   - `RELEASE_KEY_ALIAS`
   - `RELEASE_KEY_PASSWORD`
6. Add the `github-release` environment variable `RELEASE_CERT_SHA256` with the signing certificate SHA-256 fingerprint as 64 uppercase hexadecimal characters without colons.

For the current alpha fallback, set repository-level values with GitHub CLI instead:

```sh
gh secret set RELEASE_KEYSTORE_BASE64 --repo OWNER/REPOSITORY < <(base64 -w 0 release.jks)
gh secret set RELEASE_KEYSTORE_PASSWORD --repo OWNER/REPOSITORY
gh secret set RELEASE_KEY_ALIAS --repo OWNER/REPOSITORY
gh secret set RELEASE_KEY_PASSWORD --repo OWNER/REPOSITORY
gh variable set RELEASE_CERT_SHA256 --repo OWNER/REPOSITORY --body NORMALIZED_CERTIFICATE_SHA256
```

7. Prepare the keystore secret from the binary keystore file:

   ```sh
   base64 -w 0 release.jks
   ```

8. Inspect the certificate fingerprint, then remove colons and whitespace and convert it to uppercase before setting `RELEASE_CERT_SHA256`:

   ```sh
   $ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner verify --print-certs release.apk
   ```

9. Open **Settings → Actions → General** and verify workflow permissions allow read and write access. Confirm `.github/workflows/release.yml` retains `contents: write`, `attestations: write`, and `id-token: write` permissions.
10. Run **Actions → Signed GitHub Release → Run workflow** with `publish: false` for a manual dry run. After approval succeeds, publish by pushing the protected matching `vMAJOR.MINOR.PATCH` tag.

`RELEASE_CERT_SHA256` identifies the signing certificate. It is not the APK checksum. Keep keystore data and passwords in GitHub secrets; never commit them, print them, or place them in workflow artifacts.

## Version and changelog contract

Before creating a release candidate:

1. Set one literal `versionName` and one positive literal `versionCode` in `app/build.gradle`.
2. Create a matching changelog section in `docs/CHANGELOG.md`:

   ```markdown
   ## [3.1.11-alpha]

   Explain changes included in 3.1.11-alpha.
   ```

3. Create tag `v3.1.11-alpha` at the exact commit containing both changes.

Workflow accepts canonical `vMAJOR.MINOR.PATCH` tags with optional dot-separated prerelease identifiers, then rejects leading-zero components, version mismatches, missing release headings, empty notes, moved tags, and non-positive version codes. Existing historical headings such as `## Status` are not release sections.

## Candidate dry runs

Use **Actions → Signed GitHub Release → Run workflow** with:

- `tag`: candidate tag matching source `versionName`, such as `v3.1.11-alpha`.
- `publish`: `false`.

Dry runs validate metadata, wait for protected-environment approval where available, build a signed release APK, verify package/version metadata, verify v1/JAR signing, compare the certificate fingerprint, and generate `SHA256SUMS`. They upload short-retention release-candidate artifacts but do not create a tag, provenance attestation, or GitHub Release. Private alpha dry runs have no provenance attestation; public publication performs it.

A dry run can use a tag that does not exist yet. This supports checking a prepared commit before creating its release tag.

## Publishing

Publishing occurs only when all gates pass:

- Push a matching `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-prerelease` tag; or
- Run workflow dispatch with `publish: true` and an existing tag pointing exactly at selected source commit.

Tags with prerelease identifiers publish as GitHub prereleases. Tag pushes are publishing events. Protected environment approval is required before signing secrets and publication permissions become available. Workflow validates the remote tag again immediately before publication and refuses to modify an existing GitHub Release.

Publication produces:

- `zm-reborn-vX.Y.Z.apk`
- `SHA256SUMS`
- Changelog-derived release notes
- GitHub artifact provenance attestation for APK on eligible public repositories

Private alpha publication omits GitHub provenance attestation; its signed APK, certificate, metadata, checksum, tag, and release checks still run. APK is built with `-PreleaseSigningRequired=true`, so missing signing inputs fail before release output. Verification requires both v1/JAR signing for legacy `minSdk` support and v2 signing for modern Android. Keystore is decoded only under the runner temporary directory with mode `0600` and removed in an always-run cleanup step.

## Consumer verification

Download APK and checksum from GitHub Release, then run:

```sh
sha256sum --check SHA256SUMS
$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner verify --verbose --print-certs zm-reborn-vX.Y.Z.apk
```

Confirm certificate digest matches published `RELEASE_CERT_SHA256`. For eligible public repositories, inspect GitHub's **Attestations** view or verify with GitHub CLI where supported:

```sh
gh attestation verify zm-reborn-vX.Y.Z.apk \
  --repo '<owner>/<repository>'
```

Private alpha releases intentionally have no provenance attestation and omit this command.

## Failure and rollback boundaries

- Metadata, signing, test, lint, APK, certificate, checksum, or tag/release integrity failure creates no GitHub Release. On eligible public repositories, provenance-attestation failure also prevents release creation; private alpha fallback skips that unavailable step.
- Existing releases are never overwritten or mutated by this workflow.
- A published release cannot be rolled back by rebuilding the same version. Prepare a higher `versionCode`, matching `versionName`, changelog section, and new semantic tag.
- Do not place keystores, passwords, unsigned APKs, Gradle logs, or generated build directories in the repository or release assets.
