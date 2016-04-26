package com.objectpartners.plummer.stockmarket.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.objectpartners.plummer.stockmarket.domain.QuoteResource;
import com.objectpartners.plummer.stockmarket.graylog.GelfMessage;
import com.objectpartners.plummer.stockmarket.graylog.GraylogRestInterface;
import com.objectpartners.plummer.stockmarket.service.QuoteService;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

@Named
public class QuoteJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteService.class);

    @Inject
    protected ObjectMapper mapper;

    @Inject
    protected GraylogRestInterface graylog;

    @Inject
    protected QuoteService quoteService;

    @Scheduled(initialDelay = 15000L, fixedDelay = 500L)
    public void quoteViaUdpGelf() {
        try {
            String symbol = RandomStringUtils.randomAlphabetic(3).toUpperCase();
            QuoteResource quote = quoteService.quote(symbol);
            LOGGER.info("Generated quote - {}@{}", quote.getSymbol(), quote.getPrice());
        } catch (Exception e) {}
    }

    @Scheduled(initialDelay = 15000L, fixedDelay = 500L)
    public void quoteViaHttpGelf() throws UnknownHostException, JsonProcessingException {
        try {
            Stopwatch timer = Stopwatch.createStarted();
            String symbol = RandomStringUtils.randomAlphabetic(3).toUpperCase();
            QuoteResource quote = quoteService.quote(symbol);

            GelfMessage message = new GelfMessage();
            message.setHost(InetAddress.getLocalHost().getHostName());
            message.setTimestamp(System.currentTimeMillis());
            message.setShortMessage(String.format("Quote %s@%2.2f", quote.getSymbol(), quote.getPrice()));
            message.setFullMessage(mapper.writeValueAsString(quote));
            message.getAdditionalProperties().put("elapsed_time", timer.stop().elapsed(TimeUnit.MICROSECONDS));
            graylog.logEvent(message);
        } catch (Exception e) {}
    }
}
