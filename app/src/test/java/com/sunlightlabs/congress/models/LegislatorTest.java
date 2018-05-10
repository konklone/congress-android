package com.sunlightlabs.congress.models;

import org.junit.Test;

import static org.junit.Assert.*;

public class LegislatorTest {

    @Test
    public void shortTitle() {
        
        assertEquals(Legislator.shortTitle("Representative"), "Rep");
        assertEquals(Legislator.shortTitle("Senator"), "Sen");
        assertEquals(Legislator.shortTitle("Delegate"), "Del");
        assertEquals(Legislator.shortTitle("Resident Commissioner"), "Com");
        assertEquals(Legislator.shortTitle(""), "");
        assertEquals(Legislator.shortTitle("Justice"), "");

    }

}