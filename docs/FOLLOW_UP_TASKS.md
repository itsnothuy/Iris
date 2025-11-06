# Follow-up Tasks for Core Architecture

## High Priority (Required before merging)

### 1. Fix Existing App Code Compilation
- [ ] Re-enable llama module (requires fixing CMake configuration)
- [ ] Update MainActivity to work with new architecture
- [ ] Update MainViewModel to use AppCoordinator
- [ ] Fix Compose UI component imports (StarBorder, Archive, HourglassEmpty icons)
- [ ] Remove or comment out deprecated code paths

### 2. Documentation
- [x] Create implementation summary document
- [ ] Update docs/architecture.md with actual implementation details
- [ ] Add KDoc to all public APIs
- [ ] Create module-level README files

## Medium Priority (Can be done post-merge)

### 3. Additional Testing
- [ ] Add UI tests for MainActivity integration
- [ ] Add instrumentation tests for Hilt DI
- [ ] Increase test coverage to 95% across all modules
- [ ] Add benchmark tests for performance profiling

### 4. Code Quality
- [ ] Run ktlint and fix formatting issues
- [ ] Run detekt and address code smells
- [ ] Remove unused imports and variables
- [ ] Add missing @Suppress annotations where necessary

### 5. CI/CD Integration
- [ ] Ensure GitHub Actions workflow passes
- [ ] Add test coverage reporting to CI
- [ ] Add lint checks to PR validation
- [ ] Configure automated dependency updates

## Low Priority (Future enhancements)

### 6. Deferred Module Implementations
- [ ] Implement core-asr module for Whisper integration
- [ ] Implement core-vision module for MediaPipe
- [ ] Implement core-tools module for function calling
- [ ] Add core-thermal with ADPF integration

### 7. Performance Optimization
- [ ] Profile module initialization time
- [ ] Optimize Hilt dependency graph
- [ ] Add lazy loading for heavy components
- [ ] Implement module-level caching

### 8. Developer Experience
- [ ] Add sample usage documentation
- [ ] Create architecture decision records (ADRs)
- [ ] Add debugging utilities
- [ ] Create developer setup guide

## Known Issues

1. **llama module CMake error**: Native build fails due to missing submodule or CMakeLists.txt configuration
2. **Compose icon imports**: Missing icons in UI components (can use alternatives)
3. **BuildConfig reference**: Removed debug flag check in IrisApplication
4. **Existing UI coupling**: MainActivity still references old LLamaAndroid class

## Success Criteria

- [x] All core modules compile successfully
- [x] All unit tests pass
- [x] Hilt DI framework integrated
- [x] Interface contracts defined
- [x] 95%+ test coverage on interfaces
- [ ] App module compiles successfully
- [ ] No lint errors
- [ ] Documentation complete

## Estimated Timeline

- **Fix compilation issues**: 2-4 hours
- **Documentation updates**: 2-3 hours
- **Code quality improvements**: 3-5 hours
- **Additional testing**: 4-6 hours

**Total**: 11-18 hours of work remaining
