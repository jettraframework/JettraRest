# Asegurar Endpoints en JettraRest

En `JettraRest`, la seguridad se gestiona usando anotaciones sobre los controladores y métodos. Se utiliza JWT (JSON Web Tokens) para validar la autenticación y las autorizaciones basadas en roles.

## Anotaciones Disponibles

- `@DeclareRoles({"ADMIN", "MANAGER"})`: Se usa a nivel de clase para declarar los roles válidos que la clase puede gestionar.
- `@RolesAllowed("ADMIN")`: Se usa a nivel de clase o de método. Define qué roles tienen acceso al endpoint especificado.
- `@Secured`: Marca un endpoint como asegurado (requiere autenticación con JWT), aunque no exija un rol específico.
- `@PermitAll`: Permite acceso sin restricciones, ignorando requerimientos de seguridad.

## Ejemplo de uso

```java
import com.jettra.rest.annotations.*;

@Path("/admin")
@DeclareRoles({"ADMIN", "MANAGER"}) // Declara los roles usados en la aplicación
public class AdminResource {

    @GET
    @Path("/dashboard")
    @RolesAllowed("ADMIN") // Solo usuarios con el rol 'ADMIN' pueden acceder
    @Produces("application/json")
    public String getDashboardData() {
        return "{\"message\": \"Welcome to the Admin Dashboard!\"}";
    }
}
```

El servidor interceptará la solicitud, extraerá el JWT del encabezado `Authorization: Bearer <token>`, obtendrá los roles embebidos en el payload del token y verificará si el usuario tiene permiso para consumir el método.
