package dpa.weather.vaa2.db.service.impl;

import dpa.weather.vaa2.db.entity.WeatherRequest;
import dpa.weather.vaa2.db.repository.WeatherRequestRepository;
import dpa.weather.vaa2.db.service.WeatherService;
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
