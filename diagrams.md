# Deployment Diagram — App Llamadas

```mermaid
graph TB
    subgraph DEVICE["📱 Android Device / Emulator"]
        subgraph APK["APK: com.cem.appllamadas"]
            UI["🖥️ Jetpack Compose UI\n(ContactoScreen, EncuestaScreen)"]
            VM["⚙️ ContactoViewModel\n(Hilt @HiltViewModel)"]
            CSM["📡 CallStateManager\n(TelephonyManager listener)"]
            UC["🧠 Use Cases\n(ObtenerSiguienteContacto\nRegistrarLlamada)"]
            REPO["🔄 Repository Layer\n(ContactoRepositoryImpl\nLlamadaRepositoryImpl)"]
            RETRO["🌐 Retrofit Client\nbaseUrl: 10.0.2.2:8080"]
            WM["⏱️ WorkManager\nSyncWorker (15 min)"]
        end

        subgraph ROOM["💾 Room DB (SQLite - Local)"]
            T_CONTACTO["Table: contacto"]
            T_LLAMADA["Table: llamada"]
        end

        TELECOM["📞 Android Telecom\n(System Service)"]
    end

    subgraph DOCKER["🐳 Docker Desktop (localhost)"]
        subgraph BACKEND["☕ Spring Boot API\nPort: 8080"]
            CTRL["🔀 SyncController\nPOST /api/sync\nGET /api/contactos/pendientes"]
            SVC["⚙️ JPA / Hibernate"]
        end

        subgraph PG["🗄️ PostgreSQL Container\napp_llamadas_db : Port 5432"]
            PG_C["Table: contacto"]
            PG_L["Table: llamada"]
        end
    end

    subgraph EXTERNAL["🌍 External Services"]
        QP["📋 QuestionPro\n(Survey URL via WebView)"]
    end

    %% UI Flow
    UI <-->|"State / Events"| VM
    VM <-->|"observes"| CSM
    CSM <-->|"CALL_STATE_*"| TELECOM
    VM <-->|"invokes"| UC
    UC <-->|"reads / writes"| REPO

    %% Local persistence
    REPO <-->|"DAO queries"| ROOM

    %% Remote sync
    REPO <-->|"Retrofit HTTP"| RETRO
    WM  <-->|"syncLlamadasPendientes()"| REPO
    RETRO <-->|"POST /api/sync\nHTTP REST"| CTRL
    CTRL <-->|"JPA saveAll()"| SVC
    SVC <-->|"JDBC"| PG

    %% External
    UI <-->|"WebView open URL"| QP

    %% Styling
    classDef androidNode fill:#E3F2FD,stroke:#1565C0,color:#0D47A1
    classDef dockerNode fill:#E8F5E9,stroke:#2E7D32,color:#1B5E20
    classDef dbNode fill:#FFF8E1,stroke:#F57F17,color:#E65100
    classDef externalNode fill:#F3E5F5,stroke:#6A1B9A,color:#4A148C

    class UI,VM,CSM,UC,REPO,RETRO,WM androidNode
    class CTRL,SVC dockerNode
    class T_CONTACTO,T_LLAMADA,PG_C,PG_L dbNode
    class QP externalNode
```

---

## Deployment Nodes

| Nodo | Tecnología | Ubicación |
|------|-----------|-----------|
| Android App (APK) | Kotlin + Jetpack Compose + Hilt | Dispositivo / Emulador |
| Room DB | SQLite (local, offline-first) | Dispositivo / Emulador |
| Spring Boot API | Kotlin + Spring Boot 3.2, puerto `8080` | `localhost` (Docker) |
| PostgreSQL | Postgres 15-alpine, puerto `5432` | Contenedor Docker |
| QuestionPro | SaaS externo | Internet |

## Flujo de sincronización

```
[Room (pendiente_sync=true)] → WorkManager cada 15 min
    → Retrofit POST /api/sync → Spring Boot → PostgreSQL
    ← Confirmación → pendiente_sync = false
```

## Conectividad offline-first

```
Online:  Android ──HTTP──▶ Spring Boot ──JDBC──▶ PostgreSQL
Offline: Android ──DAO──▶  Room (SQLite local) ──WorkManager──▶ cola pendiente
```
