package dpa.weather.vaa2;

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
import dpa.weather.vaa2.db.entity.WeatherRequest;
import dpa.weather.vaa2.db.service.WeatherService;
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

@Route("")
@UIScope
@Service
public class WeatherView extends HorizontalLayout {
    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Замените на ваш API ключ

    private final WeatherService weatherService;
    private VerticalLayout recentRequestsLayout;
    private VerticalLayout weatherInfoLayout;
    private VerticalLayout coordinatesLayout; // Контейнер для отображения координат

    public WeatherView(WeatherService weatherService) {
        this.weatherService = weatherService;

        // Левая панель: поля ввода широты и долготы
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

        // Новое поле для ввода города и кнопка "Узнать координаты"
        TextField cityField = new TextField("Город");
        Button getCoordinatesButton = new Button("Узнать координаты");
        coordinatesLayout = new VerticalLayout(); // Контейнер для отображения координат

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
//                coordinatesLayout.add(new Paragraph("Ошибка: " + e.getMessage()));
                coordinatesLayout.add(new Paragraph("Ошибка: Город не найден."));
            }
        });

        // Добавляем элементы в левую панель
        leftPanel.add(latitudeField, longitudeField, showWeatherButton, weatherInfoLayout, cityField, getCoordinatesButton, coordinatesLayout);
        leftPanel.setPadding(true); // Добавляем отступы
        leftPanel.getStyle().set("border-right", "1px solid #ccc"); // Добавляем границу справа

        // Правая панель: история запросов
        recentRequestsLayout = getRecentRequestsLayout();
        recentRequestsLayout.setPadding(true); // Добавляем отступы

        // Размещаем все элементы в горизонтальном макете
        add(leftPanel, recentRequestsLayout);
        setAlignItems(Alignment.STRETCH); // Выровнять элементы по высоте
        setSpacing(false); // Убрать отступы между элементами

        // Настройка flex-grow для автоматического распределения ширины
        leftPanel.setWidth(null); // Сброс фиксированной ширины
        recentRequestsLayout.setWidth(null); // Сброс фиксированной ширины
        leftPanel.getStyle().set("flex-grow", "1"); // Левая панель занимает 50%
        recentRequestsLayout.getStyle().set("flex-grow", "1"); // Правая панель занимает 50%
    }

    private VerticalLayout getRecentRequestsLayout() {
        VerticalLayout recentRequestsLayout = new VerticalLayout();
        updateRecentRequestsLayout(recentRequestsLayout);
        return recentRequestsLayout;
    }

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

    private Image getWeatherImage(double temperature) {
        String imagePath;
        if (temperature > 0) {
            imagePath = "images/teplo.png"; // Путь к изображению для тёплой погоды
        } else {
            imagePath = "images/holodno.png"; // Путь к изображению для холодной погоды
        }

        // Используем ClassPathResource для загрузки изображения
        ClassPathResource resource = new ClassPathResource(imagePath);
        if (!resource.exists()) {
            System.out.println("Не найден файл: " + imagePath);
            return new Image(); // Возвращаем пустое изображение, если файл не найден
        }

        // Создаём StreamResource для Vaadin
        StreamResource streamResource = new StreamResource(resource.getFilename(), () -> {
            try {
                return resource.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка при загрузке изображения: " + imagePath, e);
            }
        });

        // Создаём и настраиваем изображение
        Image image = new Image(streamResource, "Weather Image");
        image.setWidth("200px");
        image.setHeight("133px");
        return image;
    }

    private JsonObject getCityCoordinates(String city) {
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


    private void updateCoordinates(VerticalLayout coordinatesLayout, JsonObject geoData) {
        coordinatesLayout.removeAll();

        try {
            // Проверяем, содержит ли JsonObject данные о городе
            if (geoData.has("name") && geoData.has("lat") && geoData.has("lon")) {
                // Получаем значения из JsonObject
                String cityName = geoData.get("name").getAsString();
                double lat = geoData.get("lat").getAsDouble();
                double lon = geoData.get("lon").getAsDouble();

                // Отображаем координаты
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
}



//@Route("")
//@UIScope
//@Service
//public class WeatherView extends HorizontalLayout {
//    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Замените на ваш API ключ
//
//    private final WeatherService weatherService;
//    private VerticalLayout recentRequestsLayout;
//    private VerticalLayout weatherInfoLayout;
//    private VerticalLayout coordinatesLayout; // Новый контейнер для отображения координат
//
//    public WeatherView(WeatherService weatherService) {
//        this.weatherService = weatherService;
//
//        // Левая панель: поля ввода широты и долготы
//        VerticalLayout leftPanel = new VerticalLayout();
//        TextField latitudeField = new TextField("Широта");
//        TextField longitudeField = new TextField("Долгота");
//        Button showWeatherButton = new Button("Показать погоду!");
//        weatherInfoLayout = new VerticalLayout();
//
//        showWeatherButton.addClickListener(event -> {
//            String lat = latitudeField.getValue();
//            String lon = longitudeField.getValue();
//            String weatherData = getWeatherData(lat, lon);
//
//            System.out.println(weatherData);
//
//            updateWeatherInfo(weatherInfoLayout, weatherData);
//            saveWeatherRequest(lat, lon, weatherData);
//        });
//
//        // Новое поле для ввода города и кнопка "Узнать координаты"
//        TextField cityField = new TextField("Город");
//        Button getCoordinatesButton = new Button("Узнать координаты");
//        coordinatesLayout = new VerticalLayout(); // Контейнер для отображения координат
//
//        getCoordinatesButton.addClickListener(event -> {
//            String city = cityField.getValue();
//            JsonObject geoData = getCityCoordinates(city);
//
//            if (geoData != null) {
//                updateCoordinates(coordinatesLayout, geoData);
//            } else {
//                coordinatesLayout.removeAll();
//                coordinatesLayout.add(new Paragraph("Не удалось получить координаты для города: " + city));
//            }
//        });
//
//        // Добавляем элементы в левую панель
//        leftPanel.add(cityField, getCoordinatesButton, coordinatesLayout, latitudeField, longitudeField, showWeatherButton, weatherInfoLayout);
//        leftPanel.setPadding(true); // Добавляем отступы
//        leftPanel.getStyle().set("border-right", "1px solid #ccc"); // Добавляем границу справа
//
//        // Правая панель: история запросов
//        recentRequestsLayout = getRecentRequestsLayout();
//        recentRequestsLayout.setPadding(true); // Добавляем отступы
//
//        // Размещаем все элементы в горизонтальном макете
//        add(leftPanel, recentRequestsLayout);
//        setAlignItems(Alignment.STRETCH); // Выровнять элементы по высоте
//        setSpacing(false); // Убрать отступы между элементами
//
//        // Настройка flex-grow для автоматического распределения ширины
//        leftPanel.setWidth(null); // Сброс фиксированной ширины
//        recentRequestsLayout.setWidth(null); // Сброс фиксированной ширины
//        leftPanel.getStyle().set("flex-grow", "1"); // Левая панель занимает 50%
//        recentRequestsLayout.getStyle().set("flex-grow", "1"); // Правая панель занимает 50%
//    }
//
//    private VerticalLayout getRecentRequestsLayout() {
//        VerticalLayout recentRequestsLayout = new VerticalLayout();
//        updateRecentRequestsLayout(recentRequestsLayout);
//        return recentRequestsLayout;
//    }
//
//    private void updateRecentRequestsLayout(VerticalLayout layout) {
//        layout.removeAll();
//        layout.add(new H2("Последние 10 запросов"));
//
//        List<WeatherRequest> recentRequests = weatherService.getRecentRequests();
//        for (WeatherRequest request : recentRequests) {
//            String requestTimeString = request.getRequestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
//            String weatherDescription = request.getWeatherDescription();
//            layout.add(new Paragraph(
//                    requestTimeString + "  |  " + "Широта: " + request.getLatitude() + ", Долгота: " + request.getLongitude() + "  |  " + "Ответ: " + weatherDescription));
//        }
//    }
//
//    private void saveWeatherRequest(String lat, String lon, String weatherData) {
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            WeatherRequest request = new WeatherRequest();
//            request.setLatitude(lat);
//            request.setLongitude(lon);
//            request.setWeatherData(weatherData);
//            request.setWeatherDescription(weatherDescription);
//            request.setRequestTime(LocalDateTime.now());
//            weatherService.saveWeatherRequest(request);
//
//            updateRecentRequestsLayout(recentRequestsLayout);
//        } catch (JSONException e) {
//            // Обработка ошибки
//        }
//    }
//
//    private String getWeatherData(String lat, String lon) {
//        OkHttpClient client = new OkHttpClient();
//        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&&lang=ru&units=metric", lat, lon, API_KEY);
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                return response.body().string();
//            } else {
//                return "Ошибка при получении данных: " + response.message();
//            }
//        } catch (IOException e) {
//            return "Ошибка: " + e.getMessage();
//        }
//    }
//
//    private void updateWeatherInfo(VerticalLayout weatherInfoLayout, String weatherData) {
//        weatherInfoLayout.removeAll();
//
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String cityName = json.getString("name");
//            double temperature = json.getJSONObject("main").getDouble("temp");
//            double feels = json.getJSONObject("main").getDouble("feels_like");
//            String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            weatherInfoLayout.add(new H2("Погода в: " + cityName));
//            weatherInfoLayout.add(new Paragraph(description));
//            weatherInfoLayout.add(new Paragraph("температура: " + temperature + "°C" + " , ощущается как " + feels + "°C"));
//
//            Image weatherImage = getWeatherImage(temperature);
//            weatherInfoLayout.add(weatherImage);
//
//        } catch (JSONException e) {
//            weatherInfoLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }
//
//    private Image getWeatherImage(double temperature) {
//        String imagePath;
//        if (temperature > 0) {
//            imagePath = "images/teplo.png"; // Путь к изображению для тёплой погоды
//        } else {
//            imagePath = "images/holodno.png"; // Путь к изображению для холодной погоды
//        }
//
//        // Используем ClassPathResource для загрузки изображения
//        ClassPathResource resource = new ClassPathResource(imagePath);
//        if (!resource.exists()) {
//            System.out.println("Не найден файл: " + imagePath);
//            return new Image(); // Возвращаем пустое изображение, если файл не найден
//        }
//
//        // Создаём StreamResource для Vaadin
//        StreamResource streamResource = new StreamResource(resource.getFilename(), () -> {
//            try {
//                return resource.getInputStream();
//            } catch (IOException e) {
//                throw new RuntimeException("Ошибка при загрузке изображения: " + imagePath, e);
//            }
//        });
//
//        // Создаём и настраиваем изображение
//        Image image = new Image(streamResource, "Weather Image");
//        image.setWidth("200px");
//        image.setHeight("133px");
//        return image;
//    }
//
//    private JsonObject getCityCoordinates(String city) {
//        CityWeatherSDK sdk = CityWeatherSDK.createInstance(API_KEY, CityWeatherSDK.Mode.ON_DEMAND);
//
//        try {
//            return sdk.getGeoData(city);
//        } catch (IOException e) {
//            throw new RuntimeException("Ошибка при получении координат города: " + city, e);
//        }
//    }
//
//    private void updateCoordinates(VerticalLayout coordinatesLayout, JsonObject geoData) {
//        coordinatesLayout.removeAll();
//
//        try {
//            String cityName = geoData.get("name").getAsString();
//            double lat = geoData.get("lat").getAsDouble();
//            double lon = geoData.get("lon").getAsDouble();
////            String cityName = geoData.getString("name");
////            double lat = geoData.getDouble("lat");
////            double lon = geoData.getDouble("lon");
//
//            coordinatesLayout.add(new H2("Координаты для города: " + cityName));
//            coordinatesLayout.add(new Paragraph("Широта: " + lat));
//            coordinatesLayout.add(new Paragraph("Долгота: " + lon));
//        } catch (Exception e) {
//            coordinatesLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }
//}


//@Route("")
//@UIScope
//@Service
//public class WeatherView extends HorizontalLayout {
//    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Замените на ваш API ключ
//
//    private final WeatherService weatherService;
//    private VerticalLayout recentRequestsLayout;
//    private VerticalLayout weatherInfoLayout;
//    private VerticalLayout coordinatesLayout; // Новый контейнер для отображения координат
//
//    public WeatherView(WeatherService weatherService) {
//        this.weatherService = weatherService;
//
//        // Левая панель: поля ввода широты и долготы
//        VerticalLayout leftPanel = new VerticalLayout();
//        TextField latitudeField = new TextField("Широта");
//        TextField longitudeField = new TextField("Долгота");
//        Button showWeatherButton = new Button("Показать погоду!");
//        weatherInfoLayout = new VerticalLayout();
//
//        showWeatherButton.addClickListener(event -> {
//            String lat = latitudeField.getValue();
//            String lon = longitudeField.getValue();
//            String weatherData = getWeatherData(lat, lon);
//
//            System.out.println(weatherData);
//
//            updateWeatherInfo(weatherInfoLayout, weatherData);
//            saveWeatherRequest(lat, lon, weatherData);
//        });
//
//        // Новое поле для ввода города и кнопка "Узнать координаты"
//        TextField cityField = new TextField("Город");
//        Button getCoordinatesButton = new Button("Узнать координаты");
//        coordinatesLayout = new VerticalLayout(); // Контейнер для отображения координат
//
//        getCoordinatesButton.addClickListener(event -> {
//            String city = cityField.getValue();
//            String coordinatesData = getCityCoordinates(city);
//
//            if (coordinatesData != null) {
//                updateCoordinates(coordinatesLayout, coordinatesData);
//            } else {
//                coordinatesLayout.removeAll();
//                coordinatesLayout.add(new Paragraph("Не удалось получить координаты для города: " + city));
//            }
//        });
//
//        // Добавляем элементы в левую панель
//        leftPanel.add(cityField, getCoordinatesButton, coordinatesLayout, latitudeField, longitudeField, showWeatherButton, weatherInfoLayout);
//        leftPanel.setPadding(true); // Добавляем отступы
//        leftPanel.getStyle().set("border-right", "1px solid #ccc"); // Добавляем границу справа
//
//        // Правая панель: история запросов
//        recentRequestsLayout = getRecentRequestsLayout();
//        recentRequestsLayout.setPadding(true); // Добавляем отступы
//
//        // Размещаем все элементы в горизонтальном макете
//        add(leftPanel, recentRequestsLayout);
//        setAlignItems(Alignment.STRETCH); // Выровнять элементы по высоте
//        setSpacing(false); // Убрать отступы между элементами
//
//        // Настройка flex-grow для автоматического распределения ширины
//        leftPanel.setWidth(null); // Сброс фиксированной ширины
//        recentRequestsLayout.setWidth(null); // Сброс фиксированной ширины
//        leftPanel.getStyle().set("flex-grow", "1"); // Левая панель занимает 50%
//        recentRequestsLayout.getStyle().set("flex-grow", "1"); // Правая панель занимает 50%
//    }
//
//    private VerticalLayout getRecentRequestsLayout() {
//        VerticalLayout recentRequestsLayout = new VerticalLayout();
//        updateRecentRequestsLayout(recentRequestsLayout);
//        return recentRequestsLayout;
//    }
//
//    private void updateRecentRequestsLayout(VerticalLayout layout) {
//        layout.removeAll();
//        layout.add(new H2("Последние 10 запросов"));
//
//        List<WeatherRequest> recentRequests = weatherService.getRecentRequests();
//        for (WeatherRequest request : recentRequests) {
//            String requestTimeString = request.getRequestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
//            String weatherDescription = request.getWeatherDescription();
//            layout.add(new Paragraph(
//                    requestTimeString + "  |  " + "Широта: " + request.getLatitude() + ", Долгота: " + request.getLongitude() + "  |  " + "Ответ: " + weatherDescription));
//        }
//    }
//
//    private void saveWeatherRequest(String lat, String lon, String weatherData) {
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            WeatherRequest request = new WeatherRequest();
//            request.setLatitude(lat);
//            request.setLongitude(lon);
//            request.setWeatherData(weatherData);
//            request.setWeatherDescription(weatherDescription);
//            request.setRequestTime(LocalDateTime.now());
//            weatherService.saveWeatherRequest(request);
//
//            updateRecentRequestsLayout(recentRequestsLayout);
//        } catch (JSONException e) {
//            // Обработка ошибки
//        }
//    }
//
//    private String getWeatherData(String lat, String lon) {
//        OkHttpClient client = new OkHttpClient();
//        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&&lang=ru&units=metric", lat, lon, API_KEY);
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                return response.body().string();
//            } else {
//                return "Ошибка при получении данных: " + response.message();
//            }
//        } catch (IOException e) {
//            return "Ошибка: " + e.getMessage();
//        }
//    }
//
//    private void updateWeatherInfo(VerticalLayout weatherInfoLayout, String weatherData) {
//        weatherInfoLayout.removeAll();
//
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String cityName = json.getString("name");
//            double temperature = json.getJSONObject("main").getDouble("temp");
//            double feels = json.getJSONObject("main").getDouble("feels_like");
//            String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            weatherInfoLayout.add(new H2("Погода в: " + cityName));
//            weatherInfoLayout.add(new Paragraph(description));
//            weatherInfoLayout.add(new Paragraph("температура: " + temperature + "°C" + " , ощущается как " + feels + "°C"));
//
//            Image weatherImage = getWeatherImage(temperature);
//            weatherInfoLayout.add(weatherImage);
//
//        } catch (JSONException e) {
//            weatherInfoLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }
//
//    private Image getWeatherImage(double temperature) {
//        String imagePath;
//        if (temperature > 0) {
//            imagePath = "images/teplo.png"; // Путь к изображению для тёплой погоды
//        } else {
//            imagePath = "images/holodno.png"; // Путь к изображению для холодной погоды
//        }
//
//        // Используем ClassPathResource для загрузки изображения
//        ClassPathResource resource = new ClassPathResource(imagePath);
//        if (!resource.exists()) {
//            System.out.println("Не найден файл: " + imagePath);
//            return new Image(); // Возвращаем пустое изображение, если файл не найден
//        }
//
//        // Создаём StreamResource для Vaadin
//        StreamResource streamResource = new StreamResource(resource.getFilename(), () -> {
//            try {
//                return resource.getInputStream();
//            } catch (IOException e) {
//                throw new RuntimeException("Ошибка при загрузке изображения: " + imagePath, e);
//            }
//        });
//
//        // Создаём и настраиваем изображение
//        Image image = new Image(streamResource, "Weather Image");
//        image.setWidth("200px");
//        image.setHeight("133px");
//        return image;
//    }
//
//    private String getCityCoordinates(String city) {
//        OkHttpClient client = new OkHttpClient();
//        String url = String.format("http://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s", city, API_KEY);
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                return response.body().string();
//            } else {
//                return null;
//            }
//        } catch (IOException e) {
//            return null;
//        }
//    }
//
//    private void updateCoordinates(VerticalLayout coordinatesLayout, String coordinatesData) {
//        coordinatesLayout.removeAll();
//
//        try {
//            JSONArray jsonArray = new JSONArray(coordinatesData);
//            if (jsonArray.length() > 0) {
//                JSONObject json = jsonArray.getJSONObject(0);
//                String cityName = json.getString("name");
//                double lat = json.getDouble("lat");
//                double lon = json.getDouble("lon");
//
//                coordinatesLayout.add(new H2("Координаты для города: " + cityName));
//                coordinatesLayout.add(new Paragraph("Широта: " + lat));
//                coordinatesLayout.add(new Paragraph("Долгота: " + lon));
//            } else {
//                coordinatesLayout.add(new Paragraph("Город не найден."));
//            }
//        } catch (JSONException e) {
//            coordinatesLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }
//}

//@Route("")
//@UIScope
//@Service
//public class WeatherView extends HorizontalLayout {
//    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Замените на ваш API ключ
//
//    private final WeatherService weatherService;
//    private VerticalLayout recentRequestsLayout;
//    private VerticalLayout weatherInfoLayout;
//
//    public WeatherView(WeatherService weatherService) {
//        this.weatherService = weatherService;
//
//        // Левая панель: поля ввода широты и долготы
//        VerticalLayout leftPanel = new VerticalLayout();
//        TextField latitudeField = new TextField("Широта");
//        TextField longitudeField = new TextField("Долгота");
//        Button showWeatherButton = new Button("Показать погоду!");
//        weatherInfoLayout = new VerticalLayout();
//
//        showWeatherButton.addClickListener(event -> {
//            String lat = latitudeField.getValue();
//            String lon = longitudeField.getValue();
//            String weatherData = getWeatherData(lat, lon);
//
//            System.out.println(weatherData);
//
//            updateWeatherInfo(weatherInfoLayout, weatherData);
//            saveWeatherRequest(lat, lon, weatherData);
//        });
//
//        leftPanel.add(latitudeField, longitudeField, showWeatherButton, weatherInfoLayout);
//        leftPanel.setPadding(true); // Добавляем отступы
//        leftPanel.getStyle().set("border-right", "1px solid #ccc"); // Добавляем границу справа
//
//        // Правая панель: история запросов
//        recentRequestsLayout = getRecentRequestsLayout();
//        recentRequestsLayout.setPadding(true); // Добавляем отступы
//
//        // Размещаем все элементы в горизонтальном макете
//        add(leftPanel, recentRequestsLayout);
//        setAlignItems(Alignment.STRETCH); // Выровнять элементы по высоте
//        setSpacing(false); // Убрать отступы между элементами
//
//        // Настройка flex-grow для автоматического распределения ширины
//        leftPanel.setWidth(null); // Сброс фиксированной ширины
//        recentRequestsLayout.setWidth(null); // Сброс фиксированной ширины
//        leftPanel.getStyle().set("flex-grow", "1"); // Левая панель занимает 50%
//        recentRequestsLayout.getStyle().set("flex-grow", "1"); // Правая панель занимает 50%
//    }
//
//    private VerticalLayout getRecentRequestsLayout() {
//        VerticalLayout recentRequestsLayout = new VerticalLayout();
//        updateRecentRequestsLayout(recentRequestsLayout);
//        return recentRequestsLayout;
//    }
//
//    private void updateRecentRequestsLayout(VerticalLayout layout) {
//        layout.removeAll();
//        layout.add(new H2("Последние 10 запросов"));
//
//        List<WeatherRequest> recentRequests = weatherService.getRecentRequests();
//        for (WeatherRequest request : recentRequests) {
//            String requestTimeString = request.getRequestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
//            String weatherDescription = request.getWeatherDescription();
//            layout.add(new Paragraph(
//                    requestTimeString + "  |  " + "Широта: " + request.getLatitude() + ", Долгота: " + request.getLongitude() + "  |  " + "Ответ: " + weatherDescription));
//        }
//    }
//
//    private void saveWeatherRequest(String lat, String lon, String weatherData) {
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            WeatherRequest request = new WeatherRequest();
//            request.setLatitude(lat);
//            request.setLongitude(lon);
//            request.setWeatherData(weatherData);
//            request.setWeatherDescription(weatherDescription);
//            request.setRequestTime(LocalDateTime.now());
//            weatherService.saveWeatherRequest(request);
//
//            updateRecentRequestsLayout(recentRequestsLayout);
//        } catch (JSONException e) {
//            // Обработка ошибки
//        }
//    }
//
//    private String getWeatherData(String lat, String lon) {
//        OkHttpClient client = new OkHttpClient();
//        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&&lang=ru&units=metric", lat, lon, API_KEY);
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                return response.body().string();
//            } else {
//                return "Ошибка при получении данных: " + response.message();
//            }
//        } catch (IOException e) {
//            return "Ошибка: " + e.getMessage();
//        }
//    }
//
//    private void updateWeatherInfo(VerticalLayout weatherInfoLayout, String weatherData) {
//        weatherInfoLayout.removeAll();
//
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String cityName = json.getString("name");
//            double temperature = json.getJSONObject("main").getDouble("temp");
//            double feels = json.getJSONObject("main").getDouble("feels_like");
//            String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            weatherInfoLayout.add(new H2("Погода в: " + cityName));
//            weatherInfoLayout.add(new Paragraph(description));
//            weatherInfoLayout.add(new Paragraph("температура: " + temperature + "°C" + " , ощущается как " + feels + "°C"));
//
//            Image weatherImage = getWeatherImage(temperature);
//            weatherInfoLayout.add(weatherImage);
//
//        } catch (JSONException e) {
//            weatherInfoLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }
//
//    private Image getWeatherImage(double temperature) {
//        String imagePath;
//        if (temperature > 0) {
//            imagePath = "images/teplo.png"; // Путь к изображению для тёплой погоды
//        } else {
//            imagePath = "images/holodno.png"; // Путь к изображению для холодной погоды
//        }
//
//        // Используем ClassPathResource для загрузки изображения
//        ClassPathResource resource = new ClassPathResource(imagePath);
//        if (!resource.exists()) {
//            System.out.println("Не найден файл: " + imagePath);
//            return new Image(); // Возвращаем пустое изображение, если файл не найден
//        }
//
//        // Создаём StreamResource для Vaadin
//        StreamResource streamResource = new StreamResource(resource.getFilename(), () -> {
//            try {
//                return resource.getInputStream();
//            } catch (IOException e) {
//                throw new RuntimeException("Ошибка при загрузке изображения: " + imagePath, e);
//            }
//        });
//
//        // Создаём и настраиваем изображение
//        Image image = new Image(streamResource, "Weather Image");
//        image.setWidth("200px");
//        image.setHeight("133px");
//        return image;
//    }
//
//    public JsonObject void getCityCoordinates(String city) {
//
//        CityWeatherSDK sdk = CityWeatherSDK.createInstance(API_KEY, CityWeatherSDK.Mode.ON_DEMAND);
//
//        JsonObject geoData;
//
//        try {
//            geoData = sdk.getGeoData(city);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
////    private Image getWeatherImage(double temperature) {
////        String imagePath;
////        if (temperature > 0) {
////            imagePath = "images/teplo.png";
////        } else {
////            imagePath = "images/holodno.png";
////        }
////
////        InputStream inputStream = VaadinServlet.getCurrent().getServletContext().getResourceAsStream("/" + imagePath);
////        if (inputStream == null) {
////            System.out.println("Не найден файл: " + imagePath);
////        } else {
////            System.out.println("Путь к файлу: " + imagePath);
////        }
////        Image image = new Image(new StreamResource(imagePath, () -> inputStream), "Weather Image");
////        image.setWidth("200px");
////        image.setHeight("133px");
////        return image;
////    }
//
////    private Image getWeatherImage(double temperature) {
////        String imagePath;
////        if (temperature > 0) {
////            imagePath = "images/teplo.png";
////        } else {
////            imagePath = "images/holodno.png";
////        }
////
////        Image image = new Image(imagePath, "Weather Image");
////        image.setWidth("200px");
////        image.setHeight("133px");
////        return image;
////    }
//}



//@Route("")
//@UIScope
//@Service
//public class WeatherView extends HorizontalLayout {
//    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Замените на ваш API ключ
//
//    private final WeatherService weatherService;
//    private VerticalLayout recentRequestsLayout;
//    private VerticalLayout weatherInfoLayout;
//
//    public WeatherView(WeatherService weatherService) {
//        this.weatherService = weatherService;
//
//        // Левая панель: поля ввода широты и долготы
//        VerticalLayout leftPanel = new VerticalLayout();
//        leftPanel.setWidth("300px"); // Фиксированная ширина
//        TextField latitudeField = new TextField("Широта");
//        TextField longitudeField = new TextField("Долгота");
//        Button showWeatherButton = new Button("Показать погоду!");
//        weatherInfoLayout = new VerticalLayout();
//
//        showWeatherButton.addClickListener(event -> {
//            String lat = latitudeField.getValue();
//            String lon = longitudeField.getValue();
//            String weatherData = getWeatherData(lat, lon);
//            updateWeatherInfo(weatherInfoLayout, weatherData);
//            saveWeatherRequest(lat, lon, weatherData);
//        });
//
//        leftPanel.add(latitudeField, longitudeField, showWeatherButton, weatherInfoLayout);
//
//        // Середина: разделительная линия
//        Div separator = new Div();
//        separator.getStyle()
//                .set("width", "1px")
//                .set("background-color", "#ccc")
//                .set("margin", "0 16px");
//
//        // Правая панель: история запросов
//        recentRequestsLayout = getRecentRequestsLayout();
//        recentRequestsLayout.setWidth("400px"); // Фиксированная ширина
//
//        // Размещаем все элементы в горизонтальном макете
//        add(leftPanel, separator, recentRequestsLayout);
//        setAlignItems(Alignment.STRETCH); // Выровнять элементы по высоте
//        setSpacing(true); // Добавить отступы между элементами
//    }
//
//    private VerticalLayout getRecentRequestsLayout() {
//        VerticalLayout recentRequestsLayout = new VerticalLayout();
//        updateRecentRequestsLayout(recentRequestsLayout);
//        return recentRequestsLayout;
//    }
//
//    private void updateRecentRequestsLayout(VerticalLayout layout) {
//        layout.removeAll();
//        layout.add(new H2("Последние 10 запросов"));
//
//        List<WeatherRequest> recentRequests = weatherService.getRecentRequests();
//        for (WeatherRequest request : recentRequests) {
//            String requestTimeString = request.getRequestTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
//            String weatherDescription = request.getWeatherDescription();
//            layout.add(new Paragraph(
//                    "Время: " + requestTimeString + ", Широта: " + request.getLatitude() + ", Долгота: " + request.getLongitude() + ", Погода: " + weatherDescription));
//        }
//    }
//
//    private void saveWeatherRequest(String lat, String lon, String weatherData) {
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            WeatherRequest request = new WeatherRequest();
//            request.setLatitude(lat);
//            request.setLongitude(lon);
//            request.setWeatherData(weatherData);
//            request.setWeatherDescription(weatherDescription);
//            request.setRequestTime(LocalDateTime.now());
//            weatherService.saveWeatherRequest(request);
//
//            updateRecentRequestsLayout(recentRequestsLayout);
//        } catch (JSONException e) {
//            // Обработка ошибки
//        }
//    }
//
//    private String getWeatherData(String lat, String lon) {
//        OkHttpClient client = new OkHttpClient();
//        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric", lat, lon, API_KEY);
//        Request request = new Request.Builder()
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (response.isSuccessful()) {
//                return response.body().string();
//            } else {
//                return "Ошибка при получении данных: " + response.message();
//            }
//        } catch (IOException e) {
//            return "Ошибка: " + e.getMessage();
//        }
//    }
//
//    private void updateWeatherInfo(VerticalLayout weatherInfoLayout, String weatherData) {
//        weatherInfoLayout.removeAll();
//
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String cityName = json.getString("name");
//            double temperature = json.getJSONObject("main").getDouble("temp");
//            String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            weatherInfoLayout.add(new H2("Погода в " + cityName));
//            weatherInfoLayout.add(new Paragraph("Температура: " + temperature + "°C"));
//            weatherInfoLayout.add(new Paragraph("Описание: " + description));
//        } catch (JSONException e) {
//            weatherInfoLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }
//}


// ........................................................



//    private JsonObject getCityCoordinates(String city) {
//        CityWeatherSDK sdk = CityWeatherSDK.createInstance(API_KEY, CityWeatherSDK.Mode.ON_DEMAND);
//
//        try {
//            JsonArray geoDataArray = sdk.getGeoData(city); // Предполагаем, что метод возвращает JsonArray
//            if (geoDataArray == null || geoDataArray.size() == 0) {
//                throw new RuntimeException("Город не найден: " + city);
//            }
//            return geoDataArray.get(0).getAsJsonObject(); // Возвращаем первый элемент массива
//        } catch (IOException e) {
//            throw new RuntimeException("Ошибка при получении координат города: " + city, e);
//        }
//    }

//    private JsonObject getCityCoordinates(String city) {
//        CityWeatherSDK sdk = CityWeatherSDK.createInstance(API_KEY, CityWeatherSDK.Mode.ON_DEMAND);
//
//        try {
//            return sdk.getGeoData(city);
//        } catch (IOException e) {
//            throw new RuntimeException("Ошибка при получении координат города: " + city, e);
//        }
//    }


//        getCoordinatesButton.addClickListener(event -> {
//            String city = cityField.getValue();
//            if (city == null || city.isEmpty()) {
//                coordinatesLayout.removeAll();
//                coordinatesLayout.add(new Paragraph("Ошибка: Введите название города."));
//                return;
//            }
//
//            try {
//                JsonObject geoData = getCityCoordinates(city);
//                if (geoData != null) {
//                    updateCoordinates(coordinatesLayout, geoData);
//                } else {
//                    coordinatesLayout.removeAll();
//                    coordinatesLayout.add(new Paragraph("Ошибка: Город не найден."));
//                }
//            } catch (RuntimeException e) {
//                coordinatesLayout.removeAll();
//                coordinatesLayout.add(new Paragraph("Ошибка: " + e.getMessage()));
//            }
//        });


//    private void updateCoordinates(VerticalLayout coordinatesLayout, JsonObject geoData) {
//        coordinatesLayout.removeAll();
//
//        try {
//            // Получаем значения из JsonObject
//            String cityName = geoData.get("name").getAsString();
//            double lat = geoData.get("lat").getAsDouble();
//            double lon = geoData.get("lon").getAsDouble();
//
//            // Отображаем координаты
//            coordinatesLayout.add(new H2("Координаты для города: " + cityName));
//            coordinatesLayout.add(new Paragraph("Широта: " + lat));
//            coordinatesLayout.add(new Paragraph("Долгота: " + lon));
//        } catch (Exception e) {
//            coordinatesLayout.add(new Paragraph("Ошибка при обработке данных: " + e.getMessage()));
//        }
//    }


//    private void saveWeatherRequest(String lat, String lon, String weatherData) {
//        try {
//            JSONObject json = new JSONObject(weatherData);
//            String weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
//
//            WeatherRequest request = new WeatherRequest();
//            request.setLatitude(lat);
//            request.setLongitude(lon);
//            request.setWeatherData(weatherData);
//            request.setWeatherDescription(weatherDescription);
//            request.setRequestTime(LocalDateTime.now());
//            weatherService.saveWeatherRequest(request);
//
//            updateRecentRequestsLayout(recentRequestsLayout);
//        } catch (JSONException e) {
//            // Обработка ошибки
//        }
//    }