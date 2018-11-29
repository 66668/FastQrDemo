package lib.ruijia.zxing.scan.camera;


import lib.ruijia.zxing.scan.SourceData;

/**
 * Callback for camera previews.
 */
public interface PreviewCallback {
    void onPreview(SourceData sourceData);
    void onPreviewError(Exception e);
}
