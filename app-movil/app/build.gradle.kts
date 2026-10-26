import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
// La IP del backend sale de app-movil/local.properties (no se sube a git):
//   api.base.url=http://192.168.1.50:8082/
// Si no está, se usa 10.0.2.2, que es tu PC vista desde el emulador.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiBaseUrl: String = propiedadesLocales.getProperty("api.base.url", "http://10.0.2.2:8082/")

android {
    namespace = "com.proyecto.movil"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.proyecto.movil"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Retrofit (para hablar con el backend)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

// OkHttp (para ver las peticiones en el Logcat mientras programas)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Coroutines (para hacer las llamadas a la API sin trabar la app)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// ViewModel y LiveData (la V y el VM de MVVM)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    // viewModel() dentro de los @Composable
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

// Coil (para cargar las imágenes de los avatares, lo usarás en la tarea 5)
    implementation("io.coil-kt:coil-compose:2.6.0")
//Agregar la dependencia de navegación
    implementation("androidx.navigation:navigation-compose:2.8.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}