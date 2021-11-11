该程序用于构建一个Android平台的MQTT客户端，使用腾讯云MQTT服务，客户端连接上后可以发布和订阅消息。另一个客户端程序在ESP8266（NodeMCU）构建，主要功能是通过stm32采集温湿度、光照信息后通过ESP8266发布信息到腾讯云MQTT服务端，Android App通过订阅主题获得数据再进行展示。

查看[文章](https://ajream.github.io/posts/dcaf7533.html)

