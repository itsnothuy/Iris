# Contributing to iris_android

Thank you for your interest in contributing to iris_android! This document provides guidelines and instructions for contributing to the project.

## 🎯 Project Mission

iris_android is a privacy-first, on-device AI assistant for Android. All AI processing happens locally on the device with no data transmission to external servers. Our core values are:

- **Privacy First**: No data leaves the device
- **Performance**: Real-time or near real-time inference
- **Quality**: Production-ready code with comprehensive testing
- **Openness**: Transparent development and welcoming community

## 📋 Table of Contents

1. [Development Setup](#development-setup)
2. [Architecture](#architecture)
3. [Code Style](#code-style)
4. [Testing Requirements](#testing-requirements)
5. [Pull Request Process](#pull-request-process)
6. [Security](#security)
7. [Communication](#communication)

## 🚀 Development Setup

### Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: 17 or later
- **Android SDK**: API 28 (minimum) to 34 (target)
- **Android NDK**: 26.1.10909125
- **Git**: Latest version

### Initial Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/itsnothuy/Iris.git
   cd Iris
   git submodule update --init --recursive
   ```

2. **Install Android SDK and NDK**
   - Open Android Studio
   - Go to Settings → Appearance & Behavior → System Settings → Android SDK
   - Install SDK Platform 28, 33, and 34
   - Switch to "SDK Tools" tab
   - Install NDK version 26.1.10909125

3. **Configure local.properties**
   ```properties
   sdk.dir=/path/to/android/sdk
   ndk.dir=/path/to/android/ndk/26.1.10909125
   ```

4. **Run development setup script**
   ```bash
   chmod +x scripts/setup-dev.sh
   ./scripts/setup-dev.sh
   ```

5. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

6. **Run tests**
   ```bash
   ./gradlew test
   ```

## 🏗️ Architecture

**ALWAYS consult [docs/architecture.md](docs/architecture.md) before making changes.**

The architecture document contains:
- Module responsibilities and interfaces
- Data flows and hardware integration
- API specifications and deployment strategies
- Safety systems and performance management

### Key Principles

1. **Modular Design**: Each module has clear responsibilities
2. **Privacy First**: All AI processing on-device
3. **Performance**: Respect thermal and energy budgets
4. **Safety**: Content filtering and user consent mechanisms

### Module Structure

```
app/
├── src/main/java/com/nervesparks/iris/
│   ├── ui/              # UI components (Jetpack Compose)
│   ├── data/            # Data layer (Room, repositories)
│   ├── domain/          # Business logic and use cases
│   └── core/            # Core AI and hardware integration
```

## 💻 Code Style

### Kotlin

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for automatic formatting
- Maximum line length: 120 characters
- Use meaningful variable and function names

**Format your code:**
```bash
./gradlew ktlintFormat
```

**Check code style:**
```bash
./gradlew ktlintCheck
```

### Static Analysis

Run detekt for code quality checks:
```bash
./gradlew detekt
```

### C++ (Native Code)

- Follow Google C++ Style Guide
- Use clang-format for formatting
- Maximum line length: 100 characters

**Format C++ code:**
```bash
find app/src/main/cpp -name "*.cpp" -o -name "*.h" | xargs clang-format -i
```

## 🧪 Testing Requirements

### Unit Tests

- **Minimum 80% coverage** on new code
- Use JUnit 5 for test framework
- Use MockK for mocking in Kotlin
- Test file naming: `<ClassName>Test.kt`

**Run unit tests:**
```bash
./gradlew test
```

**Generate coverage report:**
```bash
./gradlew jacocoTestReport
```

### UI Tests

- Use Jetpack Compose testing APIs for Compose UI
- Use Espresso for legacy views
- Test critical user flows

**Run UI tests:**
```bash
./gradlew connectedAndroidTest
```

### Integration Tests

- Test interactions between modules
- Test native library integration
- Use Robolectric for faster CI execution

### Test Structure

```kotlin
class ExampleTest {
    @Test
    fun `should do something when condition is met`() {
        // Given
        val input = createTestInput()
        
        // When
        val result = systemUnderTest.doSomething(input)
        
        // Then
        assertThat(result).isEqualTo(expectedOutput)
    }
}
```

## 🔄 Pull Request Process

### Before Starting

1. Check existing issues and PRs to avoid duplication
2. For large changes, create an issue first to discuss
3. Reference [docs/PLAN.md](docs/PLAN.md) for planned work

### Branch Naming

- `feature/short-description` - New features
- `fix/bug-description` - Bug fixes
- `refactor/area-description` - Refactoring
- `docs/topic` - Documentation updates
- `test/area` - Test improvements

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): subject

body (optional)

footer (optional)
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `perf`: Performance improvements
- `chore`: Build process or auxiliary tool changes

**Examples:**
```
feat(chat): add streaming response support

Implements token-by-token streaming for better UX.
References #123

fix(llm): prevent crash on empty prompt

Adds null check before processing prompt.
Fixes #456
```

### PR Guidelines

1. **Keep PRs focused**: ~300 LOC maximum for effective review
2. **Fill out PR template**: Complete all sections
3. **Ensure CI passes**: All checks must be green
4. **Request review**: Assign at least one reviewer
5. **Address feedback**: Respond to all review comments
6. **Keep updated**: Rebase on main if needed

### PR Checklist

Before submitting:
- [ ] Code builds successfully
- [ ] All tests pass
- [ ] Code coverage meets threshold (80%)
- [ ] Ktlint and detekt checks pass
- [ ] Architecture compliance verified
- [ ] Documentation updated
- [ ] No telemetry or privacy violations
- [ ] Security implications considered

## 🔒 Security

### Reporting Vulnerabilities

**Do not create public issues for security vulnerabilities.**

Instead:
1. Go to Repository → Security → Advisories
2. Click "New draft security advisory"
3. Provide detailed information
4. We'll respond within 48 hours

### Security Best Practices

- **Never commit secrets** (API keys, passwords, tokens)
- **Validate all inputs** (user input, file contents, network data)
- **Use encrypted storage** for sensitive data
- **Follow principle of least privilege** for permissions
- **Keep dependencies updated** and scan for vulnerabilities

### Dependency Management

Before adding new dependencies:
```bash
./gradlew dependencyCheckAnalyze
```

## 💬 Communication

### Channels

- **GitHub Issues**: Bug reports, feature requests, technical discussions
- **Pull Requests**: Code reviews and implementation discussions
- **Discussions**: General questions, ideas, and community support

### Issue Etiquette

- **Search first**: Check if issue already exists
- **Be specific**: Provide detailed information
- **Be respectful**: Follow Code of Conduct
- **Stay on topic**: Keep discussions focused
- **Update status**: Close issues when resolved

### Code Review Etiquette

**As a reviewer:**
- Be constructive and specific
- Explain the "why" behind suggestions
- Acknowledge good practices
- Approve when ready, even with minor suggestions

**As an author:**
- Respond to all comments
- Ask questions if unclear
- Make requested changes or explain alternatives
- Thank reviewers for their time

## 📝 Documentation

### Required Documentation

1. **Code comments**: For complex logic
2. **API documentation**: For public interfaces
3. **Architecture docs**: For significant changes
4. **ADRs**: For architectural decisions
5. **README updates**: For new features or setup changes

### ADR (Architecture Decision Records)

For significant architectural decisions:

1. Copy `docs/adr/_template.md`
2. Create `docs/adr/NNNN-title.md`
3. Fill in context, decision, and consequences
4. Reference in PR

## 🎓 Learning Resources

### Android Development

- [Android Developer Guides](https://developer.android.com/guide)
- [Jetpack Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

### On-Device AI

- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/)
- [llama.cpp](https://github.com/ggerganov/llama.cpp)

### Project-Specific

- [docs/architecture.md](docs/architecture.md) - Complete architecture
- [docs/PLAN.md](docs/PLAN.md) - Development roadmap
- [docs/adr/](docs/adr/) - Architecture decisions

## 🆘 Getting Help

Stuck? Here's how to get help:

1. **Check documentation**: Start with docs/architecture.md
2. **Search issues**: Look for similar problems
3. **Ask in discussions**: For general questions
4. **Create an issue**: For specific problems

When asking for help:
- Describe what you're trying to do
- Share relevant code or error messages
- Explain what you've already tried
- Provide device/environment details

## 📜 License

By contributing to iris_android, you agree that your contributions will be licensed under the same license as the project.

## 🙏 Thank You

Thank you for contributing to iris_android! Your efforts help build a better, more private AI assistant for everyone.

---

**Questions?** Create a discussion or reach out to the maintainers.
