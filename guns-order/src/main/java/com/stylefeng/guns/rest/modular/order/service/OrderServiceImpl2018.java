package com.stylefeng.guns.rest.modular.order.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.stylefeng.guns.api.cinema.CinemaServiceAPI;
import com.stylefeng.guns.api.cinema.vo.FilmInfoVO;
import com.stylefeng.guns.api.cinema.vo.OrderQueryVO;
import com.stylefeng.guns.api.order.OrderServiceApi;
import com.stylefeng.guns.api.order.vo.OrderVO;
import com.stylefeng.guns.rest.common.persistence.dao.MoocOrder2018TMapper;
import com.stylefeng.guns.rest.common.persistence.dao.MoocOrderTMapper;
import com.stylefeng.guns.rest.common.persistence.model.MoocOrder2018T;
import com.stylefeng.guns.rest.common.persistence.model.MoocOrderT;
import com.stylefeng.guns.rest.common.util.FTPUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@Service(interfaceClass = OrderServiceApi.class,group = "2018",filter = "tracing")
public class OrderServiceImpl2018 implements OrderServiceApi {


    @Autowired
    private MoocOrder2018TMapper moocOrderTMapper;

    @Reference(interfaceClass = CinemaServiceAPI.class,check = false,filter = "tracing")
    private CinemaServiceAPI cinemaServiceAPI;

    @Autowired
    private FTPUtil ftpUtil;

    //验证售出座位是否为合法真实的座位
    @Override
    public boolean isTrueSeats(Integer fieldId, String seats) {

        //根据FieldId找到对应的座位位置图
        String seatsPath = moocOrderTMapper.getSeatsByFieldId(fieldId+"");

        //读取位置图，判断seats是否为真
        String fileStrByAddress = ftpUtil.getFileStrByAddress(seatsPath);
        //转换为json对象
        JSONObject jsonObject = JSONObject.parseObject(fileStrByAddress);
        //seats="1,2,3"  ids = "1,3,4,5,7"
        String ids = jsonObject.get("ids").toString();

        String[] seatsArr = seats.split(",");
        String[] idsArr = ids.split(",");

        Integer isTrue = 0;
        for(String seat:seatsArr){
            for(String id:idsArr){
                if(seat.equalsIgnoreCase(id)){
                    isTrue++;
                }
            }
        }

        if(isTrue == seatsArr.length){
            return true;
        }else{
            return false;
        }

    }

    //是否是已经销售的座位
    @Override
    public boolean isNotSoldSeats(Integer fieldId, String seats) {

        EntityWrapper<MoocOrder2018T> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("field_id",fieldId);

        //可以通过sql语句，将id合成一个数组(横转纵，逗号合并)，再二分查找
        List<MoocOrder2018T> list = moocOrderTMapper.selectList(entityWrapper);
        String[] seatArr = seats.split(",");

        // 有任何一个编号匹配上，则直接返回失败
        for(MoocOrder2018T moocOrderT : list){
            String[] ids = moocOrderT.getSeatsIds().split(",");
            for(String id : ids){
                for(String seat : seatArr){
                    if(id.equalsIgnoreCase(seat)){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    //创建订单信息
    @Override
    @Transactional
    public OrderVO saveOrderInfo(Integer fieldId, String soldSeats, String seatsName, Integer userId) {

        String uuid = UUID.randomUUID().toString().replaceAll("-","");

        //获取影片信息
        FilmInfoVO filmInfoVO = cinemaServiceAPI.getFilmInfoByFieldId(fieldId);
        Integer filmId = Integer.parseInt(filmInfoVO.getFilmId());

        //获取影院信息
        OrderQueryVO orderQueryVO = cinemaServiceAPI.getOrderNeeds(fieldId);
        Integer cinemaId = Integer.parseInt(orderQueryVO.getCinemaId());
        double price = Double.parseDouble(orderQueryVO.getFilmPrice());

        //求订单金额
        int solds = soldSeats.split(",").length;
        double totalPrice = getTotalPrice(solds, price);


        MoocOrder2018T moocOrderT = new MoocOrder2018T();
        moocOrderT.setUuid(uuid);
        moocOrderT.setSeatsName(seatsName);
        moocOrderT.setSeatsIds(soldSeats);
        moocOrderT.setOrderUser(userId);
        moocOrderT.setOrderPrice(totalPrice);
        moocOrderT.setFilmPrice(price);
        moocOrderT.setFilmId(filmId);
        moocOrderT.setFieldId(fieldId);
        moocOrderT.setCinemaId(cinemaId);
        moocOrderT.setOrderStatus(0);

        Integer insert = moocOrderTMapper.insert(moocOrderT);
        if (insert>0){
            //返回查询结果
            OrderVO orderVO = moocOrderTMapper.getOrderInfoById(uuid);
            if (orderVO == null || orderVO.getOrderId() == null){
                log.error("订单信息查询失败，编号为{}",uuid);
                return null;
            }else {
                return orderVO;
            }
        }else {
            log.error("订单创建失败");
            return null;
        }
    }

    private double getTotalPrice(int solds, double filmPrice){
        BigDecimal bigDecimal = new BigDecimal(solds);
        BigDecimal bigDecimal1 = new BigDecimal(filmPrice);

        BigDecimal result = bigDecimal.multiply(bigDecimal1);

        //四舍五入，取小数点后两位
        BigDecimal res = result.setScale(2, RoundingMode.HALF_UP);

        return res.doubleValue();
    }

    @Override
    public Page<OrderVO> getOderInfoBuUserId(Integer userId,Page<OrderVO> page) {
        Page<OrderVO> result = new Page<>();

        if(userId == null){
            log.error("订单查询失败，用户编号出错");
            return null;
        }
        List<OrderVO> orderVOList = moocOrderTMapper.getOrdersByUserId(userId,page);
        if (orderVOList == null || orderVOList.size() == 0){
            result.setRecords(new ArrayList<>());
            result.setTotal(0);
            return result;
        }else {
            //获取订单总数
            EntityWrapper<MoocOrder2018T> entityWrapper = new EntityWrapper<>();
            entityWrapper.eq("order_user",userId);
            Integer count = moocOrderTMapper.selectCount(entityWrapper);
            result.setTotal(count);
            result.setRecords(orderVOList);
            //结果放入配置
            return result;
        }
    }

    //根据FiledId获取所有已销售的座位编号
    @Override
    public String getSoldSeats(Integer fieldId) {
        if (fieldId == null){
            log.error("查询已售座次错误");
            return null;
        }
        String seats = moocOrderTMapper.getSoldSeatsByFieldId(fieldId);
        return seats;
    }

    @Override
    public OrderVO getOrderInfoById(String orderId) {
        OrderVO orderVO = moocOrderTMapper.getOrderInfoById(orderId);
        return orderVO;
    }

    @Override
    public boolean paySuccess(String orderId) {
        MoocOrder2018T moocOrderT = new MoocOrder2018T();
        moocOrderT.setOrderStatus(1);
        moocOrderT.setUuid(orderId);
        Integer integer = moocOrderTMapper.updateById(moocOrderT);
        if(integer>=1){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public boolean payFail(String orderId) {

        MoocOrder2018T moocOrderT = new MoocOrder2018T();
        moocOrderT.setOrderStatus(2);
        moocOrderT.setUuid(orderId);
        Integer integer = moocOrderTMapper.updateById(moocOrderT);
        if(integer>=1){
            return true;
        }else {
            return false;
        }
    }
}
