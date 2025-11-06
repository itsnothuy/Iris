# Security Policy

## 🔒 Overview

iris_android is a privacy-first, on-device AI assistant. Security and privacy are fundamental to our mission. This document outlines our security practices, how to report vulnerabilities, and what users can expect from us.

## 🛡️ Security Principles

### Core Commitments

1. **On-Device Processing Only**: All AI inference happens locally. No data is transmitted to external servers.
2. **No Telemetry**: We do not collect usage data, analytics, or telemetry.
3. **Minimal Permissions**: We request only the permissions necessary for core functionality.
4. **Encrypted Storage**: Sensitive data is stored using Android's encrypted preferences.
5. **Open Source**: All code is public and auditable.

### Privacy Architecture

- **No Network Calls**: AI models run entirely offline
- **User Consent**: Explicit consent required for accessing sensitive data
- **Data Minimization**: We collect and store only what's necessary
- **Local Storage**: All data remains on the device
- **Secure Deletion**: Proper data deletion when requested

## 🔍 Supported Versions

We provide security updates for the following versions:

| Version | Supported          | Notes                          |
| ------- | ------------------ | ------------------------------ |
| 1.x.x   | ✅ Supported       | Current stable release         |
| 0.x.x   | ❌ Not Supported   | Beta/development versions      |

## 🐛 Reporting a Vulnerability

### For Security Vulnerabilities

**Please DO NOT create public GitHub issues for security vulnerabilities.**

Instead, use GitHub's Security Advisory feature:

