# QRcode zxing 和zbar极限测试

**测试二维码识别在短时间内传输最大容量是多少/或1s多少KB。**


要求：
1. zxing二维码生成.
2. zxing/zbar二维码识别.
3. 手机前置/后置摄像头识别.
4. 特别重要的一点：原生相机，最好是主流摄像头，摄像头不好，整个识别就别想高速传输了，都是浮云。

测试手机（1080p）：
小米5，一加5T。

测试数据：已在项目名下：str_2900B_26.txt,测试时手动放到手机存储根目录。


## 项目目录说明

### app目录
测试demo
### qrapp目录
链路层的（发送端+接收端）+AIDL服务端（向客户端提供service）
链路层：只负责数据通讯正常，操作结果返回给应用层，告知失败或成功

### 

## lib库说明

### lib_zxing:
1. 使用 https://github.com/journeyapps/zxing-android-embedded 的lib
2. zxing库版本： api 'com.google.zxing:core:3.3.3'

### lib_zbar
1. 使用 https://github.com/bingoogolapple/BGAQRCode-Android 的zbar库及对应camera方法,并优化了连续识别的方式，连续扫描速度提升2~3倍
2. zbar只支持识别，不支持生成。

### lib_filepicker:
1.使用 https://github.com/gzu-liyujiang/AndroidPicker 的FilePicker库

## 识别方式

  1. 开路：只负责发送，或只负责识别。通过标记位决定 数据是否发送或结束。

  2. 闭路：

      (1). 接收端：识别完一条数据，返回二维码告知更新数据，等待识别新数据。
      (2). 发送端：触发按钮，发送二维码数据，发送端等待识别端的二维码（返回 更新数据的通知），接收后再发送新数据
 个

## 优缺点评估：

### 1 开路闭路评估

    开路识别速度特别快，时间控制在发送端的发送间隔，数据成功率控制在接收端(总耗时一般为发送间隔时长*发送次数,一次识别最大理论数据为2952B,实际测试肯定低于该值)
    闭路识别，成功率很高，但速度特别慢，人用前置摄像头，实现互扫，发现非常不理想，聚焦耗时，二维码太大不识别。原因:(1)卡在最长的识别图片上,(2)双向识别耗时多(3)与发送次数有关，毕竟一次闭路循环的最短时间是不会减少的，所以次数越多，速度越慢。

    总结，相同的数据大小，使用开路总体最优，识别失败率不可避免。
    如果设计定制机，最好使用后置相机识别，前置不可用（切记）。

### 2 zbar和zxing评估

    https://github.com/bingoogolapple/BGAQRCode-Android的zbar每次识别，都有个初始化相机的耗时，一般在60～100ms,但是，不管二维码内容大小，识别很稳定，错误率比较低。
    zbar进过改进后，识别速度非常快,碾压zxing。
    zxing的识别速度和二维码内容大小有关，还影响稳定性，内容越大，越会有错误识别。
    综合评定，如果想实现快速连续识别，1s切换近10张二维码图，推荐使用zbar。如下是具体测试。

## 生成二维码库的测试反馈

    zxing最新版本库是core3.3.3，也是最大生成容量的库，其他版本如core3.0.0等，容量更低。生成容量大小和手机版本.手机厂家，质量好坏没关系，只和版本库有关。

## zxing core3.3.3 生成二维码最大容量统计

| ErrorCorrectionLevel参数 | 字母（个） | 汉字（个）|数字（个）|较上一级效率|字节大小（byte）|
| ---------- | ------------- | --------------| --------------| --------------| --------------
| ErrorCorrectionLevel.L    | 2952| 984|7089|1|2952b|
| ErrorCorrectionLevel.M   | 2330| 776|5596|~78.9%|2330b|
| ErrorCorrectionLevel.Q  | 1662| 554|3993|~71.3%|1662b|
|ErrorCorrectionLevel.H | 1272| 424|3057|~76.5%|1272b|


## ErrorCorrectionLevel.L +二维码极限速度(连续扫描)
| 字符串大小（个）| 原版zbar (ms)|zxing(ms)|
| ---------- | ------------- | --------------
|1024|220~250 稳定|普遍60～90，极个别100+～200，有错误率|
|1536|同上，极少300+|普遍60～100,偶尔100～200，个别200+，有错误率|
|2048|同上，很稳定|60~300不稳定，经常卡顿大于300+，极个别1～2s|
|2560|同上，很稳定|普遍60~100较稳定，偶尔100～300，较稳定|
|2952|同上，很稳定|普遍60~200，经常200~400,有时卡顿到2s+|

