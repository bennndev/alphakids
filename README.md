# 🧩 Alphakids  
Sistema móvil educativo para aprendizaje de letras y palabras — Proyecto Qubit (OM03)

---

## 📘 Descripción del Proyecto
**Alphakids** es una aplicación móvil educativa desarrollada en **Kotlin + Jetpack Compose**, diseñada para que niños de **3 a 5 años** aprendan letras y palabras mediante actividades didácticas.  
Los docentes asignan palabras personalizadas y los estudiantes interactúan con juegos visuales y auditivos para reforzar su aprendizaje temprano.

---

## 🖼️ Vista Previa de la App

### 🔐 Pantalla de Inicio de Sesión
<img src="screenshots/login.png" width="350">

### 👨‍🏫 Panel Docente
<img src="screenshots/teacher_dashboard.png" width="350">

### 👦 Lista de Estudiantes
<img src="screenshots/student_list.png" width="350">

### 🎮 Juego: Completar Palabra
<img src="screenshots/game_word_complete.png" width="350">

---

## 🎯 Objetivos del Proyecto
- Fomentar el aprendizaje temprano de la lectura.  
- Facilitar que docentes asignen palabras y revisen avances.  
- Integrar actividades pedagógicas atractivas para los niños.  
- Ofrecer una experiencia visual, didáctica y accesible.  

---

## 📱 Funcionalidades Principales

### 👨‍🏫 Módulo Docente
- Crear y administrar estudiantes.
- Asignar palabras según nivel.
- Visualizar progreso individual.
- Filtrar estudiantes por desempeño.
- **Notificaciones locales** cuando se asigna una palabra.
- Dashboard dinámico.

### 👦 Módulo Estudiante
- Ver palabras asignadas.
- Jugar actividades de aprendizaje:
  - Reconocer letras.
  - Completar palabras.
  - Asociar sonido–imagen.
- Experiencia amigable y adaptada a niños pequeños.
- Progreso guardado automáticamente en Firebase.

---

## 🔔 Sistema de Notificaciones Locales

### 📄 Archivo principal: `LocalNotificationHelper.kt`

El sistema utiliza **notificaciones locales Android**, sin backend externo ni FCM.

Se usan:
- Canal de notificación Android  
- PendingIntent para abrir la app  
- Notificación autodescartable  
- Activación automática al asignar una palabra  

Ejemplo visual (referencial):

<img src="screenshots/notification_example.png" width="350">

---

## 🛠️ Tecnologías Utilizadas

### **Frontend (App Móvil)**
- Kotlin  
- Jetpack Compose  
- Material 3  
- ViewModel + StateFlow  
- MVVM Clean Architecture  

### **Servicios**
- Firebase Authentication  
- Firebase Firestore  
- Firebase Storage  
- Notificaciones locales (sin backend externo)

---

## 📂 Estructura del Proyecto
