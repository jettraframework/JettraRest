# JettraRest

## Descripción General
`JettraRest` actúa como la capa de herramientas y utilidades para comunicación RESTful, tanto para el consumo (como cliente) como para la asistencia en la creación de endpoints de las aplicaciones JettraStack.

## Detalles Específicos
- **Arquitectura general**: Basado en estándares y clientes REST modernos de Java, encapsulando la complejidad de las peticiones HTTP.
- **Dependencias clave**: Librerías HTTP base de Java o Jakarta RESTful Web Services.
- **Roles dentro del sistema**: Actúa como proxy o cliente REST (`RestClientProxy`, `WebTarget`) para facilitar que otros microservicios se comuniquen entre sí, o para realizar tests de integración de forma sencilla.

## Características Detalladas
- **Cliente REST (`RestClientProxy`)**: Herramienta potente para emitir peticiones GET, POST, PUT, DELETE de forma estandarizada.
- **`WebTarget`**: Clase utilitaria para construir URIs, inyectar parámetros, cabeceras y gestionar los "targets" de red.
- **Manejo de Errores HTTP**: Procesamiento de respuestas, estandarizando excepciones cuando la respuesta no es un código 2xx.

## Guía de Entrenamiento (AI / Nuevas Características)
- Al agregar métodos para soportar nuevos tipos de peticiones o cabeceras (ej. subida de archivos multipart), debe modificarse `WebTarget` y `RestClientProxy`.
- El código debe permanecer ligero e independiente, evitando acoplarse con módulos específicos de negocio.
- Usar este módulo cada vez que se necesite invocar un endpoint externo en lugar de instanciar un cliente HTTP nativo desde cero.
