# Dashboard Admin — App Llamadas
## Guía completa: React + Tailwind CSS + Vercel

---

## 📋 Tabla de contenidos

1. [Stack y requisitos](#1-stack-y-requisitos)
2. [Extensiones al backend Spring Boot](#2-extensiones-al-backend-spring-boot)
3. [Crear el proyecto React (Next.js)](#3-crear-el-proyecto-reactnextjs)
4. [Estructura de carpetas](#4-estructura-de-carpetas)
5. [Variables de entorno](#5-variables-de-entorno)
6. [Capa de API (fetch)](#6-capa-de-api-fetch)
7. [Componentes del dashboard](#7-componentes-del-dashboard)
8. [Páginas](#8-páginas)
9. [Configurar CORS en Spring Boot](#9-configurar-cors-en-spring-boot)
10. [Deploy en Vercel](#10-deploy-en-vercel)
11. [Desplegar el backend (para producción)](#11-desplegar-el-backend-para-producción)

---

## 1. Stack y requisitos

| Herramienta | Versión mínima | Uso |
|---|---|---|
| Node.js | 18+ | Runtime |
| npm / pnpm | cualquiera | Package manager |
| Next.js | 14 (App Router) | Framework React |
| Tailwind CSS | 3.x | Estilos |
| shadcn/ui | latest | Componentes UI |
| Recharts | 2.x | Gráficas |
| Vercel CLI | latest | Deploy |

Instalar Vercel CLI globalmente:
```bash
npm install -g vercel
```

---

## 2. Extensiones al backend Spring Boot

Antes de desarrollar el frontend, agrega estos endpoints al `SyncController.kt` (o crea un `DashboardController.kt`):

### `DashboardController.kt`
```kotlin
package com.cem.appllamadasbackend.controller

import com.cem.appllamadasbackend.domain.model.Contacto
import com.cem.appllamadasbackend.domain.model.Llamada
import com.cem.appllamadasbackend.domain.repository.ContactoRepository
import com.cem.appllamadasbackend.domain.repository.LlamadaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class MetricasResponse(
    val totalContactos: Long,
    val totalLlamadas: Long,
    val totalContestan: Long,
    val totalNoContestan: Long,
    val duracionPromedio: Double,
    val tasaContacto: Double
)

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = ["*"]) // Cambia "*" por tu dominio Vercel en producción
class DashboardController(
    private val contactoRepository: ContactoRepository,
    private val llamadaRepository: LlamadaRepository
) {

    @GetMapping("/metricas")
    fun getMetricas(): ResponseEntity<MetricasResponse> {
        val totalContactos = contactoRepository.count()
        val todasLlamadas  = llamadaRepository.findAll()
        val totalLlamadas  = todasLlamadas.size.toLong()
        val contestan      = todasLlamadas.count { it.resultado == "CONTESTA" }.toLong()
        val noContestan    = todasLlamadas.count { it.resultado == "NO_CONTESTA" }.toLong()
        val durPromedio    = todasLlamadas.mapNotNull { it.duracion }.let {
            if (it.isEmpty()) 0.0 else it.average()
        }
        val tasa = if (totalLlamadas > 0) (contestan.toDouble() / totalLlamadas) * 100 else 0.0

        return ResponseEntity.ok(MetricasResponse(
            totalContactos  = totalContactos,
            totalLlamadas   = totalLlamadas,
            totalContestan  = contestan,
            totalNoContestan = noContestan,
            duracionPromedio = durPromedio,
            tasaContacto    = tasa
        ))
    }

    @GetMapping("/llamadas")
    fun getLlamadas(): ResponseEntity<List<Llamada>> =
        ResponseEntity.ok(llamadaRepository.findAll())

    @GetMapping("/contactos")
    fun getContactos(): ResponseEntity<List<Contacto>> =
        ResponseEntity.ok(contactoRepository.findAll())

    @PostMapping("/contactos")
    fun crearContacto(@RequestBody contacto: Contacto): ResponseEntity<Contacto> =
        ResponseEntity.ok(contactoRepository.save(contacto))
}
```

Reinicia el backend después de agregar este archivo:
```bash
# En App_Llamadas_Backend/
.\gradlew.bat bootRun
```

---

## 3. Crear el proyecto React (Next.js)

```bash
# Desde la raíz del proyecto
npx create-next-app@latest dashboard --typescript --tailwind --eslint --app --no-src-dir --import-alias "@/*"
cd dashboard
```

Instalar dependencias adicionales:
```bash
npm install recharts lucide-react date-fns
npx shadcn-ui@latest init
```

Durante el `shadcn init`, selecciona:
- Style: **Default**
- Base color: **Slate**
- CSS variables: **Yes**

Agregar componentes de shadcn/ui necesarios:
```bash
npx shadcn-ui@latest add card table badge button input select
```

---

## 4. Estructura de carpetas

```
dashboard/
├── app/
│   ├── layout.tsx          ← Layout global con sidebar
│   ├── page.tsx            ← Página principal (métricas)
│   ├── contactos/
│   │   └── page.tsx        ← Tabla de contactos
│   └── llamadas/
│       └── page.tsx        ← Historial de llamadas
├── components/
│   ├── Sidebar.tsx
│   ├── MetricCard.tsx
│   ├── LlamadasChart.tsx
│   ├── ContactosTable.tsx
│   └── LlamadasTable.tsx
├── lib/
│   └── api.ts              ← Capa de fetch al backend
├── types/
│   └── index.ts            ← Tipos TypeScript
└── .env.local              ← Variables de entorno
```

---

## 5. Variables de entorno

Crear `dashboard/.env.local`:
```env
# Desarrollo local (backend corriendo en Docker)
NEXT_PUBLIC_API_URL=http://localhost:8080

# Producción (completar con tu URL de backend deployado)
# NEXT_PUBLIC_API_URL=https://tu-backend.railway.app
```

---

## 6. Capa de API (fetch)

### `lib/api.ts`
```typescript
const BASE = process.env.NEXT_PUBLIC_API_URL;

export interface Metricas {
  totalContactos: number;
  totalLlamadas: number;
  totalContestan: number;
  totalNoContestan: number;
  duracionPromedio: number;
  tasaContacto: number;
}

export interface Contacto {
  id: string;
  nombre: string;
  telefono: string;
  estado: "PENDIENTE" | "EN_GESTION" | "CONTACTADO" | "DESISTIDO";
  intentos: number;
  fechaCreacion: number;
}

export interface Llamada {
  id: string;
  contactoId: string;
  usuarioId: string;
  fechaInicio: number;
  fechaFin: number | null;
  duracion: number | null;
  resultado: "CONTESTA" | "NO_CONTESTA" | "OCUPADO" | "INVALIDO" | null;
  tipificacion: string | null;
  observacion: string | null;
}

export const api = {
  metricas:   (): Promise<Metricas>    => fetch(`${BASE}/api/dashboard/metricas`).then(r => r.json()),
  contactos:  (): Promise<Contacto[]>  => fetch(`${BASE}/api/dashboard/contactos`).then(r => r.json()),
  llamadas:   (): Promise<Llamada[]>   => fetch(`${BASE}/api/dashboard/llamadas`).then(r => r.json()),
  crearContacto: (c: Omit<Contacto, "id">): Promise<Contacto> =>
    fetch(`${BASE}/api/dashboard/contactos`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...c, id: crypto.randomUUID() })
    }).then(r => r.json()),
};
```

---

## 7. Componentes del dashboard

### `components/MetricCard.tsx`
```tsx
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { LucideIcon } from "lucide-react";

interface Props {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  color?: string;
}

export function MetricCard({ title, value, subtitle, icon: Icon, color = "text-blue-600" }: Props) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className={`h-5 w-5 ${color}`} />
      </CardHeader>
      <CardContent>
        <div className="text-3xl font-bold">{value}</div>
        {subtitle && <p className="text-xs text-muted-foreground mt-1">{subtitle}</p>}
      </CardContent>
    </Card>
  );
}
```

### `components/LlamadasChart.tsx`
```tsx
"use client";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from "recharts";
import { Llamada } from "@/lib/api";

const COLORS: Record<string, string> = {
  CONTESTA: "#22c55e",
  NO_CONTESTA: "#ef4444",
  OCUPADO: "#f59e0b",
  INVALIDO: "#94a3b8",
};

export function LlamadasChart({ llamadas }: { llamadas: Llamada[] }) {
  const data = Object.entries(
    llamadas.reduce((acc, l) => {
      const key = l.resultado ?? "SIN_RESULTADO";
      acc[key] = (acc[key] ?? 0) + 1;
      return acc;
    }, {} as Record<string, number>)
  ).map(([name, value]) => ({ name, value }));

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 5 }}>
        <XAxis dataKey="name" tick={{ fontSize: 12 }} />
        <YAxis allowDecimals={false} />
        <Tooltip />
        <Bar dataKey="value" radius={[4, 4, 0, 0]}>
          {data.map((entry) => (
            <Cell key={entry.name} fill={COLORS[entry.name] ?? "#6366f1"} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
```

### `components/ContactosTable.tsx`
```tsx
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Contacto } from "@/lib/api";

const estadoColor: Record<string, string> = {
  PENDIENTE: "bg-yellow-100 text-yellow-800",
  EN_GESTION: "bg-blue-100 text-blue-800",
  CONTACTADO: "bg-green-100 text-green-800",
  DESISTIDO: "bg-red-100 text-red-800",
};

export function ContactosTable({ contactos }: { contactos: Contacto[] }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Nombre</TableHead>
          <TableHead>Teléfono</TableHead>
          <TableHead>Estado</TableHead>
          <TableHead className="text-center">Intentos</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {contactos.map((c) => (
          <TableRow key={c.id}>
            <TableCell className="font-medium">{c.nombre}</TableCell>
            <TableCell>{c.telefono}</TableCell>
            <TableCell>
              <span className={`px-2 py-1 rounded-full text-xs font-semibold ${estadoColor[c.estado]}`}>
                {c.estado}
              </span>
            </TableCell>
            <TableCell className="text-center">{c.intentos}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
```

### `components/Sidebar.tsx`
```tsx
"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Phone, Users } from "lucide-react";

const links = [
  { href: "/",           label: "Dashboard",  icon: LayoutDashboard },
  { href: "/contactos",  label: "Contactos",  icon: Users },
  { href: "/llamadas",   label: "Llamadas",   icon: Phone },
];

export function Sidebar() {
  const path = usePathname();
  return (
    <aside className="w-56 min-h-screen bg-slate-900 text-white flex flex-col p-4 gap-1">
      <div className="text-lg font-bold mb-6 px-2">📞 App Llamadas</div>
      {links.map(({ href, label, icon: Icon }) => (
        <Link key={href} href={href}
          className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors
            ${path === href ? "bg-slate-700 text-white" : "text-slate-400 hover:bg-slate-800 hover:text-white"}`}
        >
          <Icon className="h-4 w-4" />
          {label}
        </Link>
      ))}
    </aside>
  );
}
```

---

## 8. Páginas

### `app/layout.tsx`
```tsx
import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Sidebar } from "@/components/Sidebar";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "App Llamadas — Admin",
  description: "Dashboard administrativo de gestión de llamadas",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body className={inter.className}>
        <div className="flex min-h-screen bg-slate-50">
          <Sidebar />
          <main className="flex-1 p-8 overflow-auto">{children}</main>
        </div>
      </body>
    </html>
  );
}
```

### `app/page.tsx` (Métricas principales)
```tsx
import { api } from "@/lib/api";
import { MetricCard } from "@/components/MetricCard";
import { LlamadasChart } from "@/components/LlamadasChart";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Phone, Users, CheckCircle, XCircle, Clock, TrendingUp } from "lucide-react";

export const revalidate = 30; // refresca cada 30s en Vercel

export default async function DashboardPage() {
  const [metricas, llamadas] = await Promise.all([api.metricas(), api.llamadas()]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 xl:grid-cols-3 gap-4">
        <MetricCard title="Total Contactos"  value={metricas.totalContactos}  icon={Users}       color="text-blue-600" />
        <MetricCard title="Total Llamadas"   value={metricas.totalLlamadas}   icon={Phone}       color="text-indigo-600" />
        <MetricCard title="Contestaron"      value={metricas.totalContestan}  icon={CheckCircle} color="text-green-600" />
        <MetricCard title="No Contestaron"   value={metricas.totalNoContestan} icon={XCircle}    color="text-red-500" />
        <MetricCard
          title="Tasa de Contacto"
          value={`${metricas.tasaContacto.toFixed(1)}%`}
          icon={TrendingUp}
          color="text-emerald-600"
        />
        <MetricCard
          title="Duración Promedio"
          value={`${Math.round(metricas.duracionPromedio)}s`}
          icon={Clock}
          color="text-orange-500"
        />
      </div>

      {/* Gráfica de resultados */}
      <Card>
        <CardHeader>
          <CardTitle>Resultados de llamadas</CardTitle>
        </CardHeader>
        <CardContent>
          <LlamadasChart llamadas={llamadas} />
        </CardContent>
      </Card>
    </div>
  );
}
```

### `app/contactos/page.tsx`
```tsx
import { api } from "@/lib/api";
import { ContactosTable } from "@/components/ContactosTable";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export const revalidate = 30;

export default async function ContactosPage() {
  const contactos = await api.contactos();
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Contactos ({contactos.length})</h1>
      <Card>
        <CardHeader><CardTitle>Lista de contactos</CardTitle></CardHeader>
        <CardContent><ContactosTable contactos={contactos} /></CardContent>
      </Card>
    </div>
  );
}
```

### `app/llamadas/page.tsx`
```tsx
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { format } from "date-fns";
import { es } from "date-fns/locale";

