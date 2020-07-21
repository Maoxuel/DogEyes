package com.stylefeng.guns.rest.modular.cinema;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.plugins.Page;
import com.stylefeng.guns.api.cinema.CinemaServiceAPI;
import com.stylefeng.guns.api.cinema.MyCinemaServiceAPI;
import com.stylefeng.guns.api.cinema.vo.*;
import com.stylefeng.guns.api.order.OrderServiceApi;
import com.stylefeng.guns.rest.modular.cinema.vo.CinemaConditionResponseVO;
import com.stylefeng.guns.rest.modular.cinema.vo.CinemaFiledResponseVO;
import com.stylefeng.guns.rest.modular.cinema.vo.CinemaFiledsResponseVO;
import com.stylefeng.guns.rest.modular.cinema.vo.CinemaListResponseVO;
import com.stylefeng.guns.rest.modular.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cinema/")
@Slf4j
public class CinemaController {

    private static final String IMG_PRE = "http://img.meetingshop.cn/";

    @Reference(interfaceClass = CinemaServiceAPI.class,check = false,cache = "lru",connections = 10)
    private CinemaServiceAPI cinemaServiceAPI;

//    @Reference(interfaceClass = MyCinemaServiceAPI.class,check = false,cache = "lru",connections = 10)
//    private MyCinemaServiceAPI cinemaServiceAPI;

    @Reference(interfaceClass = OrderServiceApi.class,check = false,cache = "lru",connections = 10,group = "default")
    private OrderServiceApi orderServiceApi;

    @GetMapping(value = "getCinemas")
    public ResponseVO getCinemas(CinemaQueryVO cinemaQueryVO){
        try {
            //按照5个条件进行筛查
            Page<CinemaVO> cinemas = cinemaServiceAPI.getCinemas(cinemaQueryVO);

            if (cinemas.getRecords() == null || cinemas.getRecords().size()==0){
                return ResponseVO.serviceFail("没有符合条件的影院");
            }else {
                CinemaListResponseVO cinemaListResponseVO = new CinemaListResponseVO();
                cinemaListResponseVO.setCinemas(cinemas.getRecords());
                return ResponseVO.success(cinemas.getCurrent(), (int) cinemas.getPages(),"",cinemaListResponseVO);
            }

        } catch (Exception e){
            log.error("没有符合条件的影院");
            return ResponseVO.serviceFail("没有符合条件的影院");
        }
    }

    //获取影院查询条件

    /**
     * 1.热点数据->放入缓存
     * 2.banner
     * @param cinemaQueryVO
     * @return
     */
    @GetMapping(value = "getCondition")
    public ResponseVO getCondition(CinemaQueryVO cinemaQueryVO){
        try {
            //获取三个集合，封装成对象
            List<HallTypeVO> hallType = cinemaServiceAPI.getHallType(cinemaQueryVO.getHallType());
            List<BrandVO> brands = cinemaServiceAPI.getBrands(cinemaQueryVO.getBrandId());
            List<AreaVO> areas = cinemaServiceAPI.getAreas(cinemaQueryVO.getDistrictId());

            CinemaConditionResponseVO cinemaConditionResponseVO = new CinemaConditionResponseVO();
            cinemaConditionResponseVO.setAreaList(areas);
            cinemaConditionResponseVO.setBrandList(brands);
            cinemaConditionResponseVO.setHallTypeList(hallType);

            return ResponseVO.success(cinemaConditionResponseVO);

        } catch (Exception e){
            log.error("获取条件失败");
            return ResponseVO.serviceFail("获取条件失败");
        }

    }

    @GetMapping(value = "getFields")
    public ResponseVO getFields(Integer cinemaId){
        try {
            CinemaInfoVO cinemaInfo = cinemaServiceAPI.getCinemaInfoById(cinemaId);
            List<FilmInfoVO> filmInfos = cinemaServiceAPI.getFilmInfoByCinemaId(cinemaId);

            CinemaFiledsResponseVO cinemaFiledsResponseVO = new CinemaFiledsResponseVO();
            cinemaFiledsResponseVO.setCinemaInfo(cinemaInfo);
            cinemaFiledsResponseVO.setFilmList(filmInfos);

            return ResponseVO.success(IMG_PRE, cinemaFiledsResponseVO);
        } catch (Exception e){
            log.error("获取播放场次失败");
            return ResponseVO.serviceFail("获取播放场次失败");
        }
    }

    @PostMapping(value = "getFieldInfo")
    public ResponseVO getFieldInfo(Integer cinemaId,Integer fieldId){
        try {
            CinemaInfoVO cinemaInfo = cinemaServiceAPI.getCinemaInfoById(cinemaId);
            FilmInfoVO filmInfo = cinemaServiceAPI.getFilmInfoByFieldId(fieldId);
            HallInfoVO filmFieldInfo = cinemaServiceAPI.getFilmFieldInfo(fieldId);


            filmFieldInfo.setSoldSeats(orderServiceApi.getSoldSeats(fieldId));

            CinemaFiledResponseVO cinemaFiledResponseVO = new CinemaFiledResponseVO();
            cinemaFiledResponseVO.setCinemaInfo(cinemaInfo);
            cinemaFiledResponseVO.setFilmInfo(filmInfo);
            cinemaFiledResponseVO.setHallInfo(filmFieldInfo);

            return ResponseVO.success(IMG_PRE,cinemaFiledResponseVO);
        } catch (Exception e){
            log.error("获取场次详细信息失败");
            return ResponseVO.serviceFail("获取场次详细信息失败");
        }
    }
}
