package com.stylefeng.guns.rest.modular.film.serivce;

import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.stylefeng.guns.api.film.FilmServiceApi;
import com.stylefeng.guns.api.film.vo.*;
import com.stylefeng.guns.core.util.DateUtil;
import com.stylefeng.guns.rest.common.persistence.dao.*;
import com.stylefeng.guns.rest.common.persistence.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Service(interfaceClass = FilmServiceApi.class,filter = "tracing")
public class MyFilmServiceImpl implements FilmServiceApi {

    @Autowired
    private MoocBannerTMapper moocBannerTMapper;

    @Autowired
    private MoocFilmTMapper moocFilmTMapper;

    @Autowired
    private MoocCatDictTMapper moocCatDictTMapper;

    @Autowired
    private MoocSourceDictTMapper moocSourceDictTMapper;

    @Autowired
    private MoocYearDictTMapper moocYearDictTMapper;

    @Autowired
    private MoocFilmInfoTMapper moocFilmInfoTMapper;

    @Autowired
    private MoocActorTMapper moocActorTMapper;

    @Override
    public List<BannerVO> getBanners() {

        List<BannerVO> bannerVos = new ArrayList<>();
        List<MoocBannerT> bannerTs = moocBannerTMapper.selectList(null);
        for (MoocBannerT moocBannerT:bannerTs){
            BannerVO bannerVO = new BannerVO();
            bannerVO.setBannerId(moocBannerT.getUuid()+"");
            bannerVO.setBannerUrl(moocBannerT.getBannerUrl());
            bannerVO.setBannerAddress(moocBannerT.getBannerAddress());
            bannerVos.add(bannerVO);
        }

        return bannerVos;
    }
    private List<FilmInfo> getFilmInfos(List<MoocFilmT> moocFilms){
        List<FilmInfo> filmInfos = new ArrayList<>();
        for(MoocFilmT moocFilmT : moocFilms){
            FilmInfo filmInfo = new FilmInfo();
            filmInfo.setScore(moocFilmT.getFilmScore());
            filmInfo.setImgAddress(moocFilmT.getImgAddress());
            filmInfo.setFilmType(moocFilmT.getFilmType());
            filmInfo.setFilmScore(moocFilmT.getFilmScore());
            filmInfo.setFilmName(moocFilmT.getFilmName());
            filmInfo.setFilmId(moocFilmT.getUuid()+"");
            filmInfo.setExpectNum(moocFilmT.getFilmPresalenum());
            filmInfo.setBoxNum(moocFilmT.getFilmBoxOffice());
            filmInfo.setShowTime(DateUtil.getDay(moocFilmT.getFilmTime()));

            // 将转换的对象放入结果集
            filmInfos.add(filmInfo);
        }

        return filmInfos;
    }
    @Override
    public FilmVO getHotFilms(boolean isLimit, int nums, int nowPage, int sortId, int sourceId, int yearId, int catId) {

        FilmVO filmVO = new FilmVO();
        List<FilmInfo> filmInfos = new ArrayList<>();

        EntityWrapper<MoocFilmT> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("film_status","1");

        //判断是否是首页需要的内容
        if (isLimit){
            //如果是，限制条数，限制内容为热映影片
            Page<MoocFilmT> page = new Page<>(nowPage,nums);
            List<MoocFilmT> moocFilmTS = moocFilmTMapper.selectPage(page, entityWrapper);
            //组织filmInfo
            filmInfos = getFilmInfos(moocFilmTS);
            filmVO.setFilmInfo(filmInfos);
            filmVO.setFilmNum(moocFilmTS.size());
        }else {
            //如果不是，则是列表页，同样需要限制内容为热映
            Page<MoocFilmT> page = null;
            // 根据sortId的不同，来组织不同的Page对象
            // 1-按热门搜索，2-按时间搜索，3-按评价搜索
            switch (sortId){
                case 1 :
                    page = new Page<>(nowPage,nums,"film_box_office");
                    break;
                case 2 :
                    page = new Page<>(nowPage,nums,"film_time");
                    break;
                case 3 :
                    page = new Page<>(nowPage,nums,"film_score");
                    break;
                default:
                    page = new Page<>(nowPage,nums,"film_box_office");
                    break;
            }

            // 如果sourceId,yearId,catId 不为99 ,则表示要按照对应的编号进行查询
            if (sourceId != 99){
                entityWrapper.eq("film_source",sourceId );
            }
            if (yearId != 99){
                entityWrapper.eq("film_date",yearId);
            }
            if (catId != 99){
                // #2#4#22#
                String cat = "%#"+catId+"#%";
                entityWrapper.like("film_cats",cat);
            }

            List<MoocFilmT> moocFilms = moocFilmTMapper.selectPage(page, entityWrapper);
            //组织filmInfo
            filmInfos = getFilmInfos(moocFilms);
            filmVO.setFilmInfo(filmInfos);
            filmVO.setFilmNum(moocFilms.size());

            // 需要总页数 totalCounts/nums -> 0 + 1 = 1
            // 每页10条，我现在有6条 -> 1
            filmVO.setNowPage(nowPage);
            Integer count = moocFilmTMapper.selectCount(entityWrapper);
            int totalPage = (count/nums)+1;
            filmVO.setTotalPage(totalPage);
        }

        return filmVO;
    }

