package com.stylefeng.guns.api.order;

import com.baomidou.mybatisplus.plugins.Page;
import com.stylefeng.guns.api.order.vo.OrderVO;

import java.util.List;

public interface OrderServiceApi {

    //验证售出座位是否为合法真实的座位
    boolean isTrueSeats(Integer fieldId,String seats);

    //是否是已经销售的座位
    boolean isNotSoldSeats(Integer fieldId,String seats);

    //创建订单信息
    OrderVO saveOrderInfo(Integer fieldId, String soldSeats, String seatsName,Integer userId);

    //获取订单信息
    Page<OrderVO> getOderInfoBuUserId(Integer userId,Page<OrderVO> page);

    //根据FiledId获取所有已销售的座位编号
    String getSoldSeats(Integer fieldId);

    //根据订单编号获取订单信息
    OrderVO getOrderInfoById(String orderId);

    boolean paySuccess(String orderId);

    boolean payFail(String orderId);
}
