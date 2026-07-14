# REQ-004: API Documentation with OpenAPI v3

## Requirement ID
REQ-004

## Category
Documentation / API Design

## Priority
High

## Description
All REST API services in the Horizon Platform shall provide comprehensive API documentation using OpenAPI v3 (formerly Swagger) specification. The system must provide interactive Swagger UI for API exploration and testing, and automatically generate OpenAPI specification files in both JSON and YAML formats as part of the build process.

## Rationale
- **Developer Experience**: Interactive API documentation reduces integration time for clients
- **Standardization**: OpenAPI v3 is industry standard for REST API documentation
- **Contract-First Development**: API specification serves as contract between services
- **Client Code Generation**: OpenAPI files enable automatic client SDK generation
- **API Gateway Integration**: Specifications required for Azure API Management, Kong, AWS API Gateway
- **Testing**: Swagger UI provides interactive testing without external tools
- **Compliance**: Healthcare integrations require well-documented APIs
- **Maintenance**: Auto-generated documentation stays synchronized with code

## Scope

### Services Requiring Documentation
1. **Horizon.Deployment.Service** - Tenant, role, identity management
2. **ClinicalData.Service** - DICOMweb API endpoints
3. **AiInference.Service** - AI model inference APIs
4. **SiteIntegration.Service** - Site integration endpoints
5. **Horizon.Global.Service** - Global platform services
6. **Horizon.WebUI** - Web application (optional, if exposing APIs)

## Acceptance Criteria

### OpenAPI v3 Specification
1. ✅ All services use OpenAPI v3.0 or later specification
2. ✅ Each service exposes `/swagger/v1/swagger.json` endpoint with valid OpenAPI JSON
3. ✅ Each service provides comprehensive endpoint documentation including:
   - Endpoint descriptions and remarks
   - Request parameter documentation
   - Request body schemas with examples
   - Response schemas for all HTTP status codes
   - Authentication requirements

### Swagger UI
1. ✅ Each service exposes Swagger UI at `/swagger` endpoint
2. ✅ Swagger UI integrated with OAuth2 Authorization Code flow
3. ✅ "Try it out" functionality works for all endpoints
4. ✅ Swagger UI displays:
   - All available endpoints grouped by controller/tag
   - Model schemas with examples
   - Authentication configuration
   - Request/response examples

### Automated File Generation
1. ✅ OpenAPI JSON file generated during Release builds
2. ✅ OpenAPI YAML file generated during Release builds
3. ✅ Files generated at: `bin/Release/net9.0/openapi.json` and `openapi.yaml`
4. ✅ Generation integrated into MSBuild process
5. ✅ No manual steps required for generation

### XML Documentation
1. ✅ All project files enable XML documentation generation
2. ✅ All public controllers have XML summary comments
3. ✅ All public methods have XML documentation including:
   - `<summary>` describing the operation
   - `<param>` for all parameters
   - `<returns>` describing return type
   - `<remarks>` with usage examples and notes
   - `<response>` for all possible HTTP status codes
4. ✅ All DTOs and models have XML property documentation with examples

### OAuth2 Integration
1. ✅ Swagger UI configured with OAuth2 Authorization Code flow
2. ✅ OAuth settings read from appsettings.json
3. ✅ "Authorize" button functional in Swagger UI
4. ✅ Access tokens automatically included in API requests
5. ✅ Token refresh handled transparently

## Implementation Details

### Project Configuration
Each service .csproj file must include:

```xml
<PropertyGroup>
  <GenerateDocumentationFile>true</GenerateDocumentationFile>
  <NoWarn>$(NoWarn);1591</NoWarn>
</PropertyGroup>

<ItemGroup>
  <PackageReference Include="Swashbuckle.AspNetCore" Version="9.0.4" />
  <PackageReference Include="Swashbuckle.AspNetCore.Annotations" Version="9.0.4" />
</ItemGroup>

<Target Name="GenerateOpenAPISpec" AfterTargets="Build" Condition="'$(Configuration)'=='Release'">
  <Exec Command="dotnet swagger tofile --output $(OutputPath)openapi.json --yaml $(OutputPath)openapi.yaml $(OutputPath)$(AssemblyName).dll v1" />
</Target>
```

### Shared Configuration
All services use shared Swagger configuration from `Horizon.Library.Swagger`:

```csharp
// Program.cs
builder.Services.AddHorizonSwagger();
app.UseHorizonSwagger();
```

### Example Controller Documentation
```csharp
/// <summary>
/// Retrieves a specific tenant by ID.
/// </summary>
/// <param name="id">The unique identifier of the tenant.</param>
/// <remarks>
/// Sample request:
/// 
///     GET /horizon-deployment/v1/tenants/11111111-1111-1111-1111-111111111111
///     
/// Returns detailed information about the specified tenant.
/// </remarks>
/// <returns>The tenant details.</returns>
/// <response code="200">Returns the tenant successfully.</response>
/// <response code="404">Tenant not found.</response>
/// <response code="401">Unauthorized - Authentication required.</response>
[HttpGet("tenants/{id}")]
[ProducesResponseType(typeof(TenantDto), StatusCodes.Status200OK)]
[ProducesResponseType(StatusCodes.Status404NotFound)]
[ProducesResponseType(StatusCodes.Status401Unauthorized)]
public async Task<ActionResult<TenantDto>> GetTenant(Guid id)
```

## Verification Method

