# Security Policy

Please contact us directly about security issues: security@jacem.ch. Please give appropriate time to respond.

This is a community project without 24/7 operations or formal security review. While we try to make the app as secure as possible, we cannot offer any guarantees. Do not rely on the encryption for your safety.

## Supported Versions

We only support the latest release version. We still like to hear about vulnerabilities in pre-release and older versions.

## Security Model

Since the app is a file browser with the keys to your cloud storage as well as local storage, we consider anything a security issue that...
- allows unauthorized access to oauth tokens, passwords and other secrets,
- allows unintended read/write access to cloud content,
- allows access to private app storage, code or otherwise compromises the functionality.

The app relies on the platform for protecting its private directories and executables. We therefore explicitly do not support rooted devices or devices with unpatched vulnerabilities. If it is technically feasible to add mitigations against platform vulnerabilities, we will consider it.

## Current state of security

_While we do not consider those things security vulnerabilities, they are on our "security" todo list for new/improved protections._
- The rclone configuration file is stored without additional encryption in app-private storage.
- Some operations may use flash storage as temporary storage location for large files. Depending on your Android version, those files may temporarily be available to other apps with storage permissions.
- The SAF provider offers other apps direct access to your files. Do not install apps you do not trust, since they may be able to impersonate apps you have previously granted file access to.
