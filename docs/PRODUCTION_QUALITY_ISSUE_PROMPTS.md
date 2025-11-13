# Production-Quality Issue Prompts for GitHub Copilot Coding Agent

## ⚠️ Important Disclaimer

These issue prompts are designed for aspects of the production gaps that Copilot Agent **might** be able to assist with. However, based on the analysis in `COPILOT_AGENT_LIMITATIONS_ANALYSIS.md`, Agent will **not be able to complete native implementations** without human intervention.

**Use these prompts for**:
- Architecture scaffolding and interface definitions
- Kotlin/Java business logic implementation
- Test infrastructure creation
- Documentation and planning

**Do NOT expect Agent to**:
- Write working JNI/C++ code
- Compile native libraries
- Test on actual hardware
- Make architectural decisions requiring trade-offs

---

## Issue #1: Core-LLM Native JNI Interface Scaffolding

### Title
`[core-llm] Create JNI interface scaffolding for native llama.cpp integration`

### Description

**Context**: 
The `core-llm` module has a complete Kotlin interface (`LLMEngine`) with method declarations for native JNI bindings, but the actual C++ implementation files do not exist. This issue focuses on creating the **scaffolding and structure** for JNI integration.

**Objective**:
Create the file structure, build configuration, and Kotlin-side JNI declarations necessary for native llama.cpp integration. This is **scaffolding work only** - actual C++ implementation will require manual development.

### Acceptance Criteria

1. **File Structure Created**:
   ```
   core-llm/src/main/
   ├── cpp/
   │   ├── CMakeLists.txt           # CMake build configuration
   │   ├── llm_jni.cpp              # JNI binding stub implementations
   │   ├── llama_wrapper.h          # C++ wrapper header
   │   ├── llama_wrapper.cpp        # C++ wrapper stub implementations
   │   └── include/
   │       ├── jni_utils.h          # JNI helper utilities header
   │       └── android_logging.h   # Android logging macros
   └── kotlin/com/nervesparks/iris/core/llm/
       └── LLMEngineImpl.kt         # Already exists, verify native methods
   ```

2. **CMakeLists.txt Configuration**:
   - Project configuration for `iris_llm` shared library
   - Include paths to llama.cpp headers (relative path: `../../../../app/src/main/cpp/llama.cpp/`)
   - Link against Android system libraries (`android`, `log`)
   - Support for multiple Android ABIs (arm64-v8a, armeabi-v7a, x86_64)
   - Build configuration for Debug and Release variants

3. **JNI Method Stubs**:
   Create stub implementations for all native methods declared in `LLMEngineImpl.kt`:
   - `nativeInitializeBackend(backendType: Int): Int`
   - `nativeLoadModel(modelPath: String, params: ModelLoadParams): String?`
   - `nativeStartGeneration(modelId: String, prompt: String, params: GenerationParams): Long`
   - `nativeGenerateNextToken(sessionId: Long): String?`
   - `nativeGenerateEmbedding(modelId: String, text: String): FloatArray?`
   - `nativeUnloadModel(modelId: String): Boolean`
   
   Each stub should:
   - Return appropriate default/error values
   - Log function entry with Android logging
   - Include TODO comments indicating needed implementation

4. **JNI Utility Headers**:
   - `jni_utils.h`: Helper functions for Java↔C++ type conversion
     - `jstring` ↔ `std::string` conversion
     - `jfloatArray` ↔ `std::vector<float>` conversion
     - Exception handling wrappers
   - `android_logging.h`: Macros for Android `__android_log_print`

5. **Build System Integration**:
   - Update `core-llm/build.gradle.kts` to include CMake external build
   - Configure NDK version and ABI filters
   - Add CMake arguments for build optimization

6. **Documentation**:
   - Create `core-llm/src/main/cpp/README.md` explaining:
     - Purpose of each file
     - How to build native library
     - Dependencies on llama.cpp
     - Instructions for implementing actual logic (for future developers)

### Technical Specifications

#### CMakeLists.txt Template
```cmake
cmake_minimum_required(VERSION 3.22.1)
project("iris_llm")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include paths
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../app/src/main/cpp/llama.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../app/src/main/cpp/llama.cpp/common
)

# Source files
add_library(${CMAKE_PROJECT_NAME} SHARED
    llm_jni.cpp
    llama_wrapper.cpp
)

# Find and link against llama library
# TODO: Configure path to built llama library
find_library(log-lib log)

target_link_libraries(${CMAKE_PROJECT_NAME}
    ${log-lib}
    android
)

# Optimization flags
if(CMAKE_BUILD_TYPE MATCHES Release)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O0 -g")
endif()
```

