# RestClient en JettraRest

JettraRest incluye una implementación nativa y ligera para la creación de clientes HTTP a partir de interfaces anotadas, emulando la funcionalidad estándar de MicroProfile Rest Client.

Mediante el uso de `Proxy` dinámicos (Reflection) y el cliente HTTP integrado de Java (`java.net.http.HttpClient`), JettraRest te permite invocar endpoints RESTful sin escribir código repetitivo de manejo de conexiones y serialización.

## Definición de un Cliente

Para crear un cliente REST, solo necesitas definir una interfaz y anotar sus métodos con las anotaciones de JettraRest (`@GET`, `@POST`, `@Path`, `@PathParam`, etc.).

La interfaz debe estar anotada con `@RestClient`, la cual permite opcionalmente definir el `baseUri` del endpoint.

```java
import com.jettra.rest.annotations.*;
import com.jettra.rest.client.RestClient;
import java.util.List;

@RestClient(baseUri = "http://localhost:8080/api/library/authors")
public interface IAuthorClient {
    
    @GET
    List<AuthorModel> findAll();

    @POST
    void save(AuthorModel model);

    @DELETE
    @Path("/{id}")
    void delete(@PathParam("id") String id);
}
```

## Creación y Uso del Cliente

A partir de las últimas actualizaciones, JettraRest incluye un **Annotation Processor** que detecta las interfaces marcadas con `@RestClient` y genera su implementación automáticamente en tiempo de compilación.

Para instanciar el cliente, simplemente crea una nueva instancia de la clase generada (ej. `AuthorRestClient` para la interfaz `IAuthorRestClient`) y utiliza sus métodos:

```java
import com.jettra.example.restclient.library.AuthorRestClient;
import com.jettra.example.restclient.library.interfaces.IAuthorRestClient;

public class MyService {
    
    // Instancia de la clase generada automáticamente
    private static final IAuthorRestClient client = new AuthorRestClient();
    
    public void doSomething() {
        // Ejecuta una petición HTTP GET y retorna la lista mapeada de JSON a Objetos
        List<AuthorModel> authors = client.findAll();
        
        // Ejecuta una petición HTTP POST serializando el modelo a JSON
        client.save(new AuthorModel(...));
    }
}
```

## Integración con CrudView (JettraWUI)

Ya **no** es necesario escribir clases manuales con métodos estáticos para delegar las llamadas a un proxy. En su lugar, puedes crear un servicio estándar (ej. `AuthorService`) que instancie el cliente generado y contenga los métodos requeridos por `@CrudView(controller = AuthorService.class)`.

```java
public class AuthorService {
    
    private static final IAuthorRestClient client = new AuthorRestClient();

    public static List<AuthorModel> findAll() {
        return client.findAll();
    }

    public static void save(AuthorModel model) {
        client.save(model);
    }

    public static void delete(String id) {
        client.delete(id);
    }
}
```

## Características Soportadas
- **Anotaciones HTTP**: `@GET`, `@POST`, `@PUT`, `@DELETE`.
- **Rutas**: `@Path` tanto a nivel de interfaz como a nivel de método.
- **Parámetros**: `@PathParam`, `@QueryParam`, `@HeaderParam`.
- **Serialización/Deserialización Automática**: El cuerpo de la petición (cuando no se anota) se serializa a JSON automáticamente y el cuerpo de la respuesta se mapea al tipo de retorno genérico del método.
- **Gestión de Errores**: Si el endpoint devuelve un código HTTP `>= 400`, se lanzará una `RuntimeException`.
