# 🧩 Alphakids  
Sistema móvil educativo para aprendizaje de letras y palabras — Proyecto Qubit (OM03)

---

## 📘 Descripción del Proyecto
**Alphakids** es una aplicación móvil educativa desarrollada en **Kotlin + Jetpack Compose**, diseñada para que niños de **3 a 5 años** aprendan letras y palabras mediante actividades didácticas.  
El docente asigna palabras y actividades, y el niño interactúa con juegos visuales y auditivos que refuerzan el aprendizaje temprano.

---

## 🎯 Objetivos del Proyecto
- Fomentar el aprendizaje temprano de la lectura.  
- Facilitar la asignación y seguimiento de palabras por parte de los docentes.  
- Integrar actividades pedagógicas con tecnología móvil.  
- Ofrecer una experiencia amigable, visual y accesible para los niños.

---

## 📱 Funcionalidades Principales

### 👨‍🏫 Módulo Docente
- Registro e inicio de sesión.  
- Gestión de estudiantes.  
- Asignación de palabras según dificultad.  
- Visualización del progreso de cada niño.  
- **Notificaciones locales** cuando se asigna una palabra.  
- Filtros de búsqueda y ordenamiento.

### 👦 Módulo Estudiante
- Vista de palabras asignadas.  
- Juegos didácticos:  
  - Selección de letras.  
  - Reconocimiento de sonidos.  
  - Completar palabras.  
- Retroalimentación visual y sonora.  
- Avance guardado en Firebase.

---

## 🛠️ Tecnologías Utilizadas

### **App Móvil**
- Kotlin  
- Jetpack Compose  
- ViewModel + StateFlow  
- Coroutines  
- Material 3  
- Arquitectura MVVM  

### **Servicios**
- Firebase Authentication  
- Firebase Firestore  
- Firebase Storage (opcional)  
- **Notificaciones locales Android**

---

## 📂 Estructura del Proyecto

alphakids/
├── data/
│ ├── firebase/
│ ├── mappers/
│ ├── notification/
│ └── repository/
├── domain/
│ ├── models/
│ └── repository/
├── ui/
│ ├── screens/
│ ├── components/
│ └── theme/
└── MainActivity.kt


---

## 🔔 Sistema de Notificaciones Locales
El proyecto utiliza un helper personalizado para mostrar notificaciones locales en Android cuando el docente asigna una nueva palabra al estudiante.

### **Archivo:** `LocalNotificationHelper.kt`
- Crea el canal de notificaciones.  
- Muestra notificaciones locales dentro de la app.  
- Se activa automáticamente cuando el docente asigna una palabra.

No requiere backend, ni servicios externos, ni FCM.

---

## 🧪 Estado Actual del Proyecto
- MVP funcional.  
- Módulo docente completo.  
- Módulo estudiante operativo.  
- Juegos implementados.  
- Notificaciones locales integradas.  
- Se continuará optimizando la experiencia de usuario.

---

## 👥 Equipo — Qubit (OM03)
- **Diego Raúl Llanos García**  
- **Guiller Breyneer Rojas Juño**  
- **Junior Benjamín Sullca Huamán**
- **Gerald Brand Zinanyuca Calcina**

**Mentor:** Mauricio Surco  

---

## 📄 Licencia
Proyecto académico para la carrera de Diseño y Desarrollo de Software — Tecsup.

---

## 📫 Contacto
Para mejoras, ideas y colaboración, comunicarse con el equipo Qubit.
