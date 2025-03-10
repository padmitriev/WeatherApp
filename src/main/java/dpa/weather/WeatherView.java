package dpa.weather;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.UIScope;
import dpa.weather.db.entity.WeatherRequest;
import dpa.weather.db.service.WeatherService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weathersdk.CityWeatherSDK;
import com.google.gson.JsonObject;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Предоставляет пользовательский интерфейс для отображения информации о погоде.
 * Позволяет пользователям вводить координаты или название города для получения данных о погоде,
 * а также отображает недавние запросы на получение погоды.
 *
 * @author padmitriev
 */
@Route("")
@UIScope
@Service
public class WeatherView extends HorizontalLayout {
    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Замените на ваш API ключ

    private final WeatherService weatherService;
    private VerticalLayout recentRequestsLayout;
    private VerticalLayout weatherInfoLayout;
    private VerticalLayout coordinatesLayout;

    /**
     * Конструктор класса WeatherView.
     * Инициализирует компоненты пользовательского интерфейса и добавляет обработчики событий.
     *
     * @param weatherService сервис для работы с базой данных.
     */
    public WeatherView(WeatherService weatherService) {
        this.weatherService = weatherService;

        VerticalLayout leftPanel = new VerticalLayout();
        TextField latitudeField = new TextField("Широта");
        TextField longitudeField = new TextField("Долгота");
        Button showWeatherButton = new Button("Показать погоду!");
        weatherInfoLayout = new VerticalLayout();

        showWeatherButton.addClickListener(event -> {
            String lat = latitudeField.getValue();
            String lon = longitudeField.getValue();
            String weatherData = getWeatherData(lat, lon);

            System.out.println(weatherData);

            updateWeatherInfo(weatherInfoLayout, weatherData);
            saveWeatherRequest(lat, lon, weatherData);
        });

        TextField cityField = new TextField("Город");
        Button getCoordinatesButton = new Button("Узнать координаты");
        coordinatesLayout = new VerticalLayout();

        getCoordinatesButton.addClickListener(event -> {
            String city = cityField.getValue();
            if (city == null || city.isEmpty()) {
                coordinatesLayout.removeAll();
                coordinatesLayout.add(new Paragraph("Ошибка: Введите название города."));
                return;
            }

            try {
                JsonObject geoData = getCityCoordinates(city);
                if (geoData != null) {
                    updateCoordinates(coordinatesLayout, geoData);
                } else {
                    coordinatesLayout.removeAll();
                    coordinatesLayout.add(new Paragraph("Ошибка: Город не найден."));
                }
            } catch (RuntimeException e) {
                coordinatesLayout.removeAll();
                coordinatesLayout.add(new Paragraph("Ошибка: Город не найден."));
            }
        });

        leftPanel.add(latitudeField, longitudeField, showWeatherButton, weatherInfoLayout, cityField, getCoordinatesButton, coordinatesLayout);
        leftPanel.setPadding(true);
        leftPanel.getStyle().set("border-right", "1px solid #ccc");

        recentRequestsLayout = getRecentRequestsLayout();
        recentRequestsLayout.setPadding(true);

        add(leftPanel, recentRequestsLayout);
        setAlignItems(Alignment.STRETCH);
        setSpacing(false);

        leftPanel.setWidth(null);
        recentRequestsLayout.setWidth(null);
        leftPanel.getStyle().set("flex-grow", "1");
        recentRequestsLayout.getStyle().set("flex-grow", "1");
    }

    /**
     * Создает и возвращает панель с недавними запросами на получение погоды.
     *
     * @return панель с недавними запросами.
     */
    private VerticalLayout getRecentRequestsLayout() {
        VerticalLayout recentRequestsLayout = new VerticalLayout();
        updateRecentRequestsLayout(recentRequestsLayout);
        return recentRequestsLayout;
    }

    /**
     * Обновляет панель с недавними запросами, добавляя в нее информацию о последних запросах.
     *
     * @param layout панель, которую нужно обновить.
     */
    private void updateRecentRequestsLayout(VerticalLayout layout) {
        layout.removeAll();
        layout.add(new H2("История"));

        List<WeatherRequest> recentRequests = weatherService.getRecentRequests();
        for (WeatherRequest request : recentRequests) {
            String requestTimeString = request.getRequestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String weatherDescription = request.getWeatherDescription();
            layout.add(new Paragraph(
                    requestTimeString + "  |  " + "Широта: " + request.getLatitude() + ", Долгота: " + request.getLongitude() + "  |  " + "Ответ: " + weatherDescription));
        }
    }

