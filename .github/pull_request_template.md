## Summary
- What changed and why?

## Type of Change
- [ ] Bug fix (non-breaking change)
- [ ] New feature (non-breaking change)
- [ ] Breaking change (fix or feature causing existing functionality to not work)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring
- [ ] Security fix
- [ ] Technical debt reduction

## Testing
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Integration tests pass (if applicable)
- [ ] Compose UI tests pass (if UI changed)
- [ ] Manual testing completed
- [ ] Performance impact assessed
- [ ] No new test coverage regressions

## Architecture Compliance
- [ ] Changes align with [docs/architecture.md](../docs/architecture.md)
- [ ] Module interfaces preserved or properly updated
- [ ] Dependencies properly managed
- [ ] No violation of privacy-first principles (on-device only)

## Code Quality
- [ ] Code follows project style guidelines
- [ ] Builds successfully (`./gradlew assembleDebug`)
- [ ] Ktlint check passes (`./gradlew ktlintCheck`)
- [ ] Detekt check passes (`./gradlew detekt`)
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] No new compiler warnings introduced

## Security & Privacy
- [ ] No telemetry added; privacy posture honored
- [ ] No secrets or API keys committed
- [ ] No new security vulnerabilities introduced
- [ ] Dependency vulnerabilities checked
- [ ] Proper input validation implemented
- [ ] Secure data storage practices followed (if applicable)

## Documentation
- [ ] Docs updated (`docs/pages/<page>.md` or `docs/PLAN.md`)
- [ ] ADR created for significant architectural decisions
- [ ] Code comments updated
- [ ] README updated (if needed)
- [ ] API documentation updated (if applicable)

## Screenshots / Notes
(attach emulator screenshots if UI changed, or describe behavior changes)

## Performance Impact
- [ ] No significant performance degradation
- [ ] Battery usage impact assessed
- [ ] Memory usage impact assessed
- [ ] APK size impact acceptable

## Follow-ups
- [ ] Related issues referenced (Fixes #123, Relates to #456)
- [ ] Next page/module per docs/PLAN.md identified
- [ ] Known limitations documented

---

**Reviewer Checklist:**
- [ ] Code is readable and maintainable
- [ ] Architecture compliance verified
- [ ] Security implications reviewed
- [ ] Test coverage adequate
- [ ] Documentation sufficient