#### JNI Method Stub Template
```cpp
// File: llm_jni.cpp
#include <jni.h>
#include <string>
#include <android/log.h>
#include "llama_wrapper.h"
#include "jni_utils.h"

#define TAG "LLM_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeInitializeBackend(
    JNIEnv *env, jobject thiz, jint backend_type) {
    
    LOGI("nativeInitializeBackend called with backend: %d", backend_type);
    
    // TODO: Implement actual backend initialization
    // 1. Initialize llama.cpp backend based on backend_type
    // 2. Configure for Android optimization (mmap, thread count)
    // 3. Return 0 on success, non-zero on failure
    
    LOGE("Native backend initialization not yet implemented");
    return -1; // Failure
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nervesparks_iris_core_llm_LLMEngineImpl_nativeLoadModel(
    JNIEnv *env, jobject thiz, jstring model_path, jobject params) {
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("nativeLoadModel called with path: %s", path);
    
    // TODO: Implement actual model loading
    // 1. Convert jobject params to native ModelLoadParams struct
    // 2. Call llama_load_model_from_file()
    // 3. Store model pointer in global map
    // 4. Return unique model ID as jstring
    
    env->ReleaseStringUTFChars(model_path, path);
    LOGE("Native model loading not yet implemented");
    return nullptr; // Failure
}

// TODO: Implement remaining JNI methods following the same pattern
```

#### build.gradle.kts Integration
```kotlin
// File: core-llm/build.gradle.kts
android {
    // ... existing configuration
    
    defaultConfig {
        // ... existing config
        
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fno-rtti", "-fno-exceptions")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_PLATFORM=android-24"
                )
            }
        }
    }
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
```

### Implementation Notes

⚠️ **Important Limitations**:
1. This issue creates **scaffolding only** - JNI method stubs will return error values
2. Actual llama.cpp integration requires:
   - Understanding llama.cpp API (100K+ lines of C++)
   - Proper memory management and lifecycle handling
   - Android-specific optimizations (mmap configuration, thread pooling)
   - Testing on physical devices
3. A human C++/Android NDK developer must implement the actual logic
4. The stubs provide structure for future implementation

### Files to Create
- `core-llm/src/main/cpp/CMakeLists.txt`
- `core-llm/src/main/cpp/llm_jni.cpp`
- `core-llm/src/main/cpp/llama_wrapper.h`
- `core-llm/src/main/cpp/llama_wrapper.cpp`
- `core-llm/src/main/cpp/include/jni_utils.h`
- `core-llm/src/main/cpp/include/android_logging.h`
- `core-llm/src/main/cpp/README.md`

### Files to Modify
- `core-llm/build.gradle.kts` (add externalNativeBuild configuration)

### References
- Existing Kotlin interface: `core-llm/src/main/kotlin/com/nervesparks/iris/core/llm/LLMEngineImpl.kt`
- llama.cpp source: `app/src/main/cpp/llama.cpp/`
- Architecture documentation: `docs/architecture.md`
- Production analysis: `docs/PRODUCTION_ARCHITECTURE_ANALYSIS.md`

---

## Issue #2: Core-Models Remote Catalog - Architecture & Interfaces

### Title
`[core-models] Design and implement architecture for remote model catalog integration`

### Description

**Context**:
`ModelRegistryImpl.kt` currently has a TODO comment for remote catalog fetching. The bundled catalog is functional but users cannot discover new models without app updates.

**Objective**:
Design and implement the architecture for fetching model metadata from HuggingFace Hub, including interfaces, data models, and high-level business logic. **Note**: Actual API integration will require API keys and testing that Agent cannot perform.

### Acceptance Criteria

1. **Interface Definitions**:
   ```kotlin
   // File: core-models/src/main/kotlin/.../remote/RemoteModelCatalogClient.kt
   interface RemoteModelCatalogClient {
       suspend fun fetchAvailableModels(filters: ModelSearchFilters): Result<List<RemoteModelInfo>>
       suspend fun getModelMetadata(modelId: String): Result<ModelMetadata>
       suspend fun downloadModel(
           modelId: String,
           progressCallback: (DownloadProgress) -> Unit
       ): Flow<DownloadState>
   }
   
   // File: core-models/src/main/kotlin/.../remote/ModelSearchFilters.kt
   data class ModelSearchFilters(
       val format: ModelFormat = ModelFormat.GGUF,
       val quantization: List<QuantizationType>? = null,
       val maxSizeBytes: Long? = null,
       val minDownloads: Int? = null,
       val sortBy: SortCriteria = SortCriteria.DOWNLOADS,
       val limit: Int = 50
   )
   ```

2. **Data Models**:
   ```kotlin
   // Remote model representation
   data class RemoteModelInfo(
       val id: String,
       val name: String,
       val author: String,
       val description: String,
       val downloads: Long,
       val likes: Int,
       val tags: List<String>,
       val files: List<ModelFile>,
       val lastModified: Long
   )
   
   data class ModelFile(
       val filename: String,
       val sizeBytes: Long,
       val downloadUrl: String,
       val sha256: String?
   )
   
   sealed class DownloadState {
       data class InProgress(val progress: DownloadProgress) : DownloadState()
       data class Completed(val file: File) : DownloadState()
       data class Failed(val error: Throwable) : DownloadState()
   }
   
   data class DownloadProgress(
       val downloadedBytes: Long,
       val totalBytes: Long,
       val speedBytesPerSecond: Long
   ) {
       val percentage: Float get() = if (totalBytes > 0) {
           (downloadedBytes.toFloat() / totalBytes) * 100f
       } else 0f
   }
   ```