## BGAQRCode-Android zbar库优化前后，连续扫描极限速度
说明：原zbar库的识别方式：在QRCodeView的相机预览方法onPreviewFrame中，添加AsyncTask的异步处理，有正确结果了返回并停止预览，没结果重新开启预览。所以在activity中重新开启相机，再预览扫描，需要220～250ms，比较耗时。
     而经个人改版后，连续识别速度提升非常明显，取帧+返回正确结果，耗时60～90ms，**onPreviewFrame在50~80ms取一次帧数据**。具体代码在lib_zbar的lib.ruijia.zbar.qrodecontinue包下。xml布局将ZBarView替换为ZBarContinueView即可使用。
     具体修改方式：onPreviewFrame方法，保持持续预览,并将取到的帧数据放到handler.post(runnable)的runnable方法中去处理，获取结果即可。是不是简单到爆。
     如下是个人测试。

| 字符串大小（个）| 修改前zbar (ms)200ms|修改后zbar(ms)150ms间隔发送|修改后zbar(ms)120ms间隔发送|
| ---------- | ------------- | --------------| --------------
|1024|220~250 稳定|70～90ms,稳定|70～90ms,稳定|
|1536|同上，极少300+|70～90ms,稳定|70～90ms,稳定|
|2048|同上，很稳定|70～90ms,稳定|70～90ms,稳定|
|2560|同上，很稳定|70～90ms,稳定|70～90ms,稳定|
|2952|同上，很稳定|70～90ms,稳定|70～90ms,稳定|

## 优化后，连续识别，丢失数据测试

优化后，以800*800px二维码，容量大小2900B，26张图发送，比较 200ms,150ms,120ms的数据完整性
说明：1s传输容量计算方式：以发送次数和时间间隔计算，eg：26张需要25次，间隔200ms,则25次耗时25/（1000/200）=5s;
接收端丢失3，则（26-3）*2900/5.2/1024=12.53KB

总容量：2900*26=75400


| 发送二维码时间间隔|（一次传输完时间）|平均丢失数据|效率|平均1s传输容量|2900换成2952|
| ---------- | -------------| --------------| --------------| --------------| --------------
|300ms|(7.5s)|0(经常0丢失，偶尔2～3丢失)|100%(普遍)|9.82KB|10KB|
|200ms|(5s)|2.5|90.38%|14.72KB|15KB|
|160ms|(4s)|2.5|90.38%|16.64KB|16.9KB|
|150ms|(3.75s)|2.25|91.3%位置不好有大半数据不识别的情况|17.9KB|18.2KB|
|140ms|3.5s|2.5|90.38%|19KB|19.3KB|
|140ms|3.5s|0(有全部识别的情况)|100%|21KB|21.4KB|
|130ms|3.25|稳定丢失2～3个|88.5%|20KB|20.3KB|
|120ms|(3.12)|稳定丢失3+数据，效果不理想||||

说明：
300ms，则大部分情况，0丢失数据，则1s效率9.82KB,最大10KB.

200ms 数据丢失情况较少，如果0丢失，则1s效率14.72KB（无损失数据,最大（15KB）），如果丢失2个数据（需要在花1～2s补全，这部分没法测试，假设1s补全，则1s实际效率12.27KB)。

160ms,位置合适，平均丢数据2.5个，则1s效率16.64KB（假设不全数据耗时1.5s,则1s实际效率13.38KB）。

150ms,找到合适角度，平局丢失2个（大部分时候位置不好确定）

140ms,有0丢失情况，平均丢失2.5个，**若0丢失，则1s效率21KB**若丢失2.5个,1s效率19KB,（1.5s补全丢失数据，则1s实际效率14KB）

130ms，每次测试，必丢失数据，普遍丢失2～3个数据，若丢失3个，则1s效率20KB（补充2s耗时，1s实际效率14KB）

总结：(1)最好的情况在300ms和140ms，数据都可以一次识别通过，最高效率20KB/s，换成最大容量2952KB,最高21.4KB/s。
(2)没有定制机或条件测试丢失数据补全的功能，补全数据的耗时没法计算。



参考：BGAQRCode-Android
