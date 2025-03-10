package dpa.weather;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dpa.weather.db.service.WeatherService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;



public class WeatherViewTest {

    private WeatherService weatherService;
    private WeatherView weatherView;

    @BeforeEach
    public void setUp() {
        weatherService = mock(WeatherService.class);
        weatherView = new WeatherView(weatherService);

        UI ui = Mockito.mock(UI.class);
        UI.setCurrent(ui);
    }

    @Test
    public void testTestableUpdateWeatherInfo_ValidData() {
        VerticalLayout weatherInfoLayout = new VerticalLayout();
        String weatherData = "{\"name\":\"Москва\",\"main\":{\"temp\":10,\"feels_like\":8},\"weather\":[{\"description\":\"облачно\"}]}";

        weatherView.testableUpdateWeatherInfo(weatherInfoLayout, weatherData);

        // Проверка
        assertEquals(4, weatherInfoLayout.getComponentCount());
        assertEquals("Погода в: Москва", ((H2) weatherInfoLayout.getComponentAt(0)).getText());
        assertEquals("облачно", ((Paragraph) weatherInfoLayout.getComponentAt(1)).getText());
        assertEquals("температура: 10.0°C , ощущается как 8.0°C", ((Paragraph) weatherInfoLayout.getComponentAt(2)).getText());
    }

    @Test
    public void testTestableUpdateWeatherInfo_InvalidData() {
        VerticalLayout weatherInfoLayout = new VerticalLayout();
        String weatherData = "{\"invalid_field\":\"data\"}";

        weatherView.testableUpdateWeatherInfo(weatherInfoLayout, weatherData);

        assertEquals(1, weatherInfoLayout.getComponentCount());
        assertEquals("Ошибка при обработке данных. Проверьте правильность координат.", ((Paragraph) weatherInfoLayout.getComponentAt(0)).getText());
    }
}







