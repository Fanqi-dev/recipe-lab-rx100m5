package com.voxivoid.recipelab;
import java.util.Map;
import static com.voxivoid.recipelab.Params.*;

/** Conservative camera-specific UI policy, separated for host-side regression tests. */
final class Rx100M5Policy {
    static final int CONFIRM_DEFAULT = 1; // cancel, not apply
    static boolean effectAllowed(int[] rows) { return rows[R_PE] == 0 || rows[R_QUAL] > Q_RAWJPG; }
    static Map<String,String> preview(int[] rows, String originalWhiteBalance) {
        Map<String,String> values = Params.preview(rows);
        values.remove("storage-fmt"); values.remove("jpeg-quality");
        if (rows[R_WBMODE] != WB_AUTO && rows[R_WBMODE] != WB_KELVIN) {
            if (originalWhiteBalance == null || originalWhiteBalance.length() == 0) throw new IllegalStateException("Cannot preserve preset white balance");
            values.put("whitebalance",originalWhiteBalance);
        }
        return values;
    }
}
