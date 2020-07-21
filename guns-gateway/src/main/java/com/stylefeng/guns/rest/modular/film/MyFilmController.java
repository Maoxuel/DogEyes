package com.stylefeng.guns.rest.modular.film;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.stylefeng.guns.api.film.FilmAsyncServiceApi;
import com.stylefeng.guns.api.film.FilmServiceApi;
import com.stylefeng.guns.api.film.vo.*;
import com.stylefeng.guns.rest.modular.film.vo.FilmConditionVO;
import com.stylefeng.guns.rest.modular.film.vo.FilmIndexVO;
import com.stylefeng.guns.rest.modular.film.vo.FilmRequestVO;
import com.stylefeng.guns.rest.modular.vo.ResponseVO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/film1")
public class MyFilmController {

    private static final String img_pre = "http://img.meetingshop.cn/";


    @Reference(interfaceClass = FilmServiceApi.class, check = false)
    private FilmServiceApi filmServiceApi;

    @Reference(interfaceClass = FilmAsyncServiceApi.class, async = true, check = false)
    private FilmAsyncServiceApi filmAsyncServiceApi;

    // 获取首页信息接口
    /*
        API网关：
            1、功能聚合【API聚合】
            好处：
                1、六个接口，一次请求，同一时刻节省了五次HTTP请求
                2、同一个接口对外暴漏，降低了前后端分离开发的难度和复杂度
            坏处：
                1、一次获取数据过多，容易出现问题
     */
    @GetMapping(value = "/getIndex1")
    public ResponseVO getIndex() {

        FilmIndexVO filmIndexVO = new FilmIndexVO();
        // 获取banner信息
        filmIndexVO.setBanners(filmServiceApi.getBanners());
        // 获取正在热映的电影
        filmIndexVO.setHotFilms(filmServiceApi.getHotFilms(true, 8, 1, 1, 99, 99, 99));
        // 即将上映的电影
        filmIndexVO.setSoonFilms(filmServiceApi.getSoonFilms(true, 8, 1, 1, 99, 99, 99));
        // 票房排行榜
        filmIndexVO.setBoxRanking(filmServiceApi.getBoxRanking());
        // 获取受欢迎的榜单
        filmIndexVO.setExpectRanking(filmServiceApi.getExpectRanking());
        // 获取前一百
        filmIndexVO.setTop100(filmServiceApi.getTop());

        return ResponseVO.success(filmIndexVO);
    }

    @GetMapping(value = "getConditionList1")
    public ResponseVO getConditionList(@RequestParam(name = "catId", required = false, defaultValue = "99") String catId,
                                       @RequestParam(name = "sourceId", required = false, defaultValue = "99") String sourceId,
                                       @RequestParam(name = "yearId", required = false, defaultValue = "99") String yearId) {

        FilmConditionVO filmConditionVO = new FilmConditionVO();
        //类型集合
        boolean flag = false;
        List<CatVO> cats = filmServiceApi.getCats();
        List<CatVO> catResult = new ArrayList<>();
        CatVO catVO = null;
        for (CatVO cat : cats) {
            // 判断集合是否存在catId，如果存在，则将对应的实体变成active状态
            if (cat.getCatId().equals("99")) {
                catVO = cat;
                continue;
            }
            if (cat.getCatId().equals(catId)) {
                cat.setActive(true);
                flag = true;
            } else {
                cat.setActive(false);
            }
            catResult.add(cat);
        }
        // 如果不存在，则默认将全部变为Active状态
        if (!flag) {
            catVO.setActive(true);
            catResult.add(catVO);
        } else {
            catVO.setActive(false);
            catResult.add(catVO);
        }
        //片源集合
        flag = false;
        List<SourceVO> sources = filmServiceApi.getSources();
        List<SourceVO> sourceResult = new ArrayList<>();
        SourceVO sourceVO = null;
        for (SourceVO source : sources) {
            // 判断集合是否存在catId，如果存在，则将对应的实体变成active状态
            if (source.getSourceId().equals("99")) {
                sourceVO = source;
                continue;
            }
            if (source.getSourceId().equals(catId)) {
                source.setActive(true);
                flag = true;
            } else {
                source.setActive(false);
            }
            sourceResult.add(source);
        }
        // 如果不存在，则默认将全部变为Active状态
        if (!flag) {
            sourceVO.setActive(true);
            sourceResult.add(sourceVO);
        } else {
            sourceVO.setActive(false);
            sourceResult.add(sourceVO);
        }
        //年代集合
        flag = false;
        List<YearVO> years = filmServiceApi.getYears();
        List<YearVO> yearResult = new ArrayList<>();
        YearVO yearVO = null;
        for (YearVO year : years) {
            // 判断集合是否存在catId，如果存在，则将对应的实体变成active状态
            if (year.getYearId().equals("99")) {
                yearVO = year;
                continue;
            }
            if (year.getYearId().equals(catId)) {
                year.setActive(true);
                flag = true;
            } else {
                year.setActive(false);
            }
            yearResult.add(year);
        }
        // 如果不存在，则默认将全部变为Active状态
        if (!flag) {
            yearVO.setActive(true);
            yearResult.add(yearVO);
        } else {
            yearVO.setActive(false);
            yearResult.add(yearVO);
        }

        filmConditionVO.setCatInfo(catResult);
        filmConditionVO.setSourceInfo(sourceResult);
        filmConditionVO.setYearInfo(yearResult);
        return ResponseVO.success(filmConditionVO);
    }