//public class WeatherViewTest {
//
//    private WeatherService weatherService;
//    private WeatherView weatherView;
//
//    @BeforeEach
//    public void setUp() {
//        weatherService = mock(WeatherService.class);
//        weatherView = new WeatherView(weatherService);
//    }
//
//    @Test
//    public void testShowWeatherButton_Click_ValidCoordinates() {
//        // Подготовка
//        String lat = "55.7558";
//        String lon = "37.6173";
//        String weatherData = "{\"name\":\"Москва\",\"main\":{\"temp\":10,\"feels_like\":8},\"weather\":[{\"description\":\"облачно\"}]}";
//
//        // Мокируем сервис
//        when(weatherService.getWeatherData(lat, lon)).thenReturn(weatherData);
//
//        // Имитируем нажатие кнопки
//        TextField latitudeField = (TextField) weatherView.getComponentAt(0).getComponentAt(0);
//        TextField longitudeField = (TextField) weatherView.getComponentAt(0).getComponentAt(1);
//        Button showWeatherButton = (Button) weatherView.getComponentAt(0).getComponentAt(2);
//
//        latitudeField.setValue(lat);
//        longitudeField.setValue(lon);
//        showWeatherButton.click();
//
//        // Проверка
//        VerticalLayout weatherInfoLayout = (VerticalLayout) weatherView.getComponentAt(0).getComponentAt(3);
//        assertEquals(3, weatherInfoLayout.getComponentCount());
//        assertEquals("Погода в: Москва", ((H2) weatherInfoLayout.getComponentAt(0)).getText());
//        assertEquals("облачно", ((Paragraph) weatherInfoLayout.getComponentAt(1)).getText());
//        assertEquals("температура: 10.0°C , ощущается как 8.0°C", ((Paragraph) weatherInfoLayout.getComponentAt(2)).getText());
//    }
//
//    @Test
//    public void testShowWeatherButton_Click_InvalidCoordinates() {
//        // Подготовка
//        String lat = "invalid_lat";
//        String lon = "invalid_lon";
//
//        // Имитируем нажатие кнопки
//        TextField latitudeField = (TextField) weatherView.getComponentAt(0);
//        TextField longitudeField = (TextField) weatherView.getComponentAt(0).getComponentAt(1);
//        Button showWeatherButton = (Button) weatherView.getComponentAt(0).getComponentAt(2);
//
//        latitudeField.setValue(lat);
//        longitudeField.setValue(lon);
//        showWeatherButton.click();
//
//        // Проверка
//        VerticalLayout weatherInfoLayout = (VerticalLayout) weatherView.getComponentAt(0).getComponentAt(3);
//        assertEquals(1, weatherInfoLayout.getComponentCount());
//        assertEquals("Ошибка при обработке данных. Проверьте правильность координат.", ((Paragraph) weatherInfoLayout.getComponentAt(0)).getText());
//    }
//
//    @Test
//    public void testGetCoordinatesButton_Click_ValidCity() {
//        // Подготовка
//        String city = "Москва";
//        JsonObject geoData = new JsonObject();
//        geoData.addProperty("name", "Москва");
//        geoData.addProperty("lat", 55.7558);
//        geoData.addProperty("lon", 37.6173);
//
//        // Мокируем сервис
//        when(weatherService.getCityCoordinates(city)).thenReturn(geoData);
//
//        // Имитируем нажатие кнопки
//        TextField cityField = (TextField) weatherView.getComponentAt(0).getComponentAt(4);
//        Button getCoordinatesButton = (Button) weatherView.getComponentAt(0).getComponentAt(5);
//
//        cityField.setValue(city);
//        getCoordinatesButton.click();
//
//        // Проверка
//        VerticalLayout coordinatesLayout = (VerticalLayout) weatherView.getComponentAt(0).getComponentAt(6);
//        assertEquals(3, coordinatesLayout.getComponentCount());
//        assertEquals("Координаты для города: Москва", ((H2) coordinatesLayout.getComponentAt(0)).getText());
//        assertEquals("Широта: 55.7558", ((Paragraph) coordinatesLayout.getComponentAt(1)).getText());
//        assertEquals("Долгота: 37.6173", ((Paragraph) coordinatesLayout.getComponentAt(2)).getText());
//    }
//
//    @Test
//    public void testGetCoordinatesButton_Click_InvalidCity() {
//        // Подготовка
//        String city = "НеизвестныйГород";
//
//        // Мокируем сервис
//        when(weatherService.getCityCoordinates(city)).thenReturn(null);
//
//        // Имитируем нажатие кнопки
//        TextField cityField = (TextField) weatherView.getComponentAt(0).getComponentAt(4);
//        Button getCoordinatesButton = (Button) weatherView.getComponentAt(0).getComponentAt(5);
//
//        cityField.setValue(city);
//        getCoordinatesButton.click();
//
//        // Проверка
//        VerticalLayout coordinatesLayout = (VerticalLayout) weatherView.getComponentAt(0).getComponentAt(6);
//        assertEquals(1, coordinatesLayout.getComponentCount());
//        assertEquals("Ошибка: Город не найден.", ((Paragraph) coordinatesLayout.getComponentAt(0)).getText());
//    }
//}


