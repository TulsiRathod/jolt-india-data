package io.github.tulsirathod.joltindiadata.internal;

import io.github.tulsirathod.joltindiadata.stategst.StateGstLookup;
import io.github.tulsirathod.joltindiadata.stategst.StateInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default GST state-code provider.
 *
 * <p>Unlike the other datasets, the GST state code list is small (under 40
 * entries) and changes only when the Constitution does. Bundling a binary
 * file and dictionary indirection here would be overhead with no payoff,
 * so the table is hardcoded. Source: GSTN public state-code list, current
 * as of the Dadra and Nagar Haveli / Daman and Diu merger (2020-01-26).
 *
 * <p>Legacy codes 25 (Daman and Diu, pre-merger) and 28 (Andhra Pradesh,
 * pre-bifurcation) remain queryable because legacy GSTINs containing them
 * still circulate.
 */
public final class StateGstLookupImpl implements StateGstLookup {

    private static final Map<String, StateInfo> TABLE = build();

    @Override
    public Optional<StateInfo> lookup(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(TABLE.get(code));
    }

    private static Map<String, StateInfo> build() {
        Map<String, StateInfo> m = new HashMap<>();
        add(m, "01", "Jammu and Kashmir", false);
        add(m, "02", "Himachal Pradesh", false);
        add(m, "03", "Punjab", false);
        add(m, "04", "Chandigarh", true);
        add(m, "05", "Uttarakhand", false);
        add(m, "06", "Haryana", false);
        add(m, "07", "Delhi", true);
        add(m, "08", "Rajasthan", false);
        add(m, "09", "Uttar Pradesh", false);
        add(m, "10", "Bihar", false);
        add(m, "11", "Sikkim", false);
        add(m, "12", "Arunachal Pradesh", false);
        add(m, "13", "Nagaland", false);
        add(m, "14", "Manipur", false);
        add(m, "15", "Mizoram", false);
        add(m, "16", "Tripura", false);
        add(m, "17", "Meghalaya", false);
        add(m, "18", "Assam", false);
        add(m, "19", "West Bengal", false);
        add(m, "20", "Jharkhand", false);
        add(m, "21", "Odisha", false);
        add(m, "22", "Chhattisgarh", false);
        add(m, "23", "Madhya Pradesh", false);
        add(m, "24", "Gujarat", false);
        add(m, "25", "Daman and Diu", true); // legacy pre-2020 merger
        add(m, "26", "Dadra and Nagar Haveli and Daman and Diu", true);
        add(m, "27", "Maharashtra", false);
        add(m, "28", "Andhra Pradesh (pre-bifurcation)", false); // legacy
        add(m, "29", "Karnataka", false);
        add(m, "30", "Goa", false);
        add(m, "31", "Lakshadweep", true);
        add(m, "32", "Kerala", false);
        add(m, "33", "Tamil Nadu", false);
        add(m, "34", "Puducherry", true);
        add(m, "35", "Andaman and Nicobar Islands", true);
        add(m, "36", "Telangana", false);
        add(m, "37", "Andhra Pradesh", false);
        add(m, "38", "Ladakh", true);
        add(m, "97", "Other Territory", true);
        add(m, "99", "Centre Jurisdiction", true);
        return Map.copyOf(m);
    }

    private static void add(Map<String, StateInfo> m, String code, String name, boolean isUT) {
        m.put(code, new StateInfo(code, name, isUT));
    }
}
