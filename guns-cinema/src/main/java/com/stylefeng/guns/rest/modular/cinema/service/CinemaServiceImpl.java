package com.stylefeng.guns.rest.modular.cinema.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.stylefeng.guns.api.cinema.CinemaServiceAPI;
import com.stylefeng.guns.api.cinema.vo.*;
import com.stylefeng.guns.rest.common.persistence.dao.*;
import com.stylefeng.guns.rest.common.persistence.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Service(interfaceClass = CinemaServiceAPI.class,executes = 10,filter = "tracing")
public class CinemaServiceImpl implements CinemaServiceAPI {

    @Autowired
    private MoocCinemaTMapper moocCinemaTMapper;

    @Autowired
    private MoocBrandDictTMapper moocBrandDictTMapper;

    @Autowired
    private MoocAreaDictTMapper moocAreaDictTMapper;

    @Autowired
    private MoocHallDictTMapper moocHallDictTMapper;

    @Autowired
    private MoocFieldTMapper moocFieldTMapper;

    //1.根据CinemaQueryVO查询影院列表
    @Override
    public Page<CinemaVO> getCinemas(CinemaQueryVO cinemaQueryVO) {

        // 业务实体集合
        List<CinemaVO> cinemas = new ArrayList<>();

        Page<MoocCinemaT> page = new Page<>(cinemaQueryVO.getNowPage(),cinemaQueryVO.getPageSize());
        // 判断是否传入查询条件 -> brandId,distId,hallType 是否==99
        EntityWrapper<MoocCinemaT> entityWrapper = new EntityWrapper<>();
        if(cinemaQueryVO.getBrandId() != 99){
            entityWrapper.eq("brand_id",cinemaQueryVO.getBrandId());
        }
        if(cinemaQueryVO.getDistrictId() != 99){
            entityWrapper.eq("area_id",cinemaQueryVO.getDistrictId());
        }
        if(cinemaQueryVO.getHallType() != 99){  // %#3#%
            entityWrapper.like("hall_ids","%#+"+cinemaQueryVO.getHallType()+"+#%");
        }

        // 将数据实体转换为业务实体
        List<MoocCinemaT> moocCinemaTS = moocCinemaTMapper.selectPage(page, entityWrapper);
        for(MoocCinemaT moocCinemaT : moocCinemaTS){
            CinemaVO cinemaVO = new CinemaVO();

            cinemaVO.setUuid(moocCinemaT.getUuid()+"");
            cinemaVO.setMinimumPrice(moocCinemaT.getMinimumPrice()+"");
            cinemaVO.setCinemaName(moocCinemaT.getCinemaName());
            cinemaVO.setAddress(moocCinemaT.getCinemaAddress());

            cinemas.add(cinemaVO);
        }

        // 根据条件，判断影院列表总数
        long counts = moocCinemaTMapper.selectCount(entityWrapper);

        // 组织返回值对象
        Page<CinemaVO> result = new Page<>();
        result.setRecords(cinemas);
        result.setSize(cinemaQueryVO.getPageSize());
        result.setTotal(counts);

        return result;
    }

    //2.根据条件获取品牌列表,除了99外，其余数字为isActive
    @Override
    public List<BrandVO> getBrands(int brandId) {
        boolean flag = false;
        List<BrandVO> brandVOS = new ArrayList<>();
        MoocBrandDictT brandDictT = moocBrandDictTMapper.selectById(brandId);
        if(brandId == 99 || brandDictT == null || brandDictT.getUuid() == null){
            flag = true;
        }

        List<MoocBrandDictT> moocBrandDictTS = moocBrandDictTMapper.selectList(null);
        for (MoocBrandDictT moocBrandDictT:moocBrandDictTS){
            BrandVO brandVO = new BrandVO();
            brandVO.setBrandId(moocBrandDictT.getUuid()+"");
            brandVO.setBrandName(moocBrandDictT.getShowName());
            if(flag){
                if (moocBrandDictT.getUuid() == 99){
                    brandVO.setIsActive(true);
                }
            }else {
                if (moocBrandDictT.getUuid() == brandId){
                    brandVO.setIsActive(true);
                }
            }
            brandVOS.add(brandVO);
        }
        return brandVOS;
    }

