package com.tradeoption.service.impl;

import com.tradeoption.domain.ConePoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VolatilityConeServiceTest {

    private final VolatilityConeServiceImpl service = new VolatilityConeServiceImpl();

    @Test
    public void testGenerateCone() {
        double spot = 100;
        double vol = 0.2;
        double horizon = 1.0;
        int steps = 10;

        List<ConePoint> cone = service.generateCone(spot, vol, horizon, steps);

        assertEquals(11, cone.size()); // 0 to 10

        // Check t=0
        ConePoint p0 = cone.get(0);
        assertEquals(0, p0.getTimeInYears());
        assertEquals(100, p0.getUpper1Sigma(), 0.001);
        assertEquals(100, p0.getLower1Sigma(), 0.001);

        // Check last point t=1
        ConePoint pLast = cone.get(10);
        assertEquals(1.0, pLast.getTimeInYears(), 0.001);

        // 1-sigma move = exp(0.2 * sqrt(1)) = exp(0.2) ~ 1.2214
        // Upper1 = 100 * 1.2214 = 122.14
        // Lower1 = 100 * exp(-0.2) ~ 81.87

        assertEquals(100 * Math.exp(0.2), pLast.getUpper1Sigma(), 0.001);
        assertEquals(100 * Math.exp(-0.2), pLast.getLower1Sigma(), 0.001);

        // Check 2-sigma wider than 1-sigma
        assertTrue(pLast.getUpper2Sigma() > pLast.getUpper1Sigma());
        assertTrue(pLast.getLower2Sigma() < pLast.getLower1Sigma());
    }
}
