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

Para instanciar el cliente, utiliza la clase `RestClientBuilder`. Ésta se encargará de generar un proxy dinámico que intercepta las llamadas a los métodos y las transforma en solicitudes HTTP reales de forma transparente, manejando automáticamente la serialización a JSON mediante Gson.

```java
import com.jettra.rest.client.RestClientBuilder;

public class MyService {
    
    // Generación del Proxy
    private final IAuthorClient client = RestClientBuilder.create(IAuthorClient.class);
    
    public void doSomething() {
        // Ejecuta una petición HTTP GET y retorna la lista mapeada de JSON a Objetos
        List<AuthorModel> authors = client.findAll();
        
        // Ejecuta una petición HTTP POST serializando el modelo a JSON
        client.save(new AuthorModel(...));
    }
}
```

## Integración con CrudView (JettraWUI)

Debido a que `@CrudView` en JettraWUI requiere que la clase provista en el parámetro `controller` tenga métodos estáticos (`findAll()`, `save()`, `delete()`), es un patrón común envolver el proxy generado en una clase con métodos estáticos:

```java
public class AuthorClient {
    @RestClient(baseUri = "http://localhost:8080/api/library/authors")
    public interface IAuthorClient { ... }

    private static final IAuthorClient proxy = RestClientBuilder.create(IAuthorClient.class);

    public static List<AuthorModel> findAll() { return proxy.findAll(); }
    public static void save(AuthorModel model) { proxy.save(model); }
    public static void delete(String id) { proxy.delete(id); }
}
```

De este modo, `AuthorClient.class` es totalmente compatible con la anotación `@CrudView(controller = AuthorClient.class)`.

## Características Soportadas
- **Anotaciones HTTP**: `@GET`, `@POST`, `@PUT`, `@DELETE`.
- **Rutas**: `@Path` tanto a nivel de interfaz como a nivel de método.
- **Parámetros**: `@PathParam`, `@QueryParam`, `@HeaderParam`.
- **Serialización/Deserialización Automática**: El cuerpo de la petición (cuando no se anota) se serializa a JSON automáticamente y el cuerpo de la respuesta se mapea al tipo de retorno genérico del método.
- **Gestión de Errores**: Si el endpoint devuelve un código HTTP `>= 400`, se lanzará una `RuntimeException`.
