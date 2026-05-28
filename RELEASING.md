# Releasing to Maven Central

This is a one-time setup walkthrough plus the per-release checklist. After
the first release, only the per-release section matters.

## One-time setup

### 1. Sonatype Central account

Sign up at <https://central.sonatype.com>. The post-2024 portal replaces the
old OSSRH JIRA flow — no ticket needed.

### 2. Verify the `io.github.tulsirathod` namespace

In the portal:

1. Add namespace `io.github.tulsirathod`.
2. Choose the GitHub verification method. The portal generates a verification
   code, e.g. `abcd1234`.
3. Create a public GitHub repository named exactly that code under the
   `TulsiRathod` account. The portal polls for it and marks the namespace
   verified. The repo can be deleted afterwards.

Personal-domain alternatives exist (DNS TXT record) if a custom group ID
is ever needed.

### 3. Generate a publishing token

In the portal → "Generate User Token". Save the resulting `username` and
`password` (a short string) — these are NOT your portal login. Add to
`~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR-TOKEN-USERNAME</username>
      <password>YOUR-TOKEN-PASSWORD</password>
    </server>
  </servers>
</settings>
```

The id `central` matches `<publishingServerId>central</publishingServerId>`
in the `release` profile of `pom.xml`.

### 4. GPG signing key

Maven Central requires every artefact to be signed.

```bash
gpg --gen-key                   # accept defaults; pick a passphrase
gpg --list-secret-keys --keyid-format=long
# note the key ID after "sec   rsa4096/<KEYID>"
gpg --keyserver keys.openpgp.org --send-keys <KEYID>
gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
```

Configure Maven to use it (in `~/.m2/settings.xml`):

```xml
<profiles>
  <profile>
    <id>gpg</id>
    <properties>
      <gpg.executable>gpg</gpg.executable>
      <gpg.keyname>YOUR-KEY-ID</gpg.keyname>
      <gpg.passphrase>YOUR-PASSPHRASE</gpg.passphrase>
    </properties>
  </profile>
</profiles>
<activeProfiles>
  <activeProfile>gpg</activeProfile>
</activeProfiles>
```

Or omit `<gpg.passphrase>` and let `gpg-agent` prompt at deploy time.

## Per-release checklist

1. **Pick the version.** Bump from `0.1.0-SNAPSHOT` (or current `-SNAPSHOT`)
   to the release version in `pom.xml`. SemVer rules:
   - PATCH: data refresh, bug fix — `0.1.0` → `0.1.1`.
   - MINOR: new feature, backwards-compatible — `0.1.0` → `0.2.0`.
   - MAJOR: breaking API change — `0.1.0` → `1.0.0`.

2. **Update `CHANGELOG.md`.** Move the `[Unreleased]` entries to a dated
   release header. Bullet what changed under Added / Changed / Fixed / Removed.

3. **Run the full build.**

   ```bash
   mvn clean verify
   ```

   Confirm tests pass and the assembled jar excludes the `builder` package.

4. **Deploy to Central.**

   ```bash
   mvn -Prelease deploy
   ```

   This: signs with GPG, attaches sources + javadoc, uploads to the Sonatype
   Central staging bucket via the publishing plugin. `autoPublish` is `false`
   in the POM, so the deployment lands in your portal as a manual review.

5. **Approve in the portal.** Log into <https://central.sonatype.com>,
   open the deployment under "Deployments", spot-check the staged artefacts,
   and click **Publish**. Propagation to repo1.maven.org takes 10–30 minutes.

6. **Tag the commit.**

   ```bash
   git tag -s vX.Y.Z -m "Release vX.Y.Z"
   git push origin vX.Y.Z
   ```

7. **Bump back to `-SNAPSHOT`.** On `main`, set the version to the next
   anticipated release with `-SNAPSHOT`, add a fresh `## [Unreleased]`
   entry to `CHANGELOG.md`, and commit.

## Troubleshooting

- **"401 Unauthorized" on deploy.** The `username`/`password` in
  `settings.xml` are the *token* values from step 3, not your portal login.
- **"gpg: signing failed: Inappropriate ioctl for device".** Run
  `export GPG_TTY=$(tty)` before `mvn deploy`.
- **Validation rejects the artefact.** The Central portal will list the
  failing rule (missing javadoc jar, missing license, etc.). The
  `release` profile is configured to satisfy all of them; if anything is
  missing it's worth checking that the profile was actually activated
  (`-Prelease`).