3. **Stub Implementation**:
   ```kotlin
   // File: core-models/src/main/kotlin/.../remote/HuggingFaceModelClient.kt
   class HuggingFaceModelClient @Inject constructor(
       private val httpClient: OkHttpClient,
       private val json: Json,
       @ApplicationContext private val context: Context
   ) : RemoteModelCatalogClient {
       
       override suspend fun fetchAvailableModels(
           filters: ModelSearchFilters
       ): Result<List<RemoteModelInfo>> {
           // TODO: Implement actual HuggingFace API call
           // This requires:
           // 1. Understanding HF API authentication (if needed)
           // 2. Constructing query URL with filters
           // 3. Parsing response JSON
           // 4. Handling rate limiting
           
           Log.w(TAG, "Remote model fetching not yet implemented")
           return Result.failure(NotImplementedError("HuggingFace API integration pending"))
       }
       
       override suspend fun downloadModel(
           modelId: String,
           progressCallback: (DownloadProgress) -> Unit
       ): Flow<DownloadState> = flow {
           // TODO: Implement robust download with:
           // 1. Resume capability (HTTP range requests)
           // 2. Checksum verification
           // 3. Atomic file operations
           // 4. Cancellation support
           
           emit(DownloadState.Failed(NotImplementedError("Download not yet implemented")))
       }
   }
   ```

4. **Integration with ModelRegistry**:
   ```kotlin
   // File: core-models/src/main/kotlin/.../registry/ModelRegistryImpl.kt
   class ModelRegistryImpl @Inject constructor(
       // ... existing dependencies
       private val remoteCatalogClient: RemoteModelCatalogClient,
       private val catalogMerger: ModelCatalogMerger
   ) : ModelRegistry {
       
       override suspend fun refreshCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
           try {
               // Load bundled catalog
               val bundledCatalog = loadBundledCatalog()
               
               // Attempt to fetch remote catalog
               val remoteModels = remoteCatalogClient.fetchAvailableModels(
                   ModelSearchFilters(maxSizeBytes = getMaxModelSizeForDevice())
               )
               
               val mergedCatalog = if (remoteModels.isSuccess) {
                   catalogMerger.merge(bundledCatalog, remoteModels.getOrThrow())
               } else {
                   Log.w(TAG, "Failed to fetch remote catalog, using bundled only", 
                       remoteModels.exceptionOrNull())
                   bundledCatalog
               }
               
               cachedRegistry = mergedCatalog
               cacheCatalog(mergedCatalog)
               Result.success(Unit)
               
           } catch (e: Exception) {
               Log.e(TAG, "Failed to refresh catalog", e)
               Result.failure(e)
           }
       }
   }
   ```

5. **Catalog Merging Logic**:
   ```kotlin
   // File: core-models/src/main/kotlin/.../registry/ModelCatalogMerger.kt
   class ModelCatalogMerger @Inject constructor() {
       
       fun merge(
           bundled: ModelCatalog,
           remote: List<RemoteModelInfo>
       ): ModelCatalog {
           // Merge strategy:
           // 1. Keep all bundled models (always available offline)
           // 2. Add remote models not in bundled catalog
           // 3. Update metadata for models present in both
           // 4. Filter out incompatible models
           
           val remoteDescriptors = remote.map { convertToDescriptor(it) }
           
           val allModels = (bundled.models.llm + remoteDescriptors)
               .distinctBy { it.id }
               .sortedByDescending { it.downloads }
           
           return bundled.copy(
               models = bundled.models.copy(llm = allModels),
               lastUpdated = System.currentTimeMillis()
           )
       }
       
       private fun convertToDescriptor(remote: RemoteModelInfo): ModelDescriptor {
           // Convert remote model representation to internal descriptor
           return ModelDescriptor(
               id = remote.id,
               name = remote.name,
               description = remote.description,
               type = ModelType.LLM,
               fileSize = remote.files.firstOrNull()?.sizeBytes ?: 0L,
               quantization = extractQuantizationFromTags(remote.tags),
               contextSize = extractContextSizeFromName(remote.name),
               parameterCount = extractParameterCountFromName(remote.name),
               source = ModelSource.REMOTE,
               downloadUrl = remote.files.firstOrNull()?.downloadUrl
           )
       }
   }
   ```

6. **Unit Tests**:
   - Test catalog merging logic with various scenarios
   - Test filter construction
   - Mock-based tests for RemoteModelCatalogClient

