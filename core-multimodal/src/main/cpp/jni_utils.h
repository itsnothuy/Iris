#ifndef IRIS_MULTIMODAL_JNI_UTILS_H
#define IRIS_MULTIMODAL_JNI_UTILS_H

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

// Logging macros
#define LOG_TAG "IrisMultimodal"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace iris {
namespace jni {

/**
 * RAII wrapper for JNI string handling
 */
class JString {
public:
    JString(JNIEnv* env, jstring jstr) : env_(env), jstr_(jstr) {
        if (jstr != nullptr) {
            cstr_ = env->GetStringUTFChars(jstr, nullptr);
        }
    }
    
    ~JString() {
        if (cstr_ != nullptr) {
            env_->ReleaseStringUTFChars(jstr_, cstr_);
        }
    }
    
    // No copy
    JString(const JString&) = delete;
    JString& operator=(const JString&) = delete;
    
    // Move enabled
    JString(JString&& other) noexcept 
        : env_(other.env_), jstr_(other.jstr_), cstr_(other.cstr_) {
        other.cstr_ = nullptr;
    }
    
    const char* c_str() const { return cstr_; }
    operator const char*() const { return cstr_; }
    bool is_null() const { return cstr_ == nullptr; }
    
private:
    JNIEnv* env_;
    jstring jstr_;
    const char* cstr_ = nullptr;
};

/**
 * RAII wrapper for JNI byte array handling
 */
class JByteArray {
public:
    JByteArray(JNIEnv* env, jbyteArray jarray) : env_(env), jarray_(jarray) {
        if (jarray != nullptr) {
            data_ = env->GetByteArrayElements(jarray, nullptr);
            length_ = env->GetArrayLength(jarray);
        }
    }
    
    ~JByteArray() {
        if (data_ != nullptr) {
            env_->ReleaseByteArrayElements(jarray_, data_, JNI_ABORT);
        }
    }
    
    // No copy
    JByteArray(const JByteArray&) = delete;
    JByteArray& operator=(const JByteArray&) = delete;
    
    jbyte* data() const { return data_; }
    jsize length() const { return length_; }
    bool is_null() const { return data_ == nullptr; }
    
    // Convert to unsigned char for image processing
    const unsigned char* as_uchar() const { 
        return reinterpret_cast<const unsigned char*>(data_); 
    }
    
private:
    JNIEnv* env_;
    jbyteArray jarray_;
    jbyte* data_ = nullptr;
    jsize length_ = 0;
};

/**
 * RAII wrapper for JNI float array handling
 */
class JFloatArray {
public:
    JFloatArray(JNIEnv* env, jfloatArray jarray) : env_(env), jarray_(jarray) {
        if (jarray != nullptr) {
            data_ = env->GetFloatArrayElements(jarray, nullptr);
            length_ = env->GetArrayLength(jarray);
        }
    }
    
    ~JFloatArray() {
        if (data_ != nullptr) {
            env_->ReleaseFloatArrayElements(jarray_, data_, JNI_ABORT);
        }
    }
    
    // No copy
    JFloatArray(const JFloatArray&) = delete;
    JFloatArray& operator=(const JFloatArray&) = delete;
    
    jfloat* data() const { return data_; }
    jsize length() const { return length_; }
    bool is_null() const { return data_ == nullptr; }
    
    // Convert to std::vector
    std::vector<float> to_vector() const {
        if (is_null()) return {};
        return std::vector<float>(data_, data_ + length_);
    }
    
private:
    JNIEnv* env_;
    jfloatArray jarray_;
    jfloat* data_ = nullptr;
    jsize length_ = 0;
};

/**
 * Helper to create Java string from C++ string
 */
inline jstring create_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

/**
 * Helper to create Java float array from C++ vector
 */
inline jfloatArray create_jfloat_array(JNIEnv* env, const std::vector<float>& vec) {
    jfloatArray result = env->NewFloatArray(vec.size());
    if (result != nullptr && !vec.empty()) {
        env->SetFloatArrayRegion(result, 0, vec.size(), vec.data());
    }
    return result;
}

/**
 * Helper to throw Java exception
 */
inline void throw_exception(JNIEnv* env, const char* exception_class, const char* message) {
    jclass cls = env->FindClass(exception_class);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
}

/**
 * Common exception classes
 */
namespace exceptions {
    constexpr const char* RUNTIME = "java/lang/RuntimeException";
    constexpr const char* ILLEGAL_ARGUMENT = "java/lang/IllegalArgumentException";
    constexpr const char* ILLEGAL_STATE = "java/lang/IllegalStateException";
    constexpr const char* OUT_OF_MEMORY = "java/lang/OutOfMemoryError";
}

} // namespace jni
} // namespace iris

#endif // IRIS_MULTIMODAL_JNI_UTILS_H
