package dpa.weather.db.service.impl;

import dpa.weather.db.repository.WeatherRequestRepository;
import dpa.weather.db.entity.WeatherRequest;
import dpa.weather.db.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRequestRepository weatherRequestRepository;

    @Override
    public List<WeatherRequest> getRecentRequests() {
        return weatherRequestRepository.findTop10ByOrderByRequestTimeDesc();
    }

    @Override
    public void saveWeatherRequest(WeatherRequest request) {
        weatherRequestRepository.save(request);
    }
}