    /**
     * Сохраняет запрос о погоде в базе данных.
     *
     * @param lat широта запроса.
     * @param lon долгота запроса.
     * @param weatherData данные о погоде в формате JSON.
     */
    private void saveWeatherRequest(String lat, String lon, String weatherData) {
        try {
            JSONObject json = new JSONObject(weatherData);

            if (!json.has("weather") || json.getJSONArray("weather").length() == 0) {
                throw new JSONException("Нет данных о погоде для сохранения.");
            }

            String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");

            WeatherRequest request = new WeatherRequest();
            request.setLatitude(lat);
            request.setLongitude(lon);
            request.setWeatherData(weatherData);
            request.setWeatherDescription(weatherDescription);
            request.setRequestTime(LocalDateTime.now());
            weatherService.saveWeatherRequest(request);

            updateRecentRequestsLayout(recentRequestsLayout);
        } catch (JSONException e) {
            weatherInfoLayout.add(new Paragraph("Ошибка при сохранении запроса: " + e.getMessage()));
        } catch (Exception e) {
            weatherInfoLayout.add(new Paragraph("Ошибка: " + e.getMessage()));
        }
    }

    /**
     * Получает данные о погоде по заданным координатам.
     *
     * @param lat широта.
     * @param lon долгота.
     * @return данные о погоде в формате JSON.
     */
    private String getWeatherData(String lat, String lon) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&&lang=ru&units=metric", lat, lon, API_KEY);
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return "Ошибка при получении данных: " + response.message();
            }
        } catch (IOException e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    /**
     * Обновляет информацию о погоде в заданной панели.
     *
     * @param weatherInfoLayout панель для отображения информации о погоде.
     * @param weatherData данные о погоде в формате JSON.
     */
    private void updateWeatherInfo(VerticalLayout weatherInfoLayout, String weatherData) {
        weatherInfoLayout.removeAll();

        try {
            JSONObject json = new JSONObject(weatherData);
            String cityName = json.getString("name");
            double temperature = json.getJSONObject("main").getDouble("temp");
            double feels = json.getJSONObject("main").getDouble("feels_like");
            String description = json.getJSONArray("weather").getJSONObject(0).getString("description");

            weatherInfoLayout.add(new H2("Погода в: " + cityName));
            weatherInfoLayout.add(new Paragraph(description));
            weatherInfoLayout.add(new Paragraph("температура: " + temperature + "°C" + " , ощущается как " + feels + "°C"));

            Image weatherImage = getWeatherImage(temperature);
            weatherInfoLayout.add(weatherImage);

        } catch (JSONException e) {
            weatherInfoLayout.add(new Paragraph("Ошибка при обработке данных. Проверьте правильность координат."));
        }
    }

    /**
     * Возвращает изображение, соответствующее температуре.
     *
     * @param temperature температура для определения изображения.
     * @return изображение погоды.
     */
    private Image getWeatherImage(double temperature) {
        String imagePath;
        if (temperature > 0) {
            imagePath = "images/teplo.png";
        } else {
            imagePath = "images/holodno.png";
        }

        ClassPathResource resource = new ClassPathResource(imagePath);
        if (!resource.exists()) {
            System.out.println("Не найден файл: " + imagePath);
            return new Image(); // Возвращаем пустое изображение, если файл не найден
        }

        StreamResource streamResource = new StreamResource(resource.getFilename(), () -> {
            try {
                return resource.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при загрузке изображения: " + imagePath, e);
            }
        });

        Image image = new Image(streamResource, "Weather Image");
        image.setWidth("200px");
        image.setHeight("133px");
        return image;
    }

    /**
     * Получает координаты города по его названию.
     *
     * @param city название города.
     * @return объект Json с координатами города.
     * @throws RuntimeException если город не найден или произошла ошибка.
     */
    JsonObject getCityCoordinates(String city) {
        CityWeatherSDK sdk = CityWeatherSDK.createInstance(API_KEY, CityWeatherSDK.Mode.ON_DEMAND);

        try {
            JsonObject geoData = sdk.getGeoData(city);
            if (geoData == null || geoData.entrySet().isEmpty()) {
                throw new RuntimeException("Город не найден: " + city);
            }
            return geoData;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при получении координат города: " + city, e);
        }
    }

    /**
     * Обновляет панель с координатами города.
     *
     * @param coordinatesLayout панель для отображения координат.
     * @param geoData данные о геолокации в формате Json.
     */
    private void updateCoordinates(VerticalLayout coordinatesLayout, JsonObject geoData) {
        coordinatesLayout.removeAll();

        try {
            if (geoData.has("name") && geoData.has("lat") && geoData.has("lon")) {
                String cityName = geoData.get("name").getAsString();
                double lat = geoData.get("lat").getAsDouble();
                double lon = geoData.get("lon").getAsDouble();

                coordinatesLayout.add(new H2("Координаты для города: " + cityName));
                coordinatesLayout.add(new Paragraph("Широта: " + lat));
                coordinatesLayout.add(new Paragraph("Долгота: " + lon));
            } else {
                coordinatesLayout.add(new Paragraph("Ошибка: Данные о городе отсутствуют."));
            }
        } catch (Exception e) {
            coordinatesLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
        }
    }

    /**
     * Тестируемый метод для обновления информации о погоде.
     *
     * @param weatherInfoLayout панель для отображения информации о погоде.
     * @param weatherData данные о погоде в формате JSON.
     */
    public void testableUpdateWeatherInfo(VerticalLayout weatherInfoLayout, String weatherData) {
        updateWeatherInfo(weatherInfoLayout, weatherData);
    }
}