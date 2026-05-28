package io.github.tulsirathod.joltindiadata;

import io.github.tulsirathod.joltindiadata.hsn.HSNLookup;
import io.github.tulsirathod.joltindiadata.ifsc.IFSCLookup;
import io.github.tulsirathod.joltindiadata.pincode.PincodeLookup;
import io.github.tulsirathod.joltindiadata.rto.RTOLookup;
import io.github.tulsirathod.joltindiadata.stategst.StateGstLookup;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Top-level entry point for {@code jolt-india-data}.
 *
 * <p>Construction is cheap. Dataset providers are discovered via
 * {@link ServiceLoader} on first access to the corresponding accessor
 * ({@link #ifsc()}, {@link #pincode()}, etc.); the actual binary data is
 * loaded lazily by the provider on first {@code lookup()}.
 *
 * <p>If no provider is registered for a dataset (i.e. its implementation
 * has not yet been built), the corresponding accessor throws
 * {@link IllegalStateException}. There is no silent fallback to mock data.
 *
 * <p>This class holds no global static state — callers should hold the
 * instance themselves (DI container, application singleton). Multiple
 * instances are legal but waste memory, since each will load its own
 * data buffers.
 */
public final class IndiaData {

    private volatile IFSCLookup ifsc;
    private volatile PincodeLookup pincode;
    private volatile HSNLookup hsn;
    private volatile RTOLookup rto;
    private volatile StateGstLookup stateGst;

    private IndiaData() {}

    public static IndiaData create() {
        return new IndiaData();
    }

    public IFSCLookup ifsc() {
        IFSCLookup v = ifsc;
        if (v != null) return v;
        synchronized (this) {
            if (ifsc == null) ifsc = loadOrThrow(IFSCLookup.class);
            return ifsc;
        }
    }

    public PincodeLookup pincode() {
        PincodeLookup v = pincode;
        if (v != null) return v;
        synchronized (this) {
            if (pincode == null) pincode = loadOrThrow(PincodeLookup.class);
            return pincode;
        }
    }

    public HSNLookup hsn() {
        HSNLookup v = hsn;
        if (v != null) return v;
        synchronized (this) {
            if (hsn == null) hsn = loadOrThrow(HSNLookup.class);
            return hsn;
        }
    }

    public RTOLookup rto() {
        RTOLookup v = rto;
        if (v != null) return v;
        synchronized (this) {
            if (rto == null) rto = loadOrThrow(RTOLookup.class);
            return rto;
        }
    }

    public StateGstLookup stateGst() {
        StateGstLookup v = stateGst;
        if (v != null) return v;
        synchronized (this) {
            if (stateGst == null) stateGst = loadOrThrow(StateGstLookup.class);
            return stateGst;
        }
    }

    private static <T> T loadOrThrow(Class<T> spi) {
        Iterator<T> it = ServiceLoader.load(spi).iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException(
                    "No " + spi.getSimpleName() + " provider registered. " +
                            "This dataset implementation has not yet been built.");
        }
        return it.next();
    }
}
