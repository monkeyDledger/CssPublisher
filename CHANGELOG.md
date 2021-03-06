# Change Log

## v1.1.3 (2017-3-10)
* 添加动态权限申请
* 修改so库的静态加载方式
* 修改minSDK值为15，并在主Activity添加对设备SDK版本的判断，低于5.0提示用户无法进行直播推流

## v1.1.2 (2017-2-15)
**Features**
* 我的模块添加图片上传入口
* 添加相册列表和图片列表两个页面
* 点击图片选择图片待上传，长按图片显示大图
* **FileUtil**, 添加根据路径返回文件名，添加根据路径返回Bitmap对象
* **HttpUtil**, 添加图片批量上传请求，接口/receiveImages

## v1.1.1 (2017-1-17)
**Features**
* 添加记录用户名并自动填写功能

**Bug Fix**
* 解决直播时退出程序或退出登录未发送通知问题
* 首Activity添加对Intent是否带Category判断，解决安卓系统安装器打开apk时遇到Home键重进导致程序重启问题

## v1.0.1 (2017-1-11)
**Features**
* **本地录制音频**, 本地录屏改为使用`MediaRecord`，通过设备的麦克风采集音频
* **直播录制音频**, 引入`NTAudioRecord`,通过设备的麦克风采集音频
* **FileUtil**, 添加文件重命名并复制到指定路径的方法
* **HttpUtil**, 添加开启和关闭直播的通知请求，接口/liveStatus

## v1.0.0 (2016-12-30)
### 第一版
**Features**
* **设备录屏**，录屏分本地和直播，直播可以选择是否保存本地文件
* **摄像直播**，支持前后摄像头预览并直播推流，可选择是否保存本地文件
* **我的**，视频文件的分类管理，包括删除、上传和刷新功能