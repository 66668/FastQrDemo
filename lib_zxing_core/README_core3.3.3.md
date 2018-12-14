# zxing源码
使用 core3.3.3的源码，修改了生成二维码的白边框
具体位置为：com.google.zxing.qrcode.QRCodeWriter,

源代码：

      // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
       // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
       private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
         ByteMatrix input = code.getMatrix();
         if (input == null) {
           throw new IllegalStateException();
         }
         int inputWidth = input.getWidth();
         int inputHeight = input.getHeight();
         int qrWidth = inputWidth + (quietZone * 2);
         int qrHeight = inputHeight + (quietZone * 2);
         int outputWidth = Math.max(width, qrWidth);
         int outputHeight = Math.max(height, qrHeight);
     
         int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
         // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
         // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
         // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
         // handle all the padding from 100x100 (the actual QR) up to 200x160.
         int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
         int topPadding = (outputHeight - (inputHeight * multiple)) / 2;
     
         BitMatrix output = new BitMatrix(outputWidth, outputHeight);
     
         for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
           // Write the contents of this row of the barcode
           for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
             if (input.get(inputX, inputY) == 1) {
               output.setRegion(outputX, outputY, multiple, multiple);
             }
           }
         }
     
         return output;
       }

修改后代码：
    
     //TODO 修改源代码 去除白边 /已修改 sjy
        // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
        // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
        private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
            ByteMatrix input = code.getMatrix();
            if (input == null) {
                throw new IllegalStateException();
            }
            int inputWidth = input.getWidth();
            int inputHeight = input.getHeight();
            //----------------------------开始：修改位置--------------------------------------
    //        int qrWidth = inputWidth + (quietZone * 2);
    //        int qrHeight = inputHeight + (quietZone * 2);
    
            //修改--》去掉间距
            int qrWidth = inputWidth;
            int qrHeight = inputHeight;
            //----------------------------结束：修改位置--------------------------------------
            int outputWidth = Math.max(width, qrWidth);
            int outputHeight = Math.max(height, qrHeight);
    
            int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
            // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
            // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
            // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
            // handle all the padding from 100x100 (the actual QR) up to 200x160.
            int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
            int topPadding = (outputHeight - (inputHeight * multiple)) / 2;
    
            BitMatrix output = new BitMatrix(outputWidth, outputHeight);
    
            for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
                // Write the contents of this row of the barcode
                for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
                    if (input.get(inputX, inputY) == 1) {
                        output.setRegion(outputX, outputY, multiple, multiple);
                    }
                }
            }
    
            return output;
        }
