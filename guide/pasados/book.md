# JettraRest: Soporte REST y RestClient Nativo para JettraServer

**JettraRest** añade capacidades de creación de servicios y clientes REST de alto rendimiento en el ecosistema Jettra. Permite exponer recursos RESTful utilizando anotaciones estándar de seguridad e integrarse de manera fluida con **JettraJWT** para autenticación y autorización basada en tokens.

Admite dos modalidades de cliente:
1. **Cliente Declarativo (`@RestClient`)**: Permite declarar una interfaz Java con anotaciones REST, la cual implementa dinámicamente un cliente HTTP mediante Proxies dinámicos de Java.
2. **Cliente Fluido (`Client`)**: API para realizar peticiones HTTP de forma explícita y fluida.

---

## ⚙️ Configuración (`jettra-rest.properties`)

Defina las propiedades de JettraRest en el archivo `jettra-rest.properties` ubicado en `src/main/resources`.

```properties
# Habilitar o deshabilitar el motor REST de Jettra
jettra.rest.enabled=true

# Prefijo base para todos los endpoints REST expuestos
jettra.rest.base-path=/api

# Configuración del servidor y puertos para el cliente REST por defecto
jettra.rest.client.default-host=localhost
jettra.rest.client.default-port=8080
jettra.rest.client.timeout-millis=5000

# Integración de Seguridad con JWT (JettraJWT)
jettra.rest.security.enabled=true
jettra.rest.security.header-name=Authorization
jettra.rest.security.token-prefix=Bearer 
jettra.rest.security.jwt.secret=default_secret_key_jettra_rest_2026
jettra.rest.security.jwt.expiration-millis=3600000
```

---

## 🚀 Creación de Endpoints (REST Resources)

JettraRest escanea las clases anotadas con `@Path` para registrarlas como recursos REST. Admite los métodos HTTP estándar mediante anotaciones de verbo, inyección de parámetros y serialización/deserialización JSON automática.

### Anotaciones de Endpoint
*   `@Path(value)`: Define la ruta URI base para el recurso o método.
*   `@GET`, `@POST`, `@PUT`, `@DELETE`: Especifican el método HTTP de la petición.
*   `@Produces(mediaType)`: Define el formato de retorno (por defecto `application/json`).
*   `@Consumes(mediaType)`: Define el formato esperado de la petición.
*   `@PathParam(name)`: Extrae variables de la ruta URI (por ejemplo, `/productos/{id}`).
*   `@QueryParam(name)`: Extrae parámetros de consulta (por ejemplo, `?orden=asc`).

### Ejemplo: Recurso Producto (`ProductoResource.java`)

```java
package com.jettra.example.resources;

import com.jettra.rest.annotations.*;
import com.jettra.rest.core.Response;
import com.jettra.example.model.ProductoModel;
import com.jettra.example.repository.ProductoRepository;

import java.util.List;

@Path("/productos")
@Produces("application/json")
public class ProductoResource {

    private final ProductoRepository repository = new ProductoRepository();

    @GET
    public Response obtenerTodos(@QueryParam("limite") Integer limite) {
        List<ProductoModel> productos = repository.findAll();
        if (limite != null && limite < productos.size()) {
            productos = productos.subList(0, limite);
        }
        return Response.ok(productos).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerPorId(@PathParam("id") String id) {
        return repository.findById(id)
                .map(producto -> Response.ok(producto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Producto no encontrado\"}").build());
    }

    @POST
    @Consumes("application/json")
    public Response crear(ProductoModel nuevoProducto) {
        if (nuevoProducto == null || nuevoProducto.getNombre() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Datos de producto inválidos\"}").build();
        }
        repository.save(nuevoProducto);
        return Response.status(Response.Status.CREATED).entity(nuevoProducto).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") String id) {
        if (repository.exists(id)) {
            repository.delete(id);
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
```

---

## 🔒 Seguridad de Endpoints

JettraRest simplifica la securización de recursos mediante la validación automática de tokens JettraJWT presentes en las peticiones entrantes.

### Anotaciones de Seguridad
*   `@Secured`: Indica que el recurso o método requiere autenticación mediante token JWT válido.
*   `@RolesAllowed(roles)`: Restringe el acceso únicamente a los usuarios con los roles especificados (por ejemplo, `@RolesAllowed({"ADMIN", "SUPERVISOR"})`).
*   `@PermitAll`: Permite el acceso libre sin verificación de credenciales (incluso si la clase base está anotada con `@Secured`).

### Contexto de Seguridad (`SecurityContext`)
Es posible inyectar el contexto de seguridad para obtener detalles del usuario que realiza la petición directamente en el método.

### Ejemplo: Endpoint Securizado (`AdminResource.java`)

