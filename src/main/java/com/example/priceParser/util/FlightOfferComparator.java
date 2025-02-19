package com.example.priceParser.util;

import com.example.priceParser.model.FlightOffer;
import java.util.Comparator;

public class FlightOfferComparator implements Comparator<FlightOffer> {
    @Override
    public int compare(FlightOffer o1, FlightOffer o2) {
        // Сначала сравниваем по цене
        int priceCompare = o1.getPrice().getGrandTotal().compareTo(o2.getPrice().getGrandTotal());
        
        if (priceCompare != 0) {
            return priceCompare;
        }
        
        // Если цены равны, сравниваем по длительности
        String duration1 = o1.getItineraries().get(0).getDuration();
        String duration2 = o2.getItineraries().get(0).getDuration();
        
        // Преобразуем строки длительности (PT2H30M) в минуты
        int minutes1 = parseDuration(duration1);
        int minutes2 = parseDuration(duration2);
        
        return Integer.compare(minutes1, minutes2);
    }
    
    private int parseDuration(String duration) {
        // Убираем PT в начале строки
        duration = duration.substring(2);
        int minutes = 0;
        
        // Находим часы
        int hIndex = duration.indexOf('H');
        if (hIndex > 0) {
            minutes += Integer.parseInt(duration.substring(0, hIndex)) * 60;
            duration = duration.substring(hIndex + 1);
        }
        
        // Находим минуты
        int mIndex = duration.indexOf('M');
        if (mIndex > 0) {
            minutes += Integer.parseInt(duration.substring(0, mIndex));
        }
        
        return minutes;
    }
    
    public int parseFlightDuration(String duration) {
        return parseDuration(duration);
    }
} 