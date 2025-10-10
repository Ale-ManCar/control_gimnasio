# Plan de despliegue rentable para 3-5 clientes

Este documento describe una estrategia inicial y de bajo costo para migrar el sistema a la web y atender simultáneamente a un máximo de cinco clientes con aislamiento de datos.

## ¿Es viable migrar el sistema actual?

Sí. La aplicación de escritorio puede transformarse en un servicio web siempre que se aborden tres frentes principales:

1. **Reestructurar la lógica de negocio** para exponerla como API (REST o GraphQL) y desacoplarla de la interfaz de escritorio.
2. **Diseñar un frontend web** (HTML/CSS/JS o framework moderno) que consuma esa API y replique la funcionalidad del ejecutable.
3. **Aislar la información de cada cliente** mediante un modelo multi-tenant en la base de datos y controles de autenticación y autorización.

La infraestructura propuesta en este plan cubre el entorno mínimo para ejecutar esa nueva arquitectura web y permite crecer gradualmente sin que los clientes interfieran entre sí.

### ¿Puedo usar HTML, CSS y JavaScript en IntelliJ IDEA?

Sí. IntelliJ IDEA (Community y Ultimate) incorpora soporte completo para archivos HTML, CSS y JavaScript, por lo que puedes mantenerte en el mismo IDE mientras migras el sistema a la web. La edición Community ofrece resaltado de sintaxis, autocompletado básico y previsualización en el navegador. La edición Ultimate añade depurador JavaScript integrado, soporte específico para frameworks modernos (React, Angular, Vue) y Live Edit mejorado. En ambos casos, el proyecto puede convivir con tu código Java/Maven actual y compartir el mismo control de versiones.

### ¿Cómo configuro IntelliJ IDEA para desarrollar el frontend web?

1. **Activar los plugins web**
   - Revisa `File > Settings > Plugins` y confirma que estén habilitados *HTML Tools*, *CSS*, *JavaScript* y, si lo necesitas, *Node.js* o plugins para frameworks concretos.
   - Si vas a compilar assets con npm, instala también el plugin **Node.js** (Ultimate) o configura un intérprete Node externo para ejecutar scripts desde IntelliJ.

2. **Organizar el módulo web**
   - Crea un módulo o carpeta `web` dentro del proyecto con tus vistas, estilos y scripts.
   - Ajusta `File > Project Structure` para marcarla como *Resource* o *Web Resource Directory*, de modo que IntelliJ habilite inspecciones y preprocesadores.

3. **Configurar herramientas de construcción**
   - Añade un `package.json` para manejar dependencias del frontend y ejecútalo desde la terminal integrada (`npm install`, `npm run dev` o `npm run build`).
   - Si usas herramientas como Vite, Webpack o Maven Frontend Plugin, crea *Run Configurations* (`Add New Configuration > npm` o `Node.js`) para lanzar el servidor de desarrollo con un clic.

4. **Previsualizar y depurar**
   - Usa la función **Live Edit** (Ultimate) o un navegador externo con auto-reload para ver los cambios al instante.
   - Abre la pestaña *Run/Debug* para adjuntar el depurador JavaScript o inspeccionar la consola del navegador mediante Chrome DevTools.

5. **Integrar backend y Maven**
   - Mantén tu módulo Java/Maven en el mismo proyecto y crea configuraciones separadas: una para `mvn clean javafx:run` (si sigues probando la app de escritorio) y otra para el backend web (por ejemplo `mvn spring-boot:run` o el comando que decidas).
   - Configura variables de entorno y perfiles desde `Run > Edit Configurations` para aislar credenciales por cliente durante el desarrollo.

6. **Versionado y colaboración**
   - Continúa usando la ventana **Git** de IntelliJ para revisar cambios, crear ramas y sincronizar con el repositorio remoto.
   - Define inspecciones compartidas (`.idea/inspectionProfiles`) y reglas de formateo para que todo el equipo mantenga un estilo consistente.

Con estas configuraciones puedes trabajar exclusivamente en IntelliJ IDEA y construir tanto el backend como el nuevo frontend web del sistema sin necesidad de cambiar de herramienta. Si más adelante decides experimentar con otro editor (por ejemplo VS Code), podrás hacerlo sin modificar la arquitectura descrita en este plan.

### Primer backend HTTP dentro del repositorio

El proyecto ahora incluye un servidor web ligero construido con Spark Java y Jackson que expone la base de datos SQLite existente. Esto permite comenzar a migrar casos de uso a endpoints REST sin abandonar el código actual.

