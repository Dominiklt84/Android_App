package com.example.ds1_android_app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarText
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class WebLauncherScreen(carContext: CarContext) : Screen(carContext) {

    private val url: String = carContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getString("url", "") ?: ""

    // Stany ekranu: Ładowanie, Błąd, Sukces
    private var isLoading = true
    private var errorMessage: String? = null
    private var parsedData: List<String> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        if (url.isEmpty()) {
            isLoading = false
            errorMessage = "Brak adresu URL. Ustaw go na telefonie."
            invalidate()
            return
        }

        // Uruchamiamy pobieranie w tle, żeby nie zamrozić ekranu auta
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Pobieramy stronę i wyciągamy z niej czysty tekst
                val doc = Jsoup.connect(url).get()
                val fullText = doc.body().text()

                // Dzielimy tekst na mniejsze fragmenty (po kropkach), żeby ładnie wyglądały na liście
                val chunks = fullText.split(". ")
                    .filter { it.isNotBlank() }
                    .map { it.trim() + "." }

                withContext(Dispatchers.Main) {
                    parsedData = chunks
                    isLoading = false
                    errorMessage = null
                    invalidate() // Odświeża ekran auta
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Błąd pobierania: ${e.message}"
                    invalidate()
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        // 1. EKRAN ŁADOWANIA LUB BŁĘDU
        if (isLoading || errorMessage != null) {
            return ListTemplate.Builder()
                .setTitle("DS1 Tuning")
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(
                    ItemList.Builder()
                        .addItem(Row.Builder().setTitle(errorMessage ?: "Pobieranie danych ze strony...").build())
                        .build()
                )
                .build()
        }

        // 2. EKRAN Z DANYMI (Przewijalna lista)
        val listBuilder = ItemList.Builder()

        // Ograniczamy do 100 elementów, żeby nie przeciążyć systemu auta
        parsedData.take(100).forEach { text ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(CarText.create(text))
                    .build()
            )
        }

        // Dodajemy przycisk na górze do wymuszenia odświeżenia
        val refreshAction = Action.Builder()
            .setTitle("Odśwież")
            .setOnClickListener {
                isLoading = true
                errorMessage = null
                invalidate()
                loadData()
                null
            }
            .build()

        return ListTemplate.Builder()
            .setTitle("DS1 Tuning Data")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .setActionStrip(
                androidx.car.app.model.ActionStrip.Builder().addAction(refreshAction).build()
            )
            .build()
    }
}