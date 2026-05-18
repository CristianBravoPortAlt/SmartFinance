# SmartFinance 📱💸

**SmartFinance** es una aplicación móvil nativa para Android enfocada en la gestión financiera personal (PFM) y colaborativa. Permite llevar un control total de ingresos y gastos, gestionar presupuestos compartidos y establecer metas de ahorro a largo plazo.

Este proyecto constituye la memoria final para el ciclo de **Desarrollo de Aplicaciones Multiplataforma (DAM)** desarrollado por Cristian Bravo Asensio.

## 🚀 Características Principales

* **Registro Ágil:** Formularios optimizados para añadir ingresos y gastos de forma rápida, incluyendo importes, títulos y notas descriptivas.
* **Privacidad Total:** Almacenamiento 100% local. Cero recopilación de datos, cero publicidad y sin sincronización forzada con entidades financieras.
* **Categorización:** Sistema de etiquetas para clasificar movimientos, permitiendo un análisis detallado del flujo de caja.
* **Panel de Control (Dashboard):** Visualización instantánea del balance disponible y listado de últimos movimientos.
* **Interfaz Minimalista:** Diseño limpio, elegante y profesional basado en una paleta monocromática (Blanco/Negro) con soporte nativo para Modo Oscuro.

## 🛠️ Stack Tecnológico

El proyecto está desarrollado utilizando el estándar más moderno y recomendado por Google para el ecosistema Android:

* **Lenguaje:** [Kotlin 2.0+](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Arquitectura:** MVVM (Model-View-ViewModel) + Principios Clean
* **Inyección de Dependencias:** [Dagger Hilt](https://dagger.dev/hilt/)
* **Base de Datos (Local):** [Room](https://developer.android.com/training/data-storage/room) (SQLite)
* **Navegación:** Jetpack Navigation Compose
* **Concurrencia:** Kotlin Coroutines & Flows

## 🏗️ Arquitectura del Proyecto

El código fuente está estructurado en capas para garantizar la escalabilidad, el mantenimiento y la separación de responsabilidades:

```text
com.example.smartfinance
├── data/
│   ├── local/
│   └── repository/
├── di/
├── ui/
│   ├── components/
│   ├── home/
│   ├── screens/
│   └── theme/
├── utils/
└── workers/
