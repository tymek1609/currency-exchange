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
        Double rateUSD = provider.getExchangeRateInEUR(Currency.getInstance(CurrencyStaticFields.USD));

        //then
        assertThat(exchangeRates.get(CurrencyStaticFields.USD)).isEqualTo(rateUSD);
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
        Double rateSEK = provider.getExchangeRateInEUR(Currency.getInstance(CurrencyStaticFields.SEK));
        Double rateUSD = provider.getExchangeRateInEUR(Currency.getInstance(CurrencyStaticFields.USD));

        //then
        assertAll(
                () -> assertEquals(exchangeRates.get(CurrencyStaticFields.USD), rateUSD, "USD rate should be included"),
                () -> assertEquals(exchangeRates.get(CurrencyStaticFields.SEK), rateSEK, "SEK rate should be included")
        );
    }

    @Test
    void shouldReturnCurrencyExchangeRatesForOtherCurrency() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        ExchangeRates exchangeRates = initializeExchangeRates();
        List<String> currencies = Arrays.asList(CurrencyStaticFields.EUR,
                CurrencyStaticFields.SEK,
                CurrencyStaticFields.USD);

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
        Double rate = provider.getExchangeRate(Currency.getInstance(CurrencyStaticFields.SEK), Currency.getInstance(CurrencyStaticFields.EUR));

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
        provider.getExchangeRateInEUR(Currency.getInstance(CurrencyStaticFields.SEK));

        //then
        Mockito.verify(apiClient).getLatestRates();
    }

    @Test
    @DisplayName("Should return list of latest rates for given currency symbols")
    void shouldReturnTheLatestRatesForGivenSymbolsCurrencyOnly() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        DateTime dateTimeNow = DateTime.now();
        List<ExchangeRates> exchangeRates = initializeLatestRatesForCurrencies(dateTimeNow);
        List<String> symbolList = new ArrayList<String>() {{
            add(CurrencyStaticFields.SEK);
            add(CurrencyStaticFields.USD);
        }};
        Mockito.when(apiClient.getLatestRatesForCurrencies(symbolList)).thenReturn(exchangeRates);

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        List<ExchangeRates> listOfLatestRates = provider.getLatestExchangeRatesForCurrencies(symbolList);

        //then
        assertThat(listOfLatestRates.size() == 2);
        assertThat(listOfLatestRates.stream()
                .filter(el-> CurrencyStaticFields.EUR.equals(el.getBase()))
                .count() == 1L);
        assertThat(listOfLatestRates.stream()
                .filter(el-> CurrencyStaticFields.SEK.equals(el.getBase()))
                .count() == 1L);
        assertThat(listOfLatestRates.stream()
                .filter(el -> CurrencyStaticFields.EUR.equals(el.getBase()) && dateTimeNow.equals(el.getDate())));
    }

    @Test
    @DisplayName("Should return list of the latest rates for given currency symbols - Vanilla Java Stub")
    void shouldReturnTheLatestRatesForGivenCurrencySymbolsOnlyVanillaJavaStub(){
        //given
        ForeignExchangeRatesApiClient apiClient = new MyApiClientStub();
        List<String> symbolList = new ArrayList<String>() {{
            add(CurrencyStaticFields.SEK);
            add(CurrencyStaticFields.USD);
        }};

        RatesProvider provider = new RatesProvider(apiClient);
        DateTime dateTimeNow = DateTime.now();

        //when
        List<ExchangeRates> listOfLatestRates = provider.getLatestExchangeRatesForCurrencies(symbolList);
        //then
        assertThat(listOfLatestRates.size() == 2);
        assertThat(listOfLatestRates.stream()
                .filter(el-> CurrencyStaticFields.EUR.equals(el.getBase()))
                .count() == 1L);
        assertThat(listOfLatestRates.stream()
                .filter(el-> CurrencyStaticFields.SEK.equals(el.getBase()))
                .count() == 1L);
        assertThat(listOfLatestRates.stream()
                .filter(el -> CurrencyStaticFields.EUR.equals(el.getBase()) && dateTimeNow.equals(el.getDate())));
    }

    @Test
    @DisplayName("Should return list of the latest rates for given currency symbols - Vanilla Java Mock")
    void shouldReturnTheLatestRatesForGivenCurrencySymbolsOnlyVanillaJavaMock(){
        //given
        ForeignExchangeRatesApiClient apiClient = new MyApiClientMock();
        List<String> symbolList = new ArrayList<String>() {{
            add(CurrencyStaticFields.SEK);
            add(CurrencyStaticFields.CAD);
        }};

        RatesProvider provider = new RatesProvider(apiClient);
        DateTime dateTimeNow = DateTime.now();

        //when
        List<ExchangeRates> listOfLatestRates = provider.getLatestExchangeRatesForCurrencies(symbolList);
        //then
        assertThat(listOfLatestRates.size() == 2);
        assertThat(listOfLatestRates.stream()
                .filter(el-> CurrencyStaticFields.EUR.equals(el.getBase()))
                .count() == 1L);
        assertThat(listOfLatestRates.stream()
                .filter(el-> CurrencyStaticFields.SEK.equals(el.getBase()))
                .count() == 1L);
        assertThat(listOfLatestRates.stream()
                .filter(el -> CurrencyStaticFields.EUR.equals(el.getBase()) && dateTimeNow.equals(el.getDate())));
    }

    private ExchangeRates initializeExchangeRates() {
        rates.put(CurrencyStaticFields.USD, 1.22);
        rates.put(CurrencyStaticFields.SEK, 10.30);
        return initializeExchangeRates(CurrencyStaticFields.EUR, DateTime.now(), rates);
    }

    private ExchangeRates initializeExchangeRates(String base) {
        rates.put(CurrencyStaticFields.EUR, 1.22);
        rates.put(CurrencyStaticFields.SEK, 10.30);
        return initializeExchangeRates(base, DateTime.now(), rates);
    }

    private List<ExchangeRates> initializeLatestRatesForCurrencies(DateTime dateTimeNow) {
        List<ExchangeRates> exchangeRatesList = new ArrayList<ExchangeRates>() {{
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
        return exchangeRatesList;
    }

    private ExchangeRates initializeExchangeRates(String base, DateTime date, Map<String, Double> rates) {
        return new ExchangeRates(base, date, rates);
    }

}