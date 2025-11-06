# ADR 0002: GitHub Actions for CI/CD

## Status

Accepted

## Context

iris_android requires a robust CI/CD pipeline to ensure code quality, automate testing, and streamline the release process. As a privacy-first, on-device AI assistant, we need to balance security, performance, and developer productivity.

### Requirements

1. **Automated Quality Checks**: Enforce code standards, static analysis, and test coverage
2. **Multi-Platform Testing**: Test on multiple Android API levels
3. **Security Scanning**: Detect vulnerabilities in code and dependencies
4. **Artifact Management**: Build and distribute APKs/AABs securely
5. **Developer Experience**: Fast feedback loops, clear failure messages
6. **Cost**: Free or low-cost for open-source projects
7. **Integration**: Deep GitHub integration for issues, PRs, releases

### Alternatives Considered

#### 1. **GitHub Actions**
- **Pros**: 
  - Native GitHub integration
  - Free for public repositories (2000 minutes/month)
  - Matrix builds for testing multiple API levels
  - Large ecosystem of actions
  - OIDC support for secure secrets
  - Self-hosted runner support
- **Cons**:
  - Limited to GitHub ecosystem
  - macOS runners are 10x more expensive
  - Requires careful workflow optimization

#### 2. **GitLab CI**
- **Pros**:
  - Powerful pipeline features
  - Better caching mechanisms
  - Built-in container registry
  - Free tier includes 400 compute minutes
- **Cons**:
  - Requires GitLab repository or mirror
  - More complex configuration
  - Less GitHub integration
  - Migration effort required

#### 3. **CircleCI**
- **Pros**:
  - Fast build times
  - Good Android support
  - Docker layer caching
  - Free tier: 2500 credits/month
- **Cons**:
  - Third-party service
  - Requires separate account
  - Less GitHub integration
  - Credit-based pricing can be confusing

#### 4. **Jenkins**
- **Pros**:
  - Self-hosted, full control
  - Extensive plugin ecosystem
  - No minute limits
- **Cons**:
  - Requires infrastructure
  - Maintenance overhead
  - Complex setup
  - No native GitHub integration

#### 5. **Bitrise**
- **Pros**:
  - Mobile-first platform
  - Easy Android/iOS setup
  - Good caching
- **Cons**:
  - Limited free tier (200 builds/month)
  - Vendor lock-in
  - Less flexible than general CI

## Decision

We will use **GitHub Actions** as our primary CI/CD platform.

### Implementation Strategy

1. **Multi-Workflow Architecture**: Separate workflows for different concerns
   - `enhanced-ci.yml`: Main CI pipeline (code quality, tests, builds)
   - `multi-api-testing.yml`: Multi-API level testing on emulators
   - `dependency-updates.yml`: Weekly dependency checks
   - `release.yml`: Tag-triggered release builds
   - `stale.yml`: Issue/PR lifecycle management

2. **Quality Gates**: Progressive checks from fast to slow
   ```
   Code Quality (2-3 min)
   ↓
   Lint (3-5 min)
   ↓
   Unit Tests (5-8 min)
   ↓
   Security Scan (8-12 min)
   ↓
   Build (10-15 min)
   ```

3. **Parallel Execution**: Run independent jobs concurrently
   - Code quality and lint run in parallel
   - Debug and release builds run sequentially (after tests)
   - Multi-API tests run in matrix (parallel)

4. **Caching Strategy**:
   - Gradle dependencies cached via `actions/setup-java`
   - Build cache enabled via `gradle.properties`
   - AVD caching for instrumented tests

5. **Security Integration**:
   - CodeQL for SAST
   - Dependency-Check for SCA
   - Secret scanning (GitHub native)
   - Sarif upload for detekt results

6. **Artifact Management**:
   - Debug builds: 30-day retention
   - Release builds: 90-day retention
   - Test reports: 30-day retention
   - Mapping files: 90-day retention (for crash analysis)

