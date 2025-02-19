package com.example.priceParser.util;

import com.example.priceParser.model.FlightOffer;
import java.util.Comparator;

public class FlightOfferComparator implements Comparator<FlightOffer> {
    
    @Override
    public int compare(FlightOffer o1, FlightOffer o2) {
        double price1 = Double.parseDouble(o1.getPrice().getGrandTotal());
        double price2 = Double.parseDouble(o2.getPrice().getGrandTotal());
        
        int priceCompare = Double.compare(price1, price2);
        
        if (priceCompare == 0) {
            String duration1 = o1.getItineraries().get(0).getDuration();
            String duration2 = o2.getItineraries().get(0).getDuration();
            
            return parseDuration(duration1) - parseDuration(duration2);
        }
        
        return priceCompare;
    }
    
    private int parseDuration(String duration) {
        duration = duration.substring(2);
        int hours = 0;
        int minutes = 0;
        
        int hIndex = duration.indexOf('H');
        if (hIndex > 0) {
            hours = Integer.parseInt(duration.substring(0, hIndex));
            duration = duration.substring(hIndex + 1);
        }
        
        int mIndex = duration.indexOf('M');
        if (mIndex > 0) {
            minutes = Integer.parseInt(duration.substring(0, mIndex));
        }
        
        return hours * 60 + minutes;
    }
    
    public int parseFlightDuration(String duration) {
        return parseDuration(duration);
    }
}