package com.exchange.app;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Opis klasy]
 * Klasa utworzona przez: michal.tyminski
 * Data utworzenia: 05.03.2020
 */
public class MyApiClientMock implements ForeignExchangeRatesApiClient {

    public static Set<String> SUPPORTED_CURRENCIES = new HashSet<String>(){{
        add(CurrencyStaticFields.SEK);
        add(CurrencyStaticFields.USD);
        add(CurrencyStaticFields.EUR);
    }};

    @Override
    public ExchangeRates getLatestRates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExchangeRates> getLatestRatesForCurrencies(List<String> symbols) {
        assertThat(SUPPORTED_CURRENCIES).containsAll(symbols);
        return initializeLatestRatesForCurrencies();
    }

    @Override
    public ExchangeRates getLatestRates(String base) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExchangeRates getHistoricalRates(DateTime date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExchangeRates> getHistoricalRates(DateTime start_at, DateTime end_at) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExchangeRates> getHistoricalRates(DateTime start_at, DateTime end_at, List<String> symbols) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ExchangeRates> getHistoricalRates(DateTime start_at, DateTime end_at, String base) {
        throw new UnsupportedOperationException();
    }

    private List<ExchangeRates> initializeLatestRatesForCurrencies() {
        List<ExchangeRates> exchangeRatesList = new ArrayList<ExchangeRates>() {{
            add(new RatesForCurrencyForDayBuilder().basedUSD()
                    .forDay(DateTime.now())
                    .addRate(CurrencyStaticFields.EUR, 0.84)
                    .addRate(CurrencyStaticFields.SEK, 12.34)
                    .build());
            add(new RatesForCurrencyForDayBuilder().basedSEK()
                    .forDay(DateTime.now())
                    .addRate(CurrencyStaticFields.USD, 0.12)
                    .addRate(CurrencyStaticFields.EUR, 0.09)
                    .build());
            add(new RatesForCurrencyForDayBuilder().basedEUR()
                    .forDay(DateTime.now())
                    .addRate(CurrencyStaticFields.USD, 1.12)
                    .addRate(CurrencyStaticFields.SEK, 14.09)
                    .build());
        }};
        return exchangeRatesList;
    }
}
