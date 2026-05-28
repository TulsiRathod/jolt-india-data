package io.github.tulsirathod.joltindiadata;

import io.github.tulsirathod.joltindiadata.ifsc.IFSCInfo;
import io.github.tulsirathod.joltindiadata.pincode.PincodeInfo;
import io.github.tulsirathod.joltindiadata.rto.RTOInfo;
import io.github.tulsirathod.joltindiadata.stategst.StateInfo;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndiaDataTest {

    @Test
    void providersAreDiscoverable() {
        IndiaData data = IndiaData.create();
        assertNotNull(data.ifsc());
        assertNotNull(data.pincode());
        assertNotNull(data.hsn());
        assertNotNull(data.rto());
        assertNotNull(data.stateGst());
    }

    @Test
    void providersAreCached() {
        IndiaData data = IndiaData.create();
        assertSame(data.ifsc(), data.ifsc(), "ifsc() should return cached instance");
        assertSame(data.stateGst(), data.stateGst(), "stateGst() should return cached instance");
    }

    @Test
    void sampleIfscLookupReturnsKnownBank() {
        IndiaData data = IndiaData.create();
        Optional<IFSCInfo> bank = data.ifsc().lookup("HDFC0000001");
        assertTrue(bank.isPresent());
        assertEquals("HDFC BANK", bank.get().bank());
        assertEquals("Mumbai", bank.get().city());
        assertTrue(bank.get().rtgs() && bank.get().neft() && bank.get().imps());
    }

    @Test
    void unknownIfscReturnsEmpty() {
        IndiaData data = IndiaData.create();
        assertFalse(data.ifsc().lookup("ZZZZ0000000").isPresent());
    }

    @Test
    void samplePincodeLookup() {
        IndiaData data = IndiaData.create();
        Optional<PincodeInfo> p = data.pincode().lookup("395001");
        assertTrue(p.isPresent());
        assertEquals("Surat", p.get().city());
        assertEquals("Gujarat", p.get().state());
        assertEquals("Western", p.get().region());
    }

    @Test
    void hsnMultiRateReturnsAllVariants() {
        IndiaData data = IndiaData.create();
        var rates = data.hsn().lookup("6109");
        assertEquals(2, rates.size(), "6109 should have two rate variants in the sample");
        assertTrue(rates.stream().anyMatch(r -> r.rate() == 5.0));
        assertTrue(rates.stream().anyMatch(r -> r.rate() == 12.0));
    }

    @Test
    void unknownHsnReturnsEmptyList() {
        IndiaData data = IndiaData.create();
        assertTrue(data.hsn().lookup("9999").isEmpty());
    }

    @Test
    void sampleRtoLookup() {
        IndiaData data = IndiaData.create();
        Optional<RTOInfo> rto = data.rto().lookup("GJ05");
        assertTrue(rto.isPresent());
        assertEquals("Surat", rto.get().city());
        assertEquals("Gujarat", rto.get().state());
    }

    @Test
    void stateGstCovers38PlusCodes() {
        IndiaData data = IndiaData.create();
        Optional<StateInfo> gujarat = data.stateGst().lookup("24");
        assertTrue(gujarat.isPresent());
        assertEquals("Gujarat", gujarat.get().name());
        assertFalse(gujarat.get().isUT());

        Optional<StateInfo> chandigarh = data.stateGst().lookup("04");
        assertTrue(chandigarh.isPresent());
        assertTrue(chandigarh.get().isUT());
    }

    @Test
    void nullKeyReturnsEmptyEverywhere() {
        IndiaData data = IndiaData.create();
        assertTrue(data.ifsc().lookup(null).isEmpty());
        assertTrue(data.pincode().lookup(null).isEmpty());
        assertTrue(data.hsn().lookup(null).isEmpty());
        assertTrue(data.rto().lookup(null).isEmpty());
        assertTrue(data.stateGst().lookup(null).isEmpty());
    }
}
