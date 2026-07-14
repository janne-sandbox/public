# REQ-003: Horizontal and Vertical Scalability

## Requirement ID
REQ-003

## Category
Architecture / Performance

## Priority
High

## Description
The DIMSE Server application shall support both horizontal scaling (multiple instances/pods) and vertical scaling (increased resources per instance) to handle varying workload demands. The system must safely operate with shared storage and shared database resources across multiple concurrent instances.

## Rationale
- Healthcare imaging workloads vary significantly (peak hours, batch uploads, emergency situations)
- Cloud-native deployments require horizontal scaling for high availability and fault tolerance
- Different deployment sizes (small clinics vs. large hospitals) require vertical scaling options
- Cost optimization through elastic scaling based on demand
- Regulatory requirements for high availability in medical imaging systems

## Acceptance Criteria

### Horizontal Scaling
1. **Multiple Instance Support**: System shall support 2-100+ concurrent instances
2. **Shared Storage**: All instances can safely read/write to shared network storage (NFS, Azure Files, etc.)
3. **Shared Database**: All instances connect to same database with proper transaction isolation
4. **No State Conflicts**: Instances operate independently without coordination requirements
5. **Load Distribution**: DICOM workload can be distributed across instances via load balancer
6. **Concurrent Writes**: Multiple instances writing same study/series handled safely
7. **Message Bus**: ActiveMQ supports multiple publishers without message loss

### Vertical Scaling
1. **Thread Pool Configuration**: DICOM thread pool size adjustable (1-100+ threads)
2. **Memory Scaling**: Heap size configurable for different workload sizes
3. **Database Connections**: Connection pool size adjustable per instance
4. **Storage I/O**: Support for high-throughput storage backends

### Safety Guarantees
1. **File Name Uniqueness**: No file overwrites when multiple instances receive same SOP Instance
2. **Database Integrity**: UNIQUE constraints prevent duplicate metadata
3. **Transaction Isolation**: Database transactions use READ COMMITTED or higher
4. **Idempotent Operations**: Re-processing same DICOM instance is safe

## Design Verification

### Stateless Architecture
✅ **No Local State**: No in-memory state shared across requests
✅ **Database as Source of Truth**: All persistent data in database
✅ **Immutable File Storage**: DICOM files written once, never modified

### Shared Storage Safety
✅ **Unique File Paths**: Files stored at `{studyUID}/{seriesUID}/instance_{sopUID}.dcm`
✅ **No File Locking Required**: Atomic writes with unique names
✅ **Concurrent Read Safety**: Multiple instances can read same files simultaneously

### Database Concurrency
✅ **Transactional Writes**: All metadata writes in transactions
✅ **Unique Constraints**: 
- study.study_instance_uid (UNIQUE)
- series.series_instance_uid (UNIQUE)
- instance.sop_instance_uid (UNIQUE)
✅ **Optimistic Concurrency**: JPA/Hibernate handles conflicts
✅ **Connection Pooling**: HikariCP with configurable pool size

### Message Bus Scaling
✅ **Multiple Publishers**: ActiveMQ supports concurrent publishers
✅ **Queue-based (not topic)**: Each message consumed once
✅ **Persistent Messages**: Messages survive broker restart

### Load Balancing Considerations
✅ **Stateless Endpoints**: REST API can be load balanced
✅ **DICOM Protocol**: Requires Layer 4 (TCP) load balancer or DNS round-robin
✅ **Session Affinity**: Not required (stateless)

## Verification Method

### Horizontal Scaling Test
```bash
# Deploy 3 instances with shared storage and database
docker-compose up --scale dimse-server=3

# Send 1000 DICOM instances concurrently via load balancer
for i in {1..1000}; do
  dcmsend -c DIMSE_SCP@loadbalancer:11112 test-${i}.dcm &
done
wait

# Verify:
# 1. All 1000 instances stored exactly once (no duplicates)
# 2. All metadata in database
# 3. No file conflicts or overwrites
# 4. All ActiveMQ messages published
```

### Vertical Scaling Test
```bash
# Test with minimal resources
docker run -e DICOM_THREADS=2 -m 512M dimse-server

# Test with high resources
docker run -e DICOM_THREADS=50 -m 4G dimse-server

# Verify:
# 1. Both configurations work correctly
# 2. Performance scales with resources
# 3. No resource exhaustion errors
```

### Concurrent Write Test
```bash
# Send same DICOM file from 3 different clients simultaneously
dcmsend -c DIMSE_SCP@server1:11112 same-file.dcm &
dcmsend -c DIMSE_SCP@server2:11112 same-file.dcm &
dcmsend -c DIMSE_SCP@server3:11112 same-file.dcm &
wait

# Verify:
# 1. File stored exactly once (idempotent)
# 2. Database has single entry
# 3. No errors or conflicts
```

## Configuration Parameters

### Horizontal Scaling
```properties
# Storage (must be shared filesystem)
dicom.storage.path=/mnt/shared-storage/dicom

# Database (connection string to shared database)
spring.datasource.url=jdbc:postgresql://shared-db:5432/dimse

# ActiveMQ (shared message broker)
spring.activemq.broker-url=tcp://activemq-cluster:61616
```

### Vertical Scaling
```properties
# Thread pool size (per instance)
dicom.server.threads=10  # Small: 2-5, Medium: 10-20, Large: 50-100

# JVM heap size (in docker-compose or k8s)
JAVA_OPTS=-Xms512m -Xmx2g  # Small: 512M-1G, Large: 2G-8G

# Database connection pool (per instance)
spring.datasource.hikari.maximum-pool-size=10  # Small: 5-10, Large: 20-50
```

## Deployment Patterns

### Pattern 1: Kubernetes Horizontal Pod Autoscaler (HPA)
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: dimse-server-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: dimse-server
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Pattern 2: Docker Compose with Load Balancer
```yaml
version: '3.8'
services:
  dimse-server:
    image: dimse-server:latest
    deploy:
      replicas: 3
    volumes:
      - shared-storage:/storage
    environment:
      - DATASOURCE_URL=jdbc:postgresql://postgres:5432/dimse
```

### Pattern 3: Azure App Service with Scale Out
- Manual scale: 1-100 instances
- Auto-scale rules: CPU > 70%, Memory > 80%, Custom metrics
- Azure Files for shared storage
- Azure SQL Database for metadata

## Limitations and Constraints

1. **DICOM Port Binding**: Each instance needs unique port OR use Layer 4 load balancer
2. **Storage Performance**: Shared storage must support concurrent I/O (NFS v4, Azure Files Premium)
3. **Database Connections**: Total connections across all instances < database max_connections
4. **Network Latency**: Shared storage latency impacts throughput (prefer low-latency storage)
5. **File System Semantics**: Storage must support POSIX atomicity guarantees

## Traceability
- Related to: Load testing (LOAD_TESTING.md)
- Related to: Sizing guide (SIZING_RESULTS.md)
- Related to: Database schema (database.html)
- Related to: Architecture diagram (architecture.html)

## Status
✅ **Verified**: Design supports both horizontal and vertical scaling with proper safeguards

## Last Updated
2026-01-12
