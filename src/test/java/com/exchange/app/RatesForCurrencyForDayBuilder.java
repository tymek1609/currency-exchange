package com.exchange.app;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * [Opis klasy]
 * Klasa utworzona przez: michal.tyminski
 * Data utworzenia: 05.03.2020
 */
class RatesForCurrencyForDayBuilder {

    private String currency;
    private Map<String, Double> rates;
    private DateTime date;

    public RatesForCurrencyForDayBuilder basedUSD() {
        currency = CurrencyStaticFields.USD;
        return this;
    }

    public RatesForCurrencyForDayBuilder basedSEK() {
        currency = CurrencyStaticFields.SEK;
        return this;
    }

    public RatesForCurrencyForDayBuilder basedEUR() {
        currency = CurrencyStaticFields.EUR;
        return this;
    }

    public RatesForCurrencyForDayBuilder based(String currency) {
        this.currency = currency;
        return this;
    }

    public RatesForCurrencyForDayBuilder addRate(String foreignCurrency, Double rate) {
        if (rates == null) rates = new HashMap<>();
        if (currency != null && !currency.equals(foreignCurrency))
            rates.put(foreignCurrency, rate);
        return this;
    }

    /**
     * diff from today
     *
     * @param day
     * @return
     */
    public RatesForCurrencyForDayBuilder forDay(int day) {
        DateTime dateTime = DateTime.now();
        if (day > 0) dateTime = dateTime.plusDays(day);
        if (day < 0) dateTime = dateTime.minusDays(-day);
        date = dateTime;
        return this;
    }

    public RatesForCurrencyForDayBuilder forDay(DateTime date) {
        this.date = date;
        return this;
    }

    public ExchangeRates build() {
        if (date == null) this.date = DateTime.now();
        return new ExchangeRates(currency, date, rates);
    }

}