7. **Documentation**:
   - Document remote catalog architecture in `core-models/README.md`
   - Add implementation notes for future API integration
   - Document HuggingFace API requirements

### Technical Specifications

#### Dependency Injection Setup
```kotlin
// File: core-models/src/main/kotlin/.../di/ModelModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RemoteModelModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }
    
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    
    @Provides
    @Singleton
    fun provideRemoteCatalogClient(
        httpClient: OkHttpClient,
        json: Json,
        @ApplicationContext context: Context
    ): RemoteModelCatalogClient {
        return HuggingFaceModelClient(httpClient, json, context)
    }
}
```

#### Error Handling
```kotlin
sealed class RemoteCatalogException(message: String, cause: Throwable? = null) 
    : Exception(message, cause) {
    
    class NetworkException(message: String, cause: Throwable? = null) 
        : RemoteCatalogException(message, cause)
    
    class AuthenticationException(message: String) 
        : RemoteCatalogException(message)
    
    class RateLimitException(val retryAfterSeconds: Int) 
        : RemoteCatalogException("Rate limit exceeded, retry after $retryAfterSeconds seconds")
    
    class ParseException(message: String, cause: Throwable? = null) 
        : RemoteCatalogException(message, cause)
}
```

### Implementation Notes

⚠️ **Important Limitations**:
1. This issue creates **architecture and scaffolding** - API calls will not work
2. Actual HuggingFace integration requires:
   - Understanding HF API endpoints and authentication
   - Testing with real API calls (Agent cannot do this)
   - Handling API-specific quirks and rate limiting
   - API key management (if required)
3. Download implementation requires:
   - HTTP resume capability testing
   - Checksum verification implementation
   - File system atomic operations
4. A human developer must complete the actual API integration

### Files to Create
- `core-models/src/main/kotlin/.../remote/RemoteModelCatalogClient.kt`
- `core-models/src/main/kotlin/.../remote/HuggingFaceModelClient.kt`
- `core-models/src/main/kotlin/.../remote/ModelSearchFilters.kt`
- `core-models/src/main/kotlin/.../remote/RemoteModelInfo.kt`
- `core-models/src/main/kotlin/.../remote/DownloadState.kt`
- `core-models/src/main/kotlin/.../registry/ModelCatalogMerger.kt`
- `core-models/src/main/kotlin/.../di/RemoteModelModule.kt`
- `core-models/src/test/kotlin/.../ModelCatalogMergerTest.kt`
- `core-models/README.md`

### Files to Modify
- `core-models/src/main/kotlin/.../registry/ModelRegistryImpl.kt`
- `core-models/build.gradle.kts` (add OkHttp/Kotlinx Serialization dependencies)

### References
- Existing TODO: `ModelRegistryImpl.kt:139`
- HuggingFace Hub API docs: https://huggingface.co/docs/hub/api
- Architecture: `docs/architecture.md`

---

## Issue #3: Core-RAG Vector Storage - Architecture Design

### Title
`[core-rag] Design architecture for persistent vector storage with Room database`

### Description

**Context**:
`VectorStoreImpl.kt` currently uses in-memory storage with `MutableMap`, which loses data on app restart. Production requires persistent storage with efficient vector similarity search.

**Objective**:
Design the database schema, Room DAOs, and architecture for persistent vector storage. **Note**: sqlite-vec integration requires native SQLite extension that Agent cannot compile, but the Kotlin-side architecture can be designed.

### Acceptance Criteria

1. **Database Schema Design**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../storage/VectorDatabase.kt
   @Database(
       entities = [
           VectorChunkEntity::class,
           DocumentEntity::class,
           ChunkMetadataEntity::class
       ],
       version = 2,
       exportSchema = true
   )
   @TypeConverters(VectorTypeConverters::class)
   abstract class VectorDatabase : RoomDatabase() {
       abstract fun vectorSearchDao(): VectorSearchDao
       abstract fun documentDao(): DocumentDao
       abstract fun metadataDao(): ChunkMetadataDao
       
       companion object {
           const val DATABASE_NAME = "iris_vector_store.db"
       }
   }
   ```

2. **Entity Definitions**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../storage/entities/VectorChunkEntity.kt
   @Entity(
       tableName = "vector_chunks",
       foreignKeys = [
           ForeignKey(
               entity = DocumentEntity::class,
               parentColumns = ["id"],
               childColumns = ["document_id"],
               onDelete = ForeignKey.CASCADE
           )
       ],
       indices = [
           Index(value = ["document_id"]),
           Index(value = ["created_at"])
       ]
   )
   data class VectorChunkEntity(
       @PrimaryKey
       @ColumnInfo(name = "id")
       val id: String,
       
       @ColumnInfo(name = "document_id")
       val documentId: String,
       
       @ColumnInfo(name = "content")
       val content: String,
       
       @ColumnInfo(name = "embedding")
       val embedding: ByteArray,  // FloatArray serialized to bytes
       
       @ColumnInfo(name = "chunk_index")
       val chunkIndex: Int,
       
       @ColumnInfo(name = "created_at")
       val createdAt: Long = System.currentTimeMillis()
   )
   
   @Entity(tableName = "documents")
   data class DocumentEntity(
       @PrimaryKey
       @ColumnInfo(name = "id")
       val id: String,
       
       @ColumnInfo(name = "title")
       val title: String,
       
       @ColumnInfo(name = "source")
       val source: String,
       
       @ColumnInfo(name = "content_type")
       val contentType: String,
       
       @ColumnInfo(name = "total_chunks")
       val totalChunks: Int,
       
       @ColumnInfo(name = "created_at")
       val createdAt: Long,
       
       @ColumnInfo(name = "updated_at")
       val updatedAt: Long
   )
   ```

