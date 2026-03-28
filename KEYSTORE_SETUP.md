# Keystore Setup for Automated Signed APK Releases

This project uses a keystore to consistently sign release APKs in CI/CD.
The keystore file is **never** stored in the repository — it lives solely in
GitHub Secrets so that the signing identity is stable across builds and
allows in-place app updates on devices.

## Required GitHub Secrets

Configure the following secrets in **Settings → Secrets and variables → Actions**:

| Secret name        | Description                                              |
|--------------------|----------------------------------------------------------|
| `KEYSTORE_BASE64`  | Base64-encoded `.jks` / `.keystore` file                 |
| `KEYSTORE_PASSWORD`| Password for the keystore                                |
| `KEY_ALIAS`        | Alias of the signing key inside the keystore             |
| `KEY_PASSWORD`     | Password for the signing key                             |

## Generating a new keystore (first-time setup)

```bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias release-key \
  -storepass <KEYSTORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=Dev Pipe App, OU=Mobile, O=DevPipe, L=Unknown, ST=Unknown, C=DE"
```

## Encoding the keystore for GitHub Secrets

```bash
base64 -w 0 release.jks
```

Copy the output and store it as the `KEYSTORE_BASE64` secret.

> **Important:** Keep a secure backup of `release.jks` and all passwords.
> Losing the keystore means you can no longer push updates to existing
> installations of the app.
