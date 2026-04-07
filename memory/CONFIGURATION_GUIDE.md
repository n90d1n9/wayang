# Wayang Memory Module - Configuration Guide

## Overview
This guide explains all configuration options for the wayang-gollek memory module and how to enable the latest improvements.

## Quick Configuration Examples

### Minimal Configuration (Development)
```properties
# application.properties
gamelan.embedding.provider=local
wayang.memory.short.window.size=20
```

### Production Configuration with OpenAI
```properties
# Use OpenAI for better embeddings
gamelan.embedding.provider=openai
gamelan.embedding.openai.api-key=${OPENAI_API_KEY}
gamelan.embedding.openai.model=text-embedding-3-small
gamelan.embedding.openai.timeout-secs=30
gamelan.embedding.openai.cache-enabled=true

# Short-term memory settings
wayang.memory.short.window.size=50
wayang.memory.short.ttl-minutes=1440

# Context settings
wayang.memory.agent.context.limit=10
wayang.memory.agent.context.sort-by=timestamp
```

---

## Configuration Properties Reference

### Embedding Service Configuration

#### Provider Selection
```properties
# Which embedding service to use
# Options: local, local-tfidf, openai
gamelan.embedding.provider=openai
```

#### Local TFIDF Configuration
```properties
# Model type for local embeddings
gamelan.embedding.local.model=tfidf

# Vocabulary size for TFIDF
gamelan.embedding.local.vocab.size=10000

# Minimum document frequency
gamelan.embedding.local.min-df=2

# Maximum document frequency (0.95 = 95%)
gamelan.embedding.local.max-df=0.95
```

#### OpenAI Configuration
```properties
# Required: OpenAI API key
gamelan.embedding.openai.api-key=sk-...

# Model selection
# - text-embedding-3-small: 1536 dimensions, fast, cheap
# - text-embedding-3-large: 3072 dimensions, better quality
gamelan.embedding.openai.model=text-embedding-3-small

# API request timeout in seconds
gamelan.embedding.openai.timeout-secs=30

# Enable embedding caching (recommended)
gamelan.embedding.openai.cache-enabled=true

# Cache size (max number of cached embeddings)
gamelan.embedding.openai.cache.max-size=100000

# Cache TTL in hours (0 = no expiration)
gamelan.embedding.openai.cache.ttl-hours=24

# Batch size for batch API calls
gamelan.embedding.openai.batch.size=20

# Max retries on API failure
gamelan.embedding.openai.retries.max=3

# Retry backoff in milliseconds
gamelan.embedding.openai.retries.backoff-ms=1000
```

---

### Memory Type Configuration

#### Short-Term Memory (Conversation Buffer)
```properties
# Window size: how many recent entries to keep
wayang.memory.short.window.size=20

# TTL in minutes (0 = no expiration)
wayang.memory.short.ttl-minutes=1440

# Memory eviction strategy: FIFO, LRU, LFU
wayang.memory.short.eviction-strategy=FIFO

# Persistence: whether to persist to database
wayang.memory.short.persist=true
```

#### Episodic Memory (Event-Based)
```properties
# Event retention: days to keep events
wayang.memory.episodic.retention-days=90

# Automatic consolidation to long-term
wayang.memory.episodic.consolidate-after-days=30

# Storage backend: memory, redis, postgres
wayang.memory.episodic.backend=postgres

# Indexing strategy: none, keyword, semantic
wayang.memory.episodic.indexing=semantic
```

#### Semantic Memory (Knowledge Base)
```properties
# Semantic search threshold (0.0-1.0)
wayang.memory.semantic.similarity-threshold=0.7

# Update frequency: how often to refresh semantics
wayang.memory.semantic.update-interval-hours=24

# LSH bucket count for approximate search
wayang.memory.semantic.lsh.buckets=10

# Backend storage
wayang.memory.semantic.backend=postgres
```

#### Long-Term Memory (Vector Store)
```properties
# Vector store backend
wayang.memory.longterm.backend=redis

# Redis connection
wayang.memory.redis.host=localhost
wayang.memory.redis.port=6379
wayang.memory.redis.password=

# Key prefix for Redis namespace
wayang.memory.redis.key-prefix=gollek:memory:

# Persistence strategy: aof, rdb, none
wayang.memory.redis.persistence=aof
```