    @Override
    public FilmVO getSoonFilms(boolean isLimit, int nums, int nowPage, int sortId, int sourceId, int yearId, int catId) {

        FilmVO filmVO = new FilmVO();
        List<FilmInfo> filmInfos = new ArrayList<>();

        EntityWrapper<MoocFilmT> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("film_status","2");
        //判断是否是首页需要的内容
        if (isLimit){
            //如果是，限制条数，限制内容为热映影片
            Page<MoocFilmT> page = new Page<>(nowPage,nums);
            List<MoocFilmT> moocFilmTS = moocFilmTMapper.selectPage(page, entityWrapper);
            //组织filmInfo
            filmInfos = getFilmInfos(moocFilmTS);
            filmVO.setFilmInfo(filmInfos);
            filmVO.setFilmNum(moocFilmTS.size());

        }else {
            //如果不是，则是列表页，同样需要限制内容为热映
            Page<MoocFilmT> page = null;
            // 根据sortId的不同，来组织不同的Page对象
            // 1-按热门搜索，2-按时间搜索，3-按评价搜索
            switch (sortId){
                case 1 :
                    page = new Page<>(nowPage,nums,"film_preSaleNum");
                    break;
                case 2 :
                    page = new Page<>(nowPage,nums,"film_time");
                    break;
                case 3 :
                    page = new Page<>(nowPage,nums,"film_preSaleNum");
                    break;
                default:
                    page = new Page<>(nowPage,nums,"film_preSaleNum");
                    break;
            }

            // 如果sourceId,yearId,catId 不为99 ,则表示要按照对应的编号进行查询
            if (sourceId != 99){
                entityWrapper.eq("film_source",sourceId );
            }
            if (yearId != 99){
                entityWrapper.eq("film_date",yearId);
            }
            if (catId != 99){
                // #2#4#22#
                String cat = "%#"+catId+"#%";
                entityWrapper.like("film_cats",cat);
            }

            List<MoocFilmT> moocFilms = moocFilmTMapper.selectPage(page, entityWrapper);
            //组织filmInfo
            filmInfos = getFilmInfos(moocFilms);
            filmVO.setFilmInfo(filmInfos);
            filmVO.setFilmNum(moocFilms.size());

            // 需要总页数 totalCounts/nums -> 0 + 1 = 1
            // 每页10条，我现在有6条 -> 1
            filmVO.setNowPage(nowPage);
            Integer count = moocFilmTMapper.selectCount(entityWrapper);
            int totalPage = (count/nums)+1;
            filmVO.setTotalPage(totalPage);
        }
        return null;
    }

    @Override
    public FilmVO getClassicFilms(int nums, int nowPage, int sortId, int sourceId, int yearId, int catId) {
        FilmVO filmVO = new FilmVO();
        List<FilmInfo> filmInfos = new ArrayList<>();

        // 即将上映影片的限制条件
        EntityWrapper<MoocFilmT> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("film_status","3");

        // 如果不是，则是列表页，同样需要限制内容为即将上映影片
        Page<MoocFilmT> page = null;
        // 根据sortId的不同，来组织不同的Page对象
        // 1-按热门搜索，2-按时间搜索，3-按评价搜索
        switch (sortId){
            case 1 :
                page = new Page<>(nowPage,nums,"film_box_office");
                break;
            case 2 :
                page = new Page<>(nowPage,nums,"film_time");
                break;
            case 3 :
                page = new Page<>(nowPage,nums,"film_score");
                break;
            default:
                page = new Page<>(nowPage,nums,"film_box_office");
                break;
        }

        // 如果sourceId,yearId,catId 不为99 ,则表示要按照对应的编号进行查询
        if(sourceId != 99){
            entityWrapper.eq("film_source",sourceId);
        }
        if(yearId != 99){
            entityWrapper.eq("film_date",yearId);
        }
        if(catId != 99){
            // #2#4#22#
            String catStr = "%#"+catId+"#%";
            entityWrapper.like("film_cats",catStr);
        }

        List<MoocFilmT> moocFilms = moocFilmTMapper.selectPage(page, entityWrapper);
        // 组织filmInfos
        filmInfos = getFilmInfos(moocFilms);
        filmVO.setFilmNum(moocFilms.size());

        // 需要总页数 totalCounts/nums -> 0 + 1 = 1
        // 每页10条，我现在有6条 -> 1
        int totalCounts = moocFilmTMapper.selectCount(entityWrapper);
        int totalPages = (totalCounts/nums)+1;

        filmVO.setFilmInfo(filmInfos);
        filmVO.setTotalPage(totalPages);
        filmVO.setNowPage(nowPage);

        return filmVO;
    }

    @Override
    public List<FilmInfo> getBoxRanking() {
        // 条件 -> 正在上映的，票房前10名
        EntityWrapper<MoocFilmT> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("film_status","1");

        Page<MoocFilmT> page = new Page<>(1,10,"film_box_office");
        List<MoocFilmT> moocFilms = moocFilmTMapper.selectPage(page, entityWrapper);
        List<FilmInfo> filmInfos = getFilmInfos(moocFilms);

        return filmInfos;
    }