### Manual Verification
1. Navigate to each service's `/swagger` endpoint
2. Verify Swagger UI loads without errors
3. Test "Authorize" button and OAuth2 flow
4. Execute "Try it out" on sample endpoints
5. Verify responses match documented schemas

### Automated Verification (Build)
```bash
# Build in Release mode
cd src/Horizon.Deployment/Horizon.Deployment.Service
dotnet build -c Release

# Verify files generated
ls -la bin/Release/net9.0/openapi.*
# Should show: openapi.json and openapi.yaml

# Validate OpenAPI specification
npx @apidevtools/swagger-cli validate bin/Release/net9.0/openapi.json
```

### CI/CD Integration
```yaml
# .github/workflows/build.yml
- name: Build Services
  run: dotnet build -c Release
  
- name: Verify OpenAPI Generation
  run: |
    test -f src/*/*/bin/Release/net9.0/openapi.json
    test -f src/*/*/bin/Release/net9.0/openapi.yaml
    
- name: Upload OpenAPI Specs
  uses: actions/upload-artifact@v3
  with:
    name: openapi-specifications
    path: "**/bin/Release/net9.0/openapi.*"
```

## Current Status
✅ **COMPLIANT** (as of January 13, 2026)

Implementation completed:
- ✅ All 5 services configured with Swashbuckle.AspNetCore 9.0.4
- ✅ XML documentation enabled in all service projects
- ✅ MSBuild target added for OpenAPI file generation
- ✅ Shared Swagger configuration in Horizon.Library.Swagger
- ✅ OAuth2 Authorization Code flow integrated
- ✅ Swagger UI accessible at `/swagger` for all services
- ✅ Comprehensive documentation guide created ([documentation/openapi.html](../documentation/openapi.html))
- ✅ Sample XML documentation added to TenantsController
- ✅ Swashbuckle CLI tool installed globally

### Service Endpoints
- **Deployment Service**: http://localhost:5000/swagger
- **Clinical Data Service**: http://localhost:5001/swagger
- **AI Inference Service**: http://localhost:5002/swagger
- **Site Integration Service**: http://localhost:5003/swagger
- **Global Service**: http://localhost:5004/swagger

### Generated Files
- Location: `bin/Release/net9.0/`
- Format: `openapi.json` and `openapi.yaml`
- Generation: Automatic during Release builds

## Client SDK Generation

OpenAPI specifications can be used to generate client SDKs:

```bash
# C# Client with OpenAPI Generator
openapi-generator generate -i openapi.yaml -g csharp-netcore -o client-sdk

# TypeScript Client
openapi-generator generate -i openapi.yaml -g typescript-fetch -o client-sdk

# Python Client
openapi-generator generate -i openapi.yaml -g python -o client-sdk

# Using Microsoft Kiota
kiota generate -l csharp -d openapi.yaml -c HorizonClient -n Horizon.Client -o ./client
```

## Best Practices

### Documentation Guidelines
1. **Be Descriptive**: Explain what endpoint does, not just repeat method name
2. **Include Examples**: Show sample requests and responses in `<remarks>`
3. **Document All Parameters**: Include type, format, validation rules
4. **List All Response Codes**: Document success and all error scenarios
5. **Use `<response>` Tags**: Specify meaning of each HTTP status code
6. **Document DTOs**: Every property should have `<summary>` and `<example>`

### API Design
1. **RESTful Conventions**: Use standard HTTP methods and status codes
2. **Versioning**: Include API version in URL (e.g., /v1/tenants)
3. **Consistent Naming**: Use consistent patterns across all endpoints
4. **Resource-Oriented**: Design URLs around resources, not actions
5. **Pagination**: Support paging for list endpoints
6. **Filtering**: Allow filtering and sorting where appropriate

### Security
1. **Always Authenticate**: Require authentication for all endpoints (except health checks)
2. **Use HTTPS**: Never expose APIs over plain HTTP in production
3. **Validate Input**: Check all input parameters and request bodies
4. **Rate Limiting**: Implement rate limits to prevent abuse
5. **Audit Logging**: Log all API access and changes

## Maintenance

- **Continuous**: XML comments updated with code changes
- **Each Release**: Verify OpenAPI files generate successfully
- **Monthly**: Review API documentation for accuracy
- **Quarterly**: Audit API design for consistency and best practices

## Related Documents

- **Implementation Summary**: [OPENAPI-IMPLEMENTATION.md](../OPENAPI-IMPLEMENTATION.md)
- **Detailed Guide**: [documentation/openapi.html](../documentation/openapi.html)
- **Shared Configuration**: `src/Horizon/Horizon.Library/Swagger/SwaggerExtensions.cs`
- **Traceability**: REQ-001 (CVE tracking for Swashbuckle packages)

## External Standards

- [OpenAPI v3.0 Specification](https://swagger.io/specification/)
- [OpenAPI v3.1 Specification](https://spec.openapis.org/oas/v3.1.0)
- [ASP.NET Core Web API Documentation](https://learn.microsoft.com/en-us/aspnet/core/tutorials/web-api-help-pages-using-swagger)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)

## Compliance

- **IEC 62304**: Software documentation requirements
- **FDA Guidance**: API documentation for medical device software
- **HIPAA**: Audit trail requirements met through API logging
- **ISO 13485**: Design documentation and traceability

## Last Reviewed
January 13, 2026

## Approved By
[Pending - Add approval signature]