## Consequences

### Positive

1. **Native Integration**: Seamless GitHub workflow
   - PR checks integrated natively
   - Issues/discussions linked
   - Security advisories integrated
   - Release automation built-in

2. **Cost-Effective**: Free for public repositories
   - 2000 minutes/month free
   - Sufficient for moderate development pace
   - Can add self-hosted runners if needed

3. **Developer Experience**:
   - Familiar YAML syntax
   - Rich action marketplace
   - Good documentation
   - Local testing with `act`

4. **Security**:
   - OIDC for secure authentication
   - GitHub Secrets management
   - CodeQL integration
   - Dependabot integration

5. **Flexibility**:
   - Can use self-hosted runners for faster builds
   - Matrix builds for multi-API testing
   - Custom Docker images if needed
   - Workflow reuse and composition

### Negative

1. **Platform Lock-In**: Tied to GitHub
   - Migration effort if we switch to GitLab/Bitrise
   - Workflow syntax not portable
   - **Mitigation**: Keep build logic in Gradle scripts, not workflows

2. **macOS Costs**: Expensive for emulator testing
   - macOS runners are 10x more expensive than Linux
   - **Mitigation**: 
     - Use Linux with Robolectric for unit tests
     - Limit emulator tests to critical paths
     - Use matrix strategy efficiently
     - Schedule full tests weekly, not per-commit

3. **Minute Limits**: Free tier can run out
   - 2000 minutes/month for free
   - Complex builds can consume quickly
   - **Mitigation**:
     - Optimize build times
     - Use build cache aggressively
     - Skip redundant builds (paths filter)
     - Self-hosted runners for heavy workloads

4. **Cold Start Time**: Runners take time to provision
   - 30-60 seconds to start a job
   - **Mitigation**: Parallel jobs reduce total time

### Neutral

1. **Learning Curve**: Team needs to learn GitHub Actions
   - Documentation is good
   - Similar to other CI/CD tools
   - Many examples available

2. **Debugging**: Can be challenging
   - Use `actions/upload-artifact` extensively
   - Enable debug logging when needed
   - Use `act` for local testing

## Monitoring and Metrics

We will track:
- Build success rate
- Average build time
- Test pass rate
- Code coverage trends
- Security scan results
- Dependency update frequency

Target SLOs:
- Build success rate: >95%
- Average build time: <15 minutes
- Test pass rate: >98%
- Code coverage: ≥80%
- Critical vulnerabilities: 0
- Time to fix vulnerabilities: <48 hours

## Future Considerations

1. **Self-Hosted Runners**: If build times become problematic
   - Can use ARM-based runners (Raspberry Pi cluster)
   - Better resource control
   - Faster builds with persistent caching

2. **Hybrid Approach**: Use multiple CI systems
   - GitHub Actions for quick feedback
   - Bitrise for nightly full builds
   - Self-hosted for compute-intensive tasks

3. **Build Optimization**:
   - Gradle build cache
   - Incremental compilation
   - Modularization to reduce build scope
   - Parallel test execution

4. **Advanced Features**:
   - Deployment to Play Store (when ready)
   - Firebase Test Lab integration
   - Performance regression testing
   - Screenshot testing

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/projects/continuous-integration)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [CodeQL for Android](https://codeql.github.com/docs/codeql-overview/)
- [Android Emulator in CI](https://github.com/ReactiveCircus/android-emulator-runner)

## Related ADRs

- [ADR 0001: Use Copilot Sonnet 4](0001-use-copilot-sonnet4.md)
- ADR 0002: Choice of Build Tool (Gradle) - Referenced in architecture.md
- Future ADR: Self-Hosted Runner Strategy (if implemented)

## Approval

- **Author**: GitHub Copilot Coding Agent
- **Date**: November 2025
- **Reviewers**: Project maintainers
- **Status**: Accepted and implemented

---

## Changelog

- 2025-11: Initial decision and implementation
