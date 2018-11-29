package lib.ruijia.zbar.qrodecontinue;

import android.graphics.PointF;

/**
 * 作者:王浩
 * 创建时间:2018/6/15
 * 描述:
 */
public class ContinueScanResult {
    String result;
    PointF[] resultPoints;

    public ContinueScanResult(String result) {
        this.result = result;
    }

    public ContinueScanResult(String result, PointF[] resultPoints) {
        this.result = result;
        this.resultPoints = resultPoints;
    }
}
