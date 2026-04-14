# agents.md

## 📌 Project Overview

**Nombre:** Sistema de Gestión de Llamadas y Encuestas
**Plataforma:** Android (.apk)
**Stack:** Kotlin + Jetpack Compose + CSRM + SQL (Room + PostgreSQL)

### 🎯 Objetivo

Facilitar la gestión de llamadas, registro de interacciones y aplicación de encuestas (vía QuestionPro), asegurando trazabilidad, control y productividad tanto para agentes como administradores.

---

## 🧱 Architecture (CSRM)

### Client

* Jetpack Compose (UI declarativa)
* Navigation Compose
* ViewModel (state management)

### Service (Use Cases)

* Lógica de negocio
* Validaciones (máx. 5 intentos)
* Reglas de flujo

### Repository

* Abstracción de datos
* Coordinación entre:

  * API REST
  * Base de datos local (Room)

### Model

* Entidades del dominio:

  * Contacto
  * Llamada
  * Encuesta
  * Usuario

---

## 🗄️ Data Layer (SQL)

### 📱 Local DB (Room - SQLite)

* Persistencia offline
* Cache
* Escritura inmediata

### ☁️ Remote DB (PostgreSQL)

* Fuente de verdad
* Reportes globales
* Administración

---

## 📊 Database Schema

### contacto

* id (PK)
* nombre
* telefono
* estado (pendiente, en_gestion, contactado, desistido)
* intentos
* fecha_creacion

### llamada

* id (PK)
* contacto_id (FK)
* usuario_id
* fecha_inicio
* fecha_fin
* duracion (segundos)
* resultado (contesta, no_contesta, ocupado, invalido)
* tipificacion
* observacion
* pendiente_sync (boolean)

### encuesta

* id (PK)
* contacto_id (FK)
* url
* estado (completa, incompleta, no_realizada)
* fecha

### usuario

* id (PK)
* nombre
* email
* password_hash
* rol (agente, admin)

---

## 🔁 Sync Strategy (Offline-first)

1. Registro en Room (local)
2. Marcado como `pendiente_sync = true`
3. WorkManager ejecuta sincronización
4. Backend confirma → `pendiente_sync = false`

---

## ⚙️ Functional Requirements

### 📞 Call Management

* Registrar llamadas con:

  * inicio
  * fin
  * duración (automática)
* Limitar a 5 intentos por contacto
* Registrar resultado obligatorio

### 🔄 Contact Flow

* Estados:

  * pendiente
  * en_gestion
  * contactado
  * desistido
* Transiciones automáticas

### 📝 Survey Integration (QuestionPro)

* Abrir encuesta vía URL/WebView
* Registrar estado:

  * completa
  * incompleta
  * no realizada

### 🏷️ Typification

* Clasificación estructurada
* Observaciones libres

### 📊 Tracking & Metrics

* Historial por contacto
* Métricas:

  * tasa de contacto
  * duración promedio
  * productividad

### 📲 Agent Productivity

* Priorización automática de contactos
* Sugerencia de siguiente llamada

---

## 🚫 Non-Functional Requirements

### ⚡ Performance

* UI < 200ms respuesta
* DB local < 100ms consultas

### 📱 Usability

* ≤ 3 acciones para registrar llamada
* Uso con una mano
* Feedback inmediato

### 🔐 Security

* Autenticación con token (JWT)
* Passwords encriptadas
* Protección de datos

### 📡 Connectivity

* Funcional offline
* Sincronización automática

### 🧱 Maintainability

* Separación estricta por capas
* ViewModel sin lógica compleja

---

## 🧠 Business Rules

* Máximo 5 intentos por contacto
* Encuesta solo si contesta
* Duración de llamada obligatoria
* Registro obligatorio de cada llamada
* Contacto pasa a “desistido” automáticamente

---

## 🧪 User Stories

### 👤 Agent

**US1 – Call Handling**
Como agente, quiero registrar llamadas rápidamente para maximizar productividad.

**US2 – Automatic Duration**
Como agente, quiero que la duración se calcule automáticamente.

**US3 – Survey Flow**
Como agente, quiero abrir encuestas sin salir del flujo.

**US4 – Attempt Limit**
Como agente, quiero saber cuándo dejar de llamar.

**US5 – Typification**
Como agente, quiero clasificar cada interacción.

**US6 – Offline Work**
Como agente, quiero trabajar sin internet.

**US7 – Auto Sync**
Como agente, quiero que los datos se sincronicen automáticamente.

---

### 👨‍💼 Admin

**US8 – Metrics Dashboard**
Como administrador, quiero ver métricas de desempeño.

**US9 – Contact Control**
Como administrador, quiero monitorear el estado de contactos.

---

### 🤖 System

**US10 – Auto Desist Logic**
Como sistema, quiero marcar contactos como desistidos automáticamente.

**US11 – Data Integrity**
Como sistema, quiero asegurar trazabilidad completa.

---

## 🧩 Key Technical Decisions

* Jetpack Compose → UI moderna y reactiva
* Room → persistencia local eficiente
* PostgreSQL → escalabilidad backend
* WorkManager → sync robusto
* Repository Pattern → desacoplamiento
* CSRM → arquitectura clara y mantenible

---

## 🚀 MVP Scope

Incluye:

* Registro de llamadas
* Control de intentos
* Integración con QuestionPro
* Persistencia local (Room)
* Sincronización básica
* Visualización simple de contactos

Excluye:

* Discado automático
* Analítica avanzada
* Notificaciones inteligentes

---

## 📌 Notes

El sistema actúa como:

> Middleware operativo entre agentes y encuestas externas + gestor de llamadas tipo call center liviano

Diseñado para:

* Escalar a múltiples campañas
* Integrarse con APIs externas
* Evolucionar hacia automatización completa

---
