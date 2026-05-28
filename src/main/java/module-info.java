/**
 * jolt-india-data — offline lookup library for Indian banking, postal,
 * tax, and transport codes.
 *
 * <p>Public surface: {@link io.github.tulsirathod.joltindiadata.IndiaData}
 * plus the per-domain lookup interfaces and record types. Everything under
 * {@code internal}, {@code spi}, and {@code builder} is implementation
 * detail and not part of the SemVer contract.
 */
module io.github.tulsirathod.joltindiadata {
    requires java.logging;

    exports io.github.tulsirathod.joltindiadata;
    exports io.github.tulsirathod.joltindiadata.ifsc;
    exports io.github.tulsirathod.joltindiadata.pincode;
    exports io.github.tulsirathod.joltindiadata.hsn;
    exports io.github.tulsirathod.joltindiadata.rto;
    exports io.github.tulsirathod.joltindiadata.stategst;

    uses io.github.tulsirathod.joltindiadata.ifsc.IFSCLookup;
    uses io.github.tulsirathod.joltindiadata.pincode.PincodeLookup;
    uses io.github.tulsirathod.joltindiadata.hsn.HSNLookup;
    uses io.github.tulsirathod.joltindiadata.rto.RTOLookup;
    uses io.github.tulsirathod.joltindiadata.stategst.StateGstLookup;

    provides io.github.tulsirathod.joltindiadata.ifsc.IFSCLookup
            with io.github.tulsirathod.joltindiadata.internal.IFSCLookupImpl;
    provides io.github.tulsirathod.joltindiadata.pincode.PincodeLookup
            with io.github.tulsirathod.joltindiadata.internal.PincodeLookupImpl;
    provides io.github.tulsirathod.joltindiadata.hsn.HSNLookup
            with io.github.tulsirathod.joltindiadata.internal.HSNLookupImpl;
    provides io.github.tulsirathod.joltindiadata.rto.RTOLookup
            with io.github.tulsirathod.joltindiadata.internal.RTOLookupImpl;
    provides io.github.tulsirathod.joltindiadata.stategst.StateGstLookup
            with io.github.tulsirathod.joltindiadata.internal.StateGstLookupImpl;
}