    @Override
    public List<FilmInfo> getExpectRanking() {
        // 条件 -> 即将上映的，预售前10名
        EntityWrapper<MoocFilmT> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("film_status","2");

        Page<MoocFilmT> page = new Page<>(1,10,"film_preSaleNum");
        List<MoocFilmT> moocFilms = moocFilmTMapper.selectPage(page, entityWrapper);
        List<FilmInfo> filmInfos = getFilmInfos(moocFilms);
        return filmInfos;
    }

    @Override
    public List<FilmInfo> getTop() {
        // 条件 -> 正在上映的，评分前10名
        EntityWrapper<MoocFilmT> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("film_status","1");

        Page<MoocFilmT> page = new Page<>(1,10,"film_score");
        List<MoocFilmT> moocFilms = moocFilmTMapper.selectPage(page, entityWrapper);
        List<FilmInfo> filmInfos = getFilmInfos(moocFilms);
        return filmInfos;
    }

    @Override
    public List<CatVO> getCats() {
        List<CatVO> catVOS = new ArrayList<>();
        List<MoocCatDictT> moocCatDictTList = moocCatDictTMapper.selectList(null);

        for(MoocCatDictT moocCatDictT:moocCatDictTList){
            CatVO catVO = new CatVO();
            catVO.setCatId(moocCatDictT.getUuid()+"");
            catVO.setCatName(moocCatDictT.getShowName());
            catVOS.add(catVO);
        }
        return catVOS;
    }

    @Override
    public List<SourceVO> getSources() {
        List<SourceVO> sourceVOS = new ArrayList<>();
        List<MoocSourceDictT> sourceDictTList = moocSourceDictTMapper.selectList(null);
        for(MoocSourceDictT moocSourceDictT:sourceDictTList){
            SourceVO sourceVO = new SourceVO();
            sourceVO.setSourceId(moocSourceDictT.getUuid()+"");
            sourceVO.setSourceName(moocSourceDictT.getShowName());
            sourceVOS.add(sourceVO);
        }
        return sourceVOS;
    }

    @Override
    public List<YearVO> getYears() {
        List<YearVO> years = new ArrayList<>();
        // 查询实体对象 - MoocCatDictT
        List<MoocYearDictT> moocYears = moocYearDictTMapper.selectList(null);
        // 将实体对象转换为业务对象 - CatVO
        for(MoocYearDictT moocYearDictT : moocYears){
            YearVO yearVO = new YearVO();
            yearVO.setYearId(moocYearDictT.getUuid()+"");
            yearVO.setYearName(moocYearDictT.getShowName());

            years.add(yearVO);
        }
        return years;
    }

    @Override
    public FilmDetailVO getFilmDetail(int searchType, String searchParam) {
        FilmDetailVO filmDetailVO = null;
        // searchType 1-按名称  2-按ID的查找
        if (searchType == 1){
            filmDetailVO = moocFilmTMapper.getFilmDetailByName("%" + searchParam + "%");
        }else {
            filmDetailVO = moocFilmTMapper.getFilmDetailById(searchParam);
        }
        return filmDetailVO;
    }

    private MoocFilmInfoT getFilmInfo(String filmId){
        MoocFilmInfoT moocFilmInfoT = new MoocFilmInfoT();
        moocFilmInfoT.setFilmId(filmId);
        MoocFilmInfoT filmInfo = moocFilmInfoTMapper.selectOne(moocFilmInfoT);
        return filmInfo;
    }

    @Override
    public FilmDescVO getFilmDesc(String filmId) {
        FilmDescVO filmDescVO = new FilmDescVO();
        MoocFilmInfoT filmInfo = getFilmInfo(filmId);
        filmDescVO.setBiography(filmInfo.getBiography());
        filmDescVO.setFilmId(filmId);
        return filmDescVO;
    }

    @Override
    public ImgVO getImgs(String filmId) {
        MoocFilmInfoT filmInfo = getFilmInfo(filmId);
        String imgs = filmInfo.getFilmImgs();
        String[] split = imgs.split(",");
        ImgVO imgVO = new ImgVO();
        imgVO.setMainImg(split[0]);
        imgVO.setImg01(split[1]);
        imgVO.setImg02(split[2]);
        imgVO.setImg03(split[3]);
        imgVO.setImg04(split[4]);
        return imgVO;
    }

    @Override
    public ActorVO getDectInfo(String filmId) {
        MoocFilmInfoT filmInfo = getFilmInfo(filmId);
        Integer directorId = filmInfo.getDirectorId();
        MoocActorT director = moocActorTMapper.selectById(directorId);
        ActorVO actorVO = new ActorVO();
        actorVO.setDirectorName(director.getActorName());
        actorVO.setImgAddress(director.getActorImg());
        return actorVO;
    }

    @Override
    public List<ActorVO> getActors(String filmId) {
        List<ActorVO> actors = moocActorTMapper.getActors(filmId);

        return actors;
    }
}