3. **Type Converters**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../storage/VectorTypeConverters.kt
   class VectorTypeConverters {
       
       @TypeConverter
       fun fromFloatArray(value: FloatArray): ByteArray {
           // Serialize FloatArray to ByteArray for storage
           val buffer = ByteBuffer.allocate(value.size * 4)
           buffer.order(ByteOrder.nativeOrder())
           value.forEach { buffer.putFloat(it) }
           return buffer.array()
       }
       
       @TypeConverter
       fun toFloatArray(value: ByteArray): FloatArray {
           // Deserialize ByteArray back to FloatArray
           val buffer = ByteBuffer.wrap(value)
           buffer.order(ByteOrder.nativeOrder())
           return FloatArray(value.size / 4) { buffer.getFloat() }
       }
   }
   ```

4. **DAO Interfaces**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../storage/dao/VectorSearchDao.kt
   @Dao
   interface VectorSearchDao {
       
       @Insert(onConflict = OnConflictStrategy.REPLACE)
       suspend fun insertChunk(chunk: VectorChunkEntity)
       
       @Insert(onConflict = OnConflictStrategy.REPLACE)
       suspend fun insertChunks(chunks: List<VectorChunkEntity>)
       
       @Query("SELECT * FROM vector_chunks WHERE document_id = :documentId ORDER BY chunk_index ASC")
       suspend fun getChunksByDocument(documentId: String): List<VectorChunkEntity>
       
       @Query("DELETE FROM vector_chunks WHERE document_id = :documentId")
       suspend fun deleteChunksByDocument(documentId: String): Int
       
       @Query("SELECT COUNT(*) FROM vector_chunks")
       suspend fun getTotalChunkCount(): Int
       
       // Note: Actual vector similarity search requires sqlite-vec extension
       // This method will need native implementation
       @RawQuery(observedEntities = [VectorChunkEntity::class])
       suspend fun searchSimilar(query: SupportSQLiteQuery): List<VectorChunkWithDistance>
   }
   
   data class VectorChunkWithDistance(
       @Embedded val chunk: VectorChunkEntity,
       @ColumnInfo(name = "distance") val distance: Float
   )
   ```

5. **VectorStore Implementation Update**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../VectorStoreImpl.kt
   @Singleton
   class VectorStoreImpl @Inject constructor(
       private val database: VectorDatabase,
       private val embeddingService: EmbeddingService
   ) : VectorStore {
       
       private val vectorSearchDao = database.vectorSearchDao()
       private val documentDao = database.documentDao()
       
       override suspend fun saveChunks(chunks: List<EmbeddedChunk>): Unit = 
           withContext(Dispatchers.IO) {
               val entities = chunks.map { it.toEntity() }
               vectorSearchDao.insertChunks(entities)
               Log.d(TAG, "Saved ${chunks.size} chunks to persistent storage")
           }
       
       override suspend fun searchSimilar(
           queryEmbedding: FloatArray,
           limit: Int,
           threshold: Float
       ): List<ScoredChunk> = withContext(Dispatchers.IO) {
           
           // TODO: This requires sqlite-vec extension for efficient search
           // Fallback to in-memory search for now
           val allChunks = vectorSearchDao.getAllChunks()
           
           val scored = allChunks.map { entity ->
               val embedding = entity.embedding.toFloatArray()
               val similarity = cosineSimilarity(queryEmbedding, embedding)
               ScoredChunk(entity.toDomain(), similarity)
           }
           
           return@withContext scored
               .filter { it.score >= threshold }
               .sortedByDescending { it.score }
               .take(limit)
       }
       
       // Helper: Fallback cosine similarity calculation
       private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
           // ... existing implementation
       }
   }
   ```

6. **Database Migration**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../storage/migrations/VectorDatabaseMigrations.kt
   object VectorDatabaseMigrations {
       
       val MIGRATION_1_2 = object : Migration(1, 2) {
           override fun migrate(database: SupportSQLiteDatabase) {
               // Create vector_chunks table
               database.execSQL("""
                   CREATE TABLE IF NOT EXISTS vector_chunks (
                       id TEXT PRIMARY KEY NOT NULL,
                       document_id TEXT NOT NULL,
                       content TEXT NOT NULL,
                       embedding BLOB NOT NULL,
                       chunk_index INTEGER NOT NULL,
                       created_at INTEGER NOT NULL,
                       FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
                   )
               """)
               
               database.execSQL("""
                   CREATE INDEX IF NOT EXISTS index_vector_chunks_document_id 
                   ON vector_chunks(document_id)
               """)
               
               database.execSQL("""
                   CREATE INDEX IF NOT EXISTS index_vector_chunks_created_at 
                   ON vector_chunks(created_at)
               """)
               
               // TODO: Once sqlite-vec is available, create vector index:
               // database.execSQL("CREATE VIRTUAL TABLE vec_index USING vec0(embedding)")
           }
       }
   }
   ```

