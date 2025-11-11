# Manual Implementation Guide for Issue #7.5

## 🎯 Immediate Action Plan

Since the GitHub Coding Agent has infrastructure issues, we can implement Issue #7.5 manually using the detailed specification as a guide.

## 📋 Step-by-Step Implementation

### Phase 1: Production Model Registry (2-3 days)

1. **Create MultimodalModelRegistryImpl.kt**
   - File: `core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/registry/MultimodalModelRegistryImpl.kt`
   - Copy the complete implementation from Issue #7.5 specification
   - Add required imports and dependencies

2. **Create Model Catalog Asset**
   - File: `core-multimodal/src/main/assets/multimodal_models.json`
   - Use the JSON specification from Issue #7.5
   - Contains LLaVA and Qwen-VL model definitions

3. **Test Model Registry**
   - Create unit tests for model recommendation logic
   - Test compatibility scoring algorithm
   - Verify caching functionality

### Phase 2: Production Image Processing (2-3 days)

1. **Create ImageProcessorImpl.kt**
   - File: `core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/image/ImageProcessorImpl.kt`
   - Implement real Android Bitmap processing
   - Add image validation and format conversion

2. **Test Image Processing**
   - Test with various image formats (JPEG, PNG, WEBP)
   - Verify resize algorithms maintain aspect ratios
   - Test memory management and cleanup

### Phase 3: Production Vision Engine (3-4 days)

1. **Create VisionProcessingEngineImpl.kt**
   - File: `core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/vision/VisionProcessingEngineImpl.kt`
   - Integrate with NativeInferenceEngine
   - Implement streaming capabilities

2. **Test Vision Engine**
   - Mock native inference for initial testing
   - Test different vision tasks (OCR, analysis, etc.)
   - Verify error handling and performance monitoring

### Phase 4: Integration & Testing (1-2 days)

1. **Update Dependency Injection**
   - Replace mock bindings with production implementations
   - Ensure proper scoping and lifecycle management

2. **End-to-End Testing**
   - Test complete multimodal workflows
   - Performance benchmarking
   - Memory usage validation

## 🛠️ Implementation Commands

### Create Directory Structure
```bash
# Ensure proper directory structure exists
mkdir -p core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/registry
mkdir -p core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/image  
mkdir -p core-multimodal/src/main/kotlin/com/nervesparks/iris/core/multimodal/vision
mkdir -p core-multimodal/src/main/assets
```

### Copy Implementation Files
```bash
# Use the detailed code from Issue #7.5 specification
# Each file is completely specified with full implementation
# No guesswork required - just copy the code blocks
```

### Build and Test
```bash
# Test each component as you implement
./gradlew :core-multimodal:testDebugUnitTest
./gradlew :core-multimodal:assembleDebug

# Run integration tests
./gradlew :core-multimodal:connectedDebugAndroidTest
```

## ✅ Advantages of Manual Implementation

1. **No Infrastructure Dependencies**: Bypass Coding Agent networking issues
2. **Complete Specification**: Issue #7.5 has every line of code needed
3. **Quality Control**: Manual review ensures code quality
4. **Incremental Progress**: Implement and test piece by piece
5. **Learning Opportunity**: Understand the complete multimodal architecture

## 🔄 Migration Path

Once implemented manually:
1. **Document the working implementation**
2. **Create test cases that validate functionality**  
3. **Use as reference for future Coding Agent attempts**
4. **Share lessons learned for Coding Agent improvements**

## 📈 Timeline Estimate

- **Total Implementation**: 8-12 days (as specified in Issue #7.5)
- **Phase 1** (Model Registry): 2-3 days
- **Phase 2** (Image Processing): 2-3 days  
- **Phase 3** (Vision Engine): 3-4 days
- **Phase 4** (Integration): 1-2 days

## 🎯 Success Criteria

When complete, you'll have:
- ✅ Real multimodal model registry with device compatibility
- ✅ Production Android image processing
- ✅ Vision-language inference engine (when native engine ready)
- ✅ Complete test coverage
- ✅ Performance benchmarks
- ✅ Working foundation for actual vision capabilities

This approach gives you the full benefits of Issue #7.5 without waiting for Coding Agent infrastructure fixes.