    @Override
    public List<AreaVO> getAreas(int areaId) {
        boolean flag = false;
        List<AreaVO> areaVOS = new ArrayList<>();
        MoocAreaDictT moocAreaDictT = moocAreaDictTMapper.selectById(areaId);
        if(areaId == 99 || moocAreaDictT == null || moocAreaDictT.getUuid() == null){
            flag = true;
        }

        List<MoocAreaDictT> moocAreaDictTS = moocAreaDictTMapper.selectList(null);
        for (MoocAreaDictT areaDictT:moocAreaDictTS){
            AreaVO areaVO = new AreaVO();
            areaVO.setAreaId(areaDictT.getUuid()+"");
            areaVO.setAreaName(areaDictT.getShowName());
            if(flag){
                if (areaDictT.getUuid() == 99){
                    areaVO.setIsActive(true);
                }
            }else {
                if (areaDictT.getUuid() == areaId){
                    areaVO.setIsActive(true);
                }
            }
            areaVOS.add(areaVO);
        }
        return areaVOS;
    }

    @Override
    public List<HallTypeVO> getHallType(int hallType) {
        boolean flag = false;
        List<HallTypeVO> hallTypeVOS = new ArrayList<>();
        MoocHallDictT moocHallDictT = moocHallDictTMapper.selectById(hallType);
        if(hallType == 99 || moocHallDictT == null || moocHallDictT.getUuid() == null){
            flag = true;
        }

        List<MoocHallDictT> moocHallDictTS = moocHallDictTMapper.selectList(null);
        for (MoocHallDictT hallDictT:moocHallDictTS){
            HallTypeVO hallTypeVO = new HallTypeVO();
            hallTypeVO.setHallTypeId(hallDictT.getUuid()+"");
            hallTypeVO.setHallTypeName(hallDictT.getShowName());
            if(flag){
                if (hallDictT.getUuid() == 99){
                    hallTypeVO.setIsActive(true);
                }
            }else {
                if (hallDictT.getUuid() == hallType){
                    hallTypeVO.setIsActive(true);
                }
            }
            hallTypeVOS.add(hallTypeVO);
        }
        return hallTypeVOS;
    }

    //5.根据影院编号获取影院信息
    @Override
    public CinemaInfoVO getCinemaInfoById(int cinemaId) {

        CinemaInfoVO cinemaInfoVO = new CinemaInfoVO();

        MoocCinemaT moocCinemaT = moocCinemaTMapper.selectById(cinemaId);
        cinemaInfoVO.setImgUrl(moocCinemaT.getImgAddress());
        cinemaInfoVO.setCinemaPhone(moocCinemaT.getCinemaPhone());
        cinemaInfoVO.setCinemaName(moocCinemaT.getCinemaName());
        cinemaInfoVO.setCinemaId(moocCinemaT.getUuid()+"");

        return cinemaInfoVO;
    }

    //6.获取所有电影的信息和对应的放映场次信息，根据影院编号
    @Override
    public List<FilmInfoVO> getFilmInfoByCinemaId(int cinemaId) {
        List<FilmInfoVO> filmInfos = moocFieldTMapper.getFilmInfos(cinemaId);
        return filmInfos;
    }

    //7.根据放映场次ID获取放映信息
    @Override
    public HallInfoVO getFilmFieldInfo(int fieldId) {
        HallInfoVO hallInfo = moocFieldTMapper.getHallInfo(fieldId);
        return hallInfo;
    }

    //8.根据放映场次查询播放的电影编号，后根据电影编号获取对应的电影信息
    @Override
    public FilmInfoVO getFilmInfoByFieldId(int fieldId) {
        FilmInfoVO filmInfo = moocFieldTMapper.getFilmInfoById(fieldId);
        return filmInfo;
    }

    @Override
    public OrderQueryVO getOrderNeeds(int fieldId) {

        OrderQueryVO orderQueryVO = new OrderQueryVO();

        MoocFieldT moocFieldT = moocFieldTMapper.selectById(fieldId);
        orderQueryVO.setCinemaId(moocFieldT.getCinemaId()+"");
        orderQueryVO.setFilmPrice(moocFieldT.getPrice()+"");

        return orderQueryVO;
    }
}
