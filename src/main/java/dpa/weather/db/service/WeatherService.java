package dpa.weather.vaa2.db.service;

import dpa.weather.vaa2.db.entity.WeatherRequest;

import java.util.List;

public interface WeatherService {
    List<WeatherRequest> getRecentRequests();

    void saveWeatherRequest(WeatherRequest request);
}
