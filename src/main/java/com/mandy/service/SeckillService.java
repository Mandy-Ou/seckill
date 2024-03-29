package com.mandy.service;

import com.mandy.domain.OrderInfo;
import com.mandy.domain.SeckillOrder;
import com.mandy.domain.SeckillUser;
import com.mandy.redis.RedisService;
import com.mandy.redis.SeckillKey;
import com.mandy.util.MD5Util;
import com.mandy.util.UUIDUtil;
import com.mandy.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created by MandyOu on 2019/10/23
 */
@Service
public class SeckillService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;


    @Transactional
    public OrderInfo seckill(SeckillUser user, GoodsVo goods) {
        //减库存 下订单 写入秒杀订单
        boolean success = goodsService.reduceStock(goods);
        if (success) {
            //order_info seckill_order
            return orderService.createOrder(user, goods);
        }else{
            setGoodsOver(goods.getId());
            return null;
        }
    }

    public long getSeckillResult(Long id, long goodsId) {
        SeckillOrder order = orderService.getSeckillOrderByUserIdGoodsId(id,goodsId);
        if(Objects.nonNull(order)){//秒杀成功
            return order.getOrderId();
        }else{
            //判断是否卖完(一个存在Redis中的标志)
            boolean isOver = getGoodsOver(goodsId);
            if(isOver){//已经卖完
                return -1;
            }else{
                return 0;
            }
        }
    }

    private void setGoodsOver(Long goodsId) {
        redisService.set(SeckillKey.isGoodsOver,""+goodsId,true);
    }

    private boolean getGoodsOver(long goodsId){
        return redisService.exits(SeckillKey.isGoodsOver,""+goodsId);
    }

    public void reset(List<GoodsVo> goodsList) {
        goodsService.resetStock(goodsList);
        orderService.deleteOrders();
    }

    public boolean checkPath(SeckillUser user, long goodsId, String path) {
        if(Objects.isNull(user) || Objects.isNull(path)){
            return false;
        }
        String pathCheck = redisService.get(SeckillKey.getSeckillPath,""+user.getId()+"_"+goodsId,String.class);
        return path.equals(pathCheck);
    }

    public String createSeckillPath(SeckillUser user, long goodsId) {
        if(Objects.isNull(user) || goodsId <= 0){
            return null;
        }
        String str = MD5Util.md5(UUIDUtil.uuid()+"123456");
        redisService.set(SeckillKey.getSeckillPath,""+user.getId()+"_"+goodsId,str);
        return str;
    }

    public BufferedImage createSeckillVerifyCode(SeckillUser user, long goodsId) {
        if(Objects.isNull(user) || goodsId <= 0){
            return null;
        }
        int width = 80;
        int height = 32;
        //create the image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        // set the background color
        g.setColor(new Color(0xDCDCDC));
        g.fillRect(0, 0, width, height);
        // draw the border
        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);
        // create a random instance to generate the codes
        Random rdm = new Random();
        // make some confusion
        for (int i = 0; i < 50; i++) {
            int x = rdm.nextInt(width);
            int y = rdm.nextInt(height);
            g.drawOval(x, y, 0, 0);
        }
        // generate a random code
        String verifyCode = generateVerifyCode(rdm);
        g.setColor(new Color(0, 100, 0));
        g.setFont(new Font("Candara", Font.BOLD, 24));
        g.drawString(verifyCode, 8, 24);
        g.dispose();
        //把验证码存到redis中
        int rnd = calc(verifyCode);
        redisService.set(SeckillKey.getSeckillVerifyCode, user.getId()+","+goodsId, rnd);
        //输出图片
        return image;
    }

    private static int calc(String exp) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return (Integer)engine.eval(exp);
        }catch(Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static char[] ops = new char[] {'+', '-', '*'};
    /**
     * + - *
     * */
    private String generateVerifyCode(Random rdm) {
        int num1 = rdm.nextInt(10);
        int num2 = rdm.nextInt(10);
        int num3 = rdm.nextInt(10);
        char op1 = ops[rdm.nextInt(3)];
        char op2 = ops[rdm.nextInt(3)];
        String exp = ""+ num1 + op1 + num2 + op2 + num3;
        return exp;
    }

    public boolean checkVerifyCode(SeckillUser user, long goodsId, int verifyCode) {
        if(Objects.isNull(user) || goodsId <= 0){
            return false;
        }
        Integer codeCheck = redisService.get(SeckillKey.getSeckillVerifyCode,user.getId()+","+goodsId,Integer.class);
        if(Objects.isNull(codeCheck) || !codeCheck.equals(verifyCode)){
            return false;
        }
        redisService.delete(SeckillKey.getSeckillVerifyCode,user.getId()+","+goodsId);
        return true;
    }
}