1. **Compila las dependencias**
   ```bash
   mvn -DskipTests package
   ```

2. **Arranca el servidor web** (puedes hacerlo desde IntelliJ IDEA creando una *Run Configuration* de tipo *Maven* que ejecute `exec:java`).
   ```bash
   mvn exec:java
   ```

3. **Verifica los endpoints iniciales**
   - `http://localhost:8080/api/status` devuelve `{ "status": "ok" }`.
   - `http://localhost:8080/api/clientes` responde con el listado de clientes registrado en SQLite.

4. **Conecta un frontend**
   - Usa la carpeta `src/main/resources/public/` para prototipar vistas HTML/CSS/JS que consuman la API.
   - Para clientes externos (React, Vue, etc.) habilita CORS en el nuevo servidor o despliega los archivos estáticos en un hosting aparte.

Este punto de partida abre la puerta a migrar progresivamente cada flujo de la app de escritorio hacia el entorno web.

## Objetivos

1. **Disponibilidad compartida**: un único despliegue que atienda a todos los clientes sin interferencia entre sus datos.
2. **Costos controlados**: aprovechar niveles gratuitos o de bajo costo mientras se valida el modelo de uso.
3. **Escalabilidad moderada**: permitir crecimiento sencillo a medida que se incorporen más usuarios o aumente la carga.

## Componentes propuestos

### 1. Plataforma de alojamiento (PaaS económica)

- **Opción recomendada**: Render (plan Starter) o Railway (plan Pro básico) en torno a USD 7-12/mes.
- **Alternativa**: Heroku Eco Dyno (≈USD 5/mes) si las restricciones de hibernación son aceptables.
- **Ventajas**: despliegue sencillo vía Git, certificados SSL automáticos, escalado vertical básico.

### 2. Base de datos administrada

- **Opción recomendada**: PostgreSQL gestionado en el mismo proveedor (Render o Railway) con plan compartido de 256-512 MB de RAM.
- **Costo aproximado**: USD 7-15/mes.
- **Aislamiento de datos**: implementar un esquema (`schema`) por cliente o incluir un campo `tenant_id` en todas las tablas con enforcement a nivel de consultas. Esto evita instancias separadas y mantiene costos bajos.

### 3. Dominio y SSL

- **Dominio**: registrar uno único para el servicio (ej. `misistema.com`) ≈USD 12/año.
- **Subdominios por cliente** (opcional): `cliente1.misistema.com`. Configurable sin costo adicional.
- **SSL**: los PaaS mencionados integran certificados gratuitos con Let’s Encrypt.

### 4. Copias de seguridad y monitoreo

- **Backups automáticos**: activar snapshots diarios incluidos en los planes administrados. En Render están incluidos; en Railway se habilitan manualmente.
- **Logs y alertas**: usar herramientas integradas del proveedor o un servicio gratuito como Better Stack (plan gratuito) para las primeras alertas.

## Estimación de costos mensuales

| Concepto                       | Costo estimado |
|--------------------------------|----------------|
| Servidor (PaaS)                | USD 7-12       |
| Base de datos administrada     | USD 7-15       |
| Dominio                        | USD 1 (prorrateado) |
| Backups/monitoreo (básico)     | USD 0-3        |
| **Total aproximado**           | **USD 15-30/mes** |

## Pasos sugeridos

1. **Preparar el backend** con soporte multi-tenant mediante un identificador de cliente en cada operación.
2. **Configurar entorno** en el PaaS elegido y conectar repositorio Git para despliegues automáticos.
3. **Provisionar base de datos** y aplicar migraciones iniciales.
4. **Registrar dominio** y apuntar DNS al servicio; verificar SSL automático.
5. **Crear cuentas de cliente** con esquemas o `tenant_id` dedicados.
6. **Configurar copias de seguridad** y alertas básicas.
7. **Realizar pruebas de carga ligera** (3-5 usuarios simultáneos) y ajustar parámetros de recursos si fuese necesario.

## Escalamiento futuro

- **Aumento de recursos**: cambiar el plan del servidor o la base de datos a niveles superiores dentro del mismo proveedor.
- **Separación de clientes de alto consumo**: migrar clientes específicos a instancias dedicadas si sus cargas crecen.
- **Migración a infraestructura propia (VM/Kubernetes)**: considerar sólo cuando la base de clientes y los requerimientos superen los límites del PaaS.

Esta configuración prioriza simplicidad y ahorro, manteniendo el aislamiento de datos necesario para atender hasta cinco clientes con fiabilidad.