    @RequestMapping(value = "getFilms", method = RequestMethod.GET)
    public ResponseVO getFilms(FilmRequestVO filmRequestVO) {
        String img_pre = "http://img.meetingshop.cn/";

        FilmVO filmInfo = null;
        // 根据showType判断影片查询类型
        switch (filmRequestVO.getShowType()) {
            case 1:
                // 根据sortId排序
                // 添加各种条件查询
                // 判断当前是第几页,下同
                filmInfo = filmServiceApi.getHotFilms(false, filmRequestVO.getNowPage(),
                        filmRequestVO.getPageSize(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(),
                        filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
            case 2:
                filmInfo = filmServiceApi.getSoonFilms(false, filmRequestVO.getNowPage(),
                        filmRequestVO.getPageSize(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(),
                        filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
            case 3:
                filmInfo = filmServiceApi.getClassicFilms(filmRequestVO.getNowPage(),
                        filmRequestVO.getPageSize(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(),
                        filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
            default:
                filmInfo = filmServiceApi.getHotFilms(false, filmRequestVO.getNowPage(),
                        filmRequestVO.getPageSize(), filmRequestVO.getSortId(), filmRequestVO.getSourceId(),
                        filmRequestVO.getYearId(), filmRequestVO.getCatId());
                break;
        }
        return ResponseVO.success(filmInfo.getNowPage(), filmInfo.getTotalPage(),
                img_pre, filmInfo.getFilmInfo());
    }

    @RequestMapping(value = "films/{searchParam}", method = RequestMethod.GET)
    public ResponseVO films(@PathVariable("searchParam") String searchParam,
                            int searchType) throws ExecutionException, InterruptedException {

        // 根据searchType，判断查询类型
        FilmDetailVO filmDetail = filmServiceApi.getFilmDetail(searchType, searchParam);
        if (filmDetail == null) {
            return ResponseVO.serviceFail("没有可查询的影片");
        } else if (filmDetail.getFilmId() == null || filmDetail.getFilmId().trim().length() == 0) {
            return ResponseVO.serviceFail("查询的影片为空");
        }


        String filmId = filmDetail.getFilmId();
        // 查询影片的详细信息 -> Dubbo的异步调用
        // 获取影片描述信息
        filmAsyncServiceApi.getFilmDesc(filmId);
        Future<FilmDescVO> filmDescVOFuture = RpcContext.getContext().getFuture();

        //获取导演
        filmAsyncServiceApi.getDectInfo(filmId);
        Future<ActorVO> actorVOFuture = RpcContext.getContext().getFuture();

        //获取演员
        filmAsyncServiceApi.getActors(filmId);
        Future<List<ActorVO>> actorVOFuture1 = RpcContext.getContext().getFuture();

        //获取图片
        filmAsyncServiceApi.getImgs(filmId);
        Future<ImgVO> imgVOFuture = RpcContext.getContext().getFuture();

        //组织info4
        InfoRequstVO infoRequstVO = new InfoRequstVO();

        infoRequstVO.setBiography(filmDescVOFuture.get().getBiography());
        infoRequstVO.setImgVO(imgVOFuture.get());

        ActorRequestVO actorRequestVO = new ActorRequestVO();
        actorRequestVO.setDirector(actorVOFuture.get());
        actorRequestVO.setActors(actorVOFuture1.get());
        infoRequstVO.setActors(actorRequestVO);

        // 组织成返回值
        filmDetail.setInfo04(infoRequstVO);

        return ResponseVO.success(img_pre, filmDetail);
    }
}