7. **Dependency Injection**:
   ```kotlin
   // File: core-rag/src/main/kotlin/.../di/StorageModule.kt
   @Module
   @InstallIn(SingletonComponent::class)
   object StorageModule {
       
       @Provides
       @Singleton
       fun provideVectorDatabase(
           @ApplicationContext context: Context
       ): VectorDatabase {
           return Room.databaseBuilder(
               context,
               VectorDatabase::class.java,
               VectorDatabase.DATABASE_NAME
           )
           .addMigrations(VectorDatabaseMigrations.MIGRATION_1_2)
           .fallbackToDestructiveMigration() // Development only
           .build()
       }
       
       @Provides
       @Singleton
       fun provideVectorSearchDao(database: VectorDatabase): VectorSearchDao {
           return database.vectorSearchDao()
       }
   }
   ```

8. **Unit Tests**:
   - In-memory database tests for CRUD operations
   - Type converter tests (FloatArray ↔ ByteArray)
   - Migration tests
   - Mock-based tests for VectorStoreImpl

### Technical Specifications

#### Performance Considerations
```kotlin
// Batch insertion for performance
suspend fun batchInsertChunks(chunks: List<EmbeddedChunk>) {
    database.withTransaction {
        chunks.chunked(100).forEach { batch ->
            vectorSearchDao.insertChunks(batch.map { it.toEntity() })
        }
    }
}

// Pagination for large result sets
@Query("""
    SELECT * FROM vector_chunks 
    WHERE document_id = :documentId 
    ORDER BY chunk_index ASC 
    LIMIT :limit OFFSET :offset
""")
suspend fun getChunksPaginated(documentId: String, limit: Int, offset: Int): List<VectorChunkEntity>
```

#### Fallback Search Strategy
```kotlin
/**
 * Note: Efficient vector search requires sqlite-vec extension.
 * This implementation provides a fallback that works without the extension
 * but has O(n) complexity where n = total number of chunks.
 * 
 * For production with large datasets, sqlite-vec native extension is required.
 */
private suspend fun fallbackVectorSearch(
    queryEmbedding: FloatArray,
    limit: Int,
    threshold: Float
): List<ScoredChunk> {
    // Load all chunks (acceptable for small datasets < 10K chunks)
    val allChunks = vectorSearchDao.getAllChunks()
    
    if (allChunks.size > 10_000) {
        Log.w(TAG, "Large dataset detected (${allChunks.size} chunks). " +
            "Consider implementing sqlite-vec for O(log n) search.")
    }
    
    // Compute similarity for all chunks
    return allChunks
        .map { entity ->
            val similarity = cosineSimilarity(queryEmbedding, entity.embedding.toFloatArray())
            ScoredChunk(entity.toDomain(), similarity)
        }
        .filter { it.score >= threshold }
        .sortedByDescending { it.score }
        .take(limit)
}
```

### Implementation Notes

⚠️ **Important Limitations**:
1. This issue creates **Room database architecture** - efficient vector search requires sqlite-vec
2. Fallback implementation has O(n) complexity (acceptable for small datasets)
3. sqlite-vec integration requires:
   - Compiling C extension for Android
   - Custom SQLite build with Room
   - Native CMake configuration
   - These require human C/Android expertise
4. The architecture is designed to make sqlite-vec integration easier in the future

### Files to Create
- `core-rag/src/main/kotlin/.../storage/VectorDatabase.kt`
- `core-rag/src/main/kotlin/.../storage/entities/VectorChunkEntity.kt`
- `core-rag/src/main/kotlin/.../storage/entities/DocumentEntity.kt`
- `core-rag/src/main/kotlin/.../storage/VectorTypeConverters.kt`
- `core-rag/src/main/kotlin/.../storage/dao/VectorSearchDao.kt`
- `core-rag/src/main/kotlin/.../storage/dao/DocumentDao.kt`
- `core-rag/src/main/kotlin/.../storage/migrations/VectorDatabaseMigrations.kt`
- `core-rag/src/main/kotlin/.../di/StorageModule.kt`
- `core-rag/src/test/kotlin/.../storage/VectorDatabaseTest.kt`
- `core-rag/src/test/kotlin/.../storage/VectorTypeConvertersTest.kt`

