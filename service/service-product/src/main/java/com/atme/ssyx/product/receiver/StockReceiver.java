package com.atme.ssyx.product.receiver;

import com.atme.ssyx.common.constant.MqConst;
import com.atme.ssyx.product.service.SkuInfoService;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StockReceiver {

    @Autowired
    private SkuInfoService skuInfoService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_MINUS_STOCK, durable = "true"),exchange = @Exchange(value = MqConst.EXCHANGE_ORDER_DIRECT),key = {MqConst.ROUTING_MINUS_STOCK}))
    public void minusStock(String orderNo, Message message, Channel channel) throws IOException {
        if (!StringUtils.isEmpty(orderNo)){
            skuInfoService.minusStock(orderNo);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
