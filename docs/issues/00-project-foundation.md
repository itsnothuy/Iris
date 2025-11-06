# Issue #00: Project Foundation & CI/CD Infrastructure

## 🎯 Epic: Project Foundation
**Priority**: P0 (Blocking)  
**Estimate**: 3-4 days  
**Dependencies**: None  
**Architecture Reference**: [docs/architecture.md](../architecture.md) - Section 11.2 Build System Architecture

## 📋 Overview
Establish production-ready project infrastructure, CI/CD pipelines, and development standards to ensure code quality, automated testing, and streamlined collaboration for the iris_android project.

## 🎯 Goals
- **Code Quality**: Enforce consistent coding standards and automated quality checks
- **CI/CD Pipeline**: Automated builds, tests, and deployment processes
- **Documentation**: Clear contribution guidelines and project structure
- **Security**: Secure secret management and dependency scanning
- **Collaboration**: Issue templates and structured development workflow

## 📝 Detailed Tasks

### 1. Code Standards & Quality Tools
- [ ] **Setup Ktlint for Kotlin**
  - Configure ktlint with custom rules for Jetpack Compose
  - Add pre-commit hooks and CI integration
  - Create `.editorconfig` for consistent formatting
  
- [ ] **Setup Detekt for Static Analysis**
  - Configure custom rule sets for Android development
  - Add complexity and code smell detection
  - Integrate with IDE and CI pipeline
  
- [ ] **Configure Clang-Format for C++**
  - Setup formatting rules for llama.cpp integration
  - Configure NDK build integration
  - Add format checking to CI pipeline

- [ ] **Add Gradle Dependency Analysis**
  - Setup dependency vulnerability scanning
  - Configure license compliance checking
  - Add dependency update automation

### 2. GitHub Actions CI/CD Pipeline

#### 2.1 Android Build Pipeline
```yaml
# .github/workflows/android-ci.yml
name: Android CI
on: [push, pull_request]
jobs:
  build:
    runs-on: macos-latest
    strategy:
      matrix:
        api-level: [29, 33, 34]
```

- [ ] **Multi-API Level Testing**
  - Test on Android API 29 (minimum), 33, 34
  - Use Android Test Orchestrator for reliable tests
  - Generate test coverage reports

- [ ] **NDK/CMake Build Integration**
  - Setup CMake builds for native libraries
  - Configure cross-compilation for arm64-v8a
  - Add native library testing

- [ ] **Artifact Management**
  - Build debug and release APKs
  - Generate AAB for Play Store deployment
  - Store build artifacts with retention policies

#### 2.2 Quality Assurance Pipeline
- [ ] **Automated Testing**
  - Unit tests with JUnit 5 and MockK
  - UI tests with Compose Test API
  - Integration tests for native components
  - Robolectric tests for CI speed

- [ ] **Code Quality Gates**
  - Minimum 80% test coverage on core modules
  - Zero critical security vulnerabilities
  - All linting rules must pass
  - Performance regression detection

- [ ] **Security Scanning**
  - SAST (Static Application Security Testing)
  - Dependency vulnerability scanning
  - Secret detection in commits
  - License compliance verification

### 3. Repository Structure & Branching

#### 3.1 Branch Protection Rules
- [ ] **Main Branch Protection**
  - Require PR reviews (minimum 1 approver)
  - Require status checks to pass
  - Require branches to be up to date
  - Restrict force pushes and deletions

- [ ] **Development Workflow**
  - `main` - production-ready code
  - `develop` - integration branch
  - `feature/*` - feature development
  - `hotfix/*` - critical bug fixes
  - `release/*` - release preparation

#### 3.2 Issue Templates
Create templates in `.github/ISSUE_TEMPLATE/`:

- [ ] **Bug Report Template**
```markdown
**Device Information:**
- Device Model:
- Android Version:
- App Version:
- Architecture (arm64-v8a/armeabi-v7a):

**Bug Description:**
Clear and concise description of the bug.

**Steps to Reproduce:**
1. Step one
2. Step two
3. ...

**Expected Behavior:**
What should happen.

**Actual Behavior:**
What actually happens.

**Logs:**
```
Paste relevant logs here
```

**Additional Context:**
Any other relevant information.
```

- [ ] **Feature Request Template**
- [ ] **Technical Debt Template**
- [ ] **Security Issue Template** (private)

#### 3.3 Pull Request Templates
- [ ] **PR Template**
```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix (non-breaking change)
- [ ] New feature (non-breaking change)
- [ ] Breaking change (fix or feature causing existing functionality to not work)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed
- [ ] Performance impact assessed

## Architecture Compliance
- [ ] Changes align with docs/architecture.md
- [ ] Module interfaces preserved
- [ ] Dependencies properly managed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new security vulnerabilities introduced
```

### 4. Documentation & Contributing Guidelines