### Files to Modify
- `core-rag/src/main/kotlin/.../VectorStoreImpl.kt`
- `core-rag/build.gradle.kts` (add Room dependencies)

### References
- Current implementation: `core-rag/src/main/kotlin/.../VectorStoreImpl.kt`
- sqlite-vec: https://github.com/asg017/sqlite-vec
- Room documentation: https://developer.android.com/training/data-storage/room
- Architecture: `docs/architecture.md`

---

## Issue #4: Core-Multimodal Interface Documentation & Mock Improvement

### Title
`[core-multimodal] Improve mock implementations with better documentation and testing infrastructure`

### Description

**Context**:
`VisionProcessingEngineImpl`, `SpeechToTextEngineImpl`, and `TextToSpeechEngineImpl` contain mock implementations with placeholder functions. While native implementations require C++ expertise, the Kotlin infrastructure can be improved with better mocks, comprehensive tests, and documentation for future integration.

**Objective**:
Enhance the multimodal module's testing infrastructure, improve mock implementations to be more realistic, and document the exact requirements for future native integration.

### Acceptance Criteria

1. **Enhanced Mock Implementations**:
   - Improve mock vision processing to return contextually relevant responses
   - Enhance mock STT to simulate more realistic transcription delays
   - Improve mock TTS audio generation to better match text length
   - Add configurable mock behavior for testing different scenarios

2. **Comprehensive Test Coverage**:
   - Unit tests for all multimodal interfaces
   - Integration tests with LLM pipeline
   - Performance benchmarks for mock implementations
   - Error handling tests

3. **Native Integration Documentation**:
   - Document exact native method signatures required
   - Provide pseudo-code for C++ implementations
   - List required dependencies (LLaVA, Whisper.cpp, TTS library)
   - Document JNI type conversions needed

4. **Mock Configuration System**:
   ```kotlin
   // File: core-multimodal/src/main/kotlin/.../config/MockConfig.kt
   data class MultimodalMockConfig(
       val visionProcessingDelay: Long = 500L,  // ms
       val sttTranscriptionDelay: Long = 300L,   // ms
       val ttsAudioGenerationDelay: Long = 400L, // ms
       val simulateErrors: Boolean = false,
       val errorRate: Float = 0.1f,
       val visionResponseTemplate: String = "I can see {description}"
   )
   ```

5. **Realistic Mock Vision Processing**:
   ```kotlin
   private suspend fun generateMockVisionResponse(
       bitmap: Bitmap,
       prompt: String,
       config: MultimodalMockConfig
   ): String {
       // Simulate processing delay
       delay(config.visionProcessingDelay)
       
       // Analyze bitmap characteristics
       val imageAnalysis = analyzeImageCharacteristics(bitmap)
       
       // Generate contextual response
       val description = buildString {
           append("Image dimensions: ${bitmap.width}x${bitmap.height} pixels. ")
           
           when {
               imageAnalysis.isDark -> append("The image appears to be quite dark. ")
               imageAnalysis.isBright -> append("The image is well-lit. ")
           }
           
           when {
               imageAnalysis.hasMultipleColors -> 
                   append("It contains multiple distinct colors. ")
               imageAnalysis.isMonochrome -> 
                   append("It appears to be monochrome. ")
           }
           
           if (prompt.isNotBlank()) {
               append("Based on your prompt '$prompt', ")
               append("I would need actual vision model inference to provide specific details.")
           }
       }
       
       return description
   }
   
   private fun analyzeImageCharacteristics(bitmap: Bitmap): ImageAnalysis {
       val pixels = IntArray(bitmap.width * bitmap.height)
       bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
       
       val avgBrightness = pixels.map { pixel ->
           val r = (pixel shr 16) and 0xFF
           val g = (pixel shr 8) and 0xFF
           val b = pixel and 0xFF
           (r + g + b) / 3
       }.average()
       
       val colorVariance = calculateColorVariance(pixels)
       
       return ImageAnalysis(
           isDark = avgBrightness < 50,
           isBright = avgBrightness > 200,
           hasMultipleColors = colorVariance > 1000,
           isMonochrome = colorVariance < 100
       )
   }
   ```

