package dpa.weather.vaa2.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "requests")
public class WeatherRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String latitude;
    private String longitude;
    @Column(name = "weather_data", length = 1000)
    private String weatherData;
    private LocalDateTime requestTime;
    private String weatherDescription;
}