#### Working Memory (Task Context)
```properties
# Maximum slots in working memory
wayang.memory.working.max-slots=10

# Context window size
wayang.memory.working.context-size=5

# Auto-consolidate threshold (number of items)
wayang.memory.working.consolidate-threshold=20

# Backend
wayang.memory.working.backend=memory
```

---

### Agent Memory Configuration

#### Context Retrieval
```properties
# Maximum memories to retrieve for context
wayang.memory.agent.context.limit=10

# Sort context by: timestamp, relevance, frequency
wayang.memory.agent.context.sort-by=timestamp

# Include metadata in context
wayang.memory.agent.context.include-metadata=true

# Filter by memory type: all, semantic, episodic, working
wayang.memory.agent.context.memory-types=all

# Minimum relevance score (0.0-1.0)
wayang.memory.agent.context.min-relevance=0.0
```

#### Agent Session Management
```properties
# Session timeout in minutes
wayang.memory.agent.session.timeout-minutes=60

# Max concurrent sessions per agent
wayang.memory.agent.session.max-concurrent=5

# Clear memories on session end
wayang.memory.agent.session.auto-clear=false

# Session persistence
wayang.memory.agent.session.persist=true
```

---

### Vector Store Configuration

#### Search Settings
```properties
# Vector similarity metric: cosine, euclidean, dot-product
wayang.memory.vector.similarity-metric=cosine

# Approximate search: enabled/disabled
wayang.memory.vector.approximate-search=true

# Approximate search algorithm: HNSW, IVF, LSH
wayang.memory.vector.approximate-algorithm=HNSW

# HNSW max connections per node
wayang.memory.vector.hnsw.max-connections=16

# IVF number of clusters
wayang.memory.vector.ivf.clusters=100
```

#### Filtering
```properties
# Enable metadata filtering
wayang.memory.vector.filtering=true

# Filter index: enabled/disabled
wayang.memory.vector.filter-index=true

# Filter backend: memory, rocksdb
wayang.memory.vector.filter-backend=rocksdb
```

---

### Memory Consolidation Configuration

#### Promotion Rules
```properties
# Enable automatic memory consolidation
wayang.memory.consolidation.enabled=true

# Consolidation schedule (cron expression)
wayang.memory.consolidation.schedule=0 0 * * *

# Promotion threshold: confidence score (0.0-1.0)
wayang.memory.consolidation.threshold=0.7

# Working → Short-Term after N accesses
wayang.memory.consolidation.working-access-threshold=3

# Short-Term → Long-Term after N days
wayang.memory.consolidation.shortterm-age-threshold-days=7

# Consolidation batch size
wayang.memory.consolidation.batch-size=100
```

#### Memory Decay
```properties
# Enable memory decay/forgetting
wayang.memory.decay.enabled=true

# Decay function: linear, exponential, logarithmic
wayang.memory.decay.function=exponential

# Half-life in days (when relevance drops to 50%)
wayang.memory.decay.halflife-days=30

# Minimum relevance before removal
wayang.memory.decay.min-relevance=0.1
```

---

### Monitoring & Performance

#### Metrics
```properties
# Enable memory metrics collection
wayang.memory.metrics.enabled=true

# Metrics namespace/prefix
wayang.memory.metrics.prefix=gollek.memory

# Detailed metrics (may impact performance)
wayang.memory.metrics.detailed=false
```

#### Caching
```properties
# Global cache enabled
wayang.memory.cache.enabled=true

# Cache backend: memory, redis
wayang.memory.cache.backend=redis

# Max cache size in MB
wayang.memory.cache.max-size-mb=500

# Cache TTL in hours
wayang.memory.cache.ttl-hours=24
```

#### Performance Tuning
```properties
# Thread pool size for async operations
wayang.memory.async.thread-pool-size=10

# Queue size for async tasks
wayang.memory.async.queue-size=1000

# Enable query optimization
wayang.memory.query.optimization=true

# Max query result size
wayang.memory.query.max-results=1000

# Query timeout in seconds
wayang.memory.query.timeout-secs=30
```

---

### Security & Compliance