//public class WeatherViewTest {
//
//    private WeatherService weatherService;
//    private WeatherView weatherView;
//
//    @BeforeEach
//    public void setUp() {
//        weatherService = mock(WeatherService.class);
//        weatherView = new WeatherView(weatherService);
//    }
//
//    @Test
//    public void testUpdateWeatherInfo_ValidData() {
//        // Подготовка
//        VerticalLayout weatherInfoLayout = new VerticalLayout();
//        String weatherData = "{\"name\":\"Москва\",\"main\":{\"temp\":10,\"feels_like\":8},\"weather\":[{\"description\":\"облачно\"}]}";
//
//        // Вызов метода
//        weatherView.updateWeatherInfo(weatherInfoLayout, weatherData);
//
//        // Проверка
//        assertEquals(3, weatherInfoLayout.getComponentCount());
//        assertEquals("Погода в: Москва", ((H2) weatherInfoLayout.getComponentAt(0)).getText());
//        assertEquals("облачно", ((Paragraph) weatherInfoLayout.getComponentAt(1)).getText());
//        assertEquals("температура: 10.0°C , ощущается как 8.0°C", ((Paragraph) weatherInfoLayout.getComponentAt(2)).getText());
//    }
//
//    @Test
//    public void testUpdateWeatherInfo_InvalidData() {
//        // Подготовка
//        VerticalLayout weatherInfoLayout = new VerticalLayout();
//        String weatherData = "{\"invalid_field\":\"data\"}";
//
//        // Вызов метода
//        weatherView.updateWeatherInfo(weatherInfoLayout, weatherData);
//
//        // Проверка
//        assertEquals(1, weatherInfoLayout.getComponentCount());
//        assertEquals("Ошибка при обработке данных. Проверьте правильность координат.", ((Paragraph) weatherInfoLayout.getComponentAt(0)).getText());
//    }
//
//    @Test
//    public void testUpdateCoordinates_ValidData() {
//        // Подготовка
//        VerticalLayout coordinatesLayout = new VerticalLayout();
//        JsonObject geoData = new JsonObject();
//        geoData.addProperty("name", "Москва");
//        geoData.addProperty("lat", 55.7558);
//        geoData.addProperty("lon", 37.6173);
//
//        // Вызов метода
//        weatherView.updateCoordinates(coordinatesLayout, geoData);
//
//        // Проверка
//        assertEquals(3, coordinatesLayout.getComponentCount());
//        assertEquals("Координаты для города: Москва", ((H2) coordinatesLayout.getComponentAt(0)).getText());
//        assertEquals("Широта: 55.7558", ((Paragraph) coordinatesLayout.getComponentAt(1)).getText());
//        assertEquals("Долгота: 37.6173", ((Paragraph) coordinatesLayout.getComponentAt(2)).getText());
//    }
//
//    @Test
//    public void testUpdateCoordinates_InvalidData() {
//        // Подготовка
//        VerticalLayout coordinatesLayout = new VerticalLayout();
//        JsonObject geoData = new JsonObject(); // Пустой объект
//
//        // Вызов метода
//        weatherView.updateCoordinates(coordinatesLayout, geoData);
//
//        // Проверка
//        assertEquals(1, coordinatesLayout.getComponentCount());
//        assertEquals("Ошибка: Данные о городе отсутствуют.", ((Paragraph) coordinatesLayout.getComponentAt(0)).getText());
//    }
//}


//class WeatherViewTest {
//
//    private static final String API_KEY = "5f8f1e322944e656897cbd9549859b6b"; // Укажите ваш ключ API
//    private CityWeatherSDK sdk = mock(CityWeatherSDK.class);
//    private WeatherView weatherView = new WeatherView(); // Замените на ваш класс, содержащий метод
//
//    @Test
//    public void testGetCityCoordinates_Success() throws IOException {
//        // Подготовка
//        String city = "Москва";
//        JsonObject expectedGeoData = new JsonObject();
//        expectedGeoData.addProperty("lat", 55.7558);
//        expectedGeoData.addProperty("lng", 37.6173);
//
//        when(sdk.getGeoData(city)).thenReturn(expectedGeoData);
//
//        // Вызов метода
//        JsonObject result = weatherView.getCityCoordinates(city);
//
//        // Проверка
//        assertNotNull(result);
//        assertEquals(55.7558, result.get("lat").getAsDouble());
//        assertEquals(37.6173, result.get("lng").getAsDouble());
//    }
//
//    @Test
//    public void testGetCityCoordinates_CityNotFound() throws IOException {
//        // Подготовка
//        String city = "НеизвестныйГород";
//
//        when(sdk.getGeoData(city)).thenReturn(null);
//
//        // Проверка
//        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
//            weatherView.getCityCoordinates(city);
//        });
//
//        assertEquals("Город не найден: " + city, exception.getMessage());
//    }
//
//    @Test
//    public void testGetCityCoordinates_IOException() throws IOException {
//        // Подготовка
//        String city = "Москва";
//
//        when(sdk.getGeoData(city)).thenThrow(new IOException("Ошибка сети"));
//
//        // Проверка
//        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
//            weatherView.getCityCoordinates(city);
//        });
//
//        assertEquals("Ошибка при получении координат города: " + city, exception.getMessage());
//    }
//}