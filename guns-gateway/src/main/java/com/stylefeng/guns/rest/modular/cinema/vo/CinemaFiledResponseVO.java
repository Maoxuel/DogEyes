package com.stylefeng.guns.rest.modular.cinema.vo;

import com.stylefeng.guns.api.cinema.vo.CinemaInfoVO;
import com.stylefeng.guns.api.cinema.vo.FilmInfoVO;
import com.stylefeng.guns.api.cinema.vo.HallInfoVO;
import lombok.Data;

import java.io.Serializable;

@Data
public class CinemaFiledResponseVO implements Serializable {

    private FilmInfoVO filmInfo;
    private CinemaInfoVO cinemaInfo;
    private HallInfoVO hallInfo;
}
