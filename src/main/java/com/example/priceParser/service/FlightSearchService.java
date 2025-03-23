package com.example.priceParser.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.Dimension;
import java.time.Duration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class FlightSearchService {
    
    private final RestTemplate restTemplate;
    private WebDriver driver;
    private WebDriverWait wait;
    
    @Value("${webdriver.chrome.headless:true}")
    private boolean headless;
    
    @Value("${webdriver.chrome.path:}")
    private String chromePath;
    
    @Autowired
    public FlightSearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @PostConstruct
    public void init() {
        try {
            log.info("Инициализация WebDriver...");
            
            if (!chromePath.isEmpty()) {
                log.info("Используем ChromeDriver по пути: {}", chromePath);
                System.setProperty("webdriver.chrome.driver", chromePath);
            } else {
                log.error("Путь к ChromeDriver не указан в настройках (webdriver.chrome.path)");
                throw new RuntimeException("Не указан путь к ChromeDriver");
            }
            
            ChromeOptions options = new ChromeOptions();
            // Указываем путь к исполняемому файлу Chrome
            options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
            
            if (headless) {
                options.addArguments("--headless=new");
            }
            
            // Основные настройки
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--remote-allow-origins=*");
            
            // Дополнительные настройки для обхода блокировок
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-web-security");
            options.addArguments("--allow-running-insecure-content");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--lang=ru-RU");
            options.addArguments("--ignore-certificate-errors");
            options.addArguments("--start-maximized");
            
            // Установка случайного User-Agent
            String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.6943.142 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.6943.0 Safari/537.36"
            };
            options.addArguments("--user-agent=" + userAgents[new Random().nextInt(userAgents.length)]);
            
            // Отключение уведомлений и геолокации
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-geolocation");
            
            // Установка дополнительных возможностей
            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.default_content_setting_values.notifications", 2);
            prefs.put("credentials_enable_service", false);
            prefs.put("profile.password_manager_enabled", false);
            options.setExperimentalOption("prefs", prefs);
            
            // Маскировка автоматизации
            options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
            options.setExperimentalOption("useAutomationExtension", false);
            
            try {
                log.info("Создание экземпляра ChromeDriver...");
                driver = new ChromeDriver(options);
                log.info("ChromeDriver успешно создан");
            } catch (Exception e) {
                log.error("Ошибка при создании ChromeDriver: {}", e.getMessage(), e);
                throw new RuntimeException("Не удалось создать ChromeDriver", e);
            }
            
            wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // Установка размера окна
            driver.manage().window().setSize(new Dimension(1920, 1080));
            
            log.info("WebDriver успешно инициализирован");
        } catch (Exception e) {
            log.error("Ошибка при инициализации WebDriver: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать WebDriver", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (driver != null) {
            try {
                driver.quit();
                log.info("WebDriver успешно закрыт");
            } catch (Exception e) {
                log.error("Ошибка при закрытии WebDriver: {}", e.getMessage());
            }
        }
    }
    
    public List<Map<String, Object>> searchFlights(String departureCity, String arrivalCity, String date) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            log.info("Начало поиска рейсов {} -> {} на дату {}", departureCity, arrivalCity, date);
            
            // Параллельный поиск по разным источникам
            List<Map<String, Object>> yandexResults = parseYandexTravel(departureCity, arrivalCity, date);
            List<Map<String, Object>> avitoResults = parseAvito(departureCity, arrivalCity, date);
            List<Map<String, Object>> tutuResults = parseTutu(departureCity, arrivalCity, date);
            
            // Объединяем результаты
            results.addAll(mergeResults(yandexResults, avitoResults, tutuResults));
            
            log.info("Найдено {} предложений", results.size());
            
        } catch (Exception e) {
            log.error("Ошибка при поиске рейсов: {}", e.getMessage());
        }
        return results;
    }
    
    private List<Map<String, Object>> parseYandexTravel(String from, String to, String date) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String url = String.format(
                "https://travel.yandex.ru/flights/?adult_seats=1&children_seats=0&infant_seats=0&from=%s&to=%s&when=%s",
                from, to, date
            );
            
            log.debug("Загрузка страницы Яндекс.Путешествия: {}", url);
            driver.get(url);
            
            // Увеличиваем время ожидания и добавляем явную паузу
            wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            Thread.sleep(5000);
            
            // Ждем загрузки результатов (обновленные селекторы)
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".avia-card")));
            
            // Собираем данные о рейсах
            List<WebElement> cards = driver.findElements(By.cssSelector(".avia-card"));
            for (WebElement card : cards) {
                try {
                    Map<String, Object> flight = new HashMap<>();
                    
                    // Цена
                    WebElement priceElement = card.findElement(By.cssSelector(".price__value"));
                    String price = priceElement.getText().replaceAll("[^0-9]", "");
                    flight.put("price", price);
                    
                    // Авиакомпания
                    WebElement airlineElement = card.findElement(By.cssSelector(".flight-segment__airline"));
                    flight.put("airline", airlineElement.getText().trim());
                    
                    // Время вылета и прилета
                    WebElement departureElement = card.findElement(By.cssSelector(".flight-segment__departure .flight-segment__time"));
                    WebElement arrivalElement = card.findElement(By.cssSelector(".flight-segment__arrival .flight-segment__time"));
                    flight.put("departure_time", departureElement.getText().trim());
                    flight.put("arrival_time", arrivalElement.getText().trim());
                    
                    flight.put("source", "Яндекс.Путешествия");
                    results.add(flight);
                    
                    log.debug("Найден рейс: {}", flight);
                } catch (Exception e) {
                    log.warn("Ошибка при парсинге карточки рейса: {}", e.getMessage());
                }
            }
            
            log.info("Найдено {} рейсов на Яндекс.Путешествия", results.size());
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге Яндекс.Путешествия: {}", e.getMessage());
        }
        return results;
    }
    
    private List<Map<String, Object>> parseAvito(String from, String to, String date) {
        // Временно отключаем Авито из-за сложностей с парсингом
        return new ArrayList<>();
    }
    
    private List<Map<String, Object>> parseTutu(String from, String to, String date) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            // Изменяем формат URL и добавляем параметры
            String formattedDate = date.replace("-", ".");
            String url = String.format(
                "https://www.tutu.ru/aviabilety/?date=%s&route_from=%s&route_to=%s",
                formattedDate, from, to
            );
            
            driver.get(url);
            Thread.sleep(3000); // Даем время на загрузку
            
            // Обновленные селекторы
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".SearchResults__list")));
            
            List<WebElement> tickets = driver.findElements(By.cssSelector(".SearchResults__item"));
            for (WebElement ticket : tickets) {
                try {
                    Map<String, Object> flight = new HashMap<>();
                    
                    String price = ticket.findElement(By.cssSelector(".Price__value")).getText().replaceAll("[^0-9]", "");
                    String airline = ticket.findElement(By.cssSelector(".Carrier__name")).getText();
                    String departureTime = ticket.findElement(By.cssSelector(".FlightTime__time")).getText();
                    
                    flight.put("price", price);
                    flight.put("airline", airline);
                    flight.put("departure_time", departureTime);
                    flight.put("source", "Туту.ру");
                    
                    results.add(flight);
                    log.debug("Найден рейс на Туту.ру: {}", flight);
                } catch (Exception e) {
                    log.warn("Ошибка при парсинге билета Туту.ру: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге Туту.ру: {}", e.getMessage());
        }
        return results;
    }
    
    private List<Map<String, Object>> mergeResults(List<Map<String, Object>>... resultLists) {
        List<Map<String, Object>> mergedResults = new ArrayList<>();
        Set<String> processedFlights = new HashSet<>();
        
        for (List<Map<String, Object>> resultList : resultLists) {
            for (Map<String, Object> flight : resultList) {
                String price = String.valueOf(flight.get("price"));
                String key = (flight.get("airline") != null ? flight.get("airline") : "") + "_" + price;
                
                if (!processedFlights.contains(key)) {
                    mergedResults.add(flight);
                    processedFlights.add(key);
                }
            }
        }
        
        // Сортируем по цене
        mergedResults.sort((a, b) -> {
            String priceA = String.valueOf(a.get("price")).replaceAll("[^0-9]", "");
            String priceB = String.valueOf(b.get("price")).replaceAll("[^0-9]", "");
            return Integer.compare(
                priceA.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(priceA),
                priceB.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(priceB)
            );
        });
        
        return mergedResults;
    }
} 