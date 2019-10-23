package com.mandy.controller;

import com.mandy.domain.SeckillUser;
import com.mandy.redis.RedisService;
import com.mandy.service.GoodsService;
import com.mandy.service.SeckillUserService;
import com.mandy.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Created by MandyOu on 2019/10/21
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    SeckillUserService seckillUserService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @RequestMapping("/to_list")
    public String list(Model model, SeckillUser user){
        model.addAttribute("user",user);
        //查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList",goodsList);
        return "goods_list";
    }

    @RequestMapping("/to_detail/{goodsId}")
    public String detail(Model model, SeckillUser user, @PathVariable("goodsId")long goodsId){
        model.addAttribute("user",user);
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods",goods);
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int seckillStatus = 0;
        int remainSeconds = 0;

        if(now < startAt){      //秒杀还没开始，倒计时
            seckillStatus = 0;
            remainSeconds = (int)((startAt-now)/1000);
        }else if(now > endAt){  //秒杀已经结束
            seckillStatus = 2;
        }else{                  //秒杀进行中
            seckillStatus = 1;
        }
        model.addAttribute("seckillStatus",seckillStatus);
        model.addAttribute("remainSeconds",remainSeconds);
        return "goods_detail";
    }

}
