

```markdown
db.sql是项目用到的数据和结构，nohup.out是生产的日志。结构图：
┌─────────────────────────────────────────────────────────────────┐
│                         TCP 8092端口                             │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │  称重数据 JSON   │             │  JT/T808 轨迹   │
    │  (wn_transport)  │             │  (LocationData) │
    └─────────────────┘             └─────────────────┘
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │  1. 保存数据库  │             │  1. 保存数据库  │
    │  2. 幂等检查    │             │  2. Redis缓存   │
    │  3. 联单匹配    │             │  3. WebSocket推送│
    └─────────────────┘             └─────────────────┘
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌───────────────────┐
    │  联单状态管理   │◄────────────┤  轨迹处理      	│
    │  (Redis缓存)    │             │(processTrajectory)│
    └─────────────────┘             └───────────────────┘
              │                               │
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │  预警规则检查     │◄────────────┤  轨迹更新       │
    │  - 超时停留预警   │             │  - 实时推送     │
    │  - 运输超时预警 │             │   - 报警监控    │
    │  - 重量偏差预警 │             │       		│
    │  - 断联预警     │             └─────────────────┘
	│  - 路线偏离预警 │
	│  - 超出电子围栏 │
	│  - 重复上传预警	 │
    └─────────────────┘
状态流转总结
阶段	    触发事件	                    联单状态	    轨迹采集
移出称重	收到112661/112663的称重数据	        运输中(2)	    开始采集
运输过程	收到JT/T808轨迹数据	                运输中(2)	    持续采集
接收称重	收到112662的称重数据	            已接收(3)	    停止采集

项目场景是联单监管起点到终点的车辆运输
两个起点天长市污水处理厂(站点编号：112661)、东市区污水处理厂(站点编号：112663)，一个终点天长市污泥处理厂(站点编号112662)
系统目前之监管6辆车：皖M8A011、皖M4B961、皖M8A862、	皖M9A017、	豫QC2010、皖N43777，其中 站点112661运输到终点的专属车辆是：皖M8A011，站点112662运输到终点的车辆是：皖N43777
与称重系统约定站点112661、112663每条数据都是移出的数据，112662都是接收的数据，项目高度依赖称重


-- 一条正常的称重数据应该是如data6、data7
import socket


def test_send():
    host = '127.0.0.1'
    port = 8092  # 你的服务端口

    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect((host, port))

    data1=r'{"sDetectCode": "1126611202511050936220112","sSiteCode": "112661","sDateTime": "2026-06-09 08:46:03","sPlateName": "皖M8A011","nTotalWeight": 24040.000,"nEnterChannel": 1,"nEnterAspect": 1,"sBase64TailData": "","consignee": "","dispatcher": "","productName": "污泥","suttle": 630.000,"sExitTime": "2026-06-09 08:46:03"}'

    data2=r'{"sDetectCode": "1126621202511051229090001","sSiteCode": "112662","sDateTime": "2026-06-09 12:28:45","sPlateName": "皖M8A011","nTotalWeight": 24040.000,"nEnterChannel": 1,"nEnterAspect": 1,"sBase64TailData": "","consignee": "天长市天道新型建材有限公司","dispatcher": "天长市中冶水务有限公司","productName": "污泥","suttle": 630.000,"sExitTime": "2025-12-10 12:28:45"}'

    data3=r'{"sDetectCode": "1126631202511051043370001","sSiteCode": "112663","sDateTime": "2026-01-09 10:43:15","sPlateName": "皖N43777","nTotalWeight": 24190.000,"nEnterChannel": 1,"nEnterAspect": 1,"sBase64TailData": "","consignee": "天长市天道新型建材有限公司","dispatcher": "中节能国祯环保科技股份有限公司天长东市区","productName": "干化泥","suttle": 500.000,"sExitTime": "2025-12-10 10:43:15"}'

    data4=r'{"sDetectCode": "1126621202511051229090001","sSiteCode": "112662","sDateTime": "2026-01-09 12:28:45","sPlateName": "皖N43777","nTotalWeight": 24190.000,"nEnterChannel": 1,"nEnterAspect": 1,"sBase64TailData": "","consignee": "天长市天道新型建材有限公司","dispatcher": "中节能国祯环保科技股份有限公司天长东市区","productName": "污泥","suttle": 500.000,"sExitTime": "2025-12-10 12:28:45"}'

    data5=r'{"sDetectCode": "1126611202511110206390001","sSiteCode": "112661","sDateTime": "2026-01-09 14:06:15","sPlateName": "皖M4B961","nTotalWeight": 35680.000,"nEnterChannel": 1,"nEnterAspect": 1,"sBase64TailData": "","consignee": "天长市污水处理厂","dispatcher": "滁州高新区污水处理厂","productName": "含水率80%污泥","suttle": 1600.000,"sExitTime": "2025-11-11 14:06:15"}'

    data6=r'{"sDetectCode": "1","sSiteCode": "112661","sDateTime": "2026-06-10 07:22:48","sPlateName": "皖M8A011","nTotalWeight": 26570.000,"nEnterChannel": 2,"nEnterAspect": 2,"sBase64HeadData": "","sBase64TailData":"","sBase64BodyData":"","sBase64PlateData":"","consignee": "天长市天道新型建材有限公司","dispatcher": "中节能国祯环保科技股份有限公司天长东市区","productName": "干化泥","suttle": 16770.000,"timeStampOne": "26570.000","timeStampTwo": "9800.000","timeStampOneTime": "2026-03-09 19:08:51","timeStampTwoTime": "2026-03-10 07:22:48","lastModificationTime": "2026-03-10 07:22:48","sExitTime": "2026-03-10 07:22:48"}'

    data7=r'{"sDetectCode": "2","sSiteCode": "112662","sDateTime": "2026-06-10 08:02:48","sPlateName": "皖M8A011","nTotalWeight": 26570.000,"nEnterChannel": 2,"nEnterAspect": 2,"sBase64HeadData": "","sBase64TailData":"","sBase64BodyData":"","sBase64PlateData":"","consignee": "天长市天道新型建材有限公司","dispatcher": "中节能国祯环保科技股份有限公司天长东市区","productName": "干化泥","suttle": 16770.000,"timeStampOne": "26570.000","timeStampTwo": "9800.000","timeStampOneTime": "2026-03-09 19:08:51","timeStampTwoTime": "2026-03-10 07:22:48","lastModificationTime": "2026-03-10 07:22:48","sExitTime": "2026-03-10 07:22:48"}'



    # 强制 UTF-8 编码
    client.send(data6.encode('utf-8'))

    response = client.recv(1024)
    print("Recv:", response.decode('utf-8'))

    client.close()


if __name__ == '__main__':
    test_send()
