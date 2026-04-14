# 4+1 Architecture Diagrams (Mermaid) - FINAL VERSION (Antigravity Ready)

---

## 🧩 1. Vista Lógica (Clases / Dominio)

```mermaid
classDiagram
    class Contacto {
        id
        nombre
        telefono
        estado
        intentos
    }

    class Llamada {
        id
        contactoId
        usuarioId
        fechaInicio
        fechaFin
        duracion
        resultado
        tipificacion
        pendienteSync
    }

    class Encuesta {
        id
        contactoId
        llamadaId
        url
        estado
    }

    class Usuario {
        id
        nombre
        rol
    }

    Contacto "1" --> "many" Llamada
    Usuario "1" --> "many" Llamada
    Llamada "1" --> "0..1" Encuesta
```

---

## ⚙️ 2. Vista de Desarrollo (CSRM + Data Sources)

```mermaid
graph LR

UI[Jetpack Compose UI]
VM[ViewModel]

UC[UseCases / Service Layer]

Repo[Repository]

LocalDS[Local DataSource (Room)]
RemoteDS[Remote DataSource (API)]

Worker[WorkManager Sync]

DBLocal[(SQLite)]
APIRest[REST API]
DBRemote[(PostgreSQL)]

UI --> VM
VM --> UC
UC --> Repo

Repo --> LocalDS
Repo --> RemoteDS

LocalDS --> DBLocal
RemoteDS --> APIRest

Worker --> Repo

APIRest --> DBRemote
```

---

## 🔄 3. Vista de Procesos (Flujo completo con offline + sync)

```mermaid
sequenceDiagram

actor Agente
participant App
participant Room
participant Worker
participant API
participant QuestionPro

Agente->>App: Selecciona contacto

App->>Room: Validar intentos

alt intentos >= 5
    App-->>Agente: Bloquear contacto
else
    Agente->>App: Inicia llamada
    App->>Room: Guardar inicio

    Agente->>App: Termina llamada
    App->>App: Calcular duración

    Agente->>App: Selecciona resultado

    alt Contesta
        App->>QuestionPro: Abrir encuesta
        Agente->>App: Registrar estado encuesta
    end

    App->>Room: Guardar llamada (pendiente_sync = true)

    Worker->>Room: Obtener pendientes

    Worker->>API: Enviar datos

    alt éxito
        API->>Room: Confirmar sync
    else error
        Worker->>Worker: Reintentar
    end
end
```

---

## 🌐 4. Vista Física (Despliegue)

```mermaid
graph TD

Device[Android Device]
App[App Kotlin + Compose]
Worker[WorkManager]

Backend[Backend Server]
Auth[Auth Service (JWT)]
DB[(PostgreSQL)]

External[QuestionPro]

Device --> App
App --> Worker

App -->|HTTPS| Backend
Backend --> Auth
Backend --> DB

App -->|WebView / Browser| External
```

---

## 👤 5. Vista de Escenarios (Caso completo)

```mermaid
flowchart TD

A[Inicio] --> B[Seleccionar contacto]

B --> C{Intentos >= 5}

C -->|Sí| Z[Bloquear contacto]
C -->|No| D[Realizar llamada]

D --> E{Contesta?}

E -->|Sí| F[Abrir encuesta]
F --> G[Registrar encuesta]

E -->|No| H[Registrar intento]

G --> I[Guardar llamada]
H --> I

I --> J[Marcar pendiente_sync]

J --> K[Sincronización]

K --> L{Sync OK?}

L -->|Sí| M[Actualizar estado]
L -->|No| N[Reintentar sync]

M --> O[Evaluar intentos]

O --> P{>=5 sin contacto}

P -->|Sí| Q[Desistido]
P -->|No| R[Seguir gestión]

Q --> S[Fin]
R --> S
Z --> S
```

---

## 📌 Notas

* Arquitectura basada en **CSRM (Client–Service–Repository–Model)**
* Estrategia **offline-first con sincronización automática**
* Integración externa con **QuestionPro**
* Regla crítica: máximo **5 intentos por contacto**
* Persistencia dual: **Room (SQLite) + PostgreSQL**

---