- [ ] **CONTRIBUTING.md**
```markdown
# Contributing to iris_android

## Development Setup
1. Clone repository
2. Install Android Studio Arctic Fox or later
3. Install NDK 25.1.8937393
4. Run `./gradlew assembleDebug`

## Architecture
Always consult [docs/architecture.md](docs/architecture.md) before making changes.

## Code Style
- Kotlin: Follow Ktlint rules
- C++: Follow Clang-Format configuration
- Commit messages: Use Conventional Commits

## Testing
- Minimum 80% coverage on new code
- All tests must pass before PR submission
- Add integration tests for new features

## Security
- Never commit secrets or API keys
- Report security issues privately
- Use encrypted preferences for sensitive data
```

- [ ] **CODE_OF_CONDUCT.md**
  - Adopt Contributor Covenant v2.1
  - Define enforcement procedures
  - Specify contact information

- [ ] **SECURITY.md**
  - Define vulnerability reporting process
  - Specify supported versions
  - Outline security best practices

### 5. Development Environment Setup

- [ ] **Android Studio Configuration**
  - Custom code style settings
  - Live templates for common patterns
  - NDK integration setup guide
  - Recommended plugins list

- [ ] **Local Development Scripts**
```bash
#!/bin/bash
# scripts/setup-dev.sh
echo "Setting up iris_android development environment..."

# Install Git hooks
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Verify Android SDK
if [ -z "$ANDROID_HOME" ]; then
  echo "Please set ANDROID_HOME environment variable"
  exit 1
fi

# Verify NDK
if [ ! -d "$ANDROID_HOME/ndk/25.1.8937393" ]; then
  echo "Please install NDK 25.1.8937393"
  exit 1
fi

# Run initial build
./gradlew assembleDebug

echo "Development environment setup complete!"
```

### 6. Secret Management & Security

- [ ] **GitHub Secrets Configuration**
  - `ANDROID_SIGNING_KEY_BASE64`
  - `ANDROID_SIGNING_STORE_PASSWORD`
  - `ANDROID_SIGNING_KEY_ALIAS`
  - `ANDROID_SIGNING_KEY_PASSWORD`

- [ ] **Local Properties Template**
```properties
# local.properties.template
sdk.dir=/path/to/android/sdk
ndk.dir=/path/to/android/ndk/25.1.8937393

# Signing configuration (for release builds)
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

- [ ] **ProGuard/R8 Configuration**
```proguard
# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI interfaces
-keep class com.nervesparks.iris.core.** { *; }

# Preserve lambda expressions
-dontwarn java.lang.invoke.*
-dontwarn **$$serializer
```

## 🧪 Testing Strategy

### Unit Tests
- [ ] Build system configuration validation
- [ ] Git hooks functionality
- [ ] Template generation scripts
- [ ] Security configuration validation

### Integration Tests
- [ ] Full CI pipeline execution
- [ ] Multi-platform build verification
- [ ] Dependency resolution validation
- [ ] Secret management functionality

### Manual Testing
- [ ] Development environment setup on clean machine
- [ ] PR submission and review process
- [ ] Issue creation and management
- [ ] Documentation clarity and completeness

## ✅ Acceptance Criteria

### Primary Criteria
- [ ] **CI Pipeline Complete**: All GitHub Actions workflows execute successfully
- [ ] **Code Quality Enforced**: All quality gates pass and block merging on failures
- [ ] **Development Workflow**: New contributors can setup environment in < 30 minutes
- [ ] **Security Compliance**: No secrets in repository, all dependencies scanned
- [ ] **Documentation Complete**: All templates and guidelines available

### Technical Criteria
- [ ] **Build Success**: `./gradlew assembleDebug assembleRelease` succeeds
- [ ] **Test Execution**: `./gradlew test connectedAndroidTest` passes
- [ ] **Quality Checks**: `./gradlew ktlintCheck detekt` passes with zero violations
- [ ] **Security Scan**: No high/critical vulnerabilities in dependency report

### Process Criteria
- [ ] **PR Workflow**: Sample PR demonstrates full review process
- [ ] **Issue Management**: All issue types can be created and tracked
- [ ] **Release Process**: Tag-based releases trigger automated builds
- [ ] **Documentation Access**: All docs accessible via repository navigation

## 🔗 Related Issues
- Links to subsequent implementation issues (to be created)
- Architecture compliance tracking
- Security review requirements

## 📋 Definition of Done
- [ ] All CI/CD pipelines green and stable
- [ ] Code quality tools integrated and enforcing standards
- [ ] Repository structure follows enterprise-grade practices
- [ ] Documentation comprehensive and up-to-date
- [ ] Security measures implemented and validated
- [ ] Development workflow tested by team members
- [ ] All acceptance criteria verified and signed off

---

**Note**: This issue establishes the foundation for all subsequent development. No feature development should begin until this foundation is complete and stable.