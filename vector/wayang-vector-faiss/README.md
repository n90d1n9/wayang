# Wayang FAISS Vector Store

Native FAISS (Facebook AI Similarity Search) integration for the Wayang platform via **JDK 25 FFM** (Foreign Function & Memory API).

## Features

- **Native FAISS Integration**: Direct FFM bindings to the FAISS C library — no JNI, no wrappers
- **All FAISS Index Types**: Flat, HNSW, IVF, IVFFlat, ScalarQuantizer, LSH, PreTransform, Replicas, Shards, Binary
- **All FAISS Capabilities**: Training, search, range search, reconstruction, clustering (k-means), auto-tuning, vector transforms (PCA, OPQ, ITQ), index I/O, and cloning
- **Configurable**: Index type, dimension, metric, and persistence via Quarkus config
- **Default Agent Memory**: Used as the default vector store for agent memory

## Prerequisites

**Build the native FAISS library:**

```bash
./scripts/build-faiss.sh
```

This clones FAISS to `~/.wayang/vendor/faiss`, builds `libfaiss_c`, and copies it to `~/.wayang/lib/`.

**Requirements:**
- JDK 25+
- CMake 3.23+
- C++ compiler (clang/gcc)
- BLAS library (OpenBLAS, MKL, or Accelerate on macOS)

## Configuration

```properties
# Vector store type (default: faiss)
wayang.vector.store.type=faiss

# Vector dimension (default: 768)
wayang.vector.faiss.dimension=768

# FAISS index factory string (default: Flat)
# Examples: "Flat", "HNSW32", "IVF100,Flat", "PQ16", "SQ8", "IVF256,PQ32"
wayang.vector.faiss.index.type=Flat

# Index persistence path (optional)
wayang.vector.faiss.index.path=/path/to/index.faiss
```

## Maven Dependency

```xml
<dependency>
    <groupId>tech.kayys.wayang</groupId>
    <artifactId>wayang-vector-faiss</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Basic (via VectorStore SPI)

```java
FaissVectorStore store = new FaissVectorStore(768, "Flat");
store.store(List.of(new VectorEntry("id1", embedding, "content", metadata)));
List<VectorEntry> results = store.search(new VectorQuery(queryVec, 10, 0.7f)).await().indefinitely();
```

### Advanced (via FaissIndex directly)

```java
try (FaissIndex index = new FaissIndex(768, "HNSW32")) {
    index.add("doc1", embedding1);
    index.add("doc2", embedding2);

    List<FaissIndex.SearchResult> results = index.search(queryVector, 5);
    for (var r : results) {
        System.out.printf("ID=%s distance=%.4f score=%.4f%n", r.id(), r.distance(), r.score());
    }

    index.save(Path.of("/path/to/index.faiss"));
}
```

### Clustering

```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment centroids = arena.allocate(ValueLayout.JAVA_FLOAT, k * d);
    float error = FaissClusteringBindings.kmeansClustering(arena, d, n, k, vectors, centroids);
}
```

### AutoTune

```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment space = FaissAutoTuneBindings.newParameterSpace(arena);
    FaissAutoTuneBindings.setIndexParameter(space, index, "nprobe", 32);
}
```

## Architecture

```
Agent Memory Manager → VectorMemoryStore → VectorStoreAdapter → VectorStore
                                                                     ↓
                                                              FaissVectorStore
                                                                     ↓
                                                                FaissIndex
                                                                     ↓
                                                          FaissIndexBindings (FFM)
                                                                     ↓
                                                         libfaiss_c.dylib/.so
                                                                     ↓
                                                        ~/.wayang/vendor/faiss
```

```mermaid
graph TD
    A["Agent Memory Manager"] --> B["VectorMemoryStore SPI"]
    B --> C["VectorStoreAdapter"]
    C --> D["VectorStore SPI"]
    D --> E["FaissVectorStore"]
    E --> F["FaissIndex"]
    F --> G["FaissIndexBindings"]
    G --> H["FaissNative<br/>SymbolLookup + Linker"]
    H --> I["libfaiss_c.dylib/.so"]
    I --> J["~/.wayang/vendor/faiss"]
    
    K["FaissIVFBindings"] --> H
    L["FaissHNSWBindings"] --> H
    M["FaissClusteringBindings"] --> H
    N["FaissIOBindings"] --> H
    O["FaissTransformBindings"] --> H
    P["FaissAutoTuneBindings"] --> H
    Q["FaissBinaryIndexBindings"] --> H
    R["FaissMetaIndexBindings"] --> H
    S["FaissScalarQuantizerBindings"] --> H
    T["FaissIndexFlatBindings"] --> H
```

## FFM Binding Classes

| Class | FAISS C Header | Coverage |
|---|---|---|
| `FaissIndexBindings` | `Index_c.h` + `index_factory_c.h` | 18 core functions |
| `FaissIndexFlatBindings` | `IndexFlat_c.h` | Flat/L2/IP/RefineFlat |
| `FaissIVFBindings` | `IndexIVF_c.h` + `IndexIVFFlat_c.h` | IVF operations |
| `FaissHNSWBindings` | `IndexHNSW_c.h` | HNSW graph index |
| `FaissScalarQuantizerBindings` | `IndexScalarQuantizer_c.h` | SQ/IVFSQ quantization |
| `FaissMetaIndexBindings` | `MetaIndexes_c.h` + 3 more | IDMap, Replicas, Shards, PreTransform, LSH |
| `FaissBinaryIndexBindings` | `IndexBinary_c.h` + 2 more | Binary index lifecycle |
| `FaissClusteringBindings` | `Clustering_c.h` | K-means clustering |
| `FaissAutoTuneBindings` | `AutoTune_c.h` | Parameter space tuning |
| `FaissIOBindings` | `index_io_c.h` + `clone_index_c.h` | File I/O + clone |
| `FaissTransformBindings` | `VectorTransform_c.h` | PCA, OPQ, ITQ, linear |

## License

Apache License 2.0
