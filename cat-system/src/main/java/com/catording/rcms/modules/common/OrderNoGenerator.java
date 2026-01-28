package com.catording.rcms.modules.common;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNoGenerator {
    private static final DateTimeFormatter F = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private LocalDate lastDate = LocalDate.now();
    private final AtomicInteger seq = new AtomicInteger(0);

    public synchronized String next() {
        LocalDate now = LocalDate.now();
        if (!now.equals(lastDate)) {
            lastDate = now;
            seq.set(0);
        }
        int n = seq.incrementAndGet();
        return "RCMS" + now.format(F) + "-" + String.format("%04d", n);
    }
}

