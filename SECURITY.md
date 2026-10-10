# Security Policy

## Supported Versions

Only the latest stable release (on the `main` branch) receives security fixes. The `dev` branch receives experimental pre-releases.

| Version / Branch | Supported |
| :--- | :--- |
| Latest stable release (`main`) | Yes |
| Experimental pre-releases (`dev`) | Best-effort |
| Older releases | No |

## Reporting a Vulnerability

Please report suspected security vulnerabilities using GitHub Private Vulnerability Reporting:
- Navigate to the **Security** tab of the repository (`kveld9/kveld-morphe-patches`).
- Select **Report a vulnerability**.

Do not open public issues, discussions, or pull requests for suspected vulnerabilities.

### What to Include in Your Report

To help triage and resolve the issue quickly, include:
- Affected patched application and its target version.
- Patcher release version (`.mpp` file version).
- Morphe Manager or CLI patching log excerpt showing relevant output.
- Clear, minimal steps to reproduce the issue.

### Privacy and Secret Containment

Do not include sensitive data or secrets in reports, such as:
- Private keystores or signing keys.
- API tokens, credentials, or personal access tokens.
- Hardware serial numbers, IMEI, MAC addresses, or personal user identifiers.

## Scope and Boundaries

This repository publishes client-side bytecode and resource patch specifications. It does not host, operate, or maintain any backend servers, cloud infrastructure, or user account systems.

The following items are strictly out of scope:
- Requests or bypasses for server-side subscription paywalls.
- DRM bypasses or cloud-restricted content unlocking.
- Items explicitly documented in [docs/out-of-scope.md](docs/out-of-scope.md).

## Response Process

Reports are triaged on a best-effort basis. Please allow reasonable time for investigation before any public disclosure.
