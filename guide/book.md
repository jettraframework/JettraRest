# JettraRest - Comprehensive Guide & Architecture Manual

## 1. Overview & Architecture
`JettraRest` is an asynchronous, annotation-driven REST and HTTP micro-framework built on Java 25 Virtual Threads. It simplifies the definition, routing, serialization, and validation of RESTful web APIs across Jettra services.

---

## 2. Key Features
- **Declarative HTTP Annotations**: `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PathParam`, `@QueryParam`, `@Body`.
- **Integrated JSON Content Negotiation**: Direct binding with `JettraJSON` for request/response payloads.
- **Virtual Thread Dispatching**: Every incoming HTTP request executes on an ultra-lightweight virtual thread.
- **Filter and Interceptor Pipelines**: Built-in hooks for authentication, CORS, rate limiting, and structured logging.

---

## 3. Installation
```xml
<dependency>
    <groupId>io.jettra</groupId>
    <artifactId>JettraRest</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 4. Usage & Code Examples

### 4.1 Defining a REST Resource
```java
package com.example.api;

import io.jettra.rest.annotation.*;
import io.jettra.rest.response.RestResponse;

@Path("/api/v1/customers")
public class CustomerResource {

    public record CustomerDto(String id, String name, String email) {}

    @GET
    @Path("/{id}")
    public RestResponse getCustomer(@PathParam("id") String id) {
        CustomerDto customer = new CustomerDto(id, "Carlos Mendez", "carlos@example.com");
        return RestResponse.ok(customer);
    }

    @POST
    public RestResponse createCustomer(@Body CustomerDto body) {
        System.out.println("Created customer: " + body.name());
        return RestResponse.created(body);
    }
}
```