#### Data Protection
```properties
# Enable encryption at rest
wayang.memory.security.encryption-at-rest=true

# Encryption algorithm: AES-256, AES-128
wayang.memory.security.encryption-algorithm=AES-256

# Encryption key (use environment variable!)
wayang.memory.security.encryption-key=${MEMORY_ENCRYPTION_KEY}

# Enable compression
wayang.memory.security.compression=true
```

#### Data Retention
```properties
# Default retention period in days
wayang.memory.retention.default-days=365

# Max retention period (can't exceed this)
wayang.memory.retention.max-days=2555

# Auto-delete expired memories
wayang.memory.retention.auto-delete=true

# Deletion batch size per run
wayang.memory.retention.batch-size=1000
```

#### Access Control
```properties
# Enable memory access logging
wayang.memory.audit.logging=true

# Log queries
wayang.memory.audit.log-queries=false

# Audit log retention days
wayang.memory.audit.retention-days=90
```

---

## Environment-Specific Examples

### Development Environment
```properties
# Minimal setup with local embeddings
gamelan.embedding.provider=local
wayang.memory.redis.host=localhost:6379
wayang.memory.short.window.size=20
wayang.memory.metrics.enabled=false
wayang.memory.cache.enabled=false
```

### Staging Environment
```properties
# OpenAI embeddings with caching
gamelan.embedding.provider=openai
gamelan.embedding.openai.cache-enabled=true
wayang.memory.redis.host=redis-staging:6379
wayang.memory.short.window.size=50
wayang.memory.metrics.enabled=true
wayang.memory.cache.backend=redis
```

### Production Environment
```properties
# Full-featured production setup
gamelan.embedding.provider=openai
gamelan.embedding.openai.api-key=${OPENAI_API_KEY}
gamelan.embedding.openai.cache-enabled=true
gamelan.embedding.openai.cache.ttl-hours=24

wayang.memory.redis.host=${REDIS_HOST}
wayang.memory.redis.port=${REDIS_PORT}
wayang.memory.redis.password=${REDIS_PASSWORD}

wayang.memory.short.window.size=100
wayang.memory.short.persist=true

wayang.memory.consolidation.enabled=true
wayang.memory.decay.enabled=true

wayang.memory.security.encryption-at-rest=true
wayang.memory.security.encryption-key=${ENCRYPTION_KEY}

wayang.memory.retention.default-days=365
wayang.memory.retention.auto-delete=true

wayang.memory.metrics.enabled=true
wayang.memory.cache.backend=redis
```

---

## Configuration Validation

### Required Fields
- `gamelan.embedding.openai.api-key` (if using OpenAI provider)

### Optional with Defaults
- `gamelan.embedding.provider`: defaults to `local`
- `wayang.memory.short.window.size`: defaults to `20`
- `wayang.memory.agent.context.limit`: defaults to `10`

### Validation Rules
1. Window sizes must be > 0
2. Timeout values must be > 0
3. Similarity thresholds must be 0.0-1.0
4. Cache sizes must be >= 0 (0 disables)
5. TTL values must be >= 0 (0 = no expiration)

---

## Troubleshooting Configuration

### Common Issues

**Issue: Empty context retrieved**
```properties
# Solution: Verify filter is correct
wayang.memory.agent.context.sort-by=timestamp  # or relevance
wayang.memory.agent.context.limit=10           # increase if needed
```

**Issue: Slow embeddings**
```properties
# Solution: Enable caching
gamelan.embedding.openai.cache-enabled=true
gamelan.embedding.openai.cache.max-size=100000
```

**Issue: Redis connection errors**
```properties
# Solution: Check connection settings
wayang.memory.redis.host=redis.example.com
wayang.memory.redis.port=6379
wayang.memory.redis.password=  # if needed
```

**Issue: Out of memory**
```properties
# Solution: Reduce caches and limits
wayang.memory.cache.max-size-mb=256
wayang.memory.agent.context.limit=5
```

---

## Next Steps

1. Choose your configuration environment (dev/staging/prod)
2. Copy the example configuration
3. Update with your specific values
4. Test context retrieval: `GET /api/memory/agent/{id}/context`
5. Monitor metrics: `GET /metrics/gollek_memory_*`

For more details, see:
- `MEMORY_IMPROVEMENTS.md` - Implementation details
- `VECTOR_MEMORY_INTEGRATION.md` - Vector store specifics
- `README.md` - Module overview