6. **Native Integration Documentation**:
   ```markdown
   # File: core-multimodal/NATIVE_INTEGRATION_GUIDE.md
   
   ## Vision Processing Native Implementation
   
   ### Required Dependencies
   - LLaVA model library (or similar vision-language model)
   - CLIP image encoder
   - Image preprocessing utilities
   
   ### JNI Method Signature
   ```cpp
   extern "C" JNIEXPORT jstring JNICALL
   Java_com_nervesparks_iris_core_multimodal_vision_VisionProcessingEngineImpl_nativeProcessVision(
       JNIEnv *env, 
       jobject thiz, 
       jlong model_id,
       jobject bitmap,
       jstring prompt,
       jobject params
   ) {
       // 1. Extract Android Bitmap to native image buffer
       // 2. Preprocess image (resize, normalize, convert RGB)
       // 3. Encode image with CLIP encoder
       // 4. Combine image embeddings with text prompt
       // 5. Run LLM inference with multimodal context
       // 6. Return generated description as jstring
   }
   ```
   
   ### Implementation Steps
   1. **Bitmap Extraction**:
      - Use AndroidBitmap_lockPixels() to access pixel data
      - Convert RGBA to RGB format
      - Handle different Android bitmap formats
   
   2. **Image Preprocessing**:
      - Resize to model input size (typically 336x336 or 224x224)
      - Normalize pixel values (model-specific mean/std)
      - Arrange in model-expected format (NCHW or NHWC)
   
   3. **Vision Encoding**:
      - Load CLIP/vision encoder model
      - Generate image embeddings
      - Project to LLM embedding space
   
   4. **Multimodal Inference**:
      - Combine image embeddings with text prompt tokens
      - Run transformer inference
      - Decode output tokens to text
   
   ### Performance Considerations
   - Image preprocessing should be optimized (use NEON on ARM)
   - Consider INT8 quantization for mobile deployment
   - Cache vision encoder outputs when processing multiple prompts for same image
   ```

7. **Test Utilities**:
   ```kotlin
   // File: core-multimodal/src/test/kotlin/.../TestImageGenerator.kt
   object TestImageGenerator {
       fun createSolidColorBitmap(width: Int, height: Int, color: Int): Bitmap {
           val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
           val canvas = Canvas(bitmap)
           canvas.drawColor(color)
           return bitmap
       }
       
       fun createGradientBitmap(width: Int, height: Int): Bitmap {
           val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
           val canvas = Canvas(bitmap)
           val paint = Paint().apply {
               shader = LinearGradient(
                   0f, 0f, width.toFloat(), height.toFloat(),
                   Color.BLACK, Color.WHITE,
                   Shader.TileMode.CLAMP
               )
           }
           canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
           return bitmap
       }
       
       fun createTestImageWithText(text: String): Bitmap {
           // Create bitmap with text for vision processing tests
       }
   }
   ```

### Implementation Notes

✅ **What This Issue CAN Accomplish**:
- Improved mock implementations that better simulate real behavior
- Comprehensive test infrastructure for multimodal features
- Clear documentation for future native integration
- Better developer experience when working with multimodal APIs

⚠️ **What This Issue CANNOT Accomplish**:
- Actual native vision/STT/TTS implementations
- Real AI model inference
- Hardware-accelerated processing
- Production-quality multimodal features

### Files to Create
- `core-multimodal/NATIVE_INTEGRATION_GUIDE.md`
- `core-multimodal/src/main/kotlin/.../config/MockConfig.kt`
- `core-multimodal/src/test/kotlin/.../TestImageGenerator.kt`
- `core-multimodal/src/test/kotlin/.../VisionProcessingEngineTest.kt`
- `core-multimodal/src/test/kotlin/.../SpeechToTextEngineTest.kt`
- `core-multimodal/src/test/kotlin/.../TextToSpeechEngineTest.kt`
- `core-multimodal/src/test/kotlin/.../MultimodalIntegrationTest.kt`

### Files to Modify
- `core-multimodal/src/main/kotlin/.../vision/VisionProcessingEngineImpl.kt`
- `core-multimodal/src/main/kotlin/.../voice/SpeechToTextEngineImpl.kt`
- `core-multimodal/src/main/kotlin/.../voice/TextToSpeechEngineImpl.kt`

### References
- Current implementations in `core-multimodal/src/main/kotlin/`
- Architecture: `docs/architecture.md`
- Limitations analysis: `docs/COPILOT_AGENT_LIMITATIONS_ANALYSIS.md`

---

## Summary of Issue Prompts

| Issue | What Agent CAN Do | What Agent CANNOT Do | Expected Outcome |
|-------|-------------------|---------------------|------------------|
| **#1: LLM JNI Scaffolding** | Create file structure, CMake template, method stubs, headers | Implement actual llama.cpp integration, compile native code | Architecture ready for human C++ developer |
| **#2: Remote Catalog** | Design interfaces, data models, business logic, tests | Make actual API calls, handle HF auth, test downloads | Architecture ready for API integration |
| **#3: Vector Storage** | Design Room schema, DAOs, type converters, migrations | Compile sqlite-vec extension, optimize vector search | Persistent storage with fallback search |
| **#4: Multimodal Mocks** | Improve mocks, write tests, document native requirements | Implement native vision/STT/TTS, integrate AI models | Better testing infrastructure and docs |

These issue prompts are designed to maximize what Copilot Agent can contribute while being realistic about its limitations. Each issue focuses on architecture, interfaces, and Kotlin/Java code that Agent can generate, while clearly documenting what requires human expertise.