```java
package com.jettra.example.resources;

import com.jettra.rest.annotations.*;
import com.jettra.rest.core.Response;
import com.jettra.rest.security.SecurityContext;

@Path("/admin")
@Secured
@Produces("application/json")
public class AdminResource {

    @GET
    @Path("/dashboard")
    @RolesAllowed("ADMIN")
    public Response getDashboardData(@Context SecurityContext sc) {
        String username = sc.getUserPrincipal().getName();
        
        String jsonResponse = String.format(
            "{\"usuario\": \"%s\", \"estado\": \"Acceso Autorizado\", \"sistema\": \"JettraServer Rest\"}", 
            username
        );
        return Response.ok(jsonResponse).build();
    }

    @GET
    @Path("/public-info")
    @PermitAll
    public Response getPublicInfo() {
        return Response.ok("{\"mensaje\": \"Información pública del servidor sin autenticar\"}").build();
    }
}
```

---

## 📡 Cliente REST Declarativo (`@RestClient`)

Esta modalidad permite consumir servicios REST externos e internos definiendo únicamente una interfaz Java con anotaciones. El framework genera la implementación subyacente dinámicamente mediante proxies dinámicos y la clase `RestClientBuilder`.

### Anotaciones en Clientes
*   `@RestClient(baseUri)`: Define el URI base de destino del servicio REST externo.
*   `@HeaderParam(name)`: Inyecta un parámetro de método como un encabezado HTTP de la petición (útil para tokens `Authorization`).

### Ejemplo: Interfaz Cliente (`ProductoCliente.java`)

```java
package com.jettra.example.client;

import com.jettra.rest.client.RestClient;
import com.jettra.rest.annotations.*;
import com.jettra.example.model.ProductoModel;
import java.util.List;

@RestClient(baseUri = "http://localhost:8080/api")
public interface ProductoCliente {

    @GET
    @Path("/productos")
    List<ProductoModel> listarProductos();

    @GET
    @Path("/productos/{id}")
    ProductoModel obtenerPorId(@PathParam("id") String id);

    @POST
    @Path("/productos")
    ProductoModel crear(@HeaderParam("Authorization") String token, ProductoModel nuevo);

    @DELETE
    @Path("/productos/{id}")
    void eliminar(@HeaderParam("Authorization") String token, @PathParam("id") String id);
}
```

### Ejemplo: Instanciación y Uso del Proxy

```java
package com.jettra.example.client;

import com.jettra.rest.client.RestClientBuilder;
import com.jettra.example.model.ProductoModel;
import java.util.List;

public class ClientExample {

    public static void execute() {
        // Generar la implementación dinámica de la interfaz
        ProductoCliente cliente = RestClientBuilder.create(ProductoCliente.class);

        // Llamada GET sin autenticar
        List<ProductoModel> lista = cliente.listarProductos();
        System.out.println("Productos obtenidos: " + lista.size());

        // Llamada POST con encabezado de autorización JWT
        String token = "Bearer token_generado_jettrajwt_2026";
        ProductoModel nuevo = new ProductoModel("Laptop Gamer", 1500.0);
        ProductoModel creado = cliente.crear(token, nuevo);
        System.out.println("Producto creado con ID: " + creado.getId());
    }
}
```

---

## 🌊 Cliente REST Fluido (`Client`)

Para peticiones complejas donde no se desea utilizar interfaces, JettraRest provee la clase `Client` y su constructor `RestClientBuilder`.

### Ejemplo de Cliente Fluido

```java
package com.jettra.example.client;

import com.jettra.rest.client.Client;
import com.jettra.rest.client.RestClientBuilder;
import com.jettra.rest.client.Entity;
import com.jettra.rest.client.GenericType;
import com.jettra.rest.core.Response;
import com.jettra.example.model.ProductoModel;

import java.util.List;

public class ProductoClientFluido {

    private final Client client;
    private final String targetUri = "http://localhost:8080/api/productos";

    public ProductoClientFluido() {
        // Inicialización y configuración del Client utilizando el builder
        this.client = RestClientBuilder.newBuilder()
                .connectTimeout(5000)
                .readTimeout(5000)
                .build();
    }

    /**
     * Obtener listado de productos de forma síncrona
     */
    public List<ProductoModel> listarProductos() {
        return client.target(targetUri)
                .request("application/json")
                .get(new GenericType<List<ProductoModel>>() {});
    }

    /**
     * Crear un producto enviando una entidad JSON y un token de autenticación
     */
    public ProductoModel crearProducto(ProductoModel nuevo, String jwtToken) {
        Response response = client.target(targetUri)
                .request("application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .post(Entity.json(nuevo));

        if (response.getStatus() == 201) {
            return response.readEntity(ProductoModel.class);
        } else {
            throw new RuntimeException("Error al crear producto. Código HTTP: " + response.getStatus());
        }
    }
}
```
