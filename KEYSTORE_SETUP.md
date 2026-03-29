# Keystore Setup for Automated Signed APK Releases

This project uses a keystore to consistently sign release APKs in CI/CD.

## Signing strategy

| Situation | Keystore used | Suitable for |
|-----------|--------------|--------------|
| GitHub Secrets **configured** | Secrets (`KEYSTORE_BASE64` etc.) | Production / end-user distribution |
| GitHub Secrets **not configured** | Repo-stored CI keystore (`keystore/debug-release.b64`) | Development builds, CI smoke-tests |

> **Warning:** APKs signed with the repo-stored CI keystore share a public signing
> identity and **must not be distributed to end users**. Configure the production
> secrets before shipping.

## Repo-stored CI keystore (no secrets needed)

A fixed JKS keystore (`keystore/debug-release.b64`) is committed to the repository.
When none of the signing secrets are set the CI workflow automatically decodes and
uses this keystore so that builds never fail due to missing secrets.

| Property          | Value          |
|-------------------|----------------|
| Keystore password | `cipassword`   |
| Key alias         | `release-key`  |
| Key password      | `cipassword`   |
| Store type        | `JKS`          |

## Production secrets (for real releases)

Configure the following secrets in **Settings → Secrets and variables → Actions**
to override the repo keystore with your own signing identity:

| Secret name        | Description                                              |
|--------------------|----------------------------------------------------------|
| `KEYSTORE_BASE64`  | Base64-encoded `.jks` / `.keystore` file                 |
| `KEYSTORE_PASSWORD`| Password for the keystore                                |
| `KEY_ALIAS`        | Alias of the signing key inside the keystore             |
| `KEY_PASSWORD`     | Password for the signing key                             |
| `KEYSTORE_TYPE`    | Keystore format: `JKS` (recommended) or `PKCS12`         |

## Generating a new production keystore

```bash
keytool -genkey -v \
  -keystore release.jks \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias release-key \
  -storepass <KEYSTORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=Dev Pipe App, OU=Mobile, O=DevPipe, L=Unknown, ST=Unknown, C=DE"
```

> **Note:** `-storetype JKS` is required to ensure the keystore uses the JKS
> format regardless of the JDK version. On JDK 11 and later the default store
> type is PKCS12; without the explicit flag that format can cause a
> *"Tag number over 30 is not supported"* error during signing.

## Encoding the keystore for GitHub Secrets

```bash
base64 -w 0 release.jks
```

Copy the output and store it as the `KEYSTORE_BASE64` secret.

> **Important:** The `-w 0` flag (Linux) disables line-wrapping so the secret
> is a single unbroken base64 string. On macOS use `base64 release.jks | tr -d '[:space:]'`
> to strip all whitespace. The CI workflow also strips whitespace automatically
> (`tr -d '[:space:]'`) before decoding, so minor line-wrapping in the secret
> is tolerated.

> **Important:** Keep a secure backup of `release.jks` and all passwords.
> Losing the keystore means you can no longer push updates to existing
> installations of the app.
