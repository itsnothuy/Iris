# CI/CD Status Badges

Add these badges to your README.md to show build and quality status:

## Build Status

```markdown
[![Enhanced CI](https://github.com/itsnothuy/Iris/actions/workflows/enhanced-ci.yml/badge.svg)](https://github.com/itsnothuy/Iris/actions/workflows/enhanced-ci.yml)
[![Multi-API Testing](https://github.com/itsnothuy/Iris/actions/workflows/multi-api-testing.yml/badge.svg)](https://github.com/itsnothuy/Iris/actions/workflows/multi-api-testing.yml)
[![Release Build](https://github.com/itsnothuy/Iris/actions/workflows/release.yml/badge.svg)](https://github.com/itsnothuy/Iris/actions/workflows/release.yml)
```

## Security

```markdown
[![CodeQL](https://github.com/itsnothuy/Iris/workflows/CodeQL/badge.svg)](https://github.com/itsnothuy/Iris/security/code-scanning)
[![Dependency Review](https://github.com/itsnothuy/Iris/actions/workflows/dependency-updates.yml/badge.svg)](https://github.com/itsnothuy/Iris/actions/workflows/dependency-updates.yml)
```

## Quality

```markdown
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![detekt](https://img.shields.io/badge/code%20quality-detekt-blue)](https://detekt.dev/)
```

## Example README Section

```markdown
# iris_android

[![Enhanced CI](https://github.com/itsnothuy/Iris/actions/workflows/enhanced-ci.yml/badge.svg)](https://github.com/itsnothuy/Iris/actions/workflows/enhanced-ci.yml)
[![Multi-API Testing](https://github.com/itsnothuy/Iris/actions/workflows/multi-api-testing.yml/badge.svg)](https://github.com/itsnothuy/Iris/actions/workflows/multi-api-testing.yml)
[![CodeQL](https://github.com/itsnothuy/Iris/workflows/CodeQL/badge.svg)](https://github.com/itsnothuy/Iris/security/code-scanning)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

A privacy-first, on-device AI assistant for Android.

## Features

- 🔒 **Privacy-First**: All AI processing happens on-device
- ⚡ **Fast**: Optimized for mobile performance
- 🤖 **Intelligent**: Advanced language model capabilities
- 🎨 **Modern UI**: Built with Jetpack Compose

## Quick Start

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed setup instructions.

```bash
git clone https://github.com/itsnothuy/Iris.git
cd Iris
./scripts/setup-dev.sh
./gradlew assembleDebug
```

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Security

Security is a top priority. See [SECURITY.md](SECURITY.md) for our security policy and how to report vulnerabilities.

## License

[Your License Here]
```
