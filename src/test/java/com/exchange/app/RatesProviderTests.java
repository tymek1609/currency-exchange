package com.exchange.app;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

class RatesProviderTests {

    private static final String SEK = "SEK";
    private static final String USD = "USD";
    private static final String EUR = "EUR";

    private Map<String, Double> rates;

    @BeforeEach
    void setUp() {
        rates = new HashMap<String, Double>() {
        };
    }

    @Test
    @DisplayName("For default currency (EUR) returns USD rate")
    void test1() {

        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        ExchangeRates exchangeRates = initializeExchangeRates();
        Mockito.when(apiClient.getLatestRates()).thenReturn(exchangeRates);

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        Double rateUSD = provider.getExchangeRateInEUR(Currency.getInstance(USD));

        //then
        assertThat(exchangeRates.get(USD)).isEqualTo(rateUSD);
    }

    @Test
    @DisplayName("For default currency (EUR) returns all rates")
    void test2() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        ExchangeRates exchangeRates = initializeExchangeRates();
        Mockito.when(apiClient.getLatestRates()).thenReturn(exchangeRates);

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        Double rateSEK = provider.getExchangeRateInEUR(Currency.getInstance(SEK));
        Double rateUSD = provider.getExchangeRateInEUR(Currency.getInstance(USD));

        //then
        assertAll(
                () -> assertEquals(exchangeRates.get(USD), rateUSD, "USD rate should be included"),
                () -> assertEquals(exchangeRates.get(SEK), rateSEK, "SEK rate should be included")
        );
    }

    @Test
    void shouldReturnCurrencyExchangeRatesForOtherCurrency() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        ExchangeRates exchangeRates = initializeExchangeRates();
        List<String> currencies = Arrays.asList(new String[]{EUR, SEK, USD});

        Mockito.when(apiClient.getLatestRates(anyString())).thenAnswer(
                new Answer<ExchangeRates>() {

                    @Override
                    public ExchangeRates answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object base = invocationOnMock.getArgument(0);
                        if (currencies.contains(base)) {
                            return exchangeRates;
                        } else {
                            throw new CurrencyNotSupportedException("Not supported: " + base);
                        }
                    }
                }
        );

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        Double rate = provider.getExchangeRate(Currency.getInstance(SEK), Currency.getInstance("CAD"));

        //then
        assertThat(10.30).isEqualTo(rate);
    }

    @Test
    void shouldThrowExceptionWhenCurrencyNotSupported() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        Mockito.when(apiClient.getLatestRates()).thenThrow(new IllegalArgumentException());

        RatesProvider provider = new RatesProvider(apiClient);

        //then

        CurrencyNotSupportedException actual =
                assertThrows(CurrencyNotSupportedException.class,
                        () -> provider.getExchangeRateInEUR(Currency.getInstance("CHF")));

        assertEquals("Currency is not supported: CHF", actual.getMessage());
    }

    @Test
    void shouldGetRatesOnlyOnce() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        ExchangeRates exchangeRates = initializeExchangeRates();
        Mockito.when(apiClient.getLatestRates()).thenReturn(exchangeRates);

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        provider.getExchangeRateInEUR(Currency.getInstance(SEK));

        //then
        Mockito.verify(apiClient).getLatestRates();
    }

    @Test
    @DisplayName("Should return list of latest rates of currency symbols")
    void shouldReturnRatesOfSendedSymbolsOfCurrenciesOnly() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        List<ExchangeRates> exchangeRates = initializeLatestRatesForCurrencies();
        List<String> symbolList = new ArrayList<String>() {{
            add(SEK);
            add(USD);
        }};
        Mockito.when(apiClient.getLatestRatesForCurrencies(symbolList)).thenReturn(exchangeRates);

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        List<ExchangeRates> listOfLatestRates = provider.getLatestExchangeRatesForCurrencies(symbolList);
        Set<String> symbolParamsSet = new HashSet<>(symbolList);
        Set<String> currencySymbolsOfLatestRates = new HashSet<>(listOfLatestRates.stream()
                .map(el->el.getBase())
                .collect(Collectors.toList()));
        //then
        assertThat( symbolParamsSet.size() == 2);
        assertThat( currencySymbolsOfLatestRates.containsAll(symbolParamsSet));
    }

    private ExchangeRates initializeExchangeRates() {
        rates.put(USD, 1.22);
        rates.put(SEK, 10.30);
        return initializeExchangeRates(EUR, DateTime.now(), rates);
    }

    private ExchangeRates initializeExchangeRates(String base) {
        rates.put(EUR, 1.22);
        rates.put(SEK, 10.30);
        return initializeExchangeRates(base, DateTime.now(), rates);
    }

    private List<ExchangeRates> initializeLatestRatesForCurrencies() {
        List<ExchangeRates> exchangeRatesList = new ArrayList<ExchangeRates>() {{
            add(new RatesForCurrencyForDayBuilder().basedUSD()
                    .forDay(DateTime.now())
                    .addRate(EUR, 0.84)
                    .addRate(SEK, 12.34)
                    .build());
            add(new RatesForCurrencyForDayBuilder().basedSEK()
                    .forDay(DateTime.now())
                    .addRate(USD, 0.12)
                    .addRate(EUR, 0.09)
                    .build());
            add(new RatesForCurrencyForDayBuilder().basedEUR()
                    .forDay(DateTime.now())
                    .addRate(USD, 1.12)
                    .addRate(SEK, 14.09)
                    .build());
        }};
        return exchangeRatesList;
    }

    private ExchangeRates initializeExchangeRates(String base, DateTime date, Map<String, Double> rates) {
        return new ExchangeRates(base, date, rates);
    }

    private class RatesForCurrencyForDayBuilder {

        private String currency;
        private Map<String, Double> rates;
        private DateTime date;

        public RatesForCurrencyForDayBuilder basedUSD() {
            currency = USD;
            return this;
        }

        public RatesForCurrencyForDayBuilder basedSEK() {
            currency = SEK;
            return this;
        }

        public RatesForCurrencyForDayBuilder basedEUR() {
            currency = SEK;
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
}