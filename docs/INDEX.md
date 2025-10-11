# Iris Android Documentation Index

Welcome to the Iris Android project documentation. This is an on-device multimodal AI assistant built with Kotlin/Android using Jetpack Compose, LLaMA.cpp integration, and privacy-first architecture.

## 📋 Planning & Architecture

- **[Master Plan](PLAN.md)** - Project scope, milestones, risks, and testing strategy
- **[Architecture Decision Records](adr/)** - Key technical decisions and rationale

## 📱 Feature Specifications

### Core User Interface
- **[Chat Interface](pages/chat-interface.md)** - Main conversational UI and message handling
- **[Model Management](pages/model-management.md)** - Model loading, switching, and performance monitoring
- **[Settings & Configuration](pages/settings-config.md)** - User preferences and system configuration

### Technical Components
- **[On-Device Inference Engine](pages/inference-engine.md)** - LLaMA integration and performance optimization
- **[Data & Privacy](pages/data-privacy.md)** - Local storage, conversation history, and privacy guarantees

## 🏗️ Repository Structure

```
/
├── app/                    # Android application module
│   ├── src/main/java/com/nervesparks/iris/
│   │   ├── MainActivity.kt          # Main entry point with navigation drawer
│   │   ├── ChatScreen.kt           # Navigation and routing
│   │   ├── MainViewModel.kt        # Business logic and state management
│   │   └── ui/                     # Compose UI components
│   │       ├── MainChatScreen.kt   # Primary chat interface
│   │       ├── ModelsScreen.kt     # Model management UI
│   │       ├── ParametersScreen.kt # Model parameter configuration
│   │       ├── SettingsScreen.kt   # Settings navigation
│   │       └── components/         # Reusable UI components
├── llama/                  # Native library module for LLaMA.cpp
├── model_pack/            # Model packaging and distribution
└── docs/                  # This documentation
```

## 🛠️ Development Guidelines

### Code Quality Standards
- **Testing**: JUnit 5 for unit tests, Compose UI tests for components
- **Coverage**: 80% minimum on business logic modules
- **Static Analysis**: ktlint/detekt for Kotlin, enforced in CI
- **Security**: On-device inference only, no network access without consent

### Build & Deployment
- **Build Tool**: Gradle with Kotlin DSL
- **CI/CD**: GitHub Actions with matrix builds across Android API levels
- **Commands**: 
  - `./gradlew assembleDebug` - Local development builds
  - `./gradlew assembleRelease` - Production builds
  - `./gradlew lint test` - Quality checks

### Development Workflow
1. Follow [Conventional Commits](https://conventionalcommits.org/) for commit messages
2. All changes via Pull Requests with code review
3. Branch protection requires passing CI and one approval
4. Use Copilot Edits for multi-file changes, Agent mode for interactive sessions

## 🔐 Security & Privacy

### Privacy-First Design
- All AI inference remains on-device
- No conversation data transmitted externally
- User consent required for any network operations
- Local storage encryption for sensitive data

### Secrets Management
- GitHub Actions OIDC for cloud credentials
- Environment variables for configuration
- No hardcoded API keys or tokens

## 🧪 Testing Strategy

### Test Pyramid
- **Unit Tests**: Business logic, data transformations, utilities
- **Integration Tests**: ViewModel interactions, repository patterns
- **UI Tests**: Compose components, user interactions
- **E2E Tests**: Critical user flows only

### Performance Testing
- Memory usage monitoring during inference
- Thermal throttling detection and response
- Battery impact measurement
- Model loading time benchmarks

## 📚 Additional Resources

- **[Copilot Instructions](.github/copilot-instructions.md)** - AI assistant guidelines for this repository
- **[Setup Guide](../IRIS_ANDROID_SETUP_COMPLETE_GUIDE.md)** - Complete development environment setup
- **[ADR Template](adr/_template.md)** - Template for architecture decision records

## 🎯 Quick Start

1. **Clone & Setup**: Follow setup guide for Android development environment
2. **Read the Plan**: Start with [PLAN.md](PLAN.md) for project overview
3. **Check ADRs**: Review [architecture decisions](adr/) for context
4. **Pick a Feature**: Choose a page spec to implement
5. **Follow Guidelines**: Use planning workflow and quality standards

---

*Last Updated: October 2025*  
*Project Phase: Planning & Initial Implementation*