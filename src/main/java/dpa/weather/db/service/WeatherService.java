package dpa.weather.db.service;

import dpa.weather.db.entity.WeatherRequest;

import java.util.List;

public interface WeatherService {
    List<WeatherRequest> getRecentRequests();

    void saveWeatherRequest(WeatherRequest request);
}
