package com.stylefeng.guns.rest.modular.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.baomidou.mybatisplus.plugins.Page;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.stylefeng.guns.api.alipay.AliPayServiceAPI;
import com.stylefeng.guns.api.alipay.vo.AliPayInfoVO;
import com.stylefeng.guns.api.alipay.vo.AliPayResultVO;
import com.stylefeng.guns.api.order.OrderServiceApi;
import com.stylefeng.guns.api.order.vo.OrderVO;
import com.stylefeng.guns.core.util.TokenBucket;
import com.stylefeng.guns.core.util.ToolUtil;
import com.stylefeng.guns.rest.common.CurrentUser;
import com.stylefeng.guns.rest.modular.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping(value = "/order")
public class OrderController {

    private static TokenBucket tokenBucket = new TokenBucket();

    @Reference(interfaceClass = OrderServiceApi.class, check = false, group = "2018",filter = "tracing")
    private OrderServiceApi orderServiceApi2018;

    @Reference(interfaceClass = OrderServiceApi.class, check = false, group = "default")
    private OrderServiceApi orderServiceApi;

    @Reference(interfaceClass = AliPayServiceAPI.class,check = false)
    private AliPayServiceAPI aliPayServiceAPI;

    private static final String IMG_PRE="http://img.meetingshop.cn/";

    //降级方法
    public ResponseVO error(Integer fieldId, String soldSeats, String seatsName){
        return ResponseVO.serviceFail("当前业务忙，请稍后重试");
    }

    @HystrixCommand(fallbackMethod = "error", commandProperties = {
            @HystrixProperty(name = "execution.isolation.strategy", value = "THREAD"),
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value
                    = "4000"),
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50")
    }, threadPoolProperties = {
            @HystrixProperty(name = "coreSize", value = "1"),
            @HystrixProperty(name = "maxQueueSize", value = "10"),
            @HystrixProperty(name = "keepAliveTimeMinutes", value = "1000"),
            @HystrixProperty(name = "queueSizeRejectionThreshold", value = "8"),
            @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "12"),
            @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1500")
    })
    @PostMapping(value = "/buyTickets")
    public ResponseVO buyTickets(Integer fieldId, String soldSeats, String seatsName) {

        try {
            //使用令牌桶进行限流
            if (tokenBucket.getToken()) {
                // 验证售出座位是否为合法
                boolean trueSeats = orderServiceApi2018.isTrueSeats(fieldId, soldSeats);

                //是否是已经销售的座位
                boolean notSoldSeats = orderServiceApi2018.isNotSoldSeats(fieldId, soldSeats);

                if (trueSeats && notSoldSeats) {
                    //创建订单信息，获取当前用户
                    String userId = CurrentUser.getCurrentUser();
                    if (userId == null && userId.trim().length() == 0) {
                        return ResponseVO.serviceFail("请登录");
                    }
                    OrderVO orderVO = orderServiceApi2018.saveOrderInfo(fieldId, soldSeats, seatsName, Integer.parseInt(userId));
                    if (orderVO == null) {
                        log.error("购票失败");
                        return ResponseVO.serviceFail("购票失败");
                    }
                    return ResponseVO.success(orderVO);
                } else {
                    return ResponseVO.serviceFail("购票座位冲突");
                }
            } else {
                return ResponseVO.serviceFail("当前业务忙，请稍后重试");
            }
        } catch (Exception e) {
            log.error("购票失败");
            return ResponseVO.serviceFail("购票失败");
        }
    }

    @PostMapping(value = "/getOrderInfo")
    public ResponseVO getOrderInfo(
            @RequestParam(name = "nowPage", required = false, defaultValue = "1") Integer nowPage,
            @RequestParam(name = "pageSize", required = false, defaultValue = "5") Integer pageSize) {

        //获取当前用户
        String userId = CurrentUser.getCurrentUser();

        Page<OrderVO> page = new Page<>(nowPage, pageSize);
        if (userId != null && userId.trim().length() > 0) {
            //获取订单信息
            Page<OrderVO> result = orderServiceApi2018.getOderInfoBuUserId(Integer.parseInt(userId), page);
            Page<OrderVO> result2017 = orderServiceApi.getOderInfoBuUserId(Integer.parseInt(userId), page);

            //合并结果
            int totalCounts = (int) (result.getTotal() + result2017.getTotal());
            List<OrderVO> records = new ArrayList<>();
            records.addAll(result.getRecords());
            records.addAll(result2017.getRecords());

            return ResponseVO.success(nowPage, totalCounts, "", records);
        } else {
            return ResponseVO.serviceFail("请登录");
        }

    }

    @PostMapping(value = "/getPayInfo")
    public ResponseVO getPayInfo(@RequestParam("orderId")String orderId){

        //获取当前用户
        String userId = CurrentUser.getCurrentUser();

        if(userId==null || userId.trim().length()==0){
            return ResponseVO.serviceFail("抱歉，用户未登陆");
        }
        // 订单二维码返回结果
        AliPayInfoVO aliPayInfoVO = aliPayServiceAPI.getQRCode(orderId);
        return ResponseVO.success(IMG_PRE,aliPayInfoVO);
    }

    @PostMapping(value = "getPayResult")
    public ResponseVO getPayResult(
            @RequestParam("orderId") String orderId,
            @RequestParam(name="tryNums",required = false,defaultValue = "1") Integer tryNums){
        // 获取当前登陆人的信息
        String userId = CurrentUser.getCurrentUser();
        if(userId==null || userId.trim().length()==0){
            return ResponseVO.serviceFail("抱歉，用户未登陆");
        }

        // 将当前登陆人的信息传递给后端
        RpcContext.getContext().setAttachment("userId",userId);

        // 判断是否支付超时
        if(tryNums>=4){
            return ResponseVO.serviceFail("订单支付失败，请稍后重试");
        }else{
            AliPayResultVO aliPayResultVO = aliPayServiceAPI.getOrderStatus(orderId);
            if(aliPayResultVO == null || ToolUtil.isEmpty(aliPayResultVO.getOrderId())){
                AliPayResultVO serviceFailVO = new AliPayResultVO();
                serviceFailVO.setOrderId(orderId);
                serviceFailVO.setOrderStatus(0);
                serviceFailVO.setOrderMsg("支付不成功");
                return ResponseVO.success(serviceFailVO);
            }
            return ResponseVO.success(aliPayResultVO);
        }
    }

}