1. Navigate to the [Security tab](https://github.com/itsnothuy/Iris/security)
2. Click "Advisories" → "New draft security advisory"
3. Provide detailed information about the vulnerability

### What to Include

Your security report should include:

1. **Description**: Clear description of the vulnerability
2. **Impact**: What could an attacker achieve?
3. **Affected Components**: Which parts of the app are affected?
4. **Steps to Reproduce**: Detailed steps to reproduce the issue
5. **Proof of Concept**: Code or screenshots demonstrating the vulnerability
6. **Suggested Fix**: If you have ideas for fixing it
7. **Environment**: App version, Android version, device model

### Response Timeline

- **Initial Response**: Within 48 hours
- **Severity Assessment**: Within 1 week
- **Fix Development**: Varies by severity (see below)
- **Public Disclosure**: After fix is released and users have time to update

### Severity Levels

| Severity | Response Time | Examples |
|----------|---------------|----------|
| **Critical** | 24-48 hours | Remote code execution, data exfiltration |
| **High** | 1 week | Privilege escalation, sensitive data exposure |
| **Medium** | 2-4 weeks | Information disclosure, DoS |
| **Low** | Best effort | Minor information leaks, theoretical attacks |

## 🔐 Security Best Practices for Contributors

### Code Review Checklist

- [ ] **Input Validation**: All user inputs properly validated and sanitized
- [ ] **SQL Injection**: No raw SQL queries, use parameterized queries
- [ ] **Path Traversal**: File paths properly validated
- [ ] **Intent Security**: Intent filters properly configured
- [ ] **WebView Security**: JavaScript disabled unless necessary
- [ ] **Cryptography**: Use Android Keystore for sensitive operations
- [ ] **Permissions**: Request minimum necessary permissions
- [ ] **Data Storage**: Use EncryptedSharedPreferences for sensitive data
- [ ] **Network Security**: Verify no unauthorized network calls
- [ ] **Dependencies**: Check for known vulnerabilities

### Secure Coding Guidelines

#### Input Validation

```kotlin
// ✅ Good
fun processUserInput(input: String): Result {
    require(input.isNotBlank()) { "Input cannot be blank" }
    require(input.length <= MAX_LENGTH) { "Input too long" }
    return sanitizeAndProcess(input)
}

// ❌ Bad
fun processUserInput(input: String) = process(input)
```

#### Secure Storage

```kotlin
// ✅ Good - Use EncryptedSharedPreferences
val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// ❌ Bad - Plain SharedPreferences for sensitive data
val sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
```

#### SQL Queries

```kotlin
// ✅ Good - Parameterized query
@Query("SELECT * FROM messages WHERE id = :messageId")
fun getMessageById(messageId: Long): Message?

// ❌ Bad - String concatenation
val query = "SELECT * FROM messages WHERE id = $messageId" // SQL injection risk
```

### Dependency Management

Before adding dependencies:

```bash
# Check for known vulnerabilities
./gradlew dependencyCheckAnalyze

# Update dependencies regularly
./gradlew dependencyUpdates
```

### Static Analysis

Run security-focused static analysis:

```bash
# Detekt with security rules
./gradlew detekt

# Android Lint
./gradlew lint
```

## 🔒 Security Features

### Current Security Measures

1. **ProGuard/R8**: Code obfuscation enabled in release builds
2. **Certificate Pinning**: N/A (no network calls)
3. **Root Detection**: Implemented for sensitive operations
4. **Debugger Detection**: Checks for debugging in release builds
5. **Tamper Detection**: Signature verification
6. **Secure Random**: Using SecureRandom for cryptographic operations
7. **Memory Clearing**: Sensitive data cleared after use

### Permissions Used

| Permission | Purpose | Justification |
|------------|---------|---------------|
| `RECORD_AUDIO` | Voice input | For voice-based interaction (optional) |
| `READ_EXTERNAL_STORAGE` | Access documents | For RAG/knowledge base (optional) |
| `CAMERA` | Image input | For multimodal input (optional) |

All permissions are:
- Requested at runtime
- Require explicit user consent
- Can be revoked at any time
- Not required for core functionality

## 📋 Security Audit Trail

### Regular Security Activities

- **Weekly**: Dependency vulnerability scans
- **Monthly**: Security-focused code reviews
- **Quarterly**: Penetration testing
- **Annually**: Third-party security audit (planned)

### Security Testing

```bash
# Run all security checks
./gradlew clean assembleRelease lint detekt test

# Check dependencies
./gradlew dependencyCheckAnalyze

# Verify ProGuard rules
./gradlew assembleRelease
apktool d app/build/outputs/apk/release/app-release.apk
```

## 🚨 Known Security Considerations

### Current Limitations

1. **Local Attack Vector**: If device is compromised, app data may be accessible
2. **Side-Channel Attacks**: Potential timing attacks on AI inference
3. **Physical Access**: Device with screen unlocked can access app data
4. **Rooted Devices**: Limited protection on rooted devices

### Mitigation Strategies

- Encourage device encryption
- Implement app-level screen lock
- Clear sensitive data from memory
- Use Android Keystore for cryptographic keys
- Detect and warn on rooted devices

## 📚 Security Resources

### For Users

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Device Encryption Guide](https://support.google.com/android/answer/9933445)
- [App Permissions Guide](https://support.google.com/android/answer/9431959)

### For Developers

- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [Android Security Checklist](https://developer.android.com/topic/security/best-practices)
- [Secure Coding Guidelines](https://wiki.sei.cmu.edu/confluence/display/android)

## 📝 Disclosure Policy

### Coordinated Disclosure

We follow responsible disclosure practices:

1. **Private Reporting**: Vulnerabilities reported privately
2. **Fix Development**: Security team develops and tests fix
3. **User Notification**: Users notified of security updates
4. **Public Disclosure**: After fix is released and users have updated
5. **Credit**: Reporter credited (if desired) in security advisory

### Public Disclosure Timeline

- **Critical**: 7 days after fix release
- **High**: 14 days after fix release
- **Medium**: 30 days after fix release
- **Low**: 60 days after fix release

## 🏆 Security Hall of Fame

We recognize security researchers who help improve iris_android:

- *To be added as vulnerabilities are responsibly disclosed*

## 📞 Contact

For security-related questions or concerns:

- **Security Advisories**: [GitHub Security Tab](https://github.com/itsnothuy/Iris/security)
- **General Security Questions**: Create a discussion in the Security category
- **Non-Security Bugs**: Use standard issue templates

## 📄 License

This security policy is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

---

**Last Updated**: November 2025  
**Version**: 1.0

*This policy may be updated as the project evolves. Check back regularly for updates.*
