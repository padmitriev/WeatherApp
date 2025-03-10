package dpa.weather.vaa2.db.repository;

import dpa.weather.vaa2.db.entity.WeatherRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherRequestRepository extends JpaRepository<WeatherRequest, Long> {
    List<WeatherRequest> findTop10ByOrderByRequestTimeDesc();
}