export const revalidate = 30;

const resultadoColor: Record<string, string> = {
  CONTESTA:    "text-green-600 font-semibold",
  NO_CONTESTA: "text-red-500",
  OCUPADO:     "text-yellow-600",
  INVALIDO:    "text-slate-400",
};

export default async function LlamadasPage() {
  const llamadas = await api.llamadas();
  const sorted   = [...llamadas].sort((a, b) => b.fechaInicio - a.fechaInicio);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Historial de llamadas ({sorted.length})</h1>
      <Card>
        <CardHeader><CardTitle>Todas las llamadas</CardTitle></CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Agente</TableHead>
                <TableHead>Fecha</TableHead>
                <TableHead>Resultado</TableHead>
                <TableHead>Duración</TableHead>
                <TableHead>Tipificación</TableHead>
                <TableHead>Observación</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sorted.map((l) => (
                <TableRow key={l.id}>
                  <TableCell>{l.usuarioId}</TableCell>
                  <TableCell className="text-sm text-slate-500">
                    {format(new Date(l.fechaInicio), "dd/MM/yyyy HH:mm", { locale: es })}
                  </TableCell>
                  <TableCell className={resultadoColor[l.resultado ?? ""] ?? ""}>
                    {l.resultado ?? "—"}
                  </TableCell>
                  <TableCell>{l.duracion != null ? `${l.duracion}s` : "—"}</TableCell>
                  <TableCell>{l.tipificacion ?? "—"}</TableCell>
                  <TableCell className="max-w-[180px] truncate">{l.observacion ?? "—"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
```

---

## 9. Configurar CORS en Spring Boot

En producción, el frontend en Vercel necesita hacer fetch al backend. Agrega la anotación `@CrossOrigin` o una configuración global:

### `WebConfig.kt` (agregar en el backend)
```kotlin
package com.cem.appllamadasbackend

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:3000",            // desarrollo local
                "https://tu-dashboard.vercel.app"  // ← reemplazar con tu URL de Vercel
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
    }
}
```

---

## 10. Deploy en Vercel

### Paso 1: Probar localmente
```bash
cd dashboard
npm run dev
# Abre http://localhost:3000
```

### Paso 2: Crear repositorio Git
```bash
cd dashboard
git init
git add .
git commit -m "feat: admin dashboard inicial"
```

### Paso 3: Deploy con Vercel CLI
```bash
vercel login          # Autenticar con tu cuenta Vercel
vercel                # Deploy (primera vez: configura el proyecto)
vercel --prod         # Deploy a producción
```

O bien, conecta el repositorio desde **[vercel.com/new](https://vercel.com/new)** seleccionando el repositorio de GitHub del dashboard.

### Paso 4: Variables de entorno en Vercel
En el panel de Vercel → Settings → Environment Variables:

| Variable | Valor |
|---|---|
| `NEXT_PUBLIC_API_URL` | `https://tu-backend-deployado.railway.app` |

---

## 11. Desplegar el backend (para producción)

El dashboard en Vercel necesita que el backend esté accesible en internet. Opciones gratuitas:

### Opción A: Railway (recomendada)
```bash
# Instalar Railway CLI
npm install -g @railway/cli
railway login
cd App_Llamadas_Backend
railway init
railway up
# Railway auto-detecta Spring Boot con Gradle y lo deploya
# PostgreSQL: agrega un plugin de PostgreSQL desde el panel de Railway
```

### Opción B: Render
1. Crea cuenta en [render.com](https://render.com)
2. New → Web Service → conecta tu repositorio
3. Build command: `./gradlew build`
4. Start command: `java -jar build/libs/*.jar`
5. Agrega un servicio PostgreSQL desde el panel de Render

### Variables de entorno del backend en producción
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

---

## ✅ Checklist de implementación

- [ ] Agregar `DashboardController.kt` al backend Spring Boot
- [ ] Reiniciar backend y verificar `GET http://localhost:8080/api/dashboard/metricas`
- [ ] Crear proyecto Next.js con `create-next-app`
- [ ] Instalar Recharts, shadcn/ui, date-fns
- [ ] Crear `.env.local` con `NEXT_PUBLIC_API_URL`
- [ ] Copiar archivos de `lib/api.ts`, `types/`, `components/`, `app/`
- [ ] Probar localmente con `npm run dev`
- [ ] Agregar `WebConfig.kt` al backend para CORS
- [ ] Deployar backend en Railway o Render
- [ ] Deployar frontend en Vercel con `vercel --prod`
- [ ] Actualizar `NEXT_PUBLIC_API_URL` en Vercel con la URL del backend deployado
- [ ] Actualizar CORS en `WebConfig.kt` con la URL de Vercel y re-deployar backend
