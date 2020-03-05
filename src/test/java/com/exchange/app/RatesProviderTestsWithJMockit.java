package com.exchange.app;

import mockit.Expectations;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class RatesProviderTestsWithJMockit extends MockUp<ForeignExchangeRatesApiClient> {

    private static final String SEK = "SEK";
    private static final String USD = "USD";
    private static final String EUR = "EUR";


    @Test
    void shouldReturnEURRateInUSD(@Mocked ForeignExchangeRatesApiClient apiClient) {
        //given
        Map<String, Double> rates = new HashMap<String, Double>() {};
        rates.put(EUR, 0.8);
        rates.put(SEK, 15.30);

        RatesProvider ratesProvider = new RatesProvider(apiClient);

        new Expectations(){{
            apiClient.getLatestRates(anyString);
            result = new ExchangeRates(USD, DateTime.now(), rates);
            times = 1;
        }};

        //when
        Double rateEUR = ratesProvider.getExchangeRate(Currency.getInstance(EUR), Currency.getInstance(USD));

        //then
        new Verifications(){{
            assertThat(rateEUR).isEqualTo(0.8);
        }};
    }

    @Test
    @DisplayName("Should return list of the latest rates for given currency symbols")
    void shouldReturnTheLatestRatesForGivenCurrencySymbolsOnly(@Mocked ForeignExchangeRatesApiClient apiClient) {
        //given
        List<String> symbolList = new ArrayList<String>() {{
            add(CurrencyStaticFields.EUR);
            add(CurrencyStaticFields.SEK);
        }};
        DateTime dateTimeNow = DateTime.now();

        RatesProvider ratesProvider = new RatesProvider(apiClient);

        new Expectations(){{
            apiClient.getLatestRatesForCurrencies(symbolList);
            result = new ArrayList<ExchangeRates>(){{
                add(new RatesForCurrencyForDayBuilder()
                        .basedEUR()
                        .forDay(dateTimeNow)
                        .addRate(CurrencyStaticFields.USD, 0.79)
                        .build());
                add(new RatesForCurrencyForDayBuilder()
                        .basedEUR()
                        .forDay(-1)
                        .addRate(CurrencyStaticFields.USD, 0.78)
                        .build());
                add(new RatesForCurrencyForDayBuilder()
                        .basedSEK()
                        .forDay(DateTime.now())
                        .addRate(CurrencyStaticFields.USD, 0.12)
                        .build());
            }};
        }};

        //when
        List<ExchangeRates> lastRatesList = ratesProvider.getLatestExchangeRatesForCurrencies(symbolList);

        //then
        new Verifications(){{
            assertThat(lastRatesList.size() == 2);
            assertThat(lastRatesList.stream()
                            .filter(el-> CurrencyStaticFields.EUR.equals(el.getBase()))
                            .count() == 1L);
            assertThat(lastRatesList.stream()
                            .filter(el-> CurrencyStaticFields.SEK.equals(el.getBase()))
                            .count() == 1L);
            assertThat(lastRatesList.stream()
                            .filter(el -> CurrencyStaticFields.EUR.equals(el.getBase()) && dateTimeNow.equals(el.getDate())));
        }};
    }
